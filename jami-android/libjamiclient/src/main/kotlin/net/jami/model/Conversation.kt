/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
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

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.SingleSubject
import io.reactivex.rxjava3.subjects.Subject
import net.jami.utils.Log
import net.jami.utils.StringUtils
import java.util.*
import kotlin.collections.ArrayList

class Conversation : ConversationHistory {
    val accountId: String
    val uri: Uri
    val contacts: MutableList<Contact>
    val roles: MutableMap<String, MemberRole> = HashMap()
    private val rawHistory: NavigableMap<Long, Interaction> = TreeMap()
    private val currentCalls = ArrayList<Conference>()
    val aggregateHistory = ArrayList<Interaction>(32)

    val lastDisplayedMessages: MutableMap<String, String> = HashMap()
    private val updatedElementSubject: Subject<Pair<Interaction, ElementStatus>> = PublishSubject.create()
    private val clearedSubject: Subject<List<Interaction>> = PublishSubject.create()
    private val callsSubject: Subject<List<Conference>> = BehaviorSubject.createDefault(emptyList())
    private val activeCallsSubject: Subject<List<ActiveCall>> = BehaviorSubject.createDefault(emptyList())
    private val composingStatusSubject: Subject<Account.ComposingStatus> = BehaviorSubject.createDefault(Account.ComposingStatus.Idle)
    private val color: Subject<Int> = BehaviorSubject.createDefault(0)
    private val symbol: Subject<CharSequence> = BehaviorSubject.createDefault("")

    // Notification preference. True if notifications are enabled, false otherwise.
    private val isNotificationEnabledSubject: Subject<Boolean> = BehaviorSubject.createDefault(true)
    val isNotificationEnabledObservable: Observable<Boolean>
        get() = isNotificationEnabledSubject
    val isNotificationEnabled: Boolean
        get() = isNotificationEnabledSubject.blockingFirst()

    private val mContactSubject: Subject<List<Contact>> = BehaviorSubject.create()
    var loaded: Single<Conversation>? = null
    val lastElementLoadedSubject = SingleSubject.create<Completable>()
    val lastElementLoaded = lastElementLoadedSubject.flatMapCompletable { it }
    private val mMessages: MutableMap<String, Interaction> = HashMap(16)
    private val mPendingMessages: MutableMap<String, SingleSubject<Interaction>> = HashMap(8)
    var lastRead: String? = null
        private set
    var lastNotified: String? = null
        private set
    var lastSent: String? = null
        private set
    private val mMode: Subject<Mode>

    private val profileSubject: Subject<Single<Profile>> = BehaviorSubject.createDefault(Profile.EMPTY_PROFILE_SINGLE)
    val profile: Observable<Profile> = profileSubject.switchMapSingle { it }

    // runtime flag set to true if the user is currently viewing this conversation
    private var mVisible = false
    private val mVisibleSubject: Subject<Boolean> = BehaviorSubject.createDefault(mVisible)

    // indicate the list needs sorting
    private var mDirty = false
    private var mLoadingSubject: SingleSubject<Conversation>? = null

    var request:TrustRequest? = null

    val mode: Observable<Mode>
        get() = mMode
    val isSwarm: Boolean
        get() = Uri.SWARM_SCHEME == uri.scheme

    val contactUpdates: Observable<List<Contact>>
        get() = mContactSubject

    var loading: SingleSubject<Conversation>?
        get() = mLoadingSubject
        set(l) {
            mLoadingSubject?.let { loading ->
                if (!loading.hasValue() && !loading.hasThrowable())
                    loading.onError(IllegalStateException())
            }
            mLoadingSubject = l
        }

    enum class ElementStatus {
        UPDATE, REMOVE, ADD
    }

    val updatedElements: Observable<Pair<Interaction, ElementStatus>>
        get() = updatedElementSubject

    val cleared: Observable<List<Interaction>>
        get() = clearedSubject

    val calls: Observable<List<Conference>>
        get() = callsSubject

    val activeCalls: Observable<List<ActiveCall>>
        get() = activeCallsSubject

    val composingStatus: Observable<Account.ComposingStatus>
        get() = composingStatusSubject

