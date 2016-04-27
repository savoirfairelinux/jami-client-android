/*
 *  Copyright (C) 2015-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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

package cx.ring.loaders;

import android.Manifest;
import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.OperationCanceledException;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.util.LongSparseArray;

import java.util.ArrayList;

import cx.ring.R;
import cx.ring.model.CallContact;
import cx.ring.model.SipUri;
import cx.ring.service.LocalService;

public class ContactsLoader extends AsyncTaskLoader<ContactsLoader.Result>
{
    private static final String TAG = ContactsLoader.class.getSimpleName();

    public static class Result {
        public final ArrayList<CallContact> contacts = new ArrayList<>();
        public final ArrayList<CallContact> starred = new ArrayList<>();
    }

    static private final String[] CONTACTS_ID_PROJECTION = new String[] { Contacts._ID };

    static private final String[] CONTACTS_SUMMARY_PROJECTION = new String[]{
            Contacts._ID,
            Contacts.LOOKUP_KEY,
            Contacts.DISPLAY_NAME,
            Contacts.PHOTO_ID,
            Contacts.STARRED
    };

    static private final String[] CONTACTS_DATA_PROJECTION = new String[]{
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.MIMETYPE,
            SipAddress.SIP_ADDRESS,
            SipAddress.TYPE,
            SipAddress.LABEL,
            Im.PROTOCOL,
            Im.CUSTOM_PROTOCOL
    };

    static private final String SELECT = "((" + Contacts.DISPLAY_NAME + " NOTNULL) AND (" + Contacts.DISPLAY_NAME + " != '' ))";

    private final Uri baseUri;
    private final LongSparseArray<CallContact> filterFrom;
    private volatile boolean abandon = false;

    public boolean loadSipContacts = true;
    public boolean loadRingContacts = true;

    public ContactsLoader(Context context) {
        this(context, null, null);
    }

    public ContactsLoader(Context context, Uri base, LongSparseArray < CallContact > filter) {
        super(context);
        baseUri = base;
        filterFrom = filter;
    }

    private boolean checkCancel() {
        return checkCancel(null);
    }
    private boolean checkCancel(Cursor c) {
        if (isLoadInBackgroundCanceled()) {
            Log.w(TAG, "Cancelled");
            if (c != null)
                c.close();
            throw new OperationCanceledException();
        }
        if (abandon) {
            Log.w(TAG, "Abandoned");
            if (c != null)
                c.close();
            return true;
        }
        return false;
    }

    @Override
    public Result loadInBackground()
    {
        final Result res = new Result();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean canUseContacts = sharedPreferences.getBoolean(getContext().getString(R.string.pref_systemContacts_key), true);
        if (!canUseContacts || !LocalService.checkPermission(getContext(), Manifest.permission.READ_CONTACTS))
            return res;

        long startTime = System.nanoTime();
        ContentResolver cr = getContext().getContentResolver();
        if (baseUri != null) {
            Cursor result = cr.query(baseUri, CONTACTS_ID_PROJECTION, SELECT, null, Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC");
            if (result == null)
                return res;

            int iID = result.getColumnIndex(Contacts._ID);
            long[] filter_ids = new long[result.getCount()];
            int i = 0;
            while (result.moveToNext()) {
                long cid = result.getLong(iID);
                filter_ids[i++] = cid;
            }
            result.close();
            res.contacts.ensureCapacity(filter_ids.length);
            int n = filter_ids.length;
            for (i = 0; i < n; i++) {
                CallContact c = filterFrom.get(filter_ids[i]);
                if (c != null) {
                    res.contacts.add(c);
                    if (c.isStared())
                        res.starred.add(c);
                }
            }
        }
        else {
            StringBuilder cids = new StringBuilder();
            LongSparseArray<CallContact> cache;
            {
                Cursor c = cr.query(ContactsContract.Data.CONTENT_URI, CONTACTS_DATA_PROJECTION,
                        ContactsContract.Data.MIMETYPE + "=? OR " + ContactsContract.Data.MIMETYPE + "=? OR " + ContactsContract.Data.MIMETYPE + "=?",
                        new String[]{Phone.CONTENT_ITEM_TYPE, SipAddress.CONTENT_ITEM_TYPE, Im.CONTENT_ITEM_TYPE}, null);
                if (c != null) {
                    cache = new LongSparseArray<>(c.getCount());
                    cids.ensureCapacity(c.getCount() * 4);

                    final int iID = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
                    final int iMime = c.getColumnIndex(ContactsContract.Data.MIMETYPE);
                    final int iNumber = c.getColumnIndex(SipAddress.SIP_ADDRESS);
                    final int iType = c.getColumnIndex(SipAddress.TYPE);
                    final int iLabel = c.getColumnIndex(SipAddress.LABEL);
                    final int iProto = c.getColumnIndex(Im.PROTOCOL);
                    final int iProtoName = c.getColumnIndex(Im.CUSTOM_PROTOCOL);
                    while (c.moveToNext()) {
                        long id = c.getLong(iID);
                        CallContact contact = cache.get(id);
                        boolean new_contact = false;
                        if (contact == null) {
                            contact = new CallContact(id);
                            new_contact = true;
                        }
                        String number = c.getString(iNumber);
                        int type = c.getInt(iType);
                        String label = c.getString(iLabel);
                        SipUri uri = new SipUri(number);
                        if (uri.isSingleIp() || (uri.isRingId() && loadRingContacts) || loadSipContacts) {
                            switch (c.getString(iMime)) {
                                case Phone.CONTENT_ITEM_TYPE:
                                    contact.addPhoneNumber(number, type, label);
                                    break;
                                case SipAddress.CONTENT_ITEM_TYPE:
                                    contact.addNumber(number, type, label, CallContact.NumberType.SIP);
                                    break;
                                case Im.CONTENT_ITEM_TYPE:
                                    if (new SipUri(number).isRingId())
                                        contact.addNumber(number, type, label, CallContact.NumberType.UNKNOWN);
                                    break;
                            }
                        }
                        if (new_contact && !contact.getPhones().isEmpty()) {
                            cache.put(id, contact);
                            if (cids.length() > 0)
                                cids.append(",");
                            cids.append(id);
                        }
                    }
                    c.close();
                } else {
                    cache = new LongSparseArray<>();
                }
            }
            if (checkCancel())
                return null;
            {
                Cursor c = cr.query(Contacts.CONTENT_URI, CONTACTS_SUMMARY_PROJECTION,
                        ContactsContract.Contacts._ID + " in (" + cids.toString() + ")", null,
                        ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC");
                if (c != null) {
                    final int iID = c.getColumnIndex(Contacts._ID);
                    final int iKey = c.getColumnIndex(ContactsContract.Data.LOOKUP_KEY);
                    final int iName = c.getColumnIndex(Contacts.DISPLAY_NAME);
                    final int iPhoto = c.getColumnIndex(Contacts.PHOTO_ID);
                    final int iStarred = c.getColumnIndex(Contacts.STARRED);
                    res.contacts.ensureCapacity(c.getCount());
                    while (c.moveToNext()) {
                        long id = c.getLong(iID);
                        CallContact contact = cache.get(id);
                        if (contact == null)
                            Log.w(TAG, "Can't find contact with ID " + id);
                        else {
                            contact.setContactInfos(c.getString(iKey), c.getString(iName), c.getLong(iPhoto));
                            res.contacts.add(contact);
                            if (c.getInt(iStarred) != 0) {
                                res.starred.add(contact);
                                contact.setStared();
                            }
                        }
                    }
                    c.close();
                }
            }
        }

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;
        Log.w(TAG, "Loading " + res.contacts.size() + " system contacts took " + duration / 1000. + "s");

        return checkCancel() ? null : res;
    }

    @Override
    protected void onAbandon() {
        super.onAbandon();
        abandon = true;
    }
}
