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

package cx.ring.model.account;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Account extends java.util.Observable {
    private static final String TAG = "Account";

    private final String accountID;
    private AccountConfig volatileDetails;
    private ArrayList<AccountCredentials> credentialsDetails = new ArrayList<>();
    private AccountConfig details;
    private Map<String, String> devices = new HashMap<>();

    public OnDevicesChangedListener devicesListener = null;
    public OnExportEndedListener exportListener = null;
    public OnStateChangedListener stateListener = null;

    public boolean registeringUsername = false;

    public Account(String bAccountID) {
        accountID = bAccountID;
        details =  new AccountConfig();
        volatileDetails =  new AccountConfig();
    }

    public Account(String bAccountID, final Map<String, String> d, final ArrayList<Map<String, String>> credentials, final Map<String, String> volatile_details) {
        accountID = bAccountID;
        details =  new AccountConfig(d);
        volatileDetails = new AccountConfig(volatile_details);
        setCredentials(credentials);
    }

    public void update(Account acc) {
        String old = getRegistrationState();
        details = acc.details;
        volatileDetails = acc.volatileDetails;
        credentialsDetails = acc.credentialsDetails;
        devices = acc.devices;
        String new_reg_state = getRegistrationState();
        if (old != null && !old.contentEquals(new_reg_state)) {
            if (stateListener != null) {
                stateListener.stateChanged(new_reg_state, getRegistrationStateCode());
            }
        }
    }

    public Map<String, String> getDevices() {
        return devices;
    }

    public void setCredentials(ArrayList<Map<String, String>> credentials) {
        credentialsDetails.clear();
        if (credentials != null) {
            credentialsDetails.ensureCapacity(credentials.size());
            for (int i = 0; i < credentials.size(); ++i) {
                credentialsDetails.add(new AccountCredentials(credentials.get(i)));
            }
        }
    }

    public void setDetails(Map<String, String> details) {
        this.details = new AccountConfig(details);
    }

    public void setDetail(ConfigKey key, String d) {
        details.put(key, d);
    }
    public void setDetail(ConfigKey key, boolean b) {
        details.put(key, b);
    }

    public AccountConfig getConfig() {
        return details;
    }

    public interface OnDevicesChangedListener {
        void devicesChanged(Map<String, String> devices);
    }
    public interface OnExportEndedListener {
        void exportEnded(int code, String pin);
    }
    public interface OnStateChangedListener {
        void stateChanged(String state, int code);
    }

    public void setDevices(Map<String, String> devs) {
        devices = devs;
        if (devicesListener != null)
            devicesListener.devicesChanged(devs);
    }

    public String getAccountID() {
        return accountID;
    }

    public String getUsername() {
        return details.get(ConfigKey.ACCOUNT_USERNAME);
    }

    public String getHost() {
        return details.get(ConfigKey.ACCOUNT_HOSTNAME);
    }

    public void setHost(String host) {
        details.put(ConfigKey.ACCOUNT_HOSTNAME, host);
    }

    public String getProxy() {
        return details.get(ConfigKey.ACCOUNT_ROUTESET);
    }

    public void setProxy(String proxy) {
        details.put(ConfigKey.ACCOUNT_ROUTESET, proxy);
    }

    public String getRegistrationState() {
        return volatileDetails.get(ConfigKey.ACCOUNT_REGISTRATION_STATUS);
    }
    public int getRegistrationStateCode() {
        String code_str = volatileDetails.get(ConfigKey.ACCOUNT_REGISTRATION_STATE_CODE);
        if (code_str == null || code_str.isEmpty())
            return 0;
        return Integer.parseInt(code_str);
    }

    public void setRegistrationState(String registered_state, int code) {
        String old = getRegistrationState();
        volatileDetails.put(ConfigKey.ACCOUNT_REGISTRATION_STATUS, registered_state);
        volatileDetails.put(ConfigKey.ACCOUNT_REGISTRATION_STATE_CODE, Integer.toString(code));
        if (old != null && !old.contentEquals(registered_state)) {
            if (stateListener != null) {
                stateListener.stateChanged(registered_state, code);
            }
        }
    }

    public void setVolatileDetails(Map<String, String> volatile_details) {
        String state_old = getRegistrationState();
        volatileDetails = new AccountConfig(volatile_details);
        String state_new = getRegistrationState();
        if (!state_old.contentEquals(state_new)) {
            if (stateListener != null) {
                stateListener.stateChanged(state_new, getRegistrationStateCode());
            }
        }
    }

    public String getRegisteredName() {
        return volatileDetails.get(ConfigKey.ACCOUNT_REGISTRED_NAME);
    }

    public String getAlias() {
        return details.get(ConfigKey.ACCOUNT_ALIAS);
    }

    public Boolean isSip() {
        return details.get(ConfigKey.ACCOUNT_TYPE).equals(AccountConfig.ACCOUNT_TYPE_SIP);
    }

    public Boolean isRing() {
        return details.get(ConfigKey.ACCOUNT_TYPE).equals(AccountConfig.ACCOUNT_TYPE_RING);
    }

    public void setAlias(String alias) {
        details.put(ConfigKey.ACCOUNT_ALIAS, alias);
    }

    public String getDetail(ConfigKey k) {
        return details.get(k);
    }

    public boolean getDetailBoolean(ConfigKey k) {
        return details.getBool(k);
    }

    public boolean isEnabled() {
        return details.getBool(ConfigKey.ACCOUNT_ENABLE);
    }

    public void setEnabled(boolean isChecked) {
        details.put(ConfigKey.ACCOUNT_ENABLE, isChecked);
    }

    public HashMap<String, String> getDetails() {
        return details.getAll();
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
        return isSip() && TextUtils.isEmpty(getHost());
    }

    public boolean isAutoanswerEnabled() {
        return details.getBool(ConfigKey.ACCOUNT_AUTOANSWER);
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

    @Override
    public boolean hasChanged() {
        return true;
    }

    public List getCredentialsHashMapList() {
        ArrayList<HashMap<String, String>> result = new ArrayList<>();
        for (AccountCredentials cred : credentialsDetails) {
            result.add(cred.getDetails());
        }
        return result;
    }

    public boolean hasSDESEnabled() {
        return details.get(ConfigKey.SRTP_KEY_EXCHANGE).contentEquals("sdes");
    }

    public boolean useSecureLayer() {
        return details.getBool(ConfigKey.SRTP_ENABLE) || details.getBool(ConfigKey.TLS_ENABLE);
    }

    public String getShareURI() {
        String share_uri;
        if (isRing()) {
            share_uri = getUsername();
        } else {
            share_uri = getUsername() + "@" + getHost();
        }

        return share_uri;
    }

    public boolean needsMigration () {
        return AccountConfig.STATE_NEED_MIGRATION.equals(getRegistrationState());
    }
}