    val sortedHistory: Single<List<Interaction>> = Single.fromCallable {
        sortHistory()
        ArrayList(aggregateHistory)
    }
    var lastEvent: Interaction? = null
        set(e) {
            field = e
            if (e != null)
                lastEventSubject.onNext(e)
        }
    private val lastEventSubject: Subject<Interaction> = BehaviorSubject.create()
    val currentStateObservable: Observable<Pair<Interaction, Boolean>> =
        Observable.combineLatest(
            lastEventSubject,
            callsSubject.map { calls ->
                for (call in calls)
                    if (call.isOnGoing)
                        return@map true
                false
            }) { event, hasCurrentCall -> Pair(event, hasCurrentCall) }

    constructor(accountId: String, contact: Contact) {
        // This should be a legacy conversation (contact view), no role importance.
        this.accountId = accountId
        contacts = mutableListOf(contact)
        uri = contact.uri
        participant = contact.uri.uri
        mContactSubject.onNext(contacts)
        mMode = BehaviorSubject.createDefault(Mode.Legacy)
    }

    constructor(accountId: String, uri: Uri, mode: Mode) {
        this.accountId = accountId
        this.uri = uri
        contacts = ArrayList(3)
        mMode = BehaviorSubject.createDefault(mode)
    }

    fun getConference(confId: String?): Conference? {
        val id = confId ?: return null
        for (c in currentCalls)
            if (c.id == id || c.getCallById(id) != null)
                return c
        return null
    }

    fun composingStatusChanged(contact: Contact, composing: Account.ComposingStatus) {
        composingStatusSubject.onNext(composing)
    }

    /*val displayName: String?
        get() = contacts[0].displayName*/

    fun addContact(contact: Contact, memberRole: MemberRole? = null) {
        memberRole?.let { roles[contact.uri.uri] = it }
        contacts.add(contact)
        mContactSubject.onNext(contacts)
    }

    fun removeContact(contact: Contact) {
        roles.remove(contact.uri.uri)
        contacts.remove(contact)
        mContactSubject.onNext(contacts)
    }

    @Synchronized
    fun readMessages(): List<Interaction> {
        val interactions = ArrayList<Interaction>()
        if (isSwarm) {
            if (aggregateHistory.isNotEmpty()) {
                var n = aggregateHistory.size
                do {
                    if (n == 0) break
                    val i = aggregateHistory[--n]
                    if (!i.isRead) {
                        i.read()
                        interactions.add(i)
                        lastRead = i.messageId
                    }
                } while (i.type == Interaction.InteractionType.INVALID)
            }
        } else {
            for (e in rawHistory.descendingMap().values) {
                if (e.type != Interaction.InteractionType.TEXT) continue
                if (e.isRead) {
                    break
                }
                e.read()
                interactions.add(e)
            }
        }
        // Update the last event if it was just read
        interactions.firstOrNull { it.type != Interaction.InteractionType.INVALID }?.let {
            lastEvent = it
        }
        return interactions
    }

    @Synchronized
    fun getMessage(messageId: String): Interaction? = mMessages[messageId]

    fun setLastMessageRead(lastMessageRead: String?) {
        lastRead = lastMessageRead
    }

    fun setLastMessageNotified(lastMessage: String?) {
        lastNotified = lastMessage
    }

    fun stopLoading(): SingleSubject<Conversation>? {
        val ret = mLoadingSubject
        mLoadingSubject = null
        return ret
    }

    fun setProfile(profile: Single<Profile>) {
        profileSubject.onNext(profile)
    }

    fun setMode(mode: Mode) {
        mMode.onNext(mode)
    }

    fun addConference(conference: Conference?) {
        if (conference == null) {
            return
        }
        for (i in currentCalls.indices) {
            val currentConference = currentCalls[i]
            if (currentConference === conference) {
                return
            }
            if (currentConference.id == conference.id) {
                currentCalls[i] = conference
                callsSubject.onNext(currentCalls)
                return
            }
        }
        currentCalls.add(conference)
        callsSubject.onNext(currentCalls)
    }

    fun removeConference(c: Conference) {
        currentCalls.remove(c)
        callsSubject.onNext(currentCalls)
    }

    var isVisible: Boolean
        get() = mVisible
        set(visible) {
            mVisible = visible
            mVisibleSubject.onNext(mVisible)
        }

    /** Flags that notification should not be removed */
    var isBubble = false

    fun getVisible(): Observable<Boolean> = mVisibleSubject

