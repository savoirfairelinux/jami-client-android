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
package cx.ring.client.settings

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.replaceText
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
import cx.ring.DrawableMatcher
import cx.ring.R
import cx.ring.client.HomeActivity
import cx.ring.hasTextInputLayoutError
import cx.ring.waitUntil
import cx.ring.withImageUri
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.jami.model.Account
import net.jami.utils.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class AccountSettings {

    @Rule
    @JvmField
    var mActivityScenarioRule = ActivityScenarioRule(HomeActivity::class.java)

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    companion object {
        @JvmStatic
        private var accountCreated = false

        @JvmStatic // Account A will be the one sending the trust request.
        private var accountA: Account? = null

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

        mActivityScenarioRule.scenario.onActivity { activity ->
//            accountA = AccountUtils.createAccountAndRegister(activity.mAccountService, 1)[0]

            accountA = AccountUtils.createAccount(activity.mAccountService, 1)[0]

            accountCreated = true
            Log.d(TAG, "Account created.")
        }
    }

    @Before
    fun goToAccountSettings() {

        if (!accountCreated) return // First executed test will be the setup test.

        // Go to account settings
        // Click on search bar menu
        onView(withId(R.id.menu_overflow)).perform(click())

        // Click on account settings
        // Don't know why but doesn't work to select by ID.
        val accountSettingString = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.menu_item_account_settings)
        onView(allOf(withText(accountSettingString), isDisplayed())).perform(click())
    }

//    @Test
    fun b_changeProfileName() {
        val name = "JamiTestName"

        // Click on name field
        onView(withId(R.id.usernameField)).perform(click())

        // Change name
        // Press confirm on keyboard
        onView(withId(R.id.username)).perform(typeText(name), pressImeActionButton())

        // Check if name is changed
        onView(withId(R.id.username)).check(matches(withText(name)))
    }


    // Méthode pour télécharger l'image depuis une URL et obtenir l'Uri locale
    fun downloadImageToUri(imageUrl: String, context: Context): Uri? {
        val client = OkHttpClient()
        val request = Request.Builder().url(imageUrl).build()

        return try {
            val response = client.newCall(request).execute()
            val inputStream: InputStream? = response.body?.byteStream()

            inputStream?.let {
                val file = File(context.cacheDir, "downloaded_image.jpg")
                val outputStream = FileOutputStream(file)

                val buffer = ByteArray(1024)
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.close()
                inputStream.close()

                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


//    @Test
    fun c1_changeProfileImage_FromCamera(){
        // Click on profile image
        onView(withId(R.id.user_photo)).perform(click())

        // Download image from URL
        var downloadedImageUri: Uri? = null
        mActivityScenarioRule.scenario.onActivity {
            runBlocking {
                val deferredResult = CompletableDeferred<Uri?>()
                CoroutineScope(Dispatchers.IO).launch {
                    downloadedImageUri = downloadImageToUri(
                        "https://file-examples.com/storage/fe44eeb9cb66ab8ce934f14/2017/10/file_example_JPG_100kB.jpg",
                        it.baseContext
                    )
                    Log.w(TAG, "Image downloaded to: $downloadedImageUri")
                    deferredResult.complete(downloadedImageUri)
                }
                // This will block the test until the image is downloaded
                deferredResult.await()
            }
        }

        // Do something with the intent
        val intentCallback = IntentCallback {
            if (it.action == MediaStore.ACTION_IMAGE_CAPTURE) {
                it.extras!!.getUri(MediaStore.EXTRA_OUTPUT)!!.run {
                    mActivityScenarioRule.scenario.onActivity { activity ->
                        // Copy the downloaded image to the intent uri
                        val inStream = activity.contentResolver.openInputStream(downloadedImageUri!!)
                        val outStream = activity.contentResolver.openOutputStream(this)
                        inStream?.use { input -> outStream?.use { output -> input.copyTo(output) } }
                    }
                }
            }
        }

        // Start recording intents
        Intents.init()

        // Intercept the camera intent (prevent the camera from opening)
        intending(hasAction(MediaStore.ACTION_IMAGE_CAPTURE))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

        IntentMonitorRegistry.getInstance().addIntentCallback(intentCallback)

        onView(withId(R.id.camera)).perform(click())

        IntentMonitorRegistry.getInstance().removeIntentCallback(intentCallback)

        // Stop recording intents
        Intents.release()

        // Check if the image is displayed
        onView(withId(R.id.profile_photo)).check(matches(withImageUri(downloadedImageUri!!)))

        // Click on OK
        onView(withId(android.R.string.ok)).perform(click())

        Thread.sleep(100000)
    }

//    @Test
    fun c2_changeProfileImage_FromGallery(){}

//    @Test
    // Feature not implemented.
    fun c3_removeProfileImage(){}

//    @Test
    fun d1_registerAccount_NameNotRegisteredFieldIsEnabled() {
        val notRegisteredString = InstrumentationRegistry.getInstrumentation()
            .targetContext.getString(R.string.no_registered_name_for_account)
        onView(withId(R.id.registered_name)).check(matches(withText(notRegisteredString)))
    }

//    @Test
    fun e1_registerAccount_LessThan3CharactersInvalidUsername() {
        // Click on register name
        onView(withId(R.id.register_name)).perform(click())

        onView(withId(R.id.input_username)).perform(replaceText("ab"), closeSoftKeyboard())

        val errorString = InstrumentationRegistry.getInstrumentation()
            .targetContext.getString(R.string.invalid_username)
        onView(withId(R.id.input_username_txt_box))
            .perform(waitUntil(hasTextInputLayoutError(errorString)))
    }

//    @Test
    fun e2_registerAccount_UsernameAlreadyTaken() {
        IdlingPolicies.setMasterPolicyTimeout(10, TimeUnit.SECONDS);
        // Click on register name
        onView(withId(R.id.register_name)).perform(click())

        onView(withId(R.id.input_username)).perform(typeText("abc"), closeSoftKeyboard())

        val errorString = InstrumentationRegistry
            .getInstrumentation().targetContext.getString(R.string.username_already_taken)
        onView(withId(R.id.input_username_txt_box))
            .perform(waitUntil(hasTextInputLayoutError(errorString)))
    }

    @Test
    fun e3_registerAccount_ValidUsername() {
        // Click on register name
        onView(withId(R.id.register_name)).perform(click())

        val randomUsername = "JamiTest_" + System.currentTimeMillis()
        onView(allOf(withId(R.id.input_username), isDisplayed()))
            .perform(replaceText(randomUsername), closeSoftKeyboard())

        onView(withId(R.id.input_username_txt_box)).check(matches(hasTextInputLayoutError("")))
    }

//    @Test
//    fun e2_registerAccount_nameRegisteredFieldIsDisabled() {}
}

fun Bundle.getUri(key: String): Uri? {
    return getParcelable(key, Uri::class.java)
}
