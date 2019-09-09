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
import androidx.appcompat.app.AppCompatDelegate;

import android.os.Build;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.model.Settings;
import cx.ring.utils.DeviceUtils;
import cx.ring.utils.NetworkUtils;

public class SharedPreferencesServiceImpl extends PreferencesService {

    public static final String RING_SETTINGS = "ring_settings";
    private static final String RING_REQUESTS = "ring_requests";
    public static final String PREFS_VIDEO = "video_settings";
    public static final String PREFS_THEME = "theme";
    private static final String RING_PUSH_NOTIFICATIONS = "push_notifs";
    private static final String RING_PERSISTENT_NOTIFICATION = "persistent_notif";
    private static final String RING_HW_ENCODING = "video_hwenc";
    public static final String RING_BITRATE = "video_bitrate";
    public static final String RING_RESOLUTION = "video_resolution";
    private static final String RING_SYSTEM_CONTACTS = "system_contacts";
    private static final String RING_PLACE_CALLS = "place_calls";
    private static final String RING_ON_STARTUP = "on_startup";
    public static final String PREF_DARK_MODE= "darkMode";
    private final Map<String, Set<String>> mNotifiedRequests = new HashMap<>();

    @Inject
    protected Context mContext;

    @Override
    protected void saveSettings(Settings settings) {
        SharedPreferences appPrefs = getPreferences();
        SharedPreferences.Editor edit = appPrefs.edit();
        edit.clear();
        edit.putBoolean(RING_SYSTEM_CONTACTS, settings.isAllowSystemContacts());
        edit.putBoolean(RING_PLACE_CALLS, settings.isAllowPlaceSystemCalls());
        edit.putBoolean(RING_ON_STARTUP, settings.isAllowRingOnStartup());
        edit.putBoolean(RING_PUSH_NOTIFICATIONS, settings.isAllowPushNotifications());
        edit.putBoolean(RING_PERSISTENT_NOTIFICATION, settings.isAllowPersistentNotification());
        edit.apply();
    }

    @Override
    protected Settings loadSettings() {
        SharedPreferences appPrefs = getPreferences();
        Settings settings = getUserSettings();
        if (settings == null) {
            settings = new Settings();
        }
        settings.setAllowSystemContacts(appPrefs.getBoolean(RING_SYSTEM_CONTACTS, false));
        settings.setAllowPlaceSystemCalls(appPrefs.getBoolean(RING_PLACE_CALLS, false));
        settings.setAllowRingOnStartup(appPrefs.getBoolean(RING_ON_STARTUP, true));
        settings.setAllowPushNotifications(appPrefs.getBoolean(RING_PUSH_NOTIFICATIONS, false));
        settings.setAllowPersistentNotification(appPrefs.getBoolean(RING_PERSISTENT_NOTIFICATION, false));
        return settings;
    }

    private void saveRequests(String accountId, Set<String> requests) {
        SharedPreferences preferences = mContext.getSharedPreferences(RING_REQUESTS, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = getPreferences().edit();
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
        return NetworkUtils.isConnectivityAllowed(mContext);
    }

    @Override
    public boolean isPushAllowed() {
        String token = RingApplication.getInstance().getPushToken();
        return getSettings().isAllowPushNotifications() && !TextUtils.isEmpty(token) /*&& NetworkUtils.isPushAllowed(mContext, getSettings().isAllowMobileData())*/;
    }

    @Override
    public int getResolution() {
        return Integer.parseInt(getVideoPreferences().getString(RING_RESOLUTION,
                DeviceUtils.isTv(mContext)
                        ? mContext.getString(R.string.video_resolution_default_tv)
                        : mContext.getString(R.string.video_resolution_default)));
    }

    @Override
    public int getBitrate() {
        return Integer.parseInt(getVideoPreferences().getString(RING_BITRATE, mContext.getString(R.string.video_bitrate_default)));
    }

    @Override
    public boolean isHardwareAccelerationEnabled() {
        return getVideoPreferences().getBoolean(RING_HW_ENCODING, true);
    }

    @Override
    public void setDarkMode(boolean enabled) {
        SharedPreferences.Editor edit = getThemePreferences().edit();
        edit.clear();
        edit.putBoolean(PREF_DARK_MODE, enabled);
        edit.apply();
        applyDarkMode(enabled);
    }

    @Override
    public boolean getDarkMode() {
        return getThemePreferences().getBoolean(PREF_DARK_MODE, false);
    }

    @Override
    public void loadDarkMode() {
        applyDarkMode(getDarkMode());
    }

    private void applyDarkMode(boolean enabled) {
        AppCompatDelegate.setDefaultNightMode(
                enabled ? AppCompatDelegate.MODE_NIGHT_YES
                        : Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                            ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                            : AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
    }

    private SharedPreferences getPreferences() {
        return mContext.getSharedPreferences(RING_SETTINGS, Context.MODE_PRIVATE);
    }

    private SharedPreferences getVideoPreferences() {
        return mContext.getSharedPreferences(PREFS_VIDEO, Context.MODE_PRIVATE);
    }

    private SharedPreferences getThemePreferences() {
        return mContext.getSharedPreferences(PREFS_VIDEO, Context.MODE_PRIVATE);
    }

}
