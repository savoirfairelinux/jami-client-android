/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.provider.ContactsContract;
import android.util.Base64;
import android.util.Log;
import android.util.LongSparseArray;

import androidx.annotation.NonNull;

import net.jami.model.Contact;
import net.jami.model.Conversation;
import net.jami.model.Phone;
import net.jami.model.Uri;
import net.jami.services.ContactService;
import net.jami.utils.Tuple;
import net.jami.utils.VCardUtils;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import cx.ring.utils.AndroidFileUtils;
import cx.ring.views.AvatarFactory;
import ezvcard.VCard;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ContactServiceImpl extends ContactService {

    private static final String TAG = ContactServiceImpl.class.getSimpleName();

    private static final String[] CONTACTS_SUMMARY_PROJECTION = new String[]{
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.PHOTO_ID,
            ContactsContract.Contacts.STARRED
    };

    private static final String[] CONTACTS_DATA_PROJECTION = new String[]{
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS,
            ContactsContract.CommonDataKinds.SipAddress.TYPE,
            ContactsContract.CommonDataKinds.SipAddress.LABEL,
            ContactsContract.CommonDataKinds.Im.PROTOCOL,
            ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL
    };

    private static final String[] CONTACT_PROJECTION = {
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_ID,
            ContactsContract.Contacts.STARRED
    };

    private static final String[] CONTACTS_PHONES_PROJECTION = {
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL
    };

    private static final String[] CONTACTS_SIP_PROJECTION = {
            ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS,
            ContactsContract.CommonDataKinds.SipAddress.TYPE,
            ContactsContract.CommonDataKinds.SipAddress.LABEL
    };

    private static final String[] DATA_PROJECTION = {
            ContactsContract.Data._ID,
            ContactsContract.RawContacts.CONTACT_ID,
            ContactsContract.Data.LOOKUP_KEY,
            ContactsContract.Data.DISPLAY_NAME_PRIMARY,
            ContactsContract.Data.PHOTO_ID,
            ContactsContract.Data.PHOTO_THUMBNAIL_URI,
            ContactsContract.Data.STARRED
    };

    private static final String[] PHONELOOKUP_PROJECTION = {
            ContactsContract.PhoneLookup._ID,
            ContactsContract.PhoneLookup.LOOKUP_KEY,
            ContactsContract.PhoneLookup.PHOTO_ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
    };

    private static final String ID_SELECTION = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?";

    @Inject
    Context mContext;

    @Override
    public Map<Long, Contact> loadContactsFromSystem(boolean loadRingContacts, boolean loadSipContacts) {

        Map<Long, Contact> systemContacts = new HashMap<>();
        ContentResolver contentResolver = mContext.getContentResolver();
        StringBuilder contactsIds = new StringBuilder();
        LongSparseArray<Contact> cache;

        Cursor contactCursor = contentResolver.query(ContactsContract.Data.CONTENT_URI, CONTACTS_DATA_PROJECTION,
                ContactsContract.Data.MIMETYPE + "=? OR " + ContactsContract.Data.MIMETYPE + "=? OR " + ContactsContract.Data.MIMETYPE + "=?",
                new String[]{ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE, ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE}, null);

        if (contactCursor != null) {
            cache = new LongSparseArray<>(contactCursor.getCount());
            contactsIds.ensureCapacity(contactCursor.getCount() * 4);

            final int indexId = contactCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
            final int indexMime = contactCursor.getColumnIndex(ContactsContract.Data.MIMETYPE);
            final int indexNumber = contactCursor.getColumnIndex(ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS);
            final int indexType = contactCursor.getColumnIndex(ContactsContract.CommonDataKinds.SipAddress.TYPE);
            final int indexLabel = contactCursor.getColumnIndex(ContactsContract.CommonDataKinds.SipAddress.LABEL);

            while (contactCursor.moveToNext()) {
                long contactId = contactCursor.getLong(indexId);

                String contactNumber = contactCursor.getString(indexNumber);
                int contactType = contactCursor.getInt(indexType);
                String contactLabel = contactCursor.getString(indexLabel);
                Uri uri = Uri.fromString(contactNumber);

                Contact contact = cache.get(contactId);
                boolean isNewContact = false;
                if (contact == null) {
                    contact = new Contact(uri);
                    contact.setSystemId(contactId);
                    isNewContact = true;
                    contact.setFromSystem(true);
                }

                if (uri.isSingleIp() || (uri.isHexId() && loadRingContacts) || loadSipContacts) {
                    switch (contactCursor.getString(indexMime)) {
                        case ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE:
                            contact.addPhoneNumber(uri, contactType, contactLabel);
                            break;
                        case ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE:
                            contact.addNumber(uri, contactType, contactLabel, Phone.NumberType.SIP);
                            break;
                        case ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE:
                            if (uri.isHexId()) {
                                contact.addNumber(uri, contactType, contactLabel, Phone.NumberType.UNKNOWN);
                            }
                            break;
                    }
                }

                if (isNewContact && !contact.getPhones().isEmpty()) {
                    cache.put(contactId, contact);
                    if (contactsIds.length() > 0) {
                        contactsIds.append(",");
                    }
                    contactsIds.append(contactId);
                }
            }
            contactCursor.close();
        } else {
            cache = new LongSparseArray<>();
        }

        contactCursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, CONTACTS_SUMMARY_PROJECTION,
                ContactsContract.Contacts._ID + " in (" + contactsIds.toString() + ")", null,
                ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC");

        if (contactCursor != null) {
            final int indexId = contactCursor.getColumnIndex(ContactsContract.Contacts._ID);
            final int indexKey = contactCursor.getColumnIndex(ContactsContract.Data.LOOKUP_KEY);
            final int indexName = contactCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            final int indexPhoto = contactCursor.getColumnIndex(ContactsContract.Contacts.PHOTO_ID);

            while (contactCursor.moveToNext()) {
                long contactId = contactCursor.getLong(indexId);
                Contact contact = cache.get(contactId);
                if (contact == null)
                    Log.w(TAG, "Can't find contact with ID " + contactId);
                else {
                    contact.setSystemContactInfo(contactId, contactCursor.getString(indexKey), contactCursor.getString(indexName), contactCursor.getLong(indexPhoto));
                    systemContacts.put(contactId, contact);
                }
            }
            contactCursor.close();
        }

        return systemContacts;
    }

    @Override
    protected Contact findContactByIdFromSystem(Long id, String key) {
        Contact contact = null;
        ContentResolver contentResolver = mContext.getContentResolver();

        try {
            android.net.Uri contentUri;
            if (key != null) {
                contentUri = ContactsContract.Contacts.lookupContact(
                        contentResolver,
                        ContactsContract.Contacts.getLookupUri(id, key));
            } else {
                contentUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
            }

            Cursor result = null;
            if (contentUri != null) {
                result = contentResolver.query(contentUri, CONTACT_PROJECTION, null, null, null);
            }

            if (result == null) {
                return null;
            }

            if (result.moveToFirst()) {
                int indexId = result.getColumnIndex(ContactsContract.Data._ID);
                int indexKey = result.getColumnIndex(ContactsContract.Data.LOOKUP_KEY);
                int indexName = result.getColumnIndex(ContactsContract.Data.DISPLAY_NAME);
                int indexPhoto = result.getColumnIndex(ContactsContract.Data.PHOTO_ID);
                int indexStared = result.getColumnIndex(ContactsContract.Contacts.STARRED);

                long contactId = result.getLong(indexId);

                Log.d(TAG, "Contact name: " + result.getString(indexName) + " id:" + contactId + " key:" + result.getString(indexKey));

                contact = new Contact(Uri.fromString(contentUri.toString()));
                contact.setSystemContactInfo(contactId, result.getString(indexKey), result.getString(indexName), result.getLong(indexPhoto));

                if (result.getInt(indexStared) != 0) {
                    contact.setStared();
                }

                fillContactDetails(contact);
            }

            result.close();
        } catch (Exception e) {
            Log.d(TAG, "findContactByIdFromSystem: Error while searching for contact id=" + id, e);
        }

        if (contact == null) {
            Log.d(TAG, "findContactByIdFromSystem: findById " + id + " can't find contact.");
        }

        return contact;
    }

    private void fillContactDetails(@NonNull Contact contact) {
        ContentResolver contentResolver = mContext.getContentResolver();

        try {
            Cursor cursorPhones = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    CONTACTS_PHONES_PROJECTION, ID_SELECTION,
                    new String[]{String.valueOf(contact.getId())}, null);

            if (cursorPhones != null) {
                final int indexNumber = cursorPhones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                final int indexType = cursorPhones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE);
                final int indexLabel = cursorPhones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL);

                while (cursorPhones.moveToNext()) {
                    contact.addNumber(cursorPhones.getString(indexNumber), cursorPhones.getInt(indexType), cursorPhones.getString(indexLabel), Phone.NumberType.TEL);
                    Log.d(TAG, "Phone:" + cursorPhones.getString(cursorPhones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                }

                cursorPhones.close();
            }

            android.net.Uri baseUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contact.getId());
            android.net.Uri targetUri = android.net.Uri.withAppendedPath(baseUri, ContactsContract.Contacts.Data.CONTENT_DIRECTORY);

            Cursor cursorSip = contentResolver.query(
                    targetUri,
                    CONTACTS_SIP_PROJECTION,
                    ContactsContract.Data.MIMETYPE + "=? OR " + ContactsContract.Data.MIMETYPE + " =?",
                    new String[]{ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE, ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE}, null);

            if (cursorSip != null) {
                final int indexMime = cursorSip.getColumnIndex(ContactsContract.Data.MIMETYPE);
                final int indexSip = cursorSip.getColumnIndex(ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS);
                final int indexType = cursorSip.getColumnIndex(ContactsContract.CommonDataKinds.SipAddress.TYPE);
                final int indexLabel = cursorSip.getColumnIndex(ContactsContract.CommonDataKinds.SipAddress.LABEL);

                while (cursorSip.moveToNext()) {
                    String contactMime = cursorSip.getString(indexMime);
                    String contactNumber = cursorSip.getString(indexSip);

                    if (!contactMime.contentEquals(ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE) || Uri.fromString(contactNumber).isHexId() || "ring".equalsIgnoreCase(cursorSip.getString(indexLabel))) {
                        contact.addNumber(contactNumber, cursorSip.getInt(indexType), cursorSip.getString(indexLabel), Phone.NumberType.SIP);
                    }
                    Log.d(TAG, "SIP phone:" + contactNumber + " " + contactMime + " ");
                }
                cursorSip.close();
            }
        } catch (Exception e) {
            Log.d(TAG, "fillContactDetails: Error while retrieving detail contact info", e);
        }
    }

    public Contact findContactBySipNumberFromSystem(String number) {
        Contact contact = null;
        ContentResolver contentResolver = mContext.getContentResolver();

        try {
            Cursor result = contentResolver.query(ContactsContract.Data.CONTENT_URI,
                    DATA_PROJECTION,
                    ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS + "=?" + " AND (" + ContactsContract.Data.MIMETYPE + "=? OR " + ContactsContract.Data.MIMETYPE + "=?)",
                    new String[]{number, ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE, ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE}, null);

            if (result == null) {
                Log.d(TAG, "findContactBySipNumberFromSystem: " + number + " can't find contact.");
                return Contact.buildSIP(Uri.fromString(number));
            }

            int indexId = result.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID);
            int indexKey = result.getColumnIndex(ContactsContract.Data.LOOKUP_KEY);
            int indexName = result.getColumnIndex(ContactsContract.Data.DISPLAY_NAME);
            int indexPhoto = result.getColumnIndex(ContactsContract.Data.PHOTO_ID);
            int indexStared = result.getColumnIndex(ContactsContract.Contacts.STARRED);

            if (result.moveToFirst()) {
                long contactId = result.getLong(indexId);
                contact = new Contact(Uri.fromString(number));
                contact.setSystemContactInfo(contactId, result.getString(indexKey), result.getString(indexName), result.getLong(indexPhoto));

                if (result.getInt(indexStared) != 0) {
                    contact.setStared();
                }

                fillContactDetails(contact);
            }
            result.close();

            if (contact == null || contact.getPhones() == null || contact.getPhones().isEmpty()) {
                return null;
            }
        } catch (Exception e) {
            Log.d(TAG, "findContactBySipNumberFromSystem: Error while searching for contact number=" + number, e);
        }

        return contact;
    }

    public Contact findContactByNumberFromSystem(String number) {
        Contact contact = null;
        ContentResolver contentResolver = mContext.getContentResolver();

        try {
            android.net.Uri uri = android.net.Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, android.net.Uri.encode(number));
            Cursor result = contentResolver.query(uri, PHONELOOKUP_PROJECTION, null, null, null);
            if (result == null) {
                Log.d(TAG, "findContactByNumberFromSystem: " + number + " can't find contact.");
                return findContactBySipNumberFromSystem(number);
            }
            if (result.moveToFirst()) {
                int indexId = result.getColumnIndex(ContactsContract.Contacts._ID);
                int indexKey = result.getColumnIndex(ContactsContract.Data.LOOKUP_KEY);
                int indexName = result.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                int indexPhoto = result.getColumnIndex(ContactsContract.Contacts.PHOTO_ID);
                contact = new Contact(Uri.fromString(number));
                contact.setSystemContactInfo(result.getLong(indexId), result.getString(indexKey), result.getString(indexName), result.getLong(indexPhoto));
                fillContactDetails(contact);
                Log.d(TAG, "findContactByNumberFromSystem: " + number + " found " + contact.getDisplayName());
            }
            result.close();
        } catch (Exception e) {
            Log.d(TAG, "findContactByNumber: Error while searching for contact number=" + number, e);
        }

        if (contact == null) {
            Log.d(TAG, "findContactByNumberFromSystem: " + number + " can't find contact.");
            contact = findContactBySipNumberFromSystem(number);
        }

        if (contact != null)
            contact.setFromSystem(true);
        return contact;
    }

    @Override
    public Completable loadContactData(Contact contact, String accountId) {
        if (!contact.detailsLoaded) {
            Single<Tuple<String, Object>> profile = contact.isFromSystem() ? loadSystemContactData(contact) : loadVCardContactData(contact, accountId);
            return profile
                    .doOnSuccess(p -> contact.setProfile(p.first, p.second))
                    .doOnError(e -> contact.setProfile(null, null))
                    .ignoreElement()
                    .onErrorComplete();
        }
        return Completable.complete();
    }

    @Override
    public void saveVCardContactData(Contact contact, String accountId, VCard vcard) {
        if (vcard != null) {
            Tuple<String, Object> profileData = VCardServiceImpl.readData(vcard);
            contact.setProfile(profileData.first, profileData.second);
            String filename = contact.getPrimaryNumber() + ".vcf";
            VCardUtils.savePeerProfileToDisk(vcard, accountId
                    , filename, mContext.getFilesDir());
            AvatarFactory.clearCache();
        }
    }

    @Override
    public Single<VCard> saveVCardContact(String accountId, String uri, String displayName, String picture)
    {
        return Single.fromCallable(() -> {
            VCard vcard = VCardUtils.writeData(uri, displayName, Base64.decode(picture, Base64.DEFAULT));
            String filename = uri + ".vcf";
            VCardUtils.savePeerProfileToDisk(vcard, accountId, filename, mContext.getFilesDir());
            return vcard;
        });
    }

    private Single<Tuple<String, Object>> loadVCardContactData(Contact contact, String accountId) {
        String id = contact.getPrimaryNumber();
        if (id != null) {
            return Single.fromCallable(() -> VCardUtils.loadPeerProfileFromDisk(mContext.getFilesDir(), id + ".vcf", accountId))
                    .map(VCardServiceImpl::readData)
                    .subscribeOn(Schedulers.computation());
        }
        return Single.error(new IllegalArgumentException());
    }

    private Single<Tuple<String, Object>> loadSystemContactData(Contact contact) {
        String contactName = contact.getDisplayName();
        android.net.Uri photoURI = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contact.getId());
        return AndroidFileUtils
                .loadBitmap(mContext, android.net.Uri.withAppendedPath(photoURI, ContactsContract.Contacts.Photo.DISPLAY_PHOTO))
                .map(bitmap -> new Tuple<String, Object>(contactName, bitmap))
                .onErrorReturn(e -> new Tuple<>(contactName, null))
                .subscribeOn(Schedulers.io());
    }

    public Single<Drawable> loadConversationAvatar(@NonNull Context context, @NonNull Conversation conversation) {
        return getLoadedContact(conversation.getAccountId(), conversation.getContacts(), false)
                .flatMap(contacts -> AvatarFactory.getAvatar(context, conversation, false));
    }
}
