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
    static void on_new_call_created(const std::string& arg1,
                                     const std::string& arg2,
                                     const std::string& arg3);

    static void on_call_state_changed(const std::string& arg1,
                                       const std::string& arg2);

    static void on_incoming_call(const std::string& arg1,
                                  const std::string& arg2,
                                  const std::string& arg3);

    static void on_transfer_state_changed(const std::string& arg1);

    static void on_conference_created(const std::string& arg1);

    static void on_conference_removed(const std::string& arg1);

    static void on_conference_state_changed(const std::string& arg1,
                                              const std::string& arg2);

    static void on_incoming_message(const std::string& ID,
                                    const std::string& from,
                                    const std::string& msg);

    static void on_record_playback_filepath(const std::string& id,
                                            const std::string& filename);

    static void on_recording_state_changed(const std::string& callID,
                                            const bool& state);

    static void newPresSubClientNotification(const std::string& uri,
                                            const std::string& basic,
                                            const std::string& note);

    static void newPresSubServerRequest(const std::string& remote);

    static void on_secure_sdes_on(const std::string& callID);

    static void on_secure_sdes_off(const std::string& callID);

    static void on_secure_zrtp_on(const std::string& callID,
                                const std::string& cipher);

    static void on_secure_zrtp_off(const std::string& callID);

    static void on_show_sas(const std::string& callID,
                        const std::string& sas,
                        const bool& verified);

    static void on_zrtp_not_supported(const std::string& callID);

    static void on_zrtp_negociation_failed(const std::string& callID,
                                                const std::string& reason,
                                                const std::string& severity);

    static void on_rtcp_report_received (const std::string& callID,
                                    const std::map<std::string, int>& stats);

};

static CallManagerCallback* registeredCallbackObject = NULL;

void setCallbackObject(CallManagerCallback* callback) {
    registeredCallbackObject = callback;
}

void on_new_call_created_wrapper (const std::string& accountID,
                                  const std::string& callID,
                                  const std::string& to) {
    registeredCallbackObject->on_new_call_created(accountID, callID, to);
}

void on_call_state_changed_wrapper(const std::string& callID,
                           const std::string& state) {
    registeredCallbackObject->on_call_state_changed(callID, state);
}

void on_incoming_call_wrapper (const std::string& accountID,
                               const std::string& callID,
                               const std::string& from) {
    registeredCallbackObject->on_incoming_call(accountID, callID, from);
}

void on_transfer_state_changed_wrapper (const std::string& result) {
    registeredCallbackObject->on_transfer_state_changed(result);
}

void on_conference_created_wrapper (const std::string& confID) {
    registeredCallbackObject->on_conference_created(confID);
}

void on_conference_removed_wrapper (const std::string& confID) {
    registeredCallbackObject->on_conference_removed(confID);
}

void on_conference_state_changed_wrapper (const std::string& confID,
                                          const std::string& state) {
    registeredCallbackObject->on_conference_state_changed(confID, state);
}

void on_incoming_message_wrapper(const std::string& ID, const std::string& from, const std::string& msg) {
    registeredCallbackObject->on_incoming_message(ID, from, msg);
}

void on_record_playback_filepath_wrapper(const std::string& id, const std::string& filename) {
    registeredCallbackObject->on_record_playback_filepath(id, filename);
}

void on_recording_state_changed_wrapper(const std::string& callID, const bool& state) {
    registeredCallbackObject->on_recording_state_changed(callID, state);
}

void on_newPresSubClientNotification_wrapper(const std::string& uri, const std::string& basic, const std::string& note) {
    registeredCallbackObject->newPresSubClientNotification(uri, basic, note);
}

void on_newPresSubServerRequest_wrapper(const std::string& remote) {
    registeredCallbackObject->newPresSubServerRequest(remote);
}

void on_secure_sdes_on_wrapper(const std::string& callID){
    registeredCallbackObject->on_secure_sdes_on(callID);
}

void on_secure_sdes_off_wrapper(const std::string& callID) {
    registeredCallbackObject->on_secure_sdes_off(callID);
}

void on_secure_zrtp_on_wrapper(const std::string& callID,const std::string& cipher){
    registeredCallbackObject->on_secure_zrtp_on(callID, cipher);
}

void on_secure_zrtp_off_wrapper(const std::string& callID){
    registeredCallbackObject->on_secure_zrtp_off(callID);
}

void on_show_sas_wrapper(const std::string& callID, const std::string& sas, const bool& verified){
    registeredCallbackObject->on_show_sas(callID, sas, verified);
}

void on_zrtp_not_supported_wrapper(const std::string& callID){
    registeredCallbackObject->on_zrtp_not_supported(callID);
}

void on_zrtp_negociation_failed_wrapper(const std::string& callID, const std::string& reason, const std::string& severity){
    registeredCallbackObject->on_zrtp_negociation_failed(callID, reason, severity);
}

void on_rtcp_report_received_wrapper (const std::string& callID, const std::map<std::string, int>& stats){
    registeredCallbackObject->on_rtcp_report_received(callID, stats);
}

static struct sflph_call_ev_handlers wrapper_callback_struct = {
    &on_new_call_created_wrapper,
    &on_call_state_changed_wrapper,
    &on_incoming_call_wrapper,
    &on_transfer_state_changed_wrapper,
    &on_conference_created_wrapper,
    &on_conference_removed_wrapper,
    &on_conference_state_changed_wrapper,
    &on_incoming_message_wrapper,
    &on_record_playback_filepath_wrapper,
    &on_recording_state_changed_wrapper,
    &on_newPresSubClientNotification_wrapper,
    &on_newPresSubServerRequest_wrapper,
    &on_secure_sdes_on_wrapper,
    &on_secure_sdes_off_wrapper,
    &on_secure_zrtp_on_wrapper,
    &on_secure_zrtp_off_wrapper,
    &on_show_sas_wrapper,
    &on_zrtp_not_supported_wrapper,
    &on_zrtp_negociation_failed_wrapper,
    &on_rtcp_report_received_wrapper
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

