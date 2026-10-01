package net.jami.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConferenceDowngradeTest {
    @Test
    fun hostedSwarmRemainsAConferenceWithOneRemoteLeg() {
        val conference = Conference("account", "conference")
        conference.conversationId = "t30"
        conference.addParticipant(Call("account", "leg", Uri.fromId("peer"), true))

        assertFalse(conference.canDowngradeToCall)
    }

    @Test
    fun localHostRemainsAConferenceWithOneRemoteLeg() {
        val conference = Conference("account", "conference")
        conference.hostCall = Call("account", null, Uri.fromId("self"), false)
        conference.addParticipant(Call("account", "leg", Uri.fromId("peer"), true))

        assertFalse(conference.canDowngradeToCall)
    }

    @Test
    fun legacyConferenceCanDowngradeWhenOneRemoteLegRemains() {
        val conference = Conference("account", "conference")
        conference.addParticipant(Call("account", "leg", Uri.fromId("peer"), true))

        assertTrue(conference.canDowngradeToCall)
    }

    @Test
    fun emptyConferenceCannotDowngradeToACall() {
        assertFalse(Conference("account", "conference").canDowngradeToCall)
    }

    @Test
    fun directCallContinuesTheSurvivingLegacyConferenceLeg() {
        val call = Call("account", "leg", Uri.fromId("peer"), true)
        call.confId = "conference"
        val conference = Conference("account", "conference")
        conference.addParticipant(call)
        call.confId = null

        assertTrue(Conference(call).isDowngradeOf(conference))
    }

    @Test
    fun differentCallCannotReplaceTheTrackedConference() {
        val conference = Conference("account", "conference")
        conference.addParticipant(Call("account", "leg", Uri.fromId("peer"), true))
        val unrelatedCall = Call("account", "leg", Uri.fromId("peer"), true)

        assertFalse(Conference(unrelatedCall).isDowngradeOf(conference))
    }

    @Test
    fun hostedSwarmCannotSwitchToAParticipantCall() {
        val call = Call("account", "leg", Uri.fromId("peer"), true)
        val conference = Conference("account", "conference")
        conference.conversationId = "t30"
        conference.addParticipant(call)

        assertFalse(Conference(call).isDowngradeOf(conference))
    }

    @Test
    fun conferenceWithTwoLegsCannotSwitchToOneOfThem() {
        val firstCall = Call("account", "first", Uri.fromId("peer"), true)
        val conference = Conference("account", "conference")
        conference.addParticipant(firstCall)
        conference.addParticipant(Call("account", "second", Uri.fromId("other"), true))

        assertFalse(Conference(firstCall).isDowngradeOf(conference))
    }
}
