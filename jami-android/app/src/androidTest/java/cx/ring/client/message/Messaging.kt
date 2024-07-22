package cx.ring.client.message

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.Companion.ACTION_SYSTEM_FALLBACK_PICK_IMAGES
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressBack
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cx.ring.AccountUtils
import cx.ring.R
import cx.ring.client.HomeActivity
import cx.ring.client.addcontact.AccountNavigationUtils
import cx.ring.viewholders.SmartListViewHolder
import cx.ring.waitUntil
import net.jami.model.Account
import net.jami.model.Conversation
import net.jami.model.Uri
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anyOf
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class Messaging {

    companion object {
        const val TEST_MESSAGE = "my test message"

        @JvmStatic
        private lateinit var accountA: Account

        @JvmStatic
        private lateinit var accountB: Account

        private var accountsCreated = false
    }

    private val imageFileName = "testImage_${System.currentTimeMillis()}.jpeg"

    @JvmField
    @Rule val mActivityScenarioRule = ActivityScenarioRule(HomeActivity::class.java)

    @Before fun setup() {

        if (accountsCreated) return

        mActivityScenarioRule.scenario.onActivity { activity ->
            val accountList = AccountUtils.createAccountAndRegister(activity.mAccountService, 2)

            // Need delay to give time to accounts to register on DHT before sending trust request.
            // Inferior delay will occasionally cause the trust request to fail.
            Thread.sleep(10000)

            accountA = accountList[0]
            accountB = accountList[1]

            // AccountB sends trust request to accountA
            val uri = Uri.fromString(accountB.uri!!)
            val conversation = Conversation(accountA.accountId, uri, Conversation.Mode.Request)
            activity.mAccountService.sendTrustRequest(conversation, uri)
            conversation.setMode(Conversation.Mode.Syncing)

            // AccountA accepts trust request from accountB
            val invitation = accountB.getPendingSubject().skip(1).blockingFirst().first()
            activity.mAccountService.acceptTrustRequest(accountB.accountId, invitation.uri)

            accountsCreated = true
        }

        // Restart the activity to make load accounts
        mActivityScenarioRule.scenario.close()
        ActivityScenario.launch(HomeActivity::class.java)
    }

    @Test fun t1_sendText() {

        // Current account is accountB. Go to conversation with accountA
        onView(withId(R.id.confs_list)).perform(
            waitUntil(hasDescendant(allOf(withText(accountA.displayUri!!), isDisplayed()))),
            RecyclerViewActions.actionOnItem<SmartListViewHolder>(
                hasDescendant(withText(accountA.displayUri!!)), click())
        )

        // type text msg
        onView(withId(R.id.msg_input_txt)).perform(waitUntil(isDisplayed()), typeText(TEST_MESSAGE))

        // click send button
        onView(withId(R.id.msg_send)).check(matches(isDisplayed())).perform(click())

        onView(isRoot()).perform(closeSoftKeyboard(), pressBack())

        // check if message is displayed
        onView(allOf(withText(TEST_MESSAGE), isDescendantOfA(withId(R.id.hist_list))))
            .perform(waitUntil(isDisplayed()))

        //Test Receive =====================================
        // cancel all push notifications to not let them hide the underlying views
        NotificationManagerCompat.from(InstrumentationRegistry.getInstrumentation().targetContext).cancelAll()

        // be sure that all push notifications were dismissed
        Thread.sleep(3000)

    }

    @Test fun t2_receiveText() {

        AccountNavigationUtils.moveToAccount(accountA.displayUri!!)

        // go to conversation
        onView(withId(R.id.confs_list)).perform(
            waitUntil(hasDescendant(allOf(withText(accountB.displayUri!!), isDisplayed()))),
            RecyclerViewActions.actionOnItem<SmartListViewHolder>(
                hasDescendant(withText(accountB.registeredName)), click())
        )

        // check if message was received
        onView(allOf(withText(TEST_MESSAGE), isDescendantOfA(withId(R.id.hist_list))))
            .perform(waitUntil(isDisplayed()))
    }

    @Test
    fun t3_sendImage() {

        // Current account is accountB. Go to conversation with accountA
        onView(withId(R.id.confs_list)).perform(
            waitUntil(hasDescendant(allOf(withText(accountA.displayUri!!), isDisplayed()))),
            RecyclerViewActions.actionOnItem<SmartListViewHolder>(
                hasDescendant(withText(accountA.displayUri!!)), click())
        )

        // open conversation popup menu
        onView(withId(R.id.btn_menu)).perform(waitUntil(isDisplayed()), click())

        val imageUri = drawableToUri(R.drawable.ic_jami) ?: throw Exception("Test image uri is null")

        Intents.init()

        // cover all of the possible intents types from:
        // registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(...)
        Intents.intending(anyOf(
            IntentMatchers.hasAction(MediaStore.ACTION_PICK_IMAGES),
            IntentMatchers.hasAction(ACTION_SYSTEM_FALLBACK_PICK_IMAGES),
            IntentMatchers.hasAction("com.google.android.gms.provider.action.PICK_IMAGES"))
        ).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(imageUri)))

        // select "Open gallery" from the menu --> send intent to gallery
        onView(withText(R.string.select_media)).inRoot(RootMatchers.isPlatformPopup()).perform(click())

        Intents.release()

        // todo: check the image displayed

        // go to accountA and check if message was received
        onView(isRoot()).perform(closeSoftKeyboard(), pressBack())

    }

    //@Test
    fun t4_receiveImage() {
        AccountNavigationUtils.moveToAccount(accountA.displayUri!!)

        // go to conversation
        onView(withId(R.id.confs_list)).perform(
            waitUntil(hasDescendant(allOf(withText(accountB.displayUri!!), isDisplayed()))),
            RecyclerViewActions.actionOnItem<SmartListViewHolder>(
                hasDescendant(withText(accountB.registeredName)), click())
        )

        onView(allOf( withId(R.id.image), isDescendantOfA(withId(R.id.hist_list))))
            //.check()
    }

    @Test fun z_clear() {
        // clear created accounts
        mActivityScenarioRule.scenario.onActivity { activity ->
            AccountUtils.removeAllAccounts(accountService = activity.mAccountService)
        }
    }

    // externalCacheDir content is cleared on app uninstall
    fun drawableToUri(@DrawableRes resId: Int): android.net.Uri? {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val drawable = ResourcesCompat.getDrawable(context.resources, resId, null)
        val bitmap = drawableToBitmap(drawable!!)
        val file = File(context.externalCacheDir?.path, imageFileName)
        return try {
            val outStream = file.outputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outStream)
            outStream.flush()
            outStream.close()
            android.net.Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

}