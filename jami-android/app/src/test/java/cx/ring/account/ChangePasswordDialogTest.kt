package cx.ring.account

import android.app.Application
import android.os.Bundle
import android.os.Looper
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import cx.ring.R
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28, 34])
class ChangePasswordDialogTest {
    private lateinit var controller: ActivityController<AppCompatActivity>
    private lateinit var fragment: ChangePasswordDialog
    private val changes = mutableListOf<Pair<String, String>>()
    private val dialog: AlertDialog get() = fragment.requireDialog() as AlertDialog

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
    fun shortPasswordShowsErrorsWithoutSubmitting() {
        showDialog()
        enterPassword("12345", "12345")
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        assertErrors(R.string.error_password_char_count)
        assertTrue(changes.isEmpty())
        assertTrue(dialog.isShowing)
    }

    @Test
    fun mismatchedConfirmationShowsErrorsWithoutSubmitting() {
        showDialog()
        enterPassword("123456", "12345")
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        assertErrors(R.string.error_passwords_not_equals)
        assertTrue(changes.isEmpty())
        assertTrue(dialog.isShowing)
    }

    @Test
    fun matchingPasswordSubmitsOnceAndDismisses() {
        showDialog()
        enterPassword("123456", "123456")
        val shown = dialog
        shown.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf("" to "123456"), changes)
        assertFalse(shown.isShowing)
    }

    @Test
    fun removingPasswordSubmitsOldPasswordAndEmptyNewPassword() {
        showDialog(hasPassword = true)
        dialog.findViewById<EditText>(R.id.old_password_txt)!!.setText("old-password")
        val shown = dialog
        shown.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf("old-password" to ""), changes)
        assertFalse(shown.isShowing)
    }

    @Test
    fun cancellationDoesNotSubmit() {
        showDialog()
        enterPassword("123456", "123456")
        val shown = dialog
        shown.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(changes.isEmpty())
        assertFalse(shown.isShowing)
    }

    @Test
    fun keyboardDoneSubmitsMatchingPassword() {
        showDialog()
        enterPassword("123456", "123456")
        val shown = dialog
        shown.findViewById<EditText>(R.id.new_password_repeat_txt)!!.onEditorAction(EditorInfo.IME_ACTION_DONE)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf("" to "123456"), changes)
        assertFalse(shown.isShowing)
    }

    @Test
    fun keyboardDoneRejectsInvalidPassword() {
        showDialog()
        enterPassword("12345", "12345")
        dialog.findViewById<EditText>(R.id.new_password_repeat_txt)!!.onEditorAction(EditorInfo.IME_ACTION_DONE)
        assertErrors(R.string.error_password_char_count)
        assertTrue(changes.isEmpty())
        assertTrue(dialog.isShowing)
    }

    @Test
    fun correctingPasswordClearsBothErrorsBeforeSubmitting() {
        showDialog()
        enterPassword("12345", "12345")
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        assertErrors(R.string.error_password_char_count)
        enterPassword("123456", "123456")
        val shown = dialog
        shown.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        assertNull(shown.findViewById<TextInputLayout>(R.id.new_password_txt_box)!!.error)
        assertNull(shown.findViewById<TextInputLayout>(R.id.new_password_repeat_txt_box)!!.error)
        assertEquals(listOf("" to "123456"), changes)
    }

    private fun showDialog(hasPassword: Boolean = false) {
        fragment = ChangePasswordDialog().apply {
            arguments = Bundle().apply { putBoolean(AccountEditionFragment.ACCOUNT_HAS_PASSWORD_KEY, hasPassword) }
            setListener { oldPassword, newPassword -> changes.add(oldPassword to newPassword) }
        }
        fragment.showNow(controller.get().supportFragmentManager, ChangePasswordDialog.TAG)
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun enterPassword(password: String, confirmation: String) {
        dialog.findViewById<EditText>(R.id.new_password_txt)!!.setText(password)
        dialog.findViewById<EditText>(R.id.new_password_repeat_txt)!!.setText(confirmation)
    }

    private fun assertErrors(message: Int) {
        val expected = controller.get().getString(message)
        assertEquals(expected, dialog.findViewById<TextInputLayout>(R.id.new_password_txt_box)!!.error.toString())
        assertEquals(expected, dialog.findViewById<TextInputLayout>(R.id.new_password_repeat_txt_box)!!.error.toString())
    }
}