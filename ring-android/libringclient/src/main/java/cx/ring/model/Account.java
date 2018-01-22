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
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cx.ring.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cx.ring.utils.StringUtils;

public class Account {

    private String accountID;

    private AccountConfig mVolatileDetails;
    private AccountConfig mDetails;
    private ArrayList<AccountCredentials> credentialsDetails = new ArrayList<>();
    private Map<String, String> devices = new HashMap<>();
    private Map<String, CallContact> mContacts = new HashMap<>();
    private Map<String, TrustRequest> mRequests = new HashMap<>();

    private static final String CONTACT_ADDED = "added";
    private static final String CONTACT_CONFIRMED = "confirmed";
    private static final String CONTACT_BANNED = "banned";
    private static final String CONTACT_ID = "id";

    public boolean registeringUsername = false;

    public Account(String bAccountID) {
        accountID = bAccountID;
        mDetails = new AccountConfig();
        mVolatileDetails = new AccountConfig();
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
            callContact = CallContact.buildUnknown(new Uri(id));
            mContacts.put(id, callContact);
        }
        callContact.setAddedDate(new Date());
        if (confirmed) {
            callContact.setStatus(CallContact.Status.CONFIRMED);
        } else {
            callContact.setStatus(CallContact.Status.REQUEST_SENT);
        }
    }
    public void removeContact(String id, boolean banned) {
        if (banned) {
            CallContact callContact = mContacts.get(id);
            if (callContact == null) {
                callContact = CallContact.buildUnknown(new Uri(id));
                mContacts.put(id, callContact);
            }
            callContact.setStatus(CallContact.Status.BANNED);
        } else {
            mContacts.remove(id);
        }
    }

    public void addContact(Map<String,String> contact) {
        String contactId = contact.get(CONTACT_ID);
        CallContact callContact = mContacts.get(contactId);
        if (callContact == null) {
            callContact = CallContact.buildUnknown(new Uri(contactId));
        }
        String addedStr = contact.get(CONTACT_ADDED);
        if (!StringUtils.isEmpty(addedStr)) {
            long added = Long.valueOf(contact.get(CONTACT_ADDED));
            callContact.setAddedDate(new Date(added*1000));
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

    public void setContacts(List<Map<String,String>> contacts) {
        for (Map<String, String> contact : contacts) {
            addContact(contact);
        }
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

    public TrustRequest getRequest(String id) {
        return mRequests.get(id);
    }

    public void addRequest(TrustRequest request) {
        mRequests.put(request.getContactId(), request);
    }

    public void setRequests(List<TrustRequest> requests) {
        for (TrustRequest request : requests)
            addRequest(request);
    }

    public void removeRequest(String id) {
        mRequests.remove(id);
    }
}
