/*
 *  Copyright (C) 2004, 2005, 2006, 2008, 2009, 2010, 2011 Savoir-Faire Linux Inc.
 *  Author: Yan Morin <yan.morin@savoirfairelinux.com>
 *  Author: Laurielle Lea <laurielle.lea@savoirfairelinux.com>
 *  Author: Emmanuel Milou <emmanuel.milou@savoirfairelinux.com>
 *  Author: Guillaume Carmel-Archambault <guillaume.carmel-archambault@savoirfairelinux.com>
 *  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
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

#ifndef MANAGER_IMPL_H_
#define MANAGER_IMPL_H_

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <string>
#include <vector>
#include <set>
#include <map>
#include <tr1/memory>
#include "cc_thread.h"
#include "dbus/dbusmanager.h"

#include "config/sfl_config.h"

#include "call.h"
#include "conference.h"

#include "audio/audiolayer.h"
#include "audio/sound/tone.h"  // for Tone::TONEID declaration
#include "audio/codecs/audiocodecfactory.h"

#include "audio/mainbuffer.h"
#include "preferences.h"
#include "history/history.h"
#include "noncopyable.h"

namespace Conf {
    class YamlParser;
    class YamlEmitter;
}

class DTMF;
class AudioFile;
class AudioLayer;
class History;
class TelephoneTone;
class VoIPLink;

#ifdef USE_ZEROCONF
class DNSService;
#endif

class Account;
class SIPAccount;

/** Define a type for a AccountMap container */
typedef std::map<std::string, Account*> AccountMap;

/** Define a type for a std::string to std::string Map inside ManagerImpl */
typedef std::map<std::string, std::string> CallAccountMap;

/** To send multiple string */
typedef std::list<std::string> TokenList;

/** To store conference objects by conference ids */
typedef std::map<std::string, Conference*> ConferenceMap;

static const char * const default_conf = "conf";

/** Manager (controller) of sflphone daemon */
class ManagerImpl {
    public:
        ManagerImpl();

        /**
         * General preferences configuration
         */
        Preferences preferences;

        /**
         * Voip related preferences
         */
        VoipPreference voipPreferences;

        /**
         * Addressbook related preferences
         */
        AddressbookPreference addressbookPreference;

        /**
         * Hook preferences
         */
        HookPreference hookPreference;

        /**
         * Audio preferences
         */
        AudioPreference audioPreference;

        /**
         * Shortcut preferences
         */
        ShortcutPreferences shortcutPreferences;

        /**
         * Initialisation of thread (sound) and map.
         * Init a new VoIPLink, audio codec and audio driver
         */
        void init(const std::string &config_file);

        /**
         * Enter Dbus mainloop
         */
        void run();

        /**
         * Terminate all thread (sound, link) and unload AccountMap
         */
        void terminate();

        /*
         * Terminate all threads and exit DBus loop
         */
        void finish();

        /**
         * Accessor to audiodriver.
         * it's multi-thread and use mutex internally
         * @return AudioLayer*  The audio layer object
         */
        AudioLayer* getAudioDriver() {
            return audiodriver_;
        }

        /**
         * Functions which occur with a user's action
         * Place a new call
         * @param accountId	The account to make tha call with
         * @param call_id  The call identifier
         * @param to  The recipient of the call
         * @param conf_id The conference identifier if any
         * @return bool true on success
         *		  false otherwise
         */
        bool outgoingCall(const std::string&, const std::string&, const std::string&, const std::string& = "");

        /**
         * Functions which occur with a user's action
         * Answer the call
         * @param id  The call identifier
         */
        bool answerCall(const std::string& id);

        /**
         * Functions which occur with a user's action
         * Hangup the call
         * @param id  The call identifier
         */
        void hangupCall(const std::string& id);


        /**
         * Functions which occur with a user's action
         * Hangup the conference (hangup every participants)
         * @param id  The call identifier
         */
        bool hangupConference(const std::string& id);

        /**
         * Functions which occur with a user's action
         * Put the call on hold
         * @param id  The call identifier
         */
        void onHoldCall(const std::string& id);

        /**
         * Functions which occur with a user's action
         * Put the call off hold
         * @param id  The call identifier
         */
        void offHoldCall(const std::string& id);

        /**
         * Functions which occur with a user's action
         * Transfer the call
         * @param id  The call identifier
         * @param to  The recipient of the transfer
         */
        bool transferCall(const std::string& id, const std::string& to);

