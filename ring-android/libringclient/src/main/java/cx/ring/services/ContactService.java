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
    private Map<String, CallContact> mContactsRing = new HashMap<>();

    public abstract Map<Long, CallContact> loadContactsFromSystem(boolean loadRingContacts, boolean loadSipContacts);

    protected abstract CallContact findContactByIdFromSystem(Long contactId, String contactKey);
    protected abstract CallContact findContactBySipNumberFromSystem(String number);
    protected abstract CallContact findContactByNumberFromSystem(String number);

    public abstract void loadContactData(CallContact callContact);

    public abstract void saveVCardContactData(CallContact contact);
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
            mContactsRing.clear();
            Map<String, CallContact> ringContacts = account.getContacts();
            for (CallContact contact : ringContacts.values()) {
                mContactsRing.put(contact.getPhones().get(0).getNumber().getRawUriString(), contact);
            }
            /*setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.CONTACTS_CHANGED);
            notifyObservers(event);*/
        });
    }

    /*public void updateContactUserName(Uri contactId, String userName) {
        CallContact callContact = getContact(contactId);
        callContact.setUsername(userName);
    }*/

    /**
     * Add a contact to the local cache
     */
    public void addContact(CallContact contact) {
        if (contact == null) {
            return;
        }

        if (contact.getId() == CallContact.UNKNOWN_ID) {
            Log.w(TAG, "addContact " + contact);
            mContactsRing.put(contact.getPhones().get(0).getNumber().getRawUriString(), contact);
        } else {
            mContactList.put(contact.getId(), contact);
        }
    }

    /**
     * Get a contact from the local cache
     * @return null if contact does not exist in the cache
     */
    public CallContact getContact(Uri uri) {
        if (uri == null || uri.isEmpty()) {
            return null;
        }

        CallContact contact = mContactsRing.get(uri.getRawUriString());
        if (contact != null) {
            return contact;
        }

        for (CallContact c : mContactList.values()) {
            if (c.hasNumber(uri)) {
                return c;
            }
        }

        return CallContact.buildUnknown(uri);
    }

    /*public CallContact setRingContactName(String accountId, Uri uri, String name) {
        Log.w(TAG, "setRingContactName " + uri + " " + name);
        CallContact contact = findContact(accountId, uri);
        if (contact != null) {
            contact.setUsername(name);
            return contact;
        }
        return null;
    }*/

    private Collection<CallContact> getContacts() {
        List<CallContact> contacts = new ArrayList<>(mContactList.values());
        List<CallContact> contactsRing = new ArrayList<>(mContactsRing.values());
        for (CallContact contact : contacts) {
            if (!contactsRing.contains(contact)) {
                contactsRing.add(contact);
            }
        }
        return contactsRing;
    }

    public Collection<CallContact> getContactsNoBanned() {
        List<CallContact> contacts = new ArrayList<>(getContacts());
        Iterator<CallContact> it = contacts.iterator();
        while (it.hasNext()) {
            CallContact contact = it.next();
            if (contact.isBanned()) {
                it.remove();
            }
        }
        return contacts;
    }

    public Collection<CallContact> getContactsDaemon() {
        List<CallContact> contacts = new ArrayList<>(mContactsRing.values());
        Iterator<CallContact> it = contacts.iterator();
        while (it.hasNext()) {
            CallContact contact = it.next();
            if (contact.isBanned()) {
                it.remove();
            }
        }
        return contacts;
    }


    /**
     * Searches a contact in the local cache and then in the system repository
     * In the last case, the contact is created and added to the local cache
     *
     * @return The found/created contact
     */
    public CallContact findContactById(long id) {
        return findContactById(id, null);
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