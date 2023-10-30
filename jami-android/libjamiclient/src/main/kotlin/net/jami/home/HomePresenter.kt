/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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
package net.jami.home

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.subjects.BehaviorSubject
import net.jami.mvp.RootPresenter
import net.jami.services.ConversationFacade
import net.jami.services.PreferencesService
import net.jami.utils.Log
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

class HomePresenter @Inject constructor(
) : RootPresenter<HomeView>() {
    override fun bindView(view: HomeView) {
        super.bindView(view)
    }

    fun clickQRSearch() {
        view?.goToQRFragment()
    }

    fun clickNewSwarm() {
        view?.startNewSwarm()
    }

    companion object {
        private val TAG = HomePresenter::class.simpleName!!
    }
}