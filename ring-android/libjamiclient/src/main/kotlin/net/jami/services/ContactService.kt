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
package net.jami.services

import ezvcard.VCard
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.Single
import net.jami.model.Account
import net.jami.model.Contact
import net.jami.model.Uri
import net.jami.utils.Log
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * This service handles the contacts
 * - Load the contacts stored in the system
 * - Keep a local cache of the contacts
 * - Provide query tools to search contacts by id, number, ...
 */
abstract class ContactService(
    val mPreferencesService: PreferencesService,
    val mDeviceRuntimeService: DeviceRuntimeService,
    val mAccountService: AccountService
) {
    abstract fun loadContactsFromSystem(loadRingContacts: Boolean, loadSipContacts: Boolean): Map<Long, Contact>
    protected abstract fun findContactByIdFromSystem(contactId: Long, contactKey: String): Contact?
    protected abstract fun findContactBySipNumberFromSystem(number: String): Contact?
    protected abstract fun findContactByNumberFromSystem(number: String): Contact?
    abstract fun loadContactData(contact: Contact, accountId: String): Completable
    abstract fun saveVCardContactData(contact: Contact, accountId: String, vcard: VCard)
    abstract fun saveVCardContact(accountId: String, uri: String?, displayName: String?, pictureB64: String?): Single<VCard>

    /**
     * Load contacts from system and generate a local contact cache
     *
     * @param loadRingContacts if true, ring contacts will be taken care of
     * @param loadSipContacts  if true, sip contacts will be taken care of
     */
    fun loadContacts(loadRingContacts: Boolean, loadSipContacts: Boolean, account: Account?): Single<Map<Long, Contact>> {
        return Single.fromCallable {
            val settings = mPreferencesService.settings
            if (settings.useSystemContacts && mDeviceRuntimeService.hasContactPermission()) {
                return@fromCallable loadContactsFromSystem(loadRingContacts, loadSipContacts)
            }
            HashMap()
        }
    }

    fun observeContact(accountId: String, contact: Contact, withPresence: Boolean): Observable<Contact> {
        //Log.w(TAG, "observeContact " + accountId + " " + contact.getUri() + " " + contact.isUser());
        var withPresence = withPresence
        if (contact.isUser) withPresence = false
        val uri = contact.uri
        val uriString = uri.rawUriString
        synchronized(contact) {
            if (contact.presenceUpdates == null) {
                contact.presenceUpdates = Observable.create { emitter: ObservableEmitter<Boolean> ->
                    emitter.onNext(false)
                    contact.setPresenceEmitter(emitter)
                    mAccountService.subscribeBuddy(accountId, uriString, true)
                    emitter.setCancellable {
                        mAccountService.subscribeBuddy(accountId, uriString, false)
                        contact.setPresenceEmitter(null)
                        emitter.onNext(false)
                    }
                }
                    .replay(1)
                    .refCount(5, TimeUnit.SECONDS)
            }
            if (contact.updates == null) {
                contact.updates = contact.updatesSubject
                    .doOnSubscribe {
                        if (!contact.isUsernameLoaded) mAccountService.lookupAddress(accountId, "", uri.rawRingId)
                        loadContactData(contact, accountId)
                            .subscribe({}) { }
                    }
                    .filter { c: Contact -> c.isUsernameLoaded && c.detailsLoaded }
                    .replay(1)
                    .refCount()
            }
            return if (withPresence) Observable.combineLatest(contact.updates, contact.presenceUpdates,
                { c: Contact, p: Boolean -> c }) else contact.updates!!
        }
    }

    fun observeContact(accountId: String, contacts: List<Contact>, withPresence: Boolean): Observable<List<Contact>> {
        return if (contacts.isEmpty()) {
            Observable.just(emptyList())
        } /*else if (contacts.size() == 1 || contacts.size() == 2) {

            return observeContact(accountId, contacts.get(contacts.size() - 1), withPresence).map(Collections::singletonList);
        } */ else {
            val observables: MutableList<Observable<Contact>> = ArrayList(contacts.size)
            for (contact in contacts) {
                if (!contact.isUser) observables.add(observeContact(accountId, contact, withPresence))
            }
            if (observables.isEmpty()) Observable.just(emptyList()) else Observable.combineLatest(observables) { a: Array<Any> ->
                val obs: MutableList<Contact> = ArrayList(a.size)
                for (o in a) obs.add(o as Contact)
                obs
            }
        }
    }

    fun getLoadedContact(accountId: String, contact: Contact, withPresence: Boolean): Single<Contact> {
        return observeContact(accountId, contact, withPresence)
            .filter { c: Contact -> c.isUsernameLoaded && c.detailsLoaded }
            .firstOrError()
    }

    fun getLoadedContact(accountId: String, contact: Contact): Single<Contact> {
        return getLoadedContact(accountId, contact, false)
    }

    fun getLoadedContact(accountId: String, contacts: List<Contact>, withPresence: Boolean): Single<List<Contact>> {
        return if (contacts.isEmpty()) Single.just(emptyList()) else Observable.fromIterable(contacts)
            .concatMapEager { contact: Contact -> getLoadedContact(accountId, contact, withPresence).toObservable() }
            .toList(contacts.size)
    }

    fun observeLoadedContact(
        accountId: String,
        contacts: List<Contact>,
        withPresence: Boolean
    ): List<Observable<Contact>> {
        if (contacts.isEmpty()) return emptyList()
        val ret: MutableList<Observable<Contact>> = ArrayList(contacts.size)
        for (contact in contacts) ret.add(observeContact(accountId, contact, withPresence)
            .filter { c: Contact -> c.isUsernameLoaded && c.detailsLoaded })
        return ret
    }

    /**
     * Searches a contact in the local cache and then in the system repository
     * In the last case, the contact is created and added to the local cache
     *
     * @return The found/created contact
     */
    fun findContactByNumber(account: Account, number: String): Contact? {
        return if (number.isEmpty()) null else findContact(account, Uri.fromString(number))
    }

    fun findContact(account: Account, uri: Uri): Contact {
        val contact = account.getContactFromCache(uri)
        // TODO load system contact info into SIP contact
        if (account.isSip) {
            loadContactData(contact, account.accountId).subscribe({}) { e: Throwable? ->
                Log.e(
                    TAG,
                    "Can't load contact data"
                )
            }
        }
        return contact
    }

    companion object {
        private val TAG = ContactService::class.simpleName!!
    }
}