/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import cx.ring.utils.Log;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class Conversation {
    private static final String TAG = Conversation.class.getSimpleName();

    private final String mAccountId;
    private final CallContact mContact;

    private final NavigableMap<Long, ConversationElement> mHistory = new TreeMap<>();
    private final ArrayList<Conference> mCurrentCalls = new ArrayList<>();
    private final ArrayList<ConversationElement> mAggregateHistory = new ArrayList<>(32);

    private final Subject<ConversationElement> newElementSubject = PublishSubject.create();
    private final Subject<ConversationElement> updatedElementSubject = PublishSubject.create();
    private final Subject<ConversationElement> removedElementSubject = PublishSubject.create();
    private final Subject<List<ConversationElement>> clearedSubject = PublishSubject.create();
    private final Subject<List<Conference>> callsSubject = BehaviorSubject.create();
    private Subject<Integer> color = BehaviorSubject.create();

    // runtime flag set to true if the user is currently viewing this conversation
    private boolean mVisible = false;

    // indicate the list needs sorting
    private boolean mDirty = false;

    public Conversation(String accountId, CallContact contact) {
        mAccountId = accountId;
        mContact = contact;
    }

    public Conference getConference(String id) {
        for (Conference c : mCurrentCalls)
            if (c.getId().contentEquals(id) || c.getCallById(id) != null) {
                return c;
            }
        return null;
    }

    public Observable<ConversationElement> getNewElements() {
        return newElementSubject;
    }
    public Observable<ConversationElement> getUpdatedElements() {
        return updatedElementSubject;
    }
    public Observable<ConversationElement> getRemovedElements() {
        return removedElementSubject;
    }
    public Observable<List<ConversationElement>> getCleared() {
        return clearedSubject;
    }
    public Observable<List<Conference>> getCalls() {
        return callsSubject;
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
        callsSubject.onNext(mCurrentCalls);
    }

    public void removeConference(Conference c) {
        mCurrentCalls.remove(c);
        callsSubject.onNext(mCurrentCalls);
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
        //mHistory.addHistoryCall(call, getContact());
        mDirty = true;
        mAggregateHistory.add(call);
        newElementSubject.onNext(call);
    }

    public void addTextMessage(TextMessage txt) {
        if (mVisible) {
            txt.read();
        }
        if (txt.getContact() == null) {
            txt.setContact(getContact());
        }
        mHistory.put(txt.getDate(), txt);
        mDirty = true;
        mAggregateHistory.add(txt);
        newElementSubject.onNext(txt);
    }

    public void addRequestEvent(TrustRequest request) {
        ContactEvent event = new ContactEvent(mContact, request);
        mDirty = true;
        mAggregateHistory.add(event);
        newElementSubject.onNext(event);
    }

    public void addContactEvent() {
        ContactEvent event = new ContactEvent(mContact);
        mDirty = true;
        mAggregateHistory.add(event);
        newElementSubject.onNext(event);
    }

    public void updateTextMessage(TextMessage text) {
        long time = text.getDate();
        NavigableMap<Long, ConversationElement> msgs = mHistory.subMap(time, true, time, true);
        for (ConversationElement txt : msgs.values()) {
            if (txt.equals(text)) {
                ((TextMessage)txt).setStatus(text.getStatus());
                updatedElementSubject.onNext(txt);
                return;
            }
        }
        Log.e(TAG, "Can't find message to update: " + text.getId());
    }

    public ArrayList<ConversationElement> getAggregateHistory() {
        return mAggregateHistory;
    }

    private final Single<List<ConversationElement>> sortedHistory = Single.fromCallable(() -> {
        sortHistory();
        return mAggregateHistory;
    });

    public void sortHistory() {
        if (mDirty) {
            synchronized (mAggregateHistory) {
                Collections.sort(mAggregateHistory, (c1, c2) -> Long.compare(c1.getDate(), c2.getDate()));
            }
            mDirty = false;
        }
    }

    public Single<List<ConversationElement>> getSortedHistory() {
        return sortedHistory;
    }

    public ConversationElement getLastEvent() {
        sortHistory();
        if (mAggregateHistory.isEmpty())
            return null;
        return mAggregateHistory.get(mAggregateHistory.size() - 1);
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
        for (ConversationElement ce : mAggregateHistory) {
            if (ce.getType() == ConversationElement.CEType.CALL) {
                result.add((HistoryCall) ce);
            }
        }
        return result;
    }

    public TreeMap<Long, TextMessage> getUnreadTextMessages() {
        TreeMap<Long, TextMessage> texts = new TreeMap<>();
        for (Map.Entry<Long, ConversationElement> entry : mHistory.descendingMap().entrySet()) {
            ConversationElement value = entry.getValue();
            if (value instanceof TextMessage) {
                if (entry.getValue().isRead())
                    break;
                texts.put(entry.getKey(), (TextMessage) entry.getValue());
            }
        }
        return texts;
    }

    public NavigableMap<Long, ConversationElement> getRawHistory() {
        return mHistory;
    }

    private DataTransfer findConversationElement(Long transferId) {
        for (ConversationElement iConversationElement : mAggregateHistory) {
            if (iConversationElement != null && iConversationElement.getType() == ConversationElement.CEType.FILE) {
                DataTransfer hft = (DataTransfer) iConversationElement;
                if (transferId.equals(hft.getId())) {
                    return hft;
                }
            }
        }
        return null;
    }

    private boolean removeConversationElement(long id) {
        Iterator<ConversationElement> it = mAggregateHistory.iterator();
        while (it.hasNext()) {
            ConversationElement e = it.next();
            if (e != null && id == e.getId()) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    public void clearHistory() {
        mAggregateHistory.clear();
        mHistory.clear();
        mDirty = true;
        clearedSubject.onNext(mAggregateHistory);
    }

    public void addFileTransfer(DataTransfer dataTransfer) {
        if (mAggregateHistory.contains(dataTransfer)) {
            return;
        }
        mDirty = true;
        mAggregateHistory.add(dataTransfer);
        newElementSubject.onNext(dataTransfer);
    }

    public void addElement(ConversationElement e, CallContact contact) {
        if (e instanceof HistoryCall) {
            HistoryCall call = (HistoryCall) e;
            addHistoryCall(call);
        } else if (e instanceof TextMessage) {
            TextMessage msg = (TextMessage) e;
            msg.setContact(contact);
            addTextMessage(msg);
        } else if (e instanceof DataTransfer) {
            DataTransfer t = (DataTransfer) e;
            addFileTransfer(t);
        }
    }

    public void addDataTransfers(List<DataTransfer> dataTransfers) {
        for (DataTransfer dataTransfer : dataTransfers) {
            addFileTransfer(dataTransfer);
        }
    }

    public void updateFileTransfer(DataTransfer transfer, DataTransferEventCode eventCode) {
        DataTransfer dataTransfer = findConversationElement(transfer.getId());
        if (dataTransfer != null) {
            dataTransfer.setEventCode(eventCode);
            updatedElementSubject.onNext(dataTransfer);
        }
    }

    public void removeFileTransfer(DataTransfer transfer) {
        if (removeConversationElement(transfer.getId()))
            removedElementSubject.onNext(transfer);
    }

    public void removeMessage(TextMessage message) {
        if (removeConversationElement(message.getId()))
            removedElementSubject.onNext(message);
    }

    public void removeCallHistory(HistoryCall message) {
        if (removeConversationElement(message.getId()))
            removedElementSubject.onNext(message);
    }

    public void removeAll() {
        mAggregateHistory.clear();
        mCurrentCalls.clear();
        mHistory.clear();
        mDirty = true;
    }

    public void setColor(int c) {
        color.onNext(c);
    }

    public Observable<Integer> getColor() {
        return color;
    }

    public String getAccountId() {
        return mAccountId;
    }

    public interface ConversationActionCallback {

        void removeConversation(CallContact callContact);

        void clearConversation(CallContact callContact);

        void copyContactNumberToClipboard(String contactNumber);

    }

}
