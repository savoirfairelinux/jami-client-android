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
package net.jami.services

import com.google.gson.JsonParser
import ezvcard.VCard
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.SingleSubject
import io.reactivex.rxjava3.subjects.Subject
import net.jami.daemon.*
import net.jami.model.*
import net.jami.model.Interaction.InteractionStatus
import net.jami.utils.Log
import net.jami.utils.SwigNativeConverter
import net.jami.utils.VCardUtils
import java.io.File
import java.io.UnsupportedEncodingException
import java.net.SocketException
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.absoluteValue
import kotlin.math.min

/**
 * This service handles the accounts
 * - Load and manage the accounts stored in the daemon
 * - Keep a local cache of the accounts
 * - handle the callbacks that are send by the daemon
 */
class AccountService(
    private val mExecutor: ScheduledExecutorService,
    private val mHistoryService: HistoryService,
    private val mDeviceRuntimeService: DeviceRuntimeService,
    private val mVCardService: VCardService
) {
    private val scheduler = Schedulers.from(mExecutor)
    /**
     * @return the current Account from the local cache
     */
    var currentAccount: Account?
        get() = mAccountList.getOrNull(0)
        set(account) {
            // the account order is changed
            // the current Account is now on the top of the list
            val accounts: List<Account> = mAccountList
            if (account == null || accounts.isEmpty() || accounts[0] === account)
                return
            val orderedAccountIdList: MutableList<String> = ArrayList(accounts.size)
            val selectedID = account.accountId
            orderedAccountIdList.add(selectedID)
            for (a in accounts) {
                if (a.accountId == selectedID)
                    continue
                orderedAccountIdList.add(a.accountId)
            }
            setAccountOrder(orderedAccountIdList)
        }

    private var mAccountList: List<Account> = ArrayList()
    private var mHasSipAccount = false
    private var mHasRingAccount = false
    private var mStartingTransfer: DataTransfer? = null
    private val accountsSubject = BehaviorSubject.create<List<Account>>()
    private val observableAccounts: Subject<Account> = PublishSubject.create()
    val currentAccountSubject: Observable<Account> = accountsSubject
        .filter { l -> l.isNotEmpty() }
        .map { l -> l[0] }
        .distinctUntilChanged()

    data class Message(
        val accountId: String,
        val messageId: String?,
        val callId: String?,
        val author: String,
        val messages: Map<String, String>
    )

    class Location(
        val account: String,
        val callId: String?,
        val peer: Uri,
        var date: Long) {
        enum class Type {
            Position, Stop
        }

        lateinit var type: Type
        var latitude = 0.0
        var longitude = 0.0

        override fun toString(): String = "Location{$type $latitude $longitude $date account:$account callId:$callId peer:$peer}"
    }

    private val incomingMessageSubject: Subject<Message> = PublishSubject.create()
    private val incomingSwarmMessageSubject: Subject<Interaction> = PublishSubject.create()
    private val incomingGroupCallSubject: Subject<Conversation> = PublishSubject.create()
    val incomingMessages: Observable<TextMessage> = incomingMessageSubject
        .flatMapMaybe { msg: Message ->
            val message = msg.messages[CallService.MIME_TEXT_PLAIN]
            if (message != null) {
                return@flatMapMaybe mHistoryService
                    .incomingMessage(msg.accountId, msg.messageId, msg.author, message)
                    .toMaybe()
            }
            Maybe.empty()
        }
        .share()
    val locationUpdates: Observable<Location> = incomingMessageSubject
        .flatMapMaybe { msg: Message ->
            try {
                val loc = msg.messages[CallService.MIME_GEOLOCATION] ?: return@flatMapMaybe Maybe.empty<Location>()
                val obj = JsonParser.parseString(loc).asJsonObject
                if (obj.size() < 2) return@flatMapMaybe Maybe.empty<Location>()
                Maybe.just(Location(msg.accountId, msg.callId, Uri.fromId(msg.author), obj["time"].asLong).apply {
                    val t = obj["type"]
                    if (t == null || t.asString.lowercase() == Location.Type.Position.toString().lowercase()) {
                        type = Location.Type.Position
                        latitude = obj["lat"].asDouble
                        longitude = obj["long"].asDouble
                    } else if (t.asString.lowercase() == Location.Type.Stop.toString().lowercase()) {
                        type = Location.Type.Stop
                    }
                })
            } catch (e: Exception) {
                Log.w(TAG, "Failed to receive geolocation", e)
                Maybe.empty<Location>()
            }
        }
        .share()
    private val messageSubject: Subject<Interaction> = PublishSubject.create()
    val dataTransfers: Subject<DataTransfer> = PublishSubject.create()
    private val incomingRequestsSubject: Subject<TrustRequest> = PublishSubject.create()

    data class RegisteredName(
        val accountId: String,
        val name: String,
        val address: String? = null,
        val state: Int = 0
    )

    data class ConversationSearchResult(val results: List<Interaction>)
    private val conversationSearches: MutableMap<Long, Subject<ConversationSearchResult>> = ConcurrentHashMap()
    private val loadingTasks: MutableMap<Long, SingleSubject<List<Interaction>>> = ConcurrentHashMap()

    class UserSearchResult(val accountId: String, val query: String, var state: Int = 0) {
        var results: List<Contact>? = null
    }

    private val registeredNameSubject: Subject<RegisteredName> = PublishSubject.create()
    private val searchResultSubject: Subject<UserSearchResult> = PublishSubject.create()

    private data class ExportOnRingResult (
        val accountId: String,
        val code: Int,
        val pin: String?
    )

    private data class DeviceRevocationResult (
        val accountId: String,
        val deviceId: String,
        val code: Int
    )

    private data class MigrationResult (
        val accountId: String,
        val state: String
    )

    private val mExportSubject: Subject<ExportOnRingResult> = PublishSubject.create()
    private val mDeviceRevocationSubject: Subject<DeviceRevocationResult> = PublishSubject.create()
    private val mMigrationSubject: Subject<MigrationResult> = PublishSubject.create()
    private val registeredNames: Observable<RegisteredName>
        get() = registeredNameSubject
    private val searchResults: Observable<UserSearchResult>
        get() = searchResultSubject
    val incomingSwarmMessages: Observable<TextMessage>
        get() = incomingSwarmMessageSubject
            .filter { i: Interaction -> i is TextMessage }
            .map { i: Interaction -> i as TextMessage }
    val messageStateChanges: Observable<Interaction>
        get() = messageSubject
    val incomingRequests: Observable<TrustRequest>
        get() = incomingRequestsSubject
    val incomingGroupCall: Observable<Conversation>
        get() = incomingGroupCallSubject

    /**
     * @return true if at least one of the loaded accounts is a SIP one
     */
    fun hasSipAccount(): Boolean = mHasSipAccount

    /**
     * @return true if at least one of the loaded accounts is a Jami one
     */
    fun hasJamiAccount(): Boolean = mHasRingAccount

    /**
     * Loads the accounts from the daemon and then builds the local cache (also sends ACCOUNTS_CHANGED event)
     *
     * @param isConnected sets the initial connection state of the accounts
     */
    fun loadAccountsFromDaemon(isConnected: Boolean) {
        refreshAccountsCacheFromDaemon()
        setAccountsActive(isConnected)
    }

    private fun refreshAccountsCacheFromDaemon() {
        val curList: List<Account> = mAccountList
        val toLoad: MutableList<Account> = ArrayList()
        val newAccounts: List<Account> = JamiService.getAccountList().map { id ->
            curList.find { it.accountId == id } ?: Account(id, JamiService.getAccountDetails(id), JamiService.getCredentials(id), JamiService.getVolatileAccountDetails(id)).apply {
                toLoad.add(this)
            }
        }
        mAccountList = newAccounts
        val scheduler = Schedulers.computation()
        toLoad.forEach {
            scheduler.createWorker().schedule {
                loadAccount(it)
                it.loadedSubject.onComplete()
            }
        }
        // Cleanup removed accounts
        for (acc in curList) if (!newAccounts.contains(acc)) acc.cleanup()
        accountsSubject.onNext(newAccounts)
    }

    private fun loadAccount(account: Account) {
        if (!account.isJami) {
            return
        }
        Log.w(TAG, "${account.accountId} loading devices")
        account.devices = JamiService.getKnownRingDevices(account.accountId).toNative()
        Log.w(TAG, "${account.accountId} loading contacts")
        account.setContacts(JamiService.getContacts(account.accountId).toNative())
        val conversations: List<String> = JamiService.getConversations(account.accountId)
        Log.w(TAG, "${account.accountId} loading ${conversations.size} conversations: ")
        for (conversationId in conversations) {
            try {
                val info: Map<String, String> = JamiService.conversationInfos(account.accountId, conversationId).toNativeFromUtf8()
                //info.forEach { (key, value) -> Log.w(TAG, "conversation info: $key $value") }
                val mode = if ("true" == info["syncing"]) Conversation.Mode.Syncing else Conversation.Mode.values()[info["mode"]?.toInt() ?: Conversation.Mode.Syncing.ordinal]
                val conversation = account.newSwarm(conversationId, mode)
                conversation.setProfile(mVCardService.loadConversationProfile(info))

                val preferences = // Load conversation preferences (color, symbol, etc.)
                    JamiService.getConversationPreferences(account.accountId, conversationId)
                conversation.updatePreferences(preferences)

                conversation.setLastMessageNotified(mHistoryService.getLastMessageNotified(account.accountId, conversation.uri))
                for (member in JamiService.getConversationMembers(account.accountId, conversationId)) {
                    /*for (Map.Entry<String, String> i : member.entrySet()) {
                        Log.w(TAG, "conversation member: " + i.getKey() + " " + i.getValue());
                    }*/
                    val uri = Uri.fromId(member["uri"]!!)
                    //String role = member.get("role");
                    val lastDisplayed = member["lastDisplayed"]
                    var contact = conversation.findContact(uri)
                    if (contact == null) {
                        contact = account.getContactFromCache(uri)
                        conversation.addContact(contact)
                    }
                    if (!lastDisplayed.isNullOrEmpty()) {
                        if (contact.isUser) {
                            conversation.setLastMessageRead(lastDisplayed)
                        } else {
                            conversation.setLastMessageDisplayed(uri.host, lastDisplayed)
                        }
                    }
                }
                if (!conversation.lastElementLoadedSubject.hasValue())
                    conversation.lastElementLoadedSubject.onSuccess(loadMore(conversation, 8).ignoreElement().cache())
                account.conversationStarted(conversation)
            } catch (e: Exception) {
                Log.w(TAG, "Error loading conversation", e)
            }
        }
        Log.w(TAG, "${account.accountId} loading conversation requests")
        for (requestData in JamiService.getConversationRequests(account.accountId).map { it.toNativeFromUtf8() }) {
            try {
                /* for ((key, value) in requestData.entries)
                Log.e(TAG, "Request: $key $value") */
                val from = Uri.fromString(requestData["from"]!!)
                val conversationId = requestData["id"] ?: continue
                account.addRequest(TrustRequest(
                    account.accountId,
                    from,
                    requestData["received"]!!.toLong() * 1000L,
                    Uri(Uri.SWARM_SCHEME, conversationId),
                    mVCardService.loadConversationProfile(requestData),
                    requestData["mode"]?.let { m -> Conversation.Mode.values()[m.toInt()] } ?: Conversation.Mode.OneToOne))
            } catch (e: Exception) {
                Log.w(TAG, "Error loading request", e)
            }
        }
        account.setHistoryLoaded()
    }

    fun getNewAccountName(prefix: String): String {
        val accountList = mAccountList
        var name = String.format(prefix, "").trim { it <= ' ' }
        if (accountList.firstOrNull { it.alias == name } == null) {
            return name
        }
        var num = 1
        do {
            num++
            name = String.format(prefix, num).trim { it <= ' ' }
        } while (accountList.firstOrNull { it.alias == name } != null)
        return name
    }

    /**
     * Adds a new Account in the Daemon (also sends an ACCOUNT_ADDED event)
     * Sets the new account as the current one
     *
     * @param map the account details
     * @return the created Account
     */
    fun addAccount(map: Map<String, String>): Observable<Account> =
        Single.fromCallable {
            JamiService.addAccount(StringMap.toSwig(map)).apply {
            if (isEmpty()) throw RuntimeException("Can't create account.") }
        }
        .flatMapObservable { accountId ->
            Observable.merge(observableAccountList.mapOptional { Optional.ofNullable(it.firstOrNull { a -> a.accountId == accountId }) },
                observableAccounts.filter { account: Account -> account.accountId == accountId })
        }
        .subscribeOn(scheduler)

    /**
     * @return the Account from the local cache that matches the accountId
     */
    fun getAccount(accountId: String?): Account? =
        if (!accountId.isNullOrEmpty()) mAccountList.find { accountId == it.accountId } else null

    fun getAccountSingle(accountId: String): Single<Account> = accountsSubject
        .firstOrError()
        .map { accounts -> accounts.first { it.accountId == accountId } }

    val observableAccountList: Observable<List<Account>>
        get() = accountsSubject

    fun getObservableAccountUpdates(accountId: String): Observable<Account> =
        observableAccounts.filter { acc -> acc.accountId == accountId }

    fun getObservableAccountProfile(accountId: String): Observable<Pair<Account, Profile>> =
        getObservableAccount(accountId).flatMap { a: Account ->
            mVCardService.loadProfile(a).map { profile -> Pair(a, profile) }
        }

    fun getObservableAccount(accountId: String): Observable<Account> =
        Observable.fromCallable<Account> { getAccount(accountId)!! }
            .concatWith(getObservableAccountUpdates(accountId))

    fun getObservableAccount(account: Account): Observable<Account> =
        Observable.just(account)
            .concatWith(observableAccounts.filter { acc -> acc === account })

    val currentProfileAccountSubject: Observable<Pair<Account, Profile>>
        get() = currentAccountSubject.flatMap { a: Account ->
            mVCardService.loadProfile(a).map { profile -> Pair(a, profile) }
        }

    fun subscribeBuddy(accountID: String, uri: String, flag: Boolean) {
        mExecutor.execute { JamiService.subscribeBuddy(accountID, uri, flag) }
    }

    /**
     * Send profile through SIP
     */
    fun sendProfile(callId: String, accountId: String) {
        mVCardService.loadSmallVCard(accountId, VCardService.MAX_SIZE_SIP)
            .subscribeOn(Schedulers.computation())
            .observeOn(scheduler)
            .subscribe({ vcard: VCard ->
                var stringVCard = VCardUtils.vcardToString(vcard)!!
                val nbTotal = stringVCard.length / VCARD_CHUNK_SIZE + if (stringVCard.length % VCARD_CHUNK_SIZE != 0) 1 else 0
                var i = 1
                val r = Random(System.currentTimeMillis())
                val key = r.nextInt().absoluteValue
                Log.d(TAG, "sendProfile, vcard $callId")
                while (i <= nbTotal) {
                    Log.d(TAG, "length vcard ${stringVCard.length} id $key part $i nbTotal $nbTotal")
                    val chunk = HashMap<String, String>()
                    val keyHashMap = "${VCardUtils.MIME_PROFILE_VCARD}; id=$key,part=$i,of=$nbTotal"
                    chunk[keyHashMap] = stringVCard.substring(0, min(VCARD_CHUNK_SIZE, stringVCard.length))
                    JamiService.sendTextMessage(accountId, callId, StringMap.toSwig(chunk), "Me", false)
                    if (stringVCard.length > VCARD_CHUNK_SIZE) {
                        stringVCard = stringVCard.substring(VCARD_CHUNK_SIZE)
                    }
                    i++
                }
            }) { e: Throwable -> Log.w(TAG, "Not sending empty profile", e) }
    }

    fun setMessageDisplayed(accountId: String?, conversationUri: Uri, messageId: String) {
        mExecutor.execute { JamiService.setMessageDisplayed(accountId, conversationUri.uri, messageId, 3) }
    }

    fun startConversation(accountId: String, initialMembers: Collection<String>): Single<Conversation> =
        getAccountSingle(accountId).map { account ->
            Log.w(TAG, "startConversation")
            val id = JamiService.startConversation(accountId)
            val conversation = account.getSwarm(id)!!
            for (member in initialMembers) {
                Log.w(TAG, "addConversationMember $member")
                JamiService.addConversationMember(accountId, id, member)
                conversation.addContact(account.getContactFromCache(member))
            }
            account.conversationStarted(conversation)
            Log.w(TAG, "loadConversationMessages")
            conversation
        }.subscribeOn(scheduler)

    fun removeConversation(accountId: String, conversationUri: Uri): Completable =
        Completable.fromAction { JamiService.removeConversation(accountId, conversationUri.rawRingId) }
            .subscribeOn(scheduler)

    private fun loadConversationHistory(accountId: String, conversationUri: Uri, root: String, n: Long) =
        Schedulers.io().scheduleDirect { JamiService.loadConversationMessages(accountId, conversationUri.rawRingId, root, n) }


    fun loadMore(conversation: Conversation, n: Int = 32): Single<Conversation> {
        synchronized(conversation) {
            if (conversation.isLoaded()) {
                Log.w(TAG, "loadMore: conversation already fully loaded")
                return Single.just(conversation)
            }
            val mode = conversation.mode.blockingFirst()
            if (mode == Conversation.Mode.Syncing || mode == Conversation.Mode.Request) {
                Log.w(TAG, "loadMore: conversation is syncing")
                return Single.just(conversation)
            }
            conversation.loading?.let { return it }
            val ret = SingleSubject.create<Conversation>()
            val roots = conversation.swarmRoot
            // Log.w(TAG, "${conversation.accountId} loadMore ${conversation.uri} $mode $roots")
            conversation.loading = ret
            if (roots.isEmpty())
                loadConversationHistory(conversation.accountId, conversation.uri, "", n.toLong())
            else
                for (root in roots)
                    loadConversationHistory(conversation.accountId, conversation.uri, root, n.toLong())
            return ret
        }
    }

    fun loadUntil(conversation: Conversation, from: String = "", until: String = ""): Single<List<Interaction>> {
        val mode = conversation.mode.blockingFirst()
        if (mode == Conversation.Mode.Syncing || mode == Conversation.Mode.Request) {
            Log.w(TAG, "loadUntil: conversation is syncing")
            return Single.just(emptyList())
        }
        return SingleSubject.create<List<Interaction>>().apply {
            loadingTasks[JamiService.loadConversationUntil(conversation.accountId, conversation.uri.rawRingId, from, until)] = this
        }
    }

    fun searchConversation(
        accountId: String,
        conversationUri: Uri,
        query: String = "",
        author: String = "",
        type: String = "",
        lastId: String = "",
        after: Long = 0,
        before: Long = 0,
        maxResult: Long = 0
    ): Observable<ConversationSearchResult> = PublishSubject.create<ConversationSearchResult>().apply {
        conversationSearches[JamiService.searchConversation(
            accountId, conversationUri.rawRingId, author, lastId, query, type, after, before, maxResult, 0)] = this
    }

    fun messagesFound(id: Long, accountId: String, conversationId: String, messages: List<Map<String, String>>) {
        if (conversationId.isEmpty()) {
            conversationSearches.remove(id)?.onComplete()
        } else if (messages.isNotEmpty()) {
            val account = getAccount(accountId) ?: return
            val conversation = account.getSwarm(conversationId) ?: return
            conversationSearches[id]?.onNext(ConversationSearchResult(messages.map { getInteraction(account, conversation, it) }))
        }
    }

    fun sendConversationMessage(accountId: String, conversationUri: Uri, txt: String, replyTo: String?, flag: Int = 0) {
        mExecutor.execute {
            Log.w(TAG, "sendConversationMessage ${conversationUri.rawRingId} $txt $replyTo $flag")
            JamiService.sendMessage(accountId, conversationUri.rawRingId, txt, replyTo ?: "", flag)
        }
    }

    fun deleteConversationMessage(accountId: String, conversationUri: Uri, messageId: String) {
        sendConversationMessage(accountId, conversationUri, "", messageId, 1)
    }
    fun editConversationMessage(accountId: String, conversationUri: Uri, txt: String, messageId: String) {
        sendConversationMessage(accountId, conversationUri, txt, messageId, 1)
    }
    fun sendConversationReaction(accountId: String, conversationUri: Uri, txt: String, replyTo: String) {
        sendConversationMessage(accountId, conversationUri, txt, replyTo, 2)
    }

    /**
     * Sets the order of the accounts in the Daemon
     *
     * @param accountOrder The ordered list of account ids
     */
    private fun setAccountOrder(accountOrder: List<String>) {
        mExecutor.execute {
            val order = StringBuilder()
            for (accountId in accountOrder) {
                order.append(accountId)
                order.append(File.separator)
            }
            JamiService.setAccountsOrder(order.toString())
        }
    }

    /**
     * Sets the account details in the Daemon
     */
    fun setAccountDetails(accountId: String, map: Map<String, String>) {
        Log.i(TAG, "setAccountDetails() $accountId")
        mExecutor.execute { JamiService.setAccountDetails(accountId, StringMap.toSwig(map)) }
    }

    fun migrateAccount(accountId: String, password: String): Single<String> {
        return mMigrationSubject
            .filter { r: MigrationResult -> r.accountId == accountId }
            .map { r: MigrationResult -> r.state }
            .firstOrError()
            .doOnSubscribe {
                val details = getAccount(accountId)!!.details
                details[ConfigKey.ARCHIVE_PASSWORD.key] = password
                mExecutor.execute { JamiService.setAccountDetails(accountId, StringMap.toSwig(details)) }
            }
            .subscribeOn(scheduler)
    }

    fun setAccountEnabled(accountId: String, active: Boolean) {
        mExecutor.execute { JamiService.sendRegister(accountId, active) }
    }

    /**
     * Sets the activation state of the account in the Daemon
     */
    fun setAccountActive(accountId: String, active: Boolean) {
        mExecutor.execute { JamiService.setAccountActive(accountId, active) }
    }

    /**
     * Sets the activation state of all the accounts in the Daemon
     */
    fun setAccountsActive(active: Boolean) {
        mExecutor.execute {
            Log.i(TAG, "setAccountsActive() running... $active")
            for (a in mAccountList) {
                // If the proxy is enabled we can considered the account
                // as always active
                if (a.isDhtProxyEnabled) {
                    JamiService.setAccountActive(a.accountId, true)
                } else {
                    JamiService.setAccountActive(a.accountId, active)
                }
            }
        }
    }

    /**
     * Sets the video activation state of all the accounts in the local cache
     */
    fun setAccountsVideoEnabled(isEnabled: Boolean) {
        for (account in mAccountList) {
            account.setDetail(ConfigKey.VIDEO_ENABLED, isEnabled)
        }
    }

    /**
     * @return the default template (account details) for a type of account
     */
    fun getAccountTemplate(accountType: String): Single<HashMap<String, String>> {
        Log.i(TAG, "getAccountTemplate() $accountType")
        return Single.fromCallable { JamiService.getAccountTemplate(accountType).toNative() }
            .subscribeOn(scheduler)
    }

    /**
     * Removes the account in the Daemon as well as local history
     */
    fun removeAccount(accountId: String) {
        Log.i(TAG, "removeAccount() $accountId")
        mExecutor.execute { JamiService.removeAccount(accountId) }
        mHistoryService.clearHistory(accountId).subscribe()
    }

    /**
     * Exports the account on the DHT (used for multi-devices feature)
     */
    fun exportOnRing(accountId: String, password: String): Single<String> =
        mExportSubject
            .filter { r: ExportOnRingResult -> r.accountId == accountId }
            .firstOrError()
            .map { result: ExportOnRingResult ->
                when (result.code) {
                    PIN_GENERATION_SUCCESS -> return@map result.pin!!
                    PIN_GENERATION_WRONG_PASSWORD -> throw IllegalArgumentException()
                    PIN_GENERATION_NETWORK_ERROR -> throw SocketException()
                    else -> throw UnsupportedOperationException()
                }
            }
            .doOnSubscribe {
                Log.i(TAG, "exportOnRing() $accountId")
                mExecutor.execute { JamiService.exportOnRing(accountId, password) }
            }
            .subscribeOn(Schedulers.io())

    /**
     * @return the list of the account's devices from the Daemon
     */
    fun getKnownRingDevices(accountId: String): Map<String, String> {
        Log.i(TAG, "getKnownRingDevices() $accountId")
        return try {
             mExecutor.submit<HashMap<String, String>> {
                JamiService.getKnownRingDevices(accountId).toNative()
            }.get()
        } catch (e: Exception) {
            Log.e(TAG, "Error running getKnownRingDevices()", e)
            return HashMap()
        }
    }

    /**
     * @param accountId id of the account used with the device
     * @param deviceId  id of the device to revoke
     * @param password  password of the account
     */
    fun revokeDevice(accountId: String, password: String, deviceId: String): Single<Int> =
        mDeviceRevocationSubject
            .filter { r: DeviceRevocationResult -> r.accountId == accountId && r.deviceId == deviceId }
            .firstOrError()
            .map { r: DeviceRevocationResult -> r.code }
            .doOnSubscribe { mExecutor.execute {
                JamiService.revokeDevice(accountId, password, deviceId)
            }}
            .subscribeOn(Schedulers.io())

    /**
     * @param accountId id of the account used with the device
     * @param newName   new device name
     */
    fun renameDevice(accountId: String, newName: String) {
        val account = getAccount(accountId)
        mExecutor.execute {
            Log.i(TAG, "renameDevice() thread running... $newName")
            val details = JamiService.getAccountDetails(accountId)
            details[ConfigKey.ACCOUNT_DEVICE_NAME.key] = newName
            JamiService.setAccountDetails(accountId, details)
            account?.setDetail(ConfigKey.ACCOUNT_DEVICE_NAME, newName)
            account?.devices = JamiService.getKnownRingDevices(accountId).toNative()
        }
    }

    fun exportToFile(accountId: String, absolutePath: String, password: String): Completable =
        Completable.fromAction {
            require(JamiService.exportToFile(accountId, absolutePath, password)) { "Can't export archive" }
        }.subscribeOn(scheduler)

    /**
     * @param accountId   id of the account
     * @param oldPassword old account password
     */
    fun setAccountPassword(accountId: String, oldPassword: String, newPassword: String): Completable =
        Completable.fromAction {
            require(JamiService.changeAccountPassword(accountId, oldPassword, newPassword)) { "Can't change password" }
        }.subscribeOn(scheduler)

    /**
     * Sets the active codecs list of the account in the Daemon
     */
    fun setActiveCodecList(accountId: String, codecs: List<Long>) {
        mExecutor.execute {
            val list = UintVect()
            list.reserve(codecs.size.toLong())
            list.addAll(codecs)
            JamiService.setActiveCodecList(accountId, list)
            observableAccounts.onNext(getAccount(accountId) ?: return@execute)
        }
    }

    /**
     * @return The account's codecs list from the Daemon
     */
    fun getCodecList(accountId: String): Single<List<Codec>> = Single.fromCallable {
        val activePayloads = JamiService.getActiveCodecList(accountId)
        JamiService.getCodecList()
            .map { Codec(it, JamiService.getCodecDetails(accountId, it), activePayloads.contains(it)) }
    }

    fun validateCertificatePath(
        accountID: String,
        certificatePath: String,
        privateKeyPath: String,
        privateKeyPass: String
    ): Map<String, String>? {
        try {
            return mExecutor.submit<HashMap<String, String>> {
                Log.i(TAG, "validateCertificatePath() running...")
                JamiService.validateCertificatePath(accountID, certificatePath, privateKeyPath, privateKeyPass, "").toNative()
            }.get()
        } catch (e: Exception) {
            Log.e(TAG, "Error running validateCertificatePath()", e)
        }
        return null
    }

    fun validateCertificate(accountId: String, certificate: String): Map<String, String>? {
        try {
            return mExecutor.submit<HashMap<String, String>> {
                Log.i(TAG, "validateCertificate() running...")
                JamiService.validateCertificate(accountId, certificate).toNative()
            }.get()
        } catch (e: Exception) {
            Log.e(TAG, "Error running validateCertificate()", e)
        }
        return null
    }

    fun getCertificateDetailsPath(accountId: String, certificatePath: String): Map<String, String>? {
        try {
            return mExecutor.submit<HashMap<String, String>> {
                Log.i(TAG, "getCertificateDetailsPath() running...")
                JamiService.getCertificateDetails(accountId, certificatePath).toNative()
            }.get()
        } catch (e: Exception) {
            Log.e(TAG, "Error running getCertificateDetailsPath()", e)
        }
        return null
    }

    fun getCertificateDetails(accountId: String, certificateRaw: String): Map<String, String>? {
        try {
            return mExecutor.submit<HashMap<String, String>> {
                Log.i(TAG, "getCertificateDetails() running...")
                JamiService.getCertificateDetails(accountId, certificateRaw).toNative()
            }.get()
        } catch (e: Exception) {
            Log.e(TAG, "Error running getCertificateDetails()", e)
        }
        return null
    }

    /**
     * @return the supported TLS methods from the Daemon
     */
    val tlsSupportedMethods: List<String>
        get() {
            Log.i(TAG, "getTlsSupportedMethods()")
            return SwigNativeConverter.toJava(JamiService.getSupportedTlsMethod())
        }

    /**
     * @return the account's credentials from the Daemon
     */
    fun getCredentials(accountId: String): List<Map<String, String>>? {
        try {
            return mExecutor.submit<ArrayList<Map<String, String>>> {
                Log.i(TAG, "getCredentials() running...")
                JamiService.getCredentials(accountId).toNative()
            }.get()
        } catch (e: Exception) {
            Log.e(TAG, "Error running getCredentials()", e)
        }
        return null
    }

    /**
     * Sets the account's credentials in the Daemon
     */
    fun setCredentials(accountId: String, credentials: List<Map<String, String>>) {
        Log.i(TAG, "setCredentials() $accountId")
        mExecutor.execute { JamiService.setCredentials(accountId, SwigNativeConverter.toSwig(credentials)) }
    }

    /**
     * Sets the registration state to true for all the accounts in the Daemon
     */
    fun registerAllAccounts() {
        Log.i(TAG, "registerAllAccounts()")
        mExecutor.execute { registerAllAccounts() }
    }

    /**
     * Registers a new name on the blockchain for the account
     */
    fun registerName(account: Account, password: String?, name: String) {
        if (account.registeringUsername) {
            Log.w(TAG, "Already trying to register username")
            return
        }
        account.registeringUsername = true
        registerName(account.accountId, password ?: "", name)
    }

    /**
     * Register a new name on the blockchain for the account Id
     */
    fun registerName(account: String, password: String, name: String) {
        Log.i(TAG, "registerName()")
        mExecutor.execute { JamiService.registerName(account, password, name) }
    }
    /* contact requests */
    /**
     * @return all trust requests from the daemon for the account Id
     */
    fun getTrustRequests(accountId: String): List<Map<String, String>>? {
        try {
            return mExecutor.submit<ArrayList<Map<String, String>>> {
                JamiService.getTrustRequests(accountId).toNative()
            }.get()
        } catch (e: Exception) {
            Log.e(TAG, "Error running getTrustRequests()", e)
        }
        return null
    }

    /**
     * Accepts a pending trust request
     */
    fun acceptTrustRequest(accountId: String, from: Uri) {
        Log.i(TAG, "acceptRequest() $accountId $from")
        mExecutor.execute {
            if (from.isSwarm)
                JamiService.acceptConversationRequest(accountId, from.rawRingId)
            else {
                JamiService.acceptTrustRequest(accountId, from.rawRingId)
                /*getAccount(accountId)?.let { account -> account.getRequest(from)?.vCard?.let{ vcard ->
                    VCardUtils.savePeerProfileToDisk(vcard, accountId, from.rawRingId + ".vcf", mDeviceRuntimeService.provideFilesDir())
                }}*/
            }
        }
    }

    /**
     * Refuses and blocks a pending trust request
     */
    fun discardTrustRequest(accountId: String, contactUri: Uri): Boolean =
        if (contactUri.isSwarm)  {
            JamiService.declineConversationRequest(accountId, contactUri.rawRingId)
            true
        } else {
            val account = getAccount(accountId)
            var removed = false
            if (account != null) {
                removed = account.removeRequest(contactUri) != null
                mHistoryService.clearHistory(contactUri.rawRingId, accountId, true).subscribe()
            }
            mExecutor.execute { JamiService.discardTrustRequest(accountId, contactUri.rawRingId) }
            removed
        }

    /**
     * Sends a new trust request
     */
    fun sendTrustRequest(conversation: Conversation, to: Uri, message: Blob = Blob()) {
        Log.i(TAG, "sendTrustRequest() " + conversation.accountId + " " + to)
        mExecutor.execute { JamiService.sendTrustRequest(conversation.accountId, to.rawRingId, message) }
    }

    /**
     * Add a new contact for the account Id on the Daemon
     */
    fun addContact(accountId: String, uri: String) {
        Log.i(TAG, "addContact() $accountId $uri")
        mExecutor.execute { JamiService.addContact(accountId, uri) }
    }

    /**
     * Remove an existing contact for the account Id on the Daemon
     */
    fun removeContact(accountId: String, uri: String, ban: Boolean) {
        Log.i(TAG, "removeContact() $accountId $uri ban:$ban")
        mExecutor.execute { JamiService.removeContact(accountId, uri, ban) }
    }

    fun findRegistrationByName(account: String, nameserver: String, name: String): Single<RegisteredName> =
        if (name.isEmpty())
            Single.just(RegisteredName(account, name))
        else registeredNames
            .filter { r: RegisteredName -> account == r.accountId && name == r.name }
            .firstOrError()
            .doOnSubscribe {
                mExecutor.execute { JamiService.lookupName(account, nameserver, name) }
            }
            .subscribeOn(scheduler)

    fun findRegistrationByAddress(account: String, nameserver: String, address: String): Single<RegisteredName> =
        if (address.isEmpty())
            Single.error(IllegalArgumentException())
        else registeredNames
            .filter { r: RegisteredName -> account == r.accountId && address == r.address }
            .firstOrError()
            .doOnSubscribe {
                mExecutor.execute { JamiService.lookupAddress(account, nameserver, address) }
            }
            .subscribeOn(scheduler)

    fun searchUser(account: String, query: String): Single<UserSearchResult> {
        if (query.isEmpty()) {
            return Single.just(UserSearchResult(account, query))
        }
        val encodedUrl: String = try {
            URLEncoder.encode(query, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            return Single.error(e)
        }
        return searchResults
            .filter { r: UserSearchResult -> account == r.accountId && encodedUrl == r.query }
            .firstOrError()
            .doOnSubscribe {
                mExecutor.execute { JamiService.searchUser(account, encodedUrl) }
            }
            .subscribeOn(scheduler)
    }

    /**
     * Reverse looks up the address in the blockchain to find the name
     */
    fun lookupAddress(account: String, nameserver: String, address: String) {
        //Log.w(TAG, "lookupAddress $address")
        mExecutor.execute { JamiService.lookupAddress(account, nameserver, address) }
    }

    fun pushNotificationReceived(from: String, data: Map<String, String>) {
        // Log.i(TAG, "pushNotificationReceived() $data");
        mExecutor.execute { JamiService.pushNotificationReceived(from, StringMap.toSwig(data)) }
    }

    fun setPushNotificationToken(pushNotificationToken: String) {
        Log.i(TAG, "setPushNotificationToken()");
        mExecutor.execute { JamiService.setPushNotificationToken(pushNotificationToken) }
    }
    fun setPushNotificationConfig(token: String = "", topic: String = "", platform: String = "") {
        Log.i(TAG, "setPushNotificationConfig() $token $topic $platform");
        mExecutor.execute { JamiService.setPushNotificationConfig(StringMap().apply {
            put("token", token)
            put("topic", topic)
            put("platform", platform)
        }) }
    }

    fun volumeChanged(device: String, value: Int) {
        Log.w(TAG, "volumeChanged $device $value")
    }

    fun accountsChanged() {
        // Accounts have changed in Daemon, we have to update our local cache
        refreshAccountsCacheFromDaemon()
    }

    fun stunStatusFailure(accountId: String) {
        Log.d(TAG, "stun status failure: $accountId")
    }

    fun registrationStateChanged(accountId: String, newState: String, code: Int, detailString: String?) {
        Log.d(TAG, "registrationStateChanged: $accountId, $newState, $code, $detailString")
        val account = getAccount(accountId) ?: return
        val state = AccountConfig.RegistrationState.valueOf(newState)
        val oldState = account.registrationState
        if (oldState == AccountConfig.RegistrationState.INITIALIZING && state != AccountConfig.RegistrationState.INITIALIZING) {
            account.setDetails(JamiService.getAccountDetails(account.accountId).toNative())
            account.setCredentials(JamiService.getCredentials(account.accountId).toNative())
            account.devices = JamiService.getKnownRingDevices(account.accountId).toNative()
            account.setVolatileDetails(JamiService.getVolatileAccountDetails(account.accountId).toNative())
        } else {
            account.setRegistrationState(state, code)
            /*synchronized(account) {
                if (!account.loadedSubject.hasValue()) {
                    account.loadedSubject.onSuccess(Completable.fromAction { loadAccount(account) }
                        .subscribeOn(Schedulers.computation()))
                }
            }*/
        }
        if (oldState != state) {
            observableAccounts.onNext(account)
        }
    }

    fun accountDetailsChanged(accountId: String, details: Map<String, String>) {
        val account = getAccount(accountId) ?: return
        Log.d(TAG, "accountDetailsChanged: $accountId ${details.size}")
        account.setDetails(details)
        observableAccounts.onNext(account)
    }

    fun volatileAccountDetailsChanged(accountId: String, details: Map<String, String>) {
        val account = getAccount(accountId) ?: return
        //Log.d(TAG, "volatileAccountDetailsChanged: " + accountId + " " + details.size());
        account.setVolatileDetails(details)
        observableAccounts.onNext(account)
    }

    fun activeCallsChanged(
        accountId: String,
        conversationId: String,
        activeCalls: List<Map<String, String>>,
    ) = getAccount(accountId)?.setActiveCalls(conversationId, activeCalls)

    fun accountProfileReceived(accountId: String, name: String?, photo: String?) {
        val account = getAccount(accountId) ?: return
        mVCardService.saveVCardProfile(accountId, account.uri, name, photo)
            .subscribeOn(Schedulers.io())
            .subscribe({ vcard -> account.loadedProfile = mVCardService.loadVCardProfile(vcard).cache() })
                { e -> Log.e(TAG, "Error saving profile", e) }
    }

    fun profileReceived(accountId: String, peerId: String, vcardPath: String) {
        val account = getAccount(accountId) ?: return
        Log.w(TAG, "profileReceived: $accountId, $peerId, $vcardPath")
        val contact = account.getContactFromCache(peerId)
        if (contact.isUser) {
            mVCardService.accountProfileReceived(accountId, File(vcardPath))
                .subscribe({ profile: Profile ->
                    account.loadedProfile = Single.just(profile)
                }) { e -> Log.e(TAG, "Error saving contact profile", e) }
        } else {
            mVCardService.peerProfileReceived(accountId, peerId, File(vcardPath))
                .subscribe({ profile -> contact.setProfile(profile) })
                    { e -> Log.e(TAG, "Error saving contact profile", e) }
            //contact.setProfile(mVCardService.peerProfileReceived(accountId, peerId, File(vcardPath)))
        }
    }

    fun incomingAccountMessage(accountId: String, messageId: String?, callId: String?, from: String, messages: Map<String, String>) {
        Log.d(TAG, "incomingAccountMessage ---> accountId: $accountId , messageId: $messageId, from: $from, messages.size: ${messages.size} messages: $messages")
        incomingMessageSubject.onNext(Message(accountId, messageId, callId, from, messages))
    }

    fun accountMessageStatusChanged(accountId: String, conversationId: String, messageId: String, contactId: String, status: Int) {
        val newStatus = InteractionStatus.fromIntTextMessage(status)
        Log.d(TAG, "accountMessageStatusChanged: $accountId, $conversationId, $messageId, $contactId, $newStatus")
        val account = getAccount(accountId) ?: return
        if (conversationId.isEmpty() && !account.isJami) {
            mHistoryService
                .accountMessageStatusChanged(accountId, messageId, contactId, newStatus)
                .subscribe({ t: TextMessage -> messageSubject.onNext(t) }) { e: Throwable ->
                    Log.e(TAG, "Error updating message: " + e.localizedMessage) }
        } else {
            account.getSwarm(conversationId)
                ?.updateSwarmInteraction(messageId, Uri.fromId(contactId), newStatus)
        }
    }

    fun composingStatusChanged(accountId: String, conversationId: String, contactUri: String, status: Int) {
        Log.d(TAG, "composingStatusChanged: $accountId, $contactUri, $conversationId, $status")
        getAccount(accountId)?.composingStatusChanged(conversationId, Uri.fromId(contactUri), Account.ComposingStatus.fromInt(status))
    }

    fun errorAlert(alert: Int) {
        Log.d(TAG, "errorAlert : $alert")
    }

    fun knownDevicesChanged(accountId: String, devices: Map<String, String>) {
        getAccount(accountId)?.let { account ->
            account.devices = devices
            observableAccounts.onNext(account)
        }
    }

    fun exportOnRingEnded(accountId: String, code: Int, pin: String) {
        Log.d(TAG, "exportOnRingEnded: $accountId, $code, $pin")
        mExportSubject.onNext(ExportOnRingResult(accountId, code, pin))
    }

    fun nameRegistrationEnded(accountId: String, state: Int, name: String) {
        Log.d(TAG, "nameRegistrationEnded: $accountId, $state, $name")
        val acc = getAccount(accountId)
        if (acc == null) {
            Log.w(TAG, "Can't find account for name registration callback")
            return
        }
        acc.registeringUsername = false
        acc.setVolatileDetails(JamiService.getVolatileAccountDetails(acc.accountId).toNative())
        if (state == 0) {
            acc.setDetail(ConfigKey.ACCOUNT_REGISTERED_NAME, name)
        }
        observableAccounts.onNext(acc)
    }

    fun migrationEnded(accountId: String, state: String) {
        Log.d(TAG, "migrationEnded: $accountId, $state")
        mMigrationSubject.onNext(MigrationResult(accountId, state))
    }

    fun deviceRevocationEnded(accountId: String, device: String, state: Int) {
        Log.d(TAG, "deviceRevocationEnded: $accountId, $device, $state")
        if (state == 0) {
            getAccount(accountId)?.let { account ->
                val devices = HashMap(account.devices)
                devices.remove(device)
                account.devices = devices
                observableAccounts.onNext(account)
            }
        }
        mDeviceRevocationSubject.onNext(DeviceRevocationResult(accountId, device, state))
    }

    fun setConversationPreferences(accountId: String, conversationId: String, info: Map<String, String>) {
        JamiService.setConversationPreferences(accountId, conversationId, StringMap.toSwig(info))
    }

    /**
     * Daemon interface to get conversation preferences (color, emoji, etc.)
     * @param accountId account id
     * @param conversationId conversation id
     * @return map of preferences
     */
    fun getConversationPreferences(accountId: String, conversationId: String): Map<String, String> {
        return JamiService.getConversationPreferences(accountId, conversationId)
    }

    fun updateConversationInfo(accountId: String, conversationId: String, info: Map<String, String>) {
        JamiService.updateConversationInfos(accountId, conversationId, StringMap.toSwig(info))
    }
    fun addConversationMembers(accountId: String, conversationId: String, uris: List<String>) {
        mExecutor.execute {
            for (uri in uris)
                JamiService.addConversationMember(accountId, conversationId, uri)
        }
    }

    fun addConversationMember(accountId: String, conversationId: String, uri: String) {
        // mExecutor.execute {
        JamiService.addConversationMember(accountId, conversationId, uri)
    }
    fun removeConversationMember(accountId: String, conversationId: String, uri: String) {
        // mExecutor.execute {
        JamiService.removeConversationMember(accountId, conversationId, uri)
    }

    fun contactAdded(accountId: String, uri: String, confirmed: Boolean) {
        getAccount(accountId)?.let { account ->
            val details: Map<String, String> = JamiService.getContactDetails(accountId, uri)
            val contact = account.addContact(details)
            val conversationUri = contact.conversationUri.blockingFirst()
            if (conversationUri.isSwarm) {
                var conversation = account.getByUri(conversationUri)
                if (conversation == null) {
                    conversation = account.newSwarm(conversationUri.rawRingId, Conversation.Mode.Syncing)
                    conversation.addContact(contact)
                }
            }
            //account.addContact(uri, confirmed);
            if (account.isEnabled) lookupAddress(accountId, "", uri)
        }
    }

    fun contactRemoved(accountId: String, uri: String, banned: Boolean) {
        Log.d(TAG, "Contact removed: $uri User is banned: $banned")
        getAccount(accountId)?.let { account ->
            mHistoryService.clearHistory(uri, accountId, true).subscribe()
            account.removeContact(uri, banned)
        }
    }

    fun registeredNameFound(accountId: String, state: Int, address: String, name: String) {
        try {
            registeredNameSubject.onNext(RegisteredName(accountId, name, address, state))
        } catch (e: Exception) {
            Log.w(TAG, "registeredNameFound exception", e)
        }
    }

    fun userSearchEnded(accountId: String, state: Int, query: String, results: List<Map<String, String>>) {
        val account = getAccount(accountId) ?: return
        val r = UserSearchResult(accountId, query, state)
        r.results = results.map { m ->
            val uri = m["id"]!!
            account.getContactFromCache(uri).apply {
                synchronized(this) {
                    m["username"]?.let { name ->
                        if (this.username == null)
                            this.username = Single.just(name)
                    }
                    setProfile(Single.fromCallable {
                        val firstName = m["firstName"]
                        val lastName = m["lastName"]
                        val profilePicture = m["profilePicture"]
                        Profile("$firstName $lastName", mVCardService.base64ToBitmap(profilePicture))
                    }.cache())
                }
            }
        }
        searchResultSubject.onNext(r)
    }

    /**
     * Retrieves the Interaction object from the given account, conversation, and message data.
     *
     * @param account      The account associated with the interaction.
     * @param conversation The conversation in which the interaction occurred.
     * @param message      The message data containing information about the interaction.
     * @return The Interaction object representing the interaction.
     */
    private fun getInteraction(
        account: Account, conversation: Conversation, message: Map<String, String>
    ): Interaction {
        val id = message["id"]!!
        val type = message["type"]!!
        val author = message["author"]!!
        val parent = message["linearizedParent"]
        val authorUri = Uri.fromId(author)
        val timestamp = message["timestamp"]!!.toLong() * 1000
        val replyTo = message["reply-to"]
        val reactTo = message["react-to"]
        val edit = message["edit"]
        val contact = conversation.findContact(authorUri) ?: account.getContactFromCache(authorUri)
        val interaction: Interaction = when (type) {
            "initial" -> if (conversation.mode.blockingFirst() == Conversation.Mode.OneToOne) {
                val invited = message["invited"]!!
                val invitedContact = conversation.findContact(Uri.fromId(invited)) ?: account.getContactFromCache(invited)
                invitedContact.addedDate = Date(timestamp)
                Log.w(TAG, "invited $invited $invitedContact")
                ContactEvent(account.accountId, invitedContact).setEvent(ContactEvent.Event.INVITED)
            } else {
                Interaction(conversation, Interaction.InteractionType.INVALID)
            }
            "member" -> {
                val action = message["action"]!!
                val uri = message["uri"]!!
                Log.w(TAG, "member $action $uri")
                val member = conversation.findContact(Uri.fromId(uri)) ?: account.getContactFromCache(uri)
                member.addedDate = Date(timestamp)
                ContactEvent(account.accountId, member).setEvent(ContactEvent.Event.fromConversationAction(action))
            }
            "application/edited-message",
            "text/plain" -> TextMessage(author, account.accountId, timestamp, conversation, message["body"]!!, !contact.isUser)
            "application/data-transfer+json" -> {
                try {
                    val fileName = message["displayName"]!!
                    val fileId = message["fileId"]
                    val paths = arrayOfNulls<String>(1)
                    val progressA = LongArray(1)
                    val totalA = LongArray(1)
                    JamiService.fileTransferInfo(account.accountId, conversation.uri.rawRingId, fileId, paths, totalA, progressA)
                    if (totalA[0] == 0L) {
                        totalA[0] = message["totalSize"]!!.toLong()
                    }
                    val path = File(paths[0]!!)
                    val isComplete = path.exists() && progressA[0] == totalA[0]
                    Log.w(TAG, "add $isComplete DataTransfer at ${paths[0]} with progress ${progressA[0]}/${totalA[0]}")
                    DataTransfer(fileId, account.accountId, author, fileName, contact.isUser, timestamp, totalA[0], progressA[0]).apply {
                        daemonPath = path
                        status = if (isComplete) InteractionStatus.TRANSFER_FINISHED else InteractionStatus.FILE_AVAILABLE
                    }
                } catch (e: Exception) {
                    Interaction(conversation, Interaction.InteractionType.INVALID)
                }
            }
            "application/call-history+json" -> {
                val callDirection =
                    if (contact.isUser) Call.Direction.OUTGOING else Call.Direction.INCOMING
                Call(null, account.accountId, authorUri.rawUriString, callDirection, timestamp)
                    .apply {
                        message["duration"]?.let { d -> duration = d.toLong() }
                        message["confId"]?.let { c -> confId = c }
                    }
            }
            "application/update-profile" -> Interaction(conversation, Interaction.InteractionType.INVALID)
            "merge" -> Interaction(conversation, Interaction.InteractionType.INVALID)
            else -> Interaction(conversation, Interaction.InteractionType.INVALID)
        }
        interaction.replyToId = replyTo
        interaction.reactToId = reactTo
        interaction.edit = edit
        if (replyTo != null) {
            interaction.replyTo = conversation.loadMessage(replyTo) {
                JamiService.loadConversationUntil(account.accountId, conversation.uri.rawRingId, id, replyTo)
            }
        }
        if (interaction.contact == null)
            interaction.contact = contact
        interaction.setSwarmInfo(conversation.uri.rawRingId, id, if (parent.isNullOrEmpty()) null else parent)
        interaction.conversation = conversation
        return interaction
    }

    private fun addMessage(account: Account, conversation: Conversation, message: Map<String, String>, newMessage: Boolean): Interaction {
        val interaction = getInteraction(account, conversation, message)
        if (conversation.addSwarmElement(interaction, newMessage)) {
            /*if (conversation.isVisible)
                mHistoryService.setMessageRead(account.accountID, conversation.uri, interaction.messageId!!)*/
        }
        return interaction
    }

    fun conversationLoaded(id: Long, accountId: String, conversationId: String, messages: List<Map<String, String>>) {
        try {
            val task = loadingTasks.remove(id)
            // Log.w(TAG, "ConversationCallback: conversationLoaded $accountId/$conversationId ${messages.size}")
            getAccount(accountId)?.let { account -> account.getSwarm(conversationId)?.let { conversation ->
                val interactions: List<Interaction>
                synchronized(conversation) {
                    interactions = messages.map { addMessage(account, conversation, it, false) }
                    conversation.stopLoading()
                }
                task?.onSuccess(interactions)
                account.conversationChanged()
            }}
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading message", e)
        }
    }

    fun conversationProfileUpdated(accountId: String, conversationId: String, info: StringMap) {
        getAccount(accountId)?.getSwarm(conversationId)?.setProfile(mVCardService.loadConversationProfile(info.toNativeFromUtf8()))
    }

    fun conversationPreferencesUpdated(accountId: String, conversationId: String, preferences: StringMap) {
        getAccount(accountId)?.getSwarm(conversationId)?.updatePreferences(preferences)
    }

    private enum class ConversationMemberEvent {
        Add, Join, Remove, Ban
    }

    fun conversationMemberEvent(accountId: String, conversationId: String, peerUri: String, event: Int) {
        Log.w(TAG, "ConversationCallback: conversationMemberEvent $accountId/$conversationId")
        getAccount(accountId)?.let { account -> account.getSwarm(conversationId)?.let { conversation ->
            val uri = Uri.fromId(peerUri)
            when (ConversationMemberEvent.values()[event]) {
                ConversationMemberEvent.Add, ConversationMemberEvent.Join -> {
                    val contact = conversation.findContact(uri)
                    if (contact == null) {
                        conversation.addContact(account.getContactFromCache(uri))
                    }
                }
                ConversationMemberEvent.Remove, ConversationMemberEvent.Ban -> {
                    if (conversation.mode.blockingFirst() != Conversation.Mode.OneToOne) {
                        conversation.findContact(uri)?.let { contact -> conversation.removeContact(contact) }
                    }
                }
            }
        }}
    }

    fun conversationReady(accountId: String, conversationId: String) {
        Log.w(TAG, "ConversationCallback: conversationReady $accountId/$conversationId")
        val account = getAccount(accountId)
        if (account == null) {
            Log.w(TAG, "conversationReady: can't find account")
            return
        }
        val info = JamiService.conversationInfos(accountId, conversationId).toNativeFromUtf8()
        /*for (Map.Entry<String, String> i : info.entrySet()) {
            Log.w(TAG, "conversation info: " + i.getKey() + " " + i.getValue());
        }*/
        val mode = Conversation.Mode.values()[info["mode"]!!.toInt()]
        val uri = Uri(Uri.SWARM_SCHEME, conversationId)
        var c = account.getByUri(uri)//getSwarm(conversationId) ?: account.getByUri(Uri(Uri.SWARM_SCHEME, conversationId))
        var setMode = false
        if (c == null) {
            c = account.newSwarm(conversationId, mode).apply {
                setLastMessageNotified(mHistoryService.getLastMessageNotified(accountId, uri))
            }
        } else {
            c.loaded = null
            c.clearHistory(true)
            setMode = mode != c.mode.blockingFirst()
        }
        val conversation = c
        synchronized(conversation) {
            conversation.setProfile(mVCardService.loadConversationProfile(info))
            // Making sure to add contacts before changing the mode
            for (member in JamiService.getConversationMembers(accountId, conversationId)) {
                val memberUri = Uri.fromId(member["uri"]!!)
                var contact = conversation.findContact(memberUri)
                if (contact == null) {
                    contact = account.getContactFromCache(memberUri)
                    conversation.addContact(contact)
                }
            }
            if (!conversation.lastElementLoadedSubject.hasValue())
                conversation.lastElementLoadedSubject.onSuccess(loadMore(conversation, 8).ignoreElement().cache())
            if (setMode)
                conversation.setMode(mode)
        }
        account.conversationStarted(conversation)
        loadMore(conversation, 2)
    }

    fun conversationRemoved(accountId: String, conversationId: String) {
        val account = getAccount(accountId)
        if (account == null) {
            Log.w(TAG, "conversationRemoved: can't find account")
            return
        }
        account.removeSwarm(conversationId)
    }

    fun conversationRequestDeclined(accountId: String, conversationId: String) {
        Log.d(TAG, "conversation request for $conversationId is declined")
        getAccount(accountId)?.removeRequest(Uri(Uri.SWARM_SCHEME, conversationId))
    }

    fun conversationRequestReceived(accountId: String, conversationId: String, metadata: Map<String, String>) {
        Log.w(TAG, "ConversationCallback: conversationRequestReceived $accountId/$conversationId ${metadata.size}")
        val account = getAccount(accountId)
        if (account == null || conversationId.isEmpty()) {
            Log.w(TAG, "conversationRequestReceived: can't find account")
            return
        }
        val from = Uri.fromId(metadata["from"]!!)
        account.addRequest(TrustRequest(
            account.accountId,
            from,
            metadata["received"]!!.toLong() * 1000L,
            Uri(Uri.SWARM_SCHEME, conversationId),
            mVCardService.loadConversationProfile(metadata),
            metadata["mode"]?.let { m -> Conversation.Mode.values()[m.toInt()] } ?: Conversation.Mode.OneToOne))
    }

    fun messageReceived(accountId: String, conversationId: String, message: Map<String, String>) {
        Log.w(TAG, "ConversationCallback: messageReceived " + accountId + "/" + conversationId + " " + message.size)
        getAccount(accountId)?.let { account -> account.getSwarm(conversationId)?.let { conversation ->
            synchronized(conversation) {
                val interaction = addMessage(account, conversation, message, true)
                account.conversationUpdated(conversation)
                val isIncoming = !interaction.contact!!.isUser
                if (isIncoming)
                    incomingSwarmMessageSubject.onNext(interaction)
                if (interaction is DataTransfer)
                    dataTransfers.onNext(interaction)
                if (interaction is Call && interaction.isGroupCall && isIncoming)
                    incomingGroupCallSubject.onNext(conversation)
            }
        }}
    }

    fun sendFile(conversation: Conversation, file: File) {
        mExecutor.execute { JamiService.sendFile(conversation.accountId, conversation.uri.rawRingId,file.absolutePath, file.name, "") }
    }

    fun acceptFileTransfer(accountId: String, conversationUri: Uri, messageId: String?, fileId: String) {
        getAccount(accountId)?.let { account -> account.getByUri(conversationUri)?.let { conversation ->
            val transfer = if (conversation.isSwarm)
                conversation.getMessage(messageId!!) as DataTransfer?
            else
                account.getDataTransfer(fileId)
            acceptFileTransfer(conversation, fileId, transfer!!)
        }}
    }

    fun acceptFileTransfer(conversation: Conversation, fileId: String, transfer: DataTransfer) {
        if (conversation.isSwarm) {
            val conversationId = conversation.uri.rawRingId
            val newPath = mDeviceRuntimeService.getNewConversationPath(conversation.accountId, conversationId, transfer.displayName)
            Log.i(TAG, "downloadFile() id=" + conversation.accountId + ", path=" + conversationId + " " + fileId + " to -> " + newPath.absolutePath)
            JamiService.downloadFile(conversation.accountId, conversationId, transfer.messageId, fileId, newPath.absolutePath)
        }
    }

    fun cancelDataTransfer(accountId: String, conversationId: String, messageId: String?, fileId: String) {
        Log.i(TAG, "cancelDataTransfer() id=$fileId")
        mExecutor.execute { JamiService.cancelDataTransfer(accountId, conversationId, fileId) }
    }

    private inner class DataTransferRefreshTask(
        private val account: Account,
        private val conversation: Conversation,
        private val toUpdate: DataTransfer
    ) : Runnable {
        val scheduledTask: ScheduledFuture<*> = mExecutor.scheduleAtFixedRate(
            this,
            DATA_TRANSFER_REFRESH_PERIOD,
            DATA_TRANSFER_REFRESH_PERIOD, TimeUnit.MILLISECONDS
        )
        override fun run() {
            synchronized(toUpdate) {
                if (toUpdate.status == InteractionStatus.TRANSFER_ONGOING) {
                    dataTransferEvent(account, conversation, toUpdate.messageId, toUpdate.fileId!!, 5)
                } else {
                    scheduledTask.cancel(false)
                }
            }
        }
    }

    fun dataTransferEvent(accountId: String, conversationId: String, interactionId: String, fileId: String, eventCode: Int) {
        val account = getAccount(accountId)
        if (account != null) {
            val conversation = account.getSwarm(conversationId) ?: return
            dataTransferEvent(account, conversation, interactionId, fileId, eventCode)
        }
    }

    fun dataTransferEvent(account: Account, conversation: Conversation, interactionId: String?, fileId: String, eventCode: Int) {
        val transferStatus = InteractionStatus.fromIntFile(eventCode)
        Log.d(TAG, "Data Transfer $interactionId $fileId $transferStatus")
        val transfer = account.getDataTransfer(fileId) ?: conversation.getMessage(interactionId!!) as DataTransfer? ?: return
        val paths = arrayOfNulls<String>(1)
        val progressA = LongArray(1)
        val totalA = LongArray(1)
        JamiService.fileTransferInfo(account.accountId, conversation.uri.rawRingId, fileId, paths, totalA, progressA)
        val progress = progressA[0]
        val total = totalA[0]
        synchronized(transfer) {
            val oldState = transfer.status
            transfer.conversation = conversation
            transfer.daemonPath = File(paths[0]!!)
            transfer.status = transferStatus
            transfer.bytesProgress = progress
            if (oldState != transferStatus) {
                if (transferStatus == InteractionStatus.TRANSFER_ONGOING) {
                    DataTransferRefreshTask(account, conversation, transfer)
                } else if (transferStatus.isError) {
                    if (!transfer.isOutgoing) {
                        val tmpPath = mDeviceRuntimeService.getTemporaryPath(
                            conversation.uri.rawRingId, transfer.storagePath
                        )
                        tmpPath.delete()
                    }
                }
            }
        }
        Log.d(TAG, "Data Transfer dataTransferSubject.onNext")
        dataTransfers.onNext(transfer)
    }

    fun setProxyEnabled(enabled: Boolean) {
        mExecutor.execute {
            for (acc in mAccountList) {
                if (acc.isJami && acc.isDhtProxyEnabled != enabled) {
                    Log.d(TAG, (if (enabled) "Enabling" else "Disabling") + " proxy for account " + acc.accountId)
                    acc.isDhtProxyEnabled = enabled
                    val details = JamiService.getAccountDetails(acc.accountId)
                    details[ConfigKey.PROXY_ENABLED.key] = if (enabled) "true" else "false"
                    JamiService.setAccountDetails(acc.accountId, details)
                }
            }
        }
    }

    companion object {
        private val TAG = AccountService::class.java.simpleName
        private const val VCARD_CHUNK_SIZE = 1000
        private const val DATA_TRANSFER_REFRESH_PERIOD: Long = 500
        private const val PIN_GENERATION_SUCCESS = 0
        private const val PIN_GENERATION_WRONG_PASSWORD = 1
        private const val PIN_GENERATION_NETWORK_ERROR = 2

        private fun getDataTransferError(errorCode: Long): DataTransferError = try {
            DataTransferError.values()[errorCode.toInt()]
        } catch (ignored: ArrayIndexOutOfBoundsException) {
            Log.e(TAG, "getDataTransferError: invalid data transfer error from daemon")
            DataTransferError.UNKNOWN
        }
    }
}