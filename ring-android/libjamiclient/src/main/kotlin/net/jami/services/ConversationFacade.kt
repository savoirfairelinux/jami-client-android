/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
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
package net.jami.services

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import net.jami.model.*
import net.jami.model.Account.ContactLocationEntry
import net.jami.model.Call.CallStatus
import net.jami.model.Interaction.InteractionStatus
import net.jami.services.AccountService.RegisteredName
import net.jami.smartlist.ConversationItemViewModel
import net.jami.utils.FileUtils.moveFile
import net.jami.utils.Log
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class ConversationFacade(
    private val mHistoryService: HistoryService,
    private val mCallService: CallService,
    private val mAccountService: AccountService,
    private val mContactService: ContactService,
    private val mNotificationService: NotificationService,
    private val mHardwareService: HardwareService,
    private val mDeviceRuntimeService: DeviceRuntimeService,
    private val mPreferencesService: PreferencesService
) {
    private val mDisposableBag = CompositeDisposable()
    val currentAccountSubject: Observable<Account> = mAccountService
        .currentAccountSubject
        .switchMapSingle { account: Account -> loadSmartlist(account) }
    private val conversationSubject: Subject<Conversation> = PublishSubject.create()
    val updatedConversation: Observable<Conversation>
        get() = conversationSubject

    fun startConversation(accountId: String, contactId: Uri): Single<Conversation> = getAccountSubject(accountId)
        .map { account: Account -> account.getByUri(contactId) }

    fun getAccountSubject(accountId: String): Single<Account> = mAccountService.getAccountSingle(accountId)
        .flatMap { account: Account -> loadSmartlist(account) }

    val conversationsSubject: Observable<List<Conversation>>
        get() = currentAccountSubject
            .switchMap { obj: Account -> obj.getConversationsSubject() }

    fun messageNotified(accountId: String, conversationUri: Uri, messageId: String) {
        val conversation  = mAccountService.getAccount(accountId)?.getByUri(conversationUri) ?: return
        conversation.getMessage(messageId)?.let { message ->
            message.isNotified = true
        }
        mHistoryService.setMessageNotified(accountId, conversationUri, messageId)
    }

    fun readMessages(accountId: String, contact: Uri): String? {
        val account = mAccountService.getAccount(accountId) ?: return null
        val conversation  = account.getByUri(contact) ?: return null
        return readMessages(account, conversation, true)
    }

    fun readMessages(account: Account, conversation: Conversation, cancelNotification: Boolean): String? {
        val lastMessage = readMessages(conversation) ?: return null
        account.refreshed(conversation)
        if (mPreferencesService.settings.enableReadIndicator) {
            mAccountService.setMessageDisplayed(account.accountId, conversation.uri, lastMessage)
        }
        if (cancelNotification) {
            mNotificationService.cancelTextNotification(account.accountId, conversation.uri)
        }
        return lastMessage
    }

    private fun readMessages(conversation: Conversation): String? {
        val messages = conversation.readMessages()
        var lastRead: String? = null
        for (message in messages) {
            if (conversation.isSwarm) {
                mHistoryService.setMessageNotified(conversation.accountId, conversation.uri, message.messageId!!)
                lastRead = message.messageId
            } else {
                val did = message.daemonId
                if (lastRead == null && did != null && did != 0L)
                    lastRead = java.lang.Long.toString(did, 16)
                mHistoryService.updateInteraction(message, conversation.accountId).subscribe()
            }
        }
        return lastRead
    }

    fun sendTextMessage(c: Conversation, to: Uri, txt: String): Completable {
        if (c.isSwarm) {
            mAccountService.sendConversationMessage(c.accountId, c.uri, txt)
            return Completable.complete()
        }
        return mCallService.sendAccountTextMessage(c.accountId, to.rawUriString, txt)
            .map { id: Long ->
                val message = TextMessage(null, c.accountId, java.lang.Long.toHexString(id), c, txt)
                if (c.isVisible) message.read()
                mHistoryService.insertInteraction(c.accountId, c, message).subscribe()
                c.addTextMessage(message)
                mAccountService.getAccount(c.accountId)!!.conversationUpdated(c)
                message
            }.ignoreElement()
    }

    fun sendTextMessage(c: Conversation, conf: Conference, txt: String) {
        mCallService.sendTextMessage(conf.accountId, conf.id, txt)
        val message = TextMessage(null, conf.accountId, conf.id, c, txt)
        message.read()
        mHistoryService.insertInteraction(c.accountId, c, message).subscribe()
        c.addTextMessage(message)
    }

    fun setIsComposing(accountId: String, conversationUri: Uri, isComposing: Boolean) {
        mCallService.setIsComposing(accountId, conversationUri.uri, isComposing)
    }

    fun sendFile(conversation: Conversation, to: Uri, file: File): Completable {
        if (!file.exists() || !file.canRead()) {
            return Completable.error(IllegalArgumentException("file not found or not readable"))
        }
        if (conversation.isSwarm) {
            val destPath = mDeviceRuntimeService.getNewConversationPath(conversation.accountId, conversation.uri.rawRingId, file.name)
            moveFile(file, destPath)
            mAccountService.sendFile(conversation, destPath)
            return Completable.complete()
        }
        return Single.fromCallable {
            val transfer = DataTransfer(conversation, to.rawRingId, conversation.accountId, file.name, true, file.length(), 0, null)
            mHistoryService.insertInteraction(conversation.accountId, conversation, transfer).blockingAwait()
            transfer.destination = mDeviceRuntimeService.getConversationDir(conversation.uri.rawRingId)
            transfer
        }
            .flatMap { t: DataTransfer -> mAccountService.sendFile(file, t) }
            .flatMapCompletable { transfer: DataTransfer -> Completable.fromAction {
                val destination = File(transfer.destination, transfer.storagePath)
                if (!mDeviceRuntimeService.hardLinkOrCopy(file, destination)) {
                    Log.e(TAG, "sendFile: can't move file to $destination")
                }
            } }
            .subscribeOn(Schedulers.io())
    }

    fun deleteConversationItem(conversation: Conversation, element: Interaction) {
        if (conversation.isSwarm)
            return
        if (element.type === Interaction.InteractionType.DATA_TRANSFER) {
            val transfer = element as DataTransfer
            if (transfer.status === InteractionStatus.TRANSFER_ONGOING) {
                mAccountService.cancelDataTransfer(conversation.accountId, conversation.uri.rawRingId, transfer.messageId, transfer.fileId!!)
            } else {
                val file = mDeviceRuntimeService.getConversationPath(conversation.uri.rawRingId, transfer.storagePath)
                mDisposableBag.add(Completable.mergeArrayDelayError(
                    mHistoryService.deleteInteraction(element.id, element.account!!),
                    Completable.fromAction { file.delete() }
                        .subscribeOn(Schedulers.io()))
                    .subscribe({ conversation.removeInteraction(transfer) })
                        { e: Throwable -> Log.e(TAG, "Can't delete file transfer", e) })
            }
        } else {
            // handling is the same for calls and texts
            mDisposableBag.add(mHistoryService.deleteInteraction(element.id, element.account!!)
                .subscribeOn(Schedulers.io())
                .subscribe({ conversation.removeInteraction(element) })
                    { e: Throwable -> Log.e(TAG, "Can't delete message", e) })
        }
    }

    fun cancelMessage(message: Interaction) {
        val accountId = message.account!!
        mDisposableBag.add(
            mCallService.cancelMessage(accountId, message.id.toLong()).subscribeOn(Schedulers.io())
                .andThen(startConversation(accountId, Uri.fromString(message.conversation!!.participant!!)))
                .subscribe({ c: Conversation -> c.removeInteraction(message) }
                ) { e: Throwable -> Log.e(TAG, "Can't cancel message sending", e) })
    }

    /**
     * Loads the smartlist from cache or database
     *
     * @param account the user account
     * @return an account single
     */
    private fun loadSmartlist(account: Account): Single<Account> {
        synchronized(account) {
            account.historyLoader.let { loader ->
                return loader ?: getSmartlist(account).apply {
                    account.historyLoader = this
                }
            }
        }
    }

    /**
     * Loads history for a specific conversation from cache or database
     *
     * @param account         the user account
     * @param conversationUri the conversation
     * @return a conversation single
     */
    fun loadConversationHistory(account: Account, conversationUri: Uri): Single<Conversation> {
        val conversation = account.getByUri(conversationUri)
            ?: return Single.error(RuntimeException("Can't get conversation $conversationUri"))
        synchronized(conversation) {
            if ((!conversation.isSwarm && conversation.id == null) || (conversation.isSwarm && conversation.mode.blockingFirst() == Conversation.Mode.Request)) {
                return Single.just(conversation)
            }
            var ret = conversation.loaded
            if (ret == null) {
                ret = if (conversation.isSwarm) mAccountService.loadMore(conversation)
                else getConversationHistory(conversation)
                conversation.loaded = ret
            }
            return ret
        }
    }

    fun observeConversation(accountId: String, conversationUri: Uri, hasPresence: Boolean): Observable<ConversationItemViewModel> =
        startConversation(accountId, conversationUri)
            .flatMapObservable { conversation -> observeConversation(conversation, hasPresence) }

    fun observeConversation(conversation: Conversation, hasPresence: Boolean = false): Observable<ConversationItemViewModel> {
        val account = mAccountService.getAccount(conversation.accountId) ?: return Observable.error(IllegalArgumentException())
        return observeConversation(account, conversation, hasPresence)
    }

    private fun observeConversation(account: Account, conversation: Conversation, hasPresence: Boolean): Observable<ConversationItemViewModel> =
         Observable.combineLatest(account.getConversationSubject()
            .filter { c: Conversation -> c == conversation }
            .startWithItem(conversation),
            mContactService.observeContact(conversation.accountId, conversation.contacts, hasPresence)
         ) { c, contacts -> ConversationItemViewModel(c, contacts, hasPresence) }

    fun getSmartList(currentAccount: Observable<Account>, hasPresence: Boolean): Observable<List<Observable<ConversationItemViewModel>>> =
        currentAccount.switchMap { account: Account ->
            account.getConversationsSubject()
                .switchMapSingle { conversations -> if (conversations.isEmpty()) Single.just(emptyList())
                else Observable.fromIterable(conversations)
                    .map { conversation: Conversation -> observeConversation(account, conversation, hasPresence) }
                    .toList(conversations.size) } }

    fun getConversationSmartlist(currentAccount: Observable<Account>): Observable<List<Conversation>> =
        currentAccount
            .switchMapSingle { a -> loadSmartlist(a) }
            .switchMap { account: Account -> account.getConversationsSubject() }

    fun getConversationSmartlist(): Observable<List<Conversation>> = getConversationSmartlist(mAccountService.currentAccountSubject)

    fun getContactList(currentAccount: Observable<Account>): Observable<MutableList<ConversationItemViewModel>> = currentAccount
        .switchMap { account: Account ->
            account.getConversationsSubject()
                .map { conversations -> conversations.filter { c -> !c.isSwarm }
                    .map { conversation -> mContactService.getLoadedConversation(conversation) }}
                .switchMapSingle { conversations -> Single.zip(conversations) { it.toMutableList() as MutableList<ConversationItemViewModel> } }
        }

    fun getConversationList(): Observable<MutableList<ConversationItemViewModel>> = getConversationList(mAccountService.currentAccountSubject)

    fun getConversationList(currentAccount: Observable<Account>): Observable<MutableList<ConversationItemViewModel>> =
        currentAccount.switchMap { account: Account ->
            account.getConversationsSubject()
                .map { conversations -> conversations.map { conversation -> mContactService.getLoadedConversation(conversation) }}
                .switchMapSingle { conversations -> Single.zip(conversations) { it.toMutableList() as MutableList<ConversationItemViewModel> } }
        }

    fun getPendingList(currentAccount: Observable<Account>): Observable<List<Observable<ConversationItemViewModel>>> =
        currentAccount.switchMap { account: Account ->
            account.getPendingSubject()
                .switchMapSingle { conversations -> Observable.fromIterable(conversations)
                    .map { conversation: Conversation -> observeConversation(account, conversation, false) }
                    .toList() } }

    fun getPendingConversationList(currentAccount: Observable<Account>): Observable<List<Conversation>> =
        currentAccount.switchMap { account: Account ->
            account.getPendingSubject()}

    fun getSmartList(hasPresence: Boolean): Observable<List<Observable<ConversationItemViewModel>>> =
        getSmartList(mAccountService.currentAccountSubject, hasPresence)

    val pendingList: Observable<List<Observable<ConversationItemViewModel>>>
        get() = getPendingList(mAccountService.currentAccountSubject)
    val contactList: Observable<MutableList<ConversationItemViewModel>>
        get() = getContactList(mAccountService.currentAccountSubject)

    private fun getSearchResults(account: Account, query: String): Single<List<Observable<ConversationItemViewModel>>> {
        val uri = Uri.fromString(query)
        return if (account.isSip) {
            val contact = account.getContactFromCache(uri)
            mContactService.loadContactData(contact, account.accountId)
                .map { profile -> listOf(Observable.just(ConversationItemViewModel(account.accountId, ContactViewModel(contact, profile, null, false), contact.primaryNumber, null)))}
        } else if (uri.isHexId) {
            mContactService.getLoadedContact(account.accountId, account.getContactFromCache(uri))
                .map { contact -> listOf(Observable.just(ConversationItemViewModel(account.accountId, contact, contact.contact.primaryNumber, null))) }
        } else if (account.canSearch() && !query.contains("@")) {
            mAccountService.searchUser(account.accountId, query)
                .map { results -> results.results!!.map { contact -> observeConversation(account.getByUri(contact.conversationUri.blockingFirst())!!) } }
        } else {
            mAccountService.findRegistrationByName(account.accountId, "", query)
                .map { result: RegisteredName ->
                    if (result.state == 0)
                        listOf(observeConversation(account, account.getByKey(result.address!!).apply {
                            contact?.let { c -> synchronized(c) {
                                if (c.username == null)
                                    c.username = Single.just(result.name)
                            }}
                        }, false))
                    else
                        emptyList()
                }
        }
    }

    private fun getConversationSearchResults(account: Account, query: String): Single<List<Conversation>> {
        val uri = Uri.fromString(query)
        return if (account.isSip || uri.isHexId) {
            /*Single.just(listOf(account.getByUri(uri)!!))
        } else if (uri.isHexId) {*/
            Single.just(listOf(account.getByUri(uri)!!))
        } else if (account.canSearch() && !query.contains("@")) {
            mAccountService.searchUser(account.accountId, query)
                .map { results -> results.results!!.map { contact -> account.getByUri(contact.conversationUri.blockingFirst())!! } }
        } else {
            mAccountService.findRegistrationByName(account.accountId, "", query)
                .map { result: RegisteredName ->
                    if (result.state == 0)
                        listOf(account.getByKey(result.address!!).apply {
                            contact?.let { c -> synchronized(c) {
                                if (c.username == null)
                                    c.username = Single.just(result.name)
                            }}
                        })
                    else
                        emptyList()
                }
        }
    }

    fun getConversationSearchResults(account: Account, query: Observable<String>): Observable<List<Conversation>> =
        query.switchMapSingle { q: String ->
            if (q.isEmpty())
                Single.just(emptyList())
            else getConversationSearchResults(account, q)
        }

    data class ConversationList(val conversations: List<Conversation> = emptyList(), val publicDirectory: List<Conversation> = emptyList(), val query: String = "") {
        fun isEmpty(): Boolean = conversations.isEmpty() && publicDirectory.isEmpty()

        fun getCombinedSize(): Int {
            if (publicDirectory.isEmpty()) return conversations.size
            if (conversations.isEmpty()) return publicDirectory.size + 1
            return conversations.size + publicDirectory.size + 2
        }

        operator fun get(index: Int): Conversation? {
            return if (publicDirectory.isEmpty()) conversations.getOrNull(index)
            else if (conversations.isEmpty() || index < publicDirectory.size + 1) publicDirectory.getOrNull(index - 1)
            else conversations.getOrNull(index - publicDirectory.size - 2)
        }

        fun getHeader(index: Int): ConversationItemViewModel.Title {
            return if (publicDirectory.isEmpty()) ConversationItemViewModel.Title.None
            else if (index == 0) ConversationItemViewModel.Title.PublicDirectory
            else if (conversations.isNotEmpty() && index == publicDirectory.size + 1) ConversationItemViewModel.Title.Conversations
            else ConversationItemViewModel.Title.None
        }
    }

    fun getFilteredConversationList(currentAccount: Observable<Account>, query: Observable<String>): Observable<List<Conversation>> =
        currentAccount.switchMap { account ->
            Observable.combineLatest(account.getConversationsSubject(), query) { conversations, q -> ConversationList(conversations, emptyList(), q) }
                .flatMapSingle { list ->
                    if (list.query.isNotEmpty() && list.conversations.isNotEmpty()) {
                        val lq = list.query.lowercase()
                        Maybe.concatEager(list.conversations.map { c -> mContactService.getLoadedConversation(c)
                            .filter { it.matches(lq) }.map { c }
                        }).toList()
                    } else Single.just(list.conversations)
                }
        }

    fun getFullConversationList(currentAccount: Observable<Account>, query: Observable<String>): Observable<ConversationList> =
        currentAccount.switchMap { account ->
            Observable.combineLatest(
                account.getConversationsSubject(),
                getConversationSearchResults(account, query),
                query
            ) { conversations, searchResults, q -> ConversationList(conversations, searchResults, q) }
        }.flatMapSingle { list ->
            if (list.query.isNotEmpty() && list.conversations.isNotEmpty()) {
                val lq = list.query.lowercase()
                Maybe.concatEager(list.conversations.map { c -> mContactService.getLoadedConversation(c)
                    .filter { it.matches(lq) }.map { c }
                }).toList().map { newList -> ConversationList(newList, list.publicDirectory, list.query) }
            } else Single.just(list)
        }

    /**
     * Loads the smartlist from the database and updates the view
     *
     * @param account the user account
     */
    private fun getSmartlist(account: Account): Single<Account> {
        val actions: MutableList<Completable> = ArrayList(account.getConversations().size + 1)
        for (c in account.getConversations()) {
            if (c.isSwarm && c.lastElementLoaded != null) actions.add(c.lastElementLoaded!!)
        }
        actions.add(mHistoryService.getSmartlist(account.accountId)
            .flatMapCompletable { conversationHistoryList: List<Interaction> ->
                Completable.fromAction {
                    val conversations: MutableList<Conversation> = ArrayList()
                    for (e in conversationHistoryList) {
                        val conversation = account.getByUri(e.conversation!!.participant) ?: continue
                        conversation.id = e.conversation!!.id
                        conversation.addElement(e)
                        conversation.setLastMessageNotified(mHistoryService.getLastMessageNotified(account.accountId, conversation.uri))
                        conversations.add(conversation)
                    }
                    account.setHistoryLoaded(conversations)
                }
            })
        return Completable.merge(actions)
            .andThen(Single.just(account))
            .cache()
    }

    /**
     * Loads a conversation's history from the database
     *
     * @param conversation a conversation object with a valid conversation ID
     * @return a conversation single
     */
    private fun getConversationHistory(conversation: Conversation): Single<Conversation> =
        mHistoryService.getConversationHistory(conversation.accountId, conversation.id!!)
            .map { loadedConversation: List<Interaction> ->
                conversation.clearHistory(true)
                conversation.setHistory(loadedConversation)
                conversation
            }
            .cache()

    fun clearHistory(accountId: String, contact: Uri): Completable = mHistoryService
        .clearHistory(contact.uri, accountId, false)
        .doOnSubscribe {
            mAccountService.getAccount(accountId)?.clearHistory(contact, false)
        }

    fun clearAllHistory(): Completable = mAccountService.observableAccountList
        .firstElement()
        .flatMapCompletable { accounts: List<Account> ->
            mHistoryService.clearHistory(accounts)
                .doOnSubscribe {
                    for (account in accounts)
                        account.clearAllHistory()
                }
        }

    fun updateTextNotifications(accountId: String, conversations: List<Conversation>) {
        Log.d(TAG, "updateTextNotifications() " + accountId + " " + conversations.size)
        for (conversation in conversations) {
            mNotificationService.showTextNotification(accountId, conversation)
        }
    }

    private fun parseNewMessage(txt: TextMessage) {
        val accountId = txt.account!!
        if (txt.isRead) {
            if (txt.messageId == null) {
                mHistoryService.updateInteraction(txt, accountId).subscribe()
            }
            if (mPreferencesService.settings.enableReadIndicator) {
                if (txt.messageId != null) {
                    mAccountService.setMessageDisplayed(txt.account, Uri(Uri.SWARM_SCHEME, txt.conversationId!!), txt.messageId!!)
                } else {
                    mAccountService.setMessageDisplayed(txt.account, Uri(Uri.JAMI_URI_SCHEME, txt.author!!), txt.daemonIdString!!)
                }
            }
        }
        getAccountSubject(accountId)
            .flatMapObservable { obj: Account -> obj.getConversationsSubject() }
            .firstOrError()
            .subscribeOn(Schedulers.io())
            .subscribe({ c: List<Conversation> -> updateTextNotifications(txt.account!!, c) })
            { e: Throwable -> Log.e(TAG, e.message!!) }
    }

    fun acceptRequest(accountId: String, contactUri: Uri) {
        mPreferencesService.removeRequestPreferences(accountId, contactUri.rawRingId)
        mAccountService.acceptTrustRequest(accountId, contactUri)
    }

    fun discardRequest(accountId: String, contact: Uri) {
        //mHistoryService.clearHistory(contact.uri, accountId, true).subscribe()
        mPreferencesService.removeRequestPreferences(accountId, contact.rawRingId)
        mAccountService.discardTrustRequest(accountId, contact)
    }

    private fun handleDataTransferEvent(transfer: DataTransfer) {
        val account = transfer.account!!
        val conversation = mAccountService.getAccount(account)!!.onDataTransferEvent(transfer)
        val status = transfer.status
        Log.d(TAG, "handleDataTransferEvent $status " + transfer.canAutoAccept(mPreferencesService.getMaxFileAutoAccept(account)))
        if (status === InteractionStatus.TRANSFER_AWAITING_HOST || status === InteractionStatus.FILE_AVAILABLE) {
            if (transfer.canAutoAccept(mPreferencesService.getMaxFileAutoAccept(account))) {
                mAccountService.acceptFileTransfer(conversation, transfer.fileId!!, transfer)
                return
            }
        }
        mNotificationService.handleDataTransferNotification(transfer, conversation, conversation.isVisible)
    }

    private fun onConfStateChange(conference: Conference) {
        Log.d(TAG, "onConfStateChange Thread id: " + Thread.currentThread().id)
    }

    private fun onCallStateChange(call: Call) {
        Log.d(TAG, "onCallStateChange Thread id: " + Thread.currentThread().id)
        val newState = call.callStatus
        val incomingCall = newState === CallStatus.RINGING && call.isIncoming
        mHardwareService.updateAudioState(newState, incomingCall, call.hasMedia(Media.MediaType.MEDIA_TYPE_VIDEO), mHardwareService.isSpeakerphoneOn)
        val account = mAccountService.getAccount(call.account!!) ?: return
        val contact = call.contact
        val conversationId = call.conversationId
        Log.w(TAG, "CallStateChange " + call.daemonIdString + " conversationId:" + conversationId + " contact:" + contact + " " + contact?.conversationUri?.blockingFirst())
        val conversation = if (conversationId == null)
            if (contact == null)
                null
            else
                account.getByUri(contact.conversationUri.blockingFirst()) ?: account.getByUri(contact.uri)
        else
            account.getByUri(Uri(Uri.SWARM_SCHEME, conversationId))
        val conference = if (conversation != null) (conversation.getConference(call.daemonIdString) ?: Conference(call).apply {
            if (newState === CallStatus.OVER) return@onCallStateChange
            conversation.addConference(this)
            account.updated(conversation)
        }) else null

        Log.w(TAG, "CALL_STATE_CHANGED : updating call state to $newState")
        if ((newState.isRinging || newState === CallStatus.CURRENT) && call.timestamp == 0L) {
            call.timestamp = System.currentTimeMillis()
        }
        if (incomingCall) {
            mNotificationService.handleCallNotification(conference!!, false)
            mHardwareService.setPreviewSettings()
        } else if (newState === CallStatus.CURRENT && call.isIncoming
            || newState === CallStatus.RINGING && !call.isIncoming) {
            mNotificationService.handleCallNotification(conference!!, false)
        } else if (newState.isOver) {
            if (conference != null)
                mNotificationService.handleCallNotification(conference, true)
            else {
                mNotificationService.removeCallNotification(call.id)
            }
            mHardwareService.closeAudioState()
            val now = System.currentTimeMillis()
            if (call.timestamp == 0L) {
                call.timestamp = now
            }
            if (call.timestampEnd == 0L) {
                call.timestampEnd = now
            }
            if (conference != null && conference.removeParticipant(call) && conversation != null && !conversation.isSwarm) {
                Log.w(TAG, "Adding call history for conversation " + conversation.uri)
                mHistoryService.insertInteraction(account.accountId, conversation, call).subscribe()
                conversation.addCall(call)
                if (call.isIncoming && call.isMissed) {
                    mNotificationService.showMissedCallNotification(call)
                }
                account.updated(conversation)
            }
            if (conversation != null && conference != null && conference.participants.isEmpty()) {
                conversation.removeConference(conference)
            }
        }
    }

    fun cancelFileTransfer(accountId: String, conversationId: Uri, messageId: String?, fileId: String?) {
        mAccountService.cancelDataTransfer(accountId, if (conversationId.isSwarm) conversationId.rawRingId else "", messageId, fileId!!)
        mNotificationService.removeTransferNotification(accountId, conversationId, fileId)
        if (!conversationId.isSwarm)
            mAccountService.getAccount(accountId)?.getDataTransfer(fileId)?.let { transfer ->
                deleteConversationItem(transfer.conversation as Conversation, transfer)
            }
    }

    fun removeConversation(accountId: String, conversationUri: Uri): Completable {
        val account = mAccountService.getAccount(accountId) ?: return Completable.error(IllegalArgumentException("Unknown account"))
        return if (conversationUri.isSwarm) {
            // For a one to one conversation, contact is strongly related, so remove the contact.
            // This will remove related conversations
            val conversation = account.getSwarm(conversationUri.rawRingId)
            if (conversation != null && conversation.mode.blockingFirst() === Conversation.Mode.OneToOne) {
                val contact = conversation.contact
                mAccountService.removeContact(accountId, contact!!.uri.rawRingId, false)
                Completable.complete()
            } else {
                mAccountService.removeConversation(accountId, conversationUri)
            }
        } else {
            mHistoryService
                .clearHistory(conversationUri.uri, accountId, true)
                .doOnSubscribe {
                    account.clearHistory(conversationUri, true)
                    mAccountService.removeContact(accountId, conversationUri.rawRingId, false)
                }
        }
    }

    fun banConversation(accountId: String, conversationUri: Uri) {
        if (conversationUri.isSwarm) {
            startConversation(accountId, conversationUri)
                .subscribe { conversation: Conversation ->
                    try {
                        val contact = conversation.contact
                        mAccountService.removeContact(accountId, contact!!.uri.rawUriString, true)
                    } catch (e: Exception) {
                        mAccountService.removeConversation(accountId, conversationUri)
                    }
                }
            //return mAccountService.removeConversation(accountId, conversationUri);
        } else {
            mAccountService.removeContact(accountId, conversationUri.rawUriString, true)
        }
    }

    fun createConversation(accountId: String, currentSelection: Collection<Contact>): Single<Conversation> {
        val contactIds = currentSelection.map { contact -> contact.primaryNumber }
        return mAccountService.startConversation(accountId, contactIds)
    }

    companion object {
        private val TAG = ConversationFacade::class.simpleName!!
    }

    init {
        mDisposableBag.add(mCallService.callsUpdates
            .subscribe { call: Call -> onCallStateChange(call) })

        /*mDisposableBag.add(mCallService.getConnectionUpdates()
                    .subscribe(mNotificationService::onConnectionUpdate));*/
        mDisposableBag.add(mCallService.confsUpdates
            .observeOn(Schedulers.io())
            .subscribe { conference: Conference -> onConfStateChange(conference) })
        mDisposableBag.add(currentAccountSubject
            .switchMap { a: Account -> a.getPendingSubject()
                    .doOnNext { mNotificationService.showIncomingTrustRequestNotification(a) } }
            .subscribe())
        mDisposableBag.add(mAccountService.incomingRequests
            .concatMapSingle { r: TrustRequest -> getAccountSubject(r.accountId) }
            .subscribe({ account: Account -> mNotificationService.showIncomingTrustRequestNotification(account) })
                { Log.e(TAG, "Error showing contact request") })
        mDisposableBag.add(mAccountService
            .incomingMessages
            .concatMapSingle { msg: TextMessage ->
                getAccountSubject(msg.account!!)
                    .map { a: Account ->
                        a.addTextMessage(msg)
                        msg }
            }
            .subscribe({ txt: TextMessage -> parseNewMessage(txt) })
                { e: Throwable -> Log.e(TAG, "Error adding text message", e) })
        mDisposableBag.add(mAccountService.incomingSwarmMessages
                .subscribe({ txt: TextMessage -> parseNewMessage(txt) },
                    { e: Throwable -> Log.e(TAG, "Error adding text message", e) }))
        mDisposableBag.add(mAccountService.locationUpdates
            .concatMapSingle { location: AccountService.Location ->
                getAccountSubject(location.account)
                    .map { a: Account ->
                        val expiration = a.onLocationUpdate(location)
                        mDisposableBag.add(Completable.timer(expiration, TimeUnit.MILLISECONDS)
                                .subscribe { a.maintainLocation() })
                        location
                    } }
            .subscribe())
        mDisposableBag.add(mAccountService.observableAccountList
            .switchMap { accounts: List<Account> ->
                val r: MutableList<Observable<Pair<Account, ContactLocationEntry>>> = ArrayList(accounts.size)
                for (a in accounts) r.add(a.locationUpdates.map { s: ContactLocationEntry -> Pair(a, s) })
                Observable.merge(r)
            }
            .distinctUntilChanged()
            .subscribe { t: Pair<Account, ContactLocationEntry> ->
                Log.e(TAG, "Location reception started for " + t.second.contact)
                mNotificationService.showLocationNotification(t.first, t.second.contact)
                mDisposableBag.add(t.second.location.doOnComplete {
                    mNotificationService.cancelLocationNotification(t.first, t.second.contact)
                }.subscribe()) })
        mDisposableBag.add(mAccountService
            .messageStateChanges
            .concatMapMaybe { e: Interaction ->
                getAccountSubject(e.account!!)
                    .flatMapMaybe<Conversation> { a: Account -> Maybe.fromCallable {
                            if (e.conversation == null)
                                a.getByUri(Uri(Uri.SWARM_SCHEME, e.conversationId!!))
                            else
                                a.getByUri(e.conversation!!.participant)
                        }
                    }
                    .doOnSuccess { conversation -> conversation.updateInteraction(e) }
            }
            .subscribe({}) { e: Throwable -> Log.e(TAG, "Error updating text message", e) })
        mDisposableBag.add(mAccountService.dataTransfers
                .subscribe({ transfer: DataTransfer -> handleDataTransferEvent(transfer) },
                     { e: Throwable -> Log.e(TAG, "Error adding data transfer", e) }))
    }
}