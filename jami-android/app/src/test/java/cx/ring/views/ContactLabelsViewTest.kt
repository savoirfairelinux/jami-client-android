package cx.ring.views

import android.app.Application
import android.content.res.Configuration
import android.graphics.Color
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.children
import androidx.core.view.isVisible
import com.google.android.material.chip.Chip
import cx.ring.R
import cx.ring.databinding.FragConversationActionsBinding
import cx.ring.databinding.ItemSmartlistBinding
import cx.ring.viewholders.SmartListViewHolder
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Contact
import net.jami.model.Conversation
import net.jami.model.MemberRole
import net.jami.model.Uri
import net.jami.services.ConversationFacade
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28, 34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ContactLabelsViewTest {
    private lateinit var controller: ActivityController<AppCompatActivity>

    @Before
    fun setUp() {
        controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.AppTheme)
        controller.setup()
    }

    @After
    fun tearDown() {
        controller.pause().stop().destroy()
    }

    @Test
    fun detailsRenderEveryLabelAsAPillAndHideWhenEmpty() {
        val view = ContactLabelsView(controller.get())
        view.setLabels(setOf("friends", "family", "work"))
        assertEquals(listOf("family", "friends", "work"),
            view.children.map { (it as TextView).text.toString() }.toList())
        assertTrue(view.children.all { it.background != null })
        assertTrue(view.contentDescription.contains("work"))
        measure(view, 100)
        assertTrue(view.children.map { it.top }.distinct().count() > 1)
        view.setLabels(emptySet())
        assertFalse(view.isVisible)
        assertEquals(0, view.childCount)
    }

    @Test
    fun compactPillsFitANarrowRowAndSummarizeTheRemainingLabels() {
        val view = ContactLabelsView(controller.get())
        view.setLabels(setOf("a very long family label", "another very long label", "friends", "work"), compact = true)
        measure(view, 80)
        assertEquals(listOf(controller.get().getString(R.string.contact_labels_more, 4)),
            view.children.filter { it.isVisible }.map { (it as TextView).text.toString() }.toList())
        val narrowHeight = view.height
        measure(view, 320)
        assertEquals(3, view.children.count { it.isVisible })
        assertEquals(controller.get().getString(R.string.contact_labels_more, 2),
            (view.getChildAt(2) as TextView).text.toString())
        assertTrue(view.children.all { it.right <= view.width && it.top == 0 })
        assertEquals(narrowHeight, view.height)
        assertTrue(view.contentDescription.contains("friends"))
    }

    @Test
    fun labelsShareTheNameRowWithoutIncreasingConversationHeight() = assertInlineLabels(View.LAYOUT_DIRECTION_LTR)

    @Test
    fun inlineLabelsFollowTheNameInRtl() = assertInlineLabels(View.LAYOUT_DIRECTION_RTL)

    @Test
    fun detailsWrapAtTheEndAndAlignTheFirstLineWithTheHeader() = assertDetailsAlignment(View.LAYOUT_DIRECTION_LTR)

    @Test
    fun detailsFollowTheValueAlignmentInRtl() = assertDetailsAlignment(View.LAYOUT_DIRECTION_RTL)

    @Test
    fun compactPillsKeepTheirTextProportionsAtNormalAndLargeFontSizes() {
        for (scale in listOf(1f, 1.6f)) {
            val config = Configuration(controller.get().resources.configuration).apply { fontScale = scale }
            val context = ContextThemeWrapper(controller.get().createConfigurationContext(config), R.style.AppTheme)
            val view = ContactLabelsView(context)
            view.setLabels(listOf("friends", "family", "work"), compact = true)
            measure(view, 320)
            for (chip in view.children.filterIsInstance<Chip>().filter { it.isVisible }) {
                val metrics = chip.paint.fontMetricsInt
                val textHeight = metrics.descent - metrics.ascent
                val density = context.resources.displayMetrics.density
                assertTrue("font scale $scale: height=${chip.height}, text height=$textHeight, " +
                    "padding=${chip.paddingTop}/${chip.paddingBottom}, min height=${chip.minHeight}",
                    chip.height <= textHeight + 8 * density + 1)
                assertTrue(chip.height >= textHeight + 2 * density)
                assertTrue(chip.textSize >= TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP, 11f, context.resources.displayMetrics))
            }
        }
    }

    @Test
    fun labelColorsAreReadableAndDetailsStandOutFromTheirPanel() = assertLabelPalette()

    @Test
    @Config(qualifiers = "night")
    fun labelColorsStaySubtleAndReadableAtNight() = assertLabelPalette()

    @Test
    fun conversationRowsRefreshLabelsAndClearThemWhenRecycledForAGroup() {
        val binding = ItemSmartlistBinding.inflate(controller.get().layoutInflater)
        val disposables = CompositeDisposable()
        val holder = SmartListViewHolder(binding, disposables)
        val contact = Contact(Uri.fromId("a".repeat(40)))
        val direct = Conversation("account", Uri(Uri.SWARM_SCHEME, "direct"), Conversation.Mode.OneToOne)
        direct.addContact(contact, MemberRole.MEMBER)
        val group = Conversation("account", Uri(Uri.SWARM_SCHEME, "group"), Conversation.Mode.InvitesOnly)
        group.addContact(contact, MemberRole.MEMBER)
        val facade = mock(ConversationFacade::class.java)
        `when`(facade.observeConversation(direct, true)).thenReturn(Observable.never())
        `when`(facade.observeConversation(group, false)).thenReturn(Observable.never())
        val listener = mock(SmartListViewHolder.SmartListListeners::class.java)
        holder.bind(facade, listener, direct)
        assertFalse(binding.contactLabels.isVisible)
        contact.setContactLabels(setOf("friends"))
        assertTrue(binding.contactLabels.isVisible)
        assertEquals("friends", (binding.contactLabels.getChildAt(0) as TextView).text.toString())
        holder.bind(facade, listener, group)
        assertFalse(binding.contactLabels.isVisible)
        contact.setContactLabels(setOf("family"))
        assertFalse(binding.contactLabels.isVisible)
        holder.unbind()
        disposables.dispose()
    }

    private fun assertLabelPalette() {
        for (compact in listOf(false, true)) {
            val view = ContactLabelsView(controller.get())
            view.setLabels(listOf("friends"), compact)
            val chip = view.getChildAt(0) as Chip
            val background = checkNotNull(chip.chipBackgroundColor).defaultColor
            val channels = listOf(Color.red(background), Color.green(background), Color.blue(background))
            if (compact) {
                assertTrue("List labels should have a neutral background", channels.max() - channels.min() <= 8)
            } else {
                val panel = controller.get().getColor(R.color.background_secondary)
                assertTrue("Detail labels should be tinted, not grey on grey", channels.max() - channels.min() >= 16)
                assertTrue("Detail labels should stand out from their panel",
                    listOf(Color.red(panel), Color.green(panel), Color.blue(panel))
                        .zip(channels).any { (surface, label) -> abs(surface - label) >= 16 })
            }
            assertTrue("Label text needs readable contrast",
                ColorUtils.calculateContrast(chip.currentTextColor, background) >= 4.5)
            if (controller.get().resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES) {
                assertTrue("Dark-mode labels should not glow", ColorUtils.calculateLuminance(background) <= 0.06)
            }
        }
    }

    private fun assertDetailsAlignment(direction: Int) {
        val binding = FragConversationActionsBinding.inflate(controller.get().layoutInflater)
        binding.root.layoutDirection = direction
        binding.contactPanel.isVisible = true
        binding.contactLabelsPanel.isVisible = true
        binding.contactLabelsPanel.layoutDirection = direction
        binding.contactLabels.layoutDirection = direction
        binding.contactLabels.setLabels(listOf(
            "a very long contact label that needs to be truncated", "family", "friends", "weekends", "work"))
        binding.contactLabelsEmpty.isVisible = false
        measure(binding.root, 320)
        val labels = binding.contactLabels
        val header = binding.contactLabelsPanel.children.filterIsInstance<TextView>().first {
            it.text.toString() == controller.get().getString(R.string.contact_labels)
        }
        val pills = labels.children.toList()
        assertTrue("Details should wrap", pills.map { it.top }.distinct().count() > 1)
        assertTrue("Long labels should fit in the value column", pills.all {
            it.left >= labels.paddingLeft && it.right <= labels.width - labels.paddingRight
        })
        assertEquals(header.top + header.baseline,
            labels.top + pills.first().top + pills.first().baseline)
        for (row in pills.groupBy { it.top }.values) {
            if (direction == View.LAYOUT_DIRECTION_LTR)
                assertEquals(labels.width - labels.paddingRight, row.maxOf { it.right })
            else
                assertEquals(labels.paddingLeft, row.minOf { it.left })
        }
        if (direction == View.LAYOUT_DIRECTION_LTR) {
            assertTrue(labels.left > header.right)
            assertEquals(binding.contactLabelsPanel.width - binding.contactLabelsPanel.paddingRight, labels.right)
        } else {
            assertTrue(labels.right < header.left)
            assertEquals(binding.contactLabelsPanel.paddingLeft, labels.left)
        }
        binding.contactLabels.setLabels(emptySet())
        binding.contactLabelsEmpty.isVisible = true
        measure(binding.root, 320)
        assertEquals(header.top + header.baseline,
            binding.contactLabelsEmpty.top + binding.contactLabelsEmpty.baseline)
    }

    private fun assertInlineLabels(direction: Int) {
        val binding = ItemSmartlistBinding.inflate(controller.get().layoutInflater)
        binding.root.layoutDirection = direction
        binding.convTitle.layoutDirection = direction
        binding.contactLabels.layoutDirection = direction
        binding.convParticipant.text = "A contact with a long display name"
        binding.convLastTime.text = "2 days ago"
        binding.convLastItem.text = "Last message"
        measure(binding.root, 320)
        val heightWithoutLabels = binding.root.height
        binding.contactLabels.setLabels(setOf("a long label", "another long label", "friends", "work"), compact = true)
        measure(binding.root, 320)
        assertEquals("name=${binding.convParticipant.height}, labels=${binding.contactLabels.height}, " +
            "title=${binding.convTitle.height}, preview=${binding.convLastItem.height}",
            heightWithoutLabels, binding.root.height)
        assertSame(binding.convParticipant.parent, binding.contactLabels.parent)
        assertTrue(binding.contactLabels.top < binding.convParticipant.bottom)
        assertTrue(binding.convParticipant.width >= binding.convTitle.width / 2)
        if (direction == View.LAYOUT_DIRECTION_LTR)
            assertTrue(binding.contactLabels.left >= binding.convParticipant.right)
        else
            assertTrue(binding.contactLabels.right <= binding.convParticipant.left)
        assertTrue(binding.contactLabels.children.filter { it.isVisible }.all {
            it.left >= 0 && it.right <= binding.contactLabels.width
        })
    }

    private fun measure(view: View, widthDp: Int) {
        val width = (widthDp * view.resources.displayMetrics.density).toInt()
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        view.layout(0, 0, width, view.measuredHeight)
    }
}
