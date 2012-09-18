/*
 *  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */
package com.savoirfairelinux.sflphone.client;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;
import java.util.ArrayList;

public class ContactManager
{
    int mCount;
    Context mContext;
    ArrayList<CallContact> contactList;

    public ContactManager(Context context)
    {
        mCount = 0;
        mContext = context;
        contactList = new ArrayList<CallContact>();

        loadContactList();
    }

    public void loadContactList()
    {
        ContentResolver resolver = mContext.getContentResolver();

        Cursor cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        while(cursor.moveToNext()) {
            mCount++;
            String displayName = "";
            String phoneNumber = "";
            String emailAddress = "";

            String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));

            displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

            /*
            Cursor structuredName = resolver.query(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, null,
                ContactsContract.Contacts._ID +" = "+ contactId, null, null);
            if(structureName.getCount() > 0)
                Log.i("loadContactList", "Got a given name");
            */

            /*
            Cursor sipAddress = resolver.query(ContactsContract.Contacts.SIP_ADDRESS, null,
                ContactsContract.Contacts._ID +" = "+ contactId, null, null);
            if(sipAddress.getCount() > 0)
                Log.i("loadContactList", "Got a sip address");
            */

            String hasPhone = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
            if(hasPhone.compareTo("1") == 0) {
                // You know it has a number so now query it like this
                Cursor phones = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = "+ contactId, null, null);
                while(phones.moveToNext()) {
                    phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                }
                phones.close();
            }

            Cursor emails = resolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = " + contactId, null, null);
            while (emails.moveToNext()) {
                // This would allow you get several email addresses 
                emailAddress = emails.getString(emails.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
            }
            emails.close();

            contactList.add(new CallContact(displayName, phoneNumber, emailAddress));

        }
        cursor.close();
    }

    public int getNbContacts()
    {
        return mCount;
    }

    public CallContact getContact(int position)
    {
        return contactList.get(position);
    }

    public ArrayList<CallContact> getContactList()
    {
        return contactList;
    }
}

