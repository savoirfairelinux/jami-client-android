/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.jami.services

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.Subject
import net.jami.model.DonationSettings
import net.jami.model.Settings
import net.jami.model.Uri

abstract class PreferencesService(
    private val mAccountService: AccountService,
    private val mDeviceService: DeviceRuntimeService
) {
    protected abstract fun loadSettings(): Settings
    protected abstract fun saveSettings(settings: Settings)

    private val mSettingsSubject: Subject<Settings> = BehaviorSubject.create()
    private var userSettings: Settings? = null
    var settings: Settings
        get() = userSettings ?: loadSettings().also { settings ->
            userSettings = settings
            mSettingsSubject.onNext(settings)
        }
        set(settings) {
            saveSettings(settings)
            val allowPush = settings.enablePushNotifications
            val previousSettings = userSettings
            if (previousSettings == null || previousSettings.enablePushNotifications != allowPush) {
                mAccountService.setPushNotificationToken(if (allowPush) mDeviceService.pushToken else "")
                mAccountService.setProxyEnabled(allowPush)
            }
            userSettings = settings
            mSettingsSubject.onNext(settings)
        }
    val settingsSubject: Observable<Settings>
        get() = mSettingsSubject

    abstract fun donationSettings(): Observable<DonationSettings>

    abstract fun setDonationSettings(settings: DonationSettings)

    /**
     * Get the preferences for a given account
     *
     * @param accountId the account id
     * @param conversationUri the conversation uri
     * @return the preferences for the given account
     */
    abstract fun getConversationPreferences(
        accountId: String,
        conversationUri: Uri,
    ): Map<String, String>

    /**
     * Set the preferences for a given account
     *
     * @param accountId the account id
     * @param conversationUri the conversation uri
     * @param preferences the preferences to set
     */
    abstract fun setConversationPreferences(
        accountId: String,
        conversationUri: Uri,
        preferences: Map<String, String>,
    )

    abstract fun hasNetworkConnected(): Boolean
    abstract val isPushAllowed: Boolean
    abstract fun saveRequestPreferences(accountId: String, contactId: String)
    abstract fun loadRequestsPreferences(accountId: String): Set<String>
    abstract fun removeRequestPreferences(accountId: String, contactId: String)
    abstract val resolution: Int
    abstract val bitrate: Int
    abstract val isHardwareAccelerationEnabled: Boolean
    abstract var darkMode: Boolean
    abstract var isLogActive: Boolean
    abstract fun loadDarkMode()
    abstract fun getMaxFileAutoAccept(accountId: String): Int
}