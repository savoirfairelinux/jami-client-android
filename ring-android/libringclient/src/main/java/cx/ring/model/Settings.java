/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
package cx.ring.model;

public class Settings {
    private boolean mAllowMobileData;
    private boolean mAllowPushNotifications;
    private boolean mAllowPersistentNotification;
    private boolean mAllowSystemContacts;
    private boolean mAllowPlaceSystemCalls;
    private boolean mAllowRingOnStartup;
    private boolean mHdUpload;
    private boolean mHwEncoding;

    public Settings() {
    }
    public Settings(Settings s) {
        mAllowMobileData = s.mAllowMobileData;
        mAllowPushNotifications = s.mAllowPushNotifications;
        mAllowPersistentNotification = s.mAllowPersistentNotification;
        mAllowSystemContacts = s.mAllowSystemContacts;
        mAllowPlaceSystemCalls = s.mAllowPlaceSystemCalls;
        mAllowRingOnStartup = s.mAllowRingOnStartup;
        mHdUpload = s.mHdUpload;
    }

    public boolean isAllowMobileData() {
        return mAllowMobileData;
    }

    public void setAllowMobileData(boolean allowMobileData) {
        this.mAllowMobileData = allowMobileData;
    }
    public boolean isAllowPushNotifications() {
        return mAllowPushNotifications;
    }

    public void setAllowPushNotifications(boolean push) {
        this.mAllowPushNotifications = push;
    }

    public boolean isAllowSystemContacts() {
        return mAllowSystemContacts;
    }

    public void setAllowSystemContacts(boolean allowSystemContacts) {
        this.mAllowSystemContacts = allowSystemContacts;
    }

    public boolean isAllowPlaceSystemCalls() {
        return mAllowPlaceSystemCalls;
    }

    public void setAllowPlaceSystemCalls(boolean allowPlaceSystemCalls) {
        this.mAllowPlaceSystemCalls = allowPlaceSystemCalls;
    }

    public boolean isAllowRingOnStartup() {
        return mAllowRingOnStartup;
    }

    public void setAllowRingOnStartup(boolean allowRingOnStartup) {
        this.mAllowRingOnStartup = allowRingOnStartup;
    }

    public void setAllowPersistentNotification(boolean checked) {
        this.mAllowPersistentNotification = checked;
    }

    public boolean isAllowPersistentNotification() {
        return mAllowPersistentNotification;
    }
}
