/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import cx.ring.model.Interaction.InteractionType;
import cx.ring.utils.Log;
import cx.ring.utils.StringUtils;
import cx.ring.utils.Tuple;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class Conversation extends ConversationHistory {
    private static final String TAG = Conversation.class.getSimpleName();

    private final String mAccountId;
    private final Uri mKey;
    private final List<CallContact> mContacts;

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
    private final Subject<List<CallContact>> mContactSubject = BehaviorSubject.create();

    private Single<Conversation> isLoaded = null;

    private final Set<String> mRoots = new HashSet<>(2);
    private final Set<String> mBranches = new HashSet<>(2);
    private final Map<String, Interaction> mMessages = new HashMap<>(16);
    private String lastRead = null;
    private final Mode mMode;

    // runtime flag set to true if the user is currently viewing this conversation
    private boolean mVisible = false;

    // indicate the list needs sorting
    private boolean mDirty = false;

    public Conversation(String accountId, CallContact contact) {
        mAccountId = accountId;
        mContacts = Collections.singletonList(contact);
        mKey = contact.getUri();
        mParticipant = contact.getUri().getUri();
        mContactSubject.onNext(mContacts);
        mMode = null;
    }

    public Conversation(String accountId, Uri uri, Mode mode) {
        mAccountId = accountId;
        mKey = uri;
        mContacts = new ArrayList<>(3);
        mContactSubject.onNext(mContacts);
        mMode = mode;
    }

    public Conference getConference(String id) {
        for (Conference c : mCurrentCalls)
            if (c.getId().contentEquals(id) || c.getCallById(id) != null) {
                return c;
            }
        return null;
    }

    public void composingStatusChanged(CallContact contact, Account.ComposingStatus composing) {
        composingStatusSubject.onNext(composing);
    }

    public Uri getUri() {
        return mKey;
    }

    public Mode getMode() { return mMode; }

    public boolean isSwarm() {
        return Uri.SWARM_SCHEME.equals(getUri().getScheme());
    }

    public boolean matches(String query) {
        for (CallContact contact : getContacts()) {
            if (contact.matches(query))
                return true;
        }
        return false;
    }

    public String getDisplayName() {
        return mContacts.get(0).getDisplayName();
    }

    public void addContact(CallContact contact) {
        mContacts.add(contact);
        mContactSubject.onNext(mContacts);
    }

    public void removeContact(CallContact contact)  {
        mContacts.remove(contact);
        mContactSubject.onNext(mContacts);
    }

    public String getTitle() {
        if (mContacts.isEmpty()) {
            return null;
        } else if (mContacts.size() == 1) {
            return mContacts.get(0).getDisplayName();
        }
        StringBuilder ret = new StringBuilder();
        ArrayList<String> names = new ArrayList<>(mContacts.size());
        int target = mContacts.size();
        for (CallContact c : mContacts) {
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
        StringBuilder ret = new StringBuilder();
        Iterator<CallContact> it = mContacts.iterator();
        while (it.hasNext()) {
            CallContact c = it.next();
            if (c.isUser())
                continue;
            ret.append(c.getRingUsername());
            if (it.hasNext())
                ret.append(", ");
        }
        return ret.toString();
    }

    public Observable<List<CallContact>> getContactUpdates() {
        return mContactSubject;
    }

    public String readMessages() {
        Interaction interaction = null;
        for (String branch : mBranches) {
            Interaction i = mMessages.get(branch);
            if (i != null && !i.isRead()) {
                i.read();
                interaction = i;
                lastRead = i.getMessageId();
            }
        }
        return interaction == null ? null : interaction.getMessageId();
    }

    public Interaction getMessage(String messageId) {
        return mMessages.get(messageId);
    }

    public void setLastMessageRead(String lastMessageRead) {
        lastRead = lastMessageRead;
    }

    public String getLastRead() {
        return lastRead;
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

    public void setLoaded(Single<Conversation> loaded) {
        isLoaded = loaded;
    }

    public Single<Conversation> getLoaded() {
        return isLoaded;
    }

    public void setVisible(boolean visible) {
        mVisible = visible;
    }

    public List<CallContact> getContacts() {
        return mContacts;
    }

    public CallContact getContact() {
        if (mContacts.size() == 1)
            return mContacts.get(0);
        if (isSwarm()) {
            if (mContacts.size() > 2)
                throw new IllegalStateException("getContact() called for group conversation of size " + mContacts.size());
        }
        for (CallContact contact : mContacts) {
            if (!contact.isUser())
                return contact;
        }
        return null;
    }

    public void addCall(SipCall call) {
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
            else
                interaction.setContact(findContact(Uri.fromString(interaction.getAuthor())));
        }
    }

    public CallContact findContact(Uri uri) {
        for (CallContact contact : mContacts)  {
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

    public void addRequestEvent(TrustRequest request, CallContact contact) {
        if (isSwarm())
            return;
        ContactEvent event = new ContactEvent(contact, request);
        mDirty = true;
        mAggregateHistory.add(event);
        updatedElementSubject.onNext(new Tuple<>(event, ElementStatus.ADD));
    }

    public void addContactEvent(CallContact contact) {
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

    public void updateTextMessage(TextMessage text) {
        if (isSwarm()) {
            TextMessage txt = (TextMessage) mMessages.get(text.getMessageId());
            if (txt != null) {
                txt.setStatus(text.getStatus());
                updatedElementSubject.onNext(new Tuple<>(txt, ElementStatus.UPDATE));
                if (text.getStatus() == Interaction.InteractionStatus.DISPLAYED) {
                    if (lastDisplayed == null || lastDisplayed.getTimestamp() < text.getTimestamp()) {
                        lastDisplayed = text;
                        lastDisplayedSubject.onNext(text);
                    }
                }
            } else {
                Log.e(TAG, "Can't find swarm message to update: " + text.getMessageId());
            }
        } else {
            setInteractionProperties(text);
            long time = text.getTimestamp();
            NavigableMap<Long, Interaction> msgs = mHistory.subMap(time, true, time, true);
            for (Interaction txt : msgs.values()) {
                if (txt.getId() == text.getId()) {
                    txt.setStatus(text.getStatus());
                    updatedElementSubject.onNext(new Tuple<>(txt, ElementStatus.UPDATE));
                    if (text.getStatus() == Interaction.InteractionStatus.DISPLAYED) {
                        if (lastDisplayed == null || lastDisplayed.getTimestamp() < text.getTimestamp()) {
                            lastDisplayed = text;
                            lastDisplayedSubject.onNext(text);
                        }
                    }
                    return;
                }
            }
            Log.e(TAG, "Can't find message to update: " + text.getId());
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

    public Collection<SipCall> getCallHistory() {
        List<SipCall> result = new ArrayList<>();
        for (Interaction interaction : mAggregateHistory) {
            if (interaction.getType() == InteractionType.CALL) {
                result.add((SipCall) interaction);
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
                if (value.getType() == InteractionType.TEXT) {
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
            if (interaction != null && interaction.getType() == (InteractionType.DATA_TRANSFER)) {
                if (transferId == (interaction.getId())) {
                    return interaction;
                }
            }
        }
        return null;
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
                return new SipCall(interaction);
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
        if (interaction.getType() == InteractionType.TEXT) {
            TextMessage msg = new TextMessage(interaction);
            addTextMessage(msg);
        } else if (interaction.getType() == InteractionType.CALL) {
            SipCall call = new SipCall(interaction);
            addCall(call);
        } else if (interaction.getType() == InteractionType.CONTACT) {
            ContactEvent event = new ContactEvent(interaction);
            addContactEvent(event);
        } else if (interaction.getType() == InteractionType.DATA_TRANSFER) {
            DataTransfer dataTransfer = new DataTransfer(interaction);
            addFileTransfer(dataTransfer);
        }
    }

    private boolean isNewLeaf(List<String> roots) {
        if (mBranches.isEmpty())
            return true;
        boolean addLeaf = false;
        for (String root : roots) {
            if (mBranches.remove(root))
                addLeaf = true;
        }
        return addLeaf;
    }

    public boolean addSwarmElement(Interaction interaction) {
        if (mMessages.containsKey(interaction.getMessageId())) {
            return false;
        }
        boolean newMessage = false;
        mMessages.put(interaction.getMessageId(), interaction);
        if (mRoots.isEmpty() || mRoots.contains(interaction.getMessageId())) {
            mRoots.remove(interaction.getMessageId());
            mRoots.addAll(interaction.getParentIds());
            // Log.w(TAG, "Found new roots for " + getUri() + " " + mRoots);
        }
        if (lastRead != null && lastRead.equals(interaction.getMessageId()))
            interaction.read();
        if (isNewLeaf(interaction.getParentIds())) {
            mBranches.add(interaction.getMessageId());
            if (isVisible()) {
                interaction.read();
                setLastMessageRead(interaction.getMessageId());
            }
            newMessage = true;
        }
        if (mAggregateHistory.isEmpty() || interaction.getParentIds().contains(mAggregateHistory.get(mAggregateHistory.size()-1).getMessageId())) {
            // New leaf
            mAggregateHistory.add(interaction);
            updatedElementSubject.onNext(new Tuple<>(interaction, ElementStatus.ADD));
        } else {
            // New root or normal node
            for (int i = 0; i < mAggregateHistory.size(); i++) {
                if (mAggregateHistory.get(i).getParentIds() != null && mAggregateHistory.get(i).getParentIds().contains(interaction.getMessageId())) {
                    mAggregateHistory.add(i, interaction);
                    updatedElementSubject.onNext(new Tuple<>(interaction, ElementStatus.ADD));
                    break;
                }
            }
        }
        return newMessage;
    }

    public Collection<String> getSwarmRoot() {
        return mRoots;
    }

    public void updateFileTransfer(DataTransfer transfer, Interaction.InteractionStatus eventCode) {
        DataTransfer dataTransfer = (DataTransfer) findConversationElement(transfer.getId());
        if (dataTransfer != null) {
            dataTransfer.setStatus(eventCode);
            updatedElementSubject.onNext(new Tuple<>(dataTransfer, ElementStatus.UPDATE));
        }
    }

    public void removeInteraction(Interaction interaction) {
        if (removeInteraction(interaction.getId()))
            updatedElementSubject.onNext(new Tuple<>(interaction, ElementStatus.REMOVE));
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
        Public
    }

    public interface ConversationActionCallback {

        void removeConversation(Uri callContact);

        void clearConversation(Uri callContact);

        void copyContactNumberToClipboard(String contactNumber);

    }

}
