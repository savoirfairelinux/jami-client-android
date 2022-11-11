/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
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
package net.jami.settings

import io.reactivex.rxjava3.core.Scheduler
import net.jami.model.Settings
import net.jami.mvp.GenericView
import net.jami.mvp.RootPresenter
import net.jami.services.ConversationFacade
import net.jami.services.PreferencesService
import net.jami.utils.Log
import javax.inject.Inject
import javax.inject.Named

class SettingsPresenter @Inject constructor(
    private val mPreferencesService: PreferencesService,
    @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : RootPresenter<GenericView<Settings>>() {
    override fun bindView(view: GenericView<Settings>) {
        super.bindView(view)
        mCompositeDisposable.add(mPreferencesService.settingsSubject
            .subscribeOn(mUiScheduler)
            .subscribe { settings: Settings -> this.view?.showViewModel(settings) })
    }

    fun loadSettings() {
        mPreferencesService.settings
    }

    fun saveSettings(settings: Settings) {
        mPreferencesService.settings = settings
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