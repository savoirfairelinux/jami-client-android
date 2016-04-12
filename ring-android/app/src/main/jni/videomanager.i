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
#include <mutex>

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

std::map<ANativeWindow*, std::unique_ptr<DRing::FrameBuffer>> windows {};
std::mutex windows_mutex;

std::vector<uint8_t> workspace;

void rotateNV21(std::vector<uint8_t>& input, unsigned width, unsigned height, int rotation, uint8_t* output)
{
    if (rotation == 0) {
        std::copy_n(input.begin(), input.size(), output);
        return;
    }
    if (rotation % 90 != 0 || rotation < 0 || rotation > 270) {
        __android_log_print(ANDROID_LOG_ERROR, "videomanager.i", "%u %u %d", width, height, rotation);
        return;
    }
    unsigned frameSize = width * height;
    bool swap      = rotation % 180 != 0;
    bool xflip     = rotation % 270 != 0;
    bool yflip     = rotation >= 180;
    unsigned wOut  = swap ? height : width;
    unsigned hOut  = swap ? width  : height;

    for (unsigned j = 0; j < height; j++) {
        for (unsigned i = 0; i < width; i++) {
            unsigned yIn = j * width + i;
            unsigned uIn = frameSize + (j >> 1) * width + (i & ~1);
            unsigned vIn = uIn + 1;
            unsigned iSwapped = swap ? j : i;
            unsigned jSwapped = swap ? i : j;
            unsigned iOut     = xflip ? wOut - iSwapped - 1 : iSwapped;
            unsigned jOut     = yflip ? hOut - jSwapped - 1 : jSwapped;
            unsigned yOut = jOut * wOut + iOut;
            unsigned uOut = frameSize + (jOut >> 1) * wOut + (iOut & ~1);
            unsigned vOut = uOut + 1;
            output[yOut] = input[yIn];
            output[uOut] = input[uIn];
            output[vOut] = input[vIn];
        }
    }
    return output;
}

JNIEXPORT void JNICALL Java_cx_ring_service_RingserviceJNI_setVideoFrame(JNIEnv *jenv, jclass jcls, void* frame, int frame_size, jlong target, int w, int h, int rotation)
{
    uint8_t* f_target = (uint8_t*) ((intptr_t) target);
    if (rotation == 0)
         jenv->GetByteArrayRegion(frame, 0, frame_size, f_target);
    else {
        workspace.resize(frame_size);
        jenv->GetByteArrayRegion(frame, 0, frame_size, workspace.data());
        rotateNV21(workspace, w, h, rotation, f_target);
    }
}

JNIEXPORT jlong JNICALL Java_cx_ring_service_RingserviceJNI_acquireNativeWindow(JNIEnv *jenv, jclass jcls, jobject javaSurface)
{
    return (jlong)(intptr_t)ANativeWindow_fromSurface(jenv, javaSurface);
}

JNIEXPORT void JNICALL Java_cx_ring_service_RingserviceJNI_releaseNativeWindow(JNIEnv *jenv, jclass jcls, jlong window_)
{
    std::lock_guard<std::mutex> guard(windows_mutex);
    ANativeWindow *window = (ANativeWindow*)((intptr_t) window_);
    ANativeWindow_release(window);
}

JNIEXPORT void JNICALL Java_cx_ring_service_RingserviceJNI_setNativeWindowGeometry(JNIEnv *jenv, jclass jcls, jlong window_, int width, int height)
{
    ANativeWindow *window = (ANativeWindow*)((intptr_t) window_);
    ANativeWindow_setBuffersGeometry(window, width, height, WINDOW_FORMAT_RGBA_8888);
}

void AndroidDisplayCb(ANativeWindow *window, std::unique_ptr<DRing::FrameBuffer> frame)
{
    std::lock_guard<std::mutex> guard(windows_mutex);
    try {
        auto& i = windows.at(window);
        ANativeWindow_Buffer buffer;
        if (ANativeWindow_lock(window, &buffer, NULL) == 0) {
            if (buffer.bits && frame && frame->ptr) {
                if (buffer.stride == frame->width)
                    memcpy(buffer.bits, frame->ptr, frame->width * frame->height * 4);
                else {
                    size_t line_size_in = frame->width * 4;
                    size_t line_size_out = buffer.stride * 4;
                    for (size_t i=0, n=frame->height; i<n; i++)
                        memcpy(buffer.bits + line_size_out * i, frame->ptr + line_size_in * i, line_size_in);
                }
            }
            else
                __android_log_print(ANDROID_LOG_WARN, "videomanager.i", "Can't copy surface");
            ANativeWindow_unlockAndPost(window);
        } else {
            __android_log_print(ANDROID_LOG_WARN, "videomanager.i", "Can't lock surface");
        }
        i = std::move(frame);
    } catch (...) {
        __android_log_print(ANDROID_LOG_WARN, "videomanager.i", "Can't copy frame: no window");
    }
}

