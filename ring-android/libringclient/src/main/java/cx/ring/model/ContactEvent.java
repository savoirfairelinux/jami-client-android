/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
package cx.ring.model;

public class ContactEvent implements ConversationElement {
    public enum Event {
        INCOMING_REQUEST,
        ADDED,
        REMOVED,
        BANNED
    };
    public CallContact contact;
    public TrustRequest request;
    public Event event;

    ContactEvent(CallContact contact) {
        this.contact = contact;
        event = Event.ADDED;
    }

    ContactEvent(CallContact contact, TrustRequest request) {
        this.contact = contact;
        this.request = request;
        event = Event.INCOMING_REQUEST;
    }

    @Override
    public CEType getType() {
        return CEType.CONTACT;
    }

    @Override
    public long getDate() {
        return (event == Event.ADDED) ? contact.getAddedDate().getTime() : request.getTimestamp();
    }

    @Override
    public Uri getContactNumber() {
        return contact.getPrimaryUri();
    }

    @Override
    public boolean isRead() {
        return true;
    }

    @Override
    public long getId() {
        return contact.getAddedDate().getTime();
    }
}
