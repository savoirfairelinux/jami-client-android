/*
 *  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package org.sflphone.model;

import java.util.HashMap;

import org.sflphone.account.AccountDetailAdvanced;
import org.sflphone.account.AccountDetailBasic;
import org.sflphone.account.AccountDetailSrtp;
import org.sflphone.account.AccountDetailTls;

import android.os.Parcel;
import android.os.Parcelable;

public class Account implements Parcelable {

    String accountID;
    private AccountDetailBasic basicDetails = null;
    private AccountDetailAdvanced advancedDetails = null;
    private AccountDetailSrtp srtpDetails = null;
    private AccountDetailTls tlsDetails = null;

    public Account(String bAccountID, HashMap<String, String> details) {
        accountID = bAccountID;
        basicDetails = new AccountDetailBasic(details);
        advancedDetails = new AccountDetailAdvanced(details);
        srtpDetails = new AccountDetailSrtp(details);
        tlsDetails = new AccountDetailTls(details);
    }

    public String getAccountID() {
        return accountID;
    }

    public void setAccountID(String accountID) {
        this.accountID = accountID;
    }

    public String getHost() {
        return basicDetails.getDetailString(AccountDetailBasic.CONFIG_ACCOUNT_HOSTNAME);
    }

    public void setHost(String host) {
        basicDetails.setDetailString(AccountDetailBasic.CONFIG_ACCOUNT_HOSTNAME, host);
    }

    public String getRegistered_state() {
        return advancedDetails.getDetailString(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATUS);
    }

    public void setRegistered_state(String registered_state) {
        advancedDetails.setDetailString(AccountDetailAdvanced.CONFIG_ACCOUNT_REGISTRATION_STATUS, registered_state);
    }

    public String getAlias() {
        return basicDetails.getDetailString(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS);
    }

    public void setAlias(String alias) {
        basicDetails.setDetailString(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS, alias);
    }

    public Account(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int arg1) {

        dest.writeString(accountID);
        dest.writeSerializable(basicDetails.getDetailsHashMap());
        dest.writeSerializable(advancedDetails.getDetailsHashMap());
        dest.writeSerializable(tlsDetails.getDetailsHashMap());
        dest.writeSerializable(srtpDetails.getDetailsHashMap());
    }

    private void readFromParcel(Parcel in) {

        accountID = in.readString();
        basicDetails = new AccountDetailBasic((HashMap<String, String>) in.readSerializable());
        advancedDetails = new AccountDetailAdvanced((HashMap<String, String>) in.readSerializable());
        srtpDetails = new AccountDetailSrtp((HashMap<String, String>) in.readSerializable());
        tlsDetails = new AccountDetailTls((HashMap<String, String>) in.readSerializable());
    }

    public static final Parcelable.Creator<Account> CREATOR = new Parcelable.Creator<Account>() {
        @Override
        public Account createFromParcel(Parcel in) {
            return new Account(in);
        }

        @Override
        public Account[] newArray(int size) {
            return new Account[size];
        }
    };

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
        return (basicDetails.getDetailString(AccountDetailBasic.CONFIG_ACCOUNT_ENABLE)
                .contentEquals(AccountDetailBasic.CONFIG_ACCOUNT_DEFAULT_ENABLE));
    }

    public void setEnabled(boolean isChecked) {
        basicDetails.setDetailString(AccountDetailBasic.CONFIG_ACCOUNT_ENABLE, (isChecked ? AccountDetailAdvanced.TRUE_STR
                : AccountDetailAdvanced.FALSE_STR));
    }

    public HashMap<String, String> getDetails() {
        HashMap<String, String> results = new HashMap<String, String>();

        results.putAll(basicDetails.getDetailsHashMap());
        results.putAll(advancedDetails.getDetailsHashMap());
        results.putAll(tlsDetails.getDetailsHashMap());
        results.putAll(srtpDetails.getDetailsHashMap());
        return results;
    }

    public boolean isRegistered() {
        // FIXME Hardcoded values
        return (getRegistered_state().contentEquals("REGISTERED") || getRegistered_state().contentEquals("OK"));
    }

    public boolean isIP2IP() {
        return basicDetails.getDetailString(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS).contentEquals("IP2IP");
    }

    public boolean isAutoanswerEnabled() {
        return basicDetails.getDetailString(AccountDetailBasic.CONFIG_ACCOUNT_AUTOANSWER).contentEquals("true");
    }

}
