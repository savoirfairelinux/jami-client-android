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
package cx.ring.tv.contact

import cx.ring.utils.ConversationPath
import ezvcard.VCard
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import net.jami.daemon.Blob
import net.jami.model.Account
import net.jami.services.ConversationFacade
import net.jami.model.Call
import net.jami.model.Conversation
import net.jami.model.Uri
import net.jami.mvp.RootPresenter
import net.jami.services.AccountService
import net.jami.services.VCardService
import net.jami.smartlist.ConversationItemViewModel
import net.jami.utils.VCardUtils.vcardToString
import javax.inject.Inject
import javax.inject.Named

class TVContactPresenter @Inject constructor(
    private val mAccountService: AccountService,
    private val mConversationService: ConversationFacade,
    @param:Named("UiScheduler") private val mUiScheduler: Scheduler,
    private val mVCardService: VCardService
) : RootPresenter<TVContactView>() {
    private var mAccountId: String? = null
    private var mUri: Uri? = null
    private var mAccount: Account? = null
    private var mConversation: Conversation? = null

    fun setContact(path: ConversationPath) {
        mAccountId = path.accountId
        mUri = path.conversationUri
        mCompositeDisposable.clear()

        mCompositeDisposable.add(
            mConversationService.getAccountSubject(path.accountId)
                .flatMapObservable { account: Account ->
                    val conversation = account.getByUri(path.conversationUri)!!
                    mAccount = account
                    mConversation = conversation
                    conversation.mode
                        .flatMap { conversationMode: Conversation.Mode ->
                            if (conversationMode === Conversation.Mode.Legacy)
                                conversation.contact!!.conversationUri.switchMapSingle { uri ->
                                    mConversationService.startConversation(account.accountId, uri)
                                }
                            else Observable.just(conversation)
                        }.map { Pair(account, it) }
                }.flatMap { (account: Account, conversation: Conversation) ->
                    mConversationService.observeConversation(account, conversation, true)
                        .map { vm -> Pair(account, vm) }
                }.observeOn(mUiScheduler)
                .subscribe({ (account: Account, c: ConversationItemViewModel) ->
                    view?.showContact(account, c)
                }, { view?.finishView() })
        )
    }

    fun contactClicked() {
        val conversation = mConversation ?: mAccountService.getAccount(mAccountId)?.getByUri(mUri) ?: return
        val conf = conversation.currentCall
        val call = conf?.firstCall
        if (call != null && call.callStatus !== Call.CallStatus.INACTIVE && call.callStatus !== Call.CallStatus.FAILURE) {
            view?.goToCallActivity(conf.id)
        } else {
            if (conversation.isSwarm) {
                view?.callContact(conversation.accountId, conversation.uri, conversation.contact?.uri ?: conversation.uri)
            } else {
                view?.callContact(conversation.accountId, conversation.uri, conversation.uri)
            }
        }
    }

    fun onAddContact() {
        mConversation?.let { sendTrustRequest(it) }
        view?.switchToConversationView()
    }

    private fun sendTrustRequest(conversation: Conversation) {
        mVCardService.loadSmallVCardWithDefault(conversation.accountId, VCardService.MAX_SIZE_REQUEST)
            .subscribe({ vCard: VCard ->
                mAccountService.sendTrustRequest(conversation, conversation.uri, Blob.fromString(vcardToString(vCard))) })
            { mAccountService.sendTrustRequest(conversation, conversation.uri) }
    }

    fun acceptTrustRequest() {
        val conversation = mConversation ?: mAccountService.getAccount(mAccountId)?.getByUri(mUri) ?: return
        mConversationService.acceptRequest(conversation)
        view?.switchToConversationView()
    }

    fun refuseTrustRequest() {
        mConversationService.discardRequest(mAccountId!!, mUri!!)
        view?.finishView()
    }

    fun blockTrustRequest() {
        mConversationService.blockConversation(mAccountId!!, mUri!!)
        mConversationService.discardRequest(mAccountId!!, mUri!!)
        view?.finishView()
    }
}