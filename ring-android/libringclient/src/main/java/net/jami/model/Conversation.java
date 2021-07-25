/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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

package net.jami.model;

import net.jami.utils.Log;
import net.jami.utils.StringUtils;
import net.jami.utils.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.SingleSubject;
import io.reactivex.rxjava3.subjects.Subject;

public class Conversation extends ConversationHistory {
    private static final String TAG = Conversation.class.getSimpleName();

    private final String mAccountId;
    private final Uri mKey;
    private final List<Contact> mContacts;

    private final NavigableMap<Long, Interaction> mHistory = new TreeMap<>();
    private final ArrayList<Conference> mCurrentCalls = new ArrayList<>();
    private final ArrayList<Interaction> mAggregateHistory = new ArrayList<>(32);
    private Interaction lastDisplayed = null;

    private final Subject<Tuple<Interaction, ElementStatus>> updatedElementSubject = PublishSubject.create();
    private final Subject<Interaction> lastDisplayedSubject = BehaviorSubject.create();
    private final Subject<List<Interaction>> clearedSubject = PublishSubject.create();
    private final Subject<List<Conference>> callsSubject = BehaviorSubject.create();
    private final Subject<Account.ComposingStatus> composingStatusSubject = BehaviorSubject.createDefault(Account.ComposingStatus.Idle);
    private final Subject<Integer> color = BehaviorSubject.create();
    private final Subject<CharSequence> symbol = BehaviorSubject.create();
    private final Subject<List<Contact>> mContactSubject = BehaviorSubject.create();

    private Single<Conversation> isLoaded = null;
    private Completable lastElementLoaded = null;

    private final Set<String> mRoots = new HashSet<>(2);
    private final Map<String, Interaction> mMessages = new HashMap<>(16);
    private String lastRead = null;
    private final Subject<Mode> mMode;

    // runtime flag set to true if the user is currently viewing this conversation
    private boolean mVisible = false;
    private final Subject<Boolean> mVisibleSubject = BehaviorSubject.createDefault(mVisible);

    // indicate the list needs sorting
    private boolean mDirty = false;
    private SingleSubject<Conversation> mLoadingSubject = null;

    public Conversation(String accountId, Contact contact) {
        mAccountId = accountId;
        mContacts = Collections.singletonList(contact);
        mKey = contact.getUri();
        mParticipant = contact.getUri().getUri();
        mContactSubject.onNext(mContacts);
        mMode = BehaviorSubject.createDefault(Mode.Legacy);
    }

    public Conversation(String accountId, Uri uri, Mode mode) {
        mAccountId = accountId;
        mKey = uri;
        mContacts = new ArrayList<>(3);
        mMode = BehaviorSubject.createDefault(mode);
    }

    public Conference getConference(String id) {
        for (Conference c : mCurrentCalls)
            if (c.getId().contentEquals(id) || c.getCallById(id) != null) {
                return c;
            }
        return null;
    }

    public void composingStatusChanged(Contact contact, Account.ComposingStatus composing) {
        composingStatusSubject.onNext(composing);
    }

    public Uri getUri() {
        return mKey;
    }

    public Observable<Mode> getMode() { return mMode; }

    public boolean isSwarm() {
        return Uri.SWARM_SCHEME.equals(getUri().getScheme());
    }

    public boolean matches(String query) {
        for (Contact contact : getContacts()) {
            if (contact.matches(query))
                return true;
        }
        return false;
    }

    public String getDisplayName() {
        return mContacts.get(0).getDisplayName();
    }

    public void addContact(Contact contact) {
        mContacts.add(contact);
        mContactSubject.onNext(mContacts);
    }

    public void removeContact(Contact contact)  {
        mContacts.remove(contact);
        mContactSubject.onNext(mContacts);
    }

    public String getTitle() {
        if (mContacts.isEmpty()) {
            if (mMode.blockingFirst() == Mode.Syncing) {
                return "(Syncing)";
            }
            return null;
        } else if (mContacts.size() == 1) {
            return mContacts.get(0).getDisplayName();
        }
        ArrayList<String> names = new ArrayList<>(mContacts.size());
        int target = mContacts.size();
        for (Contact c : mContacts) {
            if (c.isUser()) {
                target--;
                continue;
            }
            String displayName = c.getDisplayName();
            if (!StringUtils.isEmpty(displayName)) {
                names.add(displayName);
                if (names.size() == 3)
                    break;
            }
        }
        StringBuilder ret = new StringBuilder();
        ret.append(StringUtils.join(", ", names));
        if (!names.isEmpty() && names.size() < target) {
            ret.append(" + ").append(mContacts.size() - names.size());
        }
        String result = ret.toString();
        return result.isEmpty() ? mKey.getRawUriString() : result;
    }

