/*
 *  Copyright (C) 2004, 2005, 2006, 2008, 2009, 2010, 2011 Savoir-Faire Linux Inc.
 *  Author: Pierre-Luc Beaudoin <pierre-luc.beaudoin@savoirfairelinux.com>
 *  Author: Alexandre Bourget <alexandre.bourget@savoirfairelinux.com>
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
#include <vector>

#include "global.h"
#include "callmanager.h"

#include "sip/sipcall.h"
#include "sip/sipvoiplink.h"
#include "audio/audiolayer.h"
#include "audio/audiortp/audio_rtp_factory.h"
#if HAVE_ZRTP
#include "audio/audiortp/audio_zrtp_session.h"
#endif

#include "logger.h"
#include "manager.h"

CallManager::CallManager(DBus::Connection& connection)
    : DBus::ObjectAdaptor(connection, "/org/sflphone/SFLphone/CallManager")
{}

void CallManager::placeCall(const std::string& accountID,
                            const std::string& callID,
                            const std::string& to)
{
    // Check if a destination number is available
    if (to.empty())
        DEBUG("No number entered - Call stopped");
    else
        Manager::instance().outgoingCall(accountID, callID, to);
}

void CallManager::placeCallFirstAccount(const std::string& callID,
                                        const std::string& to)
{
    using std::vector;
    using std::string;

    if (to.empty()) {
        WARN("CallManager: Warning: No number entered, call stopped");
        return;
    }

    vector<string> accountList(Manager::instance().loadAccountOrder());

    if (accountList.empty())
        accountList = Manager::instance().getAccountList();

    for (vector<string>::const_iterator iter = accountList.begin(); iter != accountList.end(); ++iter) {
        if ((*iter != SIPAccount::IP2IP_PROFILE) && Manager::instance().getAccount(*iter)->isEnabled()) {
            Manager::instance().outgoingCall(*iter, callID, to);
            return;
        }
    }
}

void
CallManager::refuse(const std::string& callID)
{
    Manager::instance().refuseCall(callID);
}

void
CallManager::accept(const std::string& callID)
{
    Manager::instance().answerCall(callID);
}

void
CallManager::hangUp(const std::string& callID)
{
    Manager::instance().hangupCall(callID);
}

void
CallManager::hangUpConference(const std::string& confID)
{
    Manager::instance().hangupConference(confID);
}

void
CallManager::hold(const std::string& callID)
{
    Manager::instance().onHoldCall(callID);
}

void
CallManager::unhold(const std::string& callID)
{
    Manager::instance().offHoldCall(callID);
}

void
CallManager::transfer(const std::string& callID, const std::string& to)
{
    Manager::instance().transferCall(callID, to);
}

void CallManager::attendedTransfer(const std::string& transferID, const std::string& targetID)
{
    Manager::instance().attendedTransfer(transferID, targetID);
}

void CallManager::setVolume(const std::string& device, const double& value)
{
    AudioLayer *audiolayer = Manager::instance().getAudioDriver();

    if(!audiolayer) {
        ERROR("Audio layer not valid while updating volume");
        return;
    }

    DEBUG("DBUS set volume for %s: %f", device.c_str(), value);

    if (device == "speaker") {
        audiolayer->setPlaybackGain((int)(value * 100.0));
    } else if (device == "mic") {
        audiolayer->setCaptureGain((int)(value * 100.0));
    }

    volumeChanged(device, value);
}

double
CallManager::getVolume(const std::string& device)
{
    AudioLayer *audiolayer = Manager::instance().getAudioDriver();

    if(!audiolayer) {
        ERROR("Audio layer not valid while updating volume");
        return 0.0;
    }

    if (device == "speaker")
        return audiolayer->getPlaybackGain() / 100.0;
    else if (device == "mic")
        return audiolayer->getCaptureGain() / 100.0;

    return 0;
}

void
CallManager::joinParticipant(const std::string& sel_callID, const std::string& drag_callID)
{
    Manager::instance().joinParticipant(sel_callID, drag_callID);
}

void
CallManager::createConfFromParticipantList(const std::vector<std::string>& participants)
{
    Manager::instance().createConfFromParticipantList(participants);
}

void
CallManager::addParticipant(const std::string& callID, const std::string& confID)
{
    Manager::instance().addParticipant(callID, confID);
}

void
CallManager::addMainParticipant(const std::string& confID)
{
    Manager::instance().addMainParticipant(confID);
}

void
CallManager::detachParticipant(const std::string& callID)
{
    Manager::instance().detachParticipant(callID, "");
}

void
CallManager::joinConference(const std::string& sel_confID, const std::string& drag_confID)
{
    Manager::instance().joinConference(sel_confID, drag_confID);
}

void
CallManager::holdConference(const std::string& confID)
{
    Manager::instance().holdConference(confID);
}

void
CallManager::unholdConference(const std::string& confID)
{
    Manager::instance().unHoldConference(confID);
}

std::map<std::string, std::string>
CallManager::getConferenceDetails(const std::string& callID)
{
    return Manager::instance().getConferenceDetails(callID);
}

std::vector<std::string>
CallManager::getConferenceList()
{
    return Manager::instance().getConferenceList();
}

std::vector<std::string>
CallManager::getParticipantList(const std::string& confID)
{
    return Manager::instance().getParticipantList(confID);
}

std::string
CallManager::getConferenceId(const std::string& callID)
{
    return Manager::instance().getConferenceId(callID);
}

bool
CallManager::startRecordedFilePlayback(const std::string& filepath)
{
    return Manager::instance().startRecordedFilePlayback(filepath);
}

void
CallManager::stopRecordedFilePlayback(const std::string& filepath)
{
    Manager::instance().stopRecordedFilePlayback(filepath);
}

void
CallManager::setRecording(const std::string& callID)
{
    Manager::instance().setRecordingCall(callID);
}

void
CallManager::recordPlaybackSeek(const double& value)
{
    Manager::instance().recordingPlaybackSeek(value);
}

bool
CallManager::getIsRecording(const std::string& callID)
{
    return Manager::instance().isRecording(callID);
}

std::string CallManager::getCurrentAudioCodecName(const std::string& callID)
{
    return Manager::instance().getCurrentAudioCodecName(callID);
}

std::map<std::string, std::string>
CallManager::getCallDetails(const std::string& callID)
{
    return Manager::instance().getCallDetails(callID);
}

std::vector<std::string>
CallManager::getCallList()
{
    return Manager::instance().getCallList();
}

void
CallManager::playDTMF(const std::string& key)
{
    Manager::instance().sendDtmf(Manager::instance().getCurrentCallId(), key.data()[0]);
}

void
CallManager::startTone(const int32_t& start , const int32_t& type)
{
    if (start) {
        if (type == 0)
            Manager::instance().playTone();
        else
            Manager::instance().playToneWithMessage();
    } else
        Manager::instance().stopTone();
}

// TODO: this will have to be adapted
// for conferencing in order to get
// the right pointer for the given
// callID.
#if HAVE_ZRTP
sfl::AudioZrtpSession *
CallManager::getAudioZrtpSession(const std::string& callID)
{
    SIPVoIPLink * link = dynamic_cast<SIPVoIPLink *>(Manager::instance().getAccountLink(""));

    if (!link)
        throw CallManagerException("Failed to get sip link");

    SIPCall *call;

    try {
        call = link->getSIPCall(callID);
    } catch (const VoipLinkException &e) {
        throw CallManagerException("Call id " + callID + " is not valid");
    }

    sfl::AudioZrtpSession * zSession = call->getAudioRtp().getAudioZrtpSession();

    if (!zSession)
        throw CallManagerException("Failed to get AudioZrtpSession");

    return zSession;
}
#endif

void
CallManager::setSASVerified(const std::string& callID)
{
#if HAVE_ZRTP
    try {
        sfl::AudioZrtpSession * zSession;
        zSession = getAudioZrtpSession(callID);
        zSession->SASVerified();
    } catch (...) {
    }
#else
    ERROR("No zrtp support for %s, please recompile SFLphone with zrtp", callID.c_str());
#endif
}

void
CallManager::resetSASVerified(const std::string& callID)
{
#if HAVE_ZRTP
    try {
        sfl::AudioZrtpSession * zSession;
        zSession = getAudioZrtpSession(callID);
        zSession->resetSASVerified();
    } catch (...) {
    }
#else
    ERROR("No zrtp support for %s, please recompile SFLphone with zrtp", callID.c_str());
#endif
}

void
CallManager::setConfirmGoClear(const std::string& callID)
{
#if HAVE_ZRTP
    try {
        sfl::AudioZrtpSession * zSession;
        zSession = getAudioZrtpSession(callID);
        zSession->goClearOk();
    } catch (...) {
    }
#else
    ERROR("No zrtp support for %s, please recompile SFLphone with zrtp", callID.c_str());
#endif
}

void
CallManager::requestGoClear(const std::string& callID)
{
#if HAVE_ZRTP
    try {
        sfl::AudioZrtpSession * zSession;
        zSession = getAudioZrtpSession(callID);
        zSession->requestGoClear();
    } catch (...) {
    }
#else
    ERROR("No zrtp support for %s, please recompile SFLphone with zrtp", callID.c_str());
#endif
}

void
CallManager::acceptEnrollment(const std::string& callID, const bool& accepted)
{
#if HAVE_ZRTP
    try {
        sfl::AudioZrtpSession * zSession;
        zSession = getAudioZrtpSession(callID);
        zSession->acceptEnrollment(accepted);
    } catch (...) {
    }
#else
    ERROR("No zrtp support for %s, please recompile SFLphone with zrtp", callID.c_str());
#endif
}

void
CallManager::sendTextMessage(const std::string& callID, const std::string& message)
{
#if HAVE_INSTANT_MESSAGING
    if (!Manager::instance().sendTextMessage(callID, message, "Me"))
        throw CallManagerException();
#else
    ERROR("Could not send \"%s\" text message to %s since SFLphone daemon does not support it, please recompile with instant messaging support", message.c_str(), callID.c_str());
#endif
}
