package cx.ring.views

import android.app.Application
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.view.isVisible
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import cx.ring.R
import net.jami.smartlist.ConversationFilter
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28, 34])
class ConversationFilterChipsTest {
    private lateinit var controller: ActivityController<AppCompatActivity>
    private lateinit var group: ChipGroup
    private lateinit var filters: ConversationFilterChips
    private val selections = mutableListOf<ConversationFilter>()
    private var creations = 0

    @Before
    fun setUp() {
        controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.AppTheme)
        controller.setup()
        val view = LayoutInflater.from(controller.get()).inflate(R.layout.view_conversation_filters, null)
        controller.get().setContentView(view)
        group = view.findViewById(R.id.conversation_filters)
        filters = ConversationFilterChips(group, { selections.add(it) }, { creations++ })
    }

    @After
    fun tearDown() {
        controller.pause().stop().destroy()
    }

    @Test
    fun labelsShareOneSingleSelectionRowWithTypesAndTheAddButtonStaysLast() {
        filters.render(listOf("family", "friends"), ConversationFilter.All)
        assertTrue(group.isSingleLine)
        assertTrue(group.isSingleSelection)
        assertEquals(
            listOf(ConversationFilter.All, ConversationFilter.Contacts, ConversationFilter.Groups, ConversationFilter.Feeds,
                ConversationFilter.Label("family"), ConversationFilter.Label("friends")),
            group.children.mapNotNull { it.tag as? ConversationFilter }.toList()
        )
        assertEquals(R.id.add_contact_label, group.getChildAt(group.childCount - 1).id)
        chip(ConversationFilter.Label("friends")).performClick()
        assertEquals(listOf(ConversationFilter.Label("friends")), selections)
        assertFalse(chip(ConversationFilter.All).isChecked)
        chip(ConversationFilter.Groups).performClick()
        assertEquals(ConversationFilter.Groups, selections.last())
        assertFalse(chip(ConversationFilter.Label("friends")).isChecked)
        assertEquals(listOf(R.id.filter_groups), group.checkedChipIds)
    }

    @Test
    fun addButtonCreatesWithoutChangingTheSelectedFilterEvenWithNoLabels() {
        filters.render(emptyList(), ConversationFilter.Contacts)
        group.findViewById<View>(R.id.add_contact_label).performClick()
        assertEquals(1, creations)
        assertTrue(selections.isEmpty())
        assertTrue(chip(ConversationFilter.Contacts).isChecked)
    }

    @Test
    fun liveLabelUpdatesRetainTheSelectionAndRemovingItSelectsAll() {
        val selected = ConversationFilter.Label("All")
        filters.render(listOf("All", "friends"), selected)
        assertTrue(chip(selected).isChecked)
        filters.render(listOf("All", "family", "friends"), selected)
        assertTrue(chip(selected).isChecked)
        assertTrue(selections.isEmpty())
        filters.render(listOf("family", "friends"), selected)
        assertEquals(listOf(ConversationFilter.All), selections)
        assertTrue(chip(ConversationFilter.All).isChecked)
    }

    @Test
    fun labelActionsStayHiddenInPendingViewsAndCreationIsUnavailableForSip() {
        filters.showLabelActions(false)
        filters.render(listOf("friends"), ConversationFilter.All)
        assertFalse(chip(ConversationFilter.Label("friends")).isVisible)
        assertFalse(group.findViewById<View>(R.id.add_contact_label).isVisible)
        filters.showLabelActions(true)
        assertTrue(chip(ConversationFilter.Label("friends")).isVisible)
        filters.render(emptyList(), ConversationFilter.All, canCreate = false)
        assertFalse(group.findViewById<View>(R.id.add_contact_label).isVisible)
    }

    @Test
    fun labelTextMatchesExistingChipColorsInBothSelectionStates() = assertMatchingTextColors()

    @Test
    @Config(qualifiers = "night")
    fun labelTextMatchesExistingChipColorsAtNight() = assertMatchingTextColors()

    @Test
    fun textOnlyLabelChipsHaveBalancedPaddingWithoutRemovingTypeIcons() {
        filters.render(listOf("friends"), ConversationFilter.All)
        val label = chip(ConversationFilter.Label("friends"))
        for (checked in listOf(false, true)) {
            label.isChecked = checked
            assertTrue("Text-only chip padding should be balanced",
                abs(label.paddingStart - label.paddingEnd) <= 1)
        }
        val contacts = chip(ConversationFilter.Contacts)
        assertTrue(contacts.isChipIconVisible)
        assertNotNull(contacts.chipIcon)
    }

    private fun assertMatchingTextColors() {
        filters.render(listOf("friends"), ConversationFilter.All)
        val existing = chip(ConversationFilter.Contacts)
        val label = chip(ConversationFilter.Label("friends"))
        assertEquals(existing.currentTextColor, label.currentTextColor)
        existing.isChecked = true
        val checkedColor = existing.currentTextColor
        label.isChecked = true
        assertEquals(checkedColor, label.currentTextColor)
        chip(ConversationFilter.All).isChecked = true
        assertEquals(existing.currentTextColor, label.currentTextColor)
    }

    private fun chip(filter: ConversationFilter) = group.children.filterIsInstance<Chip>().first { it.tag == filter }
}
