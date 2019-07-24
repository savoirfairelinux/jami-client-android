/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
 *  Author: Adrien Beraud <adrien.beraud@savoirfairelinux.com>
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
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import android.text.TextUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import cx.ring.application.RingApplication;
import cx.ring.model.Settings;
import cx.ring.utils.NetworkUtils;

public class SharedPreferencesServiceImpl extends PreferencesService {

    private static final String RING_SETTINGS = "ring_settings";
    private static final String RING_REQUESTS = "ring_requests";
    private static final String RING_MOBILE_DATA = "mobile_data";
    private static final String RING_PUSH_NOTIFICATIONS = "push_notifs";
    private static final String RING_PERSISTENT_NOTIFICATION = "persistent_notif";
    private static final String RING_HD = "hd_upload";
    private static final String RING_HW_ENCODING = "video_hwenc";
    private static final String RING_BITRATE = "video_bitrate";
    private static final String RING_RESOLUTION = "video_resolution";
    private static final String RING_SYSTEM_CONTACTS = "system_contacts";
    private static final String RING_PLACE_CALLS = "place_calls";
    private static final String RING_ON_STARTUP = "on_startup";
    private final Map<String, Set<String>> mNotifiedRequests = new HashMap<>();

    @Inject
    protected Context mContext;

    @Override
    protected void saveSettings(Settings settings) {
        SharedPreferences appPrefs = mContext.getSharedPreferences(RING_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = appPrefs.edit();
        edit.clear();
        edit.putBoolean(RING_MOBILE_DATA, settings.isAllowMobileData());
        edit.putBoolean(RING_SYSTEM_CONTACTS, settings.isAllowSystemContacts());
        edit.putBoolean(RING_PLACE_CALLS, settings.isAllowPlaceSystemCalls());
        edit.putBoolean(RING_ON_STARTUP, settings.isAllowRingOnStartup());
        edit.putBoolean(RING_PUSH_NOTIFICATIONS, settings.isAllowPushNotifications());
        edit.putBoolean(RING_PERSISTENT_NOTIFICATION, settings.isAllowPersistentNotification());
        edit.apply();
    }

    @Override
    protected Settings loadSettings() {
        SharedPreferences appPrefs = mContext.getSharedPreferences(RING_SETTINGS, Context.MODE_PRIVATE);
        Settings settings = getUserSettings();
        if (settings == null) {
            settings = new Settings();
        }
        settings.setAllowMobileData(appPrefs.getBoolean(RING_MOBILE_DATA, false));
        settings.setAllowSystemContacts(appPrefs.getBoolean(RING_SYSTEM_CONTACTS, false));
        settings.setAllowPlaceSystemCalls(appPrefs.getBoolean(RING_PLACE_CALLS, false));
        settings.setAllowRingOnStartup(appPrefs.getBoolean(RING_ON_STARTUP, true));
        settings.setAllowPushNotifications(appPrefs.getBoolean(RING_PUSH_NOTIFICATIONS, false));
        settings.setAllowPersistentNotification(appPrefs.getBoolean(RING_PERSISTENT_NOTIFICATION, false));
        return settings;
    }

    private void saveRequests(String accountId, Set<String> requests) {
        SharedPreferences preferences = mContext.getSharedPreferences(RING_REQUESTS, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putStringSet(accountId, requests);
        edit.apply();
    }

    @Override
    public void saveRequestPreferences(String accountId, String contactId) {
        Set<String> requests = loadRequestsPreferences(accountId);
        requests.add(contactId);
        saveRequests(accountId, requests);
    }

    @Override
    @NonNull
    public Set<String> loadRequestsPreferences(@NonNull String accountId) {
        Set<String> requests = mNotifiedRequests.get(accountId);
        if (requests == null) {
            SharedPreferences preferences = mContext.getSharedPreferences(RING_REQUESTS, Context.MODE_PRIVATE);
            requests = new HashSet<>(preferences.getStringSet(accountId, new HashSet<>()));
            mNotifiedRequests.put(accountId, requests);
        }
        return requests;
    }

    @Override
    public void removeRequestPreferences(String accountId, String contactId) {
        Set<String> requests = loadRequestsPreferences(accountId);
        requests.remove(contactId);
        saveRequests(accountId, requests);
    }

    @Override
    public boolean hasNetworkConnected() {
        return NetworkUtils.isConnectivityAllowed(mContext, getSettings().isAllowMobileData());
    }

    @Override
    public boolean isPushAllowed() {
        String token = RingApplication.getInstance().getPushToken();
        return getSettings().isAllowPushNotifications() && !TextUtils.isEmpty(token) /*&& NetworkUtils.isPushAllowed(mContext, getSettings().isAllowMobileData())*/;
    }

    @Override
    public String getResolution() {
        return getPreferenceValue(RING_RESOLUTION, "720");
    }

    @Override
    public String getBitrate() {
        return getPreferenceValue(RING_BITRATE, "Auto");
    }

    @Override
    public boolean isHardwareAccelerationEnabled() {
        return getPreferenceValue(RING_HW_ENCODING, null).equals("true");

    }

    private String getPreferenceValue(String key, String defaultValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        if(key.equals(RING_HW_ENCODING)) {
            return String.valueOf(prefs.getBoolean(key, false));
        }
        return prefs.getString(key, defaultValue);
    }
}
