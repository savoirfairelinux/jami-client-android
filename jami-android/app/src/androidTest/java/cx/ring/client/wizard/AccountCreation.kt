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
import androidx.core.os.bundleOf
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
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import cx.ring.AccountUtils
import cx.ring.ImageProvider
import cx.ring.R
import cx.ring.account.AccountWizardActivity
import cx.ring.account.ProfilePhotoFragment
import cx.ring.application.JamiApplication
import cx.ring.client.HomeActivity
import cx.ring.waitUntil
import cx.ring.hasTextInputLayoutError
import net.jami.utils.Log
import cx.ring.isDialogWithTitle
import cx.ring.waitForView
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

    @get:Rule
    val grantPermissionRuleLocalNetwork: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.ACCESS_LOCAL_NETWORK)

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
     * Checks if an identity can be created without filling anything.
     */
    @Test
    fun accountCreation_SkipAllSteps() = createDefaultAccount()

    /**
     * Checks if an identity can be created by specifying a username only.
     */
    @Test
    fun accountCreation_SpecifyUsernameOnly() {
        val randomUsername = "JamiTest_" + System.currentTimeMillis()
        createAccountWithUsername(randomUsername)
    }

    /**
     * Checks if an identity can be created by specifying a display name.
     */
    @Test
    fun accountCreation_SpecifyProfileName() {
        clickCreateAccountBtn()

        onView(allOf(withId(R.id.display_name), isDisplayed()))
            .perform(replaceText("Bonjour"), closeSoftKeyboard())

        clickCreateIdentityBtn()

        // Check that we are in the home activity.
        waitForView(withId(R.id.search_bar)).perform(waitUntil(isDisplayed()))

        // Go to identity settings. Click on search bar menu.
        onView(withId(R.id.menu_overflow)).perform(click())

        // Click on identity settings. Don't know why but doesn't work to select by ID.
        onView(allOf(withText(R.string.menu_item_identity_settings), isDisplayed())).perform(click())

        // Check if name is changed.
        onView(withId(R.id.username)).check(matches(withText("Bonjour")))
    }

    /**
     * Checks if an identity can be created by specifying a picture (via camera).
     */
    @Test
    fun accountCreation_SpecifyProfilePictureViaCamera() {
        clickCreateAccountBtn()

        // Click on camera (should launch a camera intent).
        onView(withId(R.id.camera)).perform(scrollTo(), click())

        returnPhotoFromProfilePhotoFragment(downloadedImagesUri[0])

        waitForView(withId(R.id.profile_photo)).perform(waitUntil(isDisplayed()))

        // Check if the image is displayed.
        onView(withId(R.id.profile_photo)).check(matches(withImageUri(downloadedImagesUri[0])))

        clickCreateIdentityBtn()

        // Check that we are in the home activity.
        waitForView(withId(R.id.search_bar)).perform(waitUntil(isDisplayed()))

        Thread.sleep(3000)

        // Go to identity settings. Click on search bar menu.
        onView(withId(R.id.menu_overflow)).perform(click())

        // Click on identity settings. Don't know why but doesn't work to select by ID.
        onView(allOf(withText(R.string.menu_item_identity_settings), isDisplayed())).perform(click())

        // Check if picture is changed in profile.
        onView(withId(R.id.user_photo)).check(matches(withImageUri(downloadedImagesUri[0])))
    }

    /**
     * Checks if an identity can be created by specifying a picture (via gallery).
     */
    @Test
    fun accountCreation_SpecifyProfilePictureViaGallery() {
        clickCreateAccountBtn()

        pickPhotoFromGallery(downloadedImagesUri[1])

        // Check if the image is displayed.
        onView(withId(R.id.profile_photo)).check(matches(withImageUri(downloadedImagesUri[1])))

        clickCreateIdentityBtn()

        // Check that we are in the home activity.
        waitForView(withId(R.id.search_bar)).perform(waitUntil(isDisplayed()))

        Thread.sleep(3000)

        // Go to identity settings. Click on search bar menu.
        onView(withId(R.id.menu_overflow)).perform(click())

        // Click on identity settings. Don't know why but doesn't work to select by ID.
        onView(allOf(withText(R.string.menu_item_identity_settings), isDisplayed())).perform(click())

        // Check if picture is changed in profile.
        onView(withId(R.id.user_photo)).check(matches(withImageUri(downloadedImagesUri[1])))
    }

    /**
     * Checks if a picture can be removed.
     */
    @Test
    fun accountCreation_SpecifyProfilePicture_CanCancel(){
        clickCreateAccountBtn()

        pickPhotoFromGallery(downloadedImagesUri[1])

        // Click on delete photo.
        onView(allOf(withId(R.id.remove_photo), isDisplayed())).perform(click())

        // Photo should be deleted.
        onView(withId(R.id.profile_photo)).check(matches(not(withImageUri(downloadedImagesUri[1]))))

        // Button to delete photo should be gone.
        onView(withId(R.id.remove_photo)).check(matches(not(isDisplayed())))
    }

    /**
     * Check what happens when writing an invalid username.
     * Assert that the create identity button is disabled.
     * Assert that the error message is displayed.
     */
    @Test
    fun usernameSelection_LessThan3Characters_InvalidUsername() {
        clickCreateAccountBtn()

        onView(withId(R.id.input_username)).perform(scrollTo(), replaceText("ab"), closeSoftKeyboard())

        onView(withId(R.id.input_username_txt_box))
            .perform(waitUntil(hasTextInputLayoutError(R.string.invalid_username)))

        onView(withId(R.id.create_identity_btn)).check(matches(isNotEnabled()))
    }

    /**
     * Check what happens when writing an already taken username.
     * Assert that the create identity button is disabled.
     * Assert that the error message is displayed.
     */
    @Test
    fun usernameSelection_UsernameAlreadyTaken() {
        clickCreateAccountBtn()

        onView(withId(R.id.input_username)).perform(scrollTo(), replaceText("abc"), closeSoftKeyboard())

        onView(withId(R.id.input_username_txt_box))
            .perform(waitUntil(hasTextInputLayoutError(R.string.username_already_taken)))

        onView(withId(R.id.create_identity_btn)).check(matches(isNotEnabled()))
    }

    /**
     * Check what happens when writing a valid username.
     * Assert that the create identity button is enabled.
     */
    @Test
    fun usernameSelection_ValidUsername() {
        clickCreateAccountBtn()

        val randomUsername = "JamiTest_" + System.currentTimeMillis()
        onView(allOf(withId(R.id.input_username), isDisplayed()))
            .perform(replaceText(randomUsername), closeSoftKeyboard())

        onView(withId(R.id.create_identity_btn)).perform(waitUntil(isEnabled()))
    }

    @After
    fun removeAccount() {
        try { Intents.release() } catch (_: IllegalStateException) {}
        AccountUtils.removeAllAccounts()
    }

    private fun clickCreateAccountBtn() {
        waitForView(withId(R.id.ring_create_btn)).perform(scrollTo(), click())
    }

    private fun clickCreateIdentityBtn() {
        onView(withId(R.id.create_identity_btn))
            .perform(scrollTo(), waitUntil(allOf(isDisplayed(), isEnabled())), click())
    }

    private fun pickPhotoFromGallery(uri: Uri) {
        // Start recording intents and subscribe to the callback.
        Intents.init()
        try {
            intending(hasAction(MediaStore.ACTION_PICK_IMAGES))
                .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(uri)))

            // Click on gallery (should launch a gallery explorer).
            onView(withId(R.id.gallery)).perform(scrollTo(), click())
        } finally {
            Intents.release()
        }
    }

    private fun createDefaultAccount() {
        clickCreateAccountBtn()
        clickCreateIdentityBtn()
    }

    private fun moveToWizard() {
        JamiApplication.instance!!.mAccountService.customNameServer = "https://ns-test.jami.net/"

        try {
            onView(withContentDescription(R.string.searchbar_navigation_identity)).perform(click())
            onView(withText(R.string.add_ring_identity_title)).perform(click())
        } catch (_: Exception) { // Already in the wizard ?
            // Todo: Should check before exception where we are.
        }
    }

    private fun createAccountWithUsername(username: String) {
        clickCreateAccountBtn()

        onView(allOf(withId(R.id.input_username), isDisplayed()))
            .perform(replaceText(username), closeSoftKeyboard())

        clickCreateIdentityBtn()

        Log.d(TAG, "Identity created: $username")
    }

    private fun returnPhotoFromProfilePhotoFragment(uri: Uri) {
        val wizard = getCurrentActivity() as AccountWizardActivity
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        instrumentation.runOnMainSync {
            wizard.supportFragmentManager.setFragmentResult(
                ProfilePhotoFragment.REQUEST_KEY_PHOTO,
                bundleOf(ProfilePhotoFragment.RESULT_URI to uri.toString())
            )
            wizard.supportFragmentManager.popBackStackImmediate()
        }
    }

    private fun getCurrentActivity(): Activity {
        val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
        val activities = arrayOfNulls<Activity>(1)
        instrumentation.runOnMainSync {
            val resumed = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
            activities[0] = resumed.firstOrNull()
        }
        return activities[0] ?: throw IllegalStateException("No RESUMED activity")
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
