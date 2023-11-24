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
package net.jami.model

import io.reactivex.rxjava3.core.*
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.CompletableSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import net.jami.model.Interaction.InteractionStatus
import net.jami.services.AccountService
import net.jami.utils.Log
import java.util.Collections
import java.util.Date
import kotlin.collections.ArrayList

class Account(
    val accountId: String,
    details: Map<String, String>,
    credentials: List<Map<String, String>>,
    volDetails: Map<String, String>
) {
    private var mVolatileDetails: AccountConfig = AccountConfig(volDetails)
    var config: AccountConfig = AccountConfig(details)
        private set
    var username: String? = config[ConfigKey.ACCOUNT_USERNAME]
        private set
    val credentials = credentials.mapTo(ArrayList()) { AccountCredentials(it) }
    var devices: Map<String, String> = HashMap()
    private val mContacts: MutableMap<String, Contact> = HashMap()
    private val mContactCache: MutableMap<String, Contact> = HashMap()
    private val swarmConversations: MutableMap<String, Conversation> = HashMap()
    private val mDataTransfers = HashMap<String, DataTransfer>()
    private val conversations: MutableMap<String, Conversation> = HashMap()
    private val pending: MutableMap<String, Conversation> = HashMap()
    private val cache: MutableMap<String, Conversation> = HashMap()
    private val sortedConversations: MutableList<Conversation> = ArrayList()
    private val sortedPending: MutableList<Conversation> = ArrayList()
    var registeringUsername = false
    val loadedSubject = CompletableSubject.create()
    val loaded: Completable
        get() = loadedSubject
    private var conversationsChanged = true
    private var pendingsChanged = true
    private var historyLoaded = false
    private val conversationSubject: Subject<Conversation> = PublishSubject.create()
    private val pendingSubject: Subject<List<Conversation>> = BehaviorSubject.create()
    private val conversationsSubject: Subject<List<Conversation>> = BehaviorSubject.create()
    private val contactListSubject = BehaviorSubject.create<Collection<Contact>>()
    private val contactLocations: MutableMap<Contact, Observable<ContactLocation>> = HashMap()
    private val mLocationSubject: Subject<Map<Contact, Observable<ContactLocation>>> = BehaviorSubject.createDefault(contactLocations)
    private val mLocationStartedSubject: Subject<ContactLocationEntry> = PublishSubject.create()
    private val registrationStateSubject = BehaviorSubject.createDefault(AccountConfig.RegistrationState.valueOf(mVolatileDetails[ConfigKey.ACCOUNT_REGISTRATION_STATUS]))

    var historyLoader: Single<Account>? = null
    var loadedProfile: Single<Profile>? = null
        set(profile) {
            field = profile
            if  (profile != null)
                mProfileSubject.onNext(profile)
        }

    private val mProfileSubject: Subject<Single<Profile>> = BehaviorSubject.create()
    val loadedProfileObservable: Observable<Profile> = mProfileSubject.switchMapSingle { single -> single }

    fun cleanup() {
        conversationSubject.onComplete()
        conversationsSubject.onComplete()
        pendingSubject.onComplete()
        contactListSubject.onComplete()
        //trustRequestsSubject.onComplete();
    }

    fun canSearch(): Boolean = getDetail(ConfigKey.MANAGER_URI).isNotEmpty()

    fun isContact(conversation: Conversation): Boolean =
        conversation.contact?.let { isContact(it.uri) } ?: false

    fun isContact(uri: Uri): Boolean = getContact(uri) != null

    fun conversationStarted(conversation: Conversation) {
        //Log.w(TAG, "conversationStarted ${conversation.accountId} ${conversation.uri} ${conversation.isSwarm} ${conversation.contacts.size} ${conversation.mode.blockingFirst()}")
        synchronized(conversations) {
            if (conversation.isSwarm) {
                removeRequest(conversation.uri)
                swarmConversations[conversation.uri.rawRingId] = conversation
            }
            conversations[conversation.uri.uri] = conversation
            if (conversation.isSwarm && conversation.mode.blockingFirst() === Conversation.Mode.OneToOne) {
                try {
                    val contact = conversation.contact!!
                    val key = contact.uri.uri
                    val removed = cache.remove(key)
                    conversations.remove(key)
                    //Conversation contactConversation = getByUri(contact.getPrimaryUri());
                    // Log.w(TAG, "conversationStarted " + conversation.accountId + " contact " + key + " " + removed)
                    /*if (contactConversation != null) {
                        conversations.remove(contactConversation.getUri().getUri());
                    }*/
                    contact.setConversationUri(conversation.uri)
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "conversationStarted ${conversation.accountId} ${conversation.uri} ${conversation.contacts} ${conversation.mode.blockingFirst()}", e)
                }
            }
            conversationChanged()
        }
    }

    fun getSwarm(conversationId: String): Conversation? {
        synchronized(conversations) { return swarmConversations[conversationId] }
    }

    fun newSwarm(conversationId: String, mode: Conversation.Mode): Conversation {
        synchronized(conversations) {
            var c = swarmConversations[conversationId]
            if (c == null) {
                c = Conversation(accountId, Uri(Uri.SWARM_SCHEME, conversationId), mode)
                swarmConversations[conversationId] = c
            }
            c.setMode(mode)
            return c
        }
    }

    fun removeSwarm(conversationId: String) {
        Log.d(TAG, "removeSwarm $conversationId")
        synchronized(conversations) {
            val conversation = swarmConversations.remove(conversationId)
            if (conversation != null) {
                try {
                    val c = conversations.remove(conversation.uri.uri)
                    val contact = c!!.contact
                    Log.w(TAG, "removeSwarm: adding back contact conversation " + contact + " " + contact!!.conversationUri.blockingFirst() + " " + c.uri)
                    if (contact.conversationUri.blockingFirst().equals(c.uri)) {
                        contact.setConversationUri(contact.uri)
                        contactAdded(contact)
                    }
                } catch (ignored: Exception) {
                }
                conversationChanged()
            }
        }
    }

    data class ContactLocation (
        val latitude: Double,
        val longitude: Double,
        val timestamp: Long,
        val receivedDate: Date
    )

    class ContactLocationEntry (
        val contact: Contact,
        val conversation: Conversation,
        val location: Observable<ContactLocation>
    )

    enum class ComposingStatus {
        Idle, Active;

        companion object {
            fun fromInt(status: Int): ComposingStatus {
                return if (status == 1) Active else Idle
            }
        }
    }

    fun getConversationsSubject(withBanned: Boolean = false): Observable<List<Conversation>> =
        if (withBanned) conversationsSubject
        else conversationsSubject.map { list ->
            // In case where banned contact is in a swarm:group, we want to keep the conversation,
            // except if it is a swarm:group with only him and the user.
            list.filter { it.isGroup() || it.contact?.isBanned == false }
        }

    /**
     * Get conversation subject
     */
    fun getConversationSubject(): Observable<Conversation> = conversationSubject

    /**
     * Get invitation subject
     */
    fun getPendingSubject(): Observable<List<Conversation>> = pendingSubject

    fun getConversations(): Collection<Conversation> {
        return conversations.values
    }

    fun getPending(): Collection<Conversation> {
        return pending.values
    }

    private fun pendingRefreshed() {
        if (historyLoaded) {
            pendingSubject.onNext(getSortedPending())
        }
    }

    private fun pendingChanged() {
        pendingsChanged = true
        pendingRefreshed()
    }

    private fun pendingUpdated(conversation: Conversation?) {
        if (!historyLoaded) return
        if (pendingsChanged) {
            getSortedPending()
        } else {
            conversation?.sortHistory()
            sortedPending.sortWith(ConversationComparator())
        }
        pendingSubject.onNext(getSortedPending())
    }

    private fun conversationRefreshed(conversation: Conversation) {
        if (historyLoaded) {
            conversationSubject.onNext(conversation)
        }
    }

    fun conversationChanged() {
        synchronized(conversations) {
            conversationsChanged = true
            if (historyLoaded) {
                conversationsSubject.onNext(ArrayList(getSortedConversations()))
            }
        }
    }

    fun conversationUpdated(conversation: Conversation) {
        synchronized(conversations) {
            if (!historyLoaded) return
            if (conversationsChanged) {
                getSortedConversations()
            } else {
                conversation.sortHistory()
                sortedConversations.sortWith(ConversationComparator())
            }
            // TODO: remove next line when profile is updated through dedicated signal
            conversationSubject.onNext(conversation)
            conversationsSubject.onNext(ArrayList(sortedConversations))
        }
    }

    /**
     * Clears a conversation
     *
     * @param contact the contact
     * @param delete  true if you want to remove the conversation
     */
    fun clearHistory(contact: Uri?, delete: Boolean) {
        val conversation = getByUri(contact)
        // if it is a sip account, we do not add a contact event
        conversation!!.clearHistory(delete || isSip)
        conversationChanged()
    }

    fun clearAllHistory() {
        for (conversation in getConversations()) {
            // if it is a sip account, we do not add a contact event
            conversation.clearHistory(isSip)
        }
        for (conversation in pending.values) {
            conversation.clearHistory(true)
        }
        conversationChanged()
        pendingChanged()
    }

    fun updated(conversation: Conversation) {
        val key = conversation.uri.uri
        synchronized(conversations) {
            if (conversation == conversations[key]) {
                conversationUpdated(conversation)
                return
            }
        }
        synchronized(pending) {
            if (conversation == pending[key]) {
                pendingUpdated(conversation)
                return
            }
        }
        if (conversation == cache[key]) {
            if (isJami && !conversation.isSwarm
                //&& conversation.contacts.size == 1
                && !conversation.contact!!.conversationUri.blockingFirst().equals(conversation.uri)) {
                return
            }
            if (mContacts.containsKey(key) || !isJami) {
                Log.w(TAG, "updated " + conversation.accountId + " contact " + key)
                conversations[key] = conversation
                conversationChanged()
            } else {
                pending[key] = conversation
                pendingChanged()
            }
        }
    }

    fun refreshed(conversation: Conversation) {
        synchronized(conversations) {
            if (conversations.containsValue(conversation)) {
                conversationRefreshed(conversation)
                return
            }
        }
        synchronized(pending) {
            if (pending.containsValue(conversation))
                pendingRefreshed()
        }
    }

    fun addTextMessage(txt: TextMessage) {
        var conversation: Conversation? = null
        val daemonId = txt.daemonIdString
        if (daemonId != null && daemonId.isNotEmpty()) {
            conversation = getConversationByCallId(daemonId)
        }
        if (conversation == null) {
            conversation = getByKey(txt.conversation!!.participant!!)
            txt.contact = conversation.contact
        }
        conversation.addTextMessage(txt)
        updated(conversation)
    }

    fun onDataTransferEvent(transfer: DataTransfer): Conversation {
        Log.d(TAG, "Account onDataTransferEvent " + transfer.messageId)
        val conversation = transfer.conversation as Conversation
        val transferEventCode = transfer.status
        synchronized(conversation) {
            if (transferEventCode == InteractionStatus.TRANSFER_CREATED) {
                conversation.addFileTransfer(transfer)
                updated(conversation)
            } else {
                conversation.updateFileTransfer(transfer, transferEventCode)
            }
        }
        return conversation
    }

    val bannedContactsUpdates: Observable<Collection<Contact>>
        get() = contactListSubject.map { list -> list.filterTo(ArrayList(list.size), Contact::isBanned) }

    fun getContactFromCache(key: String): Contact {
        if (key.isEmpty()) throw IllegalStateException()
        synchronized(mContactCache) {
            return mContactCache.getOrPut(key) {
                if (isSip) Contact.buildSIP(Uri.fromString(key))
                else Contact.build(key, username == key)
            }
        }
    }

    fun getContactFromCache(uri: Uri): Contact {
        return getContactFromCache(uri.uri)
    }

    fun dispose() {
        contactListSubject.onComplete()
        //trustRequestsSubject.onComplete();
    }

    fun setCredentials(creds: List<Map<String, String>>) {
        credentials.clear()
        credentials.ensureCapacity(creds.size)
        creds.forEach { c -> credentials.add(AccountCredentials(c)) }
    }

    fun setDetails(details: Map<String, String>) {
        config = AccountConfig(details)
        username = config[ConfigKey.ACCOUNT_USERNAME]
    }

    fun setDetail(key: ConfigKey, value: String?) {
        config.put(key, value)
    }

    fun setDetail(key: ConfigKey, value: Boolean) {
        config.put(key, value)
    }

    val displayname: String
        get() = config[ConfigKey.ACCOUNT_DISPLAYNAME]
    val displayUsername: String?
        get() {
            if (isJami) {
                val registeredName = registeredName
                if (registeredName.isNotEmpty())
                    return registeredName
            }
            return username
        }
    var host: String?
        get() = config[ConfigKey.ACCOUNT_HOSTNAME]
        set(host) {
            config.put(ConfigKey.ACCOUNT_HOSTNAME, host!!)
        }
    var proxy: String
        get() = config[ConfigKey.ACCOUNT_ROUTESET]
        set(proxy) {
            config.put(ConfigKey.ACCOUNT_ROUTESET, proxy)
        }
    var isDhtProxyEnabled: Boolean
        get() = config.getBool(ConfigKey.PROXY_ENABLED)
        set(active) {
            config.put(ConfigKey.PROXY_ENABLED, if (active) "true" else "false")
        }

    var dhtProxy: String
        get() = config[ConfigKey.PROXY_SERVER]
        set(proxy) {
            config.put(ConfigKey.PROXY_SERVER, proxy)
        }

    val registrationState: AccountConfig.RegistrationState
        get() = registrationStateSubject.blockingFirst()
    val registrationStateObservable: Observable<AccountConfig.RegistrationState>
        get() = registrationStateSubject

    val dhtProxyUsed: String
        get() = mVolatileDetails[ConfigKey.PROXY_SERVER]

    fun setRegistrationState(registeredState: AccountConfig.RegistrationState, code: Int=0) {
        //mVolatileDetails.put(ConfigKey.ACCOUNT_REGISTRATION_STATUS, registeredState)
        registrationStateSubject.onNext(registeredState)
        //mVolatileDetails.put(ConfigKey.ACCOUNT_REGISTRATION_STATE_CODE, code.toString())
    }

    fun setVolatileDetails(volatileDetails: Map<String, String>) {
        mVolatileDetails = AccountConfig(volatileDetails)
        setRegistrationState(AccountConfig.RegistrationState.valueOf(mVolatileDetails[ConfigKey.ACCOUNT_REGISTRATION_STATUS]))
    }

    val registeredName: String
        get() = mVolatileDetails[ConfigKey.ACCOUNT_REGISTERED_NAME]
    var alias: String?
        get() = config[ConfigKey.ACCOUNT_ALIAS]
        set(alias) {
            config.put(ConfigKey.ACCOUNT_ALIAS, alias!!)
        }
    val isSip: Boolean
        get() = config[ConfigKey.ACCOUNT_TYPE] == AccountConfig.ACCOUNT_TYPE_SIP
    val isJami: Boolean
        get() = config[ConfigKey.ACCOUNT_TYPE] == AccountConfig.ACCOUNT_TYPE_JAMI

    private fun getDetail(key: ConfigKey): String = config[key]

    fun getDetailBoolean(key: ConfigKey): Boolean {
        return config.getBool(key)
    }

    var isEnabled: Boolean
        get() = config.getBool(ConfigKey.ACCOUNT_ENABLE)
        set(isChecked) {
            config.put(ConfigKey.ACCOUNT_ENABLE, isChecked)
        }
    val isActive: Boolean
        get() = mVolatileDetails.getBool(ConfigKey.ACCOUNT_ACTIVE)

    fun hasPassword(): Boolean {
        return config.getBool(ConfigKey.ARCHIVE_HAS_PASSWORD)
    }

    fun hasManager(): Boolean {
        return config[ConfigKey.MANAGER_URI].isNotEmpty()
    }

    val details: HashMap<String, String>
        get() = config.all
    val isTrying: Boolean
        get() = registrationState == AccountConfig.RegistrationState.TRYING
    val isRegistered: Boolean
        get() = registrationState == AccountConfig.RegistrationState.REGISTERED
    val isInError: Boolean
        get() {
            val state = registrationState
            return (state == AccountConfig.RegistrationState.ERROR_AUTH
                    || state == AccountConfig.RegistrationState.ERROR_GENERIC
                    || state == AccountConfig.RegistrationState.ERROR_HOST
                    || state == AccountConfig.RegistrationState.ERROR_NETWORK
                    || state == AccountConfig.RegistrationState.ERROR_SERVICE_UNAVAILABLE)
        }
    val isIP2IP: Boolean
        get() {
            val emptyHost = host == null || host != null && host!!.isEmpty()
            return isSip && emptyHost
        }
    val isAutoanswerEnabled: Boolean
        get() = config.getBool(ConfigKey.ACCOUNT_AUTOANSWER)

    fun addCredential(newValue: AccountCredentials) {
        credentials.add(newValue)
    }

    fun removeCredential(accountCredentials: AccountCredentials) {
        credentials.remove(accountCredentials)
    }

    val credentialsHashMapList: List<Map<String, String>>
        get() {
            val result = ArrayList<Map<String, String>>(credentials.size)
            for (cred in credentials) {
                result.add(cred.details)
            }
            return result
        }

    private fun getUri(display: Boolean): String? {
        val username = if (display) displayUsername else username
        return if (isJami) username else "$username@$host"
    }

    val uri: String?
        get() = getUri(false)
    val displayUri: String?
        get() = getUri(true)

    fun needsMigration(): Boolean = AccountConfig.RegistrationState.ERROR_NEED_MIGRATION == registrationState

    val deviceId: String
        get() = mVolatileDetails[ConfigKey.ACCOUNT_DEVICE_ID]
    val deviceName: String
        get() = getDetail(ConfigKey.ACCOUNT_DEVICE_NAME)
    val contacts: Map<String, Contact>
        get() = mContacts
    val bannedContacts: List<Contact>
        get() {
            val banned = ArrayList<Contact>()
            for (contact in mContacts.values) {
                if (contact.isBanned) {
                    banned.add(contact)
                }
            }
            return banned
        }


    fun getContact(uri: Uri): Contact? = getContact(uri.rawRingId)
    fun getContact(ringId: String?): Contact? = mContacts[ringId]

    fun addContact(id: String, confirmed: Boolean) {
        val contact = mContacts.getOrPut(id) { getContactFromCache(Uri.fromId(id)) }
        contact.addedDate = Date()
        contact.status = if (confirmed) Contact.Status.CONFIRMED else Contact.Status.REQUEST_SENT
        contactAdded(contact)
        contactListSubject.onNext(mContacts.values)
    }

    fun removeContact(id: String, banned: Boolean) {
        Log.w(TAG, "removeContact $id $banned")
        var contact = mContacts[id]
        if (banned) {
            if (contact == null) {
                contact = getContactFromCache(Uri.fromId(id))
                mContacts[id] = contact
            }
            contact.status = Contact.Status.BANNED
        } else {
            mContacts.remove(id)
        }
        if (contact != null) {
            contactRemoved(contact.uri, contact.conversationUri.blockingFirst())
        }
        contactListSubject.onNext(mContacts.values)
    }

    fun addContact(contact: Map<String, String>): Contact {
        val contactId = contact[CONTACT_ID]!!
        val callContact = mContacts[contactId] ?: getContactFromCache(Uri.fromId(contactId))
        val addedStr = contact[CONTACT_ADDED]
        if (!addedStr.isNullOrEmpty()) {
            val added = addedStr.toLong()
            callContact.addedDate = Date(added * 1000)
        }
        if (contact.containsKey(CONTACT_BANNED) && contact[CONTACT_BANNED] == "true") {
            callContact.status = Contact.Status.BANNED
        } else if (contact.containsKey(CONTACT_CONFIRMED)) {
            callContact.status = if (contact[CONTACT_CONFIRMED].toBoolean()) Contact.Status.CONFIRMED else Contact.Status.REQUEST_SENT
        }
        val conversationUri = contact[CONTACT_CONVERSATION]
        if (conversationUri != null && conversationUri.isNotEmpty()) {
            callContact.setConversationUri(Uri(Uri.SWARM_SCHEME, conversationUri))
        }
        mContacts[contactId] = callContact
        contactAdded(callContact)
        contactListSubject.onNext(mContacts.values)
        return callContact
    }

    fun setContacts(contacts: List<Map<String, String>>) {
        for (contact in contacts) {
            addContact(contact)
        }
        contactListSubject.onNext(mContacts.values)
    }

    fun addRequest(request: TrustRequest) {
        synchronized(pending) {
            val key = request.conversationUri.uri
            if (pending[key] == null) {
                val conversation = if (request.conversationUri.isSwarm)
                    Conversation(accountId, request.conversationUri, Conversation.Mode.Request).apply {
                        val contact = getContactFromCache(request.from).apply {
                            if (request.mode == Conversation.Mode.OneToOne && !conversationUri.blockingFirst().isSwarm)
                                setConversationUri(request.conversationUri)
                        }
                        addContact(contact)
                        addContactEvent(ContactEvent(accountId, contact, request))
                    }
                else
                    getByKey(key)
                request.profile?.let { p ->
                    // Apply request profile to contact
                    if (request.mode == Conversation.Mode.OneToOne) {
                        conversation.contact?.setProfile(p)
                    }
                    conversation.setProfile(p)
                }

                conversation.request = request

                //Log.w(TAG, "pendingRequestAdded $key")
                pending[key] = conversation
                if (!conversation.isSwarm) {
                    val contact = getContactFromCache(request.from)
                    conversation.addRequestEvent(request, contact)
                }
                pendingChanged()
            }
        }
    }

    fun removeRequest(conversationUri: Uri): TrustRequest? {
        synchronized(pending) {
            val uri = conversationUri.uri
            val conversation = pending.remove(uri)
            val request = conversation?.request
            if (request != null) {
                // When receiving a request, we point the contact to its conversation.
                // When we refuse it, we should reset this link (to the contact uri).
                request.from.let {
                    val contact = getContactFromCache(it)
                    // Make sure the contact conversation uri was pointing to request uri before
                    // deleting the link.
                    if (contact.conversationUri.blockingFirst() == request.conversationUri)
                        contact.setConversationUri(contact.uri)
                }
                pendingChanged()
            }
            return request
        }
    }

    /*fun registeredNameFound(state: Int, address: String, name: String?): Boolean {
        val uri = Uri.fromString(address)
        val key = uri.uri
        val contact = getContactFromCache(key)
        if (contact.setUsername(if (state == 0) name else null)) {
            synchronized(conversations) {
                conversations[key]?.let { conversationRefreshed(it) }
            }
            synchronized(pending) { if (pending.containsKey(key)) pendingRefreshed() }
            return true
        }
        return false
    }*/

    fun getByUri(uri: Uri?): Conversation? =
        if (uri == null || uri.isEmpty) null
        else if (uri.isSwarm) getSwarm(uri.rawRingId) ?: pending[uri.uri]
        else getByKey(uri.uri)

    fun getByUri(uri: String?): Conversation? =
        if (uri != null) getByUri(Uri.fromString(uri)) else null

    fun getByKey(key: String): Conversation =
        cache.getOrPut(key) { Conversation(accountId, getContactFromCache(key)) }

    fun setHistoryLoaded(conversations: List<Conversation> = emptyList()) {
        synchronized(this.conversations) {
            if (historyLoaded) return
            //Log.w(TAG, "setHistoryLoaded $accountId ${conversations.size}")
            for (c in conversations) {
                val contact = c.contact
                if (!c.isSwarm && contact != null && contact.conversationUri.blockingFirst() == c.uri)
                    updated(c)
            }
            historyLoaded = true
            conversationChanged()
            pendingChanged()
        }
    }

    private fun getSortedConversations(): List<Conversation> {
        if (conversationsChanged) {
            sortedConversations.clear()
            if (conversations.isNotEmpty()) {
                sortedConversations.addAll(conversations.values)
                for (c in sortedConversations) c.sortHistory()
                Collections.sort(sortedConversations, ConversationComparator())
            }
            conversationsChanged = false
        }
        return sortedConversations
    }

    private fun getSortedPending(): List<Conversation> {
        if (pendingsChanged) {
            sortedPending.clear()
            sortedPending.addAll(pending.values)
            for (c in sortedPending) c.sortHistory()
            Collections.sort(sortedPending, ConversationComparator())
            pendingsChanged = false
        }
        return sortedPending
    }

    private fun contactAdded(contact: Contact) {
        val uri = contact.uri
        val key = uri.uri
        //Log.w(TAG, "contactAdded " + accountId + " " + uri + " " + contact.conversationUri.blockingFirst())
        if (contact.conversationUri.blockingFirst() != uri) {
            //Log.w(TAG, "contactAdded Don't add conversation if we have a swarm conversation")
            // Don't add conversation if we have a swarm conversation
            return
        }
        synchronized(conversations) {
            if (conversations.containsKey(key)) return
            synchronized(pending) {
                var pendingConversation = pending[key]
                if (pendingConversation == null) {
                    pendingConversation = getByKey(key)
                    conversations[key] = pendingConversation
                } else {
                    pending.remove(key)
                    conversations[key] = pendingConversation
                    pendingChanged()
                }
                pendingConversation.addContactEvent(contact)
            }
            conversationChanged()
        }
    }

    private fun contactRemoved(uri: Uri, conversationUri: Uri) {
        Log.w(TAG, "contactRemoved $uri")
        val key = uri.uri
        synchronized(conversations) {
            synchronized(pending) { if (pending.remove(key) != null) pendingChanged() }
            conversations.remove(key)
            //swarmConversations.remove(conversationUri.uri)
            conversationChanged()
        }
    }

    private fun getConversationByCallId(callId: String): Conversation? {
        for (conversation in conversations.values) {
            val conf = conversation.getConference(callId)
            if (conf != null) {
                return conversation
            }
        }
        return null
    }

    fun presenceUpdate(contactUri: String, isOnline: Boolean) {
        //Log.w(TAG, "presenceUpdate " + contactUri + " " + isOnline);
        val contact = getContactFromCache(contactUri)
        contact.setPresence(isOnline)
        synchronized(conversations) {
            conversations[contactUri]?.let { conversationRefreshed(it) }
        }
        synchronized(pending) { if (pending.containsKey(contactUri)) pendingRefreshed() }
    }

    fun composingStatusChanged(conversationId: String, contactUri: Uri, status: ComposingStatus) {
        val isSwarm = conversationId.isNotEmpty()
        val conversation = if (isSwarm) getSwarm(conversationId) else getByUri(contactUri)
        if (conversation != null) {
            val contact = if (isSwarm) conversation.findContact(contactUri) else getContactFromCache(contactUri)
            if (contact != null) {
                conversation.composingStatusChanged(contact, status)
            }
        }
    }

    @Synchronized
    fun onLocationUpdate(location: AccountService.Location): Long {
        val contact = getContactFromCache(location.peer)
        val conversation = getByUri(contact.conversationUri.blockingFirst()) ?: return 0L
        when (location.type) {
            AccountService.Location.Type.Position -> {
                val cl = ContactLocation(location.latitude, location.longitude, location.date, Date())
                var ls = contactLocations[contact]
                if (ls == null) {
                    ls = BehaviorSubject.createDefault(cl)
                    contactLocations[contact] = ls
                    mLocationSubject.onNext(contactLocations)
                    mLocationStartedSubject.onNext(ContactLocationEntry(contact, conversation, ls))
                } else if (ls.blockingFirst().timestamp < cl.timestamp) {
                    (ls as Subject<ContactLocation>).onNext(cl)
                }
            }
            AccountService.Location.Type.Stop -> forceExpireContact(contact)
        }
        return LOCATION_SHARING_EXPIRATION_MS.toLong()
    }

    @Synchronized
    private fun forceExpireContact(contact: Contact?) {
        Log.w(TAG, "forceExpireContact " + contactLocations.size)
        val cl = contactLocations.remove(contact)
        if (cl != null) {
            Log.w(TAG, "Contact stopped sharing location: $contact")
            (cl as Subject<ContactLocation>).onComplete()
            mLocationSubject.onNext(contactLocations)
        }
    }

    @Synchronized
    fun maintainLocation() {
        Log.w(TAG, "maintainLocation ${contactLocations.size}")
        if (contactLocations.isEmpty()) return
        var changed = false
        val expiration = Date(System.currentTimeMillis() - LOCATION_SHARING_EXPIRATION_MS)
        val it: MutableIterator<Map.Entry<Contact, Observable<ContactLocation>>> =
            contactLocations.entries.iterator()
        while (it.hasNext()) {
            val e = it.next()
            if (e.value.blockingFirst().receivedDate.before(expiration)) {
                Log.w(TAG, "maintainLocation clearing $e.key")
                (e.value as Subject<ContactLocation>?)!!.onComplete()
                changed = true
                it.remove()
            }
        }
        if (changed) mLocationSubject.onNext(contactLocations)
    }

    val locationUpdates: Observable<ContactLocationEntry>
        get() = mLocationStartedSubject
    val locationsUpdates: Observable<Map<Contact, Observable<ContactLocation>>>
        get() = mLocationSubject

    fun getLocationUpdates(conversationUri: Uri): Observable<Observable<ContactLocation>> {
        val conversation = getByUri(conversationUri) ?: return Observable.empty()
        if (conversation.isGroup())
            return Observable.empty()
        val contact = conversation.contact ?: return Observable.empty()
        return if (contact.isUser) Observable.empty() else mLocationSubject
            .flatMapMaybe{ locations: Map<Contact, Observable<ContactLocation>> ->
                val r = locations[contact]
                if (r == null) Maybe.empty() else Maybe.just(r)
            }
            .distinctUntilChanged()
    }

    val accountAlias: Single<String>
        get() = loadedProfileObservable.firstOrError()
            .map { p -> if (p.displayName.isNullOrEmpty())
                if (isJami) jamiAlias else alias!!
            else p.displayName }

    /**
     * Registered name, fallback to Alias
     */
    private val jamiAlias: String
        get() = registeredName.ifEmpty { alias!! }

    fun getDataTransfer(id: String): DataTransfer? = mDataTransfers[id]

    fun putDataTransfer(fileId: String, transfer: DataTransfer) {
        mDataTransfers[fileId] = transfer
    }

    fun setActiveCalls(conversationId: String, activeCalls: List<Map<String, String>>) =
        getSwarm(conversationId)?.setActiveCalls(activeCalls)

    private class ConversationComparator : Comparator<Conversation> {
        override fun compare(a: Conversation, b: Conversation): Int =
            Interaction.compare(b.lastEvent, a.lastEvent)
    }

    companion object {
        private val TAG = Account::class.simpleName!!
        private const val CONTACT_ADDED = "added"
        private const val CONTACT_CONFIRMED = "confirmed"
        private const val CONTACT_BANNED = "banned"
        private const val CONTACT_ID = "id"
        private const val CONTACT_CONVERSATION = "conversationId"
        private const val LOCATION_SHARING_EXPIRATION_MS = 1000 * 60 * 2
    }
}