        /**
         * Attended transfer
         * @param The call id to be transfered
         * @param The target
         */
        bool attendedTransfer(const std::string& transferID, const std::string& targetID);

        /**
         * Notify the client the transfer is successful
         */
        void transferSucceeded();

        /**
         * Notify the client that the transfer failed
         */
        void transferFailed();

        /**
         * Functions which occur with a user's action
         * Refuse the call
         * @param id  The call identifier
         */
        void refuseCall(const std::string& id);

        /**
         * Create a new conference given two participant
         * @param the first participant ID
         * @param the second participant ID
         */
        Conference* createConference(const std::string& id1, const std::string& id2);

        /**
         * Delete this conference
         * @param the conference ID
         */
        void removeConference(const std::string& conference_id);

        /**
         * Return the conference id for which this call is attached
         * @ param the call id
         */
        Conference* getConferenceFromCallID(const std::string& call_id);

        /**
         * Hold every participant to a conference
         * @param the conference id
         */
        void holdConference(const std::string& conference_id);

        /**
         * Unhold all conference participants
         * @param the conference id
         */
        void unHoldConference(const std::string& conference_id);

        /**
         * Test if this id is a conference (usefull to test current call)
         * @param the call id
         */
        bool isConference(const std::string& call_id) const;

        /**
         * Test if a call id corresponds to a conference participant
         * @param the call id
         */
        bool isConferenceParticipant(const std::string& call_id);

        /**
         * Add a participant to a conference
         * @param the call id
         * @param the conference id
         */
        void addParticipant(const std::string& call_id, const std::string& conference_id);

        /**
         * Bind the main participant to a conference (mainly called on a double click action)
         * @param the conference id
         */
        void addMainParticipant(const std::string& conference_id);

        /**
         * Join two participants to create a conference
         * @param the fist call id
         * @param the second call id
         */
        void joinParticipant(const std::string& call_id1, const std::string& call_id2);

        /**
         * Create a conference from a list of participant
         * @param A vector containing the list of participant
         */
        void createConfFromParticipantList(const std::vector< std::string > &);

        /**
         * Detach a participant from a conference, put the call on hold, do not hangup it
         * @param call id
         * @param the current call id
         */
        void detachParticipant(const std::string& call_id, const std::string& current_call_id);

        /**
         * Remove the conference participant from a conference
         * @param call id
         */
        void removeParticipant(const std::string& call_id);

        /**
         * Join two conference together into one unique conference
         */
        void joinConference(const std::string& conf_id1, const std::string& conf_id2);

        void addStream(const std::string& call_id);

        void removeStream(const std::string& call_id);

        /**
         * Save config to file
         */
        void saveConfig();

        /**
         * @return true if we tried to register once
         */
        bool hasTriedToRegister_;

        /**
         * Handle choice of the DTMF-send-way
         * @param   id: callid of the line.
         * @param   code: pressed key.
         */
        void sendDtmf(const std::string& id, char code);

        /**
         * Play a ringtone
         */
        void playTone();

        /**
         * Play a special ringtone ( BUSY ) if there's at least one message on the voice mail
         */
        void playToneWithMessage();

        /**
         * Acts on the audio streams and audio files
         */
        void stopTone();

        /**
         * When receiving a new incoming call, add it to the callaccount map
         * and notify user
         * @param call A call pointer
         * @param accountId an account id
         */
        void incomingCall(Call &call, const std::string& accountId);

        /**
         * Notify the user that the recipient of the call has answered and the put the
         * call in Current state
         * @param id  The call identifier
         */
        void peerAnsweredCall(const std::string& id);

        /**
         * Rings back because the outgoing call is ringing and the put the
         * call in Ringing state
         * @param id  The call identifier
         */
        void peerRingingCall(const std::string& id);

        /**
         * Put the call in Hungup state, remove the call from the list
         * @param id  The call identifier
         */
        void peerHungupCall(const std::string& id);

#if HAVE_INSTANT_MESSAGING
        /**
         * Notify the client with an incoming message
         * @param accountId	The account identifier
         * @param message The content of the message
         */
        void incomingMessage(const std::string& callID, const std::string& from, const std::string& message);


        /**
         * Send a new text message to the call, if participate to a conference, send to all participant.
         * @param callID	The call to send the message
         * @param message	The content of the message
        * @param from	        The sender of this message (could be another participant of a conference)
         */
        bool sendTextMessage(const std::string& callID, const std::string& message, const std::string& from);
#endif // HAVE_INSTANT_MESSAGING

