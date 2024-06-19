package cx.ring.client.wizard

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import cx.ring.R
import cx.ring.assertOnView
import cx.ring.client.HomeActivity
import cx.ring.doOnView
import io.reactivex.rxjava3.core.Single
import net.jami.model.Account
import net.jami.model.AccountConfig
import net.jami.model.ConfigKey
import net.jami.services.AccountService
import net.jami.utils.Log
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.HashMap

@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class AccountCreation {

    @Rule
    @JvmField
    var mActivityScenarioRule = ActivityScenarioRule(HomeActivity::class.java)

    @Before
    fun moveToAccountCreation() = moveToWizard()

    /**
     * Checks if an account can be created by skipping all the steps.
     */
    @Test
    fun accountCreation_SkipAllSteps() = createDefaultAccount()

    /**
     * Checks if an account can be created by specifying a username only.
     * Skip others steps.
     */
    @Test
    fun accountCreation_SpecifyUsernameOnly() {
        val randomUsername = "JamiTest_" + System.currentTimeMillis()
        createAccountWithUsername(randomUsername)
    }

    /**
     * Checks if an account can be created by specifying a password only.
     * Skip others steps.
     */
    @Test
    fun accountCreation_SpecifyPasswordOnly() {
        onView(withId(R.id.ring_create_btn)).perform(scrollTo(), click())

        onView(allOf(withId(R.id.skip), isDisplayed())).perform(click())

        onView(allOf(withId(R.id.ring_password_switch), isDisplayed())).perform(click())

        specifyPassword()

        doOnView(allOf(withId(R.id.create_account_password), isDisplayed(), isEnabled()), click())

        skipBiometrics()

        onView(allOf(withId(R.id.skip_create_account), isDisplayed())).perform(click())
    }

    /**
     * Checks if an account can be created by specifying a username and a password.
     */
    @Test
    fun accountCreation_SpecifyUsernameAndPassword() {
        onView(withId(R.id.ring_create_btn)).perform(scrollTo(), click())

        val randomUsername = "JamiTest_" + System.currentTimeMillis()
        onView(allOf(withId(R.id.input_username), isDisplayed()))
            .perform(replaceText(randomUsername), closeSoftKeyboard())

        doOnView(allOf(withId(R.id.create_account_username), isDisplayed(), isEnabled()), click())

        onView(allOf(withId(R.id.ring_password_switch), isDisplayed())).perform(click())

        specifyPassword()

        doOnView(allOf(withId(R.id.create_account_password), isDisplayed(), isEnabled()), click())

        skipBiometrics()

        onView(allOf(withId(R.id.skip_create_account), isDisplayed())).perform(click())
    }

    /**
     * Checks if an account can be created by specifying a profile.
     * Skip others steps.
     */
    @Test
    fun accountCreation_SpecifyProfile() {
        onView(withId(R.id.ring_create_btn)).perform(scrollTo(), click())

        onView(allOf(withId(R.id.skip), isDisplayed())).perform(click())

        onView(allOf(withId(R.id.create_account_password), isDisplayed())).perform(click())

        onView(allOf(withId(R.id.username), isDisplayed()))
            .perform(replaceText("Bonjour"), closeSoftKeyboard())

        onView(allOf(withId(R.id.next_create_account), isDisplayed())).perform(click())
    }

    /**
     * Check what happens when writing an invalid username.
     * Assert that the create account button is disabled.
     * Assert that the error message is displayed.
     */
    @Test
    fun usernameSelection_LessThan3Characters_InvalidUsername() {

        onView(allOf(withId(R.id.ring_create_btn), isDisplayed())).perform(scrollTo(), click())

        onView(allOf(withId(R.id.input_username), isDisplayed()))
            .perform(replaceText("ab"), closeSoftKeyboard())

        val errorString = InstrumentationRegistry
            .getInstrumentation().targetContext.getString(R.string.invalid_username)
        assertOnView(
            allOf(withId(com.google.android.material.R.id.textinput_error), isDisplayed()),
            matches(withText(errorString))
        )

        assertOnView(
            allOf(withId(R.id.create_account_username), isNotEnabled()),
            matches(isDisplayed())
        )
    }

    /**
     * Check what happens when writing an already taken username.
     * Assert that the create account button is disabled.
     * Assert that the error message is displayed.
     */
    @Test
    fun usernameSelection_UsernameAlreadyTaken() {

        onView(allOf(withId(R.id.ring_create_btn), isDisplayed())).perform(scrollTo(), click())

        onView(allOf(withId(R.id.input_username), isDisplayed()))
            .perform(replaceText("abc"), closeSoftKeyboard())

        val errorString = InstrumentationRegistry
            .getInstrumentation().targetContext.getString(R.string.username_already_taken)
        assertOnView(
            allOf(withId(com.google.android.material.R.id.textinput_error), isDisplayed()),
            matches(withText(errorString))
        )

        assertOnView(
            allOf(withId(R.id.create_account_username), isNotEnabled()),
            matches(isDisplayed())
        )
    }

    /**
     * Check what happens when writing a valid username.
     * Assert that the create account button is enabled.
     */
    @Test
    fun usernameSelection_ValidUsername() {
        onView(allOf(withId(R.id.ring_create_btn), isDisplayed())).perform(scrollTo(), click())

        val randomUsername = "JamiTest_" + System.currentTimeMillis()
        onView(allOf(withId(R.id.input_username), isDisplayed()))
            .perform(replaceText(randomUsername), closeSoftKeyboard())

        assertOnView(
            allOf(withId(R.id.create_account_username), isEnabled()),
            matches(isDisplayed())
        )
    }

    /**
     * Check what happens when enabling the password section.
     * Assert that the password section is displayed.
     * Assert that the copy password section is enabled.
     */
    @Test
    fun passwordSelection_EnableSection() {
        onView(withId(R.id.ring_create_btn)).perform(scrollTo(), click())

        onView(allOf(withId(R.id.skip), isDisplayed())).perform(click())

        onView(allOf(withId(R.id.ring_password_switch), isDisplayed())).perform(click())

        onView(allOf(withId(R.id.create_account_password), isDisplayed()))

        onView(allOf(withId(R.id.ring_password_repeat_txt_box), isDisplayed()))
    }

    /**
     * Check what happens when writing an invalid username.
     * Assert that the create account button is disabled.
     * Assert that the error message is displayed.
     */
    @Test
    fun passwordSelection_LessThan6Characters_InvalidPassword() {
        onView(withId(R.id.ring_create_btn)).perform(scrollTo(), click())

        onView(allOf(withId(R.id.skip), isDisplayed())).perform(click())

        onView(allOf(withId(R.id.ring_password_switch), isDisplayed())).perform(click())

        onView(allOf(withId(R.id.password), isDisplayed()))
            .perform(replaceText("test"), closeSoftKeyboard())

        val errorString = InstrumentationRegistry
            .getInstrumentation().targetContext.getString(R.string.error_password_char_count)
        assertOnView(
            allOf(withId(com.google.android.material.R.id.textinput_error), isDisplayed()),
            matches(withText(errorString))
        )

        assertOnView(
            allOf(withId(R.id.create_account_password), isNotEnabled()),
            matches(isDisplayed())
        )
    }

    /**
     * Check what happens when badly recopying the password.
     * Assert that the create account button is disabled.
     * Assert that the error message is displayed.
     */
    @Test
    fun passwordSelection_WrongRecopy() {
        onView(withId(R.id.ring_create_btn)).perform(scrollTo(), click())

        onView(allOf(withId(R.id.skip), isDisplayed())).perform(click())

        onView(allOf(withId(R.id.ring_password_switch), isDisplayed())).perform(click())

        onView(allOf(withId(R.id.password), isDisplayed()))
            .perform(replaceText("123456"), closeSoftKeyboard())

        onView(allOf(withId(R.id.ring_password_repeat), isDisplayed()))
            .perform(replaceText("12345"), closeSoftKeyboard())

        val errorString = InstrumentationRegistry
            .getInstrumentation().targetContext.getString(R.string.error_passwords_not_equals)
        assertOnView(
            allOf(withId(com.google.android.material.R.id.textinput_error), isDisplayed()),
            matches(withText(errorString))
        )

        assertOnView(
            allOf(withId(R.id.create_account_password), isNotEnabled()),
            matches(isDisplayed())
        )
    }

    /**
     * Check what happens when writing a valid password.
     * Assert that the create account button is enabled.
     */
    @Test
    fun passwordSelection_ValidPassword() {
        onView(withId(R.id.ring_create_btn)).perform(scrollTo(), click())

        onView(allOf(withId(R.id.skip), isDisplayed())).perform(click())

        onView(allOf(withId(R.id.ring_password_switch), isDisplayed())).perform(click())

        specifyPassword()

        assertOnView(
            allOf(withId(R.id.create_account_password), isEnabled()),
            matches(isDisplayed())
        )
    }

    private fun createDefaultAccount() {
        onView(withId(R.id.ring_create_btn)).perform(scrollTo(), click())

        onView(allOf(withId(R.id.skip), isDisplayed())).perform(click())

        onView(allOf(withId(R.id.create_account_password), isDisplayed())).perform(click())

        onView(allOf(withId(R.id.skip_create_account), isDisplayed())).perform(click())
    }


    private fun specifyPassword() {
        onView(allOf(withId(R.id.password), isDisplayed()))
            .perform(replaceText("123456"), closeSoftKeyboard())

        onView(allOf(withId(R.id.ring_password_repeat), isDisplayed()))
            .perform(replaceText("123456"), closeSoftKeyboard())
    }

    private fun skipBiometrics() { // Skip biometrics popup
        // only on P+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val noThanksSrc = InstrumentationRegistry
                .getInstrumentation().targetContext.getString(R.string.no_thanks)
            onView(allOf(withText(noThanksSrc), isDisplayed())).perform(click())
        }
    }

    private fun moveToWizard() {
        try {
            val searchBarContentNavigationDescription = InstrumentationRegistry
                .getInstrumentation().targetContext.getString(R.string.searchbar_navigation_account)
            onView(withContentDescription(searchBarContentNavigationDescription)).perform(click())

            val addAccountString = InstrumentationRegistry
                .getInstrumentation().targetContext.getString(R.string.add_ring_account_title)
            onView(withText(addAccountString)).perform(click())
        } catch (_: Exception) { // Already in the wizard ?
            // Todo: Should check before exception where we are.
        }
    }

    private fun createAccountWithUsername(username: String) {
        onView(withId(R.id.ring_create_btn)).perform(scrollTo(), click())

        onView(allOf(withId(R.id.input_username), isDisplayed()))
            .perform(replaceText(username), closeSoftKeyboard())

        doOnView(allOf(withId(R.id.create_account_username), isDisplayed(), isEnabled()), click())

        onView(allOf(withId(R.id.create_account_password), isDisplayed())).perform(click())

        onView(allOf(withId(R.id.skip_create_account), isDisplayed())).perform(click())

        Log.d("devdebug", "Account created: $username")
    }
}

