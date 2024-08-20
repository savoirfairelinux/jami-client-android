package cx.ring.client.message

import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressBack
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
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
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class Messaging {

    companion object {
        const val TEST_MESSAGE = "my test message"

        @JvmStatic
        private lateinit var accountA: Account

        @JvmStatic
        private lateinit var accountB: Account

        @JvmStatic
        private var accountsCreated = false
    }

    @JvmField
    @Rule
    val mActivityScenarioRule = ActivityScenarioRule(HomeActivity::class.java)

    @Before
    fun setup() {

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

            // AccountA accepts trust request from accountB
            val invitation = accountB.getPendingSubject().skip(1).blockingFirst().first()
            activity.mConversationFacade.acceptRequest(invitation)

            accountsCreated = true
        }

        // Restart the activity to make load accounts
        mActivityScenarioRule.scenario.close()
        ActivityScenario.launch(HomeActivity::class.java)
    }

    @Test
    fun t1_sendText() {
        // Current account is accountB. Go to conversation with accountA
        onView(withId(R.id.confs_list)).perform(
            waitUntil(hasDescendant(allOf(withText(accountA.displayUri!!), isDisplayed()))),
            RecyclerViewActions.actionOnItem<SmartListViewHolder>(
                hasDescendant(withText(accountA.displayUri!!)), click()
            )
        )

        // type text msg
        onView(withId(R.id.msg_input_txt)).perform(waitUntil(isDisplayed()), typeText(TEST_MESSAGE))

        // click send button
        onView(withId(R.id.msg_send)).check(matches(isDisplayed())).perform(click())

        // check if the message is displayed in the current account
        onView(allOf(withText(TEST_MESSAGE), isDescendantOfA(withId(R.id.hist_list))))
            .perform(waitUntil(isDisplayed()))

        onView(isRoot()).perform(closeSoftKeyboard(), pressBack())

        //Test Receive =====================================
        // cancel all push notifications to not let them hide the underlying views
        NotificationManagerCompat.from(InstrumentationRegistry.getInstrumentation().targetContext)
            .cancelAll()

        // be sure that all push notifications were dismissed
        Thread.sleep(3000)
    }

    @Test
    fun t2_receiveText() {

        AccountNavigationUtils.moveToAccount(accountA.displayUri!!)

        // go to conversation
        onView(withId(R.id.confs_list)).perform(
            waitUntil(hasDescendant(allOf(withText(accountB.displayUri!!), isDisplayed()))),
            RecyclerViewActions.actionOnItem<SmartListViewHolder>(
                hasDescendant(withText(accountB.registeredName)), click()
            )
        )

        // check if message was received
        onView(allOf(withText(TEST_MESSAGE), isDescendantOfA(withId(R.id.hist_list))))
            .perform(waitUntil(isDisplayed()))
    }

    @Test
    fun z_clear() {
        // clear created accounts
        mActivityScenarioRule.scenario.onActivity { activity ->
            AccountUtils.removeAllAccounts(accountService = activity.mAccountService)
        }
    }
}