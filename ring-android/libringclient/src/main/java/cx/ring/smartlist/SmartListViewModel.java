/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
package cx.ring.smartlist;

import java.util.Collections;
import java.util.List;

import cx.ring.model.CallContact;
import cx.ring.model.Interaction;
import cx.ring.services.AccountService;
import io.reactivex.Observable;
import io.reactivex.Single;

public class SmartListViewModel
{
    public static final Observable<SmartListViewModel> TITLE_CONVERSATIONS = Observable.just(new SmartListViewModel(Title.Conversations));
    public static final Observable<SmartListViewModel> TITLE_PUBLIC_DIR = Observable.just(new SmartListViewModel(Title.PublicDirectory));
    public static final Single<List<Observable<SmartListViewModel>>> EMPTY_LIST = Single.just(Collections.emptyList());
    public static final Observable<List<SmartListViewModel>> EMPTY_RESULTS = Observable.just(Collections.emptyList());

    private final String accountId;
    private final CallContact contact;
    private final String uuid;
    private final String contactName;
    private final boolean hasUnreadTextMessage;
    private boolean hasOngoingCall;
    private boolean isOnline = false;
    private final Interaction lastEvent;

    public enum Title {
        None,
        Conversations,
        PublicDirectory
    }
    private final Title title;

    public String picture_b64 = null;

    public SmartListViewModel(String accountId, CallContact contact, String id, Interaction lastEvent) {
        this.accountId = accountId;
        this.contact = contact;
        this.uuid = id;
        this.contactName = contact.getDisplayName();
        hasUnreadTextMessage = (lastEvent != null) && !lastEvent.isRead();
        this.hasOngoingCall = false;
        this.lastEvent = lastEvent;
        isOnline = contact.isOnline();
        title = Title.None;
    }
    public SmartListViewModel(String accountId, CallContact contact, Interaction lastEvent) {
        this.accountId = accountId;
        this.contact = contact;
        this.uuid = contact.getIds().get(0);
        this.contactName = contact.getDisplayName();
        hasUnreadTextMessage = (lastEvent != null) && !lastEvent.isRead();
        this.hasOngoingCall = false;
        this.lastEvent = lastEvent;
        isOnline = contact.isOnline();
        title = Title.None;
    }

    public SmartListViewModel(String accountId, AccountService.User user) {
        contactName = user.firstName + " " + user.lastName;
        this.accountId = accountId;
        this.contact = null;
        this.uuid = user.username;
        hasUnreadTextMessage = false;
        lastEvent = null;
        picture_b64 = user.picture_b64;
        title = Title.None;
    }

    private SmartListViewModel(Title title) {
        contactName = null;
        this.accountId = null;
        this.contact = null;
        this.uuid = null;
        hasUnreadTextMessage = false;
        lastEvent = null;
        picture_b64 = null;
        this.title = title;
    }

    public CallContact getContact() {
        return contact;
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

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

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
                && contactName.equals(other.contactName)
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