object AccountCreationUtils {

    private val TAG = AccountCreationUtils::class.java.simpleName

    /**
     * Create n accounts and register them.
     * This function is blocking.
     *
     * @param accountService The account service to use.
     * @param count The number of accounts to create.
     * @return The list of registered account names.
     */
    fun createAccountAndRegister(accountService: AccountService, count: Int): List<String> {

        val baseUsername = "jamitest"
        val time = System.currentTimeMillis()
        val registeredNameList = mutableListOf<String>()

        val accountObservableList = (0..<count).map { accountCount ->
            val username = "${baseUsername}_${time}_${accountCount}"
                .apply { registeredNameList.add(this) }
            Log.d(TAG, "Account username: $username...")
            accountService.getAccountTemplate(AccountConfig.ACCOUNT_TYPE_JAMI)
                .map { accountDetails: HashMap<String, String> ->
                    accountDetails[ConfigKey.ACCOUNT_ALIAS.key] = "Jami account $accountCount"
                    accountDetails[ConfigKey.RINGNS_HOST.key] = "192.168.50.4"
                    accountDetails
                }.flatMapObservable { details ->
                    Log.d(TAG, "Adding account ...")
                    accountService.addAccount(details)
                }
                .filter { account: Account ->
                    account.registrationState != AccountConfig.RegistrationState.INITIALIZING
                }
                .firstOrError()
                .map { account: Account ->
                    Log.d(TAG, "Registering account ...")
                    accountService.registerName(
                        account, username, AccountService.ACCOUNT_SCHEME_PASSWORD, ""
                    )
                    account
                }
        }

        // Wait for all accounts to be created.
        val accountList: List<Account> =
            Single.zip(accountObservableList) { it.filterIsInstance<Account>() }.blockingGet()

        // Wait for all accounts to be registered.
        Single.zip(
            accountList.map {
                accountService.getObservableAccount(it)
                    .filter { account: Account ->
                        account.registrationState == AccountConfig.RegistrationState.REGISTERED
                    }.firstOrError()
            }
        ) { it }.blockingSubscribe()

        return registeredNameList
    }
}