    val contact: Contact?
        get() {
            if (contacts.size == 1) return contacts[0]
            if (isSwarm) {
                check(contacts.size <= 2) { "getContact() called for group conversation of size " + contacts.size }
            }
            for (contact in contacts) {
                if (!contact.isUser) return contact
            }
            return null
        }

    @Synchronized
    fun addCall(call: Call) {
        if (!isSwarm && callHistory.contains(call)) {
            return
        }
        mDirty = true
        aggregateHistory.add(call)
        updatedElementSubject.onNext(Pair(call, ElementStatus.ADD))
    }

    private fun setInteractionProperties(interaction: Interaction) {
        interaction.account = accountId
        if (interaction.contact == null) {
            if (contacts.size == 1) interaction.contact = contacts[0] else {
                if (interaction.author == null) {
                    Log.e(TAG, "Unable to set interaction properties: no author for type:" + interaction.type + " id:" + interaction.id + " status:" + interaction.status)
                } else {
                    interaction.contact = findContact(Uri.fromString(interaction.author!!))
                }
            }
        }
    }

    fun findContact(uri: Uri): Contact? = contacts.firstOrNull { it.uri == uri }

    fun addTextMessage(txt: TextMessage) {
        if (mVisible)
            txt.read()
        setInteractionProperties(txt)
        rawHistory[txt.timestamp] = txt
        mDirty = true
        aggregateHistory.add(txt)
        updatedElementSubject.onNext(Pair(txt, ElementStatus.ADD))
    }

    fun addRequestEvent(request: TrustRequest, contact: Contact) {
        if (isSwarm) return
        val event = ContactEvent(accountId, contact, request)
        mDirty = true
        aggregateHistory.add(event)
        updatedElementSubject.onNext(Pair(event, ElementStatus.ADD))
    }

    fun addContactEvent(contact: Contact) {
        addContactEvent(ContactEvent(accountId, contact))
    }

    fun addContactEvent(contactEvent: ContactEvent) {
        mDirty = true
        aggregateHistory.add(contactEvent)
        updatedElementSubject.onNext(Pair(contactEvent, ElementStatus.ADD))
    }

    fun addFileTransfer(dataTransfer: DataTransfer) {
        if (aggregateHistory.contains(dataTransfer)) {
            return
        }
        mDirty = true
        aggregateHistory.add(dataTransfer)
        updatedElementSubject.onNext(Pair(dataTransfer, ElementStatus.ADD))
    }

    private fun isAfter(previous: Interaction, query: Interaction?): Boolean {
        var query = query
        return if (isSwarm) {
            while (query?.parentId != null) {
                if (query.parentId == previous.messageId)
                    return true
                query = mMessages[query.parentId]
            }
            false
        } else {
            previous.timestamp < query!!.timestamp
        }
    }

    @Synchronized
    fun setLastMessageDisplayed(contactId: String, messageId: String) {
        // Check if the new message is after the last displayed message (could be not the case).
        val currentLastMessageDisplayed: Interaction? =
            lastDisplayedMessages[contactId]?.let { getMessage(it) }
        val newPotentialMessageDisplayed = getMessage(messageId)
        val isAfter =
            if (currentLastMessageDisplayed != null && newPotentialMessageDisplayed != null) {
                isAfter(currentLastMessageDisplayed, newPotentialMessageDisplayed)
            } else false

        // Update the last displayed message
        if (newPotentialMessageDisplayed?.type != Interaction.InteractionType.INVALID &&
            newPotentialMessageDisplayed?.type != null &&
            (isAfter || (currentLastMessageDisplayed == null))) {
            lastDisplayedMessages[contactId] = messageId

            updatedElementSubject.onNext(Pair(newPotentialMessageDisplayed, ElementStatus.UPDATE))
            // Also update the previous messages (such as change from sent to displayed)
            var interaction: Interaction? = newPotentialMessageDisplayed
            while (interaction?.messageId != currentLastMessageDisplayed?.messageId
                && interaction != null
                && currentLastMessageDisplayed != null
            ) {
                interaction = mMessages[interaction.parentId]?.apply {
                    updatedElementSubject.onNext(Pair(this, ElementStatus.UPDATE))
                }
            }
        }
    }

