/*
 * Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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

import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * Some tests to ensure Conversation integrity
 */
public class ConversationTest {

    private Conversation conversation;

    @Before
    public void setUp() {
        Contact contact = new Contact(Uri.fromString("jami://test"));
        conversation = new Conversation("", contact);
    }

    @Test
    public void init_test() {
        Contact contact = new Contact(Uri.fromString("jami://test"));
        conversation = new Conversation("", contact);

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
        conversation.addCall(new Call("Coucou", "ring:test", "1", conversation, conversation.getContact(), Call.Direction.INCOMING));
        int newSize = conversation.getAggregateHistory().size();

        assertEquals(0, oldSize);
        assertEquals(oldSize, newSize - 1);
    }

    @Test
    public void addTextMessage() throws Exception {
        int oldSize = conversation.getAggregateHistory().size();
        conversation.addTextMessage(new TextMessage( "Coucou", "ring:test", "1", conversation, "Toi"));
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
        conversation.addFileTransfer(new DataTransfer(1L, "Coucoou", "ring:sfvfv", "photo.jpg", true, 10L, 0L, 0L));
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
            conversation.addTextMessage(new TextMessage( "Coucou", "ring:test", "1", conversation, "Toi"));
        }
        int newSize = conversation.getAggregateHistory().size();

        conversation.removeAll();
        int lastSize = conversation.getAggregateHistory().size();

        assertEquals(random, newSize);
        assertEquals(0, lastSize);
    }

}
