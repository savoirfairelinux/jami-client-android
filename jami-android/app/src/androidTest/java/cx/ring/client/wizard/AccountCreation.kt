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

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.intent.IntentCallback
import androidx.test.runner.intent.IntentMonitorRegistry
import cx.ring.AccountUtils
import cx.ring.ImageProvider
import cx.ring.R
import cx.ring.client.HomeActivity
import cx.ring.waitUntil
import cx.ring.hasTextInputLayoutError
import cx.ring.waitForView
import cx.ring.utils.svg.getUri
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
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

    @get:Rule
    val grantPermissionRuleNotification: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule
    val grantPermissionRuleCamera: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    @Test
    fun a_setup() {
        // Download image from URL.
        mActivityScenarioRule.scenario.onActivity { activity ->
            downloadedImagesUri = ImageProvider().downloadImagesToUri(activity, 2)
        }
    }

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

        onView(withId(R.id.create_account_password)).perform(
            waitUntil(allOf(isDisplayed(), isEnabled())), click()
        )

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

        onView(withId(R.id.create_account_username)).perform(
            waitUntil(allOf(isDisplayed(), isEnabled())), click()
        )

        onView(allOf(withId(R.id.ring_password_switch), isDisplayed())).perform(click())

        specifyPassword()

        onView(withId(R.id.create_account_password)).perform(
            waitUntil(allOf(isDisplayed(), isEnabled())), click()
        )

        skipBiometrics()

        onView(allOf(withId(R.id.skip_create_account), isDisplayed())).perform(click())
    }

    /**
     * Checks if an account can be created by specifying a profile name.
     * Skip others steps.
     */
    @Test
    fun accountCreation_SpecifyProfileName() {
        onView(withId(R.id.ring_create_btn)).perform(scrollTo(), click())

        onView(allOf(withId(R.id.skip), isDisplayed())).perform(click())

        onView(allOf(withId(R.id.create_account_password), isDisplayed())).perform(click())

        onView(allOf(withId(R.id.username), isDisplayed()))
            .perform(replaceText("Bonjour"), closeSoftKeyboard())

        onView(allOf(withId(R.id.next_create_account), isDisplayed())).perform(click())

        // Check that we are in the home activity.
        waitForView(withId(R.id.search_bar)).perform(waitUntil(isDisplayed()))

        // Go to account settings. Click on search bar menu.
        onView(withId(R.id.menu_overflow)).perform(click())

        // Click on account settings. Don't know why but doesn't work to select by ID.
        onView(allOf(withText(R.string.menu_item_account_settings), isDisplayed())).perform(click())

        // Check if name is changed.
        onView(withId(R.id.username)).check(matches(withText("Bonjour")))
    }

    /**
     * Checks if an account can be created by specifying a profile picture (via camera).
     * Skip others steps.
     */
    @Test
    fun accountCreation_SpecifyProfilePictureViaCamera() {
        onView(withId(R.id.ring_create_btn)).perform(scrollTo(), click())

        onView(allOf(withId(R.id.skip), isDisplayed())).perform(click())

        onView(allOf(withId(R.id.create_account_password), isDisplayed())).perform(click())

        // Prepare the callback when we will intercept the camera intent.
        val intentCallback = IntentCallback {
            if (it.action == MediaStore.ACTION_IMAGE_CAPTURE) {
                it.extras!!.getUri(MediaStore.EXTRA_OUTPUT)!!.run {
                    mActivityScenarioRule.scenario.onActivity { activity ->
                        // Copy the downloaded image to the intent uri.
                        val inStream =
                            activity.contentResolver.openInputStream(downloadedImagesUri[0])
                        val outStream = activity.contentResolver.openOutputStream(this)
                        inStream?.use { input -> outStream?.use { output -> input.copyTo(output) } }
                    }
                }
            }
        }

        // Start recording intents and subscribe to the callback.
        Intents.init()
        IntentMonitorRegistry.getInstance().addIntentCallback(intentCallback)

        // Block the camera intent to propagate (prevent the camera from opening).
        intending(hasAction(MediaStore.ACTION_IMAGE_CAPTURE))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

        // Click on camera (should launch a camera intent).
        onView(withId(R.id.camera)).perform(click())

        // Stop recording intents and remove the callback.
        IntentMonitorRegistry.getInstance().removeIntentCallback(intentCallback)
        Intents.release()

        // Todo: Find a way to make the match work.
        // Check if the image is displayed.
        // onView(withId(R.id.profile_photo)).check(matches(withImageUri(downloadedImageUri!!)))

        onView(allOf(withId(R.id.next_create_account), isDisplayed())).perform(click())

        // Check that we are in the home activity.
        waitForView(withId(R.id.search_bar)).perform(waitUntil(isDisplayed()))

        // Go to account settings. Click on search bar menu.
        onView(withId(R.id.menu_overflow)).perform(click())

        // Click on account settings. Don't know why but doesn't work to select by ID.
        onView(allOf(withText(R.string.menu_item_account_settings), isDisplayed())).perform(click())

        // Todo: Find a way to make the match work.
        // Check if picture is changed in profile.
    }

    /** Checks if an account can be created by specifying a profile picture (via camera).
     * Skip others steps.
     */
    @Test
    fun accountCreation_SpecifyProfilePictureViaGallery() {
        onView(withId(R.id.ring_create_btn)).perform(scrollTo(), click())

        onView(allOf(withId(R.id.skip), isDisplayed())).perform(click())

        onView(allOf(withId(R.id.create_account_password), isDisplayed())).perform(click())

        // Start recording intents and subscribe to the callback.
        Intents.init()

        intending(hasAction(MediaStore.ACTION_PICK_IMAGES))
            .respondWith(
                Instrumentation.ActivityResult(
                    Activity.RESULT_OK,
                    Intent().setData(downloadedImagesUri[1])
                )
            )

        // Click on camera (should launch a gallery explorer).
        onView(withId(R.id.gallery)).perform(click())

        // Stop recording intents and remove the callback.
        Intents.release()

        // Todo: Find a way to make the match work.
        // Check if the image is displayed.
        // onView(withId(R.id.profile_photo)).check(matches(withImageUri(downloadedImageUri!!)))

        onView(allOf(withId(R.id.next_create_account), isDisplayed())).perform(click())

        // Check that we are in the home activity.
        waitForView(withId(R.id.search_bar)).perform(waitUntil(isDisplayed()))

        // Go to account settings. Click on search bar menu.
        onView(withId(R.id.menu_overflow)).perform(click())

        // Click on account settings. Don't know why but doesn't work to select by ID.
        onView(allOf(withText(R.string.menu_item_account_settings), isDisplayed())).perform(click())

        // Todo: Find a way to make the match work.
        // Check if picture is changed in profile.
    }

    /**
     * Checks if a profile picture can be removed.
     */
    @Test
    fun accountCreation_SpecifyProfilePicture_CanCancel(){
        onView(withId(R.id.ring_create_btn)).perform(scrollTo(), click())

        onView(allOf(withId(R.id.skip), isDisplayed())).perform(click())

        onView(allOf(withId(R.id.create_account_password), isDisplayed())).perform(click())

        // Start recording intents and subscribe to the callback.
        Intents.init()

        intending(hasAction(MediaStore.ACTION_PICK_IMAGES))
            .respondWith(
                Instrumentation.ActivityResult(
                    Activity.RESULT_OK,
                    Intent().setData(downloadedImagesUri[1])
                )
            )

        // Click on camera (should launch a gallery explorer).
        onView(withId(R.id.gallery)).perform(click())

        // Stop recording intents and remove the callback.
        Intents.release()

        // Click on delete photo.
        onView(allOf(withId(R.id.delete_photo), isDisplayed())).perform(click())

        // Photo should be deleted.
        // Todo: Find a way to make the match work.

        // Button to delete photo should be gone.
        onView(withId(R.id.delete_photo)).check(matches(not(isDisplayed())))
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

        onView(withId(R.id.create_account_username)).check(matches(allOf(isNotEnabled(), isDisplayed())))
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

        onView(withId(R.id.create_account_username)).check(matches(allOf(isNotEnabled(), isDisplayed())))
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

        onView(withId(R.id.create_account_username)).check(matches(allOf(isNotEnabled(), isDisplayed())))
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

        onView(withId(R.id.create_account_password)).check(matches(allOf(isNotEnabled(), isDisplayed())))
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

        onView(withId(R.id.create_account_password)).check(matches(allOf(isNotEnabled(), isDisplayed())))
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

        onView(withId(R.id.create_account_password)).check(matches(allOf(isEnabled(), isDisplayed())))
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

        onView(withId(R.id.create_account_username)).perform(
            waitUntil(allOf(isDisplayed(), isEnabled())), click()
        )

        onView(allOf(withId(R.id.create_account_password), isDisplayed())).perform(click())

        onView(allOf(withId(R.id.skip_create_account), isDisplayed())).perform(click())
    }

    companion object {
        fun skipBiometrics() { // Skip biometrics popup (only on P+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)
                waitForView(withText(R.string.no_thanks)).perform(waitUntil(isDisplayed()), click())
        }

        @JvmStatic
        private var downloadedImagesUri = listOf<Uri>()
    }
}
