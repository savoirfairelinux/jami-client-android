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
package net.jami.settings

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import net.jami.model.DonationSettings
import net.jami.model.Settings
import net.jami.mvp.GenericView
import net.jami.mvp.RootPresenter
import net.jami.services.PreferencesService
import javax.inject.Inject
import javax.inject.Named

data class SettingsViewModel(
    val settings: Settings,
    val donationSettings: DonationSettings
)

class SettingsPresenter @Inject constructor(
    private val mPreferencesService: PreferencesService,
    @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : RootPresenter<GenericView<SettingsViewModel>>() {

    override fun bindView(view: GenericView<SettingsViewModel>) {
        super.bindView(view)
        mCompositeDisposable.add(
            Observable.combineLatest(
                mPreferencesService.settingsSubject,
                mPreferencesService.donationSettings()
            ) { settings, donationSettings ->
                SettingsViewModel(settings, donationSettings)
            }
                .subscribeOn(mUiScheduler)
                .subscribe { vm -> this.view?.showViewModel(vm) })
    }

    fun loadSettings() {
        mPreferencesService.settings
    }

    fun saveSettings(settings: Settings) {
        mPreferencesService.settings = settings
    }

    fun saveDonationSettings(donationSettings: DonationSettings) {
        mPreferencesService.setDonationSettings(donationSettings)
    }

    var darkMode: Boolean
        get() = mPreferencesService.darkMode
        set(isChecked) {
            mPreferencesService.darkMode = isChecked
        }

    companion object {
        private val TAG = SettingsPresenter::class.simpleName!!
    }
}