        /**
         * Notify the client he has voice mails
         * @param accountId	  The account identifier
         * @param nb_msg The number of messages
         */
        void startVoiceMessageNotification(const std::string& accountId, int nb_msg);

        /**
         * ConfigurationManager - Send registration request
         * @param accountId The account to register/unregister
         * @param enable The flag for the type of registration
         *		 false for unregistration request
         *		 true for registration request
         */
        void sendRegister(const std::string& accountId, bool enable);

        /**
         * Register all account in accountMap_
         */
        void registerAllAccounts();

        /**
         * Unregister all account in accountMap_
         */
        void unregisterAllAccounts();

        /**
         * Get account list
         * @return std::vector<std::string> A list of accoundIDs
         */
        std::vector<std::string> getAccountList() const;

        /**
         * Set the account order in the config file
         */
        void setAccountsOrder(const std::string& order);

        /**
         * Retrieve details about a given account
         * @param accountID	  The account identifier
         * @return std::map< std::string, std::string > The account details
         */
        std::map<std::string, std::string> getAccountDetails(const std::string& accountID) const;

        /**
         * Retrieve details about a given call
         * @param callID	  The account identifier
         * @return std::map< std::string, std::string > The call details
         */
        std::map<std::string, std::string> getCallDetails(const std::string& callID);

        /**
         * Get call list
         * @return std::vector<std::string> A list of call IDs
         */
        std::vector<std::string> getCallList() const;

        /**
         * Retrieve details about a given call
         * @param callID	  The account identifier
         * @return std::map< std::string, std::string > The call details
         */
        std::map<std::string, std::string> getConferenceDetails(const std::string& callID) const;

        /**
         * Get call list
         * @return std::vector<std::string> A list of call IDs
         */
        std::vector<std::string> getConferenceList() const;


        /**
         * Get a list of participant to a conference
         * @return std::vector<std::string> A list of call IDs
         */
        std::vector<std::string> getParticipantList(const std::string& confID) const;

        std::string getConferenceId(const std::string& callID);

        /**
         * Save the details of an existing account, given the account ID
         * This will load the configuration map with the given data.
         * It will also register/unregister links where the 'Enabled' switched.
         * @param accountID	  The account identifier
         * @param details	  The account parameters
         */
        void setAccountDetails(const std::string& accountID,
                               const std::map<std::string, ::std::string > &details);

        /**
         * Add a new account, and give it a new account ID automatically
         * @param details The new account parameters
         * @return The account Id given to the new account
         */
        std::string addAccount(const std::map<std::string, std::string> &details);

        /**
         * Delete an existing account, unregister VoIPLink associated, and
         * purge from configuration.
         * @param accountID	The account unique ID
         */
        void removeAccount(const std::string& accountID);

        /**
         * Get current codec name
         * @param call id
         * @return std::string The codec name
         */
        std::string getCurrentAudioCodecName(const std::string& id);
        std::string getCurrentVideoCodecName(const std::string& id);

        /**
         * Set input audio plugin
         * @param audioPlugin The audio plugin
         */
        void setAudioPlugin(const std::string& audioPlugin);

        /**
             * Set audio device
             * @param index The index of the soundcard
             * @param the type of stream, either SFL_PCM_PLAYBACK, SFL_PCM_CAPTURE, SFL_PCM_RINGTONE
             */
        void setAudioDevice(int index, AudioLayer::PCMType streamType);

        /**
         * Get list of supported audio output device
         * @return std::vector<std::string> A list of the audio devices supporting playback
         */
        std::vector<std::string> getAudioOutputDeviceList();

        /**
         * Get list of supported audio input device
         * @return std::vector<std::string> A list of the audio devices supporting capture
         */
        std::vector<std::string> getAudioInputDeviceList();

        /**
         * Get string array representing integer indexes of output, input, and ringtone device
         * @return std::vector<std::string> A list of the current audio devices
         */
        std::vector<std::string> getCurrentAudioDevicesIndex();

        /**
         * Get index of an audio device
         * @param name The string description of an audio device
         * @return int  His index
         */
        int getAudioDeviceIndex(const std::string &name);

        /**
         * Get current alsa plugin
         * @return std::string  The Alsa plugin
         */
        std::string getCurrentAudioOutputPlugin() const;

        /**
         * Get the noise reduction engin state from
         * the current audio layer.
         */
        std::string getNoiseSuppressState() const;

        /**
         * Set the noise reduction engin state in the current
         * audio layer.
         */
        void setNoiseSuppressState(const std::string &state);

