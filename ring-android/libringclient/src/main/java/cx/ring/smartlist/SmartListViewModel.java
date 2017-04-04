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
    private String contactName;
    private String lastInteraction = "";
    private byte[] photoData;
    private String lastInteractionTime;
    private boolean hasUnreadTextMessage;
    private boolean hasOngoingCall;
    private int lastEntryType;

    public SmartListViewModel(Conversation conversation, String contactName, String photoUri, byte[] photoData, String lastInteractionTime) {
        this.uuid = conversation.getUuid();
        this.contactName = contactName;
        this.photoData = photoData;
        this.lastInteractionTime = lastInteractionTime;
        this.hasUnreadTextMessage = conversation.hasUnreadTextMessages();
        this.hasOngoingCall = conversation.hasCurrentCall();

        for (HistoryEntry historyEntry : conversation.getHistory().values()) {
            long lastTextTimestamp = historyEntry.getTextMessages().isEmpty() ? 0 : historyEntry.getTextMessages().lastEntry().getKey();
            long lastCallTimestamp = historyEntry.getCalls().isEmpty() ? 0 : historyEntry.getCalls().lastEntry().getKey();
            if (lastTextTimestamp == conversation.getLastInteraction().getTime()
                    && lastTextTimestamp > 0
                    && lastTextTimestamp > lastCallTimestamp) {
                TextMessage msg = historyEntry.getTextMessages().lastEntry().getValue();
                String msgString = msg.getMessage();
                if (msgString != null && !msgString.isEmpty() && msgString.contains("\n")) {
                    int lastIndexOfChar = msgString.lastIndexOf("\n");
                    if (lastIndexOfChar + 1 < msgString.length()) {
                        msgString = msgString.substring(msgString.lastIndexOf("\n") + 1);
                    }
                }
                this.lastEntryType = msg.isIncoming() ? TYPE_INCOMING_MESSAGE : TYPE_OUTGOING_MESSAGE;
                this.lastInteraction = msgString;
                break;
            }
            if (lastCallTimestamp == conversation.getLastInteraction().getTime()
                    && lastCallTimestamp > 0) {
                HistoryCall lastCall = historyEntry.getCalls().lastEntry().getValue();
                this.lastEntryType = lastCall.isIncoming() ? TYPE_INCOMING_CALL : TYPE_OUTGOING_CALL;
                this.lastInteraction = lastCall.getDurationString();
                break;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SmartListViewModel) {
            SmartListViewModel slvm = (SmartListViewModel) o;
            return !(this.photoData != null && !Arrays.equals(this.photoData, slvm.photoData))
                    && this.uuid.equals(slvm.getUuid())
                    && this.contactName.equals(slvm.getContactName())
                    && this.lastInteraction.equals(slvm.getLastInteraction())
                    && this.lastInteractionTime.equals(slvm.getLastInteractionTime())
                    && this.hasUnreadTextMessage == slvm.hasUnreadTextMessage()
                    && this.hasOngoingCall == slvm.hasOngoingCall()
                    && this.lastEntryType == slvm.getLastEntryType();
        } else {
            return false;
        }
    }

    public String getContactName() {
        return contactName;
    }

    public String getLastInteraction() {
        return lastInteraction;
    }

    public String getLastInteractionTime() {
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

    public int getLastEntryType() {
        return lastEntryType;
    }
}
