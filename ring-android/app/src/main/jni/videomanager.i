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
#include <functional>
#include <list>

#include "dring/dring.h"
#include "dring/videomanager_interface.h"
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/log.h>

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

std::list<std::unique_ptr<DRing::FrameBuffer>> frameQueue {};

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

void AndroidDisplayCb(ANativeWindow *window, std::unique_ptr<DRing::FrameBuffer> frame)
{
    if (!window) {
        __android_log_print(ANDROID_LOG_WARN, "videomanager.i", "AndroidDisplayCb no window");
        return;
    }
    ANativeWindow_Buffer buffer;
    if (ANativeWindow_lock(window, &buffer, NULL) == 0) {
        if (buffer.bits && frame && frame->ptr)
            memcpy(buffer.bits, frame->ptr, frame->width * frame->height * 4);
        else
            __android_log_print(ANDROID_LOG_WARN, "videomanager.i", "Can't copy surface");
        ANativeWindow_unlockAndPost(window);
    }
    frameQueue.emplace_back(std::move(frame));
}

std::unique_ptr<DRing::FrameBuffer> sinkTargetPullCallback(ANativeWindow *window, std::size_t bytes)
{
    std::unique_ptr<DRing::FrameBuffer> ret;
    if (frameQueue.empty()) {
        ret.reset(new DRing::FrameBuffer());
    } else {
        ret = std::move(frameQueue.front());
        frameQueue.pop_front();
    }
    ret->storage.resize(bytes);
    ret->ptr = ret->storage.data();
    ret->ptrSize = bytes;
    return ret;
}

JNIEXPORT void JNICALL Java_cx_ring_service_RingserviceJNI_registerVideoCallback(JNIEnv *jenv, jclass jcls, jstring sinkId, jlong window)
{
    std::string *arg1 = 0 ;
    int *result = 0 ;

    if(!sinkId) {
        SWIG_JavaThrowException(jenv, SWIG_JavaNullPointerException, "null string");
        return 0;
    }
    const char *arg1_pstr = (const char *)jenv->GetStringUTFChars(sinkId, 0);
    if (!arg1_pstr)
        return 0;
    std::string arg1_str(arg1_pstr);
    arg1 = &arg1_str;
    jenv->ReleaseStringUTFChars(sinkId, arg1_pstr);

    ANativeWindow *nativeWindow = (ANativeWindow*)((intptr_t) window);
    auto f_display_cb = std::bind(&AndroidDisplayCb, nativeWindow, std::placeholders::_1);
    auto p_display_cb = std::bind(&sinkTargetPullCallback, nativeWindow, std::placeholders::_1);

    DRing::registerSinkTarget((std::string const &)*arg1, DRing::SinkTarget {.pull=p_display_cb, .push=f_display_cb});
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
