/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *  Author: Raphaël Brulé <raphael.brule@savoirfairelinux.com>
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
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cx.ring.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cx.ring.model.Interaction.InteractionStatus;
import cx.ring.services.AccountService;
import cx.ring.smartlist.SmartListViewModel;
import cx.ring.utils.Log;
import cx.ring.utils.StringUtils;
import cx.ring.utils.Tuple;
import ezvcard.VCard;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class Account {
    private static final String TAG = Account.class.getSimpleName();

    private static final String CONTACT_ADDED = "added";
    private static final String CONTACT_CONFIRMED = "confirmed";
    private static final String CONTACT_BANNED = "banned";
    private static final String CONTACT_ID = "id";
    private static final int LOCATION_SHARING_EXPIRATION_MS = 1000 * 60 * 2;

    private final String accountID;

    private AccountConfig mVolatileDetails;
    private AccountConfig mDetails;
    private String mUsername;

    private final ArrayList<AccountCredentials> credentialsDetails = new ArrayList<>();
    private Map<String, String> devices = new HashMap<>();
    private final Map<String, CallContact> mContacts = new HashMap<>();
    private final Map<String, TrustRequest> mRequests = new HashMap<>();
    private final Map<String, CallContact> mContactCache = new HashMap<>();

    private final Map<String, Conversation> conversations = new HashMap<>();
    private final Map<String, Conversation> pending = new HashMap<>();
    private final Map<String, Conversation> cache = new HashMap<>();

    private final List<Conversation> sortedConversations = new ArrayList<>();
    private final List<Conversation> sortedPending = new ArrayList<>();

    public boolean registeringUsername = false;
    private boolean conversationsChanged = true;
    private boolean pendingsChanged = true;
    private boolean historyLoaded = false;

    private final Subject<Conversation> conversationSubject = PublishSubject.create();
    private final Subject<List<Conversation>> conversationsSubject = BehaviorSubject.create();
    private final Subject<List<Conversation>> pendingSubject = BehaviorSubject.create();
    private final Subject<Integer> unreadConversationsSubject = BehaviorSubject.create();
    private final Subject<Integer> unreadPendingSubject = BehaviorSubject.create();
    private final Observable<Integer> unreadConversationsCount = unreadConversationsSubject.distinct();
    private final Observable<Integer> unreadPendingCount = unreadPendingSubject.distinct();

    private final BehaviorSubject<Collection<CallContact>> contactListSubject = BehaviorSubject.create();
    private final BehaviorSubject<Collection<TrustRequest>> trustRequestsSubject = BehaviorSubject.create();

    public boolean canSearch() {
        return !StringUtils.isEmpty(getDetail(ConfigKey.MANAGER_URI));
    }

    public static class ContactLocation {
        public double latitude;
        public double longitude;
        public long timestamp;
        public Date receivedDate;
    }
    public static class ContactLocationEntry {
        public CallContact contact;
        public Observable<ContactLocation> location;
    }
    public enum ComposingStatus {
        Idle,
        Active;

        public static ComposingStatus fromInt(int status) {
            return status == 1 ? Active : Idle;
        }
    }

    private final Map<CallContact, Observable<ContactLocation>> contactLocations = new HashMap<>();
    private final Subject<Map<CallContact, Observable<ContactLocation>>> mLocationSubject = BehaviorSubject.createDefault(contactLocations);
    private final Subject<ContactLocationEntry> mLocationStartedSubject = PublishSubject.create();

    public Single<Account> historyLoader;
    private VCard mProfile;
    private Single<Tuple<String, Object>> mLoadedProfile = null;

    public Account(String bAccountID) {
        accountID = bAccountID;
        mDetails = new AccountConfig();
        mVolatileDetails = new AccountConfig();
    }

    public Account(String bAccountID, final Map<String, String> details,
                   final List<Map<String, String>> credentials,
                   final Map<String, String> volDetails) {
        accountID = bAccountID;
        setDetails(details);
        mVolatileDetails = new AccountConfig(volDetails);
        setCredentials(credentials);
    }

    public void cleanup() {
        conversationSubject.onComplete();
        conversationsSubject.onComplete();
        pendingSubject.onComplete();
        contactListSubject.onComplete();
        trustRequestsSubject.onComplete();
    }

    public Observable<List<Conversation>> getConversationsSubject() {
        return conversationsSubject;
    }

    public Observable<List<SmartListViewModel>> getConversationsViewModels(boolean withPresence) {
        return conversationsSubject
                .map(conversations -> {
                    ArrayList<SmartListViewModel> viewModel = new ArrayList<>(conversations.size());
                    for (Conversation c : conversations)
                        viewModel.add(new SmartListViewModel(c, withPresence));
                    return viewModel;
                });
    }

    public Observable<Conversation> getConversationSubject() {
        return conversationSubject;
    }

    public Observable<SmartListViewModel> getConversationViewModel() {
        return conversationSubject.map(c -> new SmartListViewModel(accountID, c.getContact(), c.getLastEvent()));
    }

    public Observable<List<Conversation>> getPendingSubject() {
        return pendingSubject;
    }

    public Collection<Conversation> getConversations() {
        return conversations.values();
    }

    public Collection<Conversation> getPending() {
        return pending.values();
    }

    public Observable<Integer> getUnreadConversations() {
        return unreadConversationsCount;
    }

    public Observable<Integer> getUnreadPending() {
        return unreadPendingCount;
    }

    private void pendingRefreshed() {
        if (historyLoaded) {
            pendingSubject.onNext(getSortedPending());
            updateUnreadPending();
        }
    }

    private void pendingChanged() {
        pendingsChanged = true;
        pendingRefreshed();
    }

    private void pendingUpdated(Conversation conversation) {
        if (!historyLoaded)
            return;
        if (pendingsChanged) {
            getSortedPending();
        } else {
            if (conversation != null)
                conversation.sortHistory();
            Collections.sort(sortedPending, (a, b) -> Interaction.compare(b.getLastEvent(), a.getLastEvent()));
        }
        pendingSubject.onNext(getSortedPending());
    }

    private void conversationRefreshed(Conversation conversation) {
        if (historyLoaded) {
            conversationSubject.onNext(conversation);
            updateUnreadConversations();
        }
    }

    private void conversationChanged() {
        conversationsChanged = true;
        if (historyLoaded) {
            conversationsSubject.onNext(new ArrayList<>(getSortedConversations()));
        }
        updateUnreadConversations();
    }

    public void conversationUpdated(Conversation conversation) {
        if (!historyLoaded)
            return;
        synchronized (sortedConversations) {
            if (conversationsChanged) {
                getSortedConversations();
            } else {
                if (conversation != null)
                    conversation.sortHistory();
                Collections.sort(sortedConversations, (a, b) -> Interaction.compare(b.getLastEvent(), a.getLastEvent()));
            }
            conversationsSubject.onNext(new ArrayList<>(sortedConversations));
            updateUnreadConversations();
        }
    }

    private void updateUnreadConversations() {
        int unread = 0;
        for (Conversation model : sortedConversations) {
            Interaction last = model.getLastEvent();
            if (last != null && !last.isRead())
                unread++;
        }
        // Log.w(TAG, "updateUnreadConversations " + unread);
        unreadConversationsSubject.onNext(unread);
    }

    private void updateUnreadPending() {
        unreadPendingSubject.onNext(sortedPending.size());
    }

    /**
     * Clears a conversation
     *
     * @param contact the contact
     * @param delete  true if you want to remove the conversation
     */
    public void clearHistory(Uri contact, boolean delete) {
        Conversation conversation = getByUri(contact);
        // if it is a sip account, we do not add a contact event
        conversation.clearHistory(delete || isSip());
        conversationChanged();
    }

    public void clearAllHistory() {
        for (Conversation conversation : getConversations()) {
            // if it is a sip account, we do not add a contact event
            conversation.clearHistory(isSip());
        }
        for (Conversation conversation : pending.values()) {
            conversation.clearHistory(true);
        }
        conversationChanged();
        pendingChanged();
    }

    public void updated(Conversation conversation) {
        String key = conversation.getContact().getPrimaryUri().getUri();
        if (conversation == conversations.get(key))
            conversationUpdated(conversation);
        else if (conversation == pending.get(key))
            pendingUpdated(conversation);
        else if (conversation == cache.get(key)) {
            if (mContacts.containsKey(key) || !isJami()) {
                conversations.put(key, conversation);
                conversationChanged();
            } else {
                pending.put(key, conversation);
                pendingChanged();
            }
        }
    }

    public void refreshed(Conversation conversation) {
        if (conversations.containsValue(conversation))
            conversationRefreshed(conversation);
        else if (pending.containsValue(conversation))
            pendingRefreshed();
    }

    public void addTextMessage(TextMessage txt) {
        Conversation conversation = null;
        String daemonId = txt.getDaemonIdString();
        if (daemonId != null && !StringUtils.isEmpty(daemonId)) {
            conversation = getConversationByCallId(daemonId);
        }
        if (conversation == null) {
            conversation = getByKey(txt.getConversation().getParticipant());
            txt.setContact(conversation.getContact());
        }
        conversation.addTextMessage(txt);
        updated(conversation);
    }

    public Conversation onDataTransferEvent(DataTransfer transfer) {
        Conversation conversation = getByUri(new Uri(transfer.getPeerId()));
        InteractionStatus transferEventCode = transfer.getStatus();
        if (transferEventCode == InteractionStatus.TRANSFER_CREATED) {
            conversation.addFileTransfer(transfer);
        } else {
            conversation.updateFileTransfer(transfer, transferEventCode);
        }
        updated(conversation);
        return conversation;
    }

    public Observable<Collection<CallContact>> getBannedContactsUpdates() {
        return contactListSubject.concatMapSingle(list -> Observable.fromIterable(list).filter(CallContact::isBanned).toList());
    }

    public CallContact getContactFromCache(String key) {
        if (StringUtils.isEmpty(key))
            return null;
        synchronized (mContactCache) {
            CallContact contact = mContactCache.get(key);
            if (contact == null) {
                if (isSip())
                    contact = CallContact.buildSIP(new Uri(key));
                else
                    contact = CallContact.build(key);
                mContactCache.put(key, contact);
            }
            return contact;
        }
    }

    public CallContact getContactFromCache(Uri uri) {
        return getContactFromCache(uri.getUri());
    }

    public void dispose() {
        contactListSubject.onComplete();
        trustRequestsSubject.onComplete();
    }

    public Map<String, String> getDevices() {
        return devices;
    }

    public void setCredentials(List<Map<String, String>> credentials) {
        credentialsDetails.clear();
        if (credentials != null) {
            credentialsDetails.ensureCapacity(credentials.size());
            for (int i = 0; i < credentials.size(); ++i) {
                credentialsDetails.add(new AccountCredentials(credentials.get(i)));
            }
        }
    }

    public void setDetails(Map<String, String> details) {
        mDetails = new AccountConfig(details);
        mUsername = mDetails.get(ConfigKey.ACCOUNT_USERNAME);
    }

    public void setDetail(ConfigKey key, String val) {
        mDetails.put(key, val);
    }

    public void setDetail(ConfigKey key, boolean val) {
        mDetails.put(key, val);
    }

    public AccountConfig getConfig() {
        return mDetails;
    }

    public void setDevices(Map<String, String> devs) {
        devices = devs;
    }

    public String getAccountID() {
        return accountID;
    }

    public String getUsername() {
        return mUsername;
    }

    public String getDisplayname() {
        return mDetails.get(ConfigKey.ACCOUNT_DISPLAYNAME);
    }

    public String getDisplayUsername() {
        if (isJami()) {
            String registeredName = getRegisteredName();
            if (registeredName != null && !registeredName.isEmpty()) {
                return registeredName;
            }
        }
        return getUsername();
    }

    public String getHost() {
        return mDetails.get(ConfigKey.ACCOUNT_HOSTNAME);
    }

    public void setHost(String host) {
        mDetails.put(ConfigKey.ACCOUNT_HOSTNAME, host);
    }

    public String getProxy() {
        return mDetails.get(ConfigKey.ACCOUNT_ROUTESET);
    }

    public void setProxy(String proxy) {
        mDetails.put(ConfigKey.ACCOUNT_ROUTESET, proxy);
    }

    public boolean isDhtProxyEnabled() {
        return mDetails.getBool(ConfigKey.PROXY_ENABLED);
    }

    public void setDhtProxyEnabled(boolean active) {
        mDetails.put(ConfigKey.PROXY_ENABLED, active ? "true" : "false");
    }

    public String getRegistrationState() {
        return mVolatileDetails.get(ConfigKey.ACCOUNT_REGISTRATION_STATUS);
    }

    public void setRegistrationState(String registeredState, int code) {
        mVolatileDetails.put(ConfigKey.ACCOUNT_REGISTRATION_STATUS, registeredState);
        mVolatileDetails.put(ConfigKey.ACCOUNT_REGISTRATION_STATE_CODE, Integer.toString(code));
    }

    public void setVolatileDetails(Map<String, String> volatileDetails) {
        mVolatileDetails = new AccountConfig(volatileDetails);
    }

    public String getRegisteredName() {
        return mVolatileDetails.get(ConfigKey.ACCOUNT_REGISTERED_NAME);
    }

    public String getAlias() {
        return mDetails.get(ConfigKey.ACCOUNT_ALIAS);
    }

    public Boolean isSip() {
        return mDetails.get(ConfigKey.ACCOUNT_TYPE).equals(AccountConfig.ACCOUNT_TYPE_SIP);
    }

    public Boolean isJami() {
        return mDetails.get(ConfigKey.ACCOUNT_TYPE).equals(AccountConfig.ACCOUNT_TYPE_RING);
    }

    public void setAlias(String alias) {
        mDetails.put(ConfigKey.ACCOUNT_ALIAS, alias);
    }

    private String getDetail(ConfigKey key) {
        return mDetails.get(key);
    }

    public boolean getDetailBoolean(ConfigKey key) {
        return mDetails.getBool(key);
    }

    public boolean isEnabled() {
        return mDetails.getBool(ConfigKey.ACCOUNT_ENABLE);
    }

    public boolean isActive() {
        return mVolatileDetails.getBool(ConfigKey.ACCOUNT_ACTIVE);
    }

    public void setEnabled(boolean isChecked) {
        mDetails.put(ConfigKey.ACCOUNT_ENABLE, isChecked);
    }

    public boolean hasPassword() {
        return mDetails.getBool(ConfigKey.ARCHIVE_HAS_PASSWORD);
    }

    public boolean hasManager() {
        return !mDetails.get(ConfigKey.MANAGER_URI).isEmpty();
    }

    public HashMap<String, String> getDetails() {
        return mDetails.getAll();
    }

    public boolean isTrying() {
        return getRegistrationState().contentEquals(AccountConfig.STATE_TRYING);
    }

    public boolean isRegistered() {
        return (getRegistrationState().contentEquals(AccountConfig.STATE_READY) || getRegistrationState().contentEquals(AccountConfig.STATE_REGISTERED));
    }

    public boolean isInError() {
        String state = getRegistrationState();
        return (state.contentEquals(AccountConfig.STATE_ERROR)
                || state.contentEquals(AccountConfig.STATE_ERROR_AUTH)
                || state.contentEquals(AccountConfig.STATE_ERROR_CONF_STUN)
                || state.contentEquals(AccountConfig.STATE_ERROR_EXIST_STUN)
                || state.contentEquals(AccountConfig.STATE_ERROR_GENERIC)
                || state.contentEquals(AccountConfig.STATE_ERROR_HOST)
                || state.contentEquals(AccountConfig.STATE_ERROR_NETWORK)
                || state.contentEquals(AccountConfig.STATE_ERROR_NOT_ACCEPTABLE)
                || state.contentEquals(AccountConfig.STATE_ERROR_SERVICE_UNAVAILABLE)
                || state.contentEquals(AccountConfig.STATE_REQUEST_TIMEOUT));
    }

    public boolean isIP2IP() {
        boolean emptyHost = getHost() == null || (getHost() != null && getHost().isEmpty());
        return isSip() && emptyHost;
    }

    public boolean isAutoanswerEnabled() {
        return mDetails.getBool(ConfigKey.ACCOUNT_AUTOANSWER);
    }

    public boolean isRendezvousEnabled() {
        return mDetails.getBool(ConfigKey.ACCOUNT_ISRENDEZVOUS);
    }

    public ArrayList<AccountCredentials> getCredentials() {
        return credentialsDetails;
    }

    public void addCredential(AccountCredentials newValue) {
        credentialsDetails.add(newValue);
    }

    public void removeCredential(AccountCredentials accountCredentials) {
        credentialsDetails.remove(accountCredentials);
    }

    public List<Map<String, String>> getCredentialsHashMapList() {
        ArrayList<Map<String, String>> result = new ArrayList<>(credentialsDetails.size());
        for (AccountCredentials cred : credentialsDetails) {
            result.add(cred.getDetails());
        }
        return result;
    }

    private String getUri(boolean display) {
        String username = display ? getDisplayUsername() : getUsername();
        if (isJami()) {
            return username;
        } else {
            return username + "@" + getHost();
        }
    }

    public String getUri() {
        return getUri(false);
    }

    public String getDisplayUri() {
        return getUri(true);
    }
    public String getDisplayUri(CharSequence defaultNameSip) {
        return isIP2IP() ? defaultNameSip.toString() : getDisplayUri();
    }

    public boolean needsMigration() {
        return AccountConfig.STATE_NEED_MIGRATION.equals(getRegistrationState());
    }

    public String getDeviceId() {
        return getDetail(ConfigKey.ACCOUNT_DEVICE_ID);
    }

    public String getDeviceName() {
        return getDetail(ConfigKey.ACCOUNT_DEVICE_NAME);
    }

    public Map<String, CallContact> getContacts() {
        return mContacts;
    }

    public List<CallContact> getBannedContacts() {
        ArrayList<CallContact> banned = new ArrayList<>();
        for (CallContact contact : mContacts.values()) {
            if (contact.isBanned()) {
                banned.add(contact);
            }
        }
        return banned;
    }

    public CallContact getContact(String ringId) {
        return mContacts.get(ringId);
    }

    public void addContact(String id, boolean confirmed) {
        CallContact callContact = mContacts.get(id);
        if (callContact == null) {
            callContact = getContactFromCache(new Uri(id));
            mContacts.put(id, callContact);
        }
        callContact.setAddedDate(new Date());
        if (confirmed) {
            callContact.setStatus(CallContact.Status.CONFIRMED);
        } else {
            callContact.setStatus(CallContact.Status.REQUEST_SENT);
        }
        TrustRequest req = mRequests.get(id);
        if (req != null) {
            mRequests.remove(id);
            trustRequestsSubject.onNext(mRequests.values());
        }
        contactAdded(callContact);
        //contactSubject.onNext(new ContactEvent(callContact, true));
        contactListSubject.onNext(mContacts.values());
    }

    public void removeContact(String id, boolean banned) {
        CallContact callContact = mContacts.get(id);
        if (banned) {
            if (callContact == null) {
                callContact = getContactFromCache(new Uri(id));
                mContacts.put(id, callContact);
            }
            callContact.setStatus(CallContact.Status.BANNED);
        } else {
            mContacts.remove(id);
        }
        TrustRequest req = mRequests.get(id);
        if (req != null) {
            mRequests.remove(id);
            trustRequestsSubject.onNext(mRequests.values());
        }
        if (callContact != null) {
            contactRemoved(callContact.getPrimaryUri());
            //contactSubject.onNext(new ContactEvent(callContact, false));
        }
        contactListSubject.onNext(mContacts.values());
    }

    private void addContact(Map<String, String> contact) {
        String contactId = contact.get(CONTACT_ID);
        CallContact callContact = mContacts.get(contactId);
        if (callContact == null) {
            callContact = getContactFromCache(new Uri(contactId));
        }
        String addedStr = contact.get(CONTACT_ADDED);
        if (!StringUtils.isEmpty(addedStr)) {
            long added = Long.parseLong(contact.get(CONTACT_ADDED));
            callContact.setAddedDate(new Date(added * 1000));
        }
        if (contact.containsKey(CONTACT_BANNED) && contact.get(CONTACT_BANNED).equals("true")) {
            callContact.setStatus(CallContact.Status.BANNED);
        } else if (contact.containsKey(CONTACT_CONFIRMED)) {
            callContact.setStatus(Boolean.parseBoolean(contact.get(CONTACT_CONFIRMED)) ?
                    CallContact.Status.CONFIRMED :
                    CallContact.Status.REQUEST_SENT);
        }
        mContacts.put(contactId, callContact);
        contactAdded(callContact);
    }

    public void setContacts(List<Map<String, String>> contacts) {
        for (Map<String, String> contact : contacts) {
            addContact(contact);
        }
        contactListSubject.onNext(mContacts.values());
    }

    public List<TrustRequest> getRequests() {
        ArrayList<TrustRequest> requests = new ArrayList<>(mRequests.size());
        for (TrustRequest request : mRequests.values()) {
            if (request.isNameResolved()) {
                requests.add(request);
            }
        }
        return requests;
    }

    public Map<String, TrustRequest> getRequestsMigration() {
        return mRequests;
    }

    public TrustRequest getRequest(Uri uri) {
        return mRequests.get(uri.getRawRingId());
    }

    public void addRequest(TrustRequest request) {
        synchronized (pending) {
            mRequests.put(request.getContactId(), request);
            //trustRequestSubject.onNext(new RequestEvent(request, true));
            trustRequestsSubject.onNext(mRequests.values());

            String key = new Uri(request.getContactId()).getUri();
            Conversation conversation = pending.get(key);
            if (conversation == null) {
                conversation = getByKey(key);
                pending.put(key, conversation);
                conversation.addRequestEvent(request);
                pendingChanged();
            }
        }
    }

    public void setRequests(List<TrustRequest> requests) {
        Log.w(TAG, "setRequests " + requests.size());
        synchronized (pending) {
            for (TrustRequest request : requests) {
                mRequests.put(request.getContactId(), request);
                String key = new Uri(request.getContactId()).getUri();
                Conversation conversation = pending.get(key);
                if (conversation == null) {
                    conversation = getByKey(key);
                    pending.put(key, conversation);
                    conversation.addRequestEvent(request);
                }
                //trustRequestSubject.onNext(new RequestEvent(request, true));
            }
            trustRequestsSubject.onNext(mRequests.values());
            pendingChanged();
        }
    }

    public boolean removeRequest(Uri contact) {
        synchronized (pending) {
            String contactUri = contact.getUri();
            TrustRequest request = mRequests.remove(contactUri);
            if (request != null) {
                //trustRequestSubject.onNext(new RequestEvent(request, true));
                trustRequestsSubject.onNext(mRequests.values());
            }
            if (pending.remove(contactUri) != null) {
                pendingChanged();
                return true;
            }
        }
        return false;
    }

    public void registeredNameFound(int state, String address, String name) {
        Uri uri = new Uri(address);
        CallContact contact = getContactFromCache(uri);
        if (contact.setUsername(state == 0 ? name : null)) {
            String key = uri.getUri();
            synchronized (conversations) {
                Conversation conversation = conversations.get(key);
                if (conversation != null)
                    conversationRefreshed(conversation);
            }
            synchronized (pending) {
                if (pending.containsKey(key))
                    pendingRefreshed();
            }
        }
    }

    public Conversation getByUri(Uri uri) {
        if (uri != null && !uri.isEmpty()) {
            return getByKey(uri.getUri());
        }
        return null;
    }

    public Conversation getByUri(String uri) {
        return getByUri(new Uri(uri));
    }

    private Conversation getByKey(String key) {
        Conversation conversation = cache.get(key);
        if (conversation != null) {
            return conversation;
        }
        CallContact contact = getContactFromCache(key);
        conversation = new Conversation(getAccountID(), contact);
        cache.put(key, conversation);
        return conversation;
    }

    public void setHistoryLoaded(List<Conversation> conversations) {
        if (historyLoaded)
            return;
        Log.w(TAG, "setHistoryLoaded() " + conversations.size());
        for (Conversation c : conversations)
            updated(c);
        historyLoaded = true;
        conversationChanged();
        pendingChanged();
    }

    private List<Conversation> getSortedConversations() {
        Log.w(TAG, "getSortedConversations() " + Thread.currentThread().getId());
        synchronized (sortedConversations) {
            if (conversationsChanged) {
                sortedConversations.clear();
                synchronized (conversations) {
                    sortedConversations.addAll(conversations.values());
                }
                for (Conversation c : sortedConversations)
                    c.sortHistory();
                Collections.sort(sortedConversations, new ConversationComparator());
                conversationsChanged = false;
            }
        }
        return sortedConversations;
    }

    private List<Conversation> getSortedPending() {
        synchronized (sortedPending) {
            if (pendingsChanged) {
                sortedPending.clear();
                synchronized (pending) {
                    sortedPending.addAll(pending.values());
                }
                for (Conversation c : sortedPending)
                    c.sortHistory();
                Collections.sort(sortedPending, new ConversationComparator());
                pendingsChanged = false;
            }
        }
        return sortedPending;
    }


    private void contactAdded(CallContact contact) {
        Uri uri = contact.getPrimaryUri();
        String key = uri.getUri();
        synchronized (conversations) {
            if (conversations.get(key) != null)
                return;
            synchronized (pending) {
                Conversation pendingConversation = pending.get(key);
                if (pendingConversation == null) {
                    pendingConversation = getByKey(key);
                    conversations.put(key, pendingConversation);
                } else {
                    pending.remove(key);
                    conversations.put(key, pendingConversation);
                    pendingChanged();
                }
                pendingConversation.addContactEvent();
            }
            conversationChanged();
        }
    }

    private void contactRemoved(Uri uri) {
        String key = uri.getUri();
        synchronized (conversations) {
            synchronized (pending) {
                if (pending.remove(key) != null)
                    pendingChanged();
            }
            conversations.remove(key);
            conversationChanged();
        }
    }

    private Conversation getConversationByCallId(String callId) {
        for (Conversation conversation : conversations.values()) {
            Conference conf = conversation.getConference(callId);
            if (conf != null) {
                return conversation;
            }
        }
        return null;
    }

    public void presenceUpdate(String contactUri, boolean isOnline) {
        CallContact contact = getContactFromCache(contactUri);
        if (contact.isOnline() == isOnline)
            return;
        contact.setOnline(isOnline);
        synchronized (conversations) {
            Conversation conversation = conversations.get(contactUri);
            if (conversation != null) {
                conversationRefreshed(conversation);
            }
        }
        synchronized (pending) {
            if (pending.containsKey(contactUri))
                pendingRefreshed();
        }
    }

    public void composingStatusChanged(Uri contactUri, ComposingStatus status) {
        Conversation conversation = getByUri(contactUri);
        if (conversation != null)
            conversation.composingStatusChanged(getContactFromCache(contactUri), status);
    }

    synchronized public long onLocationUpdate(AccountService.Location location) {
        Log.w(TAG, "onLocationUpdate " + location.getPeer() + " " + location.getLatitude() + ",  " + location.getLongitude());
        CallContact contact = getContactFromCache(location.getPeer());

        switch (location.getType()) {
            case position:
                ContactLocation cl = new ContactLocation();
                cl.timestamp = location.getDate();
                cl.latitude = location.getLatitude();
                cl.longitude = location.getLongitude();
                cl.receivedDate = new Date();

                Observable<ContactLocation> ls = contactLocations.get(contact);
                if (ls == null) {
                    ls = BehaviorSubject.createDefault(cl);
                    contactLocations.put(contact, ls);
                    mLocationSubject.onNext(contactLocations);
                    ContactLocationEntry entry = new ContactLocationEntry();
                    entry.contact = contact;
                    entry.location = ls;
                    mLocationStartedSubject.onNext(entry);
                } else {
                    if (ls.blockingFirst().timestamp < cl.timestamp)
                        ((Subject<ContactLocation>) ls).onNext(cl);
                }
                break;

            case stop:
                forceExpireContact(contact);
                break;
        }

        return LOCATION_SHARING_EXPIRATION_MS;
    }

    synchronized private void forceExpireContact(CallContact contact) {
        Log.w(TAG, "forceExpireContact " + contactLocations.size());
        Observable<ContactLocation> cl = contactLocations.remove(contact);
        if (cl != null) {
            Log.w(TAG, "Contact stopped sharing location: " + contact.getDisplayName());
            ((Subject<ContactLocation>) cl).onComplete();
            mLocationSubject.onNext(contactLocations);
        }
    }

    synchronized public void maintainLocation() {
        Log.w(TAG, "maintainLocation " + contactLocations.size());
        if (contactLocations.isEmpty())
            return;
        boolean changed = false;

        final Date expiration = new Date(System.currentTimeMillis() - LOCATION_SHARING_EXPIRATION_MS);
        Iterator<Map.Entry<CallContact, Observable<ContactLocation>>> it = contactLocations.entrySet().iterator();
        while (it.hasNext())  {
            Map.Entry<CallContact, Observable<ContactLocation>> e = it.next();
            if (e.getValue().blockingFirst().receivedDate.before(expiration)) {
                Log.w(TAG, "maintainLocation clearing " + e.getKey().getDisplayName());
                ((Subject<ContactLocation>) e.getValue()).onComplete();
                changed = true;
                it.remove();
            }
        }

        if (changed)
            mLocationSubject.onNext(contactLocations);
    }

    public Observable<ContactLocationEntry> getLocationUpdates() {
        return mLocationStartedSubject;
    }

    public Observable<Map<CallContact, Observable<ContactLocation>>> getLocationsUpdates() {
        return mLocationSubject;
    }

    public Observable<Observable<ContactLocation>> getLocationUpdates(Uri contactId) {
        CallContact contact = getContactFromCache(contactId);
        Log.w(TAG, "getLocationUpdates " + contactId + " " + contact);
        return mLocationSubject
                .flatMapMaybe(locations -> {
                    Observable<ContactLocation> r = locations.get(contact);
                    Log.w(TAG, "getLocationUpdates flatMapMaybe " + locations.size() + " " + r);
                    return r == null ? Maybe.empty() : Maybe.just(r);
                })
                .distinctUntilChanged();
    }

    public Single<String> getAccountAlias() {
        if (isJami()) {
            if (mLoadedProfile == null)
                return Single.just(getJamiAlias());
            return mLoadedProfile.map(p -> StringUtils.isEmpty(p.first) ? getJamiAlias() : p.first);
        } else {
            if (mLoadedProfile == null)
                return Single.just(getAlias());
            return mLoadedProfile.map(p -> StringUtils.isEmpty(p.first) ? getAlias() : p.first);
        }
    }

    /**
     * Registered name, fallback to Alias
     */
    private String getJamiAlias() {
        String registeredName = getRegisteredName();
        if (StringUtils.isEmpty(registeredName))
            return getAlias();
        else
            return registeredName;
    }

    public void setProfile(VCard vcard) {
        mProfile = vcard;
        mLoadedProfile = null;
    }

    public VCard getProfile() {
        return mProfile;
    }

    public Single<Tuple<String, Object>> getLoadedProfile() {
        return mLoadedProfile;
    }

    public void setLoadedProfile(Single<Tuple<String, Object>> profile) {
        mLoadedProfile = profile;
    }

    private static class ConversationComparator implements Comparator<Conversation> {
        @Override
        public int compare(Conversation a, Conversation b) {
            return Interaction.compare(b.getLastEvent(), a.getLastEvent());
        }
    }

}