        /**
         * Get the echo canceller engin state from
         * the current audio layer
         */
        bool getEchoCancelState() const;

        /**
         * Set the echo canceller engin state
         */
        void setEchoCancelState(const std::string &state);

        int getEchoCancelTailLength() const;

        void setEchoCancelTailLength(int);

        int getEchoCancelDelay() const;

        void setEchoCancelDelay(int);

        /**
         * Convert a list of payload in a special format, readable by the server.
         * Required format: payloads separated with one slash.
         * @return std::string The serializabled string
         */
        static std::string join_string(const std::vector<std::string> &v);

        static std::vector<std::string> split_string(std::string v);

        /**
         * Ringtone option.
         * If ringtone is enabled, ringtone on incoming call use custom choice. If not, only standart tone.
         * @return int	1 if enabled
         *	        0 otherwise
         */
        int isRingtoneEnabled(const std::string& id);

        /**
         * Set the ringtone option
         * Inverse current value
         */
        void ringtoneEnabled(const std::string& id);

        /**
         * Get the recording path from configuration tree
         * @return the string correspoding to the path
         */
        std::string getRecordPath() const;

        /**
         * Set the recoding path in the configuration tree
         * @param a string reresenting the path
         */
        void setRecordPath(const std::string& recPath);

        /**
         * Get is always recording functionality
         */
        bool getIsAlwaysRecording() const;

        /**
         * Set is always recording functionality, every calls will then be set in RECORDING mode
         * once answered
         */
        void setIsAlwaysRecording(bool isAlwaysRec);

        /**
         * Set recording on / off
         * Start recording
         * @param id  The call identifier
         */
        void setRecordingCall(const std::string& id);

        /**
         * Return true if the call is currently recorded
         */
        bool isRecording(const std::string& id);

        /**
         * Start playback fo a recorded file if and only if audio layer is not already started.
         * @param File path of the file to play
             */
        bool startRecordedFilePlayback(const std::string&);

        void recordingPlaybackSeek(const double value);

        /**
         * Stop playback of recorded file
         * @param File of the file to stop
         */
        void stopRecordedFilePlayback(const std::string&);

        /**
         * Set the maximum number of days to keep in the history
         * @param calls The number of days
         */
        void setHistoryLimit(int days);

        /**
         * Get the maximum number of days to keep in the history
         * @return double The number of days
         */
        int getHistoryLimit() const;

        /**
         * Configure the start-up option
         * @return int	1 if SFLphone should start in the system tray
         *	        0 otherwise
         */
        int isStartHidden();

        /**
         * Configure the start-up option
         * At startup, SFLphone can be displayed or start hidden in the system tray
         */
        void startHidden();

        /**
         * Set the desktop mail notification level
         */
        void setMailNotify();


        /**
         * Addressbook configuration
         */
        std::map<std::string, int32_t> getAddressbookSettings() const;

        /**
         * Addressbook configuration
         */
        void setAddressbookSettings(const std::map<std::string, int32_t>& settings);

        /**
         * Addressbook list
         */
        void setAddressbookList(const std::vector<  std::string >& list);

        /**
         * Addressbook list
         */
        std::vector <std::string> getAddressbookList() const;

        /**
         * Get the audio manager
         * @return int The audio manager
         *		    "alsa"
         *		    "pulseaudio"
         */
        std::string getAudioManager() const;

        /**
         * Set the audio manager
         */
        void setAudioManager(const std::string &api);

        void switchAudioManager();

        /**
         * Set the internal audio sampling rate change. Should close the audio layer and
         * reopen stream at different rate,
         */
        void audioSamplingRateChanged(int);

        /**
         * Get the desktop mail notification level
         * @return int The mail notification level
         */
        int32_t getMailNotify() const;

        /**
         * Change a specific value in the configuration tree.
         * This value will then be saved in the user config file sflphonedrc
         * @param section	The section name
         * @param name	The parameter name
         * @param value	The new string value
         * @return bool	true on success
         *		      false otherwise
         */
        void setConfig(const std::string& section, const std::string& name, const std::string& value);

        /**
         * Change a specific value in the configuration tree.
         * This value will then be saved in the user config file sflphonedrc
         * @param section	The section name
         * @param name	The parameter name
         * @param value	The new int value
         * @return bool	true on success
         *		      false otherwise
         */
        void setConfig(const std::string& section, const std::string& name, int value);

