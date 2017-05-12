/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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

import java.util.Arrays;
import java.util.Comparator;

import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.model.HistoryCall;
import cx.ring.model.HistoryEntry;
import cx.ring.model.TextMessage;

public class SmartListViewModel {

    public static final int TYPE_INCOMING_MESSAGE = 0;
    public static final int TYPE_OUTGOING_MESSAGE = 1;
    public static final int TYPE_INCOMING_CALL = 2;
    public static final int TYPE_OUTGOING_CALL = 3;

    private String uuid;
    private long conversationId;
    private String contactName;
    private String lastInteraction = "";
    private byte[] photoData;
    private long lastInteractionTime;
    private boolean hasUnreadTextMessage;
    private boolean hasOngoingCall;
    private CallContact.Status status;
    private boolean isOnline = false;
    private int lastEntryType;


    public SmartListViewModel(String id,
                              long conversationId,
                              CallContact.Status status,
                              String contactName,
                              byte[] photoData,
                              long lastInteractionTime,
                              int lastEntrytype,
                              String lastInteraction,
                              boolean hasUnreadTextMessage) {
        this.uuid = id;
        this.conversationId = conversationId;
        this.contactName = contactName;
        this.photoData = photoData;
        this.lastInteractionTime = lastInteractionTime;
        this.hasUnreadTextMessage = hasUnreadTextMessage;
        this.hasOngoingCall = false;
        this.status = status;
        this.lastEntryType = lastEntrytype;
        this.lastInteraction = lastInteraction;
    }


    public SmartListViewModel(SmartListViewModel smartListViewModel) {
        this.uuid = smartListViewModel.getUuid();
        this.contactName = smartListViewModel.getContactName();
        this.photoData = smartListViewModel.getPhotoData();
        this.lastInteractionTime = smartListViewModel.getLastInteractionTime();
        this.hasUnreadTextMessage = smartListViewModel.hasUnreadTextMessage();
        this.hasOngoingCall = smartListViewModel.hasOngoingCall();
        this.status = smartListViewModel.getStatus();
        this.lastEntryType = smartListViewModel.getLastEntryType();
        this.lastInteraction = smartListViewModel.getLastInteraction();
    }

    public SmartListViewModel(String id, CallContact callContact, String contactName, byte[] photoData) {
        this.uuid = id;
        this.conversationId = -1;
        this.contactName = contactName;
        this.photoData = photoData;
        this.lastInteractionTime = 0;
        this.hasUnreadTextMessage = false;
        this.hasOngoingCall = false;
        this.status = callContact.getStatus();
        this.lastEntryType = 0;
        this.lastInteraction = "";
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SmartListViewModel) {
            SmartListViewModel slvm = (SmartListViewModel) o;
            return !(this.photoData != null && !Arrays.equals(this.photoData, slvm.photoData))
                    && this.uuid.equals(slvm.getUuid())
                    && this.contactName.equals(slvm.getContactName())
                    && this.lastInteraction.equals(slvm.getLastInteraction())
                    && this.lastInteractionTime == slvm.getLastInteractionTime()
                    && this.hasUnreadTextMessage == slvm.hasUnreadTextMessage()
                    && this.hasOngoingCall == slvm.hasOngoingCall()
                    && this.lastEntryType == slvm.getLastEntryType()
                    && this.isOnline == slvm.isOnline()
                    && this.status == slvm.getStatus();
        } else {
            return false;
        }
    }

    public static class SmartListComparator implements Comparator<SmartListViewModel> {
        @Override
        public int compare(SmartListViewModel lhs, SmartListViewModel rhs) {
            if (rhs.getLastInteractionTime() != lhs.getLastInteractionTime()) {
                return (int) ((rhs.getLastInteractionTime() - lhs.getLastInteractionTime()) / 1000L);
            } else {
                return (rhs.getContactName().compareTo(lhs.getContactName()));
            }
        }
    }

    public String getContactName() {
        return contactName;
    }

    public String getLastInteraction() {
        return lastInteraction;
    }

    public long getLastInteractionTime() {
        return lastInteractionTime;
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

    public int getLastEntryType() {
        return lastEntryType;
    }

    public long getConversationId() {
        return conversationId;
    }
}
