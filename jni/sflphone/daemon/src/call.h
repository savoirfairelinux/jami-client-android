/*
 *  Copyright (C) 2004, 2005, 2006, 2008, 2009, 2010, 2011 Savoir-Faire Linux Inc.
 *  Author: Emmanuel Milou <emmanuel.milou@savoirfairelinux.com>
 *  Author: Yan Morin <yan.morin@savoirfairelinux.com>
 *  Author : Laurielle Lea <laurielle.lea@savoirfairelinux.com>
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
#ifndef __CALL_H__
#define __CALL_H__

#include <sstream>
#include <map>
#include "cc_thread.h"
#include "audio/recordable.h"

/*
 * @file call.h
 * @brief A call is the base class for protocol-based calls
 */

class Call : public Recordable {
    public:
        static const char * const DEFAULT_ID;

        /**
         * This determines if the call originated from the local user (OUTGOING)
         * or from some remote peer (INCOMING, MISSED).
         */
        enum CallType {INCOMING, OUTGOING, MISSED};

        /**
         * Tell where we're at with the call. The call gets Connected when we know
         * from the other end what happened with out call. A call can be 'Connected'
         * even if the call state is Busy, Refused, or Error.
         *
         * Audio should be transmitted when ConnectionState = Connected AND
         * CallState = Active.
         */
        enum ConnectionState {DISCONNECTED, TRYING, PROGRESSING, RINGING, CONNECTED};

        /**
         * The Call State.
         */
        enum CallState {INACTIVE, ACTIVE, HOLD, BUSY, CONFERENCING, REFUSED, ERROR};

        /**
         * Constructor of a call
         * @param id Unique identifier of the call
         * @param type set definitely this call as incoming/outgoing
         */
        Call(const std::string& id, Call::CallType type);
        virtual ~Call();

        /**
         * Return a copy of the call id
         * @return call id
         */
        std::string getCallId() const {
            return id_;
        }

        /**
         * Return a reference on the conference id
         * @return call id
         */
        std::string getConfId() const {
            return confID_;
        }

        void setConfId(const std::string &id) {
            confID_ = id;
        }

        CallType getCallType() const {
            return type_;
        }

        /**
         * Set the peer number (destination on outgoing)
         * not protected by mutex (when created)
         * @param number peer number
         */
        void setPeerNumber(const std::string& number) {
            peerNumber_ = number;
        }

        /**
         * Get the peer number (destination on outgoing)
         * not protected by mutex (when created)
         * @return std::string The peer number
         */
        std::string getPeerNumber() const {
            return peerNumber_;
        }

        /**
         * Set the display name (caller in ingoing)
         * not protected by mutex (when created)
         * @return std::string The peer display name
         */
        void setDisplayName(const std::string& name) {
            displayName_ = name;
        }

        /**
         * Get the peer display name (caller in ingoing)
         * not protected by mutex (when created)
         * @return std::string The peer name
         */
        const std::string& getDisplayName() const {
            return displayName_;
        }

        /**
         * Tell if the call is incoming
         * @return true if yes false otherwise
         */
        bool isIncoming() const {
            return type_ == INCOMING;
        }

        /**
         * Set the connection state of the call (protected by mutex)
         * @param state The connection state
         */
        void setConnectionState(ConnectionState state);

        /**
         * Get the connection state of the call (protected by mutex)
         * @return ConnectionState The connection state
         */
        ConnectionState getConnectionState();

        /**
         * Set the state of the call (protected by mutex)
         * @param state The call state
         */
        void setState(CallState state);

        /**
         * Get the call state of the call (protected by mutex)
         * @return CallState  The call state
         */
        CallState getState();

        std::string getStateStr();

        void setIPToIP(bool IPToIP) {
            isIPToIP_ = IPToIP;
        }

        virtual void answer() = 0;

        /**
         * Set my IP [not protected]
         * @param ip  The local IP address
         */
        void setLocalIp(const std::string& ip) {
            localIPAddress_ = ip;
        }

        /**
         * Set local audio port, as seen by me [not protected]
         * @param port  The local audio port
         */
        void setLocalAudioPort(unsigned int port) {
            localAudioPort_ = port;
        }

        /**
         * Set local video port, as seen by me [not protected]
         * @param port  The local video port
         */
        void setLocalVideoPort(unsigned int port)  {
            localVideoPort_ = port;
        }

        /**
         * Return my IP [mutex protected]
         * @return std::string The local IP
         */
        std::string getLocalIp();

        /**
         * Return port used locally (for my machine) [mutex protected]
         * @return unsigned int  The local audio port
         */
        unsigned int getLocalAudioPort();

        /**
         * Return port used locally (for my machine) [mutex protected]
         * @return unsigned int  The local video port
         */
        unsigned int getLocalVideoPort();

        void time_stop();
        std::map<std::string, std::string> getDetails();
        static std::map<std::string, std::string> getNullDetails();
        std::map<std::string, std::string> createHistoryEntry() const;
        virtual bool setRecording();

    private:
        std::string getTypeStr() const;
        /** Protect every attribute that can be changed by two threads */
        ost::Mutex callMutex_;

        // Informations about call socket / audio

        /** My IP address */
        std::string localIPAddress_;

        /** Local audio port, as seen by me. */
        unsigned int localAudioPort_;

        /** Local video port, as seen by me. */
        unsigned int localVideoPort_;

        /** Unique ID of the call */
        std::string id_;

        /** Unique conference ID, used exclusively in case of a conferece */
        std::string confID_;

        /** Type of the call */
        CallType type_;

        /** Disconnected/Progressing/Trying/Ringing/Connected */
        ConnectionState connectionState_;

        /** Inactive/Active/Hold/Busy/Refused/Error */
        CallState callState_;

        /** Direct IP-to-IP or classic call */
        bool isIPToIP_;

        /** Number of the peer */
        std::string peerNumber_;

        /** Display Name */
        std::string displayName_;

        time_t timestamp_start_;
        time_t timestamp_stop_;
};

#endif // __CALL_H__
