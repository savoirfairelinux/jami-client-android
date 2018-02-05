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
    private final ArrayList<IConversationElement> mAggregateHistory;

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
        mAggregateHistory.add(call);
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
        mAggregateHistory.add(txt);
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

    public ArrayList<IConversationElement> getAggregateHistory() {
        Collections.sort(mAggregateHistory);
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
        List<HistoryCall> result = new ArrayList<>();
        for (IConversationElement ce : mAggregateHistory) {
            if (ce.getType() == IConversationElement.CEType.CALL) {
                result.add((HistoryCall) ce);
            }
        }
        return result;
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

    public HistoryFileTransfer findConversationElement(Long transferId) {
        for (IConversationElement iConversationElement : mAggregateHistory) {
            if (iConversationElement != null && iConversationElement.getType() == IConversationElement.CEType.FILE) {
                HistoryFileTransfer hft = (HistoryFileTransfer) iConversationElement;
                if (transferId.equals(hft.getDataTransferId())) {
                    return hft;
                }
            }
        }
        return null;
    }

    public void addFileTransfer(Long transferId, String filename, boolean isOutgoing, long totalSize, long bytesProgress, String peerId, String accountId) {
        HistoryFileTransfer historyFileTransfer = new HistoryFileTransfer(transferId, filename, isOutgoing, totalSize, bytesProgress, peerId, accountId);
        addFileTransfer(historyFileTransfer);
    }

    public void addFileTransfer(HistoryFileTransfer historyFileTransfer) {
        if (mAggregateHistory.contains(historyFileTransfer)) {
            return;
        }
        mAggregateHistory.add(historyFileTransfer);
    }

    public void addFileTransfers(List<HistoryFileTransfer> historyFileTransfers) {
        for (HistoryFileTransfer historyFileTransfer : historyFileTransfers) {
            addFileTransfer(historyFileTransfer);
        }
    }

    public void updateFileTransfer(long transferId, DataTransferEventCode eventCode) {
        HistoryFileTransfer historyFileTransfer = findConversationElement(transferId);
        if (historyFileTransfer != null) {
            historyFileTransfer.setDataTransferEventCode(eventCode);
        }
    }

    public void removeAll() {
        mAggregateHistory.clear();
        mCurrentCalls.clear();
        mHistory.clear();
    }

    public interface ConversationActionCallback {

        void deleteConversation(CallContact callContact);

        void copyContactNumberToClipboard(String contactNumber);

    }

}
