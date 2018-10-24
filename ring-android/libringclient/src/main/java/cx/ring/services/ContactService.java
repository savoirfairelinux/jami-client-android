/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Settings;
import cx.ring.model.Uri;
import cx.ring.utils.Log;
import cx.ring.utils.StringUtils;
import ezvcard.VCard;

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

    @Inject
    @Named("ApplicationExecutor")
    ExecutorService mApplicationExecutor;

    private Map<Long, CallContact> mContactList = new HashMap<>();

    public abstract Map<Long, CallContact> loadContactsFromSystem(boolean loadRingContacts, boolean loadSipContacts);

    protected abstract CallContact findContactByIdFromSystem(Long contactId, String contactKey);
    protected abstract CallContact findContactBySipNumberFromSystem(String number);
    protected abstract CallContact findContactByNumberFromSystem(String number);

    public abstract void loadContactData(CallContact callContact);

    public abstract void saveVCardContactData(CallContact contact, VCard vcard);
    public abstract void loadVCardContactData(CallContact contact);

    public ContactService() {
    }

    /**
     * Load contacts from system and generate a local contact cache
     *
     * @param loadRingContacts if true, ring contacts will be taken care of
     * @param loadSipContacts  if true, sip contacts will be taken care of
     */
    public void loadContacts(final boolean loadRingContacts, final boolean loadSipContacts, final Account account) {
        mApplicationExecutor.submit(() -> {
            Settings settings = mPreferencesService.getSettings();
            if (settings.isAllowSystemContacts() && mDeviceRuntimeService.hasContactPermission()) {
                mContactList = loadContactsFromSystem(loadRingContacts, loadSipContacts);
            }
        });
    }

    /**
     * Searches a contact in the local cache and then in the system repository
     * In the last case, the contact is created and added to the local cache
     *
     * @return The found/created contact
     */
    public CallContact findContactById(long id, String key) {
        if (id <= CallContact.DEFAULT_ID) {
            return null;
        }

        Settings settings = mPreferencesService.getSettings();

        CallContact contact = mContactList.get(id);
        if (contact == null && (settings.isAllowSystemContacts() && mDeviceRuntimeService.hasContactPermission())) {
            Log.w(TAG, "getContactById : cache miss for " + id);
            contact = findContactByIdFromSystem(id, key);
            if (contact != null) {
                mContactList.put(id, contact);
            }
        }
        return contact;
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
        if (account.isRing()) {
            return account.getContactFromCache(uri);
        }
        Settings settings = mPreferencesService.getSettings();
        if (settings.isAllowSystemContacts() && mDeviceRuntimeService.hasContactPermission()) {
            CallContact contact = findContactByNumberFromSystem(uri.getRawUriString());
            if (contact != null) {
                mContactList.put(contact.getId(), contact);
                return contact;
            }
        }
        return CallContact.buildUnknown(uri);
    }

    public CallContact findContact(String accountId, Uri uri) {
        return findContact(mAccountService.getAccount(accountId), uri);
    }

    /**
     * Searches a contact by Id and then number in the local cache
     * In the contact is not found in the cache, it is created and added to the local cache
     *
     * @return The found/created contact
     */
    public CallContact findContact(long contactId, String contactKey, Uri contactNumber) {
        CallContact contact = findContactById(contactId, contactKey);
        if (contact != null) {
            contact.addPhoneNumber(contactNumber);
        } else {
            if (contactId > CallContact.DEFAULT_ID) {
                Log.d(TAG, "Can't find contact with id " + contactId);
            }
            contact = findContact(mAccountService.getCurrentAccount(), contactNumber);
        }
        return contact;
    }

}