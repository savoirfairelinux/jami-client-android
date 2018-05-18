/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.facades;

import java.util.HashMap;
import java.util.Map;

import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.model.Uri;

public class AccountConversations {
    public final Account account;
    public Map<String, Conversation> conversations = new HashMap<>();
    public Map<String, Conversation> pending = new HashMap<>();

    private Map<String, Conversation> cache = new HashMap<>();

    AccountConversations() {
        account = null;
    }

    AccountConversations(Account acc) {
        account = acc;
    }

    public Conversation getByUri(Uri uri) {
        if (uri != null) {
            return getByKey(uri.getRawUriString());
        }
        return null;
    }

    public Conversation getByKey(String key) {
        Conversation conversation = cache.get(key);
        if (conversation != null) {
            return conversation;
        }
        if (account != null) {
            CallContact contact = account.getContactFromCache(key);
            conversation = new Conversation(contact);
            cache.put(key, conversation);
        }
        return conversation;
    }

    public void contactAdded(CallContact contact) {
        Uri uri = contact.getPrimaryUri();
        String key = uri.getRawUriString();
        if (conversations.get(key) != null)
            return;
        Conversation pendingConversation = pending.get(key);
        if (pendingConversation == null) {
            pendingConversation = getByKey(key);
            conversations.put(key, pendingConversation);
        } else {
            pending.remove(key);
            conversations.put(key, pendingConversation);
        }
        pendingConversation.addContactEvent();
    }

    public void contactRemoved(Uri uri) {
        String key = uri.getRawUriString();
        pending.remove(key);
        conversations.remove(key);
    }

    public void addConversation(String key, Conversation value, boolean b) {
        cache.put(key, value);
        if (b) {
            conversations.put(key, value);
        }
    }
}