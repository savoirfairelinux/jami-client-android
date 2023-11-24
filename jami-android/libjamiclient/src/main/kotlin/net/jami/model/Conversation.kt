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

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.SingleSubject
import io.reactivex.rxjava3.subjects.Subject
import net.jami.utils.Log
import java.util.*

class Conversation : ConversationHistory {
    val accountId: String
    val uri: Uri
    val contacts: MutableList<Contact>
    private val rawHistory: NavigableMap<Long, Interaction> = TreeMap()
    private val currentCalls = ArrayList<Conference>()
    val aggregateHistory = ArrayList<Interaction>(32)

    private val lastDisplayedMessages: MutableMap<String, String> = HashMap()
    private val updatedElementSubject: Subject<Pair<Interaction, ElementStatus>> = PublishSubject.create()
    private val clearedSubject: Subject<List<Interaction>> = PublishSubject.create()
    private val callsSubject: Subject<List<Conference>> = BehaviorSubject.createDefault(emptyList())
    private val activeCallsSubject: Subject<List<ActiveCall>> =
        BehaviorSubject.createDefault(emptyList())
    private val composingStatusSubject: Subject<Account.ComposingStatus> = BehaviorSubject.createDefault(Account.ComposingStatus.Idle)
    private val color: Subject<Int> = BehaviorSubject.create()
    private val symbol: Subject<CharSequence> = BehaviorSubject.create()
    private val mContactSubject: Subject<List<Contact>> = BehaviorSubject.create()
    var loaded: Single<Conversation>? = null
    val lastElementLoadedSubject = SingleSubject.create<Completable>()
    val lastElementLoaded = lastElementLoadedSubject.flatMapCompletable { it }
    private val mRoots: MutableSet<String> = HashSet(2)
    private val mMessages: MutableMap<String, Interaction> = HashMap(16)
    private val mPendingMessages: MutableMap<String, SingleSubject<Interaction>> = HashMap(8)
    private val mPendingReactions: MutableMap<String, MutableList<Interaction>> = HashMap(8)
    private val mPendingEdits: MutableMap<String, MutableList<Interaction>> = HashMap(8)
    var lastRead: String? = null
        private set
    var lastNotified: String? = null
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

    val activeCallsObservable: Observable<List<ActiveCall>>
        get() = activeCallsSubject

    val composingStatus: Observable<Account.ComposingStatus>
        get() = composingStatusSubject

