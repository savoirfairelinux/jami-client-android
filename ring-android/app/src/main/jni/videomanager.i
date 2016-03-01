/*
 *  Copyright (C) 2015-2016 Savoir-faire Linux Inc.
 *
 *  Authors: Damien Riegel <damien.riegel@savoirfairelinux.com>
 *           Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *           Ciro Santilli <ciro.santilli@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

%include <std_shared_ptr.i>
%header %{
#include <cassert>
#include <functional>
#include <list>

#include "dring/dring.h"
#include "dring/videomanager_interface.h"
#include "media/media_buffer.h"
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/log.h>
#include <OMXAL/OpenMAXAL.h>
#include <OMXAL/OpenMAXAL_Android.h>

class VideoCallback {
public:
    virtual ~VideoCallback(){}
    virtual void getCameraInfo(const std::string& device, std::vector<int> *formats, std::vector<unsigned> *sizes, std::vector<unsigned> *rates) {}
    virtual void setParameters(const std::string, const int format, const int width, const int height, const int rate) {}
    virtual void startCapture(const std::string& camid) {}
    virtual void stopCapture() {}
    virtual void decodingStarted(const std::string& id, const std::string& shm_path, int w, int h, bool is_mixer) {}
    virtual void decodingStopped(const std::string& id, const std::string& shm_path, bool is_mixer) {}
};
%}

%feature("director") VideoCallback;

%{

std::list<ring::VideoFrame*> frameQueue {};

#define TAG "NativeMedia"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
static XAObjectItf engineObject = NULL;
static XAEngineItf engineEngine = NULL;
static XAObjectItf outputMixObject = NULL;
static XAObjectItf             playerObj = NULL;
static XAPlayItf               playerPlayItf = NULL;
static XAAndroidBufferQueueItf playerBQItf = NULL;
static XAStreamInformationItf  playerStreamInfoItf = NULL;
static XAVolumeItf             playerVolItf = NULL;
#define NB_MAXAL_INTERFACES 3
static ANativeWindow* theNativeWindow;
#define NB_BUFFERS 8
#define MPEG2_TS_PACKET_SIZE 188
#define PACKETS_PER_BUFFER 10
#define BUFFER_SIZE (PACKETS_PER_BUFFER*MPEG2_TS_PACKET_SIZE)
static char dataCache[BUFFER_SIZE * NB_BUFFERS];
static jboolean reachedEof = JNI_FALSE;
static const int kEosBufferCntxt = 1980; // a magic value we can compare against
static pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t cond = PTHREAD_COND_INITIALIZER;
static jboolean discontinuity = JNI_FALSE;
static jboolean enqueueInitialBuffers(jboolean discontinuity);

// Remove discontinuity check.
static XAresult AndroidBufferQueueCallback(
        XAAndroidBufferQueueItf caller,
        void *pCallbackContext,        /* input */
        void *pBufferContext,          /* input */
        void *pBufferData,             /* input */
        XAuint32 dataSize,             /* input */
        XAuint32 dataUsed,             /* input */
        const XAAndroidBufferItem *pItems,/* input */
        XAuint32 itemsLength           /* input */)
{
    XAresult res;
    int ok;

    // pCallbackContext was specified as NULL at RegisterCallback and is unused here
    assert(NULL == pCallbackContext);

    // note there is never any contention on this mutex unless a discontinuity request is active
    ok = pthread_mutex_lock(&mutex);
    assert(0 == ok);

    if ((pBufferData == NULL) && (pBufferContext != NULL)) {
        const int processedCommand = *(int *)pBufferContext;
        if (kEosBufferCntxt == processedCommand) {
            LOGV("EOS was processed\n");
            // our buffer with the EOS message has been consumed
            assert(0 == dataSize);
            goto exit;
        }
    }

    // pBufferData is a pointer to a buffer that we previously Enqueued
    assert((dataSize > 0) && ((dataSize % MPEG2_TS_PACKET_SIZE) == 0));
    assert(dataCache <= (char *) pBufferData && (char *) pBufferData <
            &dataCache[BUFFER_SIZE * NB_BUFFERS]);
    assert(0 == (((char *) pBufferData - dataCache) % BUFFER_SIZE));

    // don't bother trying to read more data once we've hit EOF
    if (reachedEof) {
        goto exit;
    }

    /* On the example, an fread was here. Now we read from frames instead. */
    size_t bytesRead;
    ring::VideoFrame *frame = frameQueue.pop_front();
    bytesRead = frame.size();
    pBufferData = frame.pointer();

    if (bytesRead > 0) {
        if ((bytesRead % MPEG2_TS_PACKET_SIZE) != 0) {
            LOGV("Dropping last packet because it is not whole");
        }
        size_t packetsRead = bytesRead / MPEG2_TS_PACKET_SIZE;
        size_t bufferSize = packetsRead * MPEG2_TS_PACKET_SIZE;
        res = (*caller)->Enqueue(caller, NULL /*pBufferContext*/,
                pBufferData /*pData*/,
                bufferSize /*dataLength*/,
                NULL /*pMsg*/,
                0 /*msgLength*/);
        assert(XA_RESULT_SUCCESS == res);
    } else {
        // EOF or I/O error, signal EOS
        XAAndroidBufferItem msgEos[1];
        msgEos[0].itemKey = XA_ANDROID_ITEMKEY_EOS;
        msgEos[0].itemSize = 0;
        // EOS message has no parameters, so the total size of the message is the size of the key
        //   plus the size if itemSize, both XAuint32
        res = (*caller)->Enqueue(caller, (void *)&kEosBufferCntxt /*pBufferContext*/,
                NULL /*pData*/, 0 /*dataLength*/,
                msgEos /*pMsg*/,
                sizeof(XAuint32)*2 /*msgLength*/);
        assert(XA_RESULT_SUCCESS == res);
        reachedEof = JNI_TRUE;
    }

exit:
    ok = pthread_mutex_unlock(&mutex);
    assert(0 == ok);
    return XA_RESULT_SUCCESS;
}


