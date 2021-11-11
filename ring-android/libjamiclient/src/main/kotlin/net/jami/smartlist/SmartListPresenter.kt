/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package net.jami.smartlist

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import net.jami.model.Account
import net.jami.model.Uri
import net.jami.mvp.RootPresenter
import net.jami.services.ConversationFacade
import net.jami.utils.Log
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

class SmartListPresenter @Inject constructor(
    private val mConversationFacade: ConversationFacade,
    @param:Named("UiScheduler") private val mUiScheduler: Scheduler
) : RootPresenter<SmartListView>() {
    private var mQueryDisposable: Disposable? = null
    private val mCurrentQuery: Subject<String> = BehaviorSubject.createDefault("")
    private val mQuery: Subject<String> = PublishSubject.create()
    private val mDebouncedQuery = mQuery.debounce(350, TimeUnit.MILLISECONDS)
    private val accountSubject: Observable<Account> = mConversationFacade
        .currentAccountSubject
        .doOnNext { a: Account -> mAccount = a }
    private var mAccount: Account? = null

    override fun bindView(view: SmartListView) {
        super.bindView(view)
        view.setLoading(true)
        mCompositeDisposable.add(mConversationFacade.getFullList(accountSubject, mCurrentQuery, true)
            .switchMap { viewModels ->
                if (viewModels.isEmpty()) ConversationItemViewModel.EMPTY_RESULTS
                else Observable.combineLatest(viewModels) { obs -> obs.mapTo(ArrayList(obs.size), {ob -> ob as ConversationItemViewModel}) }
                    .throttleLatest(150, TimeUnit.MILLISECONDS, mUiScheduler)
            }
            .observeOn(mUiScheduler)
            .subscribe({ viewModels: MutableList<ConversationItemViewModel> ->
                val v = this.view ?: return@subscribe
                v.setLoading(false)
                if (viewModels.isEmpty()) {
                    v.hideList()
                    v.displayNoConversationMessage()
                } else {
                    v.hideNoConversationMessage()
                    v.updateList(viewModels, mCompositeDisposable)
                }
            }) { e: Throwable -> Log.w(TAG, "showConversations error ", e) })

    }

    fun queryTextChanged(query: String) {
        if (query.isEmpty()) {
            mQueryDisposable?.let { disposable ->
                disposable.dispose()
                mQueryDisposable = null
            }
            mCurrentQuery.onNext(query)
        } else {
            if (mQueryDisposable == null) {
                mQueryDisposable = mDebouncedQuery.subscribe { t: String -> mCurrentQuery.onNext(t) }
            }
            mQuery.onNext(query)
        }
    }

    fun conversationClicked(viewModel: ConversationItemViewModel) {
        startConversation(viewModel.accountId, viewModel.uri)
    }

    fun conversationLongClicked(conversationItemViewModel: ConversationItemViewModel) {
        view?.displayConversationDialog(conversationItemViewModel)
    }

    fun fabButtonClicked() {
        view?.displayMenuItem()
    }

    private fun startConversation(accountId: String, conversationUri: Uri?) {
        Log.w(TAG, "startConversation $accountId $conversationUri")
        val view = view
        if (view != null && conversationUri != null) {
            view.goToConversation(accountId, conversationUri)
        }
    }

    fun startConversation(uri: Uri) {
        view?.goToConversation(mAccount!!.accountId, uri)
    }

    fun copyNumber(conversationItemViewModel: ConversationItemViewModel) {
        val contact = conversationItemViewModel.getContact()
        if (contact != null) {
            view?.copyNumber(contact.contact.uri)
        } else {
            view?.copyNumber(conversationItemViewModel.uri)
        }
    }

    fun clearConversation(conversationItemViewModel: ConversationItemViewModel) {
        view?.displayClearDialog(conversationItemViewModel.uri)
    }

    fun clearConversation(uri: Uri?) {
        mCompositeDisposable.add(mConversationFacade
            .clearHistory(mAccount!!.accountId, uri!!)
            .subscribeOn(Schedulers.computation()).subscribe())
    }

    fun removeConversation(conversationItemViewModel: ConversationItemViewModel) {
        view?.displayDeleteDialog(conversationItemViewModel.uri)
    }

    fun removeConversation(uri: Uri) {
        mCompositeDisposable.add(mConversationFacade.removeConversation(mAccount!!.accountId, uri)
            .subscribe())
    }

    fun banContact(conversationItemViewModel: ConversationItemViewModel) {
        mConversationFacade.banConversation(conversationItemViewModel.accountId, conversationItemViewModel.uri)
    }

    fun clickQRSearch() {
        view?.goToQRFragment()
    }

    private fun showConversations(conversations: Observable<List<Observable<ConversationItemViewModel>>>) {
    }

    companion object {
        private val TAG = SmartListPresenter::class.simpleName!!
    }
}