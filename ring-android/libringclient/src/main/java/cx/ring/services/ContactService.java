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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.daemon.ConfigurationCallback;
import cx.ring.daemon.Ringservice;
import cx.ring.model.CallContact;
import cx.ring.model.ServiceEvent;
import cx.ring.model.Settings;
import cx.ring.model.Uri;
import cx.ring.utils.FutureUtils;
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
 * - CONTACT_ADDED
 * - CONTACT_REMOVED
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

    @Inject
    @Named("DaemonExecutor")
    ExecutorService mExecutor;

    private Map<Long, CallContact> mContactList;
    private Map<String, CallContact> mContactsRing;
    private ConfigurationCallbackHandler mCallbackHandler;

    protected abstract Map<Long, CallContact> loadContactsFromSystem(boolean loadRingContacts, boolean loadSipContacts);

    protected abstract CallContact findContactByIdFromSystem(Long contactId, String contactKey);

    protected abstract CallContact findContactBySipNumberFromSystem(String number);

    protected abstract CallContact findContactByNumberFromSystem(String number);

    public ContactService() {
        mContactList = new HashMap<>();
        mContactsRing = new HashMap<>();
        mCallbackHandler = new ConfigurationCallbackHandler();
    }

    public ConfigurationCallbackHandler getCallbackHandler() {
        return mCallbackHandler;
    }

    public Map<String, CallContact> loadContactsFromDaemon(String accountId) {
        Map<String, CallContact> contacts = new HashMap<>();
        ArrayList<Map<String, String>> contactsDaemon = new ArrayList<>(getContacts(accountId));

        for (Map<String, String> contact : contactsDaemon) {
            if ((!contact.containsKey("banned") || contact.get("banned").equals("false")) && contact.containsKey("id")) {
                String contactId = contact.get("id");
                CallContact callContact = CallContact.buildUnknown("ring:" + contactId);
                contacts.put(contactId, callContact);
            }
        }
        return contacts;
    }

    /**
     * Load contacts from system and generate a local contact cache
     *
     * @param loadRingContacts if true, ring contacts will be taken care of
     * @param loadSipContacts  if true, sip contacts will be taken care of
     */
    public void loadContacts(final boolean loadRingContacts, final boolean loadSipContacts, final String accountId) {
        mApplicationExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Settings settings = mSettingsService.loadSettings();
                if (settings.isAllowSystemContacts() && mDeviceRuntimeService.hasContactPermission()) {
                    mContactList = loadContactsFromSystem(loadRingContacts, loadSipContacts);
                }
                mContactsRing = loadContactsFromDaemon(accountId);
                setChanged();
                ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.CONTACTS_CHANGED);
                notifyObservers(event);
            }
        });
    }

    /**
     * Add a contact to the local cache
     *
     * @param contact
     */
    public void addContact(CallContact contact) {
        if (contact == null) {
            return;
        }

        if (contact.getId() == CallContact.UNKNOWN_ID) {
            mContactsRing.put(contact.getDisplayName(), contact);
        } else {
            mContactList.put(contact.getId(), contact);
        }
    }

    /**
     * Get a contact from the local cache
     *
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

    public Collection<CallContact> getContacts() {
        List<CallContact> contacts = new ArrayList<>(mContactList.values());
        List<CallContact> contactsRing = new ArrayList<>(mContactsRing.values());
        for (CallContact contact : contactsRing) {
            if (!contacts.contains(contact)) {
                contacts.add(contact);
            }
        }
        return contacts;
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

            if (contact.getId() == CallContact.UNKNOWN_ID) {
                mContactsRing.put(contact.getDisplayName(), contact);
            } else {
                mContactList.put(contact.getId(), contact);
            }

            return contact;
        }

        CallContact contact = CallContact.buildUnknown(number);
        mContactsRing.put(contact.getDisplayName(), contact);

        return contact;
    }

    /**
     * Searches a contact by Id and then number in the local cache
     * In the contact is not found in the cache, it is created and added to the local cache
     *
     * @param contactId
     * @param contactKey
     * @param contactNumber
     * @return The found/created contact
     */
    public CallContact findContact(long contactId, String contactKey, String contactNumber) {
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

    /**
     * Add a new contact for the account Id on the Daemon
     *
     * @param accountId
     * @param uri
     */
    public void addContact(final String accountId, final String uri) {

        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "addContact() thread running...");
                        Ringservice.addContact(accountId, uri);
                        return true;
                    }
                }
        );
    }

    /**
     * Remove an existing contact for the account Id on the Daemon
     *
     * @param accountId
     * @param uri
     */
    public void removeContact(final String accountId, final String uri) {

        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "removeContact() thread running...");
                        Ringservice.removeContact(accountId, uri);
                        return true;
                    }
                }
        );
    }

    /**
     * @param accountId
     * @return the contacts list from the daemon
     */
    public List<Map<String, String>> getContacts(final String accountId) {

        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<List<Map<String, String>>>() {
                    @Override
                    public List<Map<String, String>> call() throws Exception {
                        Log.i(TAG, "getContacts() thread running...");
                        return Ringservice.getContacts(accountId).toNative();
                    }
                }
        );
    }

    class ConfigurationCallbackHandler extends ConfigurationCallback {

        @Override
        public void contactAdded(String accountId, String uri, boolean confirmed) {
            Log.d(TAG, "contactAdded: " + accountId + ", " + uri + ", " + confirmed);

            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.CONTACT_ADDED);
            event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
            event.addEventInput(ServiceEvent.EventInput.CONFIRMED, confirmed);
            notifyObservers(event);
        }

        @Override
        public void contactRemoved(String accountId, String uri, boolean banned) {
            Log.d(TAG, "contactRemoved: " + accountId + ", " + uri + ", " + banned);

            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.CONTACT_REMOVED);
            event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
            event.addEventInput(ServiceEvent.EventInput.BANNED, banned);
            notifyObservers(event);
        }
    }
}