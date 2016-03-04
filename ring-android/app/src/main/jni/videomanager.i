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
#include <cstring>
#include <functional>

#include "dring/dring.h"
#include "dring/videomanager_interface.h"
#include "media/NdkMediaCodec.h"
#include "media/NdkMediaFormat.h"
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/log.h>

#define TAG "videomanager.i"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)

extern "C" {
#include <libavcodec/avcodec.h>
}

static AMediaCodec *codec;

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

void FrameAvailable(AVPacket *packet, int& len, int& frameFinished) {
    __android_log_print(ANDROID_LOG_WARN, "videomanager.i", "FrameAvailable");

    // Adapted from doCodecWork.

    bool sawInputEOS = false;
    bool sawOutputEOS = false;
    size_t offset = 0;
    // TODO how to choose this? 0 (no wait) crashes app,
    // -1 (infinite wait) gives a black image + crash after exiting the call.
    long timeoutUs = 1000;

    //while (!sawInputEOS || !sawOutputEOS) {
        ssize_t bufidx = -1;
        if (!sawInputEOS) {
            bufidx = AMediaCodec_dequeueInputBuffer(codec, timeoutUs);
            LOGV("input buffer %zd", bufidx);
            if (bufidx >= 0) {
                size_t bufsize;
                uint8_t *buf = AMediaCodec_getInputBuffer(codec, bufidx, &bufsize);
                // TODO. Guessing wildly here. Put as much packet data as possible into the buffer.
                // This was the key part of the code that used MediaExtractor.
                //size_t sampleSize = AMediaExtractor_readSampleData(d->ex, buf, bufsize);
                size_t sampleSize = std::min(bufsize, packet->size - offset);
                LOGV("packet->size %d", packet->size);
                LOGV("bufsize %zd", bufsize);
                LOGV("offset %zd", offset);
                LOGV("sampleSize %zd", sampleSize);
                std::memcpy(buf, packet->data + offset, sampleSize);
                if (sampleSize == 0) {
                    sawInputEOS = true;
                    LOGV("input EOS");
                }
                // TODO microseconds since call started?
                // int64_t presentationTimeUs = AMediaExtractor_getSampleTime(d->ex);
                int64_t presentationTimeUs = 1;
                AMediaCodec_queueInputBuffer(codec, bufidx, 0, sampleSize, presentationTimeUs,
                        sawInputEOS ? AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM : 0);
                offset += sampleSize;
            }
        }

        if (!sawOutputEOS) {
            AMediaCodecBufferInfo info;
            ssize_t status = AMediaCodec_dequeueOutputBuffer(codec, &info, timeoutUs);
            if (status >= 0) {
                if (info.flags & AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM) {
                    LOGV("output EOS");
                    sawOutputEOS = true;
                }
                AMediaCodec_releaseOutputBuffer(codec, status, info.size != 0);
                // renderonce return.
                //break;
            } else if (status == AMEDIACODEC_INFO_OUTPUT_BUFFERS_CHANGED) {
                LOGV("output buffers changed");
            } else if (status == AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED) {
                AMediaFormat *format = NULL;
                format = AMediaCodec_getOutputFormat(codec);
                LOGV("format changed to: %s", AMediaFormat_toString(format));
                AMediaFormat_delete(format);
            } else if (status == AMEDIACODEC_INFO_TRY_AGAIN_LATER) {
                LOGV("no output buffer right now");
            } else {
                LOGV("unexpected info code: %zd", status);
                assert(false);
            }
        }
    //}

    /* TODO get those back properly. */
    len = 1;
    frameFinished = 1;
}

JNIEXPORT void JNICALL Java_cx_ring_service_RingserviceJNI_registerVideoCallback(
        JNIEnv *jenv, jclass jcls, jstring sinkId, jlong window, jint width, jint height)
{
    ANativeWindow *nativeWindow = (ANativeWindow*)((intptr_t) window);
    LOGV("registerVideoCallback");
    LOGV("width = %d", width);
    LOGV("height = %d", height);

    // Adapted from createStreamingMediaPlayer

    // TODO no constant MIMETYPE_VIDEO_MPEG4 on NDK? Get proper value from ring.
    const char *mime =  "video/mp4v-es";
    AMediaFormat *format = AMediaFormat_new();

    AMediaFormat_setString(format, AMEDIAFORMAT_KEY_MIME, mime);
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_WIDTH, width);
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_HEIGHT, height);
    // TODO KEY_CAPTURE_RATE may also be needed.
    // MediaDecoder::getFps() may contain it.
    codec = AMediaCodec_createDecoderByType(mime);

    AMediaCodec_configure(codec, format, nativeWindow, NULL, 0);
    AMediaCodec_start(codec);
    AMediaFormat_delete(format);
}


JNIEXPORT void JNICALL Java_cx_ring_service_RingserviceJNI_decodingStoppedNative() {
    LOGV("RingserviceJNI_decodingStoppedNative");
    AMediaCodec_stop(codec);
    AMediaCodec_delete(codec);
}
%}
%native(setVideoFrame) void setVideoFrame(void *, int, long);
%native(acquireNativeWindow) jlong acquireNativeWindow(jobject);
%native(releaseNativeWindow) void releaseNativeWindow(jlong);
%native(setNativeWindowGeometry) void setNativeWindowGeometry(jlong, int, int);
%native(registerVideoCallback) void registerVideoCallback(jstring, jlong, jint, jint);
%native(decodingStoppedNative) void decodingStoppedNative();


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
