/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
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
package cx.ring.services;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Settings;
import cx.ring.model.Uri;
import cx.ring.utils.StringUtils;
import ezvcard.VCard;
import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * This service handles the contacts
 * - Load the contacts stored in the system
 * - Keep a local cache of the contacts
 * - Provide query tools to search contacts by id, number, ...
 */
public abstract class ContactService {
    private final static String TAG = ContactService.class.getName();

    @Inject
    PreferencesService mPreferencesService;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    @Inject
    AccountService mAccountService;

    public abstract Map<Long, CallContact> loadContactsFromSystem(boolean loadRingContacts, boolean loadSipContacts);

    protected abstract CallContact findContactByIdFromSystem(Long contactId, String contactKey);
    protected abstract CallContact findContactBySipNumberFromSystem(String number);
    protected abstract CallContact findContactByNumberFromSystem(String number);

    public abstract Completable loadContactData(CallContact callContact);

    public abstract void saveVCardContactData(CallContact contact, VCard vcard);

    public ContactService() {}

    /**
     * Load contacts from system and generate a local contact cache
     *
     * @param loadRingContacts if true, ring contacts will be taken care of
     * @param loadSipContacts  if true, sip contacts will be taken care of
     */
    public Single<Map<Long, CallContact>> loadContacts(final boolean loadRingContacts, final boolean loadSipContacts, final Account account) {
        return Single.fromCallable(() -> {
            Settings settings = mPreferencesService.getSettings();
            if (settings.isAllowSystemContacts() && mDeviceRuntimeService.hasContactPermission()) {
                return loadContactsFromSystem(loadRingContacts, loadSipContacts);
            }
            return new HashMap<>();
        });
    }

    /**
     * Searches a contact in the local cache and then in the system repository
     * In the last case, the contact is created and added to the local cache
     *
     * @return The found/created contact
     */
    public CallContact findContactByNumber(String accountId, String number) {
        if (StringUtils.isEmpty(number) || StringUtils.isEmpty(accountId)) {
            return null;
        }
        return findContact(accountId, new Uri(number));
    }

    public CallContact findContact(Account account, Uri uri) {
        if (uri == null || account == null) {
            return null;
        }

        CallContact contact = account.getContactFromCache(uri);
        // TODO load system contact info into SIP contact
        if (account.isSip()) {
            loadContactData(contact).subscribe();
        }
        return contact;
    }

    public CallContact findContact(String accountId, Uri uri) {
        return findContact(mAccountService.getAccount(accountId), uri);
    }
}