std::unique_ptr<DRing::FrameBuffer> sinkTargetPullCallback(ANativeWindow *window, std::size_t bytes)
{
    try {
        std::unique_ptr<DRing::FrameBuffer> ret;
        {
            std::lock_guard<std::mutex> guard(windows_mutex);
            ret = std::move(windows.at(window));
        }
        if (not ret) {
            __android_log_print(ANDROID_LOG_WARN, "videomanager.i", "Creating new video buffer of %zu kib", bytes/1024);
            ret.reset(new DRing::FrameBuffer());
        }
        ret->storage.resize(bytes);
        ret->ptr = ret->storage.data();
        ret->ptrSize = bytes;
        return ret;
    } catch (...) {
        return {};
    }
}

JNIEXPORT void JNICALL Java_cx_ring_service_RingserviceJNI_registerVideoCallback(JNIEnv *jenv, jclass jcls, jstring sinkId, jlong window)
{
    if(!sinkId) {
        SWIG_JavaThrowException(jenv, SWIG_JavaNullPointerException, "null string");
        return 0;
    }
    const char *arg1_pstr = (const char *)jenv->GetStringUTFChars(sinkId, 0);
    if (!arg1_pstr)
        return 0;
    const std::string sink(arg1_pstr);
    jenv->ReleaseStringUTFChars(sinkId, arg1_pstr);

    ANativeWindow* nativeWindow = (ANativeWindow*)((intptr_t) window);
    auto f_display_cb = std::bind(&AndroidDisplayCb, nativeWindow, std::placeholders::_1);
    auto p_display_cb = std::bind(&sinkTargetPullCallback, nativeWindow, std::placeholders::_1);

    std::lock_guard<std::mutex> guard(windows_mutex);
    windows.emplace(nativeWindow, nullptr);
    DRing::registerSinkTarget(sink, DRing::SinkTarget {.pull=p_display_cb, .push=f_display_cb});
}

JNIEXPORT void JNICALL Java_cx_ring_service_RingserviceJNI_unregisterVideoCallback(JNIEnv *jenv, jclass jcls, jstring sinkId, jlong window)
{
    if(!sinkId) {
        SWIG_JavaThrowException(jenv, SWIG_JavaNullPointerException, "null string");
        return 0;
    }
    const char *arg1_pstr = (const char *)jenv->GetStringUTFChars(sinkId, 0);
    if (!arg1_pstr)
        return 0;
    const std::string sink(arg1_pstr);
    jenv->ReleaseStringUTFChars(sinkId, arg1_pstr);

    std::lock_guard<std::mutex> guard(windows_mutex);
    DRing::registerSinkTarget(sink, DRing::SinkTarget {});
    windows.erase(window);
}

%}
%native(setVideoFrame) void setVideoFrame(void*, int, jlong, int, int, int);
%native(acquireNativeWindow) jlong acquireNativeWindow(jobject);
%native(releaseNativeWindow) void releaseNativeWindow(jlong);
%native(setNativeWindowGeometry) void setNativeWindowGeometry(jlong, int, int);
%native(registerVideoCallback) void registerVideoCallback(jstring, jlong);
%native(unregisterVideoCallback) void unregisterVideoCallback(jstring, jlong);


namespace DRing {

void setDefaultDevice(const std::string& name);
std::string getDefaultDevice();

void startCamera();
void stopCamera();
bool hasCameraStarted();
bool switchInput(const std::string& resource);
bool switchToCamera();
std::map<std::string, std::string> getSettings(const std::string& name);
void applySettings(const std::string& name, const std::map<std::string, std::string>& settings);

void addVideoDevice(const std::string &node);
void removeVideoDevice(const std::string &node);
uintptr_t obtainFrame(int length);
void releaseFrame(uintptr_t frame);
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
