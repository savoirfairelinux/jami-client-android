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
package cx.ring.client.swarmgroup

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import cx.ring.AccountUtils
import cx.ring.client.HomeActivity
import cx.ring.client.addcontact.AccountNavigationUtils
import net.jami.model.Account
import net.jami.model.Conversation
import net.jami.model.Uri
import net.jami.utils.Log
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class SwarmGroup {

    companion object {
        @JvmStatic
        private lateinit var accountA: Account

        @JvmStatic
        private lateinit var accountB: Account

        @JvmStatic
        private lateinit var accountC: Account

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
            val accountList = AccountUtils.createAccountAndRegister(activity.mAccountService, 3)

            // Need delay to give time to accounts to register on DHT before sending trust request.
            // Inferior delay will occasionally cause the trust request to fail.
            Thread.sleep(10000)

            accountA = accountList[0]
            accountB = accountList[1]
            accountC = accountList[2]

            // AccountB sends trust request to accountA.
            val accountAUri = Uri.fromString(accountA.uri!!)
            val conversationWithB = Conversation(accountB.accountId, accountAUri, Conversation.Mode.Request)
            activity.mAccountService.sendTrustRequest(conversationWithB, accountAUri)

            // AccountC sends trust request to accountA.
            val conversationWithC = Conversation(accountC.accountId, accountAUri, Conversation.Mode.Request)
            activity.mAccountService.sendTrustRequest(conversationWithC, accountAUri)



            // AccountA accepts trust request from accountB.
            val invitationFromB = accountA.getPendingSubject().subscribe{
                Log.w("devdebug", "invitation: ${it.size}")
                it.forEach { invitation ->
                    Log.w("devdebug", "invitation: ${invitation}}")
                }
            }
//            activity.mConversationFacade.acceptRequest(invitationFromB)
//
//            // AccountB accepts trust request from accountB.
//            val invitationFromC = accountB.getPendingSubject().skip(1).blockingFirst().first()
//            activity.mConversationFacade.acceptRequest(invitationFromC)

            accountsCreated = true
        }

        // Restart the activity to make load accounts.
        mActivityScenarioRule.scenario.close()
        ActivityScenario.launch(HomeActivity::class.java)
    }

    @Test
    fun a01_() {
        AccountNavigationUtils.moveToAccount(accountA.displayUri!!)
        Thread.sleep(500000)
    }

    //

    @Test
    fun z_clear() {
        // clear created accounts
        mActivityScenarioRule.scenario.onActivity { activity ->
            AccountUtils.removeAllAccounts(accountService = activity.mAccountService)
        }
    }
}
