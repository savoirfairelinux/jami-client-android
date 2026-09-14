package net.jami.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactBlockingPolicyTest {
    private val own = "0123456789abcdef0123456789abcdef01234567"
    private val peer = "abcdef0123456789abcdef0123456789abcdef01"

    @Test
    fun ownIdentityIsRejectedRegardlessOfUriSpelling() {
        for (target in listOf(own, "ring:$own", "jami:$own", "jami:${own.uppercase()}")) {
            assertFalse(ContactBlockingPolicy.canBlock(own, Uri.fromString(target), true))
        }
        assertFalse(ContactBlockingPolicy.canBlock("jami:$own", Uri.fromString(own), true))
    }

    @Test
    fun externalContactsAreStillBlockable() {
        assertTrue(ContactBlockingPolicy.canBlock(own, Uri.fromString("jami:$peer"), true))
        assertTrue(ContactBlockingPolicy.canBlock(null, Uri.fromString("sip:peer@example.com"), false))
    }

    @Test
    fun missingAccountIdentityAndConversationUrisAreNotBlockTargets() {
        assertFalse(ContactBlockingPolicy.canBlock(null, Uri.fromString(peer), true))
        assertFalse(ContactBlockingPolicy.canBlock("", Uri.fromString(peer), true))
        assertFalse(ContactBlockingPolicy.canBlock(own, Uri(Uri.SWARM_SCHEME, peer), true))
    }

    @Test
    fun soleSelfParticipantCannotBecomeTheBlockingTarget() {
        val conversation = Conversation("account", Uri(Uri.SWARM_SCHEME, "conversation"), Conversation.Mode.Syncing)
        val self = Contact(Uri.fromId(own), true)
        conversation.addContact(self)
        assertSame(self, conversation.contact)
        assertNull(ContactBlockingPolicy.target(conversation, own, true))
    }

    @Test
    fun canonicalIdentityProtectsAgainstAnOutdatedIsUserFlag() {
        val self = Contact(Uri.fromId(own), false)
        val conversation = Conversation("account", self)
        assertNull(ContactBlockingPolicy.target(conversation, own, true))
    }

    @Test
    fun loadedDirectConversationTargetsOnlyTheExternalPeer() {
        val conversation = Conversation("account", Uri(Uri.SWARM_SCHEME, "conversation"), Conversation.Mode.OneToOne)
        val contact = Contact(Uri.fromId(peer))
        conversation.addContact(Contact(Uri.fromId(own), true))
        conversation.addContact(contact)
        assertSame(contact, ContactBlockingPolicy.target(conversation, own, true))
    }

    @Test
    fun groupMembershipDoesNotBecomeAnImplicitBlockTarget() {
        val conversation = Conversation("account", Uri(Uri.SWARM_SCHEME, "conversation"), Conversation.Mode.InvitesOnly)
        conversation.addContact(Contact(Uri.fromId(peer)))
        assertNull(ContactBlockingPolicy.target(conversation, own, true))
    }

    @Test
    fun groupInvitationTargetsItsExternalSenderNotTheAccount() {
        val conversation = Conversation("account", Uri(Uri.SWARM_SCHEME, "conversation"), Conversation.Mode.Request)
        val contact = Contact(Uri.fromId(peer))
        conversation.request = TrustRequest("account", contact.uri, 0, conversation.uri, mode = Conversation.Mode.InvitesOnly)
        conversation.addContact(contact)
        assertSame(contact, ContactBlockingPolicy.target(conversation, own, true))
    }
}
