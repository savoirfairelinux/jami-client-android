/*
 *  Copyright (C) 2004-2013 Savoir-Faire Linux Inc.
 *  Author: Emeric Vigier <emeric.vigier@savoirfairelinux.com>
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
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

/* File : jni_interface.i */
%module (directors="1") SFLPhoneservice

#define SWIG_JAVA_ATTACH_CURRENT_THREAD_AS_DAEMON
%include "typemaps.i"
%include "std_string.i" /* std::string typemaps */
%include "enums.swg"
%include "arrays_java.i";
%include "carrays.i";
%include "std_map.i";
%include "std_vector.i";
%include "stdint.i";

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

namespace std {
    %template(StringMap) map<string, string>;
    %template(StringVect) vector<string>;
    %template(VectMap) vector< map<string,string> >;
    %template(IntegerMap) map<string,int>;
    %template(IntVect) vector<int32_t>;
}

/* not parsed by SWIG but needed by generated C files */
%header %{

#include <logger.h>
#include <functional>

%}

/* parsed by SWIG to generate all the glue */
/* %include "../managerimpl.h" */
/* %include <client/callmanager.h> */

%include "managerimpl.i"
%include "callmanager.i"
%include "configurationmanager.i"

%inline %{
/* some functions that need to be declared in *_wrap.cpp
 * that are not declared elsewhere in the c++ code
 */
void init(ConfigurationCallback* conf_cb, Callback* call_cb) {

    using namespace std::placeholders;
    using std::bind;


    // Call event handlers
    sflph_call_ev_handlers callEvHandlers = {
        bind(&Callback::callOnStateChange, call_cb, _1, _2),
        bind(&Callback::callOnTransferFail, call_cb),
        bind(&Callback::callOnTransferSuccess, call_cb),
        bind(&Callback::callOnRecordPlaybackStopped, call_cb, _1),
        bind(&Callback::callOnVoiceMailNotify, call_cb, _1, _2),
        bind(&Callback::callOnIncomingMessage, call_cb, _1, _2, _3),
        bind(&Callback::callOnIncomingCall, call_cb, _1, _2, _3),
        bind(&Callback::callOnRecordPlaybackFilepath, call_cb, _1, _2),
        bind(&Callback::callOnConferenceCreated, call_cb, _1),
        bind(&Callback::callOnConferenceChanged, call_cb, _1, _2),
        bind(&Callback::callOnUpdatePlaybackScale, call_cb, _1, _2, _3),
        bind(&Callback::callOnConferenceRemove, call_cb, _1),
        bind(&Callback::callOnNewCall, call_cb, _1, _2, _3),
        bind(&Callback::callOnSipCallStateChange, call_cb, _1, _2, _3),
        bind(&Callback::callOnRecordStateChange, call_cb, _1, _2),
        bind(&Callback::callOnSecureSdesOn, call_cb, _1),
        bind(&Callback::callOnSecureSdesOff, call_cb, _1),
        bind(&Callback::callOnSecureZrtpOn, call_cb, _1, _2),
        bind(&Callback::callOnSecureZrtpOff, call_cb, _1),
        bind(&Callback::callOnShowSas, call_cb, _1, _2, _3),
        bind(&Callback::callOnZrtpNotSuppOther, call_cb, _1),
        bind(&Callback::callOnZrtpNegotiationFail, call_cb, _1, _2, _3),
        bind(&Callback::callOnRtcpReceiveReport, call_cb, _1, _2)
    };

    // Configuration event handlers
    sflph_config_ev_handlers configEvHandlers = {
        bind(&ConfigurationCallback::configOnVolumeChange, conf_cb, _1, _2),
        bind(&ConfigurationCallback::configOnAccountsChange, conf_cb),
        bind(&ConfigurationCallback::configOnHistoryChange, conf_cb),
        bind(&ConfigurationCallback::configOnStunStatusFail, conf_cb, _1),
        bind(&ConfigurationCallback::configOnRegistrationStateChange, conf_cb, _1, _2),
        bind(&ConfigurationCallback::configOnSipRegistrationStateChange, conf_cb, _1, _2, _3),
        bind(&ConfigurationCallback::configOnVolatileAccountsChange, conf_cb, _1, _2),
        bind(&ConfigurationCallback::configOnError, conf_cb, _1)
    };

    // All event handlers
    sflph_ev_handlers evHandlers = {};
    evHandlers.call_ev_handlers = callEvHandlers;
    evHandlers.config_ev_handlers = configEvHandlers;
    sflph_init(&evHandlers, 0);
}

%}
#ifndef SWIG
/* some bad declarations */
#endif
