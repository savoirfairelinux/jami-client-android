package net.jami.model

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.SingleSubject
import net.jami.model.ConversationActionsState.BlockAction
import net.jami.model.ConversationActionsState.DeleteAction
import net.jami.model.ConversationActionsState.Type
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationActionsStateTest {
    private val own = "0123456789abcdef0123456789abcdef01234567"
    private val peer = "abcdef0123456789abcdef0123456789abcdef01"

    private fun account(sip: Boolean = false) = Account(
        "account",
        mapOf(
            ConfigKey.ACCOUNT_TYPE.key to if (sip) AccountConfig.ACCOUNT_TYPE_SIP else AccountConfig.ACCOUNT_TYPE_JAMI,
            ConfigKey.ACCOUNT_USERNAME.key to own
        ),
        emptyList(),
        mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to AccountConfig.RegistrationState.REGISTERED.name)
    )

    private fun conversation(mode: Conversation.Mode = Conversation.Mode.OneToOne) =
        Conversation("account", Uri(Uri.SWARM_SCHEME, "conversation"), mode)

    private fun block(account: Account) =
        account.addContact(mapOf("id" to peer, "banned" to "true"))

    @Test
    fun unresolvedThenBlockedThenUnblockedRendersEveryControlFromMutableContactUpdates() {
        val account = account()
        val conversation = conversation()
        val scheduler = TestScheduler()
        val observer = ConversationActionsState.observe(conversation, Observable.just(listOf(account)))
            .observeOn(scheduler).test()

        val contact = block(account)
        conversation.addContact(contact)
        account.addContact(peer, true)
        assertSame(contact, account.getContact(contact.uri))
        assertSame(contact, conversation.contact)

        scheduler.triggerActions()
        observer.assertNoErrors().assertValueCount(3)
        val (unresolved, blocked, unblocked) = observer.values()
        assertEquals(BlockAction.NONE, unresolved.blockAction)
        assertFalse(unresolved.showPrivate)
        assertEquals(BlockAction.UNBLOCK, blocked.blockAction)
        assertFalse(blocked.showDelete)
        assertFalse(blocked.showRemove)
        assertFalse(blocked.showDetails)
        assertFalse(blocked.showActions)
        assertEquals(BlockAction.BLOCK, unblocked.blockAction)
        assertEquals(DeleteAction.DELETE_CONTACT, unblocked.deleteAction)
        assertTrue(unblocked.showDelete)
        assertTrue(unblocked.showRemove)
        assertTrue(unblocked.showDetails)
        assertTrue(unblocked.showActions)
        assertTrue(unblocked.showPrivate)
        assertFalse(unblocked.showDescription)
        observer.dispose()
    }

    @Test
    fun canonicalAccountStatusWinsOverAnOlderConversationContactInstance() {
        val account = account()
        val contact = Contact(Uri.fromId(peer))
        val conversation = conversation().apply { addContact(contact) }
        val observer = ConversationActionsState.observe(conversation, Observable.just(listOf(account))).test()

        block(account)
        assertFalse(contact.isBlocked)
        assertEquals(BlockAction.UNBLOCK, observer.values().last().blockAction)
        account.addContact(peer, true)
        assertEquals(BlockAction.BLOCK, observer.values().last().blockAction)
        observer.assertNoErrors()
        observer.dispose()
    }

    @Test
    fun directRequestKeepsBlockAndAcceptRulesAcrossStatusChangesAndAcceptance() {
        val account = account()
        val contact = account.getContactFromCache(Uri.fromId(peer))
        val conversation = conversation(Conversation.Mode.Request).apply {
            request = TrustRequest("account", contact.uri, 0, uri, mode = Conversation.Mode.OneToOne)
            addContact(contact)
        }
        val observer = ConversationActionsState.observe(conversation, Observable.just(listOf(account))).test()
        val request = observer.values().last()
        assertEquals(DeleteAction.ACCEPT_INVITATION, request.deleteAction)
        assertEquals(BlockAction.BLOCK, request.blockAction)
        assertTrue(request.showDelete)
        assertTrue(request.showDetails)
        assertFalse(request.showActions)
        assertFalse(request.showRemove)

        block(account)
        val blockedRequest = observer.values().last()
        assertEquals(BlockAction.BLOCK, blockedRequest.blockAction)
        assertFalse(blockedRequest.showDelete)
        assertFalse(blockedRequest.showDetails)
        account.addContact(peer, true)
        assertTrue(observer.values().last().showDelete)
        conversation.setMode(Conversation.Mode.OneToOne)
        val accepted = observer.values().last()
        assertEquals(DeleteAction.DELETE_CONTACT, accepted.deleteAction)
        assertTrue(accepted.showActions)
        assertTrue(accepted.showRemove)
        observer.assertNoErrors()
        observer.dispose()
    }

    @Test
    fun sipAndLegacyContactsNeverExposeSwarmActions() {
        for (sip in listOf(false, true)) {
            val account = account(sip)
            val contact = if (sip) Contact.buildSIP(Uri.fromString("sip:peer@example.com"))
                else account.getContactFromCache(Uri.fromId(peer))
            val conversation = Conversation("account", contact)
            val observer = ConversationActionsState.observe(conversation, Observable.just(listOf(account))).test()
            for (status in listOf(Contact.Status.BLOCKED, Contact.Status.CONFIRMED)) {
                contact.status = status
                conversation.addContact(contact)
                val state = observer.values().last()
                assertEquals(Type.CONTACT, state.type)
                assertEquals(BlockAction.NONE, state.blockAction)
                assertEquals(DeleteAction.ADD_CONTACT, state.deleteAction)
                assertEquals(!sip, state.showDelete)
                assertFalse(state.showRemove)
                assertFalse(state.showDetails)
                assertFalse(state.showActions)
                assertFalse(state.showDescription)
                assertTrue(state.showPrivate)
            }
            observer.assertNoErrors()
            observer.dispose()
        }
    }

    @Test
    fun syncingUsesAuthoritativeRequestModeAndRestoresDirectControlsWhenLoaded() {
        val account = account()
        val contact = account.getContactFromCache(Uri.fromId(peer))
        val conversation = conversation(Conversation.Mode.Syncing).apply {
            requestMode = Conversation.Mode.OneToOne
            request = TrustRequest("account", contact.uri, 0, uri, mode = Conversation.Mode.InvitesOnly)
            addContact(contact)
        }
        val observer = ConversationActionsState.observe(conversation, Observable.just(listOf(account))).test()
        val group = observer.values().last()
        assertEquals(Type.GROUP, group.type)
        assertEquals(DeleteAction.LEAVE_CONVERSATION, group.deleteAction)
        assertEquals(BlockAction.NONE, group.blockAction)
        assertFalse(group.showPrivate)
        assertFalse(group.showUsername)
        assertFalse(group.showRemove)
        assertTrue(group.showDescription)

        conversation.setMode(Conversation.Mode.Request)
        val invitation = observer.values().last()
        assertEquals(Type.GROUP, invitation.type)
        assertEquals(DeleteAction.ACCEPT_INVITATION, invitation.deleteAction)
        assertFalse(invitation.showActions)
        assertTrue(invitation.showDelete)

        conversation.setMode(Conversation.Mode.OneToOne)
        val direct = observer.values().last()
        assertEquals(Type.PRIVATE, direct.type)
        assertEquals(DeleteAction.DELETE_CONTACT, direct.deleteAction)
        assertEquals(BlockAction.BLOCK, direct.blockAction)
        assertTrue(direct.showPrivate)
        assertTrue(direct.showRemove)
        assertTrue(direct.showActions)
        assertFalse(direct.showDescription)
        observer.assertNoErrors()
        observer.dispose()
    }

    @Test
    fun lateUsernameCannotRestoreBlockedDetailsOrActions() {
        val account = account()
        val username = SingleSubject.create<String>()
        val contact = account.getContactFromCache(Uri.fromId(peer)).apply { this.username = username }
        val conversation = conversation().apply { addContact(contact) }
        val observer = ConversationActionsState.observe(conversation, Observable.just(listOf(account))).test()

        block(account)
        username.onSuccess("peer-name")
        val state = observer.values().last()
        assertEquals("peer-name", state.registeredName)
        assertEquals(BlockAction.UNBLOCK, state.blockAction)
        assertFalse(state.showDetails)
        assertFalse(state.showActions)
        assertFalse(state.showDelete)
        assertFalse(state.showRemove)
        observer.assertNoErrors()
        observer.dispose()
    }

    @Test
    fun switchingToGroupCancelsOldUsernameAndRemovingContactClearsIdentity() {
        val account = account()
        val username = SingleSubject.create<String>()
        val contact = account.getContactFromCache(Uri.fromId(peer)).apply { this.username = username }
        val conversation = conversation().apply { addContact(contact) }
        val observer = ConversationActionsState.observe(conversation, Observable.just(listOf(account))).test()
        assertTrue(username.hasObservers())

        conversation.setMode(Conversation.Mode.InvitesOnly)
        assertFalse(username.hasObservers())
        username.onSuccess("obsolete-name")
        assertEquals(Type.GROUP, observer.values().last().type)
        assertFalse(observer.values().last().showUsername)
        assertFalse(observer.values().last().showPrivate)

        conversation.setMode(Conversation.Mode.OneToOne)
        assertTrue(observer.values().last().showUsername)
        conversation.removeContact(contact)
        val unresolved = observer.values().last()
        assertNull(unresolved.contactUri)
        assertFalse(unresolved.showUsername)
        assertFalse(unresolved.showPrivate)
        assertEquals(BlockAction.NONE, unresolved.blockAction)
        observer.assertNoErrors()
        observer.dispose()
    }

    @Test
    fun replacementUsernameSourceCancelsOldLookupWithoutReplacingTheContact() {
        val account = account()
        val oldName = SingleSubject.create<String>()
        val newName = SingleSubject.create<String>()
        val contact = account.getContactFromCache(Uri.fromId(peer)).apply { username = oldName }
        val conversation = conversation().apply { addContact(contact) }
        val observer = ConversationActionsState.observe(conversation, Observable.just(listOf(account))).test()

        contact.username = newName
        conversation.addContact(contact)
        assertFalse(oldName.hasObservers())
        newName.onSuccess("current-name")
        oldName.onSuccess("obsolete-name")
        assertEquals("current-name", observer.values().last().registeredName)
        assertTrue(observer.values().last().showUsername)
        observer.assertNoErrors()
        observer.dispose()
    }

    @Test
    fun accountReplacementSwitchesStatusSubscriptionAndMissingAccountHidesBlock() {
        val first = account()
        val replacement = account()
        val contact = first.getContactFromCache(Uri.fromId(peer))
        val conversation = conversation().apply { addContact(contact) }
        val accounts = BehaviorSubject.createDefault(listOf(first))
        val observer = ConversationActionsState.observe(conversation, accounts).test()

        block(replacement)
        accounts.onNext(listOf(replacement))
        assertEquals(BlockAction.UNBLOCK, observer.values().last().blockAction)
        replacement.addContact(peer, true)
        assertEquals(BlockAction.BLOCK, observer.values().last().blockAction)
        val count = observer.values().size
        block(first)
        assertEquals(count, observer.values().size)
        accounts.onNext(emptyList())
        assertEquals(BlockAction.NONE, observer.values().last().blockAction)
        observer.assertNoErrors()
        observer.dispose()
    }
}
