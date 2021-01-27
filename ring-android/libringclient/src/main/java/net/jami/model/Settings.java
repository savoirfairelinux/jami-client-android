/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package net.jami.model;

public class Settings {
    private boolean mAllowPushNotifications;
    private boolean mAllowPersistentNotification;
    private boolean mAllowSystemContacts;
    private boolean mAllowPlaceSystemCalls;
    private boolean mAllowOnStartup;
    private boolean mAllowTypingIndicator;
    private boolean mAllowReadIndicator;
    private boolean mBlockRecordIndicator;
    private boolean mHwEncoding;

    public Settings() {
    }

    public Settings(Settings s) {
        if (s != null) {
            mAllowPushNotifications = s.mAllowPushNotifications;
            mAllowPersistentNotification = s.mAllowPersistentNotification;
            mAllowSystemContacts = s.mAllowSystemContacts;
            mAllowPlaceSystemCalls = s.mAllowPlaceSystemCalls;
            mAllowOnStartup = s.mAllowOnStartup;
            mAllowTypingIndicator = s.mAllowTypingIndicator;
            mAllowReadIndicator = s.mAllowReadIndicator;
            mBlockRecordIndicator = s.mBlockRecordIndicator;
            mHwEncoding = s.mHwEncoding;
        }
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

    public boolean isAllowOnStartup() {
        return mAllowOnStartup;
    }

    public void setAllowRingOnStartup(boolean allowRingOnStartup) {
        this.mAllowOnStartup = allowRingOnStartup;
    }

    public void setAllowPersistentNotification(boolean checked) {
        this.mAllowPersistentNotification = checked;
    }

    public boolean isAllowPersistentNotification() {
        return mAllowPersistentNotification;
    }

    public void setAllowTypingIndicator(boolean checked) {
        this.mAllowTypingIndicator = checked;
    }

    public boolean isAllowTypingIndicator() {
        return mAllowTypingIndicator;
    }

    public void setAllowReadIndicator(boolean checked) {
        this.mAllowReadIndicator = checked;
    }

    public boolean isAllowReadIndicator() {
        return mAllowReadIndicator;
    }

    public void setBlockRecordIndicator(boolean checked) {
        this.mBlockRecordIndicator = checked;
    }

    public boolean isRecordingBlocked() {
        return mBlockRecordIndicator;
    }

}
