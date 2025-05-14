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
package cx.ring.client.message

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isFocusable
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import cx.ring.AccountUtils
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.client.ColorChooserBottomSheet
import cx.ring.client.EmojiChooserBottomSheet
import cx.ring.client.HomeActivity
import cx.ring.client.addcontact.AccountNavigationUtils
import cx.ring.hasBackgroundColor
import cx.ring.viewholders.SmartListViewHolder
import cx.ring.waitUntil
import net.jami.model.Account
import net.jami.model.Conversation
import net.jami.model.Uri
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
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
        private val TAG = Messaging::class.java.simpleName

        const val TEST_MESSAGE_1 = "my test message"
        const val TEST_MESSAGE_2 = "my reply message"
        const val TEST_MESSAGE_3 = "my modified message"
        const val TEST_MESSAGE_4 = "my colored message self"

        @JvmStatic
        private lateinit var accountA: Account

        @JvmStatic
        private lateinit var accountB: Account

        @JvmStatic
        private lateinit var conversation: Conversation

        @JvmStatic
        private var accountsCreated = false
    }

    @JvmField
    @Rule
    val mActivityScenarioRule = ActivityScenarioRule(HomeActivity::class.java)

    @Before
    fun setup() {
        if (accountsCreated) return

        val accountService = JamiApplication.instance!!.mAccountService
        val conversationFacade = JamiApplication.instance!!.mConversationFacade

        val accountList = AccountUtils.createAccountAndRegister(2)

        // Need delay to give time to accounts to register on DHT before sending trust request.
        // Inferior delay will occasionally cause the trust request to fail.
        Thread.sleep(10000)

        accountA = accountList[0]
        accountB = accountList[1]

        // AccountA sends trust request to accountB.
        val uri = Uri.fromString(accountB.uri!!)
        conversation = Conversation(accountA.accountId, uri, Conversation.Mode.Request)
        accountService.sendTrustRequest(conversation, uri)

        // AccountB accepts trust request from accountA.
        val invitation = accountB.getPendingSubject().skip(1).blockingFirst().first()
        conversationFacade.acceptRequest(invitation)

        accountsCreated = true

        // Restart the activity to make load accounts.
        mActivityScenarioRule.scenario.close()
        ActivityScenario.launch(HomeActivity::class.java)
    }

    @Test
    fun a01_sendText_displayedInUserSide() {
        // Current account is accountB. Open conversation with accountA.
        onView(withId(R.id.confs_list)).perform(
            waitUntil(hasDescendant(allOf(withText(accountA.displayUri!!), isDisplayed()))),
            RecyclerViewActions.actionOnItem<SmartListViewHolder>(
                hasDescendant(withText(accountA.displayUri!!)), click()
            )
        )

        // Type text message.
        onView(withId(R.id.msg_input_txt))
            .perform(waitUntil(isDisplayed()), typeText(TEST_MESSAGE_1), closeSoftKeyboard())

        // Click send button.
        onView(withId(R.id.msg_send)).check(matches(isDisplayed())).perform(click())

        // Check if the message is displayed.
        onView(withId(R.id.bubble_message_text))
            .perform(waitUntil(allOf(withText(TEST_MESSAGE_1), isDisplayed())))
    }

    @Test
    fun a02_sendText_displayedInPeerSide() {
        Thread.sleep(8000)
        AccountNavigationUtils.moveToAccount(accountA.displayUri!!)

        // Go to conversation.
        onView(withId(R.id.confs_list)).perform(
            waitUntil(hasDescendant(allOf(withText(accountB.displayUri!!), isDisplayed()))),
            RecyclerViewActions.actionOnItem<SmartListViewHolder>(
                hasDescendant(withText(accountB.registeredName)), click()
            )
        )

        // Check if message was received.
        onView(withId(R.id.bubble_message_text))
            .perform(waitUntil(allOf(withText(TEST_MESSAGE_1), isDisplayed())))
    }

    @Test
    fun a03_replySelfText_displayedInUserSide() {
        // Move to accountB.
        AccountNavigationUtils.moveToAccount(accountB.displayUri!!)

        // Open conversation with accountA.
        onView(withId(R.id.confs_list)).perform(
            waitUntil(hasDescendant(allOf(withText(accountA.displayUri!!), isDisplayed()))),
            RecyclerViewActions.actionOnItem<SmartListViewHolder>(
                hasDescendant(withText(accountA.displayUri!!)), click()
            )
        )

        // Long press on the message.
        onView(allOf(withId(R.id.bubble_message_text), withText(TEST_MESSAGE_1)))
            .perform(longClick())

        // Click on reply button.
        // Trick to click on the reply button which is a popup window.
        onView(withId(R.id.conv_action_reply)).inRoot(not(isFocusable())).perform(click())

        // Type text message.
        onView(withId(R.id.msg_input_txt))
            .perform(waitUntil(isDisplayed()), typeText(TEST_MESSAGE_2), closeSoftKeyboard())

        // Click send button.
        onView(withId(R.id.msg_send)).check(matches(isDisplayed())).perform(click())

        // Check if the message is displayed (with reply).
        onView(allOf(withText(TEST_MESSAGE_2), withId(R.id.bubble_message_text)))
            .perform(waitUntil(isDisplayed()))
        onView(allOf(withId(R.id.reply_text), isDisplayed()))
            .check(matches(withText(TEST_MESSAGE_1)))
    }

    @Test
    fun a04_replySelfText_displayedInPeerSide() {
        Thread.sleep(8000)
        // Move to accountA.
        AccountNavigationUtils.moveToAccount(accountA.displayUri!!)

        // Open conversation with accountB.
        onView(withId(R.id.confs_list)).perform(
            waitUntil(hasDescendant(allOf(withText(accountB.displayUri!!), isDisplayed()))),
            RecyclerViewActions.actionOnItem<SmartListViewHolder>(
                hasDescendant(withText(accountB.displayUri!!)), click()
            )
        )

        // Check if the replied message is displayed.
        // Check if the message is displayed (with reply).
        onView(allOf(withText(TEST_MESSAGE_2), withId(R.id.bubble_message_text)))
            .perform(waitUntil(isDisplayed()))
        onView(allOf(withId(R.id.reply_text), isDisplayed()))
            .check(matches(withText(TEST_MESSAGE_1)))
    }

    @Test
    fun a05_modifyText_modifiedInUserSide() {
        // Move to accountB.
        AccountNavigationUtils.moveToAccount(accountB.displayUri!!)

        // Open conversation with accountA.
        onView(withId(R.id.confs_list)).perform(
            waitUntil(hasDescendant(allOf(withText(accountA.displayUri!!), isDisplayed()))),
            RecyclerViewActions.actionOnItem<SmartListViewHolder>(
                hasDescendant(withText(accountA.displayUri!!)), click()
            )
        )

        // Long press on the message.
        onView(allOf(withText(TEST_MESSAGE_1), withId(R.id.bubble_message_text)))
            .perform(longClick())

        // Click on the edit button.
        // Trick to click on the reply button which is a popup window.
        onView(withId(R.id.conv_action_edit)).inRoot(not(isFocusable())).perform(click())

        // Modify text.
        onView(withId(R.id.msg_input_txt))
            .perform(
                waitUntil(isDisplayed()),
                clearText(),
                typeText(TEST_MESSAGE_3),
                closeSoftKeyboard()
            )

        // Click send button.
        onView(withId(R.id.msg_send)).check(matches(isDisplayed())).perform(click())

        // Check if the message is displayed.
        onView(allOf(withText(TEST_MESSAGE_3), withId(R.id.bubble_message_text)))
            .perform(waitUntil(isDisplayed()))
        onView(allOf(withText(TEST_MESSAGE_1), withId(R.id.bubble_message_text)))
            .check(doesNotExist())
    }

    @Test
    fun a06_modifyText_modifiedInPeerSide() {
        // Move to accountA.
        AccountNavigationUtils.moveToAccount(accountA.displayUri!!)

        // Open conversation with accountB.
        onView(withId(R.id.confs_list)).perform(
            waitUntil(hasDescendant(allOf(withText(accountB.displayUri!!), isDisplayed()))),
            RecyclerViewActions.actionOnItem<SmartListViewHolder>(
                hasDescendant(withText(accountB.displayUri!!)), click()
            )
        )

        // Check if the modified message is displayed.
        onView(allOf(withText(TEST_MESSAGE_3), withId(R.id.bubble_message_text)))
            .perform(waitUntil(isDisplayed()))
        onView(allOf(withText(TEST_MESSAGE_1), withId(R.id.bubble_message_text)))
            .check(doesNotExist())
    }

    @Test
    fun a07_modifyText_cannotModifyPeerText() {
        // Open conversation with accountB.
        onView(withId(R.id.confs_list)).perform(
            waitUntil(hasDescendant(allOf(withText(accountB.displayUri!!), isDisplayed()))),
            RecyclerViewActions.actionOnItem<SmartListViewHolder>(
                hasDescendant(withText(accountB.displayUri!!)), click()
            )
        )

        // Long press on the message.
        onView(allOf(withText(TEST_MESSAGE_3), withId(R.id.bubble_message_text)))
            .perform(longClick())

        // Verify that the edit button is not displayed
        onView(withId(R.id.conv_action_edit)).inRoot(not(isFocusable()))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun a08_deleteText_deletedInUserSide() {
        // Move to accountB.
        AccountNavigationUtils.moveToAccount(accountB.displayUri!!)

        // Open conversation with accountA.
        onView(withId(R.id.confs_list)).perform(
            waitUntil(hasDescendant(allOf(withText(accountA.displayUri!!), isDisplayed()))),
            RecyclerViewActions.actionOnItem<SmartListViewHolder>(
                hasDescendant(withText(accountA.displayUri!!)), click()
            )
        )

        // Long press on the message
        onView(allOf(withText(TEST_MESSAGE_3), withId(R.id.bubble_message_text)))
            .perform(longClick())

        // Click on delete button.
        // Trick to click on the delete button which is a popup window.
        onView(withId(R.id.conv_action_delete)).inRoot(not(isFocusable())).perform(click())

        // Check if the message is properly displayed as deleted.
        val deletedString = getInstrumentation().targetContext.let {
            it.getString(R.string.conversation_message_deleted)
                .format(it.getString(R.string.conversation_info_contact_you))
        }
        onView(allOf(withText(deletedString), withId(R.id.bubble_message_text)))
            .perform(waitUntil(isDisplayed()))
        onView(allOf(withText(TEST_MESSAGE_3), withId(R.id.bubble_message_text)))
            .check(doesNotExist())
    }

    @Test
    fun a09_deleteText_deletedInPeerSide() {
        // Move to accountA.
        AccountNavigationUtils.moveToAccount(accountA.displayUri!!)

        // Open conversation with accountB.
        onView(withId(R.id.confs_list)).perform(
            waitUntil(hasDescendant(allOf(withText(accountB.displayUri!!), isDisplayed()))),
            RecyclerViewActions.actionOnItem<SmartListViewHolder>(
                hasDescendant(withText(accountB.displayUri!!)), click()
            )
        )

        // Check if the message is properly displayed as deleted.
        val deletedString = getInstrumentation().targetContext
            .getString(R.string.conversation_message_deleted).format(accountB.registeredName)
        onView(allOf(withText(deletedString), withId(R.id.bubble_message_text)))
            .perform(waitUntil(isDisplayed()))
        onView(allOf(withText(TEST_MESSAGE_3), withId(R.id.bubble_message_text)))
            .check(doesNotExist())
    }

    @Test
    fun a10_deleteText_cannotDeletePeerText() {
        // Open conversation with accountB.
        onView(withId(R.id.confs_list)).perform(
            waitUntil(hasDescendant(allOf(withText(accountB.displayUri!!), isDisplayed()))),
            RecyclerViewActions.actionOnItem<SmartListViewHolder>(
                hasDescendant(withText(accountB.displayUri!!)), click()
            )
        )

        // Click on the message.
        onView(allOf(withText(TEST_MESSAGE_2), withId(R.id.bubble_message_text)))
            .perform(longClick())

        // Verify that the delete button is not displayed.
        onView(withId(R.id.conv_action_delete)).inRoot(not(isFocusable()))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun a11_changeConversationColor_changedInUserSide() {
        // Move to accountB.
        AccountNavigationUtils.moveToAccount(accountB.displayUri!!)

        // Open conversation with accountA.
        onView(withId(R.id.confs_list)).perform(
            waitUntil(hasDescendant(allOf(withText(accountA.displayUri!!), isDisplayed()))),
            RecyclerViewActions.actionOnItem<SmartListViewHolder>(
                hasDescendant(withText(accountA.displayUri!!)), click()
            )
        )

        // Add a message.
        JamiApplication.instance!!.mAccountService.sendConversationMessage(
            accountId = accountB.accountId,
            conversationUri = accountB.getConversations().first().uri,
            txt = TEST_MESSAGE_4,
            replyTo = null
        )
        // Todo: Find a better way to wait for the notification to be removed. GitLab: #1787.
        Thread.sleep(8000) // Wait for the message notification to be removed.

        // Click on conversation settings button.
        openActionBarOverflowOrOptionsMenu(getInstrumentation().targetContext)
        onView(withText(R.string.conversation_details)).perform(click())

        // Click on change color button. Select a color.
        onView(withText(R.string.conversation_preference_color)).perform(click())
        // Position 12 = R.color.conversation_palette_red
        onView(withId(R.id.color_chooser)).perform(
            RecyclerViewActions
                .actionOnItemAtPosition<ColorChooserBottomSheet.ColorView>(12, click())
        )

        // Go back to conversation.
        pressBack()

        // Check if the conversation color is changed.
        onView(allOf(withId(R.id.message_content), hasDescendant(withText(TEST_MESSAGE_4))))
            .check(matches(hasBackgroundColor(R.color.conversation_palette_red)))
    }

    @Test
    fun a13_changeConversationSymbol_changedInUserSide() {
        // Move to accountB.
        AccountNavigationUtils.moveToAccount(accountB.displayUri!!)

        // Open conversation with accountA.
        onView(withId(R.id.confs_list)).perform(
            waitUntil(hasDescendant(allOf(withText(accountA.displayUri!!), isDisplayed()))),
            RecyclerViewActions.actionOnItem<SmartListViewHolder>(
                hasDescendant(withText(accountA.displayUri!!)), click()
            )
        )

        // Click on conversation settings button.
        openActionBarOverflowOrOptionsMenu(getInstrumentation().targetContext)

        // Click on change symbol button. Select a symbol.
        onView(withText(R.string.conversation_details)).perform(click())
        onView(withText(R.string.conversation_preference_emoji)).perform(click())
        // Position 4 = R.array.conversation_emojis[4] = R.string.default_emoji_5 = 👻
        onView(withId(R.id.emoji_chooser)).perform(
            RecyclerViewActions
                .actionOnItemAtPosition<EmojiChooserBottomSheet.EmojiView>(4, click())
        )

        // Go back to conversation.
        pressBack()

        // Check if the conversation symbol is changed.
        onView(withId(R.id.emoji_send)).check(matches(withText(R.string.default_emoji_5)))
    }

    @Test
    fun a15_sendSymbol_displayedInUserSide() {
        // Move to accountB.
        AccountNavigationUtils.moveToAccount(accountB.displayUri!!)

        // Open conversation with accountA.
        onView(withId(R.id.confs_list)).perform(
            waitUntil(hasDescendant(allOf(withText(accountA.displayUri!!), isDisplayed()))),
            RecyclerViewActions.actionOnItem<SmartListViewHolder>(
                hasDescendant(withText(accountA.displayUri!!)), click()
            )
        )

        // Click on the symbol button.
        onView(withId(R.id.emoji_send)).perform(click())

        // Check if the symbol is displayed.
        onView(isRoot()).perform(
            waitUntil(hasDescendant(allOf(withText(R.string.default_emoji_5), isDisplayed())))
        )
    }

    @Test
    fun a16_sendSymbol_displayedInPeerSide() {
        Thread.sleep(8000)
        // Move to accountA.
        AccountNavigationUtils.moveToAccount(accountA.displayUri!!)

        // Open conversation with accountB.
        onView(withId(R.id.confs_list)).perform(
            waitUntil(hasDescendant(allOf(withText(accountB.displayUri!!), isDisplayed()))),
            RecyclerViewActions.actionOnItem<SmartListViewHolder>(
                hasDescendant(withText(accountB.displayUri!!)), click()
            )
        )

        // Check if the symbol is displayed.
        onView(isRoot()).perform(
            waitUntil(hasDescendant(allOf(withText(R.string.default_emoji_5), isDisplayed())))
        )
    }

    @Test
    fun z_clear() = AccountUtils.removeAllAccounts()  // clear created accounts
}
