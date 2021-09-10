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
package net.jami.model

class Settings(s: Settings? = null) {
    var isAllowPushNotifications = false
    var isAllowPersistentNotification = false
    var isAllowSystemContacts = false
    var isAllowPlaceSystemCalls = false
    var isAllowOnStartup = false
        private set
    var isAllowTypingIndicator = false
    var isAllowReadIndicator = false
    var isRecordingBlocked = false
        private set
    private var mHwEncoding = false
    var notificationVisibility = 0

    init {
        if (s != null) {
            isAllowPushNotifications = s.isAllowPushNotifications
            isAllowPersistentNotification = s.isAllowPersistentNotification
            isAllowSystemContacts = s.isAllowSystemContacts
            isAllowPlaceSystemCalls = s.isAllowPlaceSystemCalls
            isAllowOnStartup = s.isAllowOnStartup
            isAllowTypingIndicator = s.isAllowTypingIndicator
            isAllowReadIndicator = s.isAllowReadIndicator
            isRecordingBlocked = s.isRecordingBlocked
            mHwEncoding = s.mHwEncoding
            notificationVisibility = s.notificationVisibility
        }
    }

    fun setAllowRingOnStartup(allowRingOnStartup: Boolean) {
        isAllowOnStartup = allowRingOnStartup
    }

    fun setBlockRecordIndicator(checked: Boolean) {
        isRecordingBlocked = checked
    }
}