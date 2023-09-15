/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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

data class Settings(
    val enablePushNotifications: Boolean,
    val enablePermanentService: Boolean,
    val enableAddGroup: Boolean,
    val useSystemContacts: Boolean,
    val allowPlaceSystemCalls: Boolean,
    val runOnStartup: Boolean,
    val enableTypingIndicator: Boolean,
    val enableLinkPreviews: Boolean,
    val isRecordingBlocked: Boolean,
    //val enableHwEncoding: Boolean,
    val notificationVisibility: Int = 0,
)

data class DonationSettings(
    val donationReminderVisibility: Boolean = true,
    val lastDismissed: Long = 0,
)
