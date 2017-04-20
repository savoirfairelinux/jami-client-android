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

import cx.ring.model.Conversation;
import cx.ring.model.HistoryEntry;

public class SmartListViewModel {

    private String uuid;
    private String contactName;
    private HistoryEntry lastInteraction;
    private String photoUri;
    private byte[] photoData;
    private long lastInteractionTime;
    private boolean hasUnreadTextMessage;
    private boolean hasOngoingCall;
    private boolean isOnline = false;

    public SmartListViewModel(Conversation conversation, String contactName, String photoUri, byte[] photoData) {
        this.uuid = conversation.getUuid();
        setData(conversation, contactName, photoUri, photoData);
    }

    public void update(Conversation conversation, String contactName, String photoUri, byte[] photoData) {
        setData(conversation, contactName, photoUri, photoData);
    }

    private void setData(Conversation conversation, String contactName, String photoUri, byte[] photoData) {
        this.contactName = contactName;
        this.photoUri = photoUri;
        this.photoData = photoData;

        for (HistoryEntry historyEntry : conversation.getHistory().values()) {
            long lastTextTimestamp = historyEntry.getTextMessages().isEmpty() ? 0 : historyEntry.getTextMessages().lastEntry().getKey();
            long lastCallTimestamp = historyEntry.getCalls().isEmpty() ? 0 : historyEntry.getCalls().lastEntry().getKey();
            if (lastTextTimestamp > 0 && lastTextTimestamp > lastCallTimestamp) {
                this.lastInteraction = historyEntry;
                break;
            }
            if (lastCallTimestamp > 0) {
                this.lastInteraction = historyEntry;
                break;
            }
        }

        this.lastInteractionTime = conversation.getLastInteraction().getTime();
        this.hasUnreadTextMessage = conversation.hasUnreadTextMessages();
        this.hasOngoingCall = conversation.hasCurrentCall();
        this.isOnline = conversation.getContact().isOnline();
    }

    public String getContactName() {
        return contactName;
    }

    public HistoryEntry getLastInteraction() {
        return lastInteraction;
    }

    public String getPhotoUri() {
        return photoUri;
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

    public boolean getStatus() {
        return isOnline;
    }
}