        /**
         * Get a string from the configuration tree
         * Throw an Conf::ConfigTreeItemException if not found
         * @param section The section name to look in
         * @param name    The parameter name
         * @return sdt::string    The string value
         */
        std::string getConfigString(const std::string& section, const std::string& name) const;

        /**
         * Retrieve the soundcards index in the user config file and try to open audio devices
         * with a specific alsa plugin.
         * Set the audio layer sample rate
         */
        void selectAudioDriver();

        /**
         * Handle audio sounds heard by a caller while they wait for their
         * connection to a called party to be completed.
         */
        void ringback();

        /**
         * Handle played music when an incoming call occurs
         */
        void ringtone(const std::string& accountID);

        /**
         * Handle played music when a congestion occurs
         */
        void congestion();

        /**
         * Handle played sound when a call can not be conpleted because of a busy recipient
         */
        void callBusy(const std::string& id);

        /**
         * Handle played sound when a failure occurs
         */
        void callFailure(const std::string& id);

        /**
         * Retrieve the current telephone tone
         * @return AudioLoop*   The audio tone or 0 if no tone (init before calling this function)
         */
        AudioLoop* getTelephoneTone();

        /**
         * Retrieve the current telephone file
         * @return AudioLoop* The audio file or 0 if the wav is stopped
         */
        AudioLoop* getTelephoneFile();

        /**
         * @return true is there is one or many incoming call waiting
         * new call, not anwsered or refused
         */
        bool incomingCallWaiting() const;

        /**
         * Return a new random callid that is not present in the list
         * @return std::string A brand new callid
         */
        std::string getNewCallID();

        /**
         * Get the current call id
         * @return std::string	The call id or ""
         */
        std::string getCurrentCallId() const;

        /**
         * Check if a call is the current one
         * @param callId the new callid
         * @return bool   True if the id is the current call
         */
        bool isCurrentCall(const std::string& callId) const;

        void initAudioDriver();

        void audioLayerMutexLock() {
            audioLayerMutex_.enterMutex();
        }

        void audioLayerMutexUnlock() {
            audioLayerMutex_.leaveMutex();
        }

        /**
         * Load the accounts order set by the user from the sflphonedrc config file
         * @return std::vector<std::string> A vector containing the account ID's
         */
        std::vector<std::string> loadAccountOrder() const;

        // map of codec (for configlist request)
        const AudioCodecFactory audioCodecFactory;

    private:

        /**
         * Get the Call referred to by callID. If the Call does not exist, return NULL
         */
        Call *getCallFromCallID(const std::string &callID);

        /**
         * Play the dtmf-associated sound
         * @param code  The pressed key
         */
        void playDtmf(char code);

        /**
         * Process remaining participant given a conference and the current call id.
         * Mainly called when a participant is detached or hagned up
         * @param current call id
         * @param conference pointer
         */
        void processRemainingParticipants(Conference &conf);

        /**
         * Create config directory in home user and return configuration file path
         */
        std::string createConfigFile() const;

        /*
         * Initialize zeroconf module and scanning
         */
        void initZeroconf();

        /**
         * Set current call ID to empty string
         */
        void unsetCurrentCall();

        /**
         * Switch of current call id
         * @param id The new callid
         */
        void switchCall(const std::string& id);

        /*
         * Play one tone
         * @return false if the driver is uninitialize
         */
        void playATone(Tone::TONEID toneId);

        DBusManager dbus_;

        /** The configuration tree. It contains accounts parameters, general user settings ,audio settings, ... */
        Conf::ConfigTree config_;

        /** Current Call ID */
        std::string currentCallId_;

        /** Protected current call access */
        ost::Mutex currentCallMutex_;

        /** Audio layer */
        AudioLayer* audiodriver_;

        // Main thread
        std::tr1::shared_ptr<DTMF> dtmfKey_;

        /////////////////////
        // Protected by Mutex
        /////////////////////
        ost::Mutex toneMutex_;
        std::tr1::shared_ptr<TelephoneTone> telephoneTone_;
        std::tr1::shared_ptr<AudioFile> audiofile_;

        // To handle volume control
        // short speakerVolume_;
        // short micVolume_;
        // End of sound variable

        /**
         * Mutex used to protect audio layer
         */
        ost::Mutex audioLayerMutex_;

        /**
         * Waiting Call Vectors
         */
        CallIDSet waitingCall_;

        /**
         * Protect waiting call list, access by many voip/audio threads
         */
        ost::Mutex waitingCallMutex_;