    public String getUriTitle() {
        if (mContacts.isEmpty()) {
            return null;
        } else if (mContacts.size() == 1) {
            return mContacts.get(0).getRingUsername();
        }
        ArrayList<String> names = new ArrayList<>(mContacts.size());
        for (Contact c : mContacts) {
            if (c.isUser())
                continue;
            names.add(c.getRingUsername());
        }
        return StringUtils.join(", ", names);
    }

    public Observable<List<Contact>> getContactUpdates() {
        return mContactSubject;
    }

    public synchronized String readMessages() {
        Interaction interaction = null;
        if (!mAggregateHistory.isEmpty()) {
            Interaction i = mAggregateHistory.get(mAggregateHistory.size() - 1);
            if (i != null && !i.isRead()) {
                i.read();
                interaction = i;
                lastRead = i.getMessageId();
            }
        }
        return interaction == null ? null : interaction.getMessageId();
    }

    public synchronized Interaction getMessage(String messageId) {
        return mMessages.get(messageId);
    }

    public void setLastMessageRead(String lastMessageRead) {
        lastRead = lastMessageRead;
    }

    public String getLastRead() {
        return lastRead;
    }

    public SingleSubject<Conversation> getLoading() {
        return mLoadingSubject;
    }

    public boolean stopLoading() {
        SingleSubject<Conversation> ret = mLoadingSubject;
        mLoadingSubject = null;
        if (ret != null) {
            ret.onSuccess(this);
            return true;
        }
        return false;
    }

    public void setLoading(SingleSubject<Conversation> l) {
        if (mLoadingSubject != null) {
            if (!mLoadingSubject.hasValue() && !mLoadingSubject.hasThrowable())
                mLoadingSubject.onError(new IllegalStateException());
        }
        mLoadingSubject = l;
    }

    public Completable getLastElementLoaded() {
        return lastElementLoaded;
    }

    public void setLastElementLoaded(Completable c) {
        lastElementLoaded = c;
    }

    public void setMode(Mode mode) {
        mMode.onNext(mode);
    }

    public enum ElementStatus {
        UPDATE, REMOVE, ADD
    }

    public Observable<Tuple<Interaction, ElementStatus>> getUpdatedElements() {
        return updatedElementSubject;
    }

    public Observable<Interaction> getLastDisplayed() {
        return lastDisplayedSubject;
    }

    public Observable<List<Interaction>> getCleared() {
        return clearedSubject;
    }

    public Observable<List<Conference>> getCalls() {
        return callsSubject;
    }

