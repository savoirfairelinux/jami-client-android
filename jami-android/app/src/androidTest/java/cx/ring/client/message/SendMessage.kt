package cx.ring.client.message

import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.test.espresso.Espresso.onView
import androidx.test.core.app.ActivityScenario
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
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cx.ring.R
import cx.ring.client.HomeActivity
import cx.ring.utils.ConversationPath
import cx.ring.viewholders.SmartListViewHolder
import cx.ring.waitForView
import io.reactivex.rxjava3.core.Single
import net.jami.model.Account
import net.jami.model.AccountConfig
import net.jami.model.ConfigKey
import net.jami.model.Conversation
import net.jami.model.Uri
import net.jami.services.AccountService
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class SendMessage {
    
    private var accountA: Account? = null
    private var accountB: Account? = null

    @JvmField
    @Rule val mActivityScenarioRule = ActivityScenarioRule(HomeActivity::class.java)

    @Before fun createAccounts() {
        mActivityScenarioRule.scenario.onActivity { activity ->
//            val accounts = activity.mAccountService.observableAccountList.blockingFirst()
            val accountList = AccountCreationUtils.createAccountAndRegister(activity.mAccountService, 2)

            // Need delay to give time to accounts to register on DHT before sending trust request.
            // Inferior delay will occasionally cause the trust request to fail.
            Thread.sleep(10000)

            accountA = accountList[0]
            accountB = accountList[1]

            // AccountB sends trust request to accountA
            val uri = Uri.fromString(accountB!!.uri!!)
            val conversation = Conversation(accountA!!.accountId, uri, Conversation.Mode.Request)
            activity.mAccountService.sendTrustRequest(conversation, uri)

            conversation.setMode(Conversation.Mode.Syncing)

            // AccountA accepts trust request from accountB
            activity.mConversationFacade.acceptRequest(accountB!!.accountId, Uri.fromString(accountA!!.uri!!))
//            conversation.mode.filter {
//                Log.d("devdebug", "it=$it")
//                it == Conversation.Mode.OneToOne
//            }.blockingFirst()
        }

        // Restart the activity to make load accounts.
        mActivityScenarioRule.scenario.close()
        ActivityScenario.launch(HomeActivity::class.java)
    }

    @Test fun test() {
        //moveToAccount(accountB!!.displayUri!!)

//        Thread.sleep(3000)
//        onView(withId(R.id.invitation_card)).perform(click())
//
//        // expand invitation list and go to conversation
//        onView(withId(R.id.pending_list)).perform(RecyclerViewActions.actionOnItem<SmartListViewHolder>(
//            hasDescendant(withText(accountA!!.registeredName)), click())
//        )
//
//        //Thread.sleep(3000)
//
//        // accept trust request on ConversationFragment
//        onView(withId(R.id.btnAccept)).check(matches(isDisplayed())).perform(click())


        moveToAccount(accountA!!.displayUri!!)

        // go to conversation with accountB
        onView(withId(R.id.confs_list)).perform(RecyclerViewActions.actionOnItem<SmartListViewHolder>(
            hasDescendant(withText(accountB!!.registeredName)), click())
        )

        // type msg
        onView(withId(R.id.msg_input_txt)).perform(typeText(TEST_MESSAGE), closeSoftKeyboard())


        // click send button
        waitForView(withId(R.id.msg_send)).check(matches(isDisplayed())).perform(click())

        onView(Matchers.allOf(withText(TEST_MESSAGE), isDescendantOfA(withId(R.id.hist_list))))
            .check(matches(isDisplayed()))

        onView(isRoot()).perform(pressBack())

        // cancel push notification to not let it hide the underlying views
        val manager = NotificationManagerCompat.from(InstrumentationRegistry.getInstrumentation().targetContext)
        manager.cancelAll()

        //Thread.sleep(3000)

        // verify peer receives the message
        moveToAccount(accountB!!.displayUri!!)

        // go to conversation
        onView(withId(R.id.confs_list)).perform(RecyclerViewActions.actionOnItem<SmartListViewHolder>(
            hasDescendant(withText(accountA!!.registeredName)), click())
        )

        // check if message was received
        waitForView(Matchers.allOf(withText(TEST_MESSAGE), isDescendantOfA(withId(R.id.hist_list))))
            .check(matches(isDisplayed()))
    }

    private fun moveToAccount(username: String) {
        val searchBarContentNavigationDescription = InstrumentationRegistry
            .getInstrumentation().targetContext.getString(R.string.searchbar_navigation_account)
        onView(withContentDescription(searchBarContentNavigationDescription)).perform(click())
        onView(withText(username.lowercase())).perform(click())
    }

    companion object {
        val TAG = SendMessage::class.java.simpleName
        private const val TEST_MESSAGE = "my test message"
    }

    object AccountCreationUtils {

        fun createAccountAndRegister(accountService: AccountService, count: Int): List<Account> {
            val baseUsername = "jamitest"
            val time = System.currentTimeMillis()
            val accountObservableList = (0..< count).map { accountCount ->
                val username = "${baseUsername}_${time}_${accountCount}"
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
            Single.zip(accountList.map {
                accountService.getObservableAccount(it)
                    .filter { account: Account ->
                        account.registrationState == AccountConfig.RegistrationState.REGISTERED
                    }.firstOrError()
            }) { it }.blockingSubscribe()

            return accountList
        }
    }
}