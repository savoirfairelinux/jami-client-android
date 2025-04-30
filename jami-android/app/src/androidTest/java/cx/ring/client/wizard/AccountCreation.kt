/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
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
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.intent.IntentCallback
import androidx.test.runner.intent.IntentMonitorRegistry
import cx.ring.AccountUtils
import cx.ring.ImageProvider
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.client.HomeActivity
import cx.ring.waitUntil
import cx.ring.hasTextInputLayoutError
import net.jami.utils.Log
import cx.ring.isDialogWithTitle
import cx.ring.waitForView
import cx.ring.utils.ContentUri.getUri
import cx.ring.withImageUri
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.hamcrest.Matchers.not
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File

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
        // Get assets if installed (should be in /data/local/tmp/jami_test_assets).
        val image1 = File("/data/local/tmp/jami_test_assets/image/image1.jpg")
        val image2 = File("/data/local/tmp/jami_test_assets/image/image1.jpg")
        if(image1.exists() && image2.exists()) {
            Log.d(TAG, "Images found in assets. Using them.")
            downloadedImagesUri = listOf(Uri.fromFile(image1), Uri.fromFile(image2))
        } else { // Download image from URL.
            Log.d(TAG, "Downloading images ...")
            InstrumentationRegistry.getInstrumentation().targetContext
                .let { downloadedImagesUri = ImageProvider().downloadImagesToUri(it, 2) }
            Log.d(TAG, "Images downloaded.")
        }
    }

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

        performClickWithKeyboardClosed(R.id.skip)

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
     * Checks if an account can be created by specifying a profile name.
     * Skip others steps.
     */
    @Test
    fun accountCreation_SpecifyProfileName() {
        onView(withId(R.id.ring_create_btn)).perform(scrollTo(), click())

        performClickWithKeyboardClosed(R.id.skip)

        onView(allOf(withId(R.id.create_account_password), isDisplayed())).perform(click())

        onView(allOf(withId(R.id.username), isDisplayed()))
            .perform(replaceText("Bonjour"), closeSoftKeyboard())

        onView(allOf(withId(R.id.next_create_account), isDisplayed())).perform(click())
        // Check that we are in the home activity.
        waitForView(withId(R.id.search_bar)).perform(waitUntil(isDisplayed()))
        Thread.sleep(1000)
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

        performClickWithKeyboardClosed(R.id.skip)

        onView(allOf(withId(R.id.create_account_password), isDisplayed())).perform(click())

        // Prepare the callback when we will intercept the camera intent.
        val intentCallback = IntentCallback { intentCallback ->
            if (intentCallback.action == MediaStore.ACTION_IMAGE_CAPTURE) {
                intentCallback.extras!!.getUri(MediaStore.EXTRA_OUTPUT)!!.run {
                    InstrumentationRegistry.getInstrumentation().targetContext.contentResolver.let {
                        // Copy the downloaded image to the intent uri.
                        val inStream = it.openInputStream(downloadedImagesUri[0])
                        val outStream = it.openOutputStream(this)
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

        // Check if the image is displayed.
         onView(withId(R.id.profile_photo)).check(matches(withImageUri(downloadedImagesUri[0])))

        onView(allOf(withId(R.id.next_create_account), isDisplayed())).perform(click())

        // Check that we are in the home activity.
        waitForView(withId(R.id.search_bar)).perform(waitUntil(isDisplayed()))

        Thread.sleep(1000)
        // Go to account settings. Click on search bar menu.
        onView(withId(R.id.menu_overflow)).perform(click())

        // Click on account settings. Don't know why but doesn't work to select by ID.
        onView(allOf(withText(R.string.menu_item_account_settings), isDisplayed())).perform(click())

        // Check if picture is changed in profile.
        onView(withId(R.id.user_photo)).check(matches(withImageUri(downloadedImagesUri[0])))
    }

    /** Checks if an account can be created by specifying a profile picture (via camera).
     * Skip others steps.
     */
    @Test
    fun accountCreation_SpecifyProfilePictureViaGallery() {
        onView(withId(R.id.ring_create_btn)).perform(scrollTo(), click())

        performClickWithKeyboardClosed(R.id.skip)

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

        // Check if the image is displayed.
         onView(withId(R.id.profile_photo)).check(matches(withImageUri(downloadedImagesUri[1])))

        onView(allOf(withId(R.id.next_create_account), isDisplayed())).perform(click())

        // Check that we are in the home activity.
        waitForView(withId(R.id.search_bar)).perform(waitUntil(isDisplayed()))

        Thread.sleep(1000)
        // Go to account settings. Click on search bar menu.
        onView(withId(R.id.menu_overflow)).perform(click())

        // Click on account settings. Don't know why but doesn't work to select by ID.
        onView(allOf(withText(R.string.menu_item_account_settings), isDisplayed())).perform(click())

        // Check if picture is changed in profile.
        onView(withId(R.id.user_photo)).check(matches(withImageUri(downloadedImagesUri[1])))
    }

    /**
     * Checks if a profile picture can be removed.
     */
    @Test
    fun accountCreation_SpecifyProfilePicture_CanCancel(){
        onView(withId(R.id.ring_create_btn)).perform(scrollTo(), click())

        performClickWithKeyboardClosed(R.id.skip)

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
        onView(allOf(withId(R.id.remove_photo), isDisplayed())).perform(click())

        // Photo should be deleted.
        onView(withId(R.id.profile_photo)).check(matches(not(withImageUri(downloadedImagesUri[1]))))

        // Button to delete photo should be gone.
        onView(withId(R.id.remove_photo)).check(matches(not(isDisplayed())))
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

        performClickWithKeyboardClosed(R.id.skip)

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

        performClickWithKeyboardClosed(R.id.skip)

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

        performClickWithKeyboardClosed(R.id.skip)

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

        performClickWithKeyboardClosed(R.id.skip)

        onView(allOf(withId(R.id.ring_password_switch), isDisplayed())).perform(click())

        specifyPassword()

        onView(withId(R.id.create_account_password))
            .check(matches(allOf(isEnabled(), isDisplayed())))
    }

    @After
    fun removeAccount() =
        AccountUtils.removeAllAccounts()

    private fun createDefaultAccount() {
        onView(withId(R.id.ring_create_btn)).perform(scrollTo(), click())

        performClickWithKeyboardClosed(R.id.skip)

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
        JamiApplication.instance!!.mAccountService.customNameServer = "https://ns-test.jami.net/"

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

        Log.d(TAG, "Account created: $username")
    }

    private fun performClickWithKeyboardClosed(viewId: Int) {
        onView(isRoot()).perform(closeSoftKeyboard())
        onView(withId(viewId)).perform(waitUntil(isDisplayed()), click())
    }


    companion object {
        private val TAG = AccountCreation::class.java.simpleName

        fun skipBiometrics() { // Skip biometrics popup (only on P+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)
                onView(withText(R.string.no_thanks))
                    .inRoot(isDialogWithTitle(R.string.account_biometry_enroll_title))
                    .perform(waitUntil(isDisplayed()), click())
        }

        @JvmStatic
        private var downloadedImagesUri = listOf<Uri>()
    }
}
