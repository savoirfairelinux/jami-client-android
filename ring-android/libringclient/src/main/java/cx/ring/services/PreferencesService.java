/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.services;

import java.util.Set;

import javax.inject.Inject;

import cx.ring.model.Settings;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public abstract class PreferencesService {

    @Inject
    AccountService mAccountService;

    @Inject
    DeviceRuntimeService mDeviceService;

    private Settings mUserSettings;
    private final Subject<Settings> mSettingsSubject = BehaviorSubject.create();

    protected abstract Settings loadSettings();
    protected abstract void saveSettings(Settings settings);

    public Settings getSettings() {
        if (mUserSettings == null) {
            mUserSettings = loadSettings();
            mSettingsSubject.onNext(mUserSettings);
        }
        return new Settings(mUserSettings);
    }

    public void setSettings(Settings settings) {
        saveSettings(settings);
        boolean allowPush = settings.isAllowPushNotifications();
        if (mUserSettings == null || mUserSettings.isAllowPushNotifications() != allowPush) {
            mAccountService.setPushNotificationToken(allowPush ? mDeviceService.getPushToken() : "");
            mAccountService.setProxyEnabled(allowPush);
        }
        mUserSettings = settings;
        mSettingsSubject.onNext(settings);
    }

    protected Settings getUserSettings() {
        return mUserSettings;
    }

    public Observable<Settings> getSettingsSubject() {
        return mSettingsSubject;
    }

    public abstract boolean hasNetworkConnected();

    public abstract boolean isPushAllowed();

    public abstract void saveRequestPreferences(String accountId, String contactId);

    public abstract Set<String> loadRequestsPreferences(String accountId);

    public abstract void removeRequestPreferences(String accountId, String contactId);

    public abstract int getResolution();

    public abstract int getBitrate();

    public abstract boolean isHardwareAccelerationEnabled();



}
