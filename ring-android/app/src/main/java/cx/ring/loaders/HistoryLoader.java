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

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import cx.ring.history.HistoryCall;
import cx.ring.history.HistoryEntry;
import cx.ring.history.HistoryManager;
import cx.ring.model.CallContact;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HistoryLoader extends AsyncTaskLoader<ArrayList<HistoryEntry>> {

    private static final String TAG = HistoryLoader.class.getSimpleName();

    private HistoryManager historyManager = null;

    public HistoryLoader(Context context) {
        super(context);
        historyManager = new HistoryManager(context);
    }

    @SuppressWarnings("unchecked")
    // Hashmap runtime cast
    @Override
    public ArrayList<HistoryEntry> loadInBackground() {

        HashMap<String,HistoryEntry> historyEntries = new HashMap<String, HistoryEntry>();

        try {
            List<HistoryCall> list = historyManager.getAll();
            CallContact.ContactBuilder builder = CallContact.ContactBuilder.getInstance();
            for (HistoryCall call : list) {
                CallContact contact;
                if (call.getContactID() == CallContact.DEFAULT_ID) {
                    contact = CallContact.ContactBuilder.buildUnknownContact(call.getNumber());
                } else {
                    Cursor result = getContext().getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null,
                            ContactsContract.Contacts._ID + " = ?",
                            new String[]{String.valueOf(call.getContactID())}, null);
                    int iID = result.getColumnIndex(ContactsContract.Contacts._ID);
                    int iName = result.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                    int iPhoto = result.getColumnIndex(ContactsContract.Contacts.PHOTO_ID);

                    if (result.moveToFirst()) {
                        builder.startNewContact(result.getLong(iID), result.getString(iName), result.getLong(iPhoto));
                        builder.addPhoneNumber(call.getNumber(), 0);
                        contact = builder.build();
                    } else {
                        contact = CallContact.ContactBuilder.buildUnknownContact(call.getNumber());
                    }
                    result.close();
                }

                if (historyEntries.containsKey(call.getNumber())) {
                    // It's a direct match
                    historyEntries.get(call.getNumber()).addHistoryCall(call, contact);
                } else {
                    // Maybe we can extract the extension @ account pattern
                    Pattern p = Pattern.compile("<sip:([^@]+)@([^>]+)>");
                    Matcher m = p.matcher(call.getNumber());
                    if (m.find()) {

                        if (historyEntries.containsKey(m.group(1) + "@" + m.group(2))) {
                            historyEntries.get(m.group(1) + "@" + m.group(2)).addHistoryCall(call, contact);
                        } else {
                            HistoryEntry e = new HistoryEntry(call.getAccountID(), contact);
                            e.addHistoryCall(call, contact);
                            historyEntries.put(m.group(1) + "@" + m.group(2), e);
                        }

                    } else {
                        HistoryEntry e = new HistoryEntry(call.getAccountID(), contact);
                        e.addHistoryCall(call, contact);
                        historyEntries.put(call.getNumber(), e);
                    }

                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

/*
        try {
            ArrayList<HashMap<String, String>> history = (ArrayList<HashMap<String, String>>) service.getHistory();

            for (HashMap<String, String> entry : history) {

                CallContact contact;
                String contactName = entry.get(ServiceConstants.history.DISPLAY_NAME_KEY);
                String number_called = entry.get(ServiceConstants.history.PEER_NUMBER_KEY);
                if (contactName.isEmpty()) {
                    contact = ContactBuilder.buildUnknownContact(number_called);
                } else {
                    contact = ContactBuilder.getInstance().buildSimpleContact(contactName, number_called);
                }

                if (historyEntries.containsKey(number_called)) {
                    // It's a direct match
                    historyEntries.get(number_called).addHistoryCall(new HistoryCall(entry), contact);
                } else {
                    // Maybe we can extract the extension @ account pattern
                    Pattern p = Pattern.compile("<sip:([^@]+)@([^>]+)>");
                    Matcher m = p.matcher(number_called);
                    if (m.find()) {

                        if (historyEntries.containsKey(m.group(1) + "@" + m.group(2))) {
                            historyEntries.get(m.group(1) + "@" + m.group(2)).addHistoryCall(new HistoryCall(entry), contact);
                        } else {
                            HistoryEntry e = new HistoryEntry(entry.get(ServiceConstants.history.ACCOUNT_ID_KEY), contact);
                            e.addHistoryCall(new HistoryCall(entry), contact);
                            historyEntries.put(m.group(1) + "@" + m.group(2), e);
                        }

                    } else {

                        HistoryEntry e = new HistoryEntry(entry.get(ServiceConstants.history.ACCOUNT_ID_KEY), contact);
                        e.addHistoryCall(new HistoryCall(entry), contact);
                        historyEntries.put(number_called, e);
                    }

                }

            }

        } catch (RemoteException e) {
            Log.i(TAG, e.toString());
        }*/
        return new ArrayList<HistoryEntry>(historyEntries.values());
    }


}
