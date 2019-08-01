/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.smartlist;

import cx.ring.model.CallContact;
import cx.ring.model.Interaction;

public class SmartListViewModel
{
    private final String accountId;
    private final CallContact contact;
    private final String uuid;
    private final String contactName;
    private final boolean hasUnreadTextMessage;
    private boolean hasOngoingCall;
    private boolean isOnline = false;
    private final Interaction lastEvent;

    public SmartListViewModel(String accountId, CallContact contact, String id, Interaction lastEvent) {
        this.accountId = accountId;
        this.contact = contact;
        this.uuid = id;
        this.contactName = contact.getDisplayName();
        hasUnreadTextMessage = (lastEvent != null) && !lastEvent.isRead();
        this.hasOngoingCall = false;
        this.lastEvent = lastEvent;
        isOnline = contact.isOnline();
    }
    public SmartListViewModel(String accountId, CallContact contact, Interaction lastEvent) {
        this.accountId = accountId;
        this.contact = contact;
        this.uuid = contact.getIds().get(0);
        this.contactName = contact.getDisplayName();
        hasUnreadTextMessage = (lastEvent != null) && !lastEvent.isRead();
        this.hasOngoingCall = false;
        this.lastEvent = lastEvent;
        isOnline = contact.isOnline();
    }

    public CallContact getContact() {
        return contact;
    }

    public String getContactName() {
        return contactName;
    }

    public long getLastInteractionTime() {
        return (lastEvent == null) ? 0 : lastEvent.getTimestamp();
    }

    public boolean hasUnreadTextMessage() {
        return hasUnreadTextMessage;
    }

    public boolean hasOngoingCall() {
        return hasOngoingCall;
    }

    public String getUuid() {
        return uuid;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public void setHasOngoingCall(boolean hasOngoingCall) {
        this.hasOngoingCall = hasOngoingCall;
    }

    public Interaction getLastEvent() {
        return lastEvent;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SmartListViewModel))
            return false;
        SmartListViewModel other = (SmartListViewModel) o;
        return contact == other.contact
                && contactName.equals(other.contactName)
                && isOnline == other.isOnline
                && lastEvent == other.lastEvent
                && hasOngoingCall == other.hasOngoingCall
                && hasUnreadTextMessage == other.hasUnreadTextMessage;
    }

    public String getAccountId() {
        return accountId;
    }
}