    val sortedHistory: Single<List<Interaction>> = Single.fromCallable {
        sortHistory()
        aggregateHistory
    }
    var lastEvent: Interaction? = null
        private set(e) {
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

    val swarmRoot: Collection<String>
        get() = mRoots

    constructor(accountId: String, contact: Contact) {
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

    fun addContact(contact: Contact) {
        contacts.add(contact)
        mContactSubject.onNext(contacts)
    }

    fun removeContact(contact: Contact) {
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

    fun stopLoading(): Boolean {
        val ret = mLoadingSubject
        mLoadingSubject = null
        if (ret != null) {
            ret.onSuccess(this)
            return true
        }
        return false
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
                    Log.e(TAG, "Can't set interaction properties: no author for type:" + interaction.type + " id:" + interaction.id + " status:" + interaction.mStatus)
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
        // Remove contact from previous interaction
        lastDisplayedMessages[contactId]?.let { mId ->
            mMessages[mId]?.let { e ->
                e.displayedContacts.remove(contactId)
                updatedElementSubject.onNext(Pair(e, ElementStatus.UPDATE))
            }
        }
        // Add contact to new displayed interaction
        lastDisplayedMessages[contactId] = messageId
        mMessages[messageId]?.let { e ->
            e.displayedContacts.add(contactId)
            updatedElementSubject.onNext(Pair(e, ElementStatus.UPDATE))
        }
    }

    @Synchronized
    fun updateSwarmInteraction(messageId: String, contactUri: Uri, newStatus: Interaction.InteractionStatus) {
        val e = mMessages[messageId] ?: return
        if (newStatus == Interaction.InteractionStatus.DISPLAYED) {
            Log.w(TAG, "updateSwarmInteraction DISPLAYED")
            findContact(contactUri)?.let { contact ->
                if (!contact.isUser)
                    setLastMessageDisplayed(contactUri.host, messageId)
            }
        } else if (newStatus != Interaction.InteractionStatus.SENDING) {
            e.status = newStatus
            updatedElementSubject.onNext(Pair(e, ElementStatus.UPDATE))
        }
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
                Log.e(TAG, "Can't find swarm message to update: ${element.messageId}")
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
            Log.e(TAG, "Can't find message to update: ${element.id}")
        }
    }

    @Synchronized
    fun sortHistory() {
        if (mDirty) {
            aggregateHistory.sortWith { c1, c2 -> c1.timestamp.compareTo(c2.timestamp) }
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
        clearedSubject.onNext(aggregateHistory)
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
    fun addSwarmElement(interaction: Interaction, newMessage: Boolean): Boolean {
        // Handle edit interaction
        if (interaction.edit != null) {
            addEdit(interaction, newMessage)
            val i = Interaction(this, Interaction.InteractionType.INVALID)
            i.setSwarmInfo(uri.rawRingId, interaction.messageId!!, interaction.parentId)
            i.conversation = this
            i.contact = interaction.contact
            return addSwarmElement(i, newMessage)
        }
        // Handle reaction interaction
        else if (interaction.reactToId != null) {
            addReaction(interaction, interaction.reactToId!!)
            val i = Interaction(this, Interaction.InteractionType.INVALID)
            i.setSwarmInfo(uri.rawRingId, interaction.messageId!!, interaction.parentId)
            i.conversation = this
            i.contact = interaction.contact
            i.reactTo = interaction
            return addSwarmElement(i, newMessage)
        }
        // Handle call interaction
        else if (interaction is Call && interaction.confId != null) {
            // interaction.duration is changed when the call is ended.
            // It means duration=0 when the call is started and duration>0 when the call is ended.
            if (interaction.duration != 0L) {
                val startedCall = conferenceStarted.remove(interaction.confId)
                if (startedCall != null) {
                    startedCall.setEnded(interaction)
                    updateInteraction(startedCall)
                }
                else conferenceEnded[interaction.confId!!] = interaction

                val invalidInteraction = // Replacement element
                    Interaction(this, Interaction.InteractionType.INVALID).apply {
                        setSwarmInfo(uri.rawRingId, interaction.messageId!!, interaction.parentId)
                        conversation = this@Conversation
                        contact = interaction.contact
                    }
                return addSwarmElement(invalidInteraction, newMessage)
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
        val previous = mMessages.put(id, interaction)
        val action = if (previous == null) ElementStatus.ADD else ElementStatus.UPDATE
        if (previous != null && interaction.type != Interaction.InteractionType.INVALID) {
            // We update an interaction, but the views might be subscribed to the old model
            // Migrate the observables to the new model
            interaction.updateFrom(previous)
        }
        mRoots.remove(id)
        mPendingReactions.remove(id)?.let { reactions -> interaction.addReactions(reactions) }
        mPendingEdits.remove(id)?.let { edits -> interaction.addEdits(edits) }
        if (interaction.parentId != null && !mMessages.containsKey(interaction.parentId)) {
            mRoots.add(interaction.parentId!!)
            // Log.w(TAG, "@@@ Found new root for " + getUri() + " " + parent + " -> " + mRoots);
        }
        for ((contactId, messageId) in lastDisplayedMessages.entries) {
            if (id == messageId) {
                interaction.displayedContacts.add(contactId)
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
            if (action == ElementStatus.ADD)
                aggregateHistory.add(interaction)
            updatedElementSubject.onNext(Pair(interaction, action))
        } else {
            // New root or normal node
            for (i in aggregateHistory.indices) {
                if (id == aggregateHistory[i].parentId) {
                    //Log.w(TAG, "@@@ New root node at " + i);
                    if (action == ElementStatus.ADD)
                        aggregateHistory.add(i, interaction)
                    updatedElementSubject.onNext(Pair(interaction, action))
                    added = true
                    break
                }
            }
            if (!added) {
                for (i in aggregateHistory.indices.reversed()) {
                    if (aggregateHistory[i].messageId == interaction.parentId) {
                        added = true
                        newLeaf = true
                        if (action == ElementStatus.ADD)
                            aggregateHistory.add(i + 1, interaction)
                        updatedElementSubject.onNext(Pair(interaction, action))
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
            Log.e(TAG, "Can't attach interaction $id with parent ${interaction.parentId}")
        }
        mPendingMessages.remove(id)?.onSuccess(interaction)
        return newLeaf
    }

    fun isLoaded(): Boolean {
        return mMessages.isNotEmpty() && mRoots.isEmpty()
    }

    fun updateFileTransfer(transfer: DataTransfer, eventCode: Interaction.InteractionStatus) {
        val dataTransfer = (if (isSwarm) transfer else findConversationElement(transfer.id)) as DataTransfer?
        if (dataTransfer != null) {
            dataTransfer.status = eventCode
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
        val symbolValue = preferences[KEY_PREFERENCE_CONVERSATION_SYMBOL]
        if (symbolValue != null) {
            symbol.onNext(symbolValue)
        } else symbol.onNext("")
    }

    /** Tells if the conversation is a swarm:group with more than 2 participants (including user) */
    fun isGroup() = isSwarm && contacts.size > 2

    /** Tells if the conversation is a swarm:group. No matter how many participants. */
    fun isSwarmGroup() = isSwarm && mode.blockingFirst() != Mode.OneToOne

    @Synchronized
    fun loadMessage(id: String, load: () -> Unit): Single<Interaction> {
        val msg = getMessage(id)
        return if (msg != null) Single.just(msg)
        else mPendingMessages.computeIfAbsent(id) {
            load()
            SingleSubject.create()
        }
    }

    private fun addEdit(interaction: Interaction, newMessage: Boolean) {
        Log.w(TAG, "addEdit $interaction ${interaction.type} ${(interaction as? TextMessage)?.body}")
        val msg = getMessage(interaction.edit!!).let {
            it?.reactTo ?: it
        }

        if (msg != null)
            msg.addEdit(interaction, newMessage)
        else
            mPendingEdits.computeIfAbsent(interaction.edit!!) { ArrayList() }.let {
                it.remove(interaction)
                if (newMessage)
                    it.add(interaction)
                else
                    it.add(0, interaction)
            }
    }

    /**
     * Add a reaction in the model.
     * @param reactionInteraction Reaction to add
     * @param reactTo Interaction we are reacting to
     */
    private fun addReaction(reactionInteraction: Interaction, reactTo: String) {
        // Connect interaction edit when pending
        mPendingEdits.remove(reactionInteraction.messageId)
            ?.let { edits -> reactionInteraction.addEdits(edits) }
        val reactedInteraction = getMessage(reactTo)
        if (reactedInteraction != null) {
            reactedInteraction.addReaction(reactionInteraction)
        } else
            mPendingReactions.computeIfAbsent(reactTo) { ArrayList() }.add(reactionInteraction)
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
        Syncing, Public, Legacy, Request
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

        private fun getTypedInteraction(interaction: Interaction) = when (interaction.type) {
            Interaction.InteractionType.TEXT -> TextMessage(interaction)
            Interaction.InteractionType.CALL -> Call(interaction)
            Interaction.InteractionType.CONTACT -> ContactEvent(interaction)
            Interaction.InteractionType.DATA_TRANSFER -> DataTransfer(interaction)
            else -> interaction
        }
    }
}