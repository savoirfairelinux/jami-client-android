/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
package net.jami.home

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import net.jami.model.Account
import net.jami.model.Conversation
import net.jami.model.Uri
import net.jami.mvp.RootPresenter
import net.jami.services.ConversationFacade
import net.jami.services.PreferencesService
import net.jami.utils.Log
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

class HomePresenter @Inject constructor(
    private val conversationFacade: ConversationFacade,
    private val preferencesService: PreferencesService,
    @param:Named("UiScheduler") private val uiScheduler: Scheduler
) : RootPresenter<HomeView>() {
    private val querySubject = BehaviorSubject.createDefault("")
    private val debouncedQury = querySubject.debounce{ item ->
        if (item.isEmpty()) Observable.empty() else Observable.timer(350, TimeUnit.MILLISECONDS)
    }.distinctUntilChanged()


    override fun bindView(view: HomeView) {
        super.bindView(view)
    }

    fun queryTextChanged(query: String) {
        Log.w(TAG, "queryTextChanged $query")
        querySubject.onNext(query)
    }

    fun clickQRSearch() {
        view?.goToQRFragment()
    }

    fun clickNewGroup() {
        view?.startNewGroup()
    }

    fun isAddGroupEnabled(): Boolean {
        return preferencesService.settings.enableAddGroup
    }

    companion object {
        private val TAG = HomePresenter::class.simpleName!!
    }
}