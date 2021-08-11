/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
package net.jami.smartlist;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.jami.model.Contact;
import net.jami.model.Conversation;
import net.jami.model.Interaction;
import net.jami.model.Uri;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public class SmartListViewModel
{
    public static final Observable<SmartListViewModel> TITLE_CONVERSATIONS = Observable.just(new SmartListViewModel(Title.Conversations));
    public static final Observable<SmartListViewModel> TITLE_PUBLIC_DIR = Observable.just(new SmartListViewModel(Title.PublicDirectory));
    public static final Single<List<Observable<SmartListViewModel>>> EMPTY_LIST = Single.just(Collections.emptyList());
    public static final Observable<List<SmartListViewModel>> EMPTY_RESULTS = Observable.just(Collections.emptyList());

    private final String accountId;
    private final Uri uri;
    private final List<Contact> contact;
    private final String uuid;
    private final String contactName;
    private final boolean hasUnreadTextMessage;
    private boolean hasOngoingCall;

    private final boolean showPresence;
    private boolean isOnline = false;
    private boolean isChecked = false;
    private Observable<Boolean> isSelected = null;
    private final Interaction lastEvent;

    public enum Title {
        None,
        Conversations,
        PublicDirectory
    }
    private final Title title;

    public SmartListViewModel(String accountId, Contact contact, Interaction lastEvent) {
        this.accountId = accountId;
        this.contact = Collections.singletonList(contact);
        this.uri = contact.getUri();
        uuid = uri.getRawUriString();
        this.contactName = contact.getDisplayName();
        hasUnreadTextMessage = (lastEvent != null) && !lastEvent.isRead();
        this.hasOngoingCall = false;
        this.lastEvent = lastEvent;
        showPresence = true;
        isOnline = contact.isOnline();
        title = Title.None;
    }
    public SmartListViewModel(String accountId, Contact contact, String id, Interaction lastEvent) {
        this.accountId = accountId;
        this.contact = Collections.singletonList(contact);
        uri = contact.getUri();
        this.uuid = id;
        this.contactName = contact.getDisplayName();
        hasUnreadTextMessage = (lastEvent != null) && !lastEvent.isRead();
        this.hasOngoingCall = false;
        this.lastEvent = lastEvent;
        showPresence = true;
        isOnline = contact.isOnline();
        title = Title.None;
    }
    public SmartListViewModel(Conversation conversation, List<Contact> contacts, boolean presence) {
        this.accountId = conversation.getAccountId();
        this.contact = contacts;
        uri = conversation.getUri();
        this.uuid = uri.getRawUriString();
        this.contactName = conversation.getTitle();
        Interaction lastEvent = conversation.getLastEvent();
        hasUnreadTextMessage = (lastEvent != null) && !lastEvent.isRead();
        this.hasOngoingCall = false;
        this.lastEvent = lastEvent;
        isSelected = conversation.getVisible();
        for (Contact contact : contacts) {
            if (contact.isUser())
                continue;
            if (contact.isOnline()) {
                isOnline = true;
                break;
            }
        }
        showPresence = presence;
        title = Title.None;
    }
    public SmartListViewModel(Conversation conversation, boolean presence) {
        this(conversation, conversation.getContacts(), presence);
    }

    private SmartListViewModel(Title title) {
        contactName = null;
        this.accountId = null;
        this.contact = null;
        this.uuid = null;
        uri = null;
        hasUnreadTextMessage = false;
        lastEvent = null;
        showPresence = false;
        this.title = title;
    }

    public Uri getUri() {
        return uri;
    }

    public boolean isSwarm() {
        return uri.isSwarm();
    }

    public List<Contact> getContacts() {
        return contact;
    }

    /**
     * Used to get contact for one to one or legacy conversations
     */
    public Contact getContact() {
        if (contact.size() == 1)
            return contact.get(0);
        for (Contact c : contact) {
            if (!c.isUser())
                return c;
        }
        return null;
    }

    public String getContactName() {
        return contactName;
    }

    public long getLastInteractionTime() {
        return (lastEvent == null) ? 0 : lastEvent.getTimestamp();
    }

    public boolean hasUnreadTextMessage() {
        return hasUnreadTextMessage;
    }

    public boolean hasOngoingCall() {
        return hasOngoingCall;
    }

    public String getUuid() {
        return uuid;
    }

    /*public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        if (showPresence)
            isOnline = online;
    }*/

    public boolean showPresence() {
        return showPresence;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public boolean isChecked() { return isChecked; }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public Observable<Boolean> getSelected() { return isSelected; }

    public void setHasOngoingCall(boolean hasOngoingCall) {
        this.hasOngoingCall = hasOngoingCall;
    }

    public Interaction getLastEvent() {
        return lastEvent;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SmartListViewModel))
            return false;
        SmartListViewModel other = (SmartListViewModel) o;
        return other.getHeaderTitle() == getHeaderTitle()
                && (getHeaderTitle() != Title.None
                || (contact == other.contact
                && Objects.equals(contactName, other.contactName)
                && isOnline == other.isOnline
                && lastEvent == other.lastEvent
                && hasOngoingCall == other.hasOngoingCall
                && hasUnreadTextMessage == other.hasUnreadTextMessage));
    }

    public String getAccountId() {
        return accountId;
    }

    public Title getHeaderTitle() {
        return title;
    }
}
