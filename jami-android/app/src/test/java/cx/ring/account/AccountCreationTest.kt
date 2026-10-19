package cx.ring.account

import android.os.Looper
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputLayout
import cx.ring.R
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.TestScheduler
import net.jami.account.JamiAccountCreationPresenter
import net.jami.services.AccountService
import net.jami.services.AccountService.LookupState
import net.jami.services.AccountService.RegisteredName
import net.jami.services.DaemonService
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verifyNoInteractions
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [28, 34])
class AccountCreationTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Inject lateinit var accounts: AccountService
    @Inject lateinit var daemon: DaemonService

    private lateinit var controller: ActivityController<HostActivity>
    private val clock = TestScheduler()
    private val activity: HostActivity get() = controller.get()

    @Before
    fun setUp() {
        hilt.inject()
        RxJavaPlugins.setComputationSchedulerHandler { clock }
        `when`(accounts.findRegistrationByName(anyString(), anyString(), anyString())).thenAnswer { invocation ->
            val query = invocation.getArgument<String>(2)
            val state = when (query) {
                "ab" -> LookupState.Invalid
                "abc" -> LookupState.Success
                "offline" -> LookupState.NetworkError
                else -> LookupState.NotFound
            }
            Single.just(RegisteredName("", query, query, state = state))
        }
        controller = Robolectric.buildActivity(HostActivity::class.java)
        activity.setTheme(R.style.AppTheme)
        controller.setup()
    }

    @After
    fun tearDown() {
        if (::controller.isInitialized) controller.pause().stop().destroy()
        RxJavaPlugins.reset()
        verifyNoInteractions(daemon)
    }

    @Test
    fun shortUsernameShowsErrorAndDisablesNext() {
        val view = show(JamiAccountUsernameFragment())
        view.findViewById<EditText>(R.id.input_username).setText("ab")
        finishLookup()
        assertEquals(activity.getString(R.string.invalid_username),
            view.findViewById<TextInputLayout>(R.id.input_username_txt_box).error.toString())
        assertFalse(view.findViewById<View>(R.id.create_account_username).isEnabled)
    }

    @Test
    fun takenUsernameShowsErrorAndDisablesNext() {
        val view = show(JamiAccountUsernameFragment())
        view.findViewById<EditText>(R.id.input_username).setText("abc")
        finishLookup()
        assertEquals(activity.getString(R.string.username_already_taken),
            view.findViewById<TextInputLayout>(R.id.input_username_txt_box).error.toString())
        assertFalse(view.findViewById<View>(R.id.create_account_username).isEnabled)
    }

    @Test
    fun availableUsernameEnablesNextAfterLookup() {
        val view = show(JamiAccountUsernameFragment())
        view.findViewById<EditText>(R.id.input_username).setText("jamitest_available")
        assertFalse(view.findViewById<View>(R.id.create_account_username).isEnabled)
        finishLookup()
        assertFalse(view.findViewById<TextInputLayout>(R.id.input_username_txt_box).isErrorEnabled)
        assertTrue(view.findViewById<View>(R.id.create_account_username).isEnabled)
    }

    @Test
    fun lookupFailureShowsErrorAndDisablesNext() {
        val view = show(JamiAccountUsernameFragment())
        view.findViewById<EditText>(R.id.input_username).setText("offline")
        finishLookup()
        assertEquals(activity.getString(R.string.unknown_error),
            view.findViewById<TextInputLayout>(R.id.input_username_txt_box).error.toString())
        assertFalse(view.findViewById<View>(R.id.create_account_username).isEnabled)
    }

    @Test
    fun enablingPasswordShowsPasswordAndConfirmationFields() {
        val view = passwordStep()
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.password_txt_box).visibility)
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.ring_password_repeat_txt_box).visibility)
        assertEquals(View.GONE, view.findViewById<View>(R.id.placeholder).visibility)
    }

    @Test
    fun shortPasswordShowsErrorAndDisablesNext() {
        val view = passwordStep()
        view.findViewById<EditText>(R.id.password).setText("test")
        assertEquals(activity.getString(R.string.error_password_char_count),
            view.findViewById<TextInputLayout>(R.id.password_txt_box).error.toString())
        assertFalse(view.findViewById<View>(R.id.create_account_password).isEnabled)
    }

    @Test
    fun mismatchedPasswordShowsErrorAndDisablesNext() {
        val view = passwordStep()
        view.findViewById<EditText>(R.id.password).setText("123456")
        view.findViewById<EditText>(R.id.ring_password_repeat).setText("12345")
        assertEquals(activity.getString(R.string.error_passwords_not_equals),
            view.findViewById<TextInputLayout>(R.id.ring_password_repeat_txt_box).error.toString())
        assertFalse(view.findViewById<View>(R.id.create_account_password).isEnabled)
    }

    @Test
    fun matchingPasswordEnablesNext() {
        val view = passwordStep()
        view.findViewById<EditText>(R.id.password).setText("123456")
        view.findViewById<EditText>(R.id.ring_password_repeat).setText("123456")
        assertTrue(view.findViewById<View>(R.id.create_account_password).isEnabled)
        assertNull(view.findViewById<TextInputLayout>(R.id.password_txt_box).error)
        assertNull(view.findViewById<TextInputLayout>(R.id.ring_password_repeat_txt_box).error)
    }

    @Test
    fun disablingPasswordClearsItAndEnablesSkipping() {
        val view = passwordStep()
        view.findViewById<EditText>(R.id.password).setText("test")
        view.findViewById<MaterialSwitch>(R.id.ring_password_switch).performClick()
        assertEquals("", ViewModelProvider(activity)[AccountCreationViewModel::class.java].model.password)
        assertTrue(view.findViewById<View>(R.id.create_account_password).isEnabled)
        assertEquals(View.GONE, view.findViewById<View>(R.id.password_txt_box).visibility)
    }

    @Test
    fun registrationDialogRejectsShortUsername() {
        val dialog = registrationDialog()
        dialog.findViewById<EditText>(R.id.input_username)!!.setText("ab")
        finishLookup()
        assertEquals(activity.getString(R.string.invalid_username),
            dialog.findViewById<TextInputLayout>(R.id.input_username_txt_box)!!.error.toString())
    }

    @Test
    fun registrationDialogRejectsTakenUsername() {
        val dialog = registrationDialog()
        dialog.findViewById<EditText>(R.id.input_username)!!.setText("abc")
        finishLookup()
        assertEquals(activity.getString(R.string.username_already_taken),
            dialog.findViewById<TextInputLayout>(R.id.input_username_txt_box)!!.error.toString())
    }

    @Test
    fun registrationDialogSubmitsAvailableUsername() {
        val names = mutableListOf<String>()
        val dialog = registrationDialog { names.add(it) }
        dialog.findViewById<EditText>(R.id.input_username)!!.setText("jamitest_available")
        finishLookup()
        assertNull(dialog.findViewById<TextInputLayout>(R.id.input_username_txt_box)!!.error)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        assertEquals(listOf("jamitest_available"), names)
    }

    private fun passwordStep(): View = show(JamiAccountPasswordFragment()).also {
        it.findViewById<MaterialSwitch>(R.id.ring_password_switch).performClick()
    }

    private fun show(fragment: Fragment): View {
        activity.supportFragmentManager.beginTransaction().replace(android.R.id.content, fragment).commitNow()
        shadowOf(Looper.getMainLooper()).idle()
        return fragment.requireView()
    }

    private fun registrationDialog(onRegister: (String) -> Unit = {}): AlertDialog {
        val fragment = RegisterNameDialog().apply {
            setListener(object : RegisterNameDialog.RegisterNameDialogListener {
                override fun onRegisterName(name: String) = onRegister(name)
            })
        }
        fragment.showNow(activity.supportFragmentManager, "register-name")
        shadowOf(Looper.getMainLooper()).idle()
        return fragment.requireDialog() as AlertDialog
    }

    private fun finishLookup() {
        clock.advanceTimeBy(JamiAccountCreationPresenter.TYPING_DELAY, TimeUnit.MILLISECONDS)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @AndroidEntryPoint
    class HostActivity : AppCompatActivity()
}