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

import net.jami.model.Uri.Companion.fromString
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.*

/**
 * Some tests to ensure Conversation integrity
 */
class ConversationTest {
    private var conversation: Conversation? = null
    @Before
    fun setUp() {
        val contact = Contact(fromString("jami://test"))
        conversation = Conversation("", contact)
    }

    @Test
    fun init_test() {
        val contact = Contact(fromString("jami://test"))
        conversation = Conversation("", contact)
        Assert.assertEquals(conversation!!.contact, contact)
    }

    @get:Throws(Exception::class)
    @get:Test
    val conference: Unit
        get() {}

    @Test
    @Throws(Exception::class)
    fun addConference() {
    }

    @Test
    @Throws(Exception::class)
    fun removeConference() {
    }

    @Test
    @Throws(Exception::class)
    fun setContact() {
    }

    @get:Throws(Exception::class)
    @get:Test
    val isVisible: Unit
        get() {}

    @Test
    @Throws(Exception::class)
    fun setVisible() {
    }

    @get:Throws(Exception::class)
    @get:Test
    val contact: Unit
        get() {}

    @Test
    @Throws(Exception::class)
    fun addHistoryCall() {
        val oldSize = conversation!!.aggregateHistory.size
        conversation!!.addCall(
            Call(
                "Coucou",
                "ring:test",
                "1",
                conversation,
                conversation!!.contact,
                Call.Direction.INCOMING
            )
        )
        val newSize = conversation!!.aggregateHistory.size
        Assert.assertEquals(0, oldSize.toLong())
        Assert.assertEquals(oldSize.toLong(), (newSize - 1).toLong())
    }

    @Test
    @Throws(Exception::class)
    fun addTextMessage() {
        val oldSize = conversation!!.aggregateHistory.size
        conversation!!.addTextMessage(TextMessage("Coucou", "ring:test", "1", conversation, "Toi"))
        val newSize = conversation!!.aggregateHistory.size
        Assert.assertEquals(0, oldSize.toLong())
        Assert.assertEquals(oldSize.toLong(), (newSize - 1).toLong())
    }

    @Test
    @Throws(Exception::class)
    fun updateTextMessage() {
    }

    @get:Throws(Exception::class)
    @get:Test
    val history: Unit
        get() {}

    @get:Throws(Exception::class)
    @get:Test
    val aggregateHistory: Unit
        get() {}

    @get:Throws(Exception::class)
    @get:Test
    val accountsUsed: Unit
        get() {}

    @get:Throws(Exception::class)
    @get:Test
    val lastAccountUsed: Unit
        get() {}

    @get:Throws(Exception::class)
    @get:Test
    val currentCall: Unit
        get() {}

    @get:Throws(Exception::class)
    @get:Test
    val currentCalls: Unit
        get() {}

    @get:Throws(Exception::class)
    @get:Test
    val historyCalls: Unit
        get() {}

    @get:Throws(Exception::class)
    @get:Test
    val unreadTextMessages: Unit
        get() {}

    @get:Throws(Exception::class)
    @get:Test
    val rawHistory: Unit
        get() {}

    @Test
    @Throws(Exception::class)
    fun findConversationElement() {
    }

    @Test
    @Throws(Exception::class)
    fun addFileTransfer() {
        val oldSize = conversation!!.aggregateHistory.size
        conversation!!.addFileTransfer(DataTransfer("1", "Coucoou", "ring:sfvfv", "photo.jpg", true, 10L, 0L, 0L))
        val newSize = conversation!!.aggregateHistory.size
        Assert.assertEquals(0, oldSize.toLong())
        Assert.assertEquals(oldSize.toLong(), (newSize - 1).toLong())
    }

    @Test
    @Throws(Exception::class)
    fun addFileTransfer1() {
    }

    @Test
    @Throws(Exception::class)
    fun addFileTransfers() {
    }

    @Test
    @Throws(Exception::class)
    fun updateFileTransfer() {
    }

    @Test
    @Throws(Exception::class)
    fun removeAll() {
        val random = Random().nextInt(20)
        for (i in 0 until random) {
            conversation!!.addTextMessage(TextMessage("Coucou", "ring:test", "1", conversation, "Toi"))
        }
        val newSize = conversation!!.aggregateHistory.size
        conversation!!.removeAll()
        val lastSize = conversation!!.aggregateHistory.size
        Assert.assertEquals(random.toLong(), newSize.toLong())
        Assert.assertEquals(0, lastSize.toLong())
    }
}