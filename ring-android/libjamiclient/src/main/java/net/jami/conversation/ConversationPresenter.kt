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
package net.jami.conversation

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.Subject
import net.jami.daemon.Blob
import net.jami.services.ConversationFacade
import net.jami.model.*
import net.jami.model.Account.ComposingStatus
import net.jami.model.Conversation.ElementStatus
import net.jami.mvp.RootPresenter
import net.jami.services.*
import net.jami.utils.Log
import net.jami.utils.StringUtils
import net.jami.utils.VCardUtils
import java.io.File
import javax.inject.Inject
import javax.inject.Named

class ConversationPresenter @Inject constructor(
    private val mContactService: ContactService,
    private val mAccountService: AccountService,
    private val mHardwareService: HardwareService,
    private val mConversationFacade: ConversationFacade,
    private val mVCardService: VCardService,
    val deviceRuntimeService: DeviceRuntimeService,
    private val mPreferencesService: PreferencesService,
    @param:Named("UiScheduler") private var mUiScheduler: Scheduler
) : RootPresenter<ConversationView>() {
    private var mConversation: Conversation? = null
    private var mConversationUri: Uri? = null
    private var mConversationDisposable: CompositeDisposable? = null
    private val mVisibilityDisposable = CompositeDisposable()
    private val mConversationSubject: Subject<Conversation> = BehaviorSubject.create()

    fun init(conversationUri: Uri, accountId: String) {
        Log.w(TAG, "init $conversationUri $accountId")
        if (conversationUri == mConversationUri) return
        mConversationUri = conversationUri
        mCompositeDisposable.add(mConversationFacade.getAccountSubject(accountId)
            .flatMap { a: Account ->
                mConversationFacade.loadConversationHistory(a, conversationUri)
                    .observeOn(mUiScheduler)
                    .doOnSuccess { c: Conversation -> setConversation(a, c) }
            }
            .observeOn(mUiScheduler)
            .subscribe({}) { e: Throwable ->
                Log.e(TAG, "Error loading conversation", e)
                view?.goToHome()
            })
        view?.setReadIndicatorStatus(showReadIndicator())
    }

    override fun unbindView() {
        super.unbindView()
        mConversation = null
        mConversationUri = null
        mConversationDisposable?.let { conversationDisposable ->
            conversationDisposable.dispose()
            mConversationDisposable = null
        }
    }

    private fun setConversation(account: Account, conversation: Conversation) {
        Log.w(TAG, "setConversation " + conversation.aggregateHistory.size)
        if (mConversation == conversation) return
        mConversation = conversation
        mConversationSubject.onNext(conversation)
        val view = view
        view?.let { initView(account, conversation, it) }
    }

    fun pause() {
        mVisibilityDisposable.clear()
        mConversation?.isVisible = false
    }

    fun resume(isBubble: Boolean) {
        Log.w(TAG, "resume $mConversationUri")
        mVisibilityDisposable.clear()
        mVisibilityDisposable.add(mConversationSubject
            .subscribe({ conversation: Conversation ->
                conversation.isVisible = true
                updateOngoingCallView(conversation)
                mAccountService.getAccount(conversation.accountId)?.let { account ->
                    mConversationFacade.readMessages(account, conversation, !isBubble)}
            }) { e -> Log.e(TAG, "Error loading conversation", e) })
    }

    private fun initContact(account: Account, conversation: Conversation, mode: Conversation.Mode, view: ConversationView) {
        if (account.isJami) {
            Log.w(TAG, "initContact " + conversation.uri)
            if (mode === Conversation.Mode.Syncing) {
                view.switchToSyncingView()
            } else if (conversation.isSwarm || account.isContact(conversation)) {
                //if (conversation.isEnded())
                //    conversation.s
                view.switchToConversationView()
            } else {
                val uri = conversation.uri
                val req = account.getRequest(uri)
                if (req == null) {
                    view.switchToUnknownView(uri.rawUriString)
                } else {
                    view.switchToIncomingTrustRequestView(req.displayname)
                }
            }
        } else {
            view.switchToConversationView()
        }
        view.displayContact(conversation)
    }

    private fun initView(account: Account, c: Conversation, view: ConversationView) {
        Log.w(TAG, "initView " + c.uri + " " + c.mode)
        val disposable = mConversationDisposable?.apply { clear() } ?: CompositeDisposable().apply {
            mConversationDisposable = this
            mCompositeDisposable.add(this)
        }

        view.hideNumberSpinner()
        disposable.add(c.mode
            .switchMapSingle { mode: Conversation.Mode ->
                mContactService.getLoadedContact(c.accountId, c.contacts, true)
                    .observeOn(mUiScheduler)
                    .doOnSuccess { initContact(account, c, mode, view) }
            }
            .subscribe())
        disposable.add(c.mode
            .switchMap { mode: Conversation.Mode -> if (mode === Conversation.Mode.Legacy || mode === Conversation.Mode.OneToOne) c.contact!!.conversationUri else Observable.empty() }
            .observeOn(mUiScheduler)
            .subscribe { uri: Uri -> init(uri, account.accountID) })
        disposable.add(Observable.combineLatest(mHardwareService.connectivityState, mAccountService.getObservableAccount(account),
            { isConnected: Boolean, a: Account -> isConnected || a.isRegistered })
            .observeOn(mUiScheduler)
            .subscribe { isOk: Boolean ->
                this.view?.let { v ->
                    if (!isOk) v.displayNetworkErrorPanel() else if (!account.isEnabled) {
                        v.displayAccountOfflineErrorPanel()
                    } else {
                        v.hideErrorPanel()
                    }
                }
            })
        disposable.add(c.sortedHistory
            .observeOn(mUiScheduler)
            .subscribe({ conversation: List<Interaction> -> this.view?.refreshView(conversation) }) { e: Throwable ->
                Log.e(TAG, "Can't update element", e)
            })
        disposable.add(c.cleared
            .observeOn(mUiScheduler)
            .subscribe({ conversation: List<Interaction> -> this.view?.refreshView(conversation) }) { e: Throwable ->
                Log.e(TAG, "Can't update elements", e)
            })
        disposable.add(c.contactUpdates
            .switchMap { contacts: List<Contact> ->
                Observable.merge(mContactService.observeLoadedContact(c.accountId, contacts, true))
            }
            .observeOn(mUiScheduler)
            .subscribe { contact: Contact -> this.view?.updateContact(contact) })
        disposable.add(c.updatedElements
            .observeOn(mUiScheduler)
            .subscribe({ elementTuple ->
                val v = this.view ?: return@subscribe
                when (elementTuple.second) {
                    ElementStatus.ADD -> v.addElement(elementTuple.first)
                    ElementStatus.UPDATE -> v.updateElement(elementTuple.first)
                    ElementStatus.REMOVE -> v.removeElement(elementTuple.first)
                }
            }, { e: Throwable -> Log.e(TAG, "Can't update element", e) })
        )
        if (showTypingIndicator()) {
            disposable.add(c.composingStatus
                .observeOn(mUiScheduler)
                .subscribe { composingStatus: ComposingStatus -> this.view?.setComposingStatus(composingStatus) })
        }
        disposable.add(c.getLastDisplayed()
            .observeOn(mUiScheduler)
            .subscribe { interaction: Interaction -> this.view?.setLastDisplayed(interaction) })
        disposable.add(c.calls
            .observeOn(mUiScheduler)
            .subscribe({ updateOngoingCallView(c) }) { e: Throwable ->
                Log.e(TAG, "Can't update call view", e)
            })
        disposable.add(c.getColor()
            .observeOn(mUiScheduler)
            .subscribe({ integer: Int -> view.setConversationColor(integer) }) { e: Throwable ->
                Log.e(TAG, "Can't update conversation color", e)
            })
        disposable.add(c.getSymbol()
            .observeOn(mUiScheduler)
            .subscribe({ symbol: CharSequence -> view.setConversationSymbol(symbol) }) { e: Throwable ->
                Log.e(TAG, "Can't update conversation color", e)
            })
        disposable.add(account
            .getLocationUpdates(c.uri)
            .observeOn(mUiScheduler)
            .subscribe {
                Log.e(TAG, "getLocationUpdates: update")
                view?.showMap(c.accountId, c.uri.uri, false)
            }
        )
    }

    fun loadMore() {
        mConversationDisposable?.add(mAccountService.loadMore(mConversation!!).subscribe({}) {})
    }

    fun openContact() {
        mConversation?.let { conversation -> view?.goToContactActivity(conversation.accountId, conversation.uri) }
    }

    fun sendTextMessage(message: String?) {
        val conversation = mConversation
        if (message == null || message.isEmpty() || conversation == null) {
            return
        }
        val conference = conversation.currentCall
        if (conversation.isSwarm || conference == null || !conference.isOnGoing) {
            mConversationFacade.sendTextMessage(conversation, conversation.uri, message).subscribe()
        } else {
            mConversationFacade.sendTextMessage(conversation, conference, message)
        }
    }

    fun selectFile() {
        view!!.openFilePicker()
    }

    fun sendFile(file: File) {
        mCompositeDisposable.add(mConversationSubject.firstElement().subscribe({ conversation ->
            mConversationFacade.sendFile(conversation, conversation.uri, file).subscribe()
        }) {e -> Log.e(TAG, "Can't send file", e)})
    }

    /**
     * Gets the absolute path of the file dataTransfer and sends both the DataTransfer and the
     * found path to the ConversationView in order to start saving the file
     *
     * @param interaction an interaction representing a datat transfer
     */
    fun saveFile(interaction: Interaction) {
        val transfer = interaction as DataTransfer
        val fileAbsolutePath = deviceRuntimeService.getConversationPath(transfer).absolutePath
        view?.startSaveFile(transfer, fileAbsolutePath)
    }

    fun shareFile(interaction: Interaction) {
        val file = interaction as DataTransfer
        val path = deviceRuntimeService.getConversationPath(file)
        view?.shareFile(path, file.displayName)
    }

    fun openFile(interaction: Interaction) {
        val file = interaction as DataTransfer
        val path = deviceRuntimeService.getConversationPath(file)
        view?.openFile(path, file.displayName)
    }

    fun acceptFile(transfer: DataTransfer) {
        view?.acceptFile(mConversation!!.accountId, mConversationUri!!, transfer)
    }

    fun refuseFile(transfer: DataTransfer) {
        view!!.refuseFile(mConversation!!.accountId, mConversationUri!!, transfer)
    }

    fun deleteConversationItem(element: Interaction) {
        mConversationFacade.deleteConversationItem(mConversation!!, element)
    }

    fun cancelMessage(message: Interaction) {
        mConversationFacade.cancelMessage(message)
    }

    private fun sendTrustRequest() {
        val conversation = mConversation ?: return
        val contact = conversation.contact ?: return
        contact.status = Contact.Status.REQUEST_SENT
        mVCardService.loadSmallVCardWithDefault(conversation.accountId, VCardService.MAX_SIZE_REQUEST)
            .subscribeOn(Schedulers.computation())
            .subscribe({ vCard -> mAccountService.sendTrustRequest(conversation, contact.uri, Blob.fromString(VCardUtils.vcardToString(vCard)))})
            { mAccountService.sendTrustRequest(conversation, contact.uri, null) }
    }

    fun clickOnGoingPane() {
        val conf = mConversation?.currentCall
        if (conf != null) {
            view?.goToCallActivity(conf.id)
        } else {
            view?.displayOnGoingCallPane(false)
        }
    }

    fun goToCall(audioOnly: Boolean) {
        if (audioOnly && !mHardwareService.hasMicrophone()) {
            view!!.displayErrorToast(Error.NO_MICROPHONE)
            return
        }
        mCompositeDisposable.add(mConversationSubject
            .firstElement()
            .subscribe { conversation: Conversation ->
                val view = view
                if (view != null) {
                    val conf = conversation.currentCall
                    if (conf != null && conf.participants.isNotEmpty()
                        && conf.participants[0].callStatus !== Call.CallStatus.INACTIVE
                        && conf.participants[0].callStatus !== Call.CallStatus.FAILURE) {
                        view.goToCallActivity(conf.id)
                    } else {
                        view.goToCallActivityWithResult(conversation.accountId, conversation.uri, conversation.contact!!.uri, audioOnly)
                    }
                }
            })
    }

    private fun updateOngoingCallView(conversation: Conversation?) {
        val conf = conversation?.currentCall
        view?.displayOnGoingCallPane(conf != null && (conf.state === Call.CallStatus.CURRENT || conf.state === Call.CallStatus.HOLD || conf.state === Call.CallStatus.RINGING))
    }

    fun onBlockIncomingContactRequest() {
        mConversation?.let { conversation ->
            mConversationFacade.discardRequest(conversation.accountId, conversation.uri)
            mAccountService.removeContact(conversation.accountId, conversation.uri.host, true)
        }
        view?.goToHome()
    }

    fun onRefuseIncomingContactRequest() {
        mConversation?.let { conversation ->
            mConversationFacade.discardRequest(conversation.accountId, conversation.uri)
        }
        view?.goToHome()
    }

    fun onAcceptIncomingContactRequest() {
        mConversation?.let { conversation ->
            mConversationFacade.acceptRequest(conversation.accountId, conversation.uri)
        }
        view?.switchToConversationView()
    }

    fun onAddContact() {
        sendTrustRequest()
        view?.switchToConversationView()
    }

    fun noSpaceLeft() {
        Log.e(TAG, "configureForFileInfoTextMessage: no space left on device")
        view?.displayErrorToast(Error.NO_SPACE_LEFT)
    }

    fun setConversationColor(color: Int) {
        mCompositeDisposable.add(mConversationSubject
            .firstElement()
            .subscribe { conversation: Conversation -> conversation.setColor(color) })
    }

    fun setConversationSymbol(symbol: CharSequence) {
        mCompositeDisposable.add(mConversationSubject.firstElement()
            .subscribe { conversation -> conversation.setSymbol(symbol) })
    }

    fun cameraPermissionChanged(isGranted: Boolean) {
        if (isGranted && mHardwareService.isVideoAvailable) {
            mHardwareService.initVideo()
                .onErrorComplete()
                .subscribe()
        }
    }

    fun shareLocation() {
        mCompositeDisposable.add(mConversationSubject.firstElement()
            .subscribe { conversation -> view?.startShareLocation(conversation.accountId, conversation.uri.uri) })
    }

    fun showPluginListHandlers() {
        view?.showPluginListHandlers(mConversation!!.accountId, mConversationUri!!.uri)
    }

    val path: Pair<String, String>
        get() = Pair(mConversation!!.accountId, mConversationUri!!.uri)

    fun onComposingChanged(hasMessage: Boolean) {
        if (showTypingIndicator()) {
            mConversation?.let { conversation ->
                mConversationFacade.setIsComposing(conversation.accountId, conversation.uri, hasMessage)
            }
        }
    }

    private fun showTypingIndicator(): Boolean {
        return mPreferencesService.settings.isAllowTypingIndicator
    }

    private fun showReadIndicator(): Boolean {
        return mPreferencesService.settings.isAllowReadIndicator
    }

    companion object {
        private val TAG = ConversationPresenter::class.simpleName!!
    }

    init {
        mCompositeDisposable.add(mVisibilityDisposable)
    }
}