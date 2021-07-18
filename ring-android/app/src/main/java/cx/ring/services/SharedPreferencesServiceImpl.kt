/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import net.jami.services.PreferencesService
import java.util.HashMap
import android.content.SharedPreferences
import java.util.HashSet
import cx.ring.utils.NetworkUtils
import cx.ring.application.JamiApplication
import android.text.TextUtils
import cx.ring.utils.DeviceUtils
import cx.ring.R
import androidx.appcompat.app.AppCompatDelegate
import android.os.Build
import androidx.preference.PreferenceManager
import net.jami.model.Settings
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.DeviceRuntimeService

class SharedPreferencesServiceImpl(val mContext: Context, accountService: AccountService, deviceService: DeviceRuntimeService) : PreferencesService(accountService, deviceService) {
    private val mNotifiedRequests: MutableMap<String, MutableSet<String>> = HashMap()

    override fun saveSettings(settings: Settings) {
        val appPrefs = preferences
        val edit = appPrefs.edit()
        edit.clear()
        edit.putBoolean(PREF_SYSTEM_CONTACTS, settings.isAllowSystemContacts)
        edit.putBoolean(PREF_PLACE_CALLS, settings.isAllowPlaceSystemCalls)
        edit.putBoolean(PREF_ON_STARTUP, settings.isAllowOnStartup)
        edit.putBoolean(PREF_PUSH_NOTIFICATIONS, settings.isAllowPushNotifications)
        edit.putBoolean(PREF_PERSISTENT_NOTIFICATION, settings.isAllowPersistentNotification)
        edit.putBoolean(PREF_SHOW_TYPING, settings.isAllowTypingIndicator)
        edit.putBoolean(PREF_SHOW_READ, settings.isAllowReadIndicator)
        edit.putBoolean(PREF_BLOCK_RECORD, settings.isRecordingBlocked)
        edit.putInt(PREF_NOTIFICATION_VISIBILITY, settings.notificationVisibility)
        edit.apply()
    }

    override fun loadSettings(): Settings {
        val appPrefs = preferences
        val settings = userSettings ?: Settings()
        settings.isAllowSystemContacts =
            appPrefs.getBoolean(PREF_SYSTEM_CONTACTS, false)
        settings.isAllowPlaceSystemCalls =
            appPrefs.getBoolean(PREF_PLACE_CALLS, false)
        settings.setAllowRingOnStartup(appPrefs.getBoolean(PREF_ON_STARTUP, true))
        settings.isAllowPushNotifications =
            appPrefs.getBoolean(PREF_PUSH_NOTIFICATIONS, false)
        settings.isAllowPersistentNotification = appPrefs.getBoolean(
            PREF_PERSISTENT_NOTIFICATION,
            false
        )
        settings.isAllowTypingIndicator =
            appPrefs.getBoolean(PREF_SHOW_TYPING, true)
        settings.isAllowReadIndicator =
            appPrefs.getBoolean(PREF_SHOW_READ, true)
        settings.setBlockRecordIndicator(appPrefs.getBoolean(PREF_BLOCK_RECORD, false))
        settings.notificationVisibility =
            appPrefs.getInt(PREF_NOTIFICATION_VISIBILITY, 0)
        return settings
    }

    private fun saveRequests(accountId: String, requests: Set<String>) {
        val preferences = mContext.getSharedPreferences(PREFS_REQUESTS, Context.MODE_PRIVATE)
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
            val preferences = mContext.getSharedPreferences(PREFS_REQUESTS, Context.MODE_PRIVATE)
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

    override fun hasNetworkConnected(): Boolean {
        return NetworkUtils.isConnectivityAllowed(mContext)
    }

    override fun isPushAllowed(): Boolean {
        val token = JamiApplication.instance!!.pushToken
        return settings.isAllowPushNotifications && !TextUtils.isEmpty(token) /*&& NetworkUtils.isPushAllowed(mContext, getSettings().isAllowMobileData())*/
    }

    override fun getResolution(): Int {
        return videoPreferences.getString(
            PREF_RESOLUTION,
            if (DeviceUtils.isTv(mContext)) mContext.getString(R.string.video_resolution_default_tv)
            else mContext.getString(R.string.video_resolution_default)
        )!!.toInt()
    }

    override fun getBitrate(): Int {
        return videoPreferences.getString(
            PREF_BITRATE,
            mContext.getString(R.string.video_bitrate_default)
        )!!.toInt()
    }

    override fun isHardwareAccelerationEnabled(): Boolean {
        return videoPreferences.getBoolean(PREF_HW_ENCODING, true)
    }

    override fun setDarkMode(enabled: Boolean) {
        val edit = themePreferences.edit()
        edit.putBoolean(PREF_DARK_MODE, enabled)
            .apply()
        applyDarkMode(enabled)
    }

    override fun getDarkMode(): Boolean {
        return themePreferences.getBoolean(PREF_DARK_MODE, false)
    }

    override fun loadDarkMode() {
        applyDarkMode(darkMode)
    }

    override fun getMaxFileAutoAccept(accountId: String): Int {
        return mContext.getSharedPreferences(PREFS_ACCOUNT + accountId, Context.MODE_PRIVATE)
            .getInt(PREF_ACCEPT_IN_MAX_SIZE, 30) * 1024 * 1024
    }

    private fun applyDarkMode(enabled: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM else AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
        )
    }

    private val preferences: SharedPreferences = mContext.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
    private val videoPreferences: SharedPreferences = mContext.getSharedPreferences(PREFS_VIDEO, Context.MODE_PRIVATE)
    private val themePreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)

    companion object {
        const val PREFS_SETTINGS = "ring_settings"
        private const val PREFS_REQUESTS = "ring_requests"
        const val PREFS_THEME = "theme"
        const val PREFS_VIDEO = "videoPrefs"
        const val PREFS_ACCOUNT = "account_"
        private const val PREF_PUSH_NOTIFICATIONS = "push_notifs"
        private const val PREF_PERSISTENT_NOTIFICATION = "persistent_notif"
        private const val PREF_SHOW_TYPING = "persistent_typing"
        private const val PREF_SHOW_READ = "persistent_read"
        private const val PREF_BLOCK_RECORD = "persistent_block_record"
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

        @JvmStatic fun getConversationPreferences(
            context: Context,
            accountId: String,
            conversationUri: Uri
        ): SharedPreferences {
            return context.getSharedPreferences(
                accountId + "_" + conversationUri.uri,
                Context.MODE_PRIVATE
            )
        }
    }
}