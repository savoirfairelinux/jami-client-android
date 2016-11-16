/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Emeric Vigier <emeric.vigier@savoirfairelinux.com>
 *          Alexandre Lision <alexnadre.L@savoirfairelinux.com>
 *          Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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

%header %{

#include "dring/dring.h"
#include "dring/callmanager_interface.h"

class Callback {
public:
    virtual ~Callback() {}
    virtual void callStateChanged(const std::string& call_id, const std::string& state, int detail_code){}
    virtual void transferFailed(void){}
    virtual void transferSucceeded(void){}
    virtual void recordPlaybackStopped(const std::string& path){}
    virtual void voiceMailNotify(const std::string& call_id, int nd_msg){}
    virtual void incomingMessage(const std::string& id, const std::string& from, const std::map<std::string, std::string>& messages){}
    virtual void incomingCall(const std::string& account_id, const std::string& call_id, const std::string& from){}
    virtual void recordPlaybackFilepath(const std::string& id, const std::string& filename){}
    virtual void conferenceCreated(const std::string& conf_id){}
    virtual void conferenceChanged(const std::string& conf_id, const std::string& state){}
    virtual void conferenceRemoved(const std::string& conf_id){}
    virtual void newCallCreated(const std::string& call_id, const std::string&, const std::string&){}
    virtual void updatePlaybackScale(const std::string& filepath, int position, int scale){}
    virtual void conferenceRemove(const std::string& conf_id){}
    virtual void newCall(const std::string& account_id, const std::string& call_id, const std::string& to){}
    virtual void sipCallStateChange(const std::string& call_id, const std::string& state, int code){}
    virtual void recordingStateChanged(const std::string& call_id, int code){}
    virtual void recordStateChange(const std::string& call_id, int state){}
    virtual void onRtcpReportReceived(const std::string& call_id, const std::map<std::string, int>& stats){}
    virtual void peerHold(const std::string& call_id, bool holding){}
};


%}

%feature("director") Callback;

namespace DRing {

/* Call related methods */
std::string placeCall(const std::string& accountID, const std::string& to);

bool refuse(const std::string& callID);
bool accept(const std::string& callID);
bool hangUp(const std::string& callID);
bool hold(const std::string& callID);
bool unhold(const std::string& callID);
bool muteLocalMedia(const std::string& callid, const std::string& mediaType, bool mute);
bool transfer(const std::string& callID, const std::string& to);
bool attendedTransfer(const std::string& transferID, const std::string& targetID);
std::map<std::string, std::string> getCallDetails(const std::string& callID);
std::vector<std::string> getCallList();

/* Conference related methods */
void removeConference(const std::string& conference_id);
bool joinParticipant(const std::string& sel_callID, const std::string& drag_callID);
void createConfFromParticipantList(const std::vector<std::string>& participants);
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
std::vector<std::string> getDisplayNames(const std::string& confID);
std::string getConferenceId(const std::string& callID);
std::map<std::string, std::string> getConferenceDetails(const std::string& callID);

/* File Playback methods */
bool startRecordedFilePlayback(const std::string& filepath);
void stopRecordedFilePlayback(const std::string& filepath);

/* General audio methods */
bool toggleRecording(const std::string& callID);
/* DEPRECATED */
void setRecording(const std::string& callID);

void recordPlaybackSeek(double value);
bool getIsRecording(const std::string& callID);
std::string getCurrentAudioCodecName(const std::string& callID);
void playDTMF(const std::string& key);
void startTone(int32_t start, int32_t type);

bool switchInput(const std::string& callID, const std::string& resource);

/* Instant messaging */
void sendTextMessage(const std::string& callID, const std::map<std::string, std::string>& messages, const std::string& from, const bool& isMixed);

}

class Callback {
public:
    virtual ~Callback() {}
    virtual void callStateChanged(const std::string& call_id, const std::string& state, int detail_code){}
    virtual void transferFailed(void){}
    virtual void transferSucceeded(void){}
    virtual void recordPlaybackStopped(const std::string& path){}
    virtual void voiceMailNotify(const std::string& call_id, int nd_msg){}
    virtual void incomingMessage(const std::string& id, const std::string& from, const std::map<std::string, std::string>& messages){}
    virtual void incomingCall(const std::string& account_id, const std::string& call_id, const std::string& from){}
    virtual void recordPlaybackFilepath(const std::string& id, const std::string& filename){}
    virtual void conferenceCreated(const std::string& conf_id){}
    virtual void conferenceChanged(const std::string& conf_id, const std::string& state){}
    virtual void conferenceRemoved(const std::string& conf_id){}
    virtual void newCallCreated(const std::string& call_id, const std::string&, const std::string&){}
    virtual void updatePlaybackScale(const std::string& filepath, int position, int scale){}
    virtual void conferenceRemove(const std::string& conf_id){}
    virtual void newCall(const std::string& account_id, const std::string& call_id, const std::string& to){}
    virtual void sipCallStateChange(const std::string& call_id, const std::string& state, int code){}
    virtual void recordingStateChanged(const std::string& call_id, int code){}
    virtual void recordStateChange(const std::string& call_id, int state){}
    virtual void onRtcpReportReceived(const std::string& call_id, const std::map<std::string, int>& stats){}
    virtual void peerHold(const std::string& call_id, bool holding){}
};
