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
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import cx.ring.utils.Log;
import cx.ring.utils.Tuple;

public class Conversation {

    private static final String TAG = Conversation.class.getSimpleName();

    private CallContact mContact;

    private final Map<String, HistoryEntry> mHistory = new HashMap<>();
    private final ArrayList<Conference> mCurrentCalls;
    private final ArrayList<ConversationElement> mAggregateHistory = new ArrayList<>(32);

    // runtime flag set to true if the user is currently viewing this conversation
    private boolean mVisible = false;
    private int uuid;

    public Conversation(CallContact c) {
        setContact(c);
        mCurrentCalls = new ArrayList<>();
        setUuid(new Random().nextInt());
    }

    public class ConversationElement {
        public HistoryCall call = null;
        public TextMessage text = null;

        ConversationElement(HistoryCall c) {
            call = c;
        }

        ConversationElement(TextMessage t) {
            text = t;
        }

        long getDate() {
            if (text != null)
                return text.getTimestamp();
            else if (call != null)
                return call.call_start;
            return 0;
        }
    }

    public boolean hasCurrentCall() {
        return !mCurrentCalls.isEmpty();
    }

    public String getLastNumberUsed(String accountID) {
        HistoryEntry he = mHistory.get(accountID);
        if (he == null)
            return null;
        return he.getLastNumberUsed();
    }

    public Conference getConference(String id) {
        for (Conference c : mCurrentCalls)
            if (c.getId().contentEquals(id) || c.getCallById(id) != null)
                return c;
        return null;
    }

    public void addConference(Conference c) {
        mCurrentCalls.add(c);
    }

    public void removeConference(Conference c) {
        mCurrentCalls.remove(c);
    }

    public Tuple<HistoryEntry, HistoryCall> findHistoryByCallId(String id) {
        for (HistoryEntry e : mHistory.values()) {
            for (HistoryCall c : e.getCalls().values()) {
                if (c.getCallId().equals(id))
                    return new Tuple<>(e, c);
            }
        }
        return null;
    }

    public void setContact(CallContact contact) {
        this.mContact = contact;
    }

    public boolean isVisible() {
        return mVisible;
    }

    public void setVisible(boolean mVisible) {
        this.mVisible = mVisible;
    }

    public int getUuid() {
        return uuid;
    }

    public void setUuid(int uuid) {
        this.uuid = uuid;
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
        return d;
    }

    public void addHistoryCall(HistoryCall c) {
        String accountId = c.getAccountID();
        if (mHistory.containsKey(accountId))
            mHistory.get(accountId).addHistoryCall(c, getContact());
        else {
            HistoryEntry e = new HistoryEntry(accountId, getContact());
            e.addHistoryCall(c, getContact());
            mHistory.put(accountId, e);
        }
        mAggregateHistory.add(new ConversationElement(c));
    }

    public void addTextMessage(TextMessage txt) {
        if (txt.getContact() == null) {
            txt.setContact(getContact());
        }
        String accountId = txt.getAccount();
        if (mHistory.containsKey(accountId)) {
            mHistory.get(accountId).addTextMessage(txt);
        } else {
            HistoryEntry e = new HistoryEntry(accountId, getContact());
            e.addTextMessage(txt);
            mHistory.put(accountId, e);
        }
        mAggregateHistory.add(new ConversationElement(txt));
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
        if (mCurrentCalls.isEmpty())
            return null;
        return mCurrentCalls.get(0);
    }

    public ArrayList<Conference> getCurrentCalls() {
        return mCurrentCalls;
    }

    public Collection<TextMessage> getTextMessages() {
        return getTextMessages(null);
    }

    public Collection<TextMessage> getTextMessages(Date since) {
        TreeMap<Long, TextMessage> texts = new TreeMap<>();
        for (HistoryEntry h : mHistory.values()) {
            texts.putAll(since == null ? h.getTextMessages() : h.getTextMessages(since.getTime()));
        }
        return texts.values();
    }

    public Collection<HistoryCall> getHistoryCalls() {
        TreeMap<Long, HistoryCall> calls = new TreeMap<>();
        for (HistoryEntry historyEntry : mHistory.values()) {
            calls.putAll(historyEntry.getCalls());
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

    public boolean hasUnreadTextMessages() {
        for (HistoryEntry h : mHistory.values()) {
            Map.Entry<Long, TextMessage> m = h.getTextMessages().lastEntry();
            if (m != null && !m.getValue().isRead()) {
                return true;
            }
        }
        return false;
    }

    public Map<String, HistoryEntry> getRawHistory() {
        return mHistory;
    }

    public interface ConversationActionCallback {
        void deleteConversation(Conversation conversation);

        void copyContactNumberToClipboard(String contactNumber);
    }
}
