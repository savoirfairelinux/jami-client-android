package cx.ring.client

import android.app.Application
import android.os.Looper
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout
import cx.ring.R
import io.reactivex.rxjava3.core.Completable
import net.jami.services.LogService
import net.jami.utils.Log
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28, 34])
class ContactLabelsDialogTest {
    private lateinit var controller: ActivityController<AppCompatActivity>
    private lateinit var dialog: AlertDialog
    private val saved = mutableListOf<Set<String>>()

    @Before
    fun setUp() {
        Log.injectLogService(mock(LogService::class.java))
        controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.AppTheme)
        controller.setup()
    }

    @After
    fun tearDown() {
        if (::dialog.isInitialized) dialog.dismiss()
        controller.pause().stop().destroy()
    }

    @Test
    fun reusesLabelsAndIncludesUnsubmittedInputWhenSaving() {
        show()
        assertTrue("Label chips should not reserve leading icon/text space",
            abs(chip("family").paddingStart - chip("family").paddingEnd) <= 1)
        chip("family").performClick()
        assertTrue(abs(chip("family").paddingStart - chip("family").paddingEnd) <= 1)
        dialog.findViewById<EditText>(R.id.new_label)!!.setText(" work ")
        save()
        assertEquals(listOf(setOf("friends", "family", "work")), saved)
        assertFalse(dialog.isShowing)
    }

    @Test
    fun deselectingEveryLabelSavesAnEmptySet() {
        show()
        chip("friends").performClick()
        save()
        assertEquals(listOf(emptySet<String>()), saved)
    }

    @Test
    fun cancellationDoesNotSave() {
        show()
        chip("family").performClick()
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        assertTrue(saved.isEmpty())
    }

    @Test
    fun failureKeepsEditsAndAllowsRetry() {
        var attempts = 0
        dialog = showContactLabelsDialog(controller.get(), setOf("friends"), listOf("friends")) { labels ->
            attempts++
            if (attempts == 1) Completable.error(IllegalStateException("write failed"))
            else Completable.fromAction { saved.add(labels) }
        }
        save()
        assertTrue(dialog.isShowing)
        assertNotNull(dialog.findViewById<TextInputLayout>(R.id.new_label_box)!!.error)
        assertTrue(chip("friends").isChecked)
        save()
        assertEquals(listOf(setOf("friends")), saved)
        assertFalse(dialog.isShowing)
    }

    @Test
    fun oversizedLabelsStayEditableAndAreNotSaved() {
        dialog = showContactLabelsDialog(controller.get(), setOf("x".repeat(64000)), emptyList()) {
            Completable.fromAction { saved.add(it) }
        }
        dialog.findViewById<EditText>(R.id.new_label)!!.setText("y".repeat(2000))
        save()
        assertTrue(saved.isEmpty())
        assertTrue(dialog.isShowing)
        assertNotNull(dialog.findViewById<TextInputLayout>(R.id.new_label_box)!!.error)
    }

    @Test
    fun creatingALabelRequiresANameBeforeChoosingContacts() {
        val names = mutableListOf<String>()
        dialog = showCreateContactLabelDialog(controller.get(), names::add)
        assertEquals(controller.get().getString(R.string.contact_labels_description),
            dialog.findViewById<TextView>(R.id.label_description)!!.text.toString())
        dialog.findViewById<EditText>(R.id.new_label)!!.setText("  ")
        save()
        assertTrue(names.isEmpty())
        assertTrue(dialog.isShowing)
        assertNotNull(dialog.findViewById<TextInputLayout>(R.id.new_label_box)!!.error)
        dialog.findViewById<EditText>(R.id.new_label)!!.setText(" friends ")
        save()
        assertEquals(listOf("friends"), names)
        assertFalse(dialog.isShowing)
    }

    @Test
    fun cancellingLabelCreationDoesNotChooseContacts() {
        val names = mutableListOf<String>()
        dialog = showCreateContactLabelDialog(controller.get(), names::add)
        dialog.findViewById<EditText>(R.id.new_label)!!.setText("friends")
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        assertTrue(names.isEmpty())
    }

    private fun show() {
        dialog = showContactLabelsDialog(controller.get(), setOf("friends"), listOf("friends", "family")) {
            Completable.fromAction { saved.add(it) }
        }
    }

    private fun chip(label: String) = dialog.findViewById<ChipGroup>(R.id.labels)!!
        .children.filterIsInstance<Chip>().first { it.text.toString() == label }

    private fun save() {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
    }
}
