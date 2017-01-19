/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
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

import java.util.ArrayList;
import java.util.Date;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;

public class HistoryEntry {

    private CallContact mContact;
    private final NavigableMap<Long, HistoryCall> mCalls = new TreeMap<>();
    private final NavigableMap<Long, TextMessage> mTextMessages = new TreeMap<>();
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

    public SortedMap<Long, TextMessage> getTextMessages(long since) {
        return mTextMessages.tailMap(since);
    }

    public CallContact getContact() {
        return mContact;
    }

    public void setContact(CallContact contact) {
        this.mContact = contact;
    }

    /**
     * Each call is associated with a mContact.
     * When adding a call to an HIstoryEntry, this methods also verifies if we can update
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
        mTextMessages.put(text.getTimestamp(), text);
        if (mContact.isUnknown() && !text.getContact().isUnknown()) {
            setContact(text.getContact());
        }
    }

    public String getNumber() {
        return mCalls.lastEntry().getValue().number;
    }

    public String getTotalDuration() {
        int duration = 0;
        ArrayList<HistoryCall> allCalls = new ArrayList<>(mCalls.values());
        for (HistoryCall call : allCalls) {
            duration += call.getDuration();
        }

        if (duration < 60) {
            return duration + "s";
        }

        return duration / 60 + "min";
    }

    public Date getLastCallDate() {
        return new Date(mCalls.isEmpty() ? 0 : mCalls.lastEntry().getKey());
    }

    public Date getLastTextDate() {
        return new Date(mTextMessages.isEmpty() ? 0 : mTextMessages.lastEntry().getKey());
    }

    public Date getLastInteractionDate() {
        return new Date(Math.max(mCalls.isEmpty() ? 0 : mCalls.lastEntry().getKey(), mTextMessages.isEmpty() ? 0 : mTextMessages.lastEntry().getKey()));
    }

    public HistoryCall getLastOutgoingCall() {
        for (HistoryCall c : mCalls.descendingMap().values()) {
            if (!c.isIncoming()) {
                return c;
            }
        }
        return null;
    }

    public TextMessage getLastOutgoingText() {
        for (TextMessage c : mTextMessages.descendingMap().values()) {
            if (c.isOutgoing()) {
                return c;
            }
        }
        return null;
    }

    public HistoryCall getLastIncomingCall() {
        for (HistoryCall c : mCalls.descendingMap().values()) {
            if (c.isIncoming()) {
                return c;
            }
        }
        return null;
    }

    public TextMessage getLastIncomingText() {
        for (TextMessage c : mTextMessages.descendingMap().values()) {
            if (c.isIncoming()) {
                return c;
            }
        }
        return null;
    }

    public String getLastNumberUsed() {
        HistoryCall call = getLastOutgoingCall();
        TextMessage text = getLastOutgoingText();
        if (call == null && text == null) {
            call = getLastIncomingCall();
            text = getLastIncomingText();
            if (call == null && text == null) {
                return null;
            }
        }
        if (call == null) {
            return text.getNumber();
        }
        if (text == null) {
            return call.getNumber();
        }
        if (call.call_start < text.getTimestamp()) {
            return text.getNumber();
        } else {
            return call.getNumber();
        }
    }
}