// callback invoked whenever there is new or changed stream information
static void StreamChangeCallback(XAStreamInformationItf caller,
        XAuint32 eventId,
        XAuint32 streamIndex,
        void * pEventData,
        void * pContext )
{
    LOGV("StreamChangeCallback called for stream %u", streamIndex);
    // pContext was specified as NULL at RegisterStreamChangeCallback and is unused here
    assert(NULL == pContext);
    switch (eventId) {
      case XA_STREAMCBEVENT_PROPERTYCHANGE: {
        /** From spec 1.0.1:
            "This event indicates that stream property change has occurred.
            The streamIndex parameter identifies the stream with the property change.
            The pEventData parameter for this event is not used and shall be ignored."
         */

        XAresult res;
        XAuint32 domain;
        res = (*caller)->QueryStreamType(caller, streamIndex, &domain);
        assert(XA_RESULT_SUCCESS == res);
        switch (domain) {
          case XA_DOMAINTYPE_VIDEO: {
            XAVideoStreamInformation videoInfo;
            res = (*caller)->QueryStreamInformation(caller, streamIndex, &videoInfo);
            assert(XA_RESULT_SUCCESS == res);
            LOGV("Found video size %u x %u, codec ID=%u, frameRate=%u, bitRate=%u, duration=%u ms",
                        videoInfo.width, videoInfo.height, videoInfo.codecId, videoInfo.frameRate,
                        videoInfo.bitRate, videoInfo.duration);
          } break;
          default:
            fprintf(stderr, "Unexpected domain %u\n", domain);
            break;
        }
      } break;
      default:
        fprintf(stderr, "Unexpected stream event ID %u\n", eventId);
        break;
    }
}

