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

#include "client/callmanager.h"


typedef struct callmanager_callback
{
    void (*on_new_call_created)(const std::string& accountID,
                                const std::string& callID,
                                const std::string& to);

    void (*on_call_state_changed)(const std::string& callID,
                                  const std::string& state);

    void (*on_incoming_call)(const std::string& accountID,
                             const std::string& callID,
                             const std::string& from);

    void (*on_transfer_state_changed) (const std::string& result);

    void (*on_conference_created) (const std::string& confID);

    void (*on_conference_removed) (const std::string& confID);

    void (*on_conference_state_changed) (const std::string& confID,
                                          const std::string& state);

    void (*on_incoming_message) (const std::string& ID,
                                    const std::string& from,
                                    const std::string& msg);

    void (*on_record_playback_filepath) (const std::string& id,
                                         const std::string& filename);

    void (*on_recording_state_changed) (const std::string& callID,
                                        const bool& state);

    void (*newPresSubClientNotification) (const std::string& uri,
											const std::string& basic,
											const std::string& note);

	void (*newPresSubServerRequest) (const std::string& remote);

    void (*on_secure_sdes_on) (const std::string& callID);

    void (*on_secure_sdes_off) (const std::string& callID);

    void (*on_secure_zrtp_on) (const std::string& callID,
                                const std::string& cipher);

    void (*on_secure_zrtp_off) (const std::string& callID);

    void (*on_show_sas) (const std::string& callID,
                        const std::string& sas,
                        const bool& verified);

    void (*on_zrtp_not_supported) (const std::string& callID);

    void (*on_zrtp_negociation_failed) (const std::string& callID,
                                                const std::string& reason,
                                                const std::string& severity);

    void (*on_rtcp_report_received) (const std::string& callID,
                                    const std::map<std::string, int>& stats);

} callmanager_callback_t;


class Callback {
public:
    virtual ~Callback() {}

    virtual void on_new_call_created(const std::string& arg1,
                                     const std::string& arg2,
                                     const std::string& arg3) {}

    virtual void on_call_state_changed(const std::string& arg1,
                                       const std::string& arg2) {}

    virtual void on_incoming_call(const std::string& arg1,
                                  const std::string& arg2,
                                  const std::string& arg3) {}

    virtual void on_transfer_state_changed (const std::string& arg1) {}

    virtual void on_conference_created (const std::string& arg1) {}

    virtual void on_conference_removed (const std::string& arg1) {}

    virtual void on_conference_state_changed (const std::string& arg1,
                                            const std::string& arg2) {}

    virtual void on_incoming_message(const std::string& ID,
                                    const std::string& from,
                                    const std::string& msg) {}

    virtual void on_record_playback_filepath(const std::string& id,
                                              const std::string& filename) {}

    virtual void on_recording_state_changed(const std::string& callID,
                                        const bool& state) {}

    virtual void newPresSubClientNotification(const std::string& uri,
                                                const std::string& basic,
                                                const std::string& note) {}

    virtual void newPresSubServerRequest(const std::string& remote) {}

    virtual void on_secure_sdes_on(const std::string& callID) {}

    virtual void on_secure_sdes_off(const std::string& callID) {}

    virtual void on_secure_zrtp_on(const std::string& callID,
                                const std::string& cipher) {}

    virtual void on_secure_zrtp_off(const std::string& callID) {}

    virtual void on_show_sas(const std::string& callID,
                        const std::string& sas,
                        const bool& verified) {}

    virtual void on_zrtp_not_supported(const std::string& callID) {}

    virtual void on_zrtp_negociation_failed(const std::string& callID,
                                                const std::string& reason,
                                                const std::string& severity) {}

    virtual void on_rtcp_report_received (const std::string& callID,
                                    const std::map<std::string, int>& stats) {}
};


static Callback* registeredCallbackObject = NULL;

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

static struct callmanager_callback wrapper_callback_struct = {
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

void setCallbackObject(Callback* callback) {
    registeredCallbackObject = callback;
}

%}

%feature("director") Callback;

class CallManager {
public:
    bool placeCall(const std::string& accountID, const std::string& callID, const std::string& to);