    @Synchronized
    fun setLastMessageSent(messageId: String) {
        val currentLastSentMessage: Interaction? = lastSent?.let { getMessage(it) }
        val newPotentialLastSentMessage: Interaction? = getMessage(messageId)
        val isAfter =
                if (currentLastSentMessage != null && newPotentialLastSentMessage != null) {
                    isAfter(currentLastSentMessage, newPotentialLastSentMessage)
                } else false
        if (newPotentialLastSentMessage?.type != Interaction.InteractionType.INVALID &&
                newPotentialLastSentMessage?.type != null &&
                (isAfter || (currentLastSentMessage == null))) {
            lastSent = messageId
        }
    }

    @Synchronized
    fun updateSwarmInteraction(
        messageId: String,
        contactUri: Uri,
        newStatus: Interaction.MessageStates,
    ) {
        val interaction = mMessages[messageId] ?: return
        if (newStatus == Interaction.MessageStates.DISPLAYED) {
            findContact(contactUri)?.let { contact ->
                if (!contact.isUser)
                    setLastMessageDisplayed(contactUri.host, messageId)
            }
        } else if (newStatus != Interaction.MessageStates.SENDING) {
            interaction.status = Interaction.InteractionStatus.SENDING
        }

        if(newStatus == Interaction.MessageStates.SUCCESS) {
            setLastMessageSent(messageId)
        }

        interaction.statusMap = interaction.statusMap.plus(Pair(contactUri.host, newStatus))
        updatedElementSubject.onNext(Pair(interaction, ElementStatus.UPDATE))
    }

    @Synchronized
    fun updateInteraction(element: Interaction) {
        Log.e(TAG, "updateInteraction: ${element.messageId} ${element.status}")
        if (isSwarm) {
            val e = mMessages[element.messageId]
            if (e != null) {
                e.status = element.status
                updatedElementSubject.onNext(Pair(e, ElementStatus.UPDATE))
            } else {
                Log.e(TAG, "Unable to find swarm message to update: ${element.messageId}")
            }
        } else {
            setInteractionProperties(element)
            val time = element.timestamp
            val msgs = rawHistory.subMap(time, true, time, true)
            for (txt in msgs.values) {
                if (txt.id == element.id) {
                    txt.status = element.status
                    updatedElementSubject.onNext(Pair(txt, ElementStatus.UPDATE))
                    return
                }
            }
            Log.e(TAG, "Unable to find message to update: ${element.id}")
        }
    }

    @Synchronized
    fun sortHistory() {
        if (mDirty) {
            if (!isSwarm) {
                aggregateHistory.sortWith { c1, c2 -> c1.timestamp.compareTo(c2.timestamp) }
            }
            lastEvent = aggregateHistory.lastOrNull { it.type != Interaction.InteractionType.INVALID }
            mDirty = false
        }
    }

    val currentCall: Conference?
        get() = if (currentCalls.isEmpty()) null else currentCalls[0]

    private val callHistory: Collection<Call>
        get() {
            val result: MutableList<Call> = ArrayList()
            for (interaction in aggregateHistory) {
                if (interaction.type == Interaction.InteractionType.CALL) {
                    result.add(interaction as Call)
                }
            }
            return result
        }

    val unreadTextMessages: TreeMap<Long, TextMessage>
        get() {
            val texts = TreeMap<Long, TextMessage>()
            if (isSwarm) {
                synchronized(this) {
                    for (j in aggregateHistory.indices.reversed()) {
                        val i = aggregateHistory[j]
                        if (i !is TextMessage) continue
                        if (i.isRead || i.isNotified) break
                        texts[i.timestamp] = i
                    }
                }
            } else {
                for ((key, value) in rawHistory.descendingMap()) {
                    if (value.type == Interaction.InteractionType.TEXT) {
                        val message = value as TextMessage
                        if (message.isRead || message.isNotified) break
                        texts[key] = message
                    }
                }
            }
            return texts
        }

    private fun findConversationElement(transferId: Int): Interaction? {
        for (interaction in aggregateHistory) {
            if (interaction.type == Interaction.InteractionType.DATA_TRANSFER) {
                if (transferId == interaction.id) {
                    return interaction
                }
            }
        }
        return null
    }

    private fun removeSwarmInteraction(messageId: String): Boolean {
        val i = mMessages.remove(messageId)
        if (i != null) {
            aggregateHistory.remove(i)
            return true
        }
        return false
    }

