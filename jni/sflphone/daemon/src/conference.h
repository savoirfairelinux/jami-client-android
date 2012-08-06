/*
 *  Copyright (C) 2004, 2005, 2006, 2008, 2009, 2010, 2011 Savoir-Faire Linux Inc.
 *  Author: Alexandre Savard  <alexandre.savard@savoirfairelinux.com>
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
#ifndef CONFERENCE_H
#define CONFERENCE_H

#include <set>
#include <string>

#include "audio/recordable.h"

typedef std::set<std::string> ParticipantSet;

class Conference : public Recordable {
    public:
        enum ConferenceState {ACTIVE_ATTACHED, ACTIVE_DETACHED, ACTIVE_ATTACHED_REC, ACTIVE_DETACHED_REC, HOLD, HOLD_REC};

        /**
         * Constructor for this class, increment static counter
         */
        Conference();

        /**
         * Return the conference id
         */
        std::string getConfID() const;

        /**
         * Return the current conference state
         */
        ConferenceState getState() const;

        /**
         * Set conference state
         */
        void setState(ConferenceState state);

        /**
         * Return a string description of the conference state
         */
        std::string getStateStr() const;

        /**
         * Add a new participant to the conference
         */
        void add(const std::string &participant_id);

        /**
         * Remove a participant from the conference
         */
        void remove(const std::string &participant_id);

        /**
         * Bind a participant to the conference
         */
        void bindParticipant(const std::string &participant_id);

        /**
         * Get the participant list for this conference
         */
        ParticipantSet getParticipantList() const;

        /**
         * Start/stop recording toggle
         */
        virtual bool setRecording();
    private:
        std::string id_;
        ConferenceState confState_;
        ParticipantSet participants_;
};

#endif
