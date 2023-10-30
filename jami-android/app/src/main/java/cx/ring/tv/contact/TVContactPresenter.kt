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
package cx.ring.tv.contact

import cx.ring.utils.ConversationPath
import ezvcard.VCard
import io.reactivex.rxjava3.core.Scheduler
import net.jami.daemon.Blob
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

    fun setContact(path: ConversationPath) {
        mAccountId = path.accountId
        mUri = path.conversationUri
        mCompositeDisposable.clear()

        mCompositeDisposable.add(mConversationService.getAccountSubject(path.accountId)
            .flatMapObservable { account ->
                val conversation = account.getByUri(path.conversationUri)!!
                mConversationService.observeConversation(account, conversation, true).map { vm -> Pair(account, vm) }
            }
            .observeOn(mUiScheduler)
            .subscribe({(account, c) -> view?.showContact(account, c) }, {view?.finishView()}))
    }

    fun contactClicked() {
        mAccountService.getAccount(mAccountId)?.let { account ->
            val conversation = account.getByUri(mUri)!!
            val conf = conversation.currentCall
            val call = conf?.firstCall
            if (call != null && call.callStatus !== Call.CallStatus.INACTIVE && call.callStatus !== Call.CallStatus.FAILURE) {
                view?.goToCallActivity(conf.id)
            } else {
                if (conversation.isSwarm) {
                    view?.callContact(account.accountId, conversation.uri, conversation.contact!!.uri)
                } else {
                    view?.callContact(account.accountId, conversation.uri, conversation.uri)
                }
            }
        }
    }

    fun onAddContact() {
        mAccountId?.let { accountId -> mUri?.let { uri ->
            sendTrustRequest(accountId, uri)
        } }
        view?.switchToConversationView()
    }

    private fun sendTrustRequest(accountId: String, conversationUri: Uri) {
        val conversation = mAccountService.getAccount(accountId)?.getByUri(conversationUri) ?: return
        mVCardService.loadSmallVCardWithDefault(accountId, VCardService.MAX_SIZE_REQUEST)
            .subscribe({ vCard: VCard ->
                mAccountService.sendTrustRequest(conversation, conversationUri, Blob.fromString(vcardToString(vCard))) })
            { mAccountService.sendTrustRequest(conversation, conversationUri) }
    }

    fun acceptTrustRequest() {
        mConversationService.acceptRequest(mAccountId!!, mUri!!)
        view?.switchToConversationView()
    }

    fun refuseTrustRequest() {
        mConversationService.discardRequest(mAccountId!!, mUri!!)
        view?.finishView()
    }

    fun blockTrustRequest() {
        mConversationService.discardRequest(mAccountId!!, mUri!!)
        mConversationService.banConversation(mAccountId!!, mUri!!)
        view?.finishView()
    }
}