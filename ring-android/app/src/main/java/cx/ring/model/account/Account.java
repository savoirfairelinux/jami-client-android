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
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.model.account;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Account extends java.util.Observable {
    private static final String TAG = "Account";

    final String accountID;
    private AccountDetailBasic basicDetails = null;
    private AccountDetailAdvanced advancedDetails = null;
    private AccountDetailSrtp srtpDetails = null;
    private AccountDetailTls tlsDetails = null;
    private AccountDetailVolatile volatileDetails = null;
    private ArrayList<AccountCredentials> credentialsDetails;

    public Account(String bAccountID, final Map<String, String> details, final ArrayList<Map<String, String>> credentials, final Map<String, String> volatile_details) {
        accountID = bAccountID;
        basicDetails = new AccountDetailBasic(details);
        advancedDetails = new AccountDetailAdvanced(details);
        srtpDetails = new AccountDetailSrtp(details);
        tlsDetails = new AccountDetailTls(details);
        if (volatile_details != null)
            volatileDetails = new AccountDetailVolatile(volatile_details);
        if (credentials != null) {
            credentialsDetails = new ArrayList<>();
            for (int i = 0; i < credentials.size(); ++i) {
                credentialsDetails.add(new AccountCredentials(credentials.get(i)));
            }
        }
    }

    public String getAccountID() {
        return accountID;
    }

    public String getHost() {
        return basicDetails.getDetailString(AccountDetailBasic.CONFIG_ACCOUNT_HOSTNAME);
    }

    public void setHost(String host) {
        basicDetails.setDetailString(AccountDetailBasic.CONFIG_ACCOUNT_HOSTNAME, host);
    }

    public String getProxy() {
        return basicDetails.getDetailString(AccountDetailBasic.CONFIG_ACCOUNT_ROUTESET);
    }

    public void setProxy(String proxy) {
        basicDetails.setDetailString(AccountDetailBasic.CONFIG_ACCOUNT_ROUTESET, proxy);
    }

    public String getRegistered_state() {
        return volatileDetails.getDetailString(AccountDetailVolatile.CONFIG_ACCOUNT_REGISTRATION_STATUS);
    }

    public void setRegistrationState(String registered_state, int code) {
        Log.i(TAG, "setRegistrationState " + registered_state + " " + code);
        volatileDetails.setDetailString(AccountDetailVolatile.CONFIG_ACCOUNT_REGISTRATION_STATUS, registered_state);
        volatileDetails.setDetailString(AccountDetailVolatile.CONFIG_ACCOUNT_REGISTRATION_STATE_CODE, Integer.toString(code));
    }

    public void setVolatileDetails(Map<String, String> volatile_details) {
        volatileDetails = new AccountDetailVolatile(volatile_details);
    }

    public String getAlias() {
        return basicDetails.getDetailString(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS);
    }

    public Boolean isSip() {
        return basicDetails.getDetailString(AccountDetailBasic.CONFIG_ACCOUNT_TYPE).equals("SIP");
    }

    public Boolean isRing() {
        return basicDetails.getDetailString(AccountDetailBasic.CONFIG_ACCOUNT_TYPE).equals("RING");
    }

    public void setAlias(String alias) {
        basicDetails.setDetailString(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS, alias);
    }

    public AccountDetailBasic getBasicDetails() {
        return basicDetails;
    }

    public void setBasicDetails(AccountDetailBasic basicDetails) {
        this.basicDetails = basicDetails;
    }

    public AccountDetailAdvanced getAdvancedDetails() {
        return advancedDetails;
    }

    public void setAdvancedDetails(AccountDetailAdvanced advancedDetails) {
        this.advancedDetails = advancedDetails;
    }

    public AccountDetailSrtp getSrtpDetails() {
        return srtpDetails;
    }

    public void setSrtpDetails(AccountDetailSrtp srtpDetails) {
        this.srtpDetails = srtpDetails;
    }

    public AccountDetailTls getTlsDetails() {
        return tlsDetails;
    }

    public void setTlsDetails(AccountDetailTls tlsDetails) {
        this.tlsDetails = tlsDetails;
    }

    public boolean isEnabled() {
        return (basicDetails.getDetailString(AccountDetailBasic.CONFIG_ACCOUNT_ENABLE).contentEquals(AccountDetail.TRUE_STR));
    }

    public void setEnabled(boolean isChecked) {
        basicDetails.setDetailString(AccountDetailBasic.CONFIG_ACCOUNT_ENABLE, (isChecked ? AccountDetail.TRUE_STR
                : AccountDetail.FALSE_STR));
    }

    public HashMap<String, String> getDetails() {
        HashMap<String, String> results = new HashMap<>();

        results.putAll(basicDetails.getDetailsHashMap());
        results.putAll(advancedDetails.getDetailsHashMap());
        results.putAll(tlsDetails.getDetailsHashMap());
        results.putAll(srtpDetails.getDetailsHashMap());
        return results;
    }

    public boolean isTrying() {
        return getRegistered_state().contentEquals(AccountDetailVolatile.STATE_TRYING);
    }

    public boolean isRegistered() {
        return (getRegistered_state().contentEquals(AccountDetailVolatile.STATE_READY) || getRegistered_state().contentEquals(AccountDetailVolatile.STATE_REGISTERED));
    }

    public boolean isInError() {
        String state = getRegistered_state();
        return (state.contentEquals(AccountDetailVolatile.STATE_ERROR)
                || state.contentEquals(AccountDetailVolatile.STATE_ERROR_AUTH)
                || state.contentEquals(AccountDetailVolatile.STATE_ERROR_CONF_STUN)
                || state.contentEquals(AccountDetailVolatile.STATE_ERROR_EXIST_STUN)
                || state.contentEquals(AccountDetailVolatile.STATE_ERROR_GENERIC)
                || state.contentEquals(AccountDetailVolatile.STATE_ERROR_HOST)
                || state.contentEquals(AccountDetailVolatile.STATE_ERROR_NETWORK)
                || state.contentEquals(AccountDetailVolatile.STATE_ERROR_NOT_ACCEPTABLE)
                || state.contentEquals(AccountDetailVolatile.STATE_ERROR_SERVICE_UNAVAILABLE)
                || state.contentEquals(AccountDetailVolatile.STATE_REQUEST_TIMEOUT));
    }

    public boolean isIP2IP() {
        return isSip() && TextUtils.isEmpty(getHost());
    }

    public boolean isAutoanswerEnabled() {
        return basicDetails.getDetailString(AccountDetailBasic.CONFIG_ACCOUNT_AUTOANSWER).contentEquals(AccountDetail.TRUE_STR);
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
        ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
        for (AccountCredentials cred : credentialsDetails) {
            result.add(cred.getDetailsHashMap());
        }
        return result;
    }

    public boolean hasSDESEnabled() {
        return srtpDetails.getDetailString(AccountDetailSrtp.CONFIG_SRTP_KEY_EXCHANGE).contentEquals("sdes");
    }

    public boolean useSecureLayer() {
        return getSrtpDetails().getDetailBoolean(AccountDetailSrtp.CONFIG_SRTP_ENABLE) || getTlsDetails().getDetailBoolean(AccountDetailTls.CONFIG_TLS_ENABLE);
    }

    public String getShareURI() {
        String share_uri;
        if (isRing()) {
            share_uri = getBasicDetails().getUsername();
        } else {
            share_uri = getBasicDetails().getUsername() + "@" + getBasicDetails().getHostname();
        }

        return share_uri;
    }
}
