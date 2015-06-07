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
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.provider.ContactsContract.Contacts;

public class ContactsLoader extends AsyncTaskLoader<Bundle> {
    
//    private static final String TAG = ContactsLoader.class.getSimpleName();

    // These are the Contacts rows that we will retrieve.
    static final String[] CONTACTS_SUMMARY_PROJECTION = new String[] { Contacts._ID, Contacts.DISPLAY_NAME, Contacts.PHOTO_ID, Contacts.LOOKUP_KEY, Contacts.STARRED };
    static final String[] CONTACTS_PHONES_PROJECTION = new String[] { Phone.NUMBER, Phone.TYPE };
    static final String[] CONTACTS_SIP_PROJECTION = new String[] { SipAddress.SIP_ADDRESS, SipAddress.TYPE };

    String select = "((" + Contacts.DISPLAY_NAME + " NOTNULL) AND (" + Contacts.HAS_PHONE_NUMBER + "=1) AND (" + Contacts.DISPLAY_NAME + " != '' ))";
    Uri baseUri;

    public ContactsLoader(Context context, Uri u) {
        super(context);
        baseUri = u;
    }

    @Override
    public Bundle loadInBackground() {
        ArrayList<CallContact> contacts = new ArrayList<CallContact>();
        ArrayList<CallContact> starred = new ArrayList<CallContact>();

        Cursor result = getContext().getContentResolver().query(baseUri, CONTACTS_SUMMARY_PROJECTION, select, null,
                Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC");
        int iID = result.getColumnIndex(Contacts._ID);
        int iName = result.getColumnIndex(Contacts.DISPLAY_NAME);
        int iPhoto = result.getColumnIndex(Contacts.PHOTO_ID);
        int iStarred = result.getColumnIndex(Contacts.STARRED);
        CallContact.ContactBuilder builder = CallContact.ContactBuilder.getInstance();
        
        while (result.moveToNext()) {
            builder.startNewContact(result.getLong(iID), result.getString(iName), result.getLong(iPhoto));
            
//            Cursor cPhones = getContext().getContentResolver().query(Phone.CONTENT_URI, CONTACTS_PHONES_PROJECTION,
//                    Phone.CONTACT_ID + " =" + result.getLong(iID), null, null);

//            while (cPhones.moveToNext()) {
//                builder.addPhoneNumber(cPhones.getString(cPhones.getColumnIndex(Phone.NUMBER)), cPhones.getInt(cPhones.getColumnIndex(Phone.TYPE)));
////                Log.i(TAG,"Phone:"+cPhones.getString(cPhones.getColumnIndex(Phone.NUMBER)));
//            }
//            cPhones.close();
//
//            Cursor cSip = getContext().getContentResolver().query(Phone.CONTENT_URI, CONTACTS_SIP_PROJECTION,
//                    Phone.CONTACT_ID + "=" + result.getLong(iID), null, null);
//
//            while (cSip.moveToNext()) {
//                builder.addSipNumber(cSip.getString(cSip.getColumnIndex(SipAddress.SIP_ADDRESS)), cSip.getInt(cSip.getColumnIndex(SipAddress.TYPE)));
////                Log.i(TAG,"Phone:"+cSip.getString(cSip.getColumnIndex(SipAddress.SIP_ADDRESS)));
//            }
//            cSip.close();

            contacts.add(builder.build());
            if (result.getInt(iStarred) == 1) {
                starred.add(builder.build());
            }
           
        }        
        
        result.close();
        Bundle toReturn = new Bundle();
        
       toReturn.putParcelableArrayList("Contacts", contacts);
       toReturn.putParcelableArrayList("Starred", starred);

        return toReturn;
    }
}
