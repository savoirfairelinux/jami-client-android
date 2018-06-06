/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
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
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cx.ring.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cx.ring.utils.Log;
import cx.ring.utils.StringUtils;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class Account {
    private static final String TAG = Account.class.getSimpleName();

    private final String accountID;

    private AccountConfig mVolatileDetails;
    private AccountConfig mDetails;
    private ArrayList<AccountCredentials> credentialsDetails = new ArrayList<>();
    private Map<String, String> devices = new HashMap<>();
    private Map<String, CallContact> mContacts = new HashMap<>();
    private Map<String, TrustRequest> mRequests = new HashMap<>();

    private Map<String, CallContact> mContactCache = new HashMap<>();

    private static final String CONTACT_ADDED = "added";
    private static final String CONTACT_CONFIRMED = "confirmed";
    private static final String CONTACT_BANNED = "banned";
    private static final String CONTACT_ID = "id";

    public boolean registeringUsername = false;

    public Collection<Conversation> getConversations() {
        return conversations.values();
    }

    public Collection<Conversation> getPending() {
        return pending.values();
    }

    public static class ContactEvent {
        public final CallContact contact;
        public final boolean added;
        ContactEvent(CallContact c, boolean a) { contact=c; added=a;}
    }
    public static class RequestEvent {
        public final TrustRequest request;
        public final boolean added;
        RequestEvent(TrustRequest c, boolean a) { request=c; added=a;}
    }

    private boolean historyLoaded = false;
    private final Map<String, Conversation> conversations = new HashMap<>();
    private final Map<String, Conversation> pending = new HashMap<>();
    private final Map<String, Conversation> cache = new HashMap<>();

    private final BehaviorSubject<List<Conversation>> conversationsSubject = BehaviorSubject.create();
    private final BehaviorSubject<Collection<CallContact>> contactListSubject = BehaviorSubject.create();
    private final BehaviorSubject<Collection<TrustRequest>> trustRequestsSubject = BehaviorSubject.create();
    private final Subject<RequestEvent> trustRequestSubject = PublishSubject.create();
    /*private final Disposable op = trustRequestsSubject.subscribe(requests -> {
        for (TrustRequest req : requests) {
            String key = new Uri(req.getContactId()).getRawUriString();
            Conversation conversation = pending.get(key);
            if (conversation == null) {
                conversation = getByKey(key);
                pending.put(key, conversation);
                conversation.addRequestEvent(req);
            }
        }
    });*/

    //private final Subject<ContactEvent> contactSubject = PublishSubject.create();

    private final Observable<CallContact> contactsObservable = Observable.create(subscriber -> {
        for (CallContact c : mContacts.values())
            subscriber.onNext(c);
        subscriber.onComplete();
    });
    private final Observable<CallContact> validContactsObservable = contactsObservable.filter(c -> !c.isBanned());
    private final Observable<CallContact> bannedContactsObservable = contactsObservable.filter(CallContact::isBanned);

    public Observable<CallContact> getValidContacts() {
        return validContactsObservable;
    }

    public Observable<Collection<CallContact>> getContactsUpdates() {
        return contactListSubject;
    }
    public Observable<Collection<TrustRequest>> getRequestsUpdates() {
        return trustRequestsSubject;
    }
    public Observable<RequestEvent> getRequestsEvents() {
        return trustRequestSubject;
    }
    public Observable<Collection<CallContact>> getValidContactsUpdates() {
        return contactListSubject.concatMapSingle(list -> Observable.fromIterable(list).filter(c -> !c.isBanned()).toList());
    }
    public Observable<Collection<CallContact>> getBannedContactsUpdates() {
        return contactListSubject.concatMapSingle(list -> Observable.fromIterable(list).filter(CallContact::isBanned).toList());
    }
    /*public Observable<ContactEvent> getContactEvents() {
        return contactSubject;
    }*/

    public CallContact getContactFromCache(String key) {
        CallContact contact = mContactCache.get(key);
        if (contact == null) {
            contact = CallContact.buildUnknown(key);
            mContactCache.put(key, contact);
        }
        return contact;
    }
    public CallContact getContactFromCache(Uri uri) {
        return getContactFromCache(uri.getRawUriString());
    }

    public Account(String bAccountID) {
        accountID = bAccountID;
        mDetails = new AccountConfig();
        mVolatileDetails = new AccountConfig();
    }

    public void dispose() {
        contactListSubject.onComplete();
        //contactSubject.onComplete();
        trustRequestsSubject.onComplete();
    }

    public Account(String bAccountID, final Map<String, String> details,
                   final List<Map<String, String>> credentials,
                   final Map<String, String> volDetails) {
        accountID = bAccountID;
        mDetails = new AccountConfig(details);
        mVolatileDetails = new AccountConfig(volDetails);
        setCredentials(credentials);
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
        this.mDetails = new AccountConfig(details);
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
        return mDetails.get(ConfigKey.ACCOUNT_USERNAME);
    }

    public String getDisplayUsername() {
        if (isRing()) {
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

    public int getRegistrationStateCode() {
        String codeStr = mVolatileDetails.get(ConfigKey.ACCOUNT_REGISTRATION_STATE_CODE);
        if (codeStr == null || codeStr.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(codeStr);
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

    public Boolean isRing() {
        return mDetails.get(ConfigKey.ACCOUNT_TYPE).equals(AccountConfig.ACCOUNT_TYPE_RING);
    }

    public void setAlias(String alias) {
        mDetails.put(ConfigKey.ACCOUNT_ALIAS, alias);
    }

    public String getDetail(ConfigKey key) {
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

    public ArrayList<AccountCredentials> getCredentials() {
        return credentialsDetails;
    }

    public void addCredential(AccountCredentials newValue) {
        credentialsDetails.add(newValue);
    }

    public void removeCredential(AccountCredentials accountCredentials) {
        credentialsDetails.remove(accountCredentials);
    }

    public List getCredentialsHashMapList() {
        ArrayList<HashMap<String, String>> result = new ArrayList<>();
        for (AccountCredentials cred : credentialsDetails) {
            result.add(cred.getDetails());
        }
        return result;
    }

    public boolean useSecureLayer() {
        return mDetails.getBool(ConfigKey.SRTP_ENABLE) || mDetails.getBool(ConfigKey.TLS_ENABLE);
    }

    public String getUri(boolean display) {
        String username = display ? getDisplayUsername() : getUsername();
        if (isRing()) {
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
            long added = Long.valueOf(contact.get(CONTACT_ADDED));
            callContact.setAddedDate(new Date(added * 1000));
        }
        if (contact.containsKey(CONTACT_BANNED) && contact.get(CONTACT_BANNED).equals("true")) {
            callContact.setStatus(CallContact.Status.BANNED);
        } else if (contact.containsKey(CONTACT_CONFIRMED)) {
            callContact.setStatus(Boolean.valueOf(contact.get(CONTACT_CONFIRMED)) ?
                    CallContact.Status.CONFIRMED :
                    CallContact.Status.REQUEST_SENT);
        }
        mContacts.put(contactId, callContact);
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

    public TrustRequest getRequest(String contactId) {
        return mRequests.get(contactId);
    }

    public void addRequest(TrustRequest request) {
        mRequests.put(request.getContactId(), request);
        CallContact contact = getContactFromCache(request.getContactId());
        if (contact.vcard == null) {
            contact.vcard = request.getVCard();
        }
        String key = new Uri(request.getContactId()).getRawUriString();
        Conversation conversation = pending.get(key);
        if (conversation == null) {
            conversation = getByKey(key);
            pending.put(key, conversation);
            conversation.addRequestEvent(request);
        }
        trustRequestSubject.onNext(new RequestEvent(request, true));
        trustRequestsSubject.onNext(mRequests.values());
    }

    public void setRequests(List<TrustRequest> requests) {
        for (TrustRequest request : requests) {
            mRequests.put(request.getContactId(), request);
            String key = new Uri(request.getContactId()).getRawUriString();
            Conversation conversation = pending.get(key);
            if (conversation == null) {
                conversation = getByKey(key);
                pending.put(key, conversation);
                conversation.addRequestEvent(request);
            }
            trustRequestSubject.onNext(new RequestEvent(request, true));
        }
        trustRequestsSubject.onNext(mRequests.values());
    }

    public boolean removeRequest(Uri contact) {
        TrustRequest request = mRequests.remove(contact.getRawUriString());
        if (request != null) {
            trustRequestSubject.onNext(new RequestEvent(request, true));
            trustRequestsSubject.onNext(mRequests.values());
        }
        return pending.remove(contact.getRawUriString()) != null;
    }

    public void registeredNameFound(int state, String address, String name) {
        if (state == 0) {
            CallContact contact = getContactFromCache(address);
            if (contact != null) {
                contact.setUsername(name);
            }
        }
        TrustRequest request = getRequest(address);
        if (request != null) {
            Log.d(TAG, "registeredNameFound: updating TrustRequest " + name);
            boolean resolved = request.isNameResolved();
            request.setUsername(name);
            if (!resolved) {
                Log.d(TAG, "registeredNameFound: TrustRequest resolved " + name);
                trustRequestsSubject.onNext(mRequests.values());
            }
        }
    }

    public Conversation getByUri(Uri uri) {
        if (uri != null) {
            return getByKey(uri.getRawUriString());
        }
        return null;
    }

    public Conversation getByKey(String key) {
        Conversation conversation = cache.get(key);
        if (conversation != null) {
            return conversation;
        }
            CallContact contact = getContactFromCache(key);
            conversation = new Conversation(contact);
            cache.put(key, conversation);
        return conversation;
    }

    public void contactAdded(CallContact contact) {
        Uri uri = contact.getPrimaryUri();
        String key = uri.getRawUriString();
        if (conversations.get(key) != null)
            return;
        Conversation pendingConversation = pending.get(key);
        if (pendingConversation == null) {
            pendingConversation = getByKey(key);
            conversations.put(key, pendingConversation);
        } else {
            pending.remove(key);
            conversations.put(key, pendingConversation);
        }
        pendingConversation.addContactEvent();
    }

    public void contactRemoved(Uri uri) {
        String key = uri.getRawUriString();
        pending.remove(key);
        conversations.remove(key);
    }

    public void addConversation(String key, Conversation value, boolean b) {
        cache.put(key, value);
        if (b) {
            conversations.put(key, value);
        }
    }

    public boolean isHistoryLoaded() {
        return historyLoaded;
    }

    public void loadHistory(HashMap<String, Conversation> history) {
        Map<String, CallContact> contacts = getContacts();
        List<TrustRequest> requests = getRequests();
        for (Map.Entry<String, Conversation> c : history.entrySet()) {
            CallContact contact = contacts.get(c.getValue().getContact().getPrimaryNumber());
            addConversation(c.getKey(), c.getValue(), contact != null && !contact.isBanned());
        }
        for (CallContact contact : contacts.values()) {
            String key = contact.getIds().get(0);
            if (!contact.isBanned()) {
                Conversation conversation = getByKey(key);
                Conversation old = conversations.put(key, conversation);
                if (old == null) {
                    conversation.addContactEvent();
                }
            }
        }
        for (TrustRequest req : requests) {
            String key = new Uri(req.getContactId()).getRawUriString();
            Conversation conversation = getByKey(key);
            Conversation old = pending.put(key, conversation);
            if (old == null) {
                conversation.addRequestEvent(req);
            }
        }
        historyLoaded = true;
    }

    public Conversation getConversationById(String id) {
        return conversations.get(id);
    }

    public Conversation getConversationByContact(CallContact contact) {
        if (contact != null) {
            ArrayList<String> keys = contact.getIds();
            for (String key : keys) {
                Conversation conversation = conversations.get(key);
                if (conversation != null) {
                    return conversation;
                }
                conversation = pending.get(key);
                if (conversation != null) {
                    return conversation;
                }
            }
        }
        return null;
    }

    public Conversation getConversationByCallId(String callId) {
        for (Conversation conversation : conversations.values()) {
            Conference conf = conversation.getConference(callId);
            if (conf != null) {
                return conversation;
            }
        }
        return null;
    }

}
