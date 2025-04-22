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
package cx.ring.client.settings

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
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
import cx.ring.client.wizard.AccountCreation
import cx.ring.hasTextInputLayoutError
import cx.ring.isDialogWithTitle
import cx.ring.utils.ContentUri.getUri
import cx.ring.waitForView
import cx.ring.waitUntil
import cx.ring.withImageUri
import junit.framework.TestCase.assertTrue
import net.jami.model.Account
import net.jami.utils.Log
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File

@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class AccountSettings {

    @Rule
    @JvmField
    var mActivityScenarioRule = ActivityScenarioRule(HomeActivity::class.java)

    @get:Rule
    val grantPermissionRuleCamera: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    @get:Rule
    val grantPermissionRuleNotification: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS)

    companion object {
        @JvmStatic
        private var accountCreated = false

        @JvmStatic
        private var accountAProfileName = "JamiTestName"

        @JvmStatic // Account A will be the one sending the trust request.
        private var accountA: Account? = null

        @JvmStatic
        private var downloadedImagesUri = listOf<Uri>()

        @JvmStatic
        val ARCHIVE_NO_PASSWORD_FILE_NAME = "backup_no_password.gz"

        @JvmStatic
        val ARCHIVE_PASSWORD_FILE_NAME = "backup_password.gz"

        private val TAG = AccountSettings::class.java.simpleName
    }

    /**
     * This test MUST be the first one because it creates the accounts.
     */
    @Test
    fun a_setup() {
        // Doing this in a test is not ideal.
        // Ideally, it should be in an `@Before` method, but the problem with `@Before` is that
        // it is executed on the same activity than the test, while we need to restart to see the
        // accounts on the app.
        // `@BeforeClass` could be used, but it does not have access to the activity.
        Log.d(TAG, "Creating accounts ...")

        accountA = AccountUtils.createAccount(1)[0]
        accountCreated = true
        Log.d(TAG, "Account created.")

        // Get assets if installed (should be in /data/local/tmp/jami_test_assets).
        val image1 = File("/data/local/tmp/jami_test_assets/image/image1.jpg")
        val image2 = File("/data/local/tmp/jami_test_assets/image/image1.jpg")
        if(image1.exists() && image2.exists()) {
            Log.d(TAG, "Images found in assets. Using them.")
            downloadedImagesUri = listOf(Uri.fromFile(image1), Uri.fromFile(image2))
        }
        else { // Download images from URL.
            Log.d(TAG, "Downloading images ...")
            InstrumentationRegistry.getInstrumentation().targetContext
                .let { downloadedImagesUri = ImageProvider().downloadImagesToUri(it, 2) }
            Log.d(TAG, "Images downloaded.")
        }
    }

    @Before
    fun goToAccountSettings() {

        if (!accountCreated) return // Skip first test (setup).

        // Go to account settings. Click on search bar menu.
        onView(withId(R.id.menu_overflow)).perform(click())

        // Click on account settings. Don't know why but doesn't work to select by ID.
        onView(allOf(withText(R.string.menu_item_account_settings), isDisplayed())).perform(click())
    }

    /**
     * Test we can change the profile name.
     * This test doesn't verify it is propagated to the rest of the app.
     */
    @Test
    fun b1_changeProfileName_CanChange() {
        // Click on name field.
        onView(withId(R.id.usernameField)).perform(click())

        // Change name. Press confirm on keyboard.
        onView(withId(R.id.username)).perform(typeText(accountAProfileName), pressImeActionButton())

        // Check if name is changed.
        onView(withId(R.id.username)).check(matches(withText(accountAProfileName)))
    }

    @Test
    fun b2_changeProfileName_IsPersistent() {
        // Check if name is changed.
        onView(withId(R.id.username)).check(matches(withText(accountAProfileName)))
    }

    /**
     * Test we can change the profile image from camera.
     * This test doesn't verify it is propagated to the rest of the app.
     */
    @Test
    fun c1_changeProfileImage_FromCamera() {
        // Click on profile image.
        onView(withId(R.id.user_photo)).perform(click())

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

        // Click on OK.
        onView(withText(android.R.string.ok)).perform(click())

        // Check if the image is displayed.
        onView(withId(R.id.user_photo)).perform(waitUntil(withImageUri(downloadedImagesUri[0])))
    }

    /**
     * Test we can change the profile image from gallery.
     * This test doesn't verify it is propagated to the rest of the app.
     */
    @Test
    fun c2_changeProfileImage_FromGallery() {
        // Click on profile image.
        onView(withId(R.id.user_photo)).perform(click())

        // Start recording intents.
        Intents.init()

        intending(hasAction(MediaStore.ACTION_PICK_IMAGES))
            .respondWith(
                Instrumentation.ActivityResult(
                    Activity.RESULT_OK,
                    Intent().setData(downloadedImagesUri[1])
                )
            )

        // Click on gallery (should launch a gallery explorer intent).
        onView(withId(R.id.gallery)).perform(click())

        // Stop recording intents.
        Intents.release()

        // Check if the image is displayed.
        onView(withId(R.id.profile_photo)).check(matches(withImageUri(downloadedImagesUri[1])))

        // Click on OK.
        onView(withText(android.R.string.ok)).perform(click())

        // Check if the image is displayed.
        onView(withId(R.id.user_photo)).perform(waitUntil(withImageUri(downloadedImagesUri[1])))
    }

    /**
     * Test we can remove the profile image.
     * This test doesn't verify it is propagated to the rest of the app.
     */
    @Test
    fun c3_removeProfileImage() {
        // Click on profile image.
        onView(withId(R.id.user_photo)).perform(click())

        // Click on gallery (should launch a gallery explorer intent).
        onView(allOf(withId(R.id.remove_photo), isDisplayed())).perform(click())

        // Check if the image is removed.
        onView(withId(R.id.profile_photo)).check(matches(not(withImageUri(downloadedImagesUri[1]))))

        // Check if the button is disabled.
        onView(withId(R.id.remove_photo)).perform(waitUntil(not(isDisplayed())))

        // Click on OK.
        onView(withText(android.R.string.ok)).perform(click())

        // Check if the image is removed.
        onView(withId(R.id.user_photo))
            .perform(waitUntil(not(withImageUri(downloadedImagesUri[1]))))
    }

    /**
     * Test the register name field/button is enabled when the name is not registered.
     */
    @Test
    fun d1_registerName_NameNotRegisteredFieldIsEnabled() {
        // Check the registered name is unset.
        onView(withId(R.id.registered_name))
            .check(matches(withText(R.string.no_registered_name_for_account)))

        // Check that the register button is enabled.
        onView(withId(R.id.register_name)).check(matches(allOf(isDisplayed(), isEnabled())))
    }

    /**
     * Test that in invalid username displays an error.
     * This test doesn't validate the register name.
     */
    @Test
    fun e1_registerName_LessThan3CharactersInvalidUsername() {
        // Click on register name.
        onView(withId(R.id.register_name)).perform(click())

        // Type invalid username.
        onView(withId(R.id.input_username)).perform(typeText("ab"), closeSoftKeyboard())

        // Verify that the error is displayed.
        onView(withId(R.id.input_username_txt_box))
            .perform(waitUntil(hasTextInputLayoutError(R.string.invalid_username)))
    }

    /**
     * Test that an already taken username displays an error.
     * This test doesn't validate the register name.
     */
    @Test
    fun f1_registerName_UsernameAlreadyTaken() {
        // Click on register name.
        onView(withId(R.id.register_name)).perform(click())

        // Type already taken username.
        onView(withId(R.id.input_username)).perform(typeText("abc"), closeSoftKeyboard())

        // Verify that the error is displayed.
        onView(withId(R.id.input_username_txt_box))
            .perform(waitUntil(hasTextInputLayoutError(R.string.username_already_taken)))
    }

    /**
     * Test that a valid username doesn't display an error.
     * This test doesn't validate the register name.
     */
    @Test
    fun g1_registerName_ValidUsername() {
        // Click on register name.
        onView(withId(R.id.register_name)).perform(click())

        // Type valid username.
        val randomUsername = "jamitest_" + System.currentTimeMillis()
        onView(allOf(withId(R.id.input_username), isDisplayed()))
            .perform(replaceText(randomUsername), closeSoftKeyboard())

        // Verify that no error is displayed.
        onView(withId(R.id.input_username_txt_box)).perform(waitUntil((hasTextInputLayoutError(null))))
    }

    /**
     * Test that a valid username can be registered.
     * This test do validates the register name.
     */
    @Test
    fun h1_registerName_Complete() {
        // Click on register name.
        onView(withId(R.id.register_name)).perform(click())

        // Type valid username.
        val randomUsername = "jamitest_" + System.currentTimeMillis()
        onView(allOf(withId(R.id.input_username), isDisplayed()))
            .perform(typeText(randomUsername), closeSoftKeyboard())

        // Verify no error is displayed.
        onView(withId(R.id.input_username_txt_box))
            .perform(waitUntil(hasTextInputLayoutError(null)))

        // Click on OK.
        onView(withText(android.R.string.ok)).perform(click())

        // Check that the registered name is displayed.
        onView(withId(R.id.registered_name)).check(matches(withText(randomUsername)))

        // Check that the `register name` button is disabled.
        onView(withId(R.id.register_name)).check(matches(not(isDisplayed())))
    }

    /**
     * Test that there is no option to enable biometrics when the password is not set.
     */
    @Test
    fun i1_noPassword_CantEnableBiometrics() {
        // Click on the `Account` button.
        onView(allOf(withId(R.id.settings_account), isDisplayed()))
            .perform(scrollTo(), click())

        // Cant see the biometric switch.
        onView(withId(R.id.system_biometric_title)).check(matches(not(isDisplayed())))
    }

    /**
     * Test there is an error displayed if the password is less than 6 characters (invalid).
     */
    @Test
    fun j1_setPassword_LessThan6Characters_InvalidPassword() {
        // Click on the `Account` button.
        onView(allOf(withId(R.id.settings_account), isDisplayed()))
            .perform(scrollTo(), click())

        // Click on `Set password` button.
        onView(withId(R.id.system_change_password_title)).perform(click())

        // Type and recopy invalid password.
        onView(withId(R.id.new_password_txt)).perform(typeText("12345"), closeSoftKeyboard())
        onView(withId(R.id.new_password_repeat_txt)).perform(typeText("12345"), closeSoftKeyboard())

        // Click on `Set Password`.
        onView(withId(android.R.id.button1)).perform(click())

        // Check that the error is displayed.
        onView(withId(R.id.new_password_txt_box))
            .check(matches(hasTextInputLayoutError(R.string.error_password_char_count)))
        onView(withId(R.id.new_password_repeat_txt_box))
            .check(matches(hasTextInputLayoutError(R.string.error_password_char_count)))
    }

    /**
     * Test there is an error displayed if the password recopy is different.
     */
    @Test
    fun k1_setPassword_WrongRecopy() {
        // Click on the `Account` button.
        onView(allOf(withId(R.id.settings_account), isDisplayed()))
            .perform(scrollTo(), click())

        // Click on `Set password` button.
        onView(withId(R.id.system_change_password_title)).perform(click())

        // Type and badly recopy password.
        onView(withId(R.id.new_password_txt)).perform(typeText("123456"), closeSoftKeyboard())
        onView(withId(R.id.new_password_repeat_txt)).perform(typeText("12345"), closeSoftKeyboard())

        // Click on `Set Password`.
        onView(withId(android.R.id.button1)).perform(click())

        // Check that the error is displayed.
        onView(withId(R.id.new_password_txt_box))
            .check(matches(hasTextInputLayoutError(R.string.error_passwords_not_equals)))
        onView(withId(R.id.new_password_repeat_txt_box))
            .check(matches(hasTextInputLayoutError(R.string.error_passwords_not_equals)))
    }

    /**
     * Test we can set a valid password.
     */
    @Test
    fun l1_setPassword_ValidPassword() {
        // Click on the `Account` button.
        onView(allOf(withId(R.id.settings_account), isDisplayed()))
            .perform(scrollTo(), click())

        // Click on `Set password` button.
        onView(withId(R.id.system_change_password_title)).perform(click())

        // Type and recopy password.
        onView(withId(R.id.new_password_txt)).perform(typeText("123456"), closeSoftKeyboard())
        onView(withId(R.id.new_password_repeat_txt)).perform(
            typeText("123456"),
            closeSoftKeyboard()
        )

        // Click on `Set Password`.
        onView(withId(android.R.id.button1)).perform(click())

        // Skip biometric popup if there is one.
        AccountCreation.skipBiometrics()

        // Check that the password is set.
        onView(withId(R.id.system_change_password_title))
            .check(matches(withText(R.string.account_password_change)))
    }

    /**
     * Check we can enable biometrics when the password is set.
     */
    @Test
    fun l2_passwordSet_CanEnableBiometrics() {
        // Click on the `Account` button.
        onView(allOf(withId(R.id.settings_account), isDisplayed()))
            .perform(scrollTo(), click())

        // Can see the biometric switch.
        onView(withId(R.id.system_biometric_title)).check(matches(isDisplayed()))
    }

    /**
     * Test we can export a backup while the account is protected with a password.
     */
    @Test
    fun l3_exportBackup_Password() {
        // Click on the `Account` button.
        onView(allOf(withId(R.id.settings_account), isDisplayed()))
            .perform(scrollTo(), click())

        // Click on `Backup account` button.
        onView(withId(R.id.system_export_title)).perform(click())

        // Give password.
        onView(withId(R.id.password_txt)).perform(typeText("123456"), closeSoftKeyboard())

        // Valid password.
        onView(withId(android.R.id.button1)).perform(click())

        onView(withText(R.string.export_account_wait_message)).check(matches(isDisplayed()))

        // Build URI where to save backup.
        val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val file = File(cache, ARCHIVE_PASSWORD_FILE_NAME)
        val uri = Uri.fromFile(file)

        // Start recording intents
        Intents.init()

        // Intercept the intent (prevent the system from opening file explorer)
        intending(hasAction(Intent.ACTION_CREATE_DOCUMENT))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(uri)))

        // Wait for `exporting backup` to display.
        onView(withText(R.string.export_account_wait_message))
            .inRoot(isDialogWithTitle(R.string.export_account_wait_title))
            .perform(waitUntil(isDisplayed()))

        // Wait for `exporting backup` dialog to disappear.
        // Give time to the system to generate the file (will launch a save file intent).
        waitForView(withId(R.id.settings_account)).check(matches(isDisplayed()))

        // Stop recording intents
        Intents.release()

        // Verify that the file is created (and not empty).
        assertTrue(file.exists() && file.length() > 0)
    }

    /**
     * Test we can remove the password.
     */
    @Test
    fun l4_setPassword_Remove() {
        // Click on the `Account` button.
        onView(allOf(withId(R.id.settings_account), isDisplayed()))
            .perform(scrollTo(), click())

        // Click on `Set password` button.
        onView(withId(R.id.system_change_password_title)).perform(click())

        // Will open a dialog.
        // Type old password.
        onView(withId(R.id.old_password_txt)).perform(typeText("123456"), closeSoftKeyboard())

        // Click on `Change Password`. Null new password will delete it.
        onView(withId(android.R.id.button1)).perform(click())

        // Verify that the password is displayed as unset.
        waitForView(withText(R.string.account_password_set)).check(matches(isDisplayed()))
    }

    @Test
    fun m1_exportBackup_NoPassword() {
        // Click on the `Account` button.
        onView(allOf(withId(R.id.settings_account), isDisplayed()))
            .perform(scrollTo(), click())

        // Start recording intents
        Intents.init()

        // Build URI where to save backup.
        val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val file = File(cache, ARCHIVE_NO_PASSWORD_FILE_NAME)
        val uri = Uri.fromFile(file)

        // Intercept the intent (prevent the system from opening file explorer)
        intending(hasAction(Intent.ACTION_CREATE_DOCUMENT))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(uri)))

        // Click on `Backup account` button.
        onView(withId(R.id.system_export_title)).perform(click())

        // Stop recording intents
        Intents.release()

        // Verify that the file is created (and not empty).
        assertTrue(file.exists() && file.length() > 0)
    }

    /**
     * Test we can remove the account.
     */
    @Test
    fun m2_removeAccount() {
        // Click on the `Account` button.
        onView(allOf(withId(R.id.settings_account), isDisplayed()))
            .perform(scrollTo(), click())

        // Click on `Delete Account`.
        onView(withId(R.id.system_delete_account_title)).perform(click())

        // Click on `Delete`.
        onView(withId(android.R.id.button1)).perform(click())

        // Check that we came back to account creation.
        onView(withId(R.id.ring_create_btn)).check(matches(isDisplayed()))

        accountCreated = false
    }

    /**
     * Test we can import a backup without password.
     */
    @Test
    fun m3_importAccount_NoPassword() {
        // Start recording intents
        Intents.init()

        // Prepare backup path.
        val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val file = File(cache, ARCHIVE_NO_PASSWORD_FILE_NAME)
        val uri = Uri.fromFile(file)

        // Intercept the intent (prevent the system from opening file explorer)
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(uri)))

        // Click on `Connect from backup`
        onView(withId(R.id.ring_import_account)).perform(click())

        // Stop recording intents
        Intents.release()

        // Confirm connection.
        onView(withId(R.id.link_button)).perform(click())

        // Check that we are in the home activity.
        waitForView(withId(R.id.search_bar)).perform(waitUntil(isDisplayed()))

        // Remove account
        AccountUtils.removeAllAccounts()
    }

    @Test
    fun m4_importAccount_Password() {
        // Start recording intents
        Intents.init()

        // Build URI where to save backup.
        val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val file = File(cache, ARCHIVE_PASSWORD_FILE_NAME)
        val uri = Uri.fromFile(file)

        // Intercept the intent (prevent the system from opening file explorer)
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(uri)))

        // Click on `Connect from backup`
        onView(withId(R.id.ring_import_account)).perform(click())

        // Stop recording intents
        Intents.release()

        // Type and confirm password.
        onView(withId(R.id.existing_password)).perform(typeText("123456"), closeSoftKeyboard())
        onView(withId(R.id.link_button)).perform(click())

        try {
            AccountCreation.skipBiometrics()
        } catch (e: Exception) {
            // Dialog not shown — safe to ignore.
            Log.d(TAG, "Biometrics dialog not shown, skipping...")
        }

        // Check that we are in the home activity.
        onView(withId(R.id.search_bar)).check(matches(isDisplayed()))

        // Remove account.
        AccountUtils.removeAllAccounts()
    }
}