    private fun removeInteraction(interactionId: Long): Boolean {
        val it = aggregateHistory.iterator()
        while (it.hasNext()) {
            val interaction = it.next()
            if (interactionId == interaction.id.toLong()) {
                it.remove()
                return true
            }
        }
        return false
    }

    /**
     * Clears the conversation cache.
     * @param delete true if you do not want to re-add contact events
     */
    @Synchronized
    fun clearHistory(delete: Boolean) {
        aggregateHistory.clear()
        rawHistory.clear()
        mDirty = false
        if (!delete && !isSwarm && contacts.size == 1)
            aggregateHistory.add(ContactEvent(accountId, contacts[0]))
        clearedSubject.onNext(ArrayList(aggregateHistory))
    }

    @Synchronized
    fun setHistory(loadedConversation: List<Interaction>) {
        mDirty = true
        aggregateHistory.ensureCapacity(loadedConversation.size)
        for (i in loadedConversation) {
            val interaction = getTypedInteraction(i)
            setInteractionProperties(interaction)
            aggregateHistory.add(interaction)
            rawHistory[interaction.timestamp] = interaction
        }
        sortHistory()
    }

    @Synchronized
    fun addElement(interaction: Interaction) {
        setInteractionProperties(interaction)
        when (interaction.type) {
            Interaction.InteractionType.TEXT -> addTextMessage(TextMessage(interaction))
            Interaction.InteractionType.CALL -> addCall(Call(interaction))
            Interaction.InteractionType.CONTACT -> addContactEvent(ContactEvent(interaction))
            Interaction.InteractionType.DATA_TRANSFER -> addFileTransfer(DataTransfer(interaction))
            else -> {}
        }
    }

    /**
     * Adds a swarm interaction to the conversation.
     *
     * @param interaction The interaction to add.
     * @param newMessage  Indicates whether it is a new message.
     */
    @Synchronized
    fun addSwarmElement(interaction: Interaction, newMessage: Boolean) {
        // Handle call interaction
        if (interaction is Call && interaction.confId != null) {
            // interaction.duration is changed when the call is ended.
            // It means duration=0 when the call is started and duration>0 when the call is ended.
            if (interaction.duration != 0L) {
                val startedCall = conferenceStarted.remove(interaction.confId)
                if (startedCall != null) {
                    startedCall.setEnded(interaction)
                    updateInteraction(startedCall)
                }
                else conferenceEnded[interaction.confId!!] = interaction

                // That is the end of the call.
                // Need to be attached to the start of the call.
                val invalidInteraction = // Replacement element
                    Interaction(this, Interaction.InteractionType.INVALID).apply {
                        setSwarmInfo(uri.rawRingId, interaction.messageId!!, interaction.parentId)
                        conversation = this@Conversation
                        contact = interaction.contact
                    }
                addSwarmElement(invalidInteraction, newMessage)
                return
            } else { // Call started but not ended
                val endedCall = conferenceEnded.remove(interaction.confId)
                if (endedCall != null) {
                    interaction.setEnded(endedCall)
                    updateInteraction(endedCall)
                }
                else conferenceStarted[interaction.confId!!] = interaction
            }
        }
        val id = interaction.messageId!!
        mMessages[id] = interaction

        // Update lastDisplayedMessages and lastSent
        interaction.statusMap.entries.forEach {
            val contact = findContact(Uri.fromString(it.key)) ?: return@forEach
            if (!contact.isUser) {
                if (it.value == Interaction.MessageStates.DISPLAYED) {
                    setLastMessageDisplayed(it.key, id)
                }
                if (it.value == Interaction.MessageStates.SUCCESS) {
                    setLastMessageSent(id)
                }
            }
        }

        if (lastRead != null && lastRead == id) interaction.read()
        if (lastNotified != null && lastNotified == id) interaction.isNotified = true
        var newLeaf = false
        var added = false
        if (aggregateHistory.isEmpty() || aggregateHistory.last().messageId == interaction.parentId) {
            // New leaf
            added = true
            newLeaf = true
            aggregateHistory.add(interaction)
            updatedElementSubject.onNext(Pair(interaction, ElementStatus.ADD))
        } else {
            // New root or normal node
            for (i in aggregateHistory.indices) {
                if (id == aggregateHistory[i].parentId) {
                    aggregateHistory.add(i, interaction)
                    updatedElementSubject.onNext(Pair(interaction, ElementStatus.ADD))
                    added = true
                    // Checks if the new interaction is the last non-invalid message.
                    // E.g. could be the case for Call where end call is marked as Invalid.
                    newLeaf = !aggregateHistory.drop(i + 1)
                        .any { it.type != Interaction.InteractionType.INVALID }
                    break
                }
            }
            if (!added) {
                for (i in aggregateHistory.indices.reversed()) {
                    if (aggregateHistory[i].messageId == interaction.parentId) {
                        added = true
                        newLeaf = true
                        aggregateHistory.add(i + 1, interaction)
                        updatedElementSubject.onNext(Pair(interaction, ElementStatus.ADD))
                        break
                    }
                }
            }
        }
        if (newLeaf) {
            if (isVisible) {
                interaction.read()
                setLastMessageRead(id)
            }
            if (interaction.type != Interaction.InteractionType.INVALID)
                lastEvent = interaction
        }
        if (!added) {
            Log.e(TAG, "Unable to attach interaction $id with parent ${interaction.parentId}")
        }
        mPendingMessages.remove(id)?.onSuccess(interaction)
    }

