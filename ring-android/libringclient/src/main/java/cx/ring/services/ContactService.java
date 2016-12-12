/*
 *  Copyright (C) 2016 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.model.CallContact;
import cx.ring.model.ServiceEvent;
import cx.ring.model.Settings;
import cx.ring.model.Uri;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;

/**
 * This service handles the contacts
 * - Load the contacts stored in the system
 * - Keep a local cache of the contacts
 * - Provide query tools to search contacts by id, number, ...
 * * <p>
 * Events are broadcasted:
 * - CONTACTS_CHANGED
 */
public abstract class ContactService extends Observable {

    private final static String TAG = ContactService.class.getName();

    @Inject
    SettingsService mSettingsService;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    @Inject
    @Named("ApplicationExecutor")
    ExecutorService mApplicationExecutor;

    private Map<Long, CallContact> mContactList;

    protected abstract Map<Long, CallContact> loadContactsFromSystem(boolean loadRingContacts, boolean loadSipContacts);

    protected abstract CallContact findContactByIdFromSystem(Long contactId, String contactKey);

    protected abstract CallContact findContactBySipNumberFromSystem(String number);

    protected abstract CallContact findContactByNumberFromSystem(String number);

    public ContactService() {
        mContactList = new HashMap<>();
    }

    /**
     * Load contacts from system and generate a local contact cache
     *
     * @param loadRingContacts if true, ring contacts will be taken care of
     * @param loadSipContacts  if true, sip contacts will be taken care of
     */
    public void loadContacts(final boolean loadRingContacts, final boolean loadSipContacts) {
        Settings settings = mSettingsService.loadSettings();

        if (settings.isAllowSystemContacts() && mDeviceRuntimeService.hasContactPermission()) {
            mApplicationExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    mContactList = loadContactsFromSystem(loadRingContacts, loadSipContacts);

                    setChanged();
                    ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.CONTACTS_CHANGED);
                    notifyObservers(event);
                }
            });
        }
    }

    /**
     * Add a contact to the local cache
     * @param contact
     */
    public void addContact(CallContact contact) {
        if (contact == null) {
            return;
        }

        mContactList.put(contact.getId(), contact);
    }

    /**
     * Get a contact from the local cache
     * @param uri
     * @return null if contact does not exist in the cache
     */
    public CallContact getContact(Uri uri) {
        if (uri == null || uri.isEmpty()) {
            return null;
        }

        for (CallContact contact : mContactList.values()) {
            if (contact.hasNumber(uri)) {
                return contact;
            }
        }

        return null;
    }

    public Collection<CallContact> getContacts () {
        return mContactList.values();
    }

    /**
     * Searches a contact in the local cache and then in the system repository
     * In the last case, the contact is created and added to the local cache
     *
     * @param id
     * @return The found/created contact
     */
    public CallContact findContactById(long id) {
        return findContactById(id, null);
    }

    /**
     * Searches a contact in the local cache and then in the system repository
     * In the last case, the contact is created and added to the local cache
     *
     * @param id
     * @param key
     * @return The found/created contact
     */
    public CallContact findContactById(long id, String key) {

        if (id <= CallContact.DEFAULT_ID) {
            return null;
        }

        Settings settings = mSettingsService.loadSettings();

        CallContact contact = mContactList.get(id);
        if (contact == null && (settings.isAllowSystemContacts() && mDeviceRuntimeService.hasContactPermission())) {
            Log.w(TAG, "getContactById : cache miss for " + id);
            contact = findContactByIdFromSystem(id, key);
            mContactList.put(id, contact);
        }
        return contact;
    }

    /**
     * Searches a contact in the local cache and then in the system repository
     * In the last case, the contact is created and added to the local cache
     *
     * @param number
     * @return The found/created contact
     */
    public CallContact findContactByNumber(String number) {

        if (number == null || number.isEmpty()) {
            return null;
        }

        Settings settings = mSettingsService.loadSettings();

        String searchedCanonicalNumber = CallContact.canonicalNumber(number);

        for (CallContact contact : mContactList.values()) {
            if (contact.hasNumber(searchedCanonicalNumber)) {
                return contact;
            }
        }

        if (settings.isAllowSystemContacts() && mDeviceRuntimeService.hasContactPermission()) {

            CallContact contact = findContactByNumberFromSystem(number);

            if (contact == null) {
                contact = CallContact.buildUnknown(number);
            }

            mContactList.put(contact.getId(), contact);

            return contact;
        }

        CallContact contact = CallContact.buildUnknown(number);
        mContactList.put(contact.getId(), contact);

        return contact;
    }

    /**
     * Searches a contact by Id and then number in the local cache
     * In the contact is not found in the cache, it is created and added to the local cache
     *
     * @param contactId
     * @param contactNumber
     * @param contactKey
     * @return The found/created contact
     */
    public CallContact findContact (long contactId, String contactNumber, String contactKey) {
        CallContact contact;

        if (contactId <= CallContact.DEFAULT_ID) {
            contact = findContactByNumber(contactNumber);
        } else {
            contact = findContactById(contactId, contactKey);
            if (contact != null) {
                contact.addPhoneNumber(contactNumber);
            } else {
                Log.d(TAG, "Can't find contact with id " + contactId);
                contact = findContactByNumber(contactNumber);
            }
        }

        return contact;
    }

}