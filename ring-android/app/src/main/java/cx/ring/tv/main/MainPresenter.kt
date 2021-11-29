/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
package cx.ring.tv.main

import android.util.Log
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import net.jami.mvp.RootPresenter
import net.jami.navigation.HomeNavigationViewModel
import net.jami.services.AccountService
import net.jami.services.ConversationFacade
import net.jami.smartlist.ConversationItemViewModel
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

class MainPresenter @Inject constructor(
    private val mAccountService: AccountService,
    private val mConversationFacade: ConversationFacade,
    @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : RootPresenter<MainView>() {
    override fun bindView(view: MainView) {
        super.bindView(view)
        loadConversations()
        reloadAccountInfo()
    }

    private fun loadConversations() {
        view?.showLoading(true)
        mCompositeDisposable.add(mConversationFacade.getSmartList(true)
            .switchMap { viewModels: List<Observable<ConversationItemViewModel>> ->
                if (viewModels.isEmpty()) ConversationItemViewModel.EMPTY_RESULTS
                else Observable.combineLatest<ConversationItemViewModel, List<ConversationItemViewModel>>(viewModels)
                { obs: Array<Any> -> obs.mapTo(ArrayList(obs.size)) { ob -> ob as ConversationItemViewModel } }
            }
            .throttleLatest(150, TimeUnit.MILLISECONDS, mUiScheduler)
            .observeOn(mUiScheduler)
            .subscribe({ viewModels: List<ConversationItemViewModel> ->
                val view = view ?: return@subscribe
                view.showLoading(false)
                view.showContacts(viewModels)
            }) { e: Throwable -> Log.w(TAG, "showConversations error ", e) })
        mCompositeDisposable.add(mConversationFacade.pendingList
            .switchMap { viewModels: List<Observable<ConversationItemViewModel>> ->
                if (viewModels.isEmpty()) ConversationItemViewModel.EMPTY_RESULTS
                else Observable.combineLatest<ConversationItemViewModel, List<ConversationItemViewModel>>(viewModels)
                { obs: Array<Any> -> obs.mapTo(ArrayList(obs.size)) { ob -> ob as ConversationItemViewModel } }
            }
            .throttleLatest(150, TimeUnit.MILLISECONDS, mUiScheduler)
            .observeOn(mUiScheduler)
            .subscribe({ viewModels: List<ConversationItemViewModel> ->
                view?.showContactRequests(viewModels)
            }) { e: Throwable -> Log.w(TAG, "showConversations error ", e) })
    }

    private fun reloadAccountInfo() {
        mCompositeDisposable.add(mAccountService.currentProfileAccountSubject
            .observeOn(mUiScheduler)
            .subscribe({ accountProfile -> view?.displayAccountInfo(HomeNavigationViewModel(accountProfile.first, accountProfile.second)) })
            { e: Throwable -> Log.d(TAG, "reloadAccountInfos getProfileAccountList onError", e) })
    }

    fun onExportClicked() {
        view?.showExportDialog(mAccountService.currentAccount!!.accountId, mAccountService.currentAccount!!.hasPassword())
    }

    fun onEditProfileClicked() {
        view?.showProfileEditing()
    }

    fun onShareAccountClicked() {
        view?.showAccountShare()
    }

    fun onSettingsClicked() {
        view?.showSettings()
    }

    companion object {
        private val TAG = MainPresenter::class.simpleName!!
    }
}