// Enqueue the initial buffers, and optionally signal a discontinuity in the first buffer
static jboolean enqueueInitialBuffers(jboolean discontinuity)
{

    /* Fill our cache.
     * We want to read whole packets (integral multiples of MPEG2_TS_PACKET_SIZE).
     * fread returns units of "elements" not bytes, so we ask for 1-byte elements
     * and then check that the number of elements is a multiple of the packet size.
     */
    size_t bytesRead;

    /*bytesRead = fread(dataCache, 1, BUFFER_SIZE * NB_BUFFERS, file);*/

    if (bytesRead <= 0) {
        // could be premature EOF or I/O error
        return JNI_FALSE;
    }
    if ((bytesRead % MPEG2_TS_PACKET_SIZE) != 0) {
        LOGV("Dropping last packet because it is not whole");
    }
    size_t packetsRead = bytesRead / MPEG2_TS_PACKET_SIZE;
    LOGV("Initially queueing %zu packets", packetsRead);

    /* Enqueue the content of our cache before starting to play,
       we don't want to starve the player */
    size_t i;
    for (i = 0; i < NB_BUFFERS && packetsRead > 0; i++) {
        // compute size of this buffer
        size_t packetsThisBuffer = packetsRead;
        if (packetsThisBuffer > PACKETS_PER_BUFFER) {
            packetsThisBuffer = PACKETS_PER_BUFFER;
        }
        size_t bufferSize = packetsThisBuffer * MPEG2_TS_PACKET_SIZE;
        XAresult res;
        if (discontinuity) {
            // signal discontinuity
            XAAndroidBufferItem items[1];
            items[0].itemKey = XA_ANDROID_ITEMKEY_DISCONTINUITY;
            items[0].itemSize = 0;
            // DISCONTINUITY message has no parameters,
            //   so the total size of the message is the size of the key
            //   plus the size if itemSize, both XAuint32
            res = (*playerBQItf)->Enqueue(playerBQItf, NULL /*pBufferContext*/,
                    dataCache + i*BUFFER_SIZE, bufferSize, items /*pMsg*/,
                    sizeof(XAuint32)*2 /*msgLength*/);
            discontinuity = JNI_FALSE;
        } else {
            res = (*playerBQItf)->Enqueue(playerBQItf, NULL /*pBufferContext*/,
                    dataCache + i*BUFFER_SIZE, bufferSize, NULL, 0);
        }
        assert(XA_RESULT_SUCCESS == res);
        packetsRead -= packetsThisBuffer;
    }

    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_cx_ring_service_RingserviceJNI_setVideoFrame(JNIEnv *jenv, jclass jcls, void * jarg1, jint jarg2, jlong jarg3)
{
    jenv->GetByteArrayRegion(jarg1, 0, jarg2, jarg3);
}

JNIEXPORT jlong JNICALL Java_cx_ring_service_RingserviceJNI_acquireNativeWindow(JNIEnv *jenv, jclass jcls, jobject javaSurface)
{
    return (jlong)(intptr_t)ANativeWindow_fromSurface(jenv, javaSurface);
}

JNIEXPORT jlong JNICALL Java_cx_ring_service_RingserviceJNI_releaseNativeWindow(JNIEnv *jenv, jclass jcls, jlong window_)
{
    __android_log_print(ANDROID_LOG_WARN, "videomanager.i", "RingserviceJNI_releaseNativeWindow");
    ANativeWindow *window = (ANativeWindow*)((intptr_t) window_);
    if (window)
        ANativeWindow_release(window);
    //__android_log_print(ANDROID_LOG_WARN, "videomanager.i", "RingserviceJNI_releaseNativeWindow clearing %z", frameQueue.size());
    //frameQueue.clear();
}

JNIEXPORT void JNICALL Java_cx_ring_service_RingserviceJNI_setNativeWindowGeometry(JNIEnv *jenv, jclass jcls, jlong window_, int width, int height)
{
    ANativeWindow *window = (ANativeWindow*)((intptr_t) window_);
    ANativeWindow_setBuffersGeometry(window, width, height, WINDOW_FORMAT_RGBA_8888);
}

void FrameAvailable(ring::VideoFrame *frame) {
    __android_log_print(ANDROID_LOG_WARN, "videomanager.i", "frameAvailable");
    frameQueue.emplace_back(frame);
}

JNIEXPORT void JNICALL Java_cx_ring_service_RingserviceJNI_registerVideoCallback(JNIEnv *jenv, jclass jcls, jstring sinkId, jlong window)
{
    ANativeWindow *nativeWindow = (ANativeWindow*)((intptr_t) window);
    /* Glue to the exampe. */
    theNativeWindow = nativeWindow;
    /* Copy past of the example.*/
    /* createEngine */
    XAresult res;
    res = xaCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    assert(XA_RESULT_SUCCESS == res);
    res = (*engineObject)->Realize(engineObject, XA_BOOLEAN_FALSE);
    assert(XA_RESULT_SUCCESS == res);
    res = (*engineObject)->GetInterface(engineObject, XA_IID_ENGINE, &engineEngine);
    assert(XA_RESULT_SUCCESS == res);
    res = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 0, NULL, NULL);
    assert(XA_RESULT_SUCCESS == res);
    res = (*outputMixObject)->Realize(outputMixObject, XA_BOOLEAN_FALSE);
    assert(XA_RESULT_SUCCESS == res);
    /* createStreamingMediaPlayer */
    XADataLocator_AndroidBufferQueue loc_abq = { XA_DATALOCATOR_ANDROIDBUFFERQUEUE, NB_BUFFERS };
    XADataFormat_MIME format_mime = {
            XA_DATAFORMAT_MIME, XA_ANDROID_MIME_MP2TS, XA_CONTAINERTYPE_MPEG_TS };
    XADataSource dataSrc = {&loc_abq, &format_mime};
    XADataLocator_OutputMix loc_outmix = { XA_DATALOCATOR_OUTPUTMIX, outputMixObject };
    XADataSink audioSnk = { &loc_outmix, NULL };
    XADataLocator_NativeDisplay loc_nd = {
            XA_DATALOCATOR_NATIVEDISPLAY,        // locatorType
            (void*)theNativeWindow,              // hWindow
            NULL                                 // hDisplay
    };
    XADataSink imageVideoSink = {&loc_nd, NULL};
    XAboolean     required[NB_MAXAL_INTERFACES]
                           = {XA_BOOLEAN_TRUE, XA_BOOLEAN_TRUE,           XA_BOOLEAN_TRUE};
    XAInterfaceID iidArray[NB_MAXAL_INTERFACES]
                           = {XA_IID_PLAY,     XA_IID_ANDROIDBUFFERQUEUESOURCE,
                                               XA_IID_STREAMINFORMATION};
    res = (*engineEngine)->CreateMediaPlayer(engineEngine, &playerObj, &dataSrc,
            NULL, &audioSnk, &imageVideoSink, NULL, NULL,
            NB_MAXAL_INTERFACES,
            iidArray,
            required);
    assert(XA_RESULT_SUCCESS == res);
    res = (*playerObj)->Realize(playerObj, XA_BOOLEAN_FALSE);
    assert(XA_RESULT_SUCCESS == res);
    res = (*playerObj)->GetInterface(playerObj, XA_IID_PLAY, &playerPlayItf);
    assert(XA_RESULT_SUCCESS == res);
    res = (*playerObj)->GetInterface(playerObj, XA_IID_STREAMINFORMATION, &playerStreamInfoItf);
    assert(XA_RESULT_SUCCESS == res);
    res = (*playerObj)->GetInterface(playerObj, XA_IID_VOLUME, &playerVolItf);
    assert(XA_RESULT_SUCCESS == res);
    res = (*playerObj)->GetInterface(playerObj, XA_IID_ANDROIDBUFFERQUEUESOURCE, &playerBQItf);
    assert(XA_RESULT_SUCCESS == res);
    res = (*playerBQItf)->SetCallbackEventsMask(playerBQItf, XA_ANDROIDBUFFERQUEUEEVENT_PROCESSED);
    assert(XA_RESULT_SUCCESS == res);
    res = (*playerBQItf)->RegisterCallback(playerBQItf, AndroidBufferQueueCallback, NULL);
    assert(XA_RESULT_SUCCESS == res);
    res = (*playerStreamInfoItf)->RegisterStreamChangeCallback(playerStreamInfoItf,
            StreamChangeCallback, NULL);
    assert(XA_RESULT_SUCCESS == res);
    if (!enqueueInitialBuffers(JNI_FALSE)) {
        return JNI_FALSE;
    }
    res = (*playerPlayItf)->SetPlayState(playerPlayItf, XA_PLAYSTATE_PAUSED);
    assert(XA_RESULT_SUCCESS == res);
    res = (*playerVolItf)->SetVolumeLevel(playerVolItf, 0);
    assert(XA_RESULT_SUCCESS == res);
    res = (*playerPlayItf)->SetPlayState(playerPlayItf, XA_PLAYSTATE_PLAYING);
    assert(XA_RESULT_SUCCESS == res);
}
%}
%native(setVideoFrame) void setVideoFrame(void *, int, long);
%native(acquireNativeWindow) jlong acquireNativeWindow(jobject);
%native(releaseNativeWindow) void releaseNativeWindow(jlong);
%native(setNativeWindowGeometry) void setNativeWindowGeometry(jlong, int, int);
%native(registerVideoCallback) void registerVideoCallback(jstring, jlong);


namespace DRing {
void startCamera();
void stopCamera();
bool hasCameraStarted();
bool switchInput(const std::string& resource);
bool switchToCamera();

void addVideoDevice(const std::string &node);
void removeVideoDevice(const std::string &node);
long obtainFrame(int length);
void releaseFrame(long frame);
void registerSinkTarget(const std::string& sinkId, const DRing::SinkTarget& target);
}

class VideoCallback {
public:
    virtual ~VideoCallback(){}
    virtual void getCameraInfo(const std::string& device, std::vector<int> *formats, std::vector<unsigned> *sizes, std::vector<unsigned> *rates){}
    virtual void setParameters(const std::string, const int format, const int width, const int height, const int rate) {}
    virtual void startCapture(const std::string& camid) {}
    virtual void stopCapture() {}
    virtual void decodingStarted(const std::string& id, const std::string& shm_path, int w, int h, bool is_mixer) {}
    virtual void decodingStopped(const std::string& id, const std::string& shm_path, bool is_mixer) {}
};