    public Observable<Account.ComposingStatus> getComposingStatus() {
        return composingStatusSubject;
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

    public Observable<Boolean> getVisible()  {
        return mVisibleSubject;
    }

    public void setLoaded(Single<Conversation> loaded) {
        isLoaded = loaded;
    }

    public Single<Conversation> getLoaded() {
        return isLoaded;
    }

    public void setVisible(boolean visible) {
        mVisible = visible;
        mVisibleSubject.onNext(mVisible);
    }

    public List<Contact> getContacts() {
        return mContacts;
    }

    public Contact getContact() {
        if (mContacts.size() == 1)
            return mContacts.get(0);
        if (isSwarm()) {
            if (mContacts.size() > 2)
                throw new IllegalStateException("getContact() called for group conversation of size " + mContacts.size());
        }
        for (Contact contact : mContacts) {
            if (!contact.isUser())
                return contact;
        }
        return null;
    }

    public void addCall(Call call) {
        if (!isSwarm() && getCallHistory().contains(call)) {
            return;
        }
        mDirty = true;
        mAggregateHistory.add(call);
        updatedElementSubject.onNext(new Tuple<>(call, ElementStatus.ADD));
    }

    private void setInteractionProperties(Interaction interaction) {
        interaction.setAccount(getAccountId());
        if (interaction.getContact() == null) {
            if (mContacts.size() == 1)
                interaction.setContact(mContacts.get(0));
            else {
                if (interaction.getAuthor() == null)  {
                    Log.e(TAG, "Can't set interaction properties: no author for type:" + interaction.getType() + " id:" + interaction.getId() + " status:" + interaction.mStatus);
                } else {
                    interaction.setContact(findContact(Uri.fromString(interaction.getAuthor())));
                }
            }
        }
    }

    public Contact findContact(Uri uri) {
        for (Contact contact : mContacts)  {
            if (contact.getUri().equals(uri)) {
                return contact;
            }
        }
        return null;
    }

    public void addTextMessage(TextMessage txt) {
        if (mVisible) {
            txt.read();
        }
        if (txt.getConversation() == null) {
            Log.e(TAG, "Error in conversation class... No conversation is attached to this interaction");
        }
        setInteractionProperties(txt);
        mHistory.put(txt.getTimestamp(), txt);
        mDirty = true;
        mAggregateHistory.add(txt);
        updatedElementSubject.onNext(new Tuple<>(txt, ElementStatus.ADD));
    }

    public void addRequestEvent(TrustRequest request, Contact contact) {
        if (isSwarm())
            return;
        ContactEvent event = new ContactEvent(contact, request);
        mDirty = true;
        mAggregateHistory.add(event);
        updatedElementSubject.onNext(new Tuple<>(event, ElementStatus.ADD));
    }

    public void addContactEvent(Contact contact) {
        ContactEvent event = new ContactEvent(contact);
        mDirty = true;
        mAggregateHistory.add(event);
        updatedElementSubject.onNext(new Tuple<>(event, ElementStatus.ADD));
    }

    public void addContactEvent(ContactEvent contactEvent) {
        mDirty = true;
        mAggregateHistory.add(contactEvent);
        updatedElementSubject.onNext(new Tuple<>(contactEvent, ElementStatus.ADD));
    }

    public void addFileTransfer(DataTransfer dataTransfer) {
        if (mAggregateHistory.contains(dataTransfer)) {
            return;
        }
        mDirty = true;
        mAggregateHistory.add(dataTransfer);
        updatedElementSubject.onNext(new Tuple<>(dataTransfer, ElementStatus.ADD));
    }

    boolean isAfter(Interaction previous, Interaction query) {
        if (isSwarm()) {
            while (query != null && query.getParentId() != null) {
                if (query.getParentId().equals(previous.getMessageId()))
                    return true;
                query = mMessages.get(query.getParentId());
            }
            return false;
        } else {
            return previous.getTimestamp() < query.getTimestamp();
        }
    }

    public void updateInteraction(Interaction element) {
        Log.e(TAG, "updateInteraction: " + element.getMessageId() + " " + element.getStatus());
        if (isSwarm()) {
            Interaction e = mMessages.get(element.getMessageId());
            if (e != null) {
                e.setStatus(element.getStatus());
                updatedElementSubject.onNext(new Tuple<>(e, ElementStatus.UPDATE));
                if (e.getStatus() == Interaction.InteractionStatus.DISPLAYED) {
                    if (lastDisplayed == null || isAfter(lastDisplayed, e)) {
                        lastDisplayed = e;
                        lastDisplayedSubject.onNext(e);
                    }
                }
            } else {
                Log.e(TAG, "Can't find swarm message to update: " + element.getMessageId());
            }
        } else {
            setInteractionProperties(element);
            long time = element.getTimestamp();
            NavigableMap<Long, Interaction> msgs = mHistory.subMap(time, true, time, true);
            for (Interaction txt : msgs.values()) {
                if (txt.getId() == element.getId()) {
                    txt.setStatus(element.getStatus());
                    updatedElementSubject.onNext(new Tuple<>(txt, ElementStatus.UPDATE));
                    if (element.getStatus() == Interaction.InteractionStatus.DISPLAYED) {
                        if (lastDisplayed == null || isAfter(lastDisplayed, element)) {
                            lastDisplayed = element;
                            lastDisplayedSubject.onNext(element);
                        }
                    }
                    return;
                }
            }
            Log.e(TAG, "Can't find message to update: " + element.getId());
        }
    }

    public ArrayList<Interaction> getAggregateHistory() {
        return mAggregateHistory;
    }

    private final Single<List<Interaction>> sortedHistory = Single.fromCallable(() -> {
        sortHistory();
        return mAggregateHistory;
    });

    public void sortHistory() {
        if (mDirty) {
            Log.w(TAG, "sortHistory()");
            synchronized (mAggregateHistory) {
                Collections.sort(mAggregateHistory, (c1, c2) -> Long.compare(c1.getTimestamp(), c2.getTimestamp()));
            }
            mDirty = false;
        }
    }

    public Single<List<Interaction>> getSortedHistory() {
        return sortedHistory;
    }

    public Interaction getLastEvent() {
        sortHistory();
        return mAggregateHistory.isEmpty() ? null : mAggregateHistory.get(mAggregateHistory.size() - 1);
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

    public Collection<Call> getCallHistory() {
        List<Call> result = new ArrayList<>();
        for (Interaction interaction : mAggregateHistory) {
            if (interaction.getType() == Interaction.InteractionType.CALL) {
                result.add((Call) interaction);
            }
        }
        return result;
    }

    public TreeMap<Long, TextMessage> getUnreadTextMessages() {
        TreeMap<Long, TextMessage> texts = new TreeMap<>();
        if (isSwarm()) {
            for(int j = mAggregateHistory.size() - 1; j >= 0; j--) {
                Interaction i = mAggregateHistory.get(j);
                if (i.isRead())
                    break;
                if (i instanceof TextMessage)
                    texts.put(i.getTimestamp(), (TextMessage) i);
            }
        } else {
            for (Map.Entry<Long, Interaction> entry : mHistory.descendingMap().entrySet()) {
                Interaction value = entry.getValue();
                if (value.getType() == Interaction.InteractionType.TEXT) {
                    TextMessage message = (TextMessage) value;
                    if (message.isRead())
                        break;
                    texts.put(entry.getKey(), message);
                }
            }
        }
        return texts;
    }

    public NavigableMap<Long, Interaction> getRawHistory() {
        return mHistory;
    }


    private Interaction findConversationElement(int transferId) {
        for (Interaction interaction : mAggregateHistory) {
            if (interaction != null && interaction.getType() == (Interaction.InteractionType.DATA_TRANSFER)) {
                if (transferId == (interaction.getId())) {
                    return interaction;
                }
            }
        }
        return null;
    }

    private boolean removeSwarmInteraction(String messageId) {
        Interaction i = mMessages.remove(messageId);
        if (i != null) {
            mAggregateHistory.remove(i);
            return true;
        }
        return false;
    }

    private boolean removeInteraction(long interactionId) {
        Iterator<Interaction> it = mAggregateHistory.iterator();
        while (it.hasNext()) {
            Interaction interaction = it.next();
            Integer id = interaction == null ? null : interaction.getId();
            if (id != null && interactionId == id) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Clears the conversation cache.
     * @param delete true if you do not want to re-add contact events
     */
    public void clearHistory(boolean delete) {
        mAggregateHistory.clear();
        mHistory.clear();
        mDirty = false;
        if (!delete && mContacts.size() == 1)
            mAggregateHistory.add(new ContactEvent(mContacts.get(0)));
        clearedSubject.onNext(mAggregateHistory);
    }

    static private Interaction getTypedInteraction(Interaction interaction) {
        switch (interaction.getType()) {
            case TEXT:
                return new TextMessage(interaction);
            case CALL:
                return new Call(interaction);
            case CONTACT:
                return new ContactEvent(interaction);
            case DATA_TRANSFER:
                return new DataTransfer(interaction);
        }
        return interaction;
    }

    public void setHistory(List<Interaction> loadedConversation) {
        mAggregateHistory.ensureCapacity(loadedConversation.size());
        Interaction last = null;
        for (Interaction i : loadedConversation) {
            Interaction interaction = getTypedInteraction(i);
            setInteractionProperties(interaction);
            mAggregateHistory.add(interaction);
            mHistory.put(interaction.getTimestamp(), interaction);
            if (!i.isIncoming() && i.getStatus() == Interaction.InteractionStatus.DISPLAYED)
                last = i;
        }
        if (last != null) {
            lastDisplayed = last;
            lastDisplayedSubject.onNext(last);
        }
        mDirty = false;
    }

    public void addElement(Interaction interaction) {
        setInteractionProperties(interaction);
        if (interaction.getType() == Interaction.InteractionType.TEXT) {
            TextMessage msg = new TextMessage(interaction);
            addTextMessage(msg);
        } else if (interaction.getType() == Interaction.InteractionType.CALL) {
            Call call = new Call(interaction);
            addCall(call);
        } else if (interaction.getType() == Interaction.InteractionType.CONTACT) {
            ContactEvent event = new ContactEvent(interaction);
            addContactEvent(event);
        } else if (interaction.getType() == Interaction.InteractionType.DATA_TRANSFER) {
            DataTransfer dataTransfer = new DataTransfer(interaction);
            addFileTransfer(dataTransfer);
        }
    }

    public boolean addSwarmElement(Interaction interaction) {
        if (mMessages.containsKey(interaction.getMessageId())) {
            return false;
        }
        mMessages.put(interaction.getMessageId(), interaction);
        mRoots.remove(interaction.getMessageId());
        if (interaction.getParentId() != null && !mMessages.containsKey(interaction.getParentId())) {
            mRoots.add(interaction.getParentId());
            // Log.w(TAG, "@@@ Found new root for " + getUri() + " " + parent + " -> " + mRoots);
        }
        if (lastRead != null && lastRead.equals(interaction.getMessageId()))
            interaction.read();
        boolean newLeaf = false;
        boolean added = false;
        if (mAggregateHistory.isEmpty() || mAggregateHistory.get(mAggregateHistory.size()-1).getMessageId().equals(interaction.getParentId())) {
            // New leaf
            // Log.w(TAG, "@@@ New end LEAF");
            added = true;
            newLeaf = true;
            mAggregateHistory.add(interaction);
            updatedElementSubject.onNext(new Tuple<>(interaction, ElementStatus.ADD));
        } else {
            // New root or normal node
            for (int i = 0; i < mAggregateHistory.size(); i++) {
                if (interaction.getMessageId().equals(mAggregateHistory.get(i).getParentId())) {
                    //Log.w(TAG, "@@@ New root node at " + i);
                    mAggregateHistory.add(i, interaction);
                    updatedElementSubject.onNext(new Tuple<>(interaction, ElementStatus.ADD));
                    added = true;
                    break;
                }
            }
            if (!added) {
                for (int i = mAggregateHistory.size()-1; i >= 0; i--) {
                    if (mAggregateHistory.get(i).getMessageId().equals(interaction.getParentId())) {
                        //Log.w(TAG, "@@@ New leaf at " + (i+1));
                        added = true;
                        newLeaf = true;
                        mAggregateHistory.add(i+1, interaction);
                        updatedElementSubject.onNext(new Tuple<>(interaction, ElementStatus.ADD));
                        break;
                    }
                }

            }
        }
        if (newLeaf) {
            if (isVisible()) {
                interaction.read();
                setLastMessageRead(interaction.getMessageId());
            }
        }
        if (!added) {
            Log.e(TAG, "Can't attach interaction " + interaction.getMessageId() + " with parent " + interaction.getParentId());
        }
        return newLeaf;
    }

    public boolean isLoaded()  {
        return !mMessages.isEmpty() && mRoots.isEmpty();
    }

    public Collection<String> getSwarmRoot() {
        return mRoots;
    }

    public void updateFileTransfer(DataTransfer transfer, Interaction.InteractionStatus eventCode) {
        DataTransfer dataTransfer = (DataTransfer) (isSwarm() ? transfer : findConversationElement(transfer.getId()));
        if (dataTransfer != null) {
            dataTransfer.setStatus(eventCode);
            updatedElementSubject.onNext(new Tuple<>(dataTransfer, ElementStatus.UPDATE));
        }
    }

    public void removeInteraction(Interaction interaction) {
        if (isSwarm()) {
            if (removeSwarmInteraction(interaction.getMessageId()))
                updatedElementSubject.onNext(new Tuple<>(interaction, ElementStatus.REMOVE));
        } else {
            if (removeInteraction(interaction.getId()))
                updatedElementSubject.onNext(new Tuple<>(interaction, ElementStatus.REMOVE));
        }
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

    public void setSymbol(CharSequence s) {
        symbol.onNext(s);
    }

    public Observable<Integer> getColor() {
        return color;
    }
    public Observable<CharSequence> getSymbol() {
        return symbol;
    }


    public String getAccountId() {
        return mAccountId;
    }

    public enum Mode {
        OneToOne,
        AdminInvitesOnly,
        InvitesOnly,
        // Non-daemon modes
        Syncing, Public, Legacy
    }

    public interface ConversationActionCallback {

        void removeConversation(Uri callContact);

        void clearConversation(Uri callContact);

        void copyContactNumberToClipboard(String contactNumber);

    }

}
