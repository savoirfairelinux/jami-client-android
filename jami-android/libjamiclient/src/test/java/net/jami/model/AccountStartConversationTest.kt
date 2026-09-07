/*
 * Copyright (C) 2004-2025 Savoir-faire Linux Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.jami.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

/**
 * Model-level regression test for AccountService.startConversation(): since conversationReady
 * is dispatched asynchronously, startConversation() may run before the swarm is registered and
 * concurrently with the conversationReady handler.
 */
class AccountStartConversationTest {
    private val accountId = "3a1118452cfb532e"
    private val self = "60189e4429fdae93ebc35df42802afa2629aa9f7"
    private val members = listOf(
        "529e3a6708878bf57c1aa99e563f2a26f63a41e4",
        "2e431b1d49cdaf803db03fded7ad686cd23c35d9",
        "eb19dc6560839480fb1288fe347df956e29104fe",
    )

    private fun newAccount() = Account(
        accountId,
        mapOf(ConfigKey.ACCOUNT_USERNAME.key to "ring:$self", ConfigKey.ACCOUNT_TYPE.key to AccountConfig.ACCOUNT_TYPE_JAMI),
        emptyList(),
        mapOf(ConfigKey.ACCOUNT_REGISTRATION_STATUS.key to AccountConfig.RegistrationState.REGISTERED.name),
    )

    /** What startConversation() does once the daemon returned the new conversation id. */
    private fun startConversation(account: Account, id: String): Conversation {
        val conversation = account.getSwarm(id) ?: account.newSwarm(id, Conversation.Mode.InvitesOnly)
        synchronized(conversation) {
            for (member in members)
                conversation.addContact(account.getContactFromCache(member), MemberRole.INVITED)
        }
        account.conversationStarted(conversation)
        return conversation
    }

    /** What conversationReadyNow() does with the members reported by the daemon (only self yet). */
    private fun conversationReady(account: Account, id: String): Conversation {
        val uri = Uri(Uri.SWARM_SCHEME, id)
        val conversation = account.getByUri(uri) ?: account.newSwarm(id, Conversation.Mode.InvitesOnly)
        synchronized(conversation) {
            val member = Uri.fromId(self)
            if (conversation.findContact(member) == null)
                conversation.addContact(account.getContactFromCache(member), MemberRole.ADMIN)
        }
        account.conversationStarted(conversation)
        return conversation
    }

    @Test
    fun startConversationBeforeConversationReadyRegistersTheSwarm() {
        val account = newAccount()
        val id = "02b2055f30e13bc7858ca7ef29261fcbf354f2e0"
        assertNull(account.getSwarm(id))

        val started = startConversation(account, id)
        val ready = conversationReady(account, id)

        assertSame(started, ready)
        assertSame(started, account.getSwarm(id))
        assertMembers(started)
    }

    @Test
    fun conversationReadyBeforeStartConversationReusesTheSwarm() {
        val account = newAccount()
        val id = "2d58a62742389538a18c7d0e3f6d7dfbe33f91ba"

        val ready = conversationReady(account, id)
        val started = startConversation(account, id)

        assertSame(ready, started)
        assertMembers(started)
    }

    @Test
    fun concurrentStartAndReadyKeepMembersConsistent() {
        repeat(500) { iteration ->
            val account = newAccount()
            val id = "%040x".format(iteration)
            val go = CountDownLatch(1)
            val results = arrayOfNulls<Conversation>(2)
            val errors = arrayOfNulls<Throwable>(2)
            fun run(index: Int, action: () -> Conversation) = thread {
                go.await()
                try { results[index] = action() } catch (e: Throwable) { errors[index] = e }
            }
            val threads = listOf(
                run(0) { startConversation(account, id) },
                run(1) { conversationReady(account, id) },
            )
            go.countDown()
            threads.forEach { it.join(5_000); assertFalse("thread still running", it.isAlive) }

            errors.filterNotNull().firstOrNull()?.let { throw AssertionError("iteration $iteration", it) }
            assertSame("iteration $iteration", results[0], results[1])
            assertSame("iteration $iteration", results[0], account.getSwarm(id))
            assertMembers(results[0]!!)
        }
    }

    private fun assertMembers(conversation: Conversation) {
        val expected = (members + self).map { Uri.fromId(it).uri }.toSet()
        val actual = conversation.contacts.map { it.uri.uri }
        assertEquals("duplicated member", actual.size, actual.toSet().size)
        assertEquals(expected, actual.toSet())
        members.forEach { assertEquals(MemberRole.INVITED, conversation.roles[Uri.fromId(it).uri]) }
        assertEquals(MemberRole.ADMIN, conversation.roles[Uri.fromId(self).uri])
    }
}
