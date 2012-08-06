/*
 *  Copyright (C) 2004, 2005, 2006, 2008, 2009, 2010, 2011 Savoir-Faire Linux Inc.
 *  Author: Pierre-Luc Beaudoin <pierre-luc.beaudoin@savoirfairelinux.com>
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

#ifndef __SFL_CALLMANAGER_H__
#define __SFL_CALLMANAGER_H__

#include "dbus_cpp.h"
#if __GNUC__ >= 4 && __GNUC_MINOR__ >= 6
/* This warning option only exists for gcc 4.6.0 and greater. */
#pragma GCC diagnostic ignored "-Wunused-but-set-variable"
#endif

#pragma GCC diagnostic ignored "-Wignored-qualifiers"
#pragma GCC diagnostic ignored "-Wunused-parameter"
#include "callmanager-glue.h"
#pragma GCC diagnostic warning "-Wignored-qualifiers"
#pragma GCC diagnostic warning "-Wunused-parameter"

#if __GNUC__ >= 4 && __GNUC_MINOR__ >= 6
/* This warning option only exists for gcc 4.6.0 and greater. */
#pragma GCC diagnostic warning "-Wunused-but-set-variable"
#endif

#include <stdexcept>

class CallManagerException: public std::runtime_error {
    public:
        CallManagerException(const std::string& str="") :
            std::runtime_error("A CallManagerException occured: " + str) {}
};

namespace sfl {
    class AudioZrtpSession;
}

class CallManager
    : public org::sflphone::SFLphone::CallManager_adaptor,
  public DBus::IntrospectableAdaptor,
      public DBus::ObjectAdaptor {
    public:

        CallManager(DBus::Connection& connection);

        /* methods exported by this interface,
         * you will have to implement them in your ObjectAdaptor
         */

        /* Call related methods */
        void placeCall(const std::string& accountID, const std::string& callID, const std::string& to);
        void placeCallFirstAccount(const std::string& callID, const std::string& to);

        void refuse(const std::string& callID);
        void accept(const std::string& callID);
        void hangUp(const std::string& callID);
        void hold(const std::string& callID);
        void unhold(const std::string& callID);
        void transfer(const std::string& callID, const std::string& to);
        void attendedTransfer(const std::string& transferID, const std::string& targetID);
        std::map< std::string, std::string > getCallDetails(const std::string& callID);
        std::vector< std::string > getCallList();

        /* Conference related methods */
        void joinParticipant(const std::string& sel_callID, const std::string& drag_callID);
        void createConfFromParticipantList(const std::vector< std::string >& participants);
        void addParticipant(const std::string& callID, const std::string& confID);
        void addMainParticipant(const std::string& confID);
        void detachParticipant(const std::string& callID);
        void joinConference(const std::string& sel_confID, const std::string& drag_confID);
        void hangUpConference(const std::string& confID);
        void holdConference(const std::string& confID);
        void unholdConference(const std::string& confID);
        std::vector<std::string> getConferenceList();
        std::vector<std::string> getParticipantList(const std::string& confID);
        std::string getConferenceId(const std::string& callID);
        std::map<std::string, std::string> getConferenceDetails(const std::string& callID);

        /* File Playback methods */
        bool startRecordedFilePlayback(const std::string& filepath);
        void stopRecordedFilePlayback(const std::string& filepath);

        /* General audio methods */
        void setVolume(const std::string& device, const double& value);
        double getVolume(const std::string& device);
        void setRecording(const std::string& callID);
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

    private:

#if HAVE_ZRTP
        sfl::AudioZrtpSession * getAudioZrtpSession(const std::string& callID);
#endif
};

#endif//CALLMANAGER_H
