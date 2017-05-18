/*
 *  Copyright (C) 2016 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
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
package cx.ring.services;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import cx.ring.model.Preferences;
import cx.ring.model.Settings;

public class SharedPreferencesServiceImpl extends PreferencesService {

    private static final String RING_SETTINGS = "ring_settings";
    private static final String RING_REQUESTS = "ring_requests";
    private static final String RING_GENERAL_PREFERENCES = "ring_general_preferences";

    private static final String RING_MOBILE_DATA = "mobile_data";
    private static final String RING_SYSTEM_CONTACTS = "system_contacts";
    private static final String RING_PLACE_CALLS = "place_calls";
    private static final String RING_ON_STARTUP = "on_startup";

    private static final String ACCOUNT_STATUS = "Account.status";
    private static final String ACCOUNT_ALIAS = "Account.alias";
    private static final String ACCOUNT_HOSTNAME = "Account.hostname";
    private static final String ACCOUNT_USERNAME = "Account.username";
    private static final String ACCOUNT_PASSWORD = "Account.password";
    private static final String ACCOUNT_ROUTESET = "Account.routeset";
    private static final String ACCOUNT_USERAGENT = "Account.useragent";
    private static final String ACCOUNT_AUTOANSWER = "Account.autoAnswer";
    private static final String ACCOUNT_UPNP_ENABLED = "Account.upnpEnabled";

    @Inject
    protected Context mContext;

    @Inject
    protected DeviceRuntimeService mDevideRuntimeService;

    public SharedPreferencesServiceImpl() {
        mUserSettings = null;
    }

    @Override
    public void saveSettings(Settings settings) {
        SharedPreferences appPrefs = mContext.getSharedPreferences(RING_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = appPrefs.edit();
        edit.clear();
        edit.putBoolean(RING_MOBILE_DATA, settings.isAllowMobileData());
        edit.putBoolean(RING_SYSTEM_CONTACTS, settings.isAllowSystemContacts());
        edit.putBoolean(RING_PLACE_CALLS, settings.isAllowPlaceSystemCalls());
        edit.putBoolean(RING_ON_STARTUP, settings.isAllowRingOnStartup());

        edit.apply();

        // notify the observers
        setChanged();
        notifyObservers();

        loadSettings();
    }

    @Override
    public Settings loadSettings() {
        SharedPreferences appPrefs = mContext.getSharedPreferences(RING_SETTINGS, Context.MODE_PRIVATE);

        if (null == mUserSettings) {
            mUserSettings = new Settings();
        }

        mUserSettings.setAllowMobileData(appPrefs.getBoolean(RING_MOBILE_DATA, false));
        mUserSettings.setAllowSystemContacts(appPrefs.getBoolean(RING_SYSTEM_CONTACTS, true));
        mUserSettings.setAllowPlaceSystemCalls(appPrefs.getBoolean(RING_PLACE_CALLS, false));
        mUserSettings.setAllowRingOnStartup(appPrefs.getBoolean(RING_ON_STARTUP, true));

        return mUserSettings;
    }

    @Override
    public Preferences loadGeneralPreferences(String accountId) {
        SharedPreferences appPrefs = mContext.getSharedPreferences(RING_GENERAL_PREFERENCES + accountId, Context.MODE_PRIVATE);
        Preferences prefs = new Preferences();

        prefs.setStatus(appPrefs.getBoolean(ACCOUNT_STATUS, false));
        prefs.setAlias(appPrefs.getString(ACCOUNT_ALIAS, ""));
        prefs.setHostname(appPrefs.getString(ACCOUNT_HOSTNAME, ""));
        prefs.setUsername(appPrefs.getString(ACCOUNT_USERNAME, ""));
        prefs.setPassword(appPrefs.getString(ACCOUNT_PASSWORD, ""));
        prefs.setRootset(appPrefs.getString(ACCOUNT_ROUTESET, ""));
        prefs.setUseragent(appPrefs.getString(ACCOUNT_USERAGENT, ""));
        prefs.setAutoAnswer(appPrefs.getBoolean(ACCOUNT_AUTOANSWER, false));

        return prefs;
    }

    @Override
    public Preferences loadRingPreferences(String accountId) {
        SharedPreferences appPrefs = mContext.getSharedPreferences(RING_GENERAL_PREFERENCES + accountId, Context.MODE_PRIVATE);
        Preferences prefs = new Preferences();

        prefs.setAlias(appPrefs.getString(ACCOUNT_ALIAS, ""));
        prefs.setHostname(appPrefs.getString(ACCOUNT_HOSTNAME, ""));
        prefs.setUseragent(appPrefs.getString(ACCOUNT_USERAGENT, ""));
        prefs.setAutoAnswer(appPrefs.getBoolean(ACCOUNT_AUTOANSWER, false));
        prefs.setUpnpEnabled(appPrefs.getBoolean(ACCOUNT_UPNP_ENABLED, false));

        return prefs;
    }

    @Override
    public Settings getUserSettings() {
        if (null == mUserSettings) {
            mUserSettings = loadSettings();
        }
        return mUserSettings;
    }

    private void saveRequests(String accountId, Set<String> requests) {
        SharedPreferences preferences = mContext.getSharedPreferences(RING_REQUESTS, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = preferences.edit();
        edit.clear();
        edit.putStringSet(accountId, requests);
        edit.apply();
    }

    @Override
    public void saveRequestPreferences(String accountId, String contactId) {
        Set<String> requests = loadRequestsPreferences(accountId);
        if (requests == null) {
            requests = new HashSet<>();
        }

        requests.add(contactId);
        saveRequests(accountId, requests);
    }

    @Override
    public Set<String> loadRequestsPreferences(String accountId) {
        SharedPreferences preferences = mContext.getSharedPreferences(RING_REQUESTS, Context.MODE_PRIVATE);
        return preferences.getStringSet(accountId, null);
    }

    @Override
    public void removeRequestPreferences(String accountId, String contactId) {
        Set<String> requests = loadRequestsPreferences(accountId);
        if (requests == null) {
            return;
        }

        requests.remove(contactId);
        saveRequests(accountId, requests);
    }


    @Override
    public boolean isConnectedWifiAndMobile() {
        return mDevideRuntimeService.isConnectedWifi() || (mDevideRuntimeService.isConnectedMobile() && getUserSettings().isAllowMobileData());
    }

}
