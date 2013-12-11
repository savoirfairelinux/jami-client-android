package org.sflphone.loaders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sflphone.model.CallContact;
import org.sflphone.model.CallContact.ContactBuilder;
import org.sflphone.model.HistoryEntry;
import org.sflphone.model.HistoryEntry.HistoryCall;
import org.sflphone.service.ISipService;
import org.sflphone.service.ServiceConstants;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.RemoteException;
import android.provider.ContactsContract.Contacts;
import android.util.Log;

public class HistoryLoader extends AsyncTaskLoader<ArrayList<HistoryEntry>> {

    private static final String TAG = HistoryLoader.class.getSimpleName();
    private ISipService service;
    HashMap<String, HistoryEntry> historyEntries;

    static final String[] CONTACTS_SUMMARY_PROJECTION = new String[] { Contacts._ID, Contacts.DISPLAY_NAME, Contacts.PHOTO_ID, Contacts.LOOKUP_KEY,
            Contacts.STARRED };

    public HistoryLoader(Context context, ISipService isip) {
        super(context);
        service = isip;
    }

    @SuppressWarnings("unchecked")
    // Hashmap runtime cast
    @Override
    public ArrayList<HistoryEntry> loadInBackground() {

        historyEntries = new HashMap<String, HistoryEntry>();

        if (service == null) {
            return new ArrayList<HistoryEntry>();
        }
        try {
            ArrayList<HashMap<String, String>> history = (ArrayList<HashMap<String, String>>) service.getHistory();
            for (HashMap<String, String> entry : history) {

                CallContact contact = null;
                String contactName = entry.get(ServiceConstants.history.DISPLAY_NAME_KEY);
                String number_called = entry.get(ServiceConstants.history.PEER_NUMBER_KEY);
                if (contactName.isEmpty()) {
                    contact = ContactBuilder.buildUnknownContact(number_called);
                } else {
                    contact = ContactBuilder.getInstance().buildSimpleContact(contactName, number_called);
                }

                if (historyEntries.containsKey(number_called)) {
                    // It's a direct match
                    historyEntries.get(number_called).addHistoryCall(new HistoryCall(entry));
                } else {
                    // Maybe we can extract the extension @ account pattern
                    Pattern p = Pattern.compile("<sip:([^@]+)@([^>]+)>");
                    Matcher m = p.matcher(number_called);
                    if (m.find()) {

                        if (historyEntries.containsKey(m.group(1) + "@" + m.group(2))) {
                            historyEntries.get(m.group(1) + "@" + m.group(2)).addHistoryCall(new HistoryCall(entry));
                        } else {
                            HistoryEntry e = new HistoryEntry(entry.get(ServiceConstants.history.ACCOUNT_ID_KEY), contact);
                            e.addHistoryCall(new HistoryCall(entry));
                            historyEntries.put(m.group(1) + "@" + m.group(2), e);
                        }

                    } else {

                        HistoryEntry e = new HistoryEntry(entry.get(ServiceConstants.history.ACCOUNT_ID_KEY), contact);
                        e.addHistoryCall(new HistoryCall(entry));
                        historyEntries.put(number_called, e);
                    }

                }

            }

        } catch (RemoteException e) {
            Log.i(TAG, e.toString());
        }
        return new ArrayList<HistoryEntry>(historyEntries.values());
    }
}