    fun updateFileTransfer(transfer: DataTransfer, eventCode: Interaction.TransferStatus) {
        val dataTransfer = (if (isSwarm) transfer else findConversationElement(transfer.id)) as? DataTransfer
        if (dataTransfer != null) {
            dataTransfer.transferStatus = eventCode
            updatedElementSubject.onNext(Pair(dataTransfer, ElementStatus.UPDATE))
        }
    }

    fun removeInteraction(interaction: Interaction) {
        if (isSwarm) {
            if (removeSwarmInteraction(interaction.messageId!!)) updatedElementSubject.onNext(Pair(interaction, ElementStatus.REMOVE))
        } else {
            if (removeInteraction(interaction.id.toLong())) updatedElementSubject.onNext(Pair(interaction, ElementStatus.REMOVE))
        }
    }

    fun removeAll() {
        aggregateHistory.clear()
        currentCalls.clear()
        rawHistory.clear()
        mDirty = true
    }

    fun setColor(c: Int) {
        color.onNext(c)
    }

    fun setSymbol(s: CharSequence) {
        symbol.onNext(s)
    }

    fun setNotification(enable: Boolean) {
        isNotificationEnabledSubject.onNext(enable)
    }

    fun getColor(): Observable<Int> = color

    fun getSymbol(): Observable<CharSequence> = symbol

    fun updatePreferences(preferences: Map<String, String>) {
        val colorValue = preferences[KEY_PREFERENCE_CONVERSATION_COLOR]
        if (colorValue != null) {
            // First, we remove the string first character (the #).
            // The color format is RRGGBB but we want AARRGGBB.
            // So we add FF in front of the color (full opacity).
            color.onNext(colorValue.substring(1).toInt(16) or 0xFF000000.toInt())
        } else color.onNext(0)
        preferences[KEY_PREFERENCE_CONVERSATION_SYMBOL].let {
            symbol.onNext(if (StringUtils.isOnlyEmoji(it)) it!! else "")
        }
        preferences[KEY_PREFERENCE_CONVERSATION_NOTIFICATION]?.let {
            isNotificationEnabledSubject.onNext(it.toBoolean())
        }?: Log.w(TAG, "No notification preference found")
    }

    /** Tells if the conversation is a swarm:group with more than 2 participants (including user) */
    fun isGroup() = isSwarm && contacts.size > 2

    /** Legacy means that user is consulting a contact */
    fun isLegacy() = mode.blockingFirst() == Mode.Legacy

    /** Syncing means that Jami is attempting to download conversation from peer */
    fun isSyncing() = mode.blockingFirst() == Mode.Syncing

    /** Tells if the conversation is a swarm:group. No matter how many participants. */
    fun isSwarmGroup() = isSwarm && mode.blockingFirst().let {
        if (it == Mode.Request) request?.mode != Mode.OneToOne
        else it != Mode.OneToOne
    }

    /** Return user. Maybe be null. */
    fun getUser(): Contact? = contacts.firstOrNull { it.isUser }

