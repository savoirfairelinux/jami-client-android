package net.jami.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ConferenceTest {
    private fun participant(id: String) =
        Call("account", id, Uri.fromString("jami:peer-$id"), false)
            .apply { confId = "conference" }

    private fun conferenceWithTwoParticipants(created: Boolean = true) =
        Conference("account", "conference").apply {
            setState("ACTIVE_ATTACHED")
            addParticipant(participant("one"))
            addParticipant(participant("two"))
            if (created) initialize("", emptyList())
        }

    @Test
    fun ordinaryConferenceCanCollapseAfterOneParticipantLeaves() {
        val conference = conferenceWithTwoParticipants()

        assertFalse(conference.canCollapseToSimpleCall)
        conference.removeParticipant(conference.participants.last())

        assertTrue(conference.canCollapseToSimpleCall)
    }

    @Test
    fun swarmConferenceKeepsItsIdentityWithOneOrNoRemoteParticipants() {
        val conference = conferenceWithTwoParticipants().apply {
            conversationId = "swarm-conversation"
        }
        val host = Call("account", null, Uri.fromString("swarm:swarm-conversation"), false)
            .apply { confId = conference.id }
        conference.hostCall = host
        val remainingCall = conference.participants.first()

        conference.removeParticipant(conference.participants.last())

        assertFalse(conference.canCollapseToSimpleCall)
        assertSame(host, conference.hostCall)
        assertSame(remainingCall, conference.participants.single())
        assertEquals(conference.id, remainingCall.confId)
        assertNull(conference.call)

        conference.removeParticipant(remainingCall)

        assertFalse(conference.canCollapseToSimpleCall)
        assertSame(host, conference.hostCall)
        assertTrue(conference.isOnGoing)
    }

    @Test
    fun swarmConferenceWithoutHostConnectionMustNotCollapse() {
        val conference = conferenceWithTwoParticipants().apply {
            conversationId = "swarm-conversation"
        }
        conference.removeParticipant(conference.participants.last())

        assertNull(conference.hostCall)
        assertFalse(conference.canCollapseToSimpleCall)
    }

    @Test
    fun hostConnectionProtectsConferenceBeforeConversationIdIsKnown() {
        val conference = conferenceWithTwoParticipants().apply {
            hostCall = Call("account", null, Uri.fromString("rdv:host"), false)
        }
        conference.removeParticipant(conference.participants.last())

        assertNull(conference.conversationId)
        assertFalse(conference.canCollapseToSimpleCall)
    }

    @Test
    fun heldSwarmConferenceMustNotCollapse() {
        val conference = conferenceWithTwoParticipants().apply {
            conversationId = "swarm-conversation"
            setState("HOLD")
        }
        conference.removeParticipant(conference.participants.last())

        assertFalse(conference.canCollapseToSimpleCall)
    }

    @Test
    fun emptyOrdinaryConferenceCannotBecomeASimpleCall() {
        val conference = Conference("account", "conference")

        assertFalse(conference.canCollapseToSimpleCall)
    }

    @Test
    fun changedBeforeCreatedKeepsHostedConferenceAndItsRemainingCall() {
        val conference = conferenceWithTwoParticipants(created = false)
        val remaining = conference.participants.first()
        val removed = conference.participants.last()
        val conferences = mutableMapOf(conference.id to conference)

        // Changed arrives twice before Created: two participants, then only one.
        conference.removeParticipant(removed)
        removed.confId = null
        conference.collapsePending = true
        assertFalse(conference.canCollapseToSimpleCall)
        assertSame(conference, conferences[conference.id])
        assertEquals(conference.id, remaining.confId)

        conference.initialize("swarm-conversation", listOf(remaining))
        assertFalse(conference.canCollapseToSimpleCall)
        assertEquals("swarm-conversation", conference.conversationId)
        assertEquals(listOf(remaining), conference.participants)
        assertSame(conference, conferences[conference.id])
        val host = Call("account", null, Uri.fromString("swarm:swarm-conversation"), false)
        conference.hostCall = host
        conference.initialize("swarm-conversation", listOf(remaining))
        assertSame(host, conference.hostCall)
        assertEquals(1, conference.participants.size)
    }

    @Test
    fun pendingOrdinaryConferenceCanCollapseOnlyAfterItsCreationIsKnown() {
        val conference = conferenceWithTwoParticipants(created = false)
        conference.removeParticipant(conference.participants.last())
        conference.collapsePending = true
        assertFalse(conference.canCollapseToSimpleCall)

        conference.initialize("", conference.participants.toList())

        assertTrue(conference.canCollapseToSimpleCall)
        assertTrue(conference.collapsePending)
        assertEquals(1, conference.participants.size)
    }

    @Test
    fun lateCreationDoesNotDuplicateParticipantsAlreadyAddedByChanged() {
        val conference = conferenceWithTwoParticipants(created = false)
        val participants = conference.participants.toList()
        conference.initialize("swarm-conversation", participants)
        conference.initialize("swarm-conversation", participants)
        assertEquals(participants, conference.participants)
        assertTrue(participants.all { it.confId == conference.id })
    }
}
