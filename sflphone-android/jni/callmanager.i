/*
 *  Copyright (C) 2004-2013 Savoir-Faire Linux Inc.
 *  Author: Emeric Vigier <emeric.vigier@savoirfairelinux.com>
 *          Alexandre Lision <alexnadre.L@savoirfairelinux.com>
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

%header %{

#include "sflphone.h"

class CallManagerCallback {
public:
    virtual ~CallManagerCallback() {}
    static void callOnStateChange(const std::string& call_id, const std::string& state){}
    static void callOnTransferFail(void){}
    static void callOnTransferSuccess(void){}
    static void callOnRecordPlaybackStopped(const std::string& path){}
    static void callOnVoiceMailNotify(const std::string& call_id, int nd_msg){}
    static void callOnIncomingMessage(const std::string& id, const std::string& from, const std::string& msg){}
    static void callOnIncomingCall(const std::string& account_id, const std::string& call_id, const std::string& from){}
    static void callOnRecordPlaybackFilepath(const std::string& id, const std::string& filename){}
    static void callOnConferenceCreated(const std::string& conf_id){}
    static void callOnConferenceChanged(const std::string& conf_id, const std::string& state){}
    static void callOnUpdatePlaybackScale(const std::string& filepath, int position, int scale){}
    static void callOnConferenceRemove(const std::string& conf_id){}
    static void callOnNewCall(const std::string& account_id, const std::string& call_id, const std::string& to){}
    static void callOnSipCallStateChange(const std::string& call_id, const std::string& state, int code){}
    static void callOnRecordStateChange(const std::string& call_id, int state){}
    static void callOnSecureSdesOn(const std::string& call_id){}
    static void callOnSecureSdesOff(const std::string& call_id){}
    static void callOnSecureZrtpOn(const std::string& call_id, const std::string& cipher){}
    static void callOnSecureZrtpOff(const std::string& call_id){}
    static void callOnShowSas(const std::string& call_id, const std::string& sas, int verified){}
    static void callOnZrtpNotSuppOther(const std::string& call_id){}
    static void callOnZrtpNegotiationFail(const std::string& call_id, const std::string& reason, const std::string& severity){}
    static void callOnRtcpReceiveReport(const std::string& call_id, const std::map<std::string, int>& stats){}
};


%}

%feature("director") CallManagerCallback;

bool sflph_call_place(const std::string& account_id, const std::string& call_id, const std::string& to);
bool sflph_call_refuse(const std::string& call_id);
bool sflph_call_accept(const std::string& call_id);
bool sflph_call_hang_up(const std::string& call_id);
bool sflph_call_hold(const std::string& call_id);
bool sflph_call_unhold(const std::string& call_id);
bool sflph_call_transfer(const std::string& call_id, const std::string& to);
bool sflph_call_attended_transfer(const std::string& transfer_id, const std::string& target_id);
std::map<std::string, std::string> sflph_call_get_call_details(const std::string& call_id);
std::vector<std::string> sflph_call_get_call_list(void);
void sflph_call_remove_conference(const std::string& conf_id);
bool sflph_call_join_participant(const std::string& sel_call_id, const std::string& drag_call_id);
void sflph_call_create_conf_from_participant_list(const std::vector<std::string>& participants);
bool sflph_call_is_conference_participant(const std::string& call_id);
bool sflph_call_add_participant(const std::string& call_id, const std::string& conf_id);
bool sflph_call_add_main_participant(const std::string& conf_id);
bool sflph_call_detach_participant(const std::string& call_id);
bool sflph_call_join_conference(const std::string& sel_conf_id, const std::string& drag_conf_id);
bool sflph_call_hang_up_conference(const std::string& conf_id);
bool sflph_call_hold_conference(const std::string& conf_id);
bool sflph_call_unhold_conference(const std::string& conf_id);
std::vector<std::string> sflph_call_get_conference_list(void);
std::vector<std::string> sflph_call_get_participant_list(const std::string& conf_id);
std::vector<std::string> sflph_call_get_display_names(const std::string& conf_id);
std::string sflph_call_get_conference_id(const std::string& call_id);
std::map<std::string, std::string> sflph_call_get_conference_details(const std::string& call_id);
bool sflph_call_play_recorded_file(const std::string& path);
void sflph_call_stop_recorded_file(const std::string& path);
bool sflph_call_toggle_recording(const std::string& call_id);
void sflph_call_set_recording(const std::string& call_id);
void sflph_call_record_playback_seek(double pos);
bool sflph_call_is_recording(const std::string& call_id);
std::string sflph_call_get_current_audio_codec_name(const std::string& call_id);
void sflph_call_play_dtmf(const std::string& key);
void sflph_call_start_tone(int start, int type);
void sflph_call_set_sas_verified(const std::string& call_id);
void sflph_call_reset_sas_verified(const std::string& call_id);
void sflph_call_set_confirm_go_clear(const std::string& call_id);
void sflph_call_request_go_clear(const std::string& call_id);
void sflph_call_accept_enrollment(const std::string& call_id, bool accepted);
void sflph_call_send_text_message(const std::string& call_id, const std::string& message);

class CallManagerCallback {
public:
    virtual ~CallManagerCallback();
static void callOnStateChange(const std::string& call_id, const std::string& state);
        static void callOnTransferFail(void);
        static void callOnTransferSuccess(void);
        static void callOnRecordPlaybackStopped(const std::string& path);
        static void callOnVoiceMailNotify(const std::string& call_id, int nd_msg);
        static void callOnIncomingMessage(const std::string& id, const std::string& from, const std::string& msg);
        static void callOnIncomingCall(const std::string& account_id, const std::string& call_id, const std::string& from);
        static void callOnRecordPlaybackFilepath(const std::string& id, const std::string& filename);
        static void callOnConferenceCreated(const std::string& conf_id);
        static void callOnConferenceChanged(const std::string& conf_id, const std::string& state);
        static void callOnUpdatePlaybackScale(const std::string& filepath, int position, int scale);
        static void callOnConferenceRemove(const std::string& conf_id);
        static void callOnNewCall(const std::string& account_id, const std::string& call_id, const std::string& to);
        static void callOnSipCallStateChange(const std::string& call_id, const std::string& state, int code);
        static void callOnRecordStateChange(const std::string& call_id, int state);
        static void callOnSecureSdesOn(const std::string& call_id);
        static void callOnSecureSdesOff(const std::string& call_id);
        static void callOnSecureZrtpOn(const std::string& call_id, const std::string& cipher);
        static void callOnSecureZrtpOff(const std::string& call_id);
        static void callOnShowSas(const std::string& call_id, const std::string& sas, int verified);
        static void callOnZrtpNotSuppOther(const std::string& call_id);
        static void callOnZrtpNegotiationFail(const std::string& call_id, const std::string& reason, const std::string& severity);
        static void callOnRtcpReceiveReport(const std::string& call_id, const std::map<std::string, int>& stats);
};
