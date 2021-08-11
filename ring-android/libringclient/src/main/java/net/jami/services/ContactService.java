/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package net.jami.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.jami.model.Account;
import net.jami.model.Contact;
import net.jami.model.Settings;
import net.jami.model.Uri;
import net.jami.utils.Log;
import net.jami.utils.StringUtils;
import ezvcard.VCard;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

/**
 * This service handles the contacts
 * - Load the contacts stored in the system
 * - Keep a local cache of the contacts
 * - Provide query tools to search contacts by id, number, ...
 */
public abstract class ContactService {
    private final static String TAG = ContactService.class.getSimpleName();

    final PreferencesService mPreferencesService;
    final DeviceRuntimeService mDeviceRuntimeService;
    final AccountService mAccountService;

    public abstract Map<Long, Contact> loadContactsFromSystem(boolean loadRingContacts, boolean loadSipContacts);

    protected abstract Contact findContactByIdFromSystem(Long contactId, String contactKey);
    protected abstract Contact findContactBySipNumberFromSystem(String number);
    protected abstract Contact findContactByNumberFromSystem(String number);

    public abstract Completable loadContactData(Contact contact, String accountId);

    public abstract void saveVCardContactData(Contact contact, String accountId, VCard vcard);
    public abstract Single<VCard> saveVCardContact(String accountId, String uri, String displayName, String pictureB64);

    public ContactService(PreferencesService preferencesService, DeviceRuntimeService deviceRuntimeService, AccountService accountService) {
        mPreferencesService = preferencesService;
        mDeviceRuntimeService = deviceRuntimeService;
        mAccountService = accountService;
    }

    /**
     * Load contacts from system and generate a local contact cache
     *
     * @param loadRingContacts if true, ring contacts will be taken care of
     * @param loadSipContacts  if true, sip contacts will be taken care of
     */
    public Single<Map<Long, Contact>> loadContacts(final boolean loadRingContacts, final boolean loadSipContacts, final Account account) {
        return Single.fromCallable(() -> {
            Settings settings = mPreferencesService.getSettings();
            if (settings.isAllowSystemContacts() && mDeviceRuntimeService.hasContactPermission()) {
                return loadContactsFromSystem(loadRingContacts, loadSipContacts);
            }
            return new HashMap<>();
        });
    }

    public Observable<Contact> observeContact(String accountId, Contact contact, boolean withPresence) {
        //Log.w(TAG, "observeContact " + accountId + " " + contact.getUri() + " " + contact.isUser());
        if (contact.isUser())
            withPresence = false;
        Uri uri = contact.getUri();
        String uriString = uri.getRawUriString();
        synchronized (contact) {
            if (contact.getPresenceUpdates() == null) {
                contact.setPresenceUpdates(Observable.<Boolean>create(emitter -> {
                    emitter.onNext(false);
                    contact.setPresenceEmitter(emitter);
                    mAccountService.subscribeBuddy(accountId, uriString, true);
                    emitter.setCancellable(() -> {
                        mAccountService.subscribeBuddy(accountId, uriString, false);
                        contact.setPresenceEmitter(null);
                        emitter.onNext(false);
                    });
                })
                        .replay(1)
                        .refCount(5, TimeUnit.SECONDS));
            }

            if (contact.getUpdates() == null) {
                contact.setUpdates(contact.getUpdatesSubject()
                        .doOnSubscribe(d -> {
                            if (!contact.isUsernameLoaded())
                                mAccountService.lookupAddress(accountId, "", uri.getRawRingId());
                            loadContactData(contact, accountId)
                                    .subscribe(() -> {}, e -> {/*Log.e(TAG, "Error loading contact data: " + e.getMessage())*/});
                        })
                        .filter(c -> c.isUsernameLoaded() && c.detailsLoaded)
                        .replay(1)
                        .refCount());
            }

            return withPresence
                    ? Observable.combineLatest(contact.getUpdates(), contact.getPresenceUpdates(), (c, p) -> {
                //Log.w(TAG, "observeContact UPDATE " + c + " " + p);
                return c;
            })
                    : contact.getUpdates();
        }
    }

    public Observable<List<Contact>> observeContact(String accountId, List<Contact> contacts, boolean withPresence) {
        if (contacts.isEmpty()) {
            return Observable.just(Collections.emptyList());
        } /*else if (contacts.size() == 1 || contacts.size() == 2) {

            return observeContact(accountId, contacts.get(contacts.size() - 1), withPresence).map(Collections::singletonList);
        } */else {
            List<Observable<Contact>> observables = new ArrayList<>(contacts.size());
            for (Contact contact : contacts) {
                if (!contact.isUser())
                    observables.add(observeContact(accountId, contact, withPresence));
            }
            if (observables.isEmpty())
                return Observable.just(Collections.emptyList());
            return Observable.combineLatest(observables, a -> {
                List<Contact> obs = new ArrayList<>(a.length);
                for (Object o : a)
                    obs.add((Contact) o);
                return obs;
            });
        }
    }

    public Single<Contact> getLoadedContact(String accountId, Contact contact, boolean withPresence) {
        return observeContact(accountId, contact, withPresence)
                .filter(c -> c.isUsernameLoaded() && c.detailsLoaded)
                .firstOrError();
    }
    public Single<Contact> getLoadedContact(String accountId, Contact contact) {
        return getLoadedContact(accountId, contact, false);
    }

    public Single<List<Contact>> getLoadedContact(String accountId, List<Contact> contacts, boolean withPresence) {
        if (contacts.isEmpty())
            return Single.just(Collections.emptyList());
        return Observable.fromIterable(contacts)
                .concatMapEager(contact -> getLoadedContact(accountId, contact, withPresence).toObservable())
                .toList(contacts.size());
    }

    public List<Observable<Contact>> observeLoadedContact(String accountId, List<Contact> contacts, boolean withPresence) {
        if (contacts.isEmpty())
            return Collections.emptyList();
        List<Observable<Contact>> ret = new ArrayList<>(contacts.size());
        for (Contact contact : contacts)
            ret.add(observeContact(accountId, contact, withPresence)
                    .filter(c -> c.isUsernameLoaded() && c.detailsLoaded));
        return ret;
    }

    /**
     * Searches a contact in the local cache and then in the system repository
     * In the last case, the contact is created and added to the local cache
     *
     * @return The found/created contact
     */
    public Contact findContactByNumber(Account account, String number) {
        if (StringUtils.isEmpty(number) || account == null) {
            return null;
        }
        return findContact(account, Uri.fromString(number));
    }

    public Contact findContact(Account account, Uri uri) {
        if (uri == null || account == null) {
            return null;
        }

        Contact contact = account.getContactFromCache(uri);
        // TODO load system contact info into SIP contact
        if (account.isSip()) {
            loadContactData(contact, account.getAccountID()).subscribe(() -> {}, e -> Log.e(TAG, "Can't load contact data"));
        }
        return contact;
    }
}