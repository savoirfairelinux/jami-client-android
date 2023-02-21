/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
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
package cx.ring.services

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.text.TextUtils
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.utils.DeviceUtils
import cx.ring.utils.NetworkUtils
import net.jami.model.Settings
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.DeviceRuntimeService
import net.jami.services.PreferencesService
import java.util.*

class SharedPreferencesServiceImpl(private val context: Context, accountService: AccountService, deviceService: DeviceRuntimeService)
    : PreferencesService(accountService, deviceService) {
    private val mNotifiedRequests: MutableMap<String, MutableSet<String>> = HashMap()

    override fun saveSettings(settings: Settings) {
        preferences.edit()
            .clear()
            .putBoolean(PREF_SYSTEM_CONTACTS, settings.useSystemContacts)
            .putBoolean(PREF_PLACE_CALLS, settings.allowPlaceSystemCalls)
            .putBoolean(PREF_ON_STARTUP, settings.runOnStartup)
            .putBoolean(PREF_PUSH_NOTIFICATIONS, settings.enablePushNotifications)
            .putBoolean(PREF_PERSISTENT_NOTIFICATION, settings.enablePermanentService)
            .putBoolean(PREF_ADD_GROUP, settings.enableAddGroup)
            .putBoolean(PREF_SHOW_TYPING, settings.enableTypingIndicator)
            .putBoolean(PREF_SHOW_READ, settings.enableReadIndicator)
            .putBoolean(PREF_BLOCK_RECORD, settings.isRecordingBlocked)
            .putBoolean(PREF_LINK_PREVIEWS, settings.enableLinkPreviews)
            .putInt(PREF_NOTIFICATION_VISIBILITY, settings.notificationVisibility)
            .apply()
    }

    override fun loadSettings(): Settings {
        val appPrefs = preferences
        return Settings(
            useSystemContacts = appPrefs.getBoolean(PREF_SYSTEM_CONTACTS, false),
            allowPlaceSystemCalls = appPrefs.getBoolean(PREF_PLACE_CALLS, false),
            runOnStartup = appPrefs.getBoolean(PREF_ON_STARTUP, true),
            enablePushNotifications = appPrefs.getBoolean(PREF_PUSH_NOTIFICATIONS, false),
            enablePermanentService = appPrefs.getBoolean(PREF_PERSISTENT_NOTIFICATION, false),
            enableAddGroup = appPrefs.getBoolean(PREF_ADD_GROUP, false),
            enableTypingIndicator = appPrefs.getBoolean(PREF_SHOW_TYPING, true),
            enableReadIndicator = appPrefs.getBoolean(PREF_SHOW_READ, true),
            isRecordingBlocked = appPrefs.getBoolean(PREF_BLOCK_RECORD, false),
            enableLinkPreviews = appPrefs.getBoolean(PREF_LINK_PREVIEWS, true),
            notificationVisibility = appPrefs.getInt(PREF_NOTIFICATION_VISIBILITY, 0)
        )
    }

    private fun saveRequests(accountId: String, requests: Set<String>) {
        val preferences = context.getSharedPreferences(PREFS_REQUESTS, Context.MODE_PRIVATE)
        val edit = preferences.edit()
        edit.putStringSet(accountId, requests)
        edit.apply()
    }

    override fun saveRequestPreferences(accountId: String, contactId: String) {
        val requests = loadRequestsPreferences(accountId)
        requests.add(contactId)
        saveRequests(accountId, requests)
    }

    override fun loadRequestsPreferences(accountId: String): MutableSet<String> {
        var requests = mNotifiedRequests[accountId]
        if (requests == null) {
            val preferences = context.getSharedPreferences(PREFS_REQUESTS, Context.MODE_PRIVATE)
            requests = HashSet(preferences.getStringSet(accountId, null) ?: HashSet())
            mNotifiedRequests[accountId] = requests
        }
        return requests
    }

    override fun removeRequestPreferences(accountId: String, contactId: String) {
        val requests = loadRequestsPreferences(accountId)
        requests.remove(contactId)
        saveRequests(accountId, requests)
    }

    override fun hasNetworkConnected(): Boolean = NetworkUtils.isConnectivityAllowed(context)

    override val isPushAllowed: Boolean
        get() {
            val token = JamiApplication.instance?.pushToken
            return settings.enablePushNotifications && !TextUtils.isEmpty(token) /*&& NetworkUtils.isPushAllowed(mContext, getSettings().allowMobileData())*/
        }

    override val resolution: Int
        get() = videoPreferences.getString(PREF_RESOLUTION, if (DeviceUtils.isTv(context)) context.getString(R.string.video_resolution_default_tv) else context.getString(R.string.video_resolution_default))!!.toInt()

    override val bitrate: Int
        get() = videoPreferences.getString(PREF_BITRATE, context.getString(R.string.video_bitrate_default))!!.toInt()

    override val isHardwareAccelerationEnabled: Boolean
        get() = videoPreferences.getBoolean(PREF_HW_ENCODING, true)

    override var darkMode: Boolean
        get() = themePreferences.getBoolean(PREF_DARK_MODE, false)
        set(enabled) {
            themePreferences.edit().putBoolean(PREF_DARK_MODE, enabled).apply()
            applyDarkMode(enabled)
        }

    override fun loadDarkMode() {
        applyDarkMode(darkMode)
    }

    override fun getMaxFileAutoAccept(accountId: String): Int {
        return context.getSharedPreferences(PREFS_ACCOUNT + accountId, Context.MODE_PRIVATE)
            .getInt(PREF_ACCEPT_IN_MAX_SIZE, 30) * 1024 * 1024
    }

    private fun applyDarkMode(enabled: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM else AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
        )
    }

    private val preferences: SharedPreferences = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
    private val videoPreferences: SharedPreferences = context.getSharedPreferences(PREFS_VIDEO, Context.MODE_PRIVATE)
    private val themePreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        const val PREFS_SETTINGS = "ring_settings"
        private const val PREFS_REQUESTS = "ring_requests"
        const val PREFS_THEME = "theme"
        const val PREFS_VIDEO = "videoPrefs"
        const val PREFS_ACCOUNT = "account_"
        private const val PREF_PUSH_NOTIFICATIONS = "push_notifs"
        private const val PREF_PERSISTENT_NOTIFICATION = "persistent_notif"
        private const val PREF_ADD_GROUP = "add_group"
        private const val PREF_SHOW_TYPING = "persistent_typing"
        private const val PREF_SHOW_READ = "persistent_read"
        private const val PREF_BLOCK_RECORD = "persistent_block_record"
        private const val PREF_LINK_PREVIEWS = "link_previews_enable"
        private const val PREF_NOTIFICATION_VISIBILITY = "persistent_notification"
        private const val PREF_HW_ENCODING = "video_hwenc"
        const val PREF_BITRATE = "video_bitrate"
        const val PREF_RESOLUTION = "video_resolution"
        private const val PREF_SYSTEM_CONTACTS = "system_contacts"
        private const val PREF_PLACE_CALLS = "place_calls"
        private const val PREF_ON_STARTUP = "on_startup"
        const val PREF_DARK_MODE = "darkMode"
        private const val PREF_ACCEPT_IN_MAX_SIZE = "acceptIncomingFilesMaxSize"
        const val PREF_PLUGINS = "plugins"

        fun getConversationPreferences(context: Context, accountId: String, conversationUri: Uri): SharedPreferences {
            return context.getSharedPreferences(accountId + "_" + conversationUri.uri, Context.MODE_PRIVATE)
        }
    }
}