package net.jami.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ContactLabelsTest {
    @Test
    fun labelsRoundTripWithoutUsingTextDelimiters() {
        val labels = listOf(" friends ", "friends", "", "family, work", "caf\u00e9 \ud83c\udfe0", "a\"b")
        val encoded = ContactLabels.encode(labels)
        assertEquals(setOf("friends", "family, work", "caf\u00e9 \ud83c\udfe0", "a\"b"), ContactLabels.decode(encoded))
        assertEquals("[]", ContactLabels.encode(emptySet()))
        assertEquals(emptySet<String>(), ContactLabels.decode(null))
        assertEquals(emptySet<String>(), ContactLabels.decode(""))
    }

    @Test
    fun malformedSnapshotsAreRejectedRatherThanClearingLabels() {
        for (invalid in listOf("null", "{}", "\"friends\"", "[1]", "[null]", "[true]", "[")) {
            assertThrows(RuntimeException::class.java) { ContactLabels.decode(invalid) }
        }
        assertThrows(IllegalArgumentException::class.java) { ContactLabels.encode(setOf("a\u0000b")) }
    }

    @Test
    fun metadataLimitIsMeasuredInUtf8BytesIncludingJson() {
        assertEquals(65536, ContactLabels.encode(setOf("a".repeat(65532))).toByteArray(Charsets.UTF_8).size)
        assertEquals(65536, ContactLabels.encode(setOf("\u00e9".repeat(32766))).toByteArray(Charsets.UTF_8).size)
        assertThrows(IllegalArgumentException::class.java) { ContactLabels.encode(setOf("a".repeat(65533))) }
        assertThrows(IllegalArgumentException::class.java) { ContactLabels.encode(setOf("\u00e9".repeat(32767))) }
    }
}