        /**
         * Number of waiting call, synchronize with waitingcall callidvector
         */
        unsigned int nbIncomingWaitingCall_;

        /**
         * Add incoming callid to the waiting list
         * @param id std::string to add
         */
        void addWaitingCall(const std::string& id);

        /**
         * Remove incoming callid to the waiting list
         * @param id std::string to remove
         */
        void removeWaitingCall(const std::string& id);

        /** Remove a CallID/std::string association
         * Protected by mutex
         * @param callID the CallID to remove
         */
        void removeCallAccount(const std::string& callID);

        /**
         * Path of the ConfigFile
         */
        std::string path_;

#ifdef USE_ZEROCONF
        // DNSService contain every zeroconf services
        //  configuration detected on the network
        DNSService *DNSService_;
#endif

        /** Map to associate a CallID to the good account */
        CallAccountMap callAccountMap_;

        /** Mutex to lock the call account map (main thread + voiplink thread) */
        ost::Mutex callAccountMapMutex_;

        std::map<std::string, bool> IPToIPMap_;


        bool isIPToIP(const std::string& callID) const;

        /**
         *Contains a list of account (sip, aix, etc) and their respective voiplink/calls */
        AccountMap accountMap_;

        /**
         * Load the account map from configuration
         */
        void loadAccountMap(Conf::YamlParser &parser);
        /**
         * Load default account map (no configuration)
         */
        void loadDefaultAccountMap();

        /**
         * Unload the account (delete them)
         */
        void unloadAccountMap();

        /**
         * Instance of the MainBuffer for the whole application
         *
         * In order to send signal to other parts of the application, one must pass through the mainbuffer.
         * Audio instances must be registered into the MainBuffer and bound together via the ManagerImpl.
         *
         */
        MainBuffer mainBuffer_;

    public:

        void setIPToIPForCall(const std::string& callID, bool IPToIP);

        /** Associate a new std::string to a std::string
         * Protected by mutex
         * @param callID the new CallID not in the list yet
         * @param accountID the known accountID present in accountMap
         * @return bool True if the new association is create
         */
        void associateCallToAccount(const std::string& callID, const std::string& accountID);

        /**
         * Test if call is a valid call, i.e. have been created and stored in
         * call-account map
         * @param callID the std::string to be tested
         * @return true if call is created and present in the call-account map
         */
        bool isValidCall(const std::string& callID);

        /**
         * Return a pointer to the  instance of the mainbuffer
         */
        MainBuffer *getMainBuffer() {
            return &mainBuffer_;
        }

        /**
         * Tell if there is a current call processed
         * @return bool True if there is a current call
         */
        bool hasCurrentCall() const;

        /**
         * Return the current DBusManager
         * @return A pointer to the DBusManager instance
         */
        DBusManager * getDbusManager() {
            return &dbus_;
        }

#ifdef SFL_VIDEO
        VideoControls * getVideoControls() {
            return dbus_.getVideoControls();
        }
#endif

        /**
        * Tell if an account exists
        * @param accountID account ID check
        * @return bool True if the account exists
        *		  false otherwise
        */
        bool accountExists(const std::string& accountID);

        std::vector<std::map<std::string, std::string> > getHistory();
        void clearHistory();

        /**
         * Get an account pointer
         * @param accountID account ID to get
         * @return Account*	 The account pointer or 0
         */
        Account* getAccount(const std::string& accountID);
        SIPAccount* getIP2IPAccount();

        /** Return the std::string from a CallID
         * Protected by mutex
         * @param callID the CallID in the list
         * @return std::string  The accountID associated or "" if the callID is not found
         */
        std::string getAccountFromCall(const std::string& callID);

        /**
         * Get the voip link from the account pointer
         * @param accountID	  Account ID to get
         * @return VoIPLink*   The voip link from the account pointer or 0
         */
        VoIPLink* getAccountLink(const std::string& accountID);

        std::string getAccountIdFromNameAndServer(const std::string& userName, const std::string& server) const;

        std::string getStunServer() const;
        void setStunServer(const std::string &server);

        int isStunEnabled();
        void enableStun();

        // Map containing conference pointers
        ConferenceMap conferenceMap_;

        /**
         * Send registration to all enabled accounts
         */
        void registerAccounts();
        void saveHistory();

    private:
        NON_COPYABLE(ManagerImpl);

        /**
          * To handle the persistent history
          * TODO: move this to ConfigurationManager
          */
        History history_;
        bool finished_;
};
#endif // MANAGER_IMPL_H_
