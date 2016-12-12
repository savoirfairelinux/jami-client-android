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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.LongSparseArray;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import cx.ring.model.CallContact;
import cx.ring.model.Uri;

public class ContactServiceImpl extends ContactService {

    private static final String TAG = ContactServiceImpl.class.getName();

    static private final String[] CONTACTS_SUMMARY_PROJECTION = new String[]{
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.PHOTO_ID,
            ContactsContract.Contacts.STARRED
    };

    static private final String[] CONTACTS_DATA_PROJECTION = new String[]{
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS,
            ContactsContract.CommonDataKinds.SipAddress.TYPE,
            ContactsContract.CommonDataKinds.SipAddress.LABEL,
            ContactsContract.CommonDataKinds.Im.PROTOCOL,
            ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL
    };

    @Inject
    Context mContext;

    @Override
    public Map<Long, CallContact> loadContactsFromSystem(boolean loadRingContacts, boolean loadSipContacts) {

        Map<Long, CallContact> systemContacts = new HashMap<>();
        ContentResolver contentResolver = mContext.getContentResolver();
        StringBuilder contactsIds = new StringBuilder();
        LongSparseArray<CallContact> cache;

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
                CallContact contact = cache.get(contactId);

                boolean isNewContact = false;
                if (contact == null) {
                    contact = new CallContact(contactId);
                    isNewContact = true;
                }

                String contactNumber = contactCursor.getString(indexNumber);
                int contactType = contactCursor.getInt(indexType);
                String contactLabel = contactCursor.getString(indexLabel);
                Uri uri = new Uri(contactNumber);

                if (uri.isSingleIp() || (uri.isRingId() && loadRingContacts) || loadSipContacts) {
                    switch (contactCursor.getString(indexMime)) {
                        case ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE:
                            contact.addPhoneNumber(contactNumber, contactType, contactLabel);
                            break;
                        case ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE:
                            contact.addNumber(contactNumber, contactType, contactLabel, cx.ring.model.Phone.NumberType.SIP);
                            break;
                        case ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE:
                            if (new Uri(contactNumber).isRingId()) {
                                contact.addNumber(contactNumber, contactType, contactLabel, cx.ring.model.Phone.NumberType.UNKNOWN);
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

        contactCursor.close();

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
                CallContact contact = cache.get(contactId);
                if (contact == null)
                    Log.w(TAG, "Can't find contact with ID " + contactId);
                else {
                    contact.setContactInfos(contactCursor.getString(indexKey), contactCursor.getString(indexName), contactCursor.getLong(indexPhoto));
                    systemContacts.put(contactId, contact);
                }
            }
            contactCursor.close();
        }


        return systemContacts;
    }

}
