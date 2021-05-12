/*
 * Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 * Author: Pierre Duchemin <pierre.duchemin@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package net.jami.model;

import net.jami.model.CallContact;
import net.jami.model.Conversation;
import net.jami.model.DataTransfer;
import net.jami.model.TextMessage;
import net.jami.model.Uri;

import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * Some tests to ensure Conversation integrity
 */
public class ConversationTest {

    private net.jami.model.Conversation conversation;

    @Before
    public void setUp() throws Exception {
        net.jami.model.CallContact contact = new net.jami.model.CallContact(1L);
        conversation = new net.jami.model.Conversation(contact);
    }

    @Test
    public void init_test() throws Exception {
        CallContact contact = new net.jami.model.CallContact(1L);
        Conversation conversation = new net.jami.model.Conversation(contact);

        assertEquals(conversation.getContact(), contact);
    }

    @Test
    public void getConference() throws Exception {
    }

    @Test
    public void addConference() throws Exception {

    }

    @Test
    public void removeConference() throws Exception {
    }

    @Test
    public void setContact() throws Exception {
    }

    @Test
    public void isVisible() throws Exception {
    }

    @Test
    public void setVisible() throws Exception {
    }

    @Test
    public void getContact() throws Exception {
    }

    @Test
    public void addHistoryCall() throws Exception {
        int oldSize = conversation.getAggregateHistory().size();
        conversation.addHistoryCall(new HistoryCall());
        int newSize = conversation.getAggregateHistory().size();

        assertEquals(0, oldSize);
        assertEquals(oldSize, newSize - 1);
    }

    @Test
    public void addTextMessage() throws Exception {
        int oldSize = conversation.getAggregateHistory().size();
        conversation.addTextMessage(new net.jami.model.TextMessage(true, "Coucou", new net.jami.model.Uri("ring:test"), "1", "Toi"));
        int newSize = conversation.getAggregateHistory().size();

        assertEquals(0, oldSize);
        assertEquals(oldSize, newSize - 1);
    }

    @Test
    public void updateTextMessage() throws Exception {
    }

    @Test
    public void getHistory() throws Exception {
    }

    @Test
    public void getAggregateHistory() throws Exception {
    }

    @Test
    public void getAccountsUsed() throws Exception {
    }

    @Test
    public void getLastAccountUsed() throws Exception {
    }

    @Test
    public void getCurrentCall() throws Exception {
    }

    @Test
    public void getCurrentCalls() throws Exception {
    }

    @Test
    public void getHistoryCalls() throws Exception {
    }

    @Test
    public void getUnreadTextMessages() throws Exception {
    }

    @Test
    public void getRawHistory() throws Exception {
    }

    @Test
    public void findConversationElement() throws Exception {
    }

    @Test
    public void addFileTransfer() throws Exception {
        int oldSize = conversation.getAggregateHistory().size();
        conversation.addFileTransfer(new DataTransfer(1L, "photo.jpg", true, 10L, 0L, "1", "1"));
        int newSize = conversation.getAggregateHistory().size();

        assertEquals(0, oldSize);
        assertEquals(oldSize, newSize - 1);
    }

    @Test
    public void addFileTransfer1() throws Exception {
    }

    @Test
    public void addFileTransfers() throws Exception {
    }

    @Test
    public void updateFileTransfer() throws Exception {
    }

    @Test
    public void removeAll() throws Exception {
        int random = new Random().nextInt(20);

        for (int i = 0; i < random; i++) {
            conversation.addTextMessage(new TextMessage(true, "Coucou", new Uri("ring:test"), "1", "Toi"));
        }
        int newSize = conversation.getAggregateHistory().size();

        conversation.removeAll();
        int lastSize = conversation.getAggregateHistory().size();

        assertEquals(random, newSize);
        assertEquals(0, lastSize);
    }

}
