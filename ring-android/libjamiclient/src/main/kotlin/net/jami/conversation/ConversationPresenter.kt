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
import net.jami.smartlist.ConversationItemViewModel
import net.jami.utils.Log
import net.jami.utils.VCardUtils
import java.io.File
import javax.inject.Inject
import javax.inject.Named

class ConversationPresenter @Inject constructor(
    private val contactService: ContactService,
    private val accountService: AccountService,
    private val hardwareService: HardwareService,
    val conversationFacade: ConversationFacade,
    private val vCardService: VCardService,
    val deviceRuntimeService: DeviceRuntimeService,
    private val preferencesService: PreferencesService,
    @param:Named("UiScheduler") private val uiScheduler: Scheduler
) : RootPresenter<ConversationView>() {
    private var mConversation: Conversation? = null
    private var mConversationUri: Uri? = null
    private var mConversationDisposable: CompositeDisposable? = null
    private val mVisibilityDisposable = CompositeDisposable().apply {
        mCompositeDisposable.add(this)
    }
    private val mConversationSubject: Subject<Conversation> = BehaviorSubject.create()

    fun init(conversationUri: Uri, accountId: String) {
        if (conversationUri == mConversationUri) return
        Log.w(TAG, "init $conversationUri $accountId")
        val settings = preferencesService.settings
        view?.setSettings(settings.enableReadIndicator, settings.enableLinkPreviews)
        mConversationUri = conversationUri
        mCompositeDisposable.add(conversationFacade.getAccountSubject(accountId)
            .flatMap { a: Account ->
                conversationFacade.loadConversationHistory(a, conversationUri)
                    .observeOn(uiScheduler)
                    .doOnSuccess { c: Conversation -> setConversation(a, c) }
            }
            .observeOn(uiScheduler)
            .subscribe({}) { e: Throwable ->
                Log.e(TAG, "Error loading conversation", e)
                view?.goToHome()
            })
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
                accountService.getAccount(conversation.accountId)?.let { account ->
                    conversationFacade.readMessages(account, conversation, !isBubble)}
            }) { e -> Log.e(TAG, "Error loading conversation", e) })
    }

    private fun initContact(account: Account,
                            c: ConversationItemViewModel,
                            view: ConversationView
    ) {
        if (account.isJami) {
            Log.w(TAG, "initContact " + c.uri + " mode: " + c.mode)
            if (c.mode === Conversation.Mode.Syncing) {
                view.switchToSyncingView()
            } else if (c.mode == Conversation.Mode.Request) {
                view.switchToIncomingTrustRequestView(c.uriTitle/*ConversationItemViewModel.getUriTitle(conversation.uri, contacts)*/)
            } else if (c.isSwarm || account.isContact(c.uri)) {
                view.switchToConversationView(c.mode == Conversation.Mode.Legacy)
            } else {
                val req = account.getRequest(c.uri)
                if (req == null) {
                    view.switchToUnknownView(c.uriTitle)
                } else {
                    view.switchToIncomingTrustRequestView(req.displayName)
                }
            }
        } else {
            view.switchToConversationView()
        }
        view.displayContact(c)
    }

    private fun initView(account: Account, c: Conversation, view: ConversationView) {
        Log.w(TAG, "initView " + c.uri)
        val disposable = mConversationDisposable?.apply { clear() } ?: CompositeDisposable().apply {
            mConversationDisposable = this
            mCompositeDisposable.add(this)
        }

        view.hideNumberSpinner()
        disposable.add(c.mode
            .switchMap { mode: Conversation.Mode ->
                conversationFacade.observeConversation(account, c, true)
                    .observeOn(uiScheduler)
                    .doOnNext { convViewModel -> initContact(account, convViewModel, this.view!!) }
            }
            .subscribe())
        disposable.add(c.mode
            .switchMap { mode: Conversation.Mode ->
                if (mode === Conversation.Mode.Legacy) c.contact!!.conversationUri else Observable.empty() }
            .observeOn(uiScheduler)
            .subscribe { uri: Uri -> init(uri, account.accountId) })
        disposable.add(Observable.combineLatest(hardwareService.connectivityState, accountService.getObservableAccount(account))
            { isConnected: Boolean, a: Account -> isConnected || a.isRegistered }
            .observeOn(uiScheduler)
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
            .observeOn(uiScheduler)
            .subscribe({ conversation: List<Interaction> -> this.view?.refreshView(conversation) }) { e: Throwable ->
                Log.e(TAG, "Can't update element", e)
            })
        disposable.add(c.cleared
            .observeOn(uiScheduler)
            .subscribe({ conversation: List<Interaction> -> this.view?.refreshView(conversation) }) { e: Throwable ->
                Log.e(TAG, "Can't update elements", e)
            })
        disposable.add(c.contactUpdates
            .switchMap { contacts -> Observable.merge(contactService.observeLoadedContact(c.accountId, contacts, true)) }
            .observeOn(uiScheduler)
            .subscribe { contact: ContactViewModel -> this.view?.updateContact(contact) })
        disposable.add(c.updatedElements
            .observeOn(uiScheduler)
            .subscribe({ elementTuple ->
                val v = this.view ?: return@subscribe
                when (elementTuple.second) {
                    ElementStatus.ADD -> v.addElement(elementTuple.first)
                    ElementStatus.UPDATE -> v.updateElement(elementTuple.first)
                    ElementStatus.REMOVE -> v.removeElement(elementTuple.first)
                }
            }, { e: Throwable -> Log.e(TAG, "Can't update element", e) }))
        if (showTypingIndicator()) {
            disposable.add(c.composingStatus
                .observeOn(uiScheduler)
                .subscribe { composingStatus: ComposingStatus -> this.view?.setComposingStatus(composingStatus) })
        }
        disposable.add(c.calls
            .observeOn(uiScheduler)
            .subscribe({ updateOngoingCallView(c) }) { e: Throwable ->
                Log.e(TAG, "Can't update call view", e)
            })
        disposable.add(c.getColor()
            .observeOn(uiScheduler)
            .subscribe({ integer: Int -> this.view?.setConversationColor(integer) }) { e: Throwable ->
                Log.e(TAG, "Can't update conversation color", e)
            })
        disposable.add(c.getSymbol()
            .observeOn(uiScheduler)
            .subscribe({ symbol: CharSequence -> this.view?.setConversationSymbol(symbol) }) { e: Throwable ->
                Log.e(TAG, "Can't update conversation color", e)
            })
        disposable.add(account
            .getLocationUpdates(c.uri)
            .observeOn(uiScheduler)
            .subscribe {
                Log.e(TAG, "getLocationUpdates: update")
                this.view?.showMap(c.accountId, c.uri.uri, false)
            })
    }

    fun loadMore() {
        mConversationDisposable?.add(accountService.loadMore(mConversation!!).subscribe({}) {})
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
            conversationFacade.sendTextMessage(conversation, conversation.uri, message).subscribe()
        } else {
            conversationFacade.sendTextMessage(conversation, conference, message)
        }
    }

    fun selectFile() {
        view?.openFilePicker()
    }

    fun sendFile(file: File) {
        mCompositeDisposable.add(mConversationSubject.firstElement().subscribe({ conversation ->
            conversationFacade.sendFile(conversation, conversation.uri, file).subscribe()
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
        conversationFacade.deleteConversationItem(mConversation!!, element)
    }

    fun cancelMessage(message: Interaction) {
        conversationFacade.cancelMessage(message)
    }

    private fun sendTrustRequest() {
        val conversation = mConversation ?: return
        val contact = conversation.contact ?: return
        if (conversation.mode.blockingFirst() == Conversation.Mode.Legacy) {
            accountService.addContact(conversation.accountId, contact.uri.rawRingId)
        } else {
            contact.status = Contact.Status.REQUEST_SENT
            vCardService.loadSmallVCardWithDefault(conversation.accountId, VCardService.MAX_SIZE_REQUEST)
                .subscribeOn(Schedulers.computation())
                .subscribe({ vCard -> accountService.sendTrustRequest(conversation, contact.uri, Blob.fromString(VCardUtils.vcardToString(vCard))) })
                { accountService.sendTrustRequest(conversation, contact.uri) }
        }
    }

    fun clickOnGoingPane() {
        val conf = mConversation?.currentCall
        if (conf != null) {
            view?.goToCallActivity(conf.id, conf.hasActiveVideo())
        } else {
            view?.displayOnGoingCallPane(false)
        }
    }

    fun goToCall(withCamera: Boolean) {
        if (!withCamera && !hardwareService.hasMicrophone()) {
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
                        view.goToCallActivity(conf.id, conf.hasActiveVideo())
                    } else {
                        view.goToCallActivityWithResult(conversation.accountId, conversation.uri, conversation.contact!!.uri, withCamera)
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
            conversationFacade.discardRequest(conversation.accountId, conversation.uri)
            accountService.removeContact(conversation.accountId, conversation.uri.host, true)
        }
        view?.goToHome()
    }

    fun onRefuseIncomingContactRequest() {
        mConversation?.let { conversation ->
            conversationFacade.discardRequest(conversation.accountId, conversation.uri)
        }
        view?.goToHome()
    }

    fun onAcceptIncomingContactRequest() {
        mConversation?.let { conversation ->
            if (conversation.mode.blockingFirst() == Conversation.Mode.Request) {
                conversation.loaded = null
                conversation.clearHistory(true)
                conversation.setMode(Conversation.Mode.Syncing)
            }
            conversationFacade.acceptRequest(conversation.accountId, conversation.uri)
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
        if (isGranted && hardwareService.isVideoAvailable) {
            hardwareService.initVideo()
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
                conversationFacade.setIsComposing(conversation.accountId, conversation.uri, hasMessage)
            }
        }
    }

    fun getNumberOfParticipants() : Int {
        return mConversation?.contacts?.size ?: 0
    }

    private fun showTypingIndicator(): Boolean {
        return preferencesService.settings.enableTypingIndicator
    }

    private fun showReadIndicator(): Boolean {
        return preferencesService.settings.enableReadIndicator
    }

    companion object {
        private val TAG = ConversationPresenter::class.simpleName!!
    }
}