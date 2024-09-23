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
package cx.ring.client.addcontact

import org.hamcrest.Matchers.allOf
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
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
import cx.ring.AccountUtils
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.assertOnView
import cx.ring.client.HomeActivity
import cx.ring.doOnView
import net.jami.model.Account
import net.jami.model.Conversation
import net.jami.model.Uri
import net.jami.utils.Log
import org.hamcrest.Matchers
import org.hamcrest.Matchers.not
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * This test class tests the addition of a contact.
 * Precondition: Should have access to the nameserver (https://ns-test.jami.net/).
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

        @JvmStatic // Account A will be the one sending the trust request.
        private var accountA: Account? = null

        @JvmStatic // Account B will be the one accepting the trust request.
        private var accountB: Account? = null

        @JvmStatic // Account C will be the one refusing the trust request.
        private var accountC: Account? = null

        @JvmStatic // Account D will be the one blocking the trust request.
        private var accountD: Account? = null

        private val TAG = AddContact::class.java.simpleName
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

        val accountService = JamiApplication.instance?.mAccountService ?: return

        val accountList =
            AccountUtils.createAccountAndRegister(accountService, 4)

        accountA = accountList[0]
        accountB = accountList[1]
        accountC = accountList[2]
        accountD = accountList[3]

        // Need delay to give time to accounts to register on DHT before sending trust request.
        // Inferior delay will occasionally cause the trust request to fail.
        Thread.sleep(10000)

        Log.d(TAG, "Accounts created.")

        // AccountB will be manually added.
        // But let's skip UI for AccountC and AccountD.
        val accountCUri = Uri.fromString(accountC!!.uri!!)
        val fakeCConversation =
            Conversation(accountA!!.accountId, accountCUri, Conversation.Mode.Request)
        accountService.sendTrustRequest(
            fakeCConversation,
            accountCUri
        )

        val accountDUri = Uri.fromString(accountD!!.uri!!)
        val fakeDConversation =
            Conversation(accountA!!.accountId, accountDUri, Conversation.Mode.Request)
        accountService.sendTrustRequest(
            fakeDConversation,
            accountDUri
        )
    }

    @Test
    fun b1_searchForContact_usingRegisteredName() {
        // Move to account A
        AccountNavigationUtils.moveToAccount(accountA!!.registeredName)

        // Click on the search bar
        onView(withId(R.id.search_bar)).perform(click())

        // Type the username of account B
        onView(allOf(supportsInputMethods(), isDescendantOfA(withId(R.id.search_view))))
            .perform(typeText(accountB!!.registeredName))

        // Click on the search result for account B
        assertOnView(
            allOf(withId(R.id.conv_participant), withText(accountB!!.registeredName)),
            matches(isDisplayed())
        )
    }

    @Test
    fun b2_searchForContact_usingJamiId() {
        // Click on the search bar
        onView(withId(R.id.search_bar)).perform(click())

        // Type the username of account B
        onView(allOf(supportsInputMethods(), isDescendantOfA(withId(R.id.search_view))))
            .perform(typeText(accountB!!.username))

        // Click on the search result for account B
        assertOnView(
            allOf(withId(R.id.conv_participant), withText(accountB!!.registeredName)),
            matches(isDisplayed())
        )
    }

    @Test
    fun b3_sendContactInvite() {
        // Click on the search bar
        onView(withId(R.id.search_bar)).perform(click())

        // Type the username of account B
        onView(allOf(supportsInputMethods(), isDescendantOfA(withId(R.id.search_view))))
            .perform(typeText(accountB!!.registeredName))

        // Click on the search result for account B
        doOnView(
            allOf(withId(R.id.conv_participant), withText(accountB!!.registeredName)),
            click()
        )

        // Click on "add contact" button
        onView(withId(R.id.unknownContactButton)).perform(click())

        // Check that the contact has been invited
        val contactInvitedString =
            String.format(
                InstrumentationRegistry.getInstrumentation().targetContext
                    .getString(R.string.conversation_contact_invited),
                accountB!!.registeredName
            )
        assertOnView(
            withText(Matchers.containsString(contactInvitedString)),
            matches(isDisplayed())
        )
    }

    @Test
    fun c1_acceptContactInvite_hasInvitationReceivedBanner() {
        // Move to account B
        AccountNavigationUtils.moveToAccount(accountB!!.registeredName)

        // Check that the contact invitation has been received
        assertOnView(withId(R.id.invitation_received_label), matches(isDisplayed()))
    }

    @Test
    fun c2_acceptContactInvite_hasOptions() {
        // Open invitations
        doOnView(allOf(withId(R.id.invitation_received_label), isDisplayed()), click())

        // Click on invitation
        onView(
            allOf(
                withId(R.id.conv_participant),
                withText(accountA!!.registeredName.lowercase())
            )
        ).perform(click())

        // Check there is three options: Block, Refuse and Accept
        assertOnView(withId(R.id.btnBlock), matches(isDisplayed()))
        assertOnView(withId(R.id.btnRefuse), matches(isDisplayed()))
        assertOnView(withId(R.id.btnAccept), matches(isDisplayed()))
    }

    @Test
    fun c3_acceptContactInvite_accept() {
        // Open invitations
        doOnView(withId(R.id.invitation_received_label), click())

        // Click on invitation
        onView(
            allOf(
                withId(R.id.conv_participant),
                withText(accountA!!.registeredName.lowercase())
            )
        ).perform(click())

        // Accept invitation
        onView(withId(R.id.btnAccept)).perform(click())

        // Check that the contact has been added
        val contactInvitedString =
            String.format(
                InstrumentationRegistry.getInstrumentation().targetContext
                    .getString(R.string.conversation_contact_added),
                accountB!!.registeredName.lowercase()
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
    fun c4_acceptContactInvite_InvitationReceivedBannerRemoved() {
        // Check that the contact invitation has been removed
        assertOnView(withId(R.id.invitation_received_label), matches(not(isDisplayed())))
    }

    @Test
    fun d1_refuseContactInvite_refuse() {
        // Move to account C
        AccountNavigationUtils.moveToAccount(accountC!!.registeredName)

        // Open invitations
        doOnView(allOf(withId(R.id.invitation_received_label), isDisplayed()), click())

        // Click on invitation
        onView(
            allOf(
                withId(R.id.conv_participant),
                isDisplayed(),
                withText(accountA!!.registeredName.lowercase())
            )
        ).perform(click())

        // Refuse invitation
        onView(withId(R.id.btnRefuse)).perform(click())

        // Check conversation fragment is well closed
        onView(withId(R.id.conversation_fragment)).check(doesNotExist())

        // Check the smart list doesnt contain the contact
        assertOnView(
            withId(R.id.confs_list),
            matches(not(hasDescendant(allOf(withId(R.id.conv_participant), isDisplayed()))))
        )
    }

    @Test
    fun d2_refuseContactInvite_InvitationReceivedBannerRemoved() {
        // Check that the contact invitation has been removed
        assertOnView(withId(R.id.invitation_received_label), matches(not(isDisplayed())))
    }

    @Test
    fun e1_blockContactInvite_block() {
        // Move to account D
        AccountNavigationUtils.moveToAccount(accountD!!.registeredName)

        // Open invitations
        doOnView(withId(R.id.invitation_received_label), click())

        // Click on invitation
        onView(
            allOf(
                withId(R.id.conv_participant),
                withText(accountA!!.registeredName.lowercase())
            )
        ).perform(click())

        // Refuse invitation
        onView(withId(R.id.btnBlock)).perform(click())

        // Check conversation fragment is well closed
        onView(withId(R.id.conversation_fragment)).check(doesNotExist())

        // Check the smart list doesnt contain the contact
        assertOnView(
            withId(R.id.confs_list),
            matches(not(hasDescendant(withId(R.id.conv_participant))))
        )
    }

    @Test
    fun e2_blockContactInvite_InvitationReceivedBannerRemoved() {
        // Check that the contact invitation has been removed
        assertOnView(withId(R.id.invitation_received_label), matches(not(isDisplayed())))
    }

    @Test
    fun e3_blockContactInvite_UserIsInBlockedList() {
        // Click on search bar menu
        onView(withId(R.id.menu_overflow)).perform(click())

        // Click on account settings
        // Don't know why but doesn't work to select by ID.
        val accountSettingString = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.menu_item_account_settings)
        onView(allOf(withText(accountSettingString), isDisplayed())).perform(click())
        onView(allOf(withId(R.id.settings_account), isDisplayed())).perform(click())

        // Click on the block list
        onView(withId(R.id.system_black_list_title)).perform(click())

        // Check that the contact is in the blocked list
        onView(withText(accountA!!.registeredName.lowercase())).check(matches(isDisplayed()))
    }

    /**
     * This test MUST be the last one because it removes the accounts.
     */
    @Test
    fun z_tearDown() {
        // Doing this in a test is not ideal.
        // Ideally, it should be in an `@After` method, but the problem is that it is executed
        // after each test and not only at the end.
        // That is the same problem with Android Test Orchestrator which removes the application
        // between each test and not only at the end.
        // `@AfterClass` could be used (executed once), but it does not have access to the activity.
        AccountUtils.removeAllAccounts(accountService = JamiApplication.instance!!.mAccountService)
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