    bool refuse(const std::string& callID);
    bool accept(const std::string& callID);
    bool hangUp(const std::string& callID);
    bool hold(const std::string& callID);
    bool unhold(const std::string& callID);
    bool transfer(const std::string& callID, const std::string& to);
    bool attendedTransfer(const std::string& transferID, const std::string& targetID);
    std::map< std::string, std::string > getCallDetails(const std::string& callID);
    std::vector< std::string > getCallList();

    /* Conference related methods */
    void removeConference(const std::string& conference_id);
    bool joinParticipant(const std::string& sel_callID, const std::string& drag_callID);
    void createConfFromParticipantList(const std::vector< std::string >& participants);
    bool isConferenceParticipant(const std::string& call_id);
    bool addParticipant(const std::string& callID, const std::string& confID);
    bool addMainParticipant(const std::string& confID);
    bool detachParticipant(const std::string& callID);
    bool joinConference(const std::string& sel_confID, const std::string& drag_confID);
    bool hangUpConference(const std::string& confID);
    bool holdConference(const std::string& confID);
    bool unholdConference(const std::string& confID);
    std::vector<std::string> getConferenceList();
    std::vector<std::string> getParticipantList(const std::string& confID);
    std::string getConferenceId(const std::string& callID);
    std::map<std::string, std::string> getConferenceDetails(const std::string& callID);

    /* File Playback methods */
    bool startRecordedFilePlayback(const std::string& filepath);
    void stopRecordedFilePlayback(const std::string& filepath);

    /* General audio methods */
    bool toggleRecording(const std::string& callID);
    void recordPlaybackSeek(const double& value);
    bool getIsRecording(const std::string& callID);
    std::string getCurrentAudioCodecName(const std::string& callID);
    void playDTMF(const std::string& key);
    void startTone(const int32_t& start, const int32_t& type);

    /* Security related methods */
    void setSASVerified(const std::string& callID);
    void resetSASVerified(const std::string& callID);
    void setConfirmGoClear(const std::string& callID);
    void requestGoClear(const std::string& callID);
    void acceptEnrollment(const std::string& callID, const bool& accepted);

    /* Instant messaging */
    void sendTextMessage(const std::string& callID, const std::string& message);
};

class Callback {
public:
    virtual ~Callback();

    virtual void on_new_call_created(const std::string& arg1,
                                     const std::string& arg2,
                                     const std::string& arg3);

    virtual void on_call_state_changed(const std::string& arg1,
                                       const std::string& arg2);

    virtual void on_incoming_call(const std::string& arg1,
                                  const std::string& arg2,
                                  const std::string& arg3);

    virtual void on_transfer_state_changed(const std::string& arg1);

    virtual void on_conference_created(const std::string& arg1);

    virtual void on_conference_removed(const std::string& arg1);

    virtual void on_conference_state_changed(const std::string& arg1,
                                              const std::string& arg2);

    virtual void on_incoming_message(const std::string& ID,
                                    const std::string& from,
                                    const std::string& msg);

    virtual void on_record_playback_filepath(const std::string& id,
                                            const std::string& filename);

    virtual void on_recording_state_changed(const std::string& callID,
                                            const bool& state);

    virtual void newPresSubClientNotification(const std::string& uri,
                                            const std::string& basic,
                                            const std::string& note);

    virtual void newPresSubServerRequest(const std::string& remote);

    virtual void on_secure_sdes_on(const std::string& callID);

    virtual void on_secure_sdes_off(const std::string& callID);

    virtual void on_secure_zrtp_on(const std::string& callID,
                                const std::string& cipher);

    virtual void on_secure_zrtp_off(const std::string& callID);

    virtual void on_show_sas(const std::string& callID,
                        const std::string& sas,
                        const bool& verified);

    virtual void on_zrtp_not_supported(const std::string& callID);

    virtual void on_zrtp_negociation_failed(const std::string& callID,
                                                const std::string& reason,
                                                const std::string& severity);

    virtual void on_rtcp_report_received (const std::string& callID,
                                    const std::map<std::string, int>& stats);
};

static Callback* registeredCallbackObject = NULL;

void setCallbackObject(Callback* callback) {
    registeredCallbackObject = callback;
}
