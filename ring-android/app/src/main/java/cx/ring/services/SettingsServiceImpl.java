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

import javax.inject.Inject;

import cx.ring.model.Settings;

public class SettingsServiceImpl extends SettingsService {

    public static final String RING_SETTINGS = "ring_settings";

    public static final String RING_MOBILE_DATA = "mobile_data";
    public static final String RING_SYSTEM_CONTACTS = "system_contacts";
    public static final String RING_PLACE_CALLS = "place_calls";
    public static final String RING_ON_STARTUP = "on_startup";

    @Inject
    protected Context mContext;

    public SettingsServiceImpl() {
        userSettings = null;
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

        if (null == userSettings) {
            userSettings = new Settings();
        }

        userSettings.setAllowMobileData(appPrefs.getBoolean(RING_MOBILE_DATA, false));
        userSettings.setAllowSystemContacts(appPrefs.getBoolean(RING_SYSTEM_CONTACTS, true));
        userSettings.setAllowPlaceSystemCalls(appPrefs.getBoolean(RING_PLACE_CALLS, false));
        userSettings.setAllowRingOnStartup(appPrefs.getBoolean(RING_ON_STARTUP, true));

        return userSettings;
    }

    @Override
    public Settings getUserSettings() {
        if (null == userSettings) {
            userSettings = loadSettings();
        }
        return userSettings;
    }
}
