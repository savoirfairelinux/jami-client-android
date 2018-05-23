/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
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
import cx.ring.model.ConversationElement;

public class SmartListViewModel
{
    private String uuid;
    private String contactName;
    private byte[] photoData;
    private boolean hasUnreadTextMessage;
    private boolean hasOngoingCall;
    private CallContact.Status status;
    private boolean isOnline = false;
    private ConversationElement lastEvent;

    public SmartListViewModel(String id, CallContact.Status status, String contactName, byte[] photoData, ConversationElement lastEvent) {
        this.uuid = id;
        this.contactName = contactName;
        this.photoData = photoData;
        this.hasUnreadTextMessage = !lastEvent.isRead();
        this.hasOngoingCall = false;
        this.status = status;
        this.lastEvent = lastEvent;
    }

    public SmartListViewModel(SmartListViewModel smartListViewModel) {
        this.uuid = smartListViewModel.getUuid();
        this.contactName = smartListViewModel.getContactName();
        this.photoData = smartListViewModel.getPhotoData();
        this.hasUnreadTextMessage = smartListViewModel.hasUnreadTextMessage();
        this.hasOngoingCall = smartListViewModel.hasOngoingCall();
        this.status = smartListViewModel.getStatus();
    }

    public String getContactName() {
        return contactName;
    }

    public long getLastInteractionTime() {
        return lastEvent.getDate();
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

    public byte[] getPhotoData() {
        return photoData;
    }

    public CallContact.Status getStatus() {
        return status;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public void setHasOngoingCall(boolean hasOngoingCall) {
        this.hasOngoingCall = hasOngoingCall;
    }

    public ConversationElement getLastEvent() {
        return lastEvent;
    }
}
