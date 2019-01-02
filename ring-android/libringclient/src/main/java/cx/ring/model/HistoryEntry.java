/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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

import java.util.Date;
import java.util.NavigableMap;
import java.util.TreeMap;

public class HistoryEntry {

    private CallContact mContact;
    private final NavigableMap<Long, HistoryCall> mCalls = new TreeMap<>();
    private final NavigableMap<Long, TextMessage> mTextMessages = new TreeMap<>();
    private final NavigableMap<Long, DataTransfer> mDataTransfers = new TreeMap<>();
    private String mAccountId;
    int mMissedCount;
    int mOutgoingCount;
    int mIncomingCount;

    public HistoryEntry(String account, CallContact c) {
        mContact = c;
        mAccountId = account;
        mMissedCount = mOutgoingCount = mIncomingCount = 0;
    }

    public String getAccountID() {
        return mAccountId;
    }

    public void setAccountID(String accountID) {
        this.mAccountId = accountID;
    }

    public NavigableMap<Long, HistoryCall> getCalls() {
        return mCalls;
    }

    public NavigableMap<Long, TextMessage> getTextMessages() {
        return mTextMessages;
    }

    public NavigableMap<Long, DataTransfer> getDataTransfers() {
        return mDataTransfers;
    }

    public CallContact getContact() {
        return mContact;
    }

    public void setContact(CallContact contact) {
        this.mContact = contact;
    }

    /**
     * Each call is associated with a mContact.
     * When adding a call to an HistoryEntry, this methods also verifies if we can update
     * the mContact (if mContact is Unknown, replace it)
     *
     * @param historyCall The call to put in this HistoryEntry
     * @param linkedTo    The associated CallContact
     */
    public void addHistoryCall(HistoryCall historyCall, CallContact linkedTo) {
        mCalls.put(historyCall.call_end, historyCall);
        if (historyCall.isIncoming()) {
            ++mIncomingCount;
        } else {
            ++mOutgoingCount;
        }
        if (historyCall.isMissed()) {
            mMissedCount++;
        }

        if (mContact.isUnknown() && !linkedTo.isUnknown()) {
            setContact(linkedTo);
        }
    }

    public void addTextMessage(TextMessage text) {
        mTextMessages.put(text.getDate(), text);
        if (mContact.isUnknown() && !text.getContact().isUnknown()) {
            setContact(text.getContact());
        }
    }

    public void addDatatransfer(DataTransfer dataTransfer) {
        mDataTransfers.put(dataTransfer.getTimestamp(), dataTransfer);
    }

    public TextMessage updateTextMessage(TextMessage text) {
        long time = text.getDate();
        NavigableMap<Long, TextMessage> msgs = mTextMessages.subMap(time, true, time, true);
        for (TextMessage txt : msgs.values()) {
            if (txt.equals(text)) {
                txt.setStatus(text.getStatus());
                return txt;
            }
        }
        return null;
    }

    public String getNumber() {
        return mCalls.lastEntry().getValue().number;
    }

    public Date getLastInteractionDate() {
        long lastCall = mCalls.isEmpty() ? 0 : mCalls.lastEntry().getKey();
        long lastTextMessage = mTextMessages.isEmpty() ? 0 : mTextMessages.lastEntry().getKey();
        long lastInteraction = Math.max(lastCall, lastTextMessage);
        long lastDataTransfer = mDataTransfers.isEmpty() ? 0 : mDataTransfers.lastEntry().getKey();
        lastInteraction = Math.max(lastInteraction, lastDataTransfer);
        return new Date(lastInteraction);
    }
}
