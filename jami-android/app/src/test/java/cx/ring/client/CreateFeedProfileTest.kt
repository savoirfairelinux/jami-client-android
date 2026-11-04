package cx.ring.client

import cx.ring.viewmodel.feedCreationProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class CreateFeedProfileTest {
    @Test
    fun feedCreationSavesTheNameAndDescriptionTogether() {
        assertEquals(
            mapOf("title" to "Updates", "description" to "Project announcements"),
            feedCreationProfile("  Updates  ", "  Project announcements  ")
        )
    }

    @Test
    fun descriptionIsOptional() {
        assertEquals(
            mapOf("title" to "Updates", "description" to ""),
            feedCreationProfile("Updates", "   ")
        )
    }
}
