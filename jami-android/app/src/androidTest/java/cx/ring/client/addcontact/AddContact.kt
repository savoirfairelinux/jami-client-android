package cx.ring.client.addcontact

import androidx.test.core.app.ActivityScenario
import org.hamcrest.Matchers.allOf
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.supportsInputMethods
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import cx.ring.R
import cx.ring.assertOnView
import cx.ring.client.HomeActivity
import cx.ring.client.wizard.AccountCreationUtils
import cx.ring.doOnView
import net.jami.utils.Log
import org.hamcrest.Matchers
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * This test class tests the addition of a contact.
 * Precondition: Should have access to the local nameserver (192.168.50.4).
 */
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class AddContact {

    @Rule
    @JvmField
    var mActivityScenarioRule = ActivityScenarioRule(HomeActivity::class.java)

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS)

    companion object {
        @JvmStatic
        private var accountsCreated = false

        @JvmStatic
        private var accountAUsername = ""

        @JvmStatic
        private var accountBUsername = ""
        private val TAG = AddContact::class.java.simpleName
    }

    @Before
    fun createAccounts() {
        if (accountsCreated) return

        Log.d(TAG, "Creating accounts ...")

        mActivityScenarioRule.scenario.onActivity { activity ->
            val username =
                AccountCreationUtils.createAccountAndRegister(activity.mAccountService, 2)
            accountAUsername = username[0]
            accountBUsername = username[1]
            accountsCreated = true // To not redo account creation for every test.
        }

        // Restart the activity to make load accounts.
        mActivityScenarioRule.scenario.close()
        ActivityScenario.launch(HomeActivity::class.java, null)
    }

    @Test
    fun a_searchForContact() {
        // Move to account A
        AccountNavigationUtils.moveToAccount(accountAUsername)

        // Click on the search bar
        onView(withId(R.id.search_bar)).perform(click())

        // Type the username of account B
        onView(allOf(supportsInputMethods(), isDescendantOfA(withId(R.id.search_view))))
            .perform(typeText(accountBUsername))

        // Click on the search result for account B
        assertOnView(
            allOf(withId(R.id.conv_participant), withText(accountBUsername)),
            matches(isDisplayed())
        )
    }

    @Test
    fun b_sendContactInvite() {
        // Move to account A
        AccountNavigationUtils.moveToAccount(accountAUsername)

        // Click on the search bar
        onView(withId(R.id.search_bar)).perform(click())

        // Type the username of account B
        onView(allOf(supportsInputMethods(), isDescendantOfA(withId(R.id.search_view))))
            .perform(typeText(accountBUsername))

        // Click on the search result for account B
        doOnView(
            allOf(withId(R.id.conv_participant), withText(accountBUsername/*.lowercase()*/)),
            click()
        )

        // Click on "add contact" button
        onView(withId(R.id.unknownContactButton)).perform(click())

        // Check that the contact has been invited
        val contactInvitedString =
            String.format(
                InstrumentationRegistry.getInstrumentation().targetContext
                    .getString(R.string.conversation_contact_invited),
                accountBUsername
            )
        assertOnView(
            withText(Matchers.containsString(contactInvitedString)),
            matches(isDisplayed())
        )
    }

    @Test
    fun c_acceptContactInvite_hasInvitationReceivedBanner() {
        // Move to account B
        AccountNavigationUtils.moveToAccount(accountBUsername)

        // Check that the contact invitation has been received
        assertOnView(withId(R.id.invitation_received_label), matches(isDisplayed()))
    }

    @Test
    fun d_acceptContactInvite_hasOptions() {
        // Move to account B
        AccountNavigationUtils.moveToAccount(accountBUsername)

        // Open invitations
        doOnView(allOf(withId(R.id.invitation_received_label), isDisplayed()), click())

        // Click on invitation
        onView(allOf(withId(R.id.conv_participant), withText(accountAUsername.lowercase())))
            .perform(click())

        // Check there is three options: Block, Refuse and Accept
        assertOnView(withId(R.id.btnBlock), matches(isDisplayed()))
        assertOnView(withId(R.id.btnRefuse), matches(isDisplayed()))
        assertOnView(withId(R.id.btnAccept), matches(isDisplayed()))
    }

    @Test
    fun e_acceptContactInvite_accept() {
        // Move to account B
        AccountNavigationUtils.moveToAccount(accountBUsername)

        // Open invitations
        doOnView(withId(R.id.invitation_received_label), click())

        // Click on invitation
        onView(
            allOf(
                withId(R.id.conv_participant),
                withText(accountAUsername.lowercase())
            )
        ).perform(click())

        // Accept invitation
        onView(withId(R.id.btnAccept)).perform(click())

        // Check that the contact has been added
        val contactInvitedString =
            String.format(
                InstrumentationRegistry.getInstrumentation().targetContext
                    .getString(R.string.conversation_contact_added),
                accountBUsername.lowercase()
            )

        assertOnView(
            withText(Matchers.containsString(contactInvitedString)),
            matches(isDisplayed())
        )

        // Going back to invitation list
        pressBack()

        assertOnView(withId(R.id.confs_list), matches(hasDescendant(withId(R.id.conv_participant))))
    }

    @Test
    fun f_acceptContactInvite_InvitationReceivedBannerRemoved() {
        // Move to account B
        AccountNavigationUtils.moveToAccount(accountBUsername)

        // Check that the contact invitation has been received
        assertOnView(withId(R.id.invitation_received_label), matches(not(isDisplayed())))
    }
}

object AccountNavigationUtils {
    fun moveToAccount(username: String) {
        val searchBarContentNavigationDescription = InstrumentationRegistry
            .getInstrumentation().targetContext.getString(R.string.searchbar_navigation_account)
        onView(ViewMatchers.withContentDescription(searchBarContentNavigationDescription))
            .perform(click())
        onView(withText(username.lowercase())).perform(click())
    }
}