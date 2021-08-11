/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *          Rayan Osseiran <rayan.osseiran@savoirfairelinux.com>
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
package net.jami.model;

public class ContactEvent extends Interaction {

    public TrustRequest request;
    public Event event;

    public ContactEvent(Interaction interaction) {
        mId = interaction.getId();
        mConversation = interaction.getConversation();
        mAuthor = interaction.getAuthor();
        mType = InteractionType.CONTACT.toString();
        mTimestamp = interaction.getTimestamp();
        mStatus = interaction.getStatus().toString();
        mIsRead = 1;
        mContact = interaction.getContact();
        event = getEventFromStatus(interaction.getStatus());
    }

    public ContactEvent() {
        mAuthor = null;
        event = Event.ADDED;
        mType = InteractionType.CONTACT.toString();
        mTimestamp = System.currentTimeMillis();
        mStatus = InteractionStatus.SUCCESS.toString();
        mIsRead = 1;
    }

    public ContactEvent(Contact contact) {
        mContact = contact;
        mAuthor = contact.getUri().getUri();
        mType = InteractionType.CONTACT.toString();
        event = Event.ADDED;
        mStatus = InteractionStatus.SUCCESS.toString();
        mTimestamp = contact.getAddedDate().getTime();
        mIsRead = 1;
    }

    public ContactEvent(Contact contact, TrustRequest request) {
        this.request = request;
        mContact = contact;
        mAuthor = contact.getUri().getUri();
        mTimestamp = request.getTimestamp();
        mType = InteractionType.CONTACT.toString();
        event = Event.INCOMING_REQUEST;
        mStatus = InteractionStatus.UNKNOWN.toString();
        mIsRead = 1;
    }

    public enum Event {
        UNKNOWN,
        INCOMING_REQUEST,
        INVITED,
        ADDED,
        REMOVED,
        BANNED;

        public static Event fromConversationAction(String action) {
            switch (action) {
                case "add": return INVITED;
                case "join": return ADDED;
                case "remove" : return REMOVED;
                case "ban": return BANNED;
                default: return UNKNOWN;
            }
        }
    }

    public ContactEvent setEvent(Event event) {
        this.event = event;
        return this;
    }

    public void setRequest(TrustRequest request) {
        this.request = request;
    }

    private Event getEventFromStatus(InteractionStatus status) {
        // success for added contacts
        if (status == InteractionStatus.SUCCESS)
            return Event.ADDED;
        // storage is unknown status for trust requests
        else if (status == InteractionStatus.UNKNOWN)
            return Event.INCOMING_REQUEST;

        return Event.UNKNOWN;
    }

    public void setTimestamp(long timestamp) {
        mTimestamp = timestamp;
    }


}
