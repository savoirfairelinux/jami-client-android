/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.client.wizard

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import cx.ring.AccountUtils
import cx.ring.R
import cx.ring.client.HomeActivity
import cx.ring.waitUntil
import cx.ring.hasTextInputLayoutError
import cx.ring.waitForView
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

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

        onView(withId(R.id.create_account_password))
            .perform(waitUntil(allOf(isDisplayed(), isEnabled())), click())

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

        onView(withId(R.id.create_account_username))
            .perform(waitUntil(allOf(isDisplayed(), isEnabled())), click())

        onView(allOf(withId(R.id.ring_password_switch), isDisplayed())).perform(click())

        specifyPassword()

        onView(withId(R.id.create_account_password))
            .perform(waitUntil(allOf(isDisplayed(), isEnabled())), click())

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

        onView(withId(R.id.input_username)).perform(replaceText("ab"), closeSoftKeyboard())

        onView(withId(R.id.input_username_txt_box))
            .perform(waitUntil(hasTextInputLayoutError(R.string.invalid_username)))

        onView(withId(R.id.create_account_username))
            .check(matches(allOf(isNotEnabled(), isDisplayed())))
    }

    /**
     * Check what happens when writing an already taken username.
     * Assert that the create account button is disabled.
     * Assert that the error message is displayed.
     */
    @Test
    fun usernameSelection_UsernameAlreadyTaken() {

        onView(allOf(withId(R.id.ring_create_btn), isDisplayed())).perform(scrollTo(), click())

        onView(withId(R.id.input_username)).perform(replaceText("abc"), closeSoftKeyboard())

        onView(withId(R.id.input_username_txt_box))
            .perform(waitUntil(hasTextInputLayoutError(R.string.username_already_taken)))

        onView(withId(R.id.create_account_username))
            .check(matches(allOf(isNotEnabled(), isDisplayed())))
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

        onView(withId(R.id.create_account_username))
            .check(matches(allOf(isNotEnabled(), isDisplayed())))
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

        onView(withId(R.id.create_account_password)).check(matches(isDisplayed()))

        onView(withId(R.id.ring_password_repeat_txt_box)).check(matches(isDisplayed()))
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

        onView(withId(R.id.password_txt_box))
            .perform(waitUntil(hasTextInputLayoutError(R.string.error_password_char_count)))

        onView(withId(R.id.create_account_password))
            .check(matches(allOf(isNotEnabled(), isDisplayed())))
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

        onView(withId(R.id.ring_password_repeat_txt_box))
            .perform(waitUntil(hasTextInputLayoutError(R.string.error_passwords_not_equals)))

        onView(withId(R.id.create_account_password))
            .check(matches(allOf(isNotEnabled(), isDisplayed())))
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

        onView(withId(R.id.create_account_password))
            .check(matches(allOf(isEnabled(), isDisplayed())))
    }

    @Test
    fun z_tearDown() {
        // Doing this in a test is not ideal.
        // Ideally, it should be in an `@After` method, but the problem is that it is executed
        // after each test and not only at the end.
        // That is the same problem with Android Test Orchestrator which removes the application
        // between each test and not only at the end.
        // `@AfterClass` could be used (executed once), but it does not have access to the activity.
        mActivityScenarioRule.scenario.onActivity { activity ->
            AccountUtils.removeAllAccounts(accountService = activity.mAccountService)
        }
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

    private fun skipBiometrics() { // Skip biometrics popup (only on P+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)
            waitForView(withText(R.string.no_thanks)).perform(waitUntil(isDisplayed()), click())
    }

    private fun moveToWizard() {
        mActivityScenarioRule.scenario.onActivity { activity -> // Set custom name server
            activity.mAccountService.customNameServer = "https://ns-test.jami.net/"
        }

        try {
            onView(withContentDescription(R.string.searchbar_navigation_account)).perform(click())
            onView(withText(R.string.add_ring_account_title)).perform(click())
        } catch (_: Exception) { // Already in the wizard ?
            // Todo: Should check before exception where we are.
        }
    }

    private fun createAccountWithUsername(username: String) {
        onView(withId(R.id.ring_create_btn)).perform(scrollTo(), click())

        onView(allOf(withId(R.id.input_username), isDisplayed()))
            .perform(replaceText(username), closeSoftKeyboard())

        onView(withId(R.id.create_account_username))
            .perform(waitUntil(allOf(isDisplayed(), isEnabled())), click())

        onView(allOf(withId(R.id.create_account_password), isDisplayed())).perform(click())

        onView(allOf(withId(R.id.skip_create_account), isDisplayed())).perform(click())
    }
}
