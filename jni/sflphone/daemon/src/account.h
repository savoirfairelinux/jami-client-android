/*
 *  Copyright (C) 2004, 2005, 2006, 2008, 2009, 2010, 2011 Savoir-Faire Linux Inc.
 *  Author: Emmanuel Milou <emmanuel.milou@savoirfairelinux.com>
 *  Author: Yan Morin <yan.morin@savoirfairelinux.com>
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

#ifndef ACCOUNT_H
#define ACCOUNT_H

#include <string>
#include <vector>

#include "noncopyable.h"
#include "config/sfl_config.h"
#include "config/serializable.h"
#include "registration_states.h"

class VoIPLink;

/**
 * @file account.h
 * @brief Interface to protocol account (SIPAccount, IAXAccount)
 * It can be enable on loading or activate after.
 * It contains account, configuration, VoIP Link and Calls (inside the VoIPLink)
 */

class Account : public Serializable {

    public:

        Account(const std::string& accountID, const std::string &type);

        /**
         * Virtual destructor
         */
        virtual ~Account();

        virtual void setAccountDetails(std::map<std::string, std::string> details) = 0;

        virtual std::map<std::string, std::string> getAccountDetails() const = 0;

        /**
         * Load the settings for this account.
         */
        virtual void loadConfig() = 0;

        /**
         * Get the account ID
         * @return constant account id
         */
        std::string getAccountID() const {
            return accountID_;
        }

        /**
         * Get the voiplink pointer
         * @return VoIPLink* the pointer or 0
         */
        virtual VoIPLink* getVoIPLink() = 0;

        /**
         * Register the underlying VoIPLink. Launch the event listener.
         * This should update the getRegistrationState() return value.
         */
        virtual void registerVoIPLink() = 0;

        /**
         * Unregister the underlying VoIPLink. Stop the event listener.
         * This should update the getRegistrationState() return value.
         */
        virtual void unregisterVoIPLink() = 0;

        /**
         * Tell if the account is enable or not.
         * @return true if enabled
         *	     false otherwise
         */
        bool isEnabled() const {
            return enabled_;
        }

        void setEnabled(bool enable) {
            enabled_ = enable;
        }

        /**
         * Set the registration state of the specified link
         * @param state	The registration state of underlying VoIPLink
         */
        void setRegistrationState(const RegistrationState &state);

        /* They should be treated like macro definitions by the C++ compiler */
        std::string getUsername() const {
            return username_;
        }

        std::string getHostname() const {
            return hostname_;
        }
        void setHostname(const std::string &hostname) {
            hostname_ = hostname;
        }

        std::string getAlias() const {
            return alias_;
        }

        void setAlias(const std::string &alias) {
            alias_ = alias;
        }

        std::vector<std::map<std::string, std::string> >
        getAllVideoCodecs() const;

        std::vector<std::map<std::string, std::string> >
        getActiveVideoCodecs() const;

         /* Accessor to data structures
         * @return CodecOrder& The list that reflects the user's choice
         */
        std::vector<int> getActiveAudioCodecs() const {
            return audioCodecList_;
        }

        /**
         * Update both the codec order structure and the codec string used for
         * SDP offer and configuration respectively
         */
        void setActiveAudioCodecs(const std::vector<std::string>& list);
        void setVideoCodecs(const std::vector<std::map<std::string, std::string> > &codecs);

        std::string getRingtonePath() const {
            return ringtonePath_;
        }
        void setRingtonePath(const std::string &path) {
            ringtonePath_ = path;
        }

        bool getRingtoneEnabled() const {
            return ringtoneEnabled_;
        }
        void setRingtoneEnabled(bool enable) {
            ringtoneEnabled_ = enable;
        }

        std::string getDisplayName() const {
            return displayName_;
        }
        void setDisplayName(const std::string &name) {
            displayName_ = name;
        }

        std::string getMailBox() const {
            return mailBox_;
        }

        void setMailBox(const std::string &mb) {
            mailBox_ = mb;
        }

        static const char * const VIDEO_CODEC_ENABLED;
        static const char * const VIDEO_CODEC_NAME;
        static const char * const VIDEO_CODEC_BITRATE;
    private:
        NON_COPYABLE(Account);

        /**
         * Helper function used to load the default codec order from the codec factory
         */
        void loadDefaultCodecs();

    protected:
        friend class ConfigurationTest;
        // General configuration keys for accounts
        static const char * const AUDIO_CODECS_KEY;
        static const char * const VIDEO_CODECS_KEY;
        static const char * const RINGTONE_PATH_KEY;
        static const char * const RINGTONE_ENABLED_KEY;
        static const char * const DISPLAY_NAME_KEY;
        static const char * const ALIAS_KEY;
        static const char * const TYPE_KEY;
        static const char * const ID_KEY;
        static const char * const USERNAME_KEY;
        static const char * const AUTHENTICATION_USERNAME_KEY;
        static const char * const PASSWORD_KEY;
        static const char * const HOSTNAME_KEY;
        static const char * const ACCOUNT_ENABLE_KEY;
        static const char * const MAILBOX_KEY;

        static std::string mapStateNumberToString(RegistrationState state);

        /**
         * Account ID are assign in constructor and shall not changed
         */
        const std::string accountID_;

        /**
         * Account login information: username
         */
        std::string username_;

        /**
         * Account login information: hostname
         */
        std::string hostname_;

        /**
         * Account login information: Alias
         */
        std::string alias_;

        /**
         * Tells if the link is enabled, active.
         * This implies the link will be initialized on startup.
         * Modified by the configuration (key: ENABLED)
         */
        bool enabled_;

        /*
         * The account type
         * IAX2 or SIP
         */
        std::string type_;

        /*
         * The general, protocol neutral registration
         * state of the account
         */
        RegistrationState registrationState_;

        /**
         * Vector containing the order of the codecs
         */
        std::vector<int> audioCodecList_;

        /**
         * Vector containing the video codecs in order
         */
        std::vector<std::map<std::string, std::string> > videoCodecList_;

        /**
         * List of audio codecs obtained when parsing configuration and used
         * to generate codec order list
         */
        std::string audioCodecStr_;

        /**
         * Ringtone .au file used for this account
         */
        std::string ringtonePath_;

        /**
         * Play ringtone when receiving a call
         */
        bool ringtoneEnabled_;

        /**
         * Display name when calling
         */
        std::string displayName_;

        /**
         * Useragent used for registration
         */
        std::string userAgent_;

        /**
             * Account mail box
         */
        std::string mailBox_;

};

#endif
