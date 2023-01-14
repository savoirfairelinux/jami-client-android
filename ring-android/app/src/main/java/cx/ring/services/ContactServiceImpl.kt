/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
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
package cx.ring.services

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.ContactsContract
import android.util.Base64
import android.util.Log
import android.util.LongSparseArray
import cx.ring.utils.AndroidFileUtils
import cx.ring.views.AvatarFactory
import ezvcard.VCard
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.model.Contact
import net.jami.model.Conversation
import net.jami.model.Phone
import net.jami.model.Profile
import net.jami.services.AccountService
import net.jami.services.ContactService
import net.jami.services.DeviceRuntimeService
import net.jami.services.PreferencesService
import net.jami.utils.VCardUtils

class ContactServiceImpl(val mContext: Context, preferenceService: PreferencesService,
                         deviceRuntimeService : DeviceRuntimeService,
                         accountService: AccountService
) : ContactService(preferenceService, deviceRuntimeService, accountService) {
    override fun loadContactsFromSystem(
        loadRingContacts: Boolean,
        loadSipContacts: Boolean
    ): Map<Long, Contact> {
        val systemContacts: MutableMap<Long, Contact> = HashMap()
        val contentResolver = mContext.contentResolver
        val contactsIds = StringBuilder()
        val cache: LongSparseArray<Contact>
        var contactCursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            CONTACTS_DATA_PROJECTION,
            ContactsContract.Data.MIMETYPE + "=? OR " + ContactsContract.Data.MIMETYPE + "=? OR " + ContactsContract.Data.MIMETYPE + "=?",
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE
            ),
            null
        )
        if (contactCursor != null) {
            cache = LongSparseArray(contactCursor.count)
            contactsIds.ensureCapacity(contactCursor.count * 4)
            val indexId =
                contactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val indexMime = contactCursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
            val indexNumber =
                contactCursor.getColumnIndex(ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS)
            val indexType =
                contactCursor.getColumnIndex(ContactsContract.CommonDataKinds.SipAddress.TYPE)
            val indexLabel =
                contactCursor.getColumnIndex(ContactsContract.CommonDataKinds.SipAddress.LABEL)
            while (contactCursor.moveToNext()) {
                val contactId = contactCursor.getLong(indexId)
                val contactNumber = contactCursor.getString(indexNumber)
                val contactType = contactCursor.getInt(indexType)
                val contactLabel = contactCursor.getString(indexLabel)
                val uri = net.jami.model.Uri.fromString(contactNumber)
                var contact = cache[contactId]
                var isNewContact = false
                if (contact == null) {
                    contact = Contact(uri)
                    contact.setSystemId(contactId)
                    isNewContact = true
                    contact.isFromSystem = true
                }
                if (uri.isSingleIp || uri.isHexId && loadRingContacts || loadSipContacts) {
                    when (contactCursor.getString(indexMime)) {
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> contact.addPhoneNumber(
                            uri,
                            contactType,
                            contactLabel
                        )
                        ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE -> contact.addNumber(
                            uri,
                            contactType,
                            contactLabel,
                            Phone.NumberType.SIP
                        )
                        ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE -> if (uri.isHexId) {
                            contact.addNumber(
                                uri,
                                contactType,
                                contactLabel,
                                Phone.NumberType.UNKNOWN
                            )
                        }
                    }
                }
                if (isNewContact && contact.phones.isNotEmpty()) {
                    cache.put(contactId, contact)
                    if (contactsIds.isNotEmpty()) {
                        contactsIds.append(",")
                    }
                    contactsIds.append(contactId)
                }
            }
            contactCursor.close()
        } else {
            cache = LongSparseArray()
        }
        contactCursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI, CONTACTS_SUMMARY_PROJECTION,
            ContactsContract.Contacts._ID + " in (" + contactsIds.toString() + ")", null,
            ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC"
        )
        if (contactCursor != null) {
            val indexId = contactCursor.getColumnIndex(ContactsContract.Contacts._ID)
            val indexKey = contactCursor.getColumnIndex(ContactsContract.Data.LOOKUP_KEY)
            val indexName = contactCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val indexPhoto = contactCursor.getColumnIndex(ContactsContract.Contacts.PHOTO_ID)
            while (contactCursor.moveToNext()) {
                val contactId = contactCursor.getLong(indexId)
                val contact = cache[contactId]
                if (contact == null) Log.w(TAG, "Can't find contact with ID $contactId") else {
                    contact.setSystemContactInfo(
                        contactId,
                        contactCursor.getString(indexKey),
                        contactCursor.getString(indexName),
                        contactCursor.getLong(indexPhoto)
                    )
                    systemContacts[contactId] = contact
                }
            }
            contactCursor.close()
        }
        return systemContacts
    }

    override fun findContactByIdFromSystem(id: Long, key: String): Contact? {
        var contact: Contact? = null
        val contentResolver = mContext.contentResolver
        try {
            val contentUri: Uri? = if (key != null) {
                ContactsContract.Contacts.lookupContact(
                    contentResolver,
                    ContactsContract.Contacts.getLookupUri(id, key)
                )
            } else {
                ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id)
            }
            var result: Cursor? = null
            if (contentUri != null) {
                result = contentResolver.query(contentUri, CONTACT_PROJECTION, null, null, null)
            }
            if (result == null) {
                return null
            }
            if (result.moveToFirst()) {
                val indexId = result.getColumnIndex(ContactsContract.Data._ID)
                val indexKey = result.getColumnIndex(ContactsContract.Data.LOOKUP_KEY)
                val indexName = result.getColumnIndex(ContactsContract.Data.DISPLAY_NAME)
                val indexPhoto = result.getColumnIndex(ContactsContract.Data.PHOTO_ID)
                val indexStared = result.getColumnIndex(ContactsContract.Contacts.STARRED)
                val contactId = result.getLong(indexId)
                Log.d(TAG, "Contact name: " + result.getString(indexName) + " id:" + contactId + " key:" + result.getString(indexKey))
                contact = Contact(net.jami.model.Uri.fromString(contentUri.toString()))
                contact.setSystemContactInfo(
                    contactId,
                    result.getString(indexKey),
                    result.getString(indexName),
                    result.getLong(indexPhoto)
                )
                if (result.getInt(indexStared) != 0) {
                    contact.setStared()
                }
                fillContactDetails(contact)
            }
            result.close()
        } catch (e: Exception) {
            Log.d(TAG, "findContactByIdFromSystem: Error while searching for contact id=$id", e)
        }
        if (contact == null) {
            Log.d(TAG, "findContactByIdFromSystem: findById $id can't find contact.")
        }
        return contact
    }

    private fun fillContactDetails(contact: Contact) {
        val contentResolver = mContext.contentResolver
        try {
            val cursorPhones = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                CONTACTS_PHONES_PROJECTION, ID_SELECTION, arrayOf(contact.id.toString()), null
            )
            if (cursorPhones != null) {
                val indexNumber = cursorPhones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val indexType = cursorPhones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val indexLabel = cursorPhones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
                while (cursorPhones.moveToNext()) {
                    contact.addNumber(
                        cursorPhones.getString(indexNumber),
                        cursorPhones.getInt(indexType),
                        cursorPhones.getString(indexLabel),
                        Phone.NumberType.TEL
                    )
                    Log.d(TAG, "Phone:" + cursorPhones.getString(cursorPhones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)))
                }
                cursorPhones.close()
            }
            val baseUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contact.id)
            val targetUri = Uri.withAppendedPath(baseUri, ContactsContract.Contacts.Data.CONTENT_DIRECTORY)
            val cursorSip = contentResolver.query(
                targetUri,
                CONTACTS_SIP_PROJECTION,
                ContactsContract.Data.MIMETYPE + "=? OR " + ContactsContract.Data.MIMETYPE + " =?",
                arrayOf(
                    ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE,
                    ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE
                ),
                null
            )
            if (cursorSip != null) {
                val indexMime = cursorSip.getColumnIndex(ContactsContract.Data.MIMETYPE)
                val indexSip = cursorSip.getColumnIndex(ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS)
                val indexType = cursorSip.getColumnIndex(ContactsContract.CommonDataKinds.SipAddress.TYPE)
                val indexLabel = cursorSip.getColumnIndex(ContactsContract.CommonDataKinds.SipAddress.LABEL)
                while (cursorSip.moveToNext()) {
                    val contactMime = cursorSip.getString(indexMime)
                    val contactNumber = cursorSip.getString(indexSip)
                    if (contactMime != ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE
                        || net.jami.model.Uri.fromString(contactNumber).isHexId
                        || "ring".equals(cursorSip.getString(indexLabel),ignoreCase = true)) {
                        contact.addNumber(
                            contactNumber,
                            cursorSip.getInt(indexType),
                            cursorSip.getString(indexLabel),
                            Phone.NumberType.SIP
                        )
                    }
                    Log.d(TAG, "SIP phone:$contactNumber $contactMime ")
                }
                cursorSip.close()
            }
        } catch (e: Exception) {
            Log.d(TAG, "fillContactDetails: Error while retrieving detail contact info", e)
        }
    }

    public override fun findContactBySipNumberFromSystem(number: String): Contact? {
        var contact: Contact? = null
        val contentResolver = mContext.contentResolver
        try {
            val result = contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                DATA_PROJECTION,
                ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS + "=?" + " AND (" + ContactsContract.Data.MIMETYPE + "=? OR " + ContactsContract.Data.MIMETYPE + "=?)",
                arrayOf(
                    number,
                    ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE,
                    ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE
                ),
                null
            )
            if (result == null) {
                Log.d(TAG, "findContactBySipNumberFromSystem: $number can't find contact.")
                return Contact.buildSIP(net.jami.model.Uri.fromString(number))
            }
            val indexId = result.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)
            val indexKey = result.getColumnIndex(ContactsContract.Data.LOOKUP_KEY)
            val indexName = result.getColumnIndex(ContactsContract.Data.DISPLAY_NAME)
            val indexPhoto = result.getColumnIndex(ContactsContract.Data.PHOTO_ID)
            val indexStared = result.getColumnIndex(ContactsContract.Contacts.STARRED)
            if (result.moveToFirst()) {
                val contactId = result.getLong(indexId)
                contact = Contact(net.jami.model.Uri.fromString(number))
                contact.setSystemContactInfo(
                    contactId,
                    result.getString(indexKey),
                    result.getString(indexName),
                    result.getLong(indexPhoto)
                )
                if (result.getInt(indexStared) != 0) {
                    contact.setStared()
                }
                fillContactDetails(contact)
            }
            result.close()
            if (contact?.phones == null || contact.phones.isEmpty()) {
                return null
            }
        } catch (e: Exception) {
            Log.d(TAG, "findContactBySipNumberFromSystem: Error while searching for contact number=$number", e)
        }
        return contact
    }

    public override fun findContactByNumberFromSystem(number: String): Contact? {
        var contact: Contact? = null
        val contentResolver = mContext.contentResolver
        try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            val result = contentResolver.query(uri, PHONELOOKUP_PROJECTION, null, null, null)
            if (result == null) {
                Log.d(TAG, "findContactByNumberFromSystem: $number can't find contact.")
                return findContactBySipNumberFromSystem(number)
            }
            if (result.moveToFirst()) {
                val indexId = result.getColumnIndex(ContactsContract.Contacts._ID)
                val indexKey = result.getColumnIndex(ContactsContract.Data.LOOKUP_KEY)
                val indexName = result.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val indexPhoto = result.getColumnIndex(ContactsContract.Contacts.PHOTO_ID)
                contact = Contact(net.jami.model.Uri.fromString(number))
                contact.setSystemContactInfo(
                    result.getLong(indexId),
                    result.getString(indexKey),
                    result.getString(indexName),
                    result.getLong(indexPhoto)
                )
                fillContactDetails(contact)
                //Log.d(TAG, "findContactByNumberFromSystem: " + number + " found " + contact.displayName)
            }
            result.close()
        } catch (e: Exception) {
            Log.d(TAG, "findContactByNumber: Error while searching for contact number=$number", e)
        }
        if (contact == null) {
            Log.d(TAG, "findContactByNumberFromSystem: $number can't find contact.")
            contact = findContactBySipNumberFromSystem(number)
        }
        if (contact != null) contact.isFromSystem = true
        return contact
    }

    override fun loadContactData(contact: Contact, accountId: String): Single<Profile> {
        val profile: Single<Profile> =
            if (contact.isFromSystem) loadSystemContactData(contact)
            else loadVCardContactData(contact, accountId)
        return profile.onErrorReturn { Profile.EMPTY_PROFILE }
    }

    override fun saveVCardContactData(contact: Contact, accountId: String, vcard: VCard) {
        contact.setProfile(VCardServiceImpl.loadVCardProfile(vcard))
        val filename = contact.primaryNumber + ".vcf"
        VCardUtils.savePeerProfileToDisk(vcard, accountId, filename, mContext.filesDir)
        AvatarFactory.clearCache()
    }

    override fun saveVCardContact(accountId: String, uri: String?, displayName: String?, picture: String?): Single<VCard> =
        Single.fromCallable {
            val vcard = VCardUtils.writeData(uri, displayName, Base64.decode(picture, Base64.DEFAULT))
            val filename = "$uri.vcf"
            VCardUtils.savePeerProfileToDisk(vcard, accountId, filename, mContext.filesDir)
            vcard
        }

    private fun loadVCardContactData(contact: Contact, accountId: String): Single<Profile> {
        val id = contact.primaryNumber
        return Single.fromCallable { VCardServiceImpl.readData(VCardUtils.loadPeerProfileFromDisk(mContext.filesDir, "$id.vcf", accountId)) }
            .subscribeOn(Schedulers.io())
    }

    private fun loadSystemContactData(contact: Contact): Single<Profile> {
        val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contact.id)

        val nameSingle = Single.fromCallable {
            mContext.contentResolver.query(contactUri, arrayOf(ContactsContract.Contacts.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        cursor.getString(idx);
                    } else null
                }!! }
            .onErrorReturn { "" }
            .subscribeOn(Schedulers.io())

        val photoSingle = AndroidFileUtils
            .loadBitmap(mContext, Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.DISPLAY_PHOTO))

        return Single.zip(nameSingle, photoSingle) { name, photo -> Profile(name, photo) }
    }

    fun loadConversationAvatar(context: Context, conversation: Conversation): Single<Drawable> =
        getLoadedConversation(conversation)
            .flatMap { contacts -> AvatarFactory.getAvatar(context, contacts) }

    companion object {
        private val TAG = ContactServiceImpl::class.java.simpleName
        private val CONTACTS_SUMMARY_PROJECTION = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.PHOTO_ID,
            ContactsContract.Contacts.STARRED
        )
        private val CONTACTS_DATA_PROJECTION = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS,
            ContactsContract.CommonDataKinds.SipAddress.TYPE,
            ContactsContract.CommonDataKinds.SipAddress.LABEL,
            ContactsContract.CommonDataKinds.Im.PROTOCOL,
            ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL
        )
        private val CONTACT_PROJECTION = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_ID,
            ContactsContract.Contacts.STARRED
        )
        private val CONTACTS_PHONES_PROJECTION = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL
        )
        private val CONTACTS_SIP_PROJECTION = arrayOf(
            ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS,
            ContactsContract.CommonDataKinds.SipAddress.TYPE,
            ContactsContract.CommonDataKinds.SipAddress.LABEL
        )
        private val DATA_PROJECTION = arrayOf(
            ContactsContract.Data._ID,
            ContactsContract.RawContacts.CONTACT_ID,
            ContactsContract.Data.LOOKUP_KEY,
            ContactsContract.Data.DISPLAY_NAME_PRIMARY,
            ContactsContract.Data.PHOTO_ID,
            ContactsContract.Data.PHOTO_THUMBNAIL_URI,
            ContactsContract.Data.STARRED
        )
        private val PHONELOOKUP_PROJECTION = arrayOf(
            ContactsContract.PhoneLookup._ID,
            ContactsContract.PhoneLookup.LOOKUP_KEY,
            ContactsContract.PhoneLookup.PHOTO_ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
        )
        private const val ID_SELECTION = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?"
    }
}