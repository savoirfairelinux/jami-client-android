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
package net.jami.conversation

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
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
    val callService: CallService,
    val contactService: ContactService,
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
    private var searchQuerySubject: Subject<String>? = null

    fun init(conversationUri: Uri, accountId: String) {
        if (conversationUri == mConversationUri) return
        Log.w(TAG, "init $conversationUri $accountId")
        val settings = preferencesService.settings
        view?.setSettings(settings.enableLinkPreviews)
        mConversationUri = conversationUri
        mCompositeDisposable.add(conversationFacade.getAccountSubject(accountId)
            .flatMapObservable { account: Account ->
                val conversation = account.getByUri(conversationUri)!!
                conversation.mode.flatMap { conversationMode: Conversation.Mode ->
                    if (conversationMode === Conversation.Mode.Legacy)
                        conversation.contact!!.conversationUri.switchMapSingle { uri ->
                            conversationFacade.startConversation(accountId, uri)
                        }
                    else
                        Observable.just(conversation)
                }.switchMapSingle { conversation ->
                    conversationFacade.loadConversationHistory(conversation)
                        .map { Pair(account, it) }
                }
            }
            .observeOn(uiScheduler)
            .subscribe(
                { (account, conversation) ->
                    setConversation(account, conversation)
                }
            ) { error: Throwable ->
                view?.goToHome()
                Log.e(TAG, "Error loading conversation", error)
            }
        )
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
        Log.w(TAG, "setConversation ${conversation.aggregateHistory.size}")
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

    private fun initContact(account: Account, c: ConversationItemViewModel, view: ConversationView) {
        if (account.isJami) {
            Log.w(TAG, "initContact ${c.uri} mode: ${c.mode}")
            if (c.mode === Conversation.Mode.Syncing) {
                view.switchToSyncingView()
            } else if (c.mode == Conversation.Mode.Request) {
                view.switchToIncomingTrustRequestView(c.uriTitle/*ConversationItemViewModel.getUriTitle(conversation.uri, contacts)*/)
            } else if (c.isSwarm || account.isContact(c.uri)) {
                if ((c.mode == Conversation.Mode.OneToOne) && c.getContact()?.contact?.isBanned == true) {
                    view.switchToBannedView()
                } else
                    view.switchToConversationView()
            } else {
                val req = c.request
                if (req == null) {
                    view.switchToUnknownView(c.uriTitle)
                } else {
                    view.switchToIncomingTrustRequestView(req.profile?.blockingGet()?.displayName ?: c.uriTitle)
                }
            }
        } else {
            view.switchToConversationView()
        }
        view.displayContact(c)
    }

    private fun initView(account: Account, c: Conversation, view: ConversationView) {
        Log.w(TAG, "initView ${c.uri}")
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
            .subscribe({ conversation -> this.view?.refreshView(conversation) })
                { e -> Log.e(TAG, "Can't update element", e) })
        disposable.add(c.cleared
            .observeOn(uiScheduler)
            .subscribe({ conversation -> this.view?.refreshView(conversation) })
                { e -> Log.e(TAG, "Can't update elements", e) })
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
        disposable.add(callService.callsUpdates
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

    fun scrollToMessage(messageId: String) {
        if(mConversation!!.getMessage(messageId) != null) {
            view?.scrollToMessage(messageId)
        } else {
            mConversationDisposable?.add(accountService.loadUntil(mConversation!!, until = messageId)
                .observeOn(uiScheduler)
                .subscribe { _ -> this.view?.scrollToMessage(messageId)})
        }
    }

    fun openContact() {
        mConversation?.let { conversation -> view?.goToContactActivity(conversation.accountId, conversation.uri) }
    }

    fun sendTextMessage(message: String?, replyTo: Interaction? = null) {
        val conversation = mConversation
        if (message.isNullOrEmpty() || conversation == null) {
            return
        }
        val conference = conversation.currentCall
        if (conversation.isSwarm || conference == null || !conference.isOnGoing) {
            conversationFacade.sendTextMessage(conversation, conversation.uri, message, replyTo?.messageId).subscribe()
        } else {
            conversationFacade.sendTextMessage(conversation, conference, message)
        }
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

    fun shareFile(file: DataTransfer) {
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

    fun goToGroupCall(media: Boolean) {
        view?.goToGroupCall(mConversation!!, mConversation!!.uri, media)
    }

    fun refuseFile(transfer: DataTransfer) {
        view?.refuseFile(mConversation!!.accountId, mConversationUri!!, transfer)
    }

    fun deleteConversationItem(element: Interaction) {
        conversationFacade.deleteConversationItem(mConversation!!, element)
    }

    fun startReplyTo(interaction: Interaction) {
        view?.startReplyTo(interaction)
    }

    fun cancelMessage(message: Interaction) {
        val conversation = mConversation ?: return
        conversationFacade.cancelMessage(conversation, message)
    }

    private fun sendTrustRequest() {
        val conversation = mConversation ?: return
        val contact = conversation.contact ?: return
        contact.status = Contact.Status.REQUEST_SENT
        vCardService.loadSmallVCardWithDefault(conversation.accountId, VCardService.MAX_SIZE_REQUEST)
            .subscribeOn(Schedulers.computation())
            .subscribe({ vCard -> accountService.sendTrustRequest(conversation, contact.uri, Blob.fromString(VCardUtils.vcardToString(vCard)))})
            { accountService.sendTrustRequest(conversation, contact.uri) }
    }

    fun clickOnGoingPane() {
        val conf = mConversation?.currentCall
        if (conf != null) {
            view?.goToCallActivity(conf.id, conf.hasActiveVideo())
        } else {
            view?.displayOnGoingCallPane(false)
        }
    }

    /**
     * Navigates to the call activity with the specified camera option.
     *
     * @param withCamera Indicates whether the call should include camera functionality.
     */
    fun goToCall(withCamera: Boolean) {
        // Check if the device has a camera or microphone
        if (!withCamera && !hardwareService.hasMicrophone()) {
            view!!.displayErrorToast(Error.NO_MICROPHONE)
            return
        }

        // Check if it's a group call
        if (isSwarmGroup()) {
            goToGroupCall(withCamera)
            return
        }

        // Get the conversation and navigate to the call activity
        mCompositeDisposable.add(mConversationSubject
            .firstElement()
            .subscribe { conversation: Conversation ->
                val view = view
                if (view != null) {
                    val conf = conversation.currentCall
                    if (conf != null && conf.participants.isNotEmpty()
                        && conf.participants[0].callStatus !== Call.CallStatus.INACTIVE
                        && conf.participants[0].callStatus !== Call.CallStatus.FAILURE) {
                        // Navigate to the call activity with the existing call
                        view.goToCallActivity(conf.id, conf.hasActiveVideo())
                    } else {
                        // Navigate to the call activity with the conversation details
                        view.goToCallActivityWithResult(
                            conversation.accountId, conversation.uri,
                            conversation.contact!!.uri, withCamera
                        )
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
            conversationFacade.banConversation(conversation.accountId, conversation.uri)
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

    val path: Pair<String, Uri>
        get() = Pair(mConversation!!.accountId, mConversationUri!!)

    fun onComposingChanged(hasMessage: Boolean) {
        if (showTypingIndicator()) {
            mConversation?.let { conversation ->
                conversationFacade.setIsComposing(conversation.accountId, conversation.uri, hasMessage)
            }
        }
    }

    fun isGroup() = mConversation?.isGroup() ?: false

    private fun isSwarmGroup() = mConversation?.isSwarmGroup() ?: false

    private fun showTypingIndicator(): Boolean {
        return preferencesService.settings.enableTypingIndicator
    }


    fun setSearchQuery(query: String) {
        searchQuerySubject?.onNext(query)
    }

    fun startSearch() {
        if (searchQuerySubject == null) {
            val conversation = mConversation ?: return
            mCompositeDisposable.add(PublishSubject.create<String>()
                .apply { searchQuerySubject = this }
                .switchMap { accountService.searchConversation(conversation.accountId, conversation.uri, it) }
                .observeOn(uiScheduler)
                .subscribe { view?.addSearchResults(it.results) })
        }
    }

    fun stopSearch() {
        searchQuerySubject?.let {
            it.onComplete()
            searchQuerySubject = null
        }
    }

    fun sendReaction(interaction: Interaction, text: CharSequence) {
        val conversation = mConversation ?: return
        accountService.sendConversationReaction(conversation.accountId, conversation.uri, text.toString(), interaction.messageId ?: return)
    }

    fun shareText(interaction: TextMessage) {
        view!!.shareText(interaction.body ?: return)
    }

    /**
     * Remove the reaction (emoji)
     * @param reactionToRemove
     */
    fun removeReaction(reactionToRemove: Interaction) {
        val conversation = mConversation ?: return
        accountService.editConversationMessage(
            conversation.accountId, conversation.uri, "", reactionToRemove.messageId!!
        )
    }

    fun editMessage(accountId: String, conversationUri: Uri, messageId: String, newMessage: String) {
        val message = mConversation?.getMessage(messageId)
        if (message?.body != newMessage)
            accountService.editConversationMessage(accountId, conversationUri, newMessage, messageId)
    }

    companion object {
        private val TAG = ConversationPresenter::class.simpleName!!
    }
}