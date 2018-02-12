/*
 *  Copyright (C) 2015-2016 Savoir-faire Linux Inc.
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import cx.ring.utils.Log;

public class Conversation {

    private static final String TAG = Conversation.class.getSimpleName();

    private CallContact mContact;

    private final Map<String, HistoryEntry> mHistory;
    private final ArrayList<Conference> mCurrentCalls;
    private final ArrayList<ConversationElement> mAggregateHistory;

    // runtime flag set to true if the user is currently viewing this conversation
    private boolean mVisible = false;

    public Conversation(CallContact contact) {
        setContact(contact);
        mHistory = new HashMap<>();
        mCurrentCalls = new ArrayList<>();
        mAggregateHistory = new ArrayList<>(32);
    }

    public Conference getConference(String id) {
        for (Conference c : mCurrentCalls)
            if (c.getId().contentEquals(id) || c.getCallById(id) != null) {
                return c;
            }
        return null;
    }

    public void addConference(final Conference conference) {
        if (conference == null) {
            return;
        }
        for (int i = 0; i < mCurrentCalls.size(); i++) {
            final Conference currentConference = mCurrentCalls.get(i);
            if (currentConference == conference) {
                return;
            }
            if (currentConference.getId().equals(conference.getId())) {
                mCurrentCalls.set(i, conference);
                return;
            }
        }
        mCurrentCalls.add(conference);
    }

    public void removeConference(Conference c) {
        mCurrentCalls.remove(c);
    }

    public void setContact(CallContact contact) {
        mContact = contact;
    }

    public boolean isVisible() {
        return mVisible;
    }

    public void setVisible(boolean mVisible) {
        this.mVisible = mVisible;
    }

    public CallContact getContact() {
        return mContact;
    }

    public Date getLastInteraction() {
        if (!mCurrentCalls.isEmpty()) {
            return new Date();
        }
        Date d = new Date(0);

        for (HistoryEntry e : mHistory.values()) {
            Date nd = e.getLastInteractionDate();
            if (d.compareTo(nd) < 0) {
                d = nd;
            }
        }
        Date added = mContact.getAddedDate();
        if (added != null) {
            if (d.compareTo(added) < 0) {
                d = added;
            }
        }
        return d;
    }

    public void addHistoryCall(HistoryCall call) {
        if (getHistoryCalls().contains(call)) {
            return;
        }
        String accountId = call.getAccountID();
        if (mHistory.containsKey(accountId)) {
            mHistory.get(accountId).addHistoryCall(call, getContact());
        } else {
            HistoryEntry entry = new HistoryEntry(accountId, getContact());
            entry.addHistoryCall(call, getContact());
            mHistory.put(accountId, entry);
        }
        mAggregateHistory.add(new ConversationElement(call));
    }

    public void addTextMessage(TextMessage txt) {
        if (mVisible) {
            txt.read();
        }
        if (txt.getContact() == null) {
            txt.setContact(getContact());
        }
        String accountId = txt.getAccount();
        HistoryEntry accountEntry = mHistory.get(accountId);
        if (accountEntry != null) {
            accountEntry.addTextMessage(txt);
        } else {
            accountEntry = new HistoryEntry(accountId, getContact());
            accountEntry.addTextMessage(txt);
            mHistory.put(accountId, accountEntry);
        }
        mAggregateHistory.add(new ConversationElement(txt));
    }

    public void updateTextMessage(TextMessage txt) {
        HistoryEntry accountEntry = mHistory.get(txt.getAccount());
        if (accountEntry != null) {
            accountEntry.updateTextMessage(txt);
        }
    }

    public Map<String, HistoryEntry> getHistory() {
        return mHistory;
    }

    public ArrayList<ConversationElement> getAggregateHistory() {
        Collections.sort(mAggregateHistory, new Comparator<ConversationElement>() {
            @Override
            public int compare(ConversationElement lhs, ConversationElement rhs) {
                return (int) ((lhs.getDate() - rhs.getDate()) / 1000L);
            }
        });
        return mAggregateHistory;
    }

    public Set<String> getAccountsUsed() {
        return mHistory.keySet();
    }

    public String getLastAccountUsed() {
        String last = null;
        Date d = new Date(0);
        for (Map.Entry<String, HistoryEntry> e : mHistory.entrySet()) {
            Date nd = e.getValue().getLastInteractionDate();
            if (d.compareTo(nd) < 0) {
                d = nd;
                last = e.getKey();
            }
        }
        Log.i(TAG, "getLastAccountUsed " + last);
        return last;
    }

    public Conference getCurrentCall() {
        if (mCurrentCalls.isEmpty()) {
            return null;
        }
        return mCurrentCalls.get(0);
    }

    public ArrayList<Conference> getCurrentCalls() {
        return mCurrentCalls;
    }

    public Collection<HistoryCall> getHistoryCalls() {
        TreeMap<Long, HistoryCall> calls = new TreeMap<>();

        for (HistoryEntry historyEntry : mHistory.values()) {
            for (Map.Entry<Long, HistoryCall> entry : historyEntry.getCalls().descendingMap().entrySet()) {
                calls.put(entry.getKey(), entry.getValue());
            }
        }
        return calls.values();
    }

    public TreeMap<Long, TextMessage> getUnreadTextMessages() {
        TreeMap<Long, TextMessage> texts = new TreeMap<>();
        for (HistoryEntry h : mHistory.values()) {
            for (Map.Entry<Long, TextMessage> entry : h.getTextMessages().descendingMap().entrySet())
                if (entry.getValue().isRead())
                    break;
                else
                    texts.put(entry.getKey(), entry.getValue());
        }
        return texts;
    }

    public Map<String, HistoryEntry> getRawHistory() {
        return mHistory;
    }

    public ConversationElement findConversationElement(Long transferId) {
        for (ConversationElement ce : mAggregateHistory) {
            if (ce.file != null && transferId.equals(ce.file.getDataTransferId())) {
                return ce;
            }
        }
        return null;
    }

    public void addDataTransfer(HistoryDataTransfer historyDataTransfer) {
        ConversationElement conversationElement = new ConversationElement(historyDataTransfer);
        if (mAggregateHistory.contains(conversationElement)) {
            return;
        }
        mAggregateHistory.add(conversationElement);

        HistoryEntry accountEntry = mHistory.get(historyDataTransfer.getPeerId());
        if (accountEntry != null) {
            accountEntry.addDatatransfer(historyDataTransfer);
        } else {
            accountEntry = new HistoryEntry(historyDataTransfer.getPeerId(), getContact());
            accountEntry.addDatatransfer(historyDataTransfer);
            mHistory.put(historyDataTransfer.getAccountId(), accountEntry);
        }
    }

    public void addDataTransfers(List<HistoryDataTransfer> historyDataTransfers) {
        for (HistoryDataTransfer historyDataTransfer : historyDataTransfers) {
            addDataTransfer(historyDataTransfer);
        }
    }

    public void updateDataTransfer(long transferId, DataTransferEventCode eventCode) {
        ConversationElement conversationElement = findConversationElement(transferId);
        if (conversationElement != null) {
            conversationElement.file.setDataTransferEventCode(eventCode);
        }
    }

    public void removeAll() {
        mAggregateHistory.clear();
    }

    public interface ConversationActionCallback {

        void deleteConversation(CallContact callContact);

        void copyContactNumberToClipboard(String contactNumber);

    }

    public class ConversationElement {
        public HistoryCall call = null;
        public TextMessage text = null;
        public HistoryDataTransfer file = null;

        ConversationElement(HistoryCall c) {
            call = c;
        }

        ConversationElement(TextMessage t) {
            text = t;
        }

        ConversationElement(HistoryDataTransfer f) {
            file = f;
        }

        long getDate() {
            if (text != null) {
                return text.getTimestamp();
            } else if (call != null) {
                return call.call_start;
            } else if (file != null) {
                return file.getTimestamp();
            }
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ConversationElement that = (ConversationElement) o;

            if (call != null ? !call.equals(that.call) : that.call != null) return false;
            if (text != null ? !text.equals(that.text) : that.text != null) return false;
            return file != null ? file.equals(that.file) : that.file == null;
        }

        @Override
        public int hashCode() {
            int result = call != null ? call.hashCode() : 0;
            result = 31 * result + (text != null ? text.hashCode() : 0);
            result = 31 * result + (file != null ? file.hashCode() : 0);
            return result;
        }
    }
}
