package com.savoirfairelinux.sflphone.loaders;

import java.util.ArrayList;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.provider.ContactsContract.Contacts;
import android.util.Log;

import com.savoirfairelinux.sflphone.model.CallContact;

public class ContactsLoader extends AsyncTaskLoader<Bundle> {
    
    private static final String TAG = ContactsLoader.class.getSimpleName();

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
            
            Cursor cPhones = getContext().getContentResolver().query(Phone.CONTENT_URI, CONTACTS_PHONES_PROJECTION,
                    Phone.CONTACT_ID + " =" + result.getLong(iID), null, null);

            while (cPhones.moveToNext()) {
                builder.addPhoneNumber(cPhones.getString(cPhones.getColumnIndex(Phone.NUMBER)), cPhones.getInt(cPhones.getColumnIndex(Phone.TYPE)));
//                Log.i(TAG,"Phone:"+cPhones.getString(cPhones.getColumnIndex(Phone.NUMBER)));
            }
            cPhones.close();

            Cursor cSip = getContext().getContentResolver().query(Phone.CONTENT_URI, CONTACTS_SIP_PROJECTION,
                    Phone.CONTACT_ID + "=" + result.getLong(iID), null, null);

            while (cSip.moveToNext()) {
                builder.addSipNumber(cSip.getString(cSip.getColumnIndex(SipAddress.SIP_ADDRESS)), cSip.getInt(cSip.getColumnIndex(SipAddress.TYPE)));
//                Log.i(TAG,"Phone:"+cSip.getString(cSip.getColumnIndex(SipAddress.SIP_ADDRESS)));
            }
            cSip.close();

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
