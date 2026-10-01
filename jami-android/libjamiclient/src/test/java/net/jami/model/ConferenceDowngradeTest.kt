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
}
