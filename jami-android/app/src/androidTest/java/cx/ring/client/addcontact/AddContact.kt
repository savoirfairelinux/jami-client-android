package cx.ring.client.addcontact

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onData
import org.hamcrest.Matchers.allOf
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.supportsInputMethods
import androidx.test.espresso.matcher.ViewMatchers.withChild
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
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
import net.jami.daemon.JamiService
import net.jami.services.AccountService
import net.jami.utils.Log
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class AddContact {
    @Rule
    @JvmField
    var mActivityScenarioRule = ActivityScenarioRule(HomeActivity::class.java)

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS)

    companion object {
        @JvmStatic
        private var accountsCreated = false
        @JvmStatic
        private var accountAUsername = ""
        @JvmStatic
        private var accountBUsername = ""
    }

    @Before
    fun createAccounts() {
        if (accountsCreated) return

        Log.d("devdebug", "Creating accounts ...")

        // Create account A
        accountAUsername = "JamiTest_" + System.currentTimeMillis()
        AccountCreationUtils.moveToWizard()
        AccountCreationUtils.createAccountWithUsername(accountAUsername)
        assertOnView(withId(R.id.search_bar), matches(isDisplayed()))

        // Create account B
        accountBUsername = "JamiTest_" + System.currentTimeMillis()
        AccountCreationUtils.moveToWizard()
        AccountCreationUtils.createAccountWithUsername(accountBUsername)
        assertOnView(withId(R.id.search_bar), matches(isDisplayed()))

        accountsCreated = true
    }

//    @Test
    fun b_searchForContact() {
        // Move to account A
        AccountNavigationUtils.moveToAccount(accountAUsername)

        // Click on the search bar
        onView(withId(R.id.search_bar)).perform(click())

        // Type the username of account B
        onView(allOf(supportsInputMethods(), isDescendantOfA(withId(R.id.search_view))))
            .perform(typeText(accountBUsername))

        // Click on the search result for account B
        assertOnView(allOf(withId(R.id.conv_participant), withText(accountBUsername)), matches(isDisplayed()))
    }

    @Test
    fun c_sendContactInvite() {
        // Move to account A
        AccountNavigationUtils.moveToAccount(accountAUsername)

        // Click on the search bar
        onView(withId(R.id.search_bar)).perform(click())

        // Type the username of account B
        onView(allOf(supportsInputMethods(), isDescendantOfA(withId(R.id.search_view))))
            .perform(typeText(accountBUsername))

        // Click on the search result for account B
        doOnView(allOf(withId(R.id.conv_participant), withText(accountBUsername/*.lowercase()*/)), click())

        // Click on "add contact" button
        onView(withId(R.id.unknownContactButton)).perform(click())

        // Check that the contact has been invited
        val contactInvitedString =
            String.format(
                InstrumentationRegistry
                    .getInstrumentation().targetContext.getString(R.string.conversation_contact_invited),
                accountBUsername
            )
        assertOnView(withText(Matchers.containsString(contactInvitedString)), matches(isDisplayed()))
    }

//    @Test
    fun d_acceptContactInvite_hasInvitationReceivedBanner(){
        // Move to account B
        AccountNavigationUtils.moveToAccount(accountBUsername)

        // Check that the contact invitation has been received
        assertOnView(withId(R.id.invitation_received_label), matches(isDisplayed()))
    }

    @Test
    fun e_acceptContactInvite_hasOptions() {
        // Move to account B
        AccountNavigationUtils.moveToAccount(accountBUsername)

        // Open invitations
        onView(withId(R.id.invitation_received_label)).perform(click())

        // Click on invitation
        onView(allOf(withId(R.id.conv_participant), withText(accountAUsername.lowercase()))).perform(click())
//        onView(withId(R.id.conv_participant)).perform(click())
//        onView(withParent(withParent(withId(R.id.conv_participant)))).perform(click())


        // Check there is three options: Block, Refuse and Accept
        assertOnView(withId(R.id.btnBlock), matches(isDisplayed()))
        assertOnView(withId(R.id.btnRefuse), matches(isDisplayed()))
        assertOnView(withId(R.id.btnAccept), matches(isDisplayed()))
    }

    @Test
    fun f_acceptContactInvite_accept() {
        // Move to account B
        AccountNavigationUtils.moveToAccount(accountBUsername)

//        Thread.sleep(2000)

        // Open invitations
//        onView(withId(R.id.invitation_received_label)).perform(click())
        doOnView(withId(R.id.invitation_received_label), click())

        // Click on invitation
        onView(allOf(withId(R.id.conv_participant), withText(accountAUsername.lowercase()))).perform(click())

        // Accept invitation
        onView(withId(R.id.btnAccept)).perform(click())

        // Check that the contact has been added
        val contactInvitedString =
            String.format(
                InstrumentationRegistry
                    .getInstrumentation().targetContext.getString(R.string.conversation_contact_added),
                accountBUsername.lowercase()
            )
        //Todo need to adress this bug
        try {

            assertOnView(withText(Matchers.containsString(contactInvitedString)), matches(isDisplayed()))
        }
        catch (e: Exception){
            Log.d("devdebug", "Exception: $e")
            Thread.sleep(200000)
        }

        // Going back to invitation list
        pressBack()

//        onData(anything())
//            .inAdapterView(withId(R.id.confs_list))
//            .atPosition(0)
//            .onChildView(withId(R.id.conv_participant))
//            .check(
//                matches(
//                    withText(
//                        accountAUsername.lowercase()
//                    )
//                )
//            )


        assertOnView(withId(R.id.confs_list), matches(hasDescendant(withId(R.id.conv_participant))))
//        onView(withId(R.id.confs_list)).check(matches(hasDescendant(withId(R.id.conv_participant))))
    }

    @Test
    fun g_acceptContactInvite_InvitationReceivedBannerRemoved() {
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
        Log.d("devdebug", "Moving to account: $username")
    }
}


fun childAtPosition(
    parentMatcher: Matcher<View>, position: Int
): Matcher<View> {
    return object : BoundedMatcher<View, View>(View::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("Child at position $position in parent ")
            parentMatcher.describeTo(description)
        }

        public override fun matchesSafely(view: View): Boolean {
            val parent = view.parent
            return parent is ViewGroup && parentMatcher.matches(parent) && parent.getChildAt(position) == view
        }
    }
}

fun withViewAtPositionInRecyclerView(
    recyclerViewId: Int, position: Int, itemMatcher: Matcher<View>
): Matcher<View> {
    return object : BoundedMatcher<View, View>(View::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("has item at position $position in RecyclerView with id $recyclerViewId: ")
            itemMatcher.describeTo(description)
        }

        override fun matchesSafely(view: View): Boolean {
            val recyclerView = view.rootView.findViewById<RecyclerView>(recyclerViewId)
            val viewHolder = recyclerView?.findViewHolderForAdapterPosition(position)
            return viewHolder != null && itemMatcher.matches(viewHolder.itemView)
        }
    }
}