    /** Tells if the user is admin of the group. */
    fun isUserGroupAdmin() =
        if (!isSwarmGroup()) false
        else {
            getUser()?.let { // Get the user contact
                roles[it.uri.uri] == MemberRole.ADMIN
            } ?: false
        }

    @Synchronized
    fun loadMessage(id: String, load: () -> Unit): Single<Interaction> {
        val msg = getMessage(id)
        return if (msg != null) Single.just(msg)
        else mPendingMessages.computeIfAbsent(id) {
            load()
            SingleSubject.create()
        }
    }

    /**
     * Add a reaction in the model.
     * @param reactionInteraction Reaction to add
     * @param reactTo Interaction we are reacting to
     */
    fun addReaction(reactionInteraction: Interaction, reactTo: String) {
        val reactedInteraction = getMessage(reactTo)
        reactedInteraction?.addReaction(reactionInteraction)
    }

    fun removeReaction(reactTo: String, id: String) {
        val interaction = getMessage(reactTo)
        interaction?.removeReaction(id)
    }

    @Synchronized
    fun updateSwarmMessage(interaction: Interaction) {
        val existingInteraction = interaction.messageId?.let { getMessage(it) } ?: return
        interaction.parentId?.let { existingInteraction.updateParent(it) }
        existingInteraction.replaceEdits(interaction.history)
        existingInteraction.replaceReactions(interaction.reactions)
        existingInteraction.body = interaction.body

        if (interaction is DataTransfer && interaction.fileId == "") {
            (existingInteraction as? DataTransfer)?.fileId = interaction.fileId
            existingInteraction.transferStatus = Interaction.TransferStatus.FILE_REMOVED
        }

        updatedElementSubject.onNext(Pair(existingInteraction, ElementStatus.UPDATE))
        if (lastEvent == existingInteraction) {
            lastEventSubject.onNext(existingInteraction)
        }
    }

    data class ActiveCall(val confId: String, val uri: String, val device: String) {
        constructor(map: Map<String, String>) :
                this(map[KEY_CONF_ID]!!, map[KEY_URI]!!, map[KEY_DEVICE]!!)

        companion object {
            const val KEY_CONF_ID = "id"
            const val KEY_URI = "uri"
            const val KEY_DEVICE = "device"
        }
    }

    fun setActiveCalls(activeCalls: List<ActiveCall>) =
        activeCallsSubject.onNext(activeCalls)

    private val conferenceStarted: MutableMap<String, Call> = HashMap()
    private val conferenceEnded: MutableMap<String, Call> = HashMap()

    enum class Mode {
        OneToOne, AdminInvitesOnly, InvitesOnly,  // Non-daemon modes
        Syncing, Public, Legacy, Request;

        val isSwarm: Boolean
            get() = this == OneToOne || this == InvitesOnly || this == Public

        val isGroup: Boolean
            get() = this == AdminInvitesOnly || this == InvitesOnly || this == Public
    }

    interface ConversationActionCallback {
        fun removeConversation(accountId: String, conversationUri: Uri)
        fun clearConversation(accountId: String, conversationUri: Uri)
        fun copyContactNumberToClipboard(contactNumber: String)
    }

    companion object {
        private val TAG = Conversation::class.simpleName!!
        const val KEY_PREFERENCE_CONVERSATION_COLOR = "color"
        const val KEY_PREFERENCE_CONVERSATION_SYMBOL = "symbol"
        const val KEY_PREFERENCE_CONVERSATION_NOTIFICATION = "notification"

        private fun getTypedInteraction(interaction: Interaction) = when (interaction.type) {
            Interaction.InteractionType.TEXT -> TextMessage(interaction)
            Interaction.InteractionType.CALL -> Call(interaction)
            Interaction.InteractionType.CONTACT -> ContactEvent(interaction)
            Interaction.InteractionType.DATA_TRANSFER -> DataTransfer(interaction)
            else -> interaction
        }
    }
}

enum class MemberRole {
    ADMIN, MEMBER, INVITED, BLOCKED, LEFT, UNKNOWN;

    companion object{
        fun fromString(value: String): MemberRole = when(value){
            "admin" -> ADMIN
            "member" -> MEMBER
            "invited" -> INVITED
            "banned" -> BLOCKED
            "left" -> LEFT
            "" -> UNKNOWN
            else -> throw IllegalArgumentException("Unknown member role: $value")
        }
    }
}