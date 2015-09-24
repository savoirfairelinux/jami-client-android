package cx.ring.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;

import android.os.AsyncTask;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
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
    static public final Pattern SIP_URI_PATTERN = Pattern.compile("(?:([^\\s]+)\\s*<)?(?:sip:)?([\\w^@]+)@([[\\w]&&[^>]]+)>?");

    Context context;
    private Map<String, Conversation> conversations = new HashMap<>();
    ISipService service;
    final private HistoryManager historyManager;

    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w(TAG, "onReceive " + intent.getAction() + " " + intent.getDataString());
            new ConversationLoader(context){
                @Override
                protected void onPostExecute(Map<String, Conversation> res) {
                    updated(res);
                }
            }.execute();
        }
    };

    public ConversationList(Context c, ISipService s) {
        context = c;
        service = s;
        historyManager = new HistoryManager(context);
    }

    public Collection<Conversation> getAll() {
        return conversations.values();
    }

    public Conversation getByContact(CallContact contact) {
        ArrayList<String> keys = contact.getIds();
        for (String k : keys) {
            Conversation c = conversations.get(k);
            if (c != null)
                return c;
        }
        return null;
    }

    private class ConversationLoader extends AsyncTask<Void, Void, Map<String, Conversation>> {
        private final WeakReference<Context> context;

        public ConversationLoader(Context c) {
            context = new WeakReference<>(c);
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

            Context ctx = context.get();
            if (ctx == null) {
                Log.e(TAG, "Cancel ConversationLoader : context is null");
                return null;
            }
            out: for (HistoryCall call : history) {

                CallContact contact = null;
                if (call.getContactID() == CallContact.DEFAULT_ID) {
                    contact = CallContact.ContactBuilder.buildUnknownContact(call.getNumber());
                } else {
                    Cursor result = ctx.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null,
                            ContactsContract.Contacts._ID + " = ?",
                            new String[]{String.valueOf(call.getContactID())}, null);
                    if (result == null)
                        continue;
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

                Log.w(TAG, "History contact : " + call.getNumber() + " " + call.getAccountID() + " " + contact.getId());


                for (Conversation c : ret.values()) {
                    // match by contact id
                    if (c.contact.getId() > 0 && contact.getId() > 0) {
                        if (c.contact.getId() != contact.getId())
                            continue;
                    } else if (!c.contact.hasNumber(call.getNumber())) {
                        continue;
                    }
                    Log.w(TAG, "        Join to " + c.getContact().getId() + " " + c.getContact().getDisplayName() + " " + call.getNumber());
                    if (c.getContact().getId() <= 0 && contact.getId() > 0) {
                        c.contact = contact;
                    }
                    c.addHistoryCall(call);
                    continue out;
                }

                if (contact.getId() > 0) {
                    String key = "c:"+Long.toHexString(contact.getId());
                    if (ret.containsKey(key))
                        ret.get(key).addHistoryCall(call);
                    else {
                        //Log.w(TAG, "History contact : " + key + " " + contact.getDisplayName());
                        Conversation c = new Conversation(contact);
                        c.addHistoryCall(call);
                        ret.put(key, c);
                    }
                }
                else if (ret.containsKey(call.getNumber())) {
                    // It's a direct match
                    //Log.w(TAG, "History contact : " + key);
                    ret.get(call.getNumber()).addHistoryCall(call);
                } else {
                    // Maybe we can extract the extension @ account pattern
                    Matcher m = SIP_URI_PATTERN.matcher(call.getNumber());

                    if (m.find()) {
                        Log.w(TAG, "        Matching " + m.group(1) + " " + m.group(2) + " " + m.group(3));
                        String display = m.group(1);
                        String raw_uri = m.group(2) + "@" + m.group(3);
                        if ((display != null && display.isEmpty()) && (contact.getDisplayName() == null || contact.getDisplayName().isEmpty()))
                            contact.setDisplayName(display);
                        //Log.w(TAG, "     uri : " + raw_uri);
                        Conversation conv = ret.get(raw_uri);
                        if (conv != null) {
                            conv.addHistoryCall(call);
                        } else {
                            /*HistoryEntry e = new HistoryEntry(call.getAccountID(), contact);
                            e.addHistoryCall(call, contact);*/
                            conv = new Conversation(contact);
                            conv.addHistoryCall(call);
                            ret.put(raw_uri, conv);
                        }

                    } else {
                        Log.w(TAG, "        Non matching " + call.getNumber());
                        /*HistoryEntry e = new HistoryEntry(call.getAccountID(), contact);
                        e.addHistoryCall(call, contact);*/
                        Conversation c = new Conversation(contact);
                        c.addHistoryCall(call);
                        ret.put(call.getNumber(), c);
                    }

                }
            }
            context.clear();
            ctx = null;
            for (Map.Entry<String, Conference> c : confs.entrySet()) {
                Log.w(TAG, "ConversationLoader handling " + c.getKey() + " " + c.getValue().getId());
                Conference conf = c.getValue();
                ArrayList<SipCall> calls = conf.getParticipants();
                if (calls.size() >= 1) {
                    CallContact contact = calls.get(0).getContact();
                    Log.w(TAG, "Contact : " + contact.getId() + " " + contact.getDisplayName());
                    Conversation conv = null;
                    ArrayList<String> ids = contact.getIds();
                    for (String id : ids) {
                        Log.w(TAG, "    uri attempt : " + id);
                        conv = ret.get(id);
                        if (conv != null) break;
                    }
                    if (conv != null) {
                        Log.w(TAG, "Adding conference to existing conversation ");
                        conv.current_calls.add(conf);
                    } else {
                        conv = new Conversation(contact);
                        conv.current_calls.add(conf);
                        ret.put(ids.get(0), conv);
                    }
                }
            }
            for (Conversation c : ret.values())
                Log.w(TAG, "Conversation : " + c.getContact().getId() + " " + c.getContact().getDisplayName() + " " + c.getContact().getPhones().get(0).getNumber() + " " + c.getLastInteraction().toString());
            return ret;
        }
    }

    private void updated(Map<String, Conversation> res) {
        conversations = res;
        setChanged();
        notifyObservers(conversations.values());
    }

    public void startListener() {
        final WeakReference<ConversationList> self = new WeakReference<>(this);
        new ConversationLoader(context){
            @Override
            protected void onPostExecute(Map<String, Conversation> res) {
                ConversationList this_ = self.get();
                if (this_ != null)
                    this_.updated(res);
                else
                    Log.e(TAG, "AsyncTask finished but parent is destroyed..");
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
