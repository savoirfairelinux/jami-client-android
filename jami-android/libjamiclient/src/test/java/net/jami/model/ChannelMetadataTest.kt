package net.jami.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelMetadataTest {
    private val prefix = ChannelMetadata.PREFIX

    @Test
    fun defaultsAreVirtualAndHaveTheSameIdentityOnEveryDevice() {
        val first = ChannelMetadata.channels(emptyMap())
        val second = ChannelMetadata.channels(emptyMap())
        assertEquals(first, second)
        assertEquals(listOf("All", "Friends", "Real Friends", "Family", "Colleague"), first.map { it.name })
        assertEquals("all", first.first().id)
        assertTrue(first.first().containsContact("jami:any-contact"))
    }

    @Test
    fun allIsImplicitEvenIfRemoteMetadataAttemptsToChangeIt() {
        val metadata = mapOf("${prefix}all/deleted" to "1", "${prefix}all/name" to "Other")
        val all = ChannelMetadata.channels(metadata).first()
        assertEquals("All", all.name)
        assertTrue(all.isAllContacts)
        assertTrue(all.containsContact("jami:new-contact"))
        expectInvalid { ChannelMetadata.rename(metadata, all.id, "Renamed") }
        expectInvalid { ChannelMetadata.delete(metadata, all.id) }
        expectInvalid { ChannelMetadata.setMemberships(metadata, "jami:peer", false, mapOf(all.id to false)) }
    }

    @Test
    fun customChannelKeepsItsIdentifierAndMembershipAcrossRename() {
        var state = ChannelMetadata.create(emptyMap(), "Trip", "trip")
        state = state + ChannelMetadata.setMemberships(state, "jami:alice", false, mapOf("trip" to true))
        state = state + ChannelMetadata.rename(state, "trip", "Summer")
        val channel = ChannelMetadata.channels(state).single { it.id == "trip" }
        assertEquals("Summer", channel.name)
        assertEquals(setOf("jami:alice"), channel.members)
        assertFalse(channel.isAllContacts)
    }

    @Test
    fun defaultChannelsOtherThanAllCanBeRenamedAndDeleted() {
        val renamed = ChannelMetadata.rename(emptyMap(), "default-friends", "Pals")
        val channel = ChannelMetadata.channels(renamed).single { it.id == "default-friends" }
        assertEquals("Pals", channel.name)
        assertFalse(channel.isAllContacts)
        val deleted = renamed + ChannelMetadata.delete(renamed, channel.id)
        assertFalse(ChannelMetadata.channels(deleted).any { it.id == channel.id })
    }

    @Test
    fun deletionWinsOverOfflineRenameAndMembershipChanges() {
        val initial = ChannelMetadata.create(emptyMap(), "Trip", "trip")
        val removed = ChannelMetadata.delete(initial, "trip")
        val offlineRename = ChannelMetadata.rename(initial, "trip", "Holiday")
        val offlineMember = ChannelMetadata.setMemberships(initial, "jami:alice", false, mapOf("trip" to true))
        assertFalse(ChannelMetadata.channels(initial + removed + offlineRename + offlineMember).any { it.id == "trip" })
        assertFalse(ChannelMetadata.channels(initial + offlineMember + offlineRename + removed).any { it.id == "trip" })
    }

    @Test
    fun deletingThenRecreatingANameUsesANewIdentity() {
        val initial = ChannelMetadata.create(emptyMap(), "Trip", "trip")
        val deleted = initial + ChannelMetadata.delete(initial, "trip")
        expectInvalid { ChannelMetadata.create(deleted, "Trip", "trip") }
        val recreated = deleted + ChannelMetadata.create(deleted, "Trip", "new-trip")
        assertEquals("new-trip", ChannelMetadata.channels(recreated).single { it.name == "Trip" }.id)
    }

    @Test
    fun disjointMembershipEditsDoNotOverwriteEachOther() {
        val first = ChannelMetadata.setMemberships(emptyMap(), "jami:alice", false, mapOf("default-friends" to true))
        val second = ChannelMetadata.setMemberships(emptyMap(), "jami:bob", false, mapOf("default-friends" to true))
        val combined = ChannelMetadata.channels(first + second).single { it.id == "default-friends" }
        assertEquals(setOf("jami:alice", "jami:bob"), combined.members)
        assertEquals(ChannelMetadata.channels(first + second), ChannelMetadata.channels(second + first))
    }

    @Test
    fun contactAndGroupWithTheSameUriHaveIndependentMemberships() {
        val uri = "swarm:conversation"
        val contact = ChannelMetadata.setMemberships(emptyMap(), uri, false, mapOf("default-family" to true))
        val group = ChannelMetadata.setMemberships(emptyMap(), uri, true, mapOf("default-family" to true))
        val removeContact = ChannelMetadata.setMemberships(contact + group, uri, false, mapOf("default-family" to false))
        val channel = ChannelMetadata.channels(contact + group + removeContact).single { it.id == "default-family" }
        assertTrue(channel.members.isEmpty())
        assertEquals(setOf(uri), channel.groups)
        assertEquals("0", removeContact.values.single())
    }

    @Test
    fun membershipUpdateDoesNotTouchChannelsNotEditedInDialog() {
        val remote = ChannelMetadata.setMemberships(emptyMap(), "jami:alice", false, mapOf("default-family" to true))
        val local = ChannelMetadata.setMemberships(remote, "jami:alice", false, mapOf("default-friends" to true))
        assertEquals(1, local.size)
        val channels = ChannelMetadata.channels(remote + local)
        assertTrue(channels.single { it.id == "default-family" }.containsContact("jami:alice"))
        assertTrue(channels.single { it.id == "default-friends" }.containsContact("jami:alice"))
    }

    @Test
    fun unicodeNamesAndUrisRoundTripWithoutDependingOnLocale() {
        val name = "Famille \u00e9largie \ud83d\udc6a"
        val uri = "sip:\u00e9lise@example.com;transport=tls"
        val initial = ChannelMetadata.create(emptyMap(), name, "unicode")
        val membership = ChannelMetadata.setMemberships(initial, uri, false, mapOf("unicode" to true))
        val channel = ChannelMetadata.channels(initial + membership).single { it.id == "unicode" }
        assertEquals(name, channel.name)
        assertEquals(setOf(uri), channel.members)
        assertTrue(membership.keys.all { key -> key.all { it.code < 128 } })
    }

    @Test
    fun unknownMetadataAndIncompleteRemoteRecordsDoNotInventChannels() {
        val metadata = mapOf("other.feature/key" to "value", "${prefix}pending/members/abc" to "0")
        assertEquals(ChannelMetadata.defaults, ChannelMetadata.channels(metadata))
    }

    @Test
    fun offlineConcurrentCreationsWithTheSameNameRemainSeparate() {
        val first = ChannelMetadata.create(emptyMap(), "Trip", "first")
        val second = ChannelMetadata.create(emptyMap(), "Trip", "second")
        val channels = ChannelMetadata.channels(first + second).filter { it.name == "Trip" }
        assertEquals(2, channels.size)
        assertNotEquals(channels[0].id, channels[1].id)
    }

    @Test
    fun localDuplicateBlankAndMissingChannelActionsAreRejected() {
        expectInvalid { ChannelMetadata.create(emptyMap(), " Friends ") }
        expectInvalid { ChannelMetadata.create(emptyMap(), "  ") }
        expectInvalid { ChannelMetadata.create(emptyMap(), "Trip", "../escape") }
        expectInvalid { ChannelMetadata.rename(emptyMap(), "missing", "Trip") }
        expectInvalid { ChannelMetadata.delete(emptyMap(), "missing") }
        expectInvalid { ChannelMetadata.setMemberships(emptyMap(), "jami:peer", false, mapOf("missing" to true)) }
    }

    @Test
    fun unchangedActionsDoNotGenerateNetworkUpdates() {
        assertTrue(ChannelMetadata.rename(emptyMap(), "default-friends", "Friends").isEmpty())
        assertTrue(ChannelMetadata.setMemberships(emptyMap(), "jami:peer", false, mapOf("default-friends" to false)).isEmpty())
    }

    @Test
    fun legacyDevicesGenerateTheSameStableIds() {
        val legacy = """[
            {"name":"Contact","builtIn":true},
            {"name":"Friend","builtIn":true,"members":["jami:alice"]},
            {"name":"Trip","builtIn":false,"groups":["swarm:group"]}
        ]"""
        val first = ChannelMetadata.legacyChannels(legacy)
        val second = ChannelMetadata.legacyChannels(legacy)
        assertEquals(first, second)
        assertEquals("all", first[0].id)
        assertEquals("default-friends", first[1].id)
        val imported = ChannelMetadata.channels(ChannelMetadata.migrate(first, emptyMap()))
        assertEquals(listOf("All", "Friends", "Trip"), imported.map { it.name })
        assertEquals(setOf("jami:alice"), imported[1].members)
        assertEquals(setOf("swarm:group"), imported[2].groups)
    }

    @Test
    fun legacyMigrationIsIdempotentAndDoesNotResetRemoteEdits() {
        val legacy = ChannelMetadata.legacyChannels("""[
            {"name":"All","builtIn":true},
            {"name":"Friends","builtIn":true,"members":["jami:alice"]}
        ]""")
        val imported = ChannelMetadata.migrate(legacy, emptyMap())
        val remote = imported +
            ChannelMetadata.rename(imported, "default-friends", "Pals") +
            ChannelMetadata.setMemberships(imported, "jami:alice", false, mapOf("default-friends" to false))
        assertTrue(ChannelMetadata.migrate(legacy, remote).isEmpty())
        assertEquals("Pals", ChannelMetadata.channels(remote).single { it.id == "default-friends" }.name)
        assertTrue(ChannelMetadata.channels(remote).single { it.id == "default-friends" }.members.isEmpty())
    }

    @Test
    fun legacyMigrationCannotResurrectADeletedChannel() {
        val legacy = ChannelMetadata.legacyChannels("""[{"name":"Trip","members":["jami:alice"]}]""")
        val channel = legacy.single()
        val tombstone = mapOf("${prefix}${channel.id}/deleted" to "1")
        val imported = ChannelMetadata.migrate(legacy, tombstone)
        assertTrue(imported.keys.none { it.startsWith("$prefix${channel.id}/") })
        assertFalse(ChannelMetadata.channels(tombstone + imported).any { it.id == channel.id })
    }

    @Test
    fun legacyMissingDefaultDoesNotDeleteAnAlreadyCustomizedRemoteDefault() {
        val remote = ChannelMetadata.rename(emptyMap(), "default-family", "Relatives")
        val legacy = ChannelMetadata.legacyChannels("""[{"name":"All","builtIn":true}]""")
        val imported = ChannelMetadata.migrate(legacy, remote)
        assertTrue(ChannelMetadata.channels(remote + imported).any { it.name == "Relatives" })
    }

    private fun expectInvalid(action: () -> Any) {
        try {
            action()
            throw AssertionError("Expected an invalid Channel operation to fail")
        } catch (_: IllegalArgumentException) {
            // Expected validation error, rather than a partial or silently ignored update.
        }
    }
}
