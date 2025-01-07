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
package cx.ring.tv.main

import android.util.Log
import io.reactivex.rxjava3.core.Scheduler
import net.jami.mvp.RootPresenter
import net.jami.navigation.HomeNavigationViewModel
import net.jami.services.AccountService
import net.jami.services.ContactService
import net.jami.services.ConversationFacade
import javax.inject.Inject
import javax.inject.Named

class MainPresenter @Inject constructor(
    private val accountService: AccountService,
    val conversationFacade: ConversationFacade,
    val contactService: ContactService,
    @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : RootPresenter<MainView>() {
    override fun bindView(view: MainView) {
        super.bindView(view)
        loadConversations()
        reloadAccountInfo()
    }

    private fun loadConversations() {
        view?.showLoading(true)
        mCompositeDisposable.add(conversationFacade.getConversationSmartlist()
            .observeOn(mUiScheduler)
            .subscribe({ viewModels ->
                val view = view ?: return@subscribe
                view.showLoading(false)
                view.showContacts(viewModels)
            }) { e -> Log.w(TAG, "showConversations error ", e) })
        mCompositeDisposable.add(conversationFacade.getPendingConversationList(conversationFacade.currentAccountSubject)
            .observeOn(mUiScheduler)
            .subscribe({ viewModels -> view?.showContactRequests(viewModels) })
            { e -> Log.w(TAG, "showConversations error ", e) })
    }

    private fun reloadAccountInfo() {
        mCompositeDisposable.add(accountService.currentProfileAccountSubject
            .observeOn(mUiScheduler)
            .subscribe({ accountProfile -> view?.displayAccountInfo(HomeNavigationViewModel(accountProfile.first, accountProfile.second)) })
            { e -> Log.d(TAG, "reloadAccountInfos getProfileAccountList onError", e) })
    }

    fun onExportClicked() {
        val account = accountService.currentAccount ?: return
        view?.showExportDialog(account.accountId, account.hasPassword())
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