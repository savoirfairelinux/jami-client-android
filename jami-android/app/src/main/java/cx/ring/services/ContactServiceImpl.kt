/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.services

import android.R.attr.bitmap
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.OperationApplicationException
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.RemoteException
import android.provider.ContactsContract
import android.util.Base64
import android.util.Log
import android.util.LongSparseArray
import androidx.core.util.getOrElse
import cx.ring.utils.AndroidFileUtils
import cx.ring.views.AvatarFactory
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.model.Contact
import net.jami.model.Conversation
import net.jami.model.Phone
import net.jami.model.Profile
import net.jami.services.AccountService
import net.jami.services.ContactService
import net.jami.services.PreferencesService
import net.jami.utils.VCardUtils
import java.io.ByteArrayOutputStream

class ContactServiceImpl(val mContext: Context, preferenceService: PreferencesService,
                         accountService: AccountService
) : ContactService(preferenceService, accountService) {
    override fun loadContactsFromSystem(
        loadRingContacts: Boolean,
        loadSipContacts: Boolean
    ): Map<Long, Contact> {
        val systemContacts: MutableMap<Long, Contact> = HashMap()
        val contentResolver = mContext.contentResolver
        val contactsIds = StringBuilder()
        val cache: LongSparseArray<Contact>
        val contactCursor = contentResolver.query(
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
                var isNewContact = false
                val contact = cache.getOrElse(contactId) {
                    isNewContact = true
                    Contact(uri).apply {
                        setSystemId(contactId)
                        isFromSystem = true
                    }
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
        contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI, CONTACTS_SUMMARY_PROJECTION,
            ContactsContract.Contacts._ID + " in (" + contactsIds.toString() + ")", null,
            ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC"
        )?.use {
            val indexId = it.getColumnIndex(ContactsContract.Contacts._ID)
            val indexKey = it.getColumnIndex(ContactsContract.Data.LOOKUP_KEY)
            val indexName = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val indexPhoto = it.getColumnIndex(ContactsContract.Contacts.PHOTO_ID)
            while (it.moveToNext()) {
                val contactId = it.getLong(indexId)
                val contact = cache[contactId]
                if (contact == null) Log.w(TAG, "Unable to find contact with ID $contactId") else {
                    contact.setSystemContactInfo(
                        contactId,
                        it.getString(indexKey),
                        it.getString(indexName),
                        it.getLong(indexPhoto)
                    )
                    systemContacts[contactId] = contact
                }
            }
        }
        return systemContacts
    }

    override fun findContactByIdFromSystem(contactId: Long, contactKey: String?): Contact? {
        var contact: Contact? = null
        val contentResolver = mContext.contentResolver
        try {
            val contentUri: Uri? = if (contactKey != null) {
                ContactsContract.Contacts.lookupContact(
                    contentResolver,
                    ContactsContract.Contacts.getLookupUri(contactId, contactKey)
                )
            } else {
                ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
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
            Log.d(TAG, "findContactByIdFromSystem: Error while searching for contact id=$contactId", e)
        }
        if (contact == null) {
            Log.d(TAG, "findContactByIdFromSystem: findById $contactId is unable to find contact.")
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
                    Log.d(TAG, "Phone:" + cursorPhones.getString(cursorPhones.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)))
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
                Log.d(TAG, "findContactBySipNumberFromSystem: $number is unable to find contact.")
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
                Log.d(TAG, "findContactByNumberFromSystem: $number is unable to find contact.")
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
            Log.d(TAG, "findContactByNumberFromSystem: $number is unable to find contact.")
            contact = findContactBySipNumberFromSystem(number)
        }
        if (contact != null) contact.isFromSystem = true
        return contact
    }

    override fun saveContact(uri: String, profile: Profile) {
        addOrUpdateContact(mContext.contentResolver, uri, profile.displayName ?: "", profile.avatar as Bitmap?)
    }

    override fun deleteContact(uri: String) {
        deleteContact(mContext.contentResolver, uri)
    }

    private fun addOrUpdateContact(contentResolver: ContentResolver, phoneNumber: String, displayName: String, photo: Bitmap?) {
        val operations = ArrayList<ContentProviderOperation>()

        // Insert or update the RawContact
        operations.add(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
            .build())

        // Insert or update the display name
        operations.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
            .build())

        // Insert or update the phone number
        operations.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
            .build())

        // Insert or update the photo
        photo?.let {
            val stream = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.JPEG, 87, stream)
            val photoByteArray = stream.toByteArray()

            operations.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoByteArray)
                .build())
        }

        // Apply the batch of operations
        try {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
        } catch (e: RemoteException) {
            e.printStackTrace()
        } catch (e: OperationApplicationException) {
            e.printStackTrace()
        }
    }

    private fun deleteContact(contentResolver: ContentResolver, phoneNumber: String) {
        val uri = ContactsContract.RawContacts.CONTENT_URI.buildUpon().build()
        val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?"
        val selectionArgs = arrayOf(phoneNumber)
        contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID),
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val contactId = cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
                contentResolver.delete(uri, "${ContactsContract.RawContacts.CONTACT_ID}=?", arrayOf(contactId.toString()))
            }
        }
    }

    override fun loadContactData(contact: Contact, accountId: String): Single<Profile> {
        val profile: Single<Profile> =
            if (contact.isFromSystem) loadSystemContactData(contact)
            else loadVCardContactData(contact, accountId)
        return profile.onErrorReturn { Profile.EMPTY_PROFILE }
    }

    override fun storeContactData(contact: Contact, profile: Profile, accountId: String) {
        val filename = Base64.encodeToString(contact.primaryNumber.toByteArray(), Base64.NO_WRAP)

        ByteArrayOutputStream().use { stream ->
            val bitmap = (profile.avatar as? Bitmap)
            if (bitmap != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val avatar = stream.toByteArray()
                VCardUtils.writePictureToDisk(avatar, accountId, filename, mContext.cacheDir)
            }
        }

        VCardUtils.savePeerProfileToDisk(VCardUtils.writeData(contact.uri.uri, profile.displayName, null), accountId, filename + ".vcf", mContext.filesDir)
    }

    override fun loadCustomProfileData(contact: Contact, accountId: String): Single<Profile> {
        val id = Base64.encodeToString(contact.primaryNumber.toByteArray(), Base64.NO_WRAP)

        return Single.fromCallable {
            val (name, avatarByteArray) = VCardUtils.getCustomProfile(
                accountId, id, mContext.filesDir)
            val avatarBitmap = avatarByteArray?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            }
            Profile(name, avatarBitmap)
        }.onErrorReturn { Profile.EMPTY_PROFILE }
    }

    private fun loadVCardContactData(contact: Contact, accountId: String): Single<Profile> =
        Single.fromCallable {
            val id = Base64.encodeToString(contact.primaryNumber.toByteArray(), Base64.NO_WRAP)
            VCardServiceImpl.readData(VCardUtils.loadPeerProfileFromDisk(mContext.filesDir, mContext.cacheDir, id, accountId))
        }
            .subscribeOn(Schedulers.io())

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