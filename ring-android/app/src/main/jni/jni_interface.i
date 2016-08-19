/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Emeric Vigier <emeric.vigier@savoirfairelinux.com>
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
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/* File : jni_interface.i */
%module (directors="1") Ringservice

#define SWIG_JAVA_ATTACH_CURRENT_THREAD_AS_DAEMON
%include "typemaps.i"
%include "std_string.i" /* std::string typemaps */
%include "enums.swg"
%include "arrays_java.i";
%include "carrays.i";
%include "std_map.i";
%include "std_vector.i";
%include "stdint.i";
%header %{

#include <android/log.h>

%}

/* void* shall be handled as byte arrays */
%typemap(jni) void * "void *"
%typemap(jtype) void * "byte[]"
%typemap(jstype) void * "byte[]"
%typemap(javain) void * "$javainput"
%typemap(in) void * %{
    $1 = $input;
%}
%typemap(javadirectorin) void * "$jniinput"
%typemap(out) void * %{
    $result = $1;
%}
%typemap(javaout) void * {
    return $jnicall;
}

/* Avoid uint64_t to be converted to BigInteger */
%apply int64_t { uint64_t };

namespace std {

%typemap(javacode) map<string, string> %{
  public static $javaclassname toSwig(java.util.Map<String,String> in) {
    $javaclassname n = new $javaclassname();
    for (java.util.Map.Entry<String, String> entry : in.entrySet()) {
      n.set(entry.getKey(), entry.getValue());
    }
    return n;
  }
  public java.util.HashMap<String,String> toNative() {
    java.util.HashMap<String,String> out = new java.util.HashMap<>((int)size());
    StringVect keys = keys();
    for (String s : keys)
      out.put(s, get(s));
    return out;
  }
  public java.util.HashMap<String,String> toNativeFromUtf8() {
      java.util.HashMap<String,String> out = new java.util.HashMap<>((int)size());
      StringVect keys = keys();
      for (String s : keys)
          out.put(s, getRaw(s).toJavaString());
      return out;
  }
%}
%extend map<string, string> {
    std::vector<std::string> keys() const {
        std::vector<std::string> k;
        k.reserve($self->size());
        for (const auto& i : *$self)
            k.push_back(i.first);
        return k;
    }
    void setRaw(std::string key, const vector<uint8_t>& value) {
        (*$self)[key] = std::string(value.data(), value.data()+value.size());
    }
    std::vector<uint8_t> getRaw(std::string key) {
        auto& v = $self->at(key);
        return {v.begin(), v.end()};
    }
}
%template(StringMap) map<string, string>;

%typemap(javabase) vector<string> "java.util.AbstractList<String>"
%typemap(javainterface) vector<string> "java.util.RandomAccess"
%extend vector<string> {
  value_type set(int i, const value_type& in) throw (std::out_of_range) {
    const std::string old = $self->at(i);
    $self->at(i) = in;
    return old;
  }
  bool add(const value_type& in) {
    $self->push_back(in);
    return true;
  }
  int32_t size() const {
    return $self->size();
  }
}
%template(StringVect) vector<string>;

%typemap(javacode) vector< map<string,string> > %{
  public java.util.ArrayList<java.util.Map<String, String>> toNative() {
    java.util.ArrayList<java.util.Map<String, String>> out = new java.util.ArrayList<>();
    for (int i = 0; i < size(); ++i)
      out.add(get(i).toNative());
    return out;
  }
%}
%template(VectMap) vector< map<string,string> >;
%template(IntegerMap) map<string,int>;
%template(IntVect) vector<int32_t>;
%template(UintVect) vector<uint32_t>;

%typemap(javacode) vector<uint8_t> %{
  public static Blob fromString(String in) {
    byte[] dat;
    try {
      dat = in.getBytes("UTF-8");
    } catch (java.io.UnsupportedEncodingException e) {
      dat = in.getBytes();
    }
    Blob n = new Blob(dat.length);
    for (int i=0; i<dat.length; i++)
      n.set(i, dat[i]);
    return n;
  }
  public String toJavaString() {
    byte[] dat = new byte[(int)size()];
    for (int i=0; i<dat.length; i++)
        dat[i] = (byte)get(i);
    try {
        return new String(dat, "utf-8");
    } catch (java.io.UnsupportedEncodingException e) {
        return "";
    }
  }
%}
%template(Blob) vector<uint8_t>;
%template(FloatVect) vector<float>;
}

/* not parsed by SWIG but needed by generated C files */
%header %{

#include <functional>

%}

/* parsed by SWIG to generate all the glue */
/* %include "../managerimpl.h" */
/* %include <client/callmanager.h> */

%include "managerimpl.i"
%include "callmanager.i"
%include "configurationmanager.i"
%include "videomanager.i"

#include "dring/callmanager_interface.h"

%inline %{
/* some functions that need to be declared in *_wrap.cpp
 * that are not declared elsewhere in the c++ code
 */

void init(ConfigurationCallback* confM, Callback* callM, VideoCallback* videoM) {
    using namespace std::placeholders;

    using std::bind;
    using DRing::exportable_callback;
    using DRing::CallSignal;
    using DRing::ConfigurationSignal;
    using DRing::VideoSignal;

    using SharedCallback = std::shared_ptr<DRing::CallbackWrapperBase>;

    // Call event handlers
    const std::map<std::string, SharedCallback> callEvHandlers = {
        exportable_callback<CallSignal::StateChange>(bind(&Callback::callStateChanged, callM, _1, _2, _3)),
        exportable_callback<CallSignal::TransferFailed>(bind(&Callback::transferFailed, callM)),
        exportable_callback<CallSignal::TransferSucceeded>(bind(&Callback::transferSucceeded, callM)),
        exportable_callback<CallSignal::RecordPlaybackStopped>(bind(&Callback::recordPlaybackStopped, callM, _1)),
        exportable_callback<CallSignal::VoiceMailNotify>(bind(&Callback::voiceMailNotify, callM, _1, _2)),
        exportable_callback<CallSignal::IncomingMessage>(bind(&Callback::incomingMessage, callM, _1, _2, _3)),
        exportable_callback<CallSignal::IncomingCall>(bind(&Callback::incomingCall, callM, _1, _2, _3)),
        exportable_callback<CallSignal::RecordPlaybackFilepath>(bind(&Callback::recordPlaybackFilepath, callM, _1, _2)),
        exportable_callback<CallSignal::ConferenceCreated>(bind(&Callback::conferenceCreated, callM, _1)),
        exportable_callback<CallSignal::ConferenceChanged>(bind(&Callback::conferenceChanged, callM, _1, _2)),
        exportable_callback<CallSignal::UpdatePlaybackScale>(bind(&Callback::updatePlaybackScale, callM, _1, _2, _3)),
        exportable_callback<CallSignal::ConferenceRemoved>(bind(&Callback::conferenceRemoved, callM, _1)),
        exportable_callback<CallSignal::NewCallCreated>(bind(&Callback::newCallCreated, callM, _1, _2, _3)),
        exportable_callback<CallSignal::RecordingStateChanged>(bind(&Callback::recordingStateChanged, callM, _1, _2)),
        exportable_callback<CallSignal::RtcpReportReceived>(bind(&Callback::onRtcpReportReceived, callM, _1, _2)),
        exportable_callback<CallSignal::PeerHold>(bind(&Callback::peerHold, callM, _1, _2))
    };

    // Configuration event handlers
    const std::map<std::string, SharedCallback> configEvHandlers = {
        exportable_callback<ConfigurationSignal::VolumeChanged>(bind(&ConfigurationCallback::volumeChanged, confM, _1, _2)),
        exportable_callback<ConfigurationSignal::AccountsChanged>(bind(&ConfigurationCallback::accountsChanged, confM)),
        exportable_callback<ConfigurationSignal::StunStatusFailed>(bind(&ConfigurationCallback::stunStatusFailure, confM, _1)),
        exportable_callback<ConfigurationSignal::RegistrationStateChanged>(bind(&ConfigurationCallback::registrationStateChanged, confM, _1, _2, _3, _4)),
        exportable_callback<ConfigurationSignal::VolatileDetailsChanged>(bind(&ConfigurationCallback::volatileAccountDetailsChanged, confM, _1, _2)),
        exportable_callback<ConfigurationSignal::Error>(bind(&ConfigurationCallback::errorAlert, confM, _1)),
        exportable_callback<ConfigurationSignal::IncomingAccountMessage>(bind(&ConfigurationCallback::incomingAccountMessage, confM, _1, _2, _3 )),
        exportable_callback<ConfigurationSignal::AccountMessageStatusChanged>(bind(&ConfigurationCallback::accountMessageStatusChanged, confM, _1, _2, _3, _4 )),
        exportable_callback<ConfigurationSignal::IncomingTrustRequest>(bind(&ConfigurationCallback::incomingTrustRequest, confM, _1, _2, _3, _4 )),
        exportable_callback<ConfigurationSignal::CertificatePinned>(bind(&ConfigurationCallback::certificatePinned, confM, _1 )),
        exportable_callback<ConfigurationSignal::CertificatePathPinned>(bind(&ConfigurationCallback::certificatePathPinned, confM, _1, _2 )),
        exportable_callback<ConfigurationSignal::CertificateExpired>(bind(&ConfigurationCallback::certificateExpired, confM, _1 )),
        exportable_callback<ConfigurationSignal::CertificateStateChanged>(bind(&ConfigurationCallback::certificateStateChanged, confM, _1, _2, _3 )),
        exportable_callback<ConfigurationSignal::GetHardwareAudioFormat>(bind(&ConfigurationCallback::getHardwareAudioFormat, confM, _1 )),
        exportable_callback<ConfigurationSignal::GetAppDataPath>(bind(&ConfigurationCallback::getAppDataPath, confM, _1, _2 ))
    };

/*
    // Presence event handlers
    const std::map<std::string, SharedCallback> presEvHandlers = {
        exportable_callback<PresenceSignal::NewServerSubscriptionRequest>(bind(&DBusPresenceManager::newServerSubscriptionRequest, presM, _1)),
        exportable_callback<PresenceSignal::ServerError>(bind(&DBusPresenceManager::serverError, presM, _1, _2, _3)),
        exportable_callback<PresenceSignal::NewBuddyNotification>(bind(&DBusPresenceManager::newBuddyNotification, presM, _1, _2, _3, _4)),
        exportable_callback<PresenceSignal::SubscriptionStateChanged>(bind(&DBusPresenceManager::subscriptionStateChanged, presM, _1, _2, _3)),
    };

#ifdef RING_VIDEO
    // Video event handlers
    const std::map<std::string, SharedCallback> videoEvHandlers = {
        exportable_callback<VideoSignal::DeviceEvent>(bind(&DBusVideoManager::deviceEvent, videoM)),
        exportable_callback<VideoSignal::DecodingStarted>(bind(&DBusVideoManager::startedDecoding, videoM, _1, _2, _3, _4, _5)),
        exportable_callback<VideoSignal::DecodingStopped>(bind(&DBusVideoManager::stoppedDecoding, videoM, _1, _2, _3)),
    };
#endif
*/

    const std::map<std::string, SharedCallback> videoEvHandlers = {
        exportable_callback<VideoSignal::GetCameraInfo>(bind(&VideoCallback::getCameraInfo, videoM, _1, _2, _3, _4)),
        exportable_callback<VideoSignal::SetParameters>(bind(&VideoCallback::setParameters, videoM, _1, _2, _3, _4, _5)),
        exportable_callback<VideoSignal::StartCapture>(bind(&VideoCallback::startCapture, videoM, _1)),
        exportable_callback<VideoSignal::StopCapture>(bind(&VideoCallback::stopCapture, videoM)),
        exportable_callback<VideoSignal::DecodingStarted>(bind(&VideoCallback::decodingStarted, videoM, _1, _2, _3, _4, _5)),
        exportable_callback<VideoSignal::DecodingStopped>(bind(&VideoCallback::decodingStopped, videoM, _1, _2, _3)),
    };

    if (!DRing::init(static_cast<DRing::InitFlag>(DRing::DRING_FLAG_DEBUG)))
        return -1;

    registerCallHandlers(callEvHandlers);
    registerConfHandlers(configEvHandlers);
/*    registerPresHandlers(presEvHandlers); */
    registerVideoHandlers(videoEvHandlers);

    DRing::start();
}


%}
#ifndef SWIG
/* some bad declarations */
#endif
