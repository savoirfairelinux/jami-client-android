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
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.intent.IntentCallback
import androidx.test.runner.intent.IntentMonitorRegistry
import cx.ring.AccountUtils
import cx.ring.R
import cx.ring.client.HomeActivity
import cx.ring.client.HomeActivity.Companion.REQUEST_CODE_PHOTO
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.jami.model.Account
import net.jami.utils.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class AccountSettings {

    @Rule
    @JvmField
    var mActivityScenarioRule = ActivityScenarioRule(HomeActivity::class.java)

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
            val accountList =
                AccountUtils.createAccountAndRegister(activity.mAccountService, 1)

            accountA = accountList[0]

            // Need delay to give time to accounts to register on DHT before sending trust request.
            // Inferior delay will occasionally cause the trust request to fail.
//            Thread.sleep(10000)

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

    private fun createImageGallerySetResultStub(uri:Uri): Instrumentation.ActivityResult {
        val bundle = Bundle()
        val parcels = ArrayList<Parcelable>()
        val resultData = Intent()
//        var dir: File? = null
//        mActivityScenarioRule.scenario.onActivity { dir = it.externalCacheDir }
//        val file = File(dir?.path, "myImageResult.jpeg")
//        val uri = Uri.fromFile(file)
        val myParcelable = uri as Parcelable
        parcels.add(myParcelable)
        bundle.putParcelableArrayList(Intent.EXTRA_STREAM, parcels)
        resultData.putExtras(bundle)
        return Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)
    }

    private fun savePickedImage() {
        // Load bitmap from resource
        var bm: Bitmap? = null
        mActivityScenarioRule.scenario.onActivity {

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeResource(it.resources, R.mipmap.ic_launcher, options)
            assertTrue(options.outWidth > 0 && options.outHeight > 0)

            bm = BitmapFactory.decodeResource(it.resources, R.mipmap.ic_launcher)
            assertTrue(bm != null)
        }

        // Prepare file to save bitmap
        var dir: File? = null
        mActivityScenarioRule.scenario.onActivity { dir = it.externalCacheDir }
        val file = File(dir?.path, "myImageResult.jpeg")
        System.out.println(file.absolutePath)

        // Save bitmap to file
        val outStream: FileOutputStream?
        try {
            outStream = FileOutputStream(file)
            bm!!.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
            outStream.flush()
            outStream.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
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


    @Test
    fun c1_changeProfileImage_fromCamera(){
        //    android:id="@+id/user_photo"

        // https://stackoverflow.com/questions/26469661/how-to-click-on-android-gallery-with-espresso

        // Click on profile image
        onView(withId(R.id.user_photo)).perform(click())

        var v:Uri? = null
        mActivityScenarioRule.scenario.onActivity {
            CoroutineScope(Dispatchers.IO).launch {
                v = downloadImageToUri(
                    "https://file-examples.com/storage/fe44eeb9cb66ab8ce934f14/2017/10/file_example_JPG_100kB.jpg",
                    it.baseContext
                )
                Log.w(TAG, "v: $v")
            }
        }

//        Thread.sleep(1000)

        // Prepare a result to be returned by the Intent
        val resultData = Intent().apply {
            data = v
        }
        val result = Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)

//        mActivityScenarioRule.scenario.onActivity { activity ->
        val intentCallback = IntentCallback {
            Log.w("devdebug-2", it.action.toString())
            if (it.action == MediaStore.ACTION_IMAGE_CAPTURE) {
                Log.w("devdebug-2", "ACTION_IMAGE_CAPTURE catch")
                it.extras?.getUri(MediaStore.EXTRA_OUTPUT)!!.run {
                    Log.w("devdebug-2", "uri: $this")

                    mActivityScenarioRule.scenario.onActivity {
                        Log.w("devdebug-2", "image setup")
                        val inStream = it.contentResolver.openInputStream(v!!)
                        val outputStream = it.contentResolver.openOutputStream(this)
                        Log.w("devdebug-2", "image loading")
                        inStream?.use { input ->
                            outputStream?.use { output ->
                                input.copyTo(output)
                            }
                        }
                        Log.w("devdebug-2", "image saved")

                    }

                }
            }
        }

        Intents.init()
        intending(hasAction(MediaStore.ACTION_IMAGE_CAPTURE)).respondWith(result)

        IntentMonitorRegistry.getInstance().addIntentCallback(intentCallback)




        onView(withId(R.id.camera)).perform(click())


        IntentMonitorRegistry.getInstance().removeIntentCallback(intentCallback)

        Thread.sleep(10000)

//        }


//        Intents.init()
        // Mock the intent that will be used to pick the image
//        intending(hasAction(Intent.ACTION_PICK)).respondWith(result)

        // Intercept the camera intent and return the mock result
//        intending(
//            allOf(
//                hasAction(MediaStore.ACTION_IMAGE_CAPTURE),
//            toPackage("com.android.camera")
//            )
//        ).respondWith(result)



    }

//    @Test
    fun c2_changeProfileImage_fromGallery(){}

//    @Test
    fun c3_changeProfileImage_remove(){}
}

fun Bundle.getUri(key: String): Uri? {
    return getParcelable(key, Uri::class.java)
}
