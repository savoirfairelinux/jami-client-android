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

    private fun conferenceWithTwoParticipants() =
        Conference("account", "conference").apply {
            setState("ACTIVE_ATTACHED")
            addParticipant(participant("one"))
            addParticipant(participant("two"))
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
}
