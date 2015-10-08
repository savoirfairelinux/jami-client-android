/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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

package cx.ring.loaders;

import java.util.ArrayList;

import cx.ring.model.CallContact;

import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.provider.ContactsContract.Contacts;
import android.util.Log;

public class ContactsLoader extends AsyncTaskLoader<ContactsLoader.Result>
{
    private static final String TAG = ContactsLoader.class.getSimpleName();

    public class Result {
        public final ArrayList<CallContact> contacts = new ArrayList<>();
        public final ArrayList<CallContact> starred = new ArrayList<>();
    }

    // These are the Contacts rows that we will retrieve.
    static final String[] CONTACTS_SUMMARY_PROJECTION = new String[] { Contacts._ID, Contacts.LOOKUP_KEY, Contacts.DISPLAY_NAME, Contacts.PHOTO_ID, Contacts.STARRED };
    static final String[] CONTACTS_PHONES_PROJECTION = new String[] { Phone.NUMBER, Phone.TYPE };
    static final String[] CONTACTS_SIP_PROJECTION = new String[] { SipAddress.SIP_ADDRESS, SipAddress.TYPE };

    static private final String select = "((" + Contacts.DISPLAY_NAME + " NOTNULL) AND (" + Contacts.HAS_PHONE_NUMBER + "=1) AND (" + Contacts.DISPLAY_NAME + " != '' ))";
    Uri baseUri;

    public ContactsLoader(Context context, Uri u) {
        super(context);
        baseUri = u;
    }

    @Override
    public Result loadInBackground() {
        Result res = new Result();

        ContentResolver cr = getContext().getContentResolver();
        Cursor result = cr.query(baseUri, CONTACTS_SUMMARY_PROJECTION, select, null, Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC");
        if (result == null)
            return res;

        int iID = result.getColumnIndex(Contacts._ID);
        int iKey = result.getColumnIndex(ContactsContract.Data.LOOKUP_KEY);
        int iName = result.getColumnIndex(Contacts.DISPLAY_NAME);
        int iPhoto = result.getColumnIndex(Contacts.PHOTO_ID);
        int iStarred = result.getColumnIndex(Contacts.STARRED);
        CallContact.ContactBuilder builder = CallContact.ContactBuilder.getInstance();

        while (result.moveToNext()) {
            long cid = result.getLong(iID);
            builder.startNewContact(cid, result.getString(iKey), result.getString(iName), result.getLong(iPhoto));
            
            Cursor cPhones = cr.query(Phone.CONTENT_URI, CONTACTS_PHONES_PROJECTION, Phone.CONTACT_ID + " =" + cid, null, null);
            if (cPhones != null) {
                while (cPhones.moveToNext()) {
                    builder.addPhoneNumber(cPhones.getString(cPhones.getColumnIndex(Phone.NUMBER)), cPhones.getInt(cPhones.getColumnIndex(Phone.TYPE)));
                    Log.w(TAG,"Phone:"+cPhones.getString(cPhones.getColumnIndex(Phone.NUMBER)));
                }
                cPhones.close();
            }

            //Cursor cSip = cr.query(Phone.CONTENT_URI, CONTACTS_SIP_PROJECTION, Phone.CONTACT_ID + "=" + cid, null, null);
            Cursor cSip = cr.query(ContactsContract.Data.CONTENT_URI,
                    CONTACTS_SIP_PROJECTION,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=? AND " + ContactsContract.Data.MIMETYPE + "=?",
                    new String[]{String.valueOf(cid), ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE}, null);
            if (cSip != null) {
                while (cSip.moveToNext()) {
                    builder.addSipNumber(cSip.getString(cSip.getColumnIndex(SipAddress.SIP_ADDRESS)), cSip.getInt(cSip.getColumnIndex(SipAddress.TYPE)));
                    Log.w(TAG, "SIP Phone for " + cid + " :" + cSip.getString(cSip.getColumnIndex(SipAddress.SIP_ADDRESS)));
                }
                cSip.close();
            }

            res.contacts.add(builder.build());
            if (result.getInt(iStarred) == 1) {
                res.starred.add(builder.build());
            }
           
        }
        result.close();

        return res;
    }
}
