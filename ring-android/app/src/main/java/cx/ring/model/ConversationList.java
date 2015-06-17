package cx.ring.model;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.DataSetObservable;
import android.database.DataSetObserver;

import android.os.AsyncTask;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Log;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cx.ring.history.HistoryCall;
import cx.ring.history.HistoryEntry;
import cx.ring.history.HistoryManager;
import cx.ring.service.CallManagerCallBack;
import cx.ring.service.ConfigurationManagerCallback;
import cx.ring.service.ISipService;

/**
 * Created by adrien on 15-06-11.
 */
public class ConversationList extends Observable {
    static final String TAG = ConversationList.class.getSimpleName();

    Context context;
    //ArrayList<Conversation> conversations;
    Map<String, Conversation> conversations = new HashMap<>();
    ISipService service;
    final private HistoryManager historyManager;

    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w(TAG, "onReceive " + intent.getAction() + " " + intent.getDataString());
        }
    };
/*
    final Set<DataSetObserver> observers = new HashSet<>();
    void registerDataSetObserver(DataSetObserver observer) {
        observers.add(observer);
    }


    void unregisterDataSetObserver(DataSetObserver observer) {
        observers.remove(observer);
        observer.onInvalidated();
    }
*/
    public ConversationList(Context c, ISipService s) {
        context = c;
        service = s;
        historyManager = new HistoryManager(context);
        //context.registerReceiver();
    }

    public Collection<Conversation> getAll() {
        return conversations.values();
    }

    private class ConversationLoader extends AsyncTask<Void, Void, Map<String, Conversation>> {
        private final Context context;
        public ConversationLoader(Context c) {
            context = c;
        }

        @Override
        protected Map<String, Conversation> doInBackground(Void... params) {
            List<HistoryCall> history = null;
            Map<String, Conference> confs = null;
            final Map<String, Conversation> ret = new HashMap<>();
            try {
                history = historyManager.getAll();
                confs = service.getConferenceList();
            } catch (RemoteException | SQLException e) {
                e.printStackTrace();
            }

            CallContact.ContactBuilder builder = CallContact.ContactBuilder.getInstance();

            for (HistoryCall call : history) {
                CallContact contact;
                if (call.getContactID() == CallContact.DEFAULT_ID) {
                    contact = CallContact.ContactBuilder.buildUnknownContact(call.getNumber());
                } else {
                    Cursor result = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null,
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

                if (contact.getId() > 0) {
                    String key = "c:"+Long.toHexString(contact.getId());
                    if (ret.containsKey(key))
                        ret.get(key).addHistoryCall(call);
                    else {
                        Conversation c = new Conversation(contact);
                        c.addHistoryCall(call);
                        ret.put(key, c);
                    }
                }
                else if (ret.containsKey(call.getNumber())) {
                    // It's a direct match
                    ret.get(call.getNumber()).addHistoryCall(call);
                } else {
                    // Maybe we can extract the extension @ account pattern
                    Pattern p = Pattern.compile("<sip:([^@]+)@([^>]+)>");
                    Matcher m = p.matcher(call.getNumber());
                    if (m.find()) {

                        if (ret.containsKey(m.group(1) + "@" + m.group(2))) {
                            ret.get(m.group(1) + "@" + m.group(2)).addHistoryCall(call);
                        } else {
                            /*HistoryEntry e = new HistoryEntry(call.getAccountID(), contact);
                            e.addHistoryCall(call, contact);*/
                            Conversation c = new Conversation(contact);
                            c.addHistoryCall(call);
                            ret.put(m.group(1) + "@" + m.group(2), c);
                        }

                    } else {
                        /*HistoryEntry e = new HistoryEntry(call.getAccountID(), contact);
                        e.addHistoryCall(call, contact);*/
                        Conversation c = new Conversation(contact);
                        c.addHistoryCall(call);
                        ret.put(call.getNumber(), c);
                    }

                }
            }
            for (Map.Entry<String, Conference> conf : confs.entrySet()) {
                ArrayList<SipCall> calls = conf.getValue().getParticipants();
                if (calls.size() == 1) {
                    conf.getValue().getParticipants().get(0).getmContact().getId();
                }
            }
            Log.w(TAG, "ConversationLoader loaded " + ret.size());
            for (Map.Entry<String, Conversation> c : ret.entrySet()) {
                Log.w(TAG, "ConversationLoader " + c.getValue().contact.getmDisplayName());
            }
            return ret;
        }
    }

    public void startListener() {
        new ConversationLoader(context){
            @Override
            protected void onPostExecute(Map<String, Conversation> res) {
                Log.w(TAG, "ConversationLoader onPostExecute " + res.size());
                conversations = res;
                setChanged();
                notifyObservers(conversations.values());
            }
        }.execute();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConfigurationManagerCallback.ACCOUNT_STATE_CHANGED);
        intentFilter.addAction(ConfigurationManagerCallback.ACCOUNTS_CHANGED);

        intentFilter.addAction(CallManagerCallBack.INCOMING_CALL);
        intentFilter.addAction(CallManagerCallBack.INCOMING_TEXT);
        intentFilter.addAction(CallManagerCallBack.CALL_STATE_CHANGED);
        intentFilter.addAction(CallManagerCallBack.CONF_CREATED);
        intentFilter.addAction(CallManagerCallBack.CONF_CHANGED);
        intentFilter.addAction(CallManagerCallBack.CONF_REMOVED);

        context.registerReceiver(receiver, intentFilter);
    }

    public void stopListener() {
        context.unregisterReceiver(receiver);
    }

}
