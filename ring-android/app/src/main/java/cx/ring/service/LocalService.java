package cx.ring.service;

import android.app.Service;
import android.content.AsyncTaskLoader;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.LongSparseArray;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cx.ring.BuildConfig;
import cx.ring.history.HistoryCall;
import cx.ring.history.HistoryManager;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.SipCall;
import cx.ring.model.account.Account;

public class LocalService extends Service {
    static final String TAG = LocalService.class.getSimpleName();
    static public final String ACTION_CONF_UPDATE = BuildConfig.APPLICATION_ID + ".action.CONF_UPDATE";
    static public final String ACTION_ACCOUNT_UPDATE = BuildConfig.APPLICATION_ID + ".action.ACCOUNT_UPDATE";

    public static final String AUTHORITY = "cx.ring";
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    private ISipService mService = null;

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private Map<String, Conversation> conversations = new HashMap<>();
    private ArrayList<Account> all_accounts = new ArrayList<>();
    private List<Account> accounts = all_accounts;
    private List<Account> ip2ip_account = all_accounts;

    private HistoryManager historyManager;

    AccountsLoader mAccountLoader = null;

    public interface Callbacks {
        ISipService getRemoteService();
        LocalService getService();
    }
    public static class DummyCallbacks implements Callbacks {
        @Override
        public ISipService getRemoteService() {
            return null;
        }
        @Override
        public LocalService getService() {
            return null;
        }
    }
    public static final Callbacks DUMMY_CALLBACKS = new DummyCallbacks();

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate");
        super.onCreate();
        historyManager = new HistoryManager(this);
        Intent intent = new Intent(this, SipService.class);
        startService(intent);
        bindService(intent, mConnection, BIND_AUTO_CREATE | BIND_IMPORTANT | BIND_ABOVE_CLIENT );
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        stopListener();
    }

    private final Loader.OnLoadCompleteListener<ArrayList<Account>> onAccountsLoaded = new Loader.OnLoadCompleteListener<ArrayList<Account>>() {
        @Override
        public void onLoadComplete(Loader<ArrayList<Account>> loader, ArrayList<Account> data) {
            Log.w(TAG, "AccountsLoader Loader.OnLoadCompleteListener");
            all_accounts = data;
            accounts = all_accounts.subList(0,data.size()-1);
            ip2ip_account = all_accounts.subList(data.size()-1,data.size());
            sendBroadcast(new Intent(ACTION_ACCOUNT_UPDATE));
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.w(TAG, "onServiceConnected " + className.getClassName());
            mService = ISipService.Stub.asInterface(service);
            //mBound = true;
            mAccountLoader = new AccountsLoader(LocalService.this);
            mAccountLoader.registerListener(1, onAccountsLoaded);
            mAccountLoader.startLoading();
            mAccountLoader.forceLoad();
            startListener();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.w(TAG, "onServiceDisconnected " + arg0.getClassName());
            if (mAccountLoader != null) {
                mAccountLoader.unregisterListener(onAccountsLoaded);
                mAccountLoader.cancelLoad();
                mAccountLoader.stopLoading();
            }
            //mBound = false;
            mService = null;
        }
    };

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public LocalService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LocalService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "onUnbind");
        if (mConnection != null) {
            unbindService(mConnection);
            mConnection = null;
        }
        return super.onUnbind(intent);
    }

    public ISipService getRemoteService() {
        return mService;
    }

    public List<Account> getAccounts() { return accounts; }
    public List<Account> getIP2IPAccount() { return ip2ip_account; }
    public Account getAccount(String account_id) {
        for (Account acc : all_accounts)
            if (acc.getAccountID().equals(account_id))
                return acc;
        return null;
    }

    public ArrayList<Conversation> getConversations() {
        ArrayList<Conversation> convs = new ArrayList<>(conversations.values());
        Collections.sort(convs, new Comparator<Conversation>() {
            @Override
            public int compare(Conversation lhs, Conversation rhs) {
                return (int) ((rhs.getLastInteraction().getTime() - lhs.getLastInteraction().getTime())/1000l);
            }
        });
        return convs;
    }

    public Conversation getConversation(String id) {
        return conversations.get(id);
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

    public CallContact findContactByNumber(String number) {
        CallContact c = null;
        for (Conversation conv : conversations.values()) {
            if (conv.contact.hasNumber(number))
                return conv.contact;
        }
        return findContactByNumber(getContentResolver(), number);
    }

    public Account guessAccount(CallContact c) {
        /*Conversation conv = getByContact(c);
        if (conv != null) {
            return
        }*/
        return null;
    }

    public static CallContact findById(@NonNull ContentResolver res, long id) {
        Log.e(TAG, "findById " + id);

        final CallContact.ContactBuilder builder = CallContact.ContactBuilder.getInstance();
        Cursor result = res.query(ContactsContract.Contacts.CONTENT_URI, new String[]{
                        ContactsContract.Contacts._ID,
                        ContactsContract.PhoneLookup.DISPLAY_NAME,
                        ContactsContract.Contacts.PHOTO_ID},
                ContactsContract.Contacts._ID + " = ?",
                new String[]{String.valueOf(id)}, null);

        CallContact contact = null;
        if (result != null && result.moveToFirst()) {
            int iID = result.getColumnIndex(ContactsContract.Contacts._ID);
            int iName = result.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            int iPhoto = result.getColumnIndex(ContactsContract.Contacts.PHOTO_ID);
            builder.startNewContact(result.getLong(iID), result.getString(iName), result.getLong(iPhoto));
            //builder.addPhoneNumber(call.getNumber(), 0);
            contact = builder.build();
            result.close();
        }/* else {
            contact = CallContact.ContactBuilder.buildUnknownContact(call.getNumber());
        }*/
        return contact;
    }

    @NonNull
    public static CallContact findContactByNumber(@NonNull ContentResolver res, String number) {
        Log.e(TAG, "findContactByNumber " + number);

        final CallContact.ContactBuilder builder = CallContact.ContactBuilder.getInstance();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        Cursor result = res.query(uri, new String[]{
                ContactsContract.Contacts._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.Contacts.PHOTO_ID}, null, null, null);
        CallContact contact = null;
        if (result != null && result.moveToFirst()) {
            int iID = result.getColumnIndex(ContactsContract.Contacts._ID);
            int iName = result.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            int iPhoto = result.getColumnIndex(ContactsContract.Contacts.PHOTO_ID);
            builder.startNewContact(result.getLong(iID), result.getString(iName), result.getLong(iPhoto));
            contact = builder.build();
            result.close();
        }
        if (contact == null) {
            contact = CallContact.ContactBuilder.buildUnknownContact(number);
        }
        return contact;
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
            final LongSparseArray<CallContact> localContactCache = new LongSparseArray<>(64);

            try {
                history = historyManager.getAll();
                confs = mService.getConferenceList();
            } catch (RemoteException | SQLException e) {
                e.printStackTrace();
            }

            final CallContact.ContactBuilder builder = CallContact.ContactBuilder.getInstance();

            Context ctx = context.get();
            if (ctx == null) {
                Log.e(TAG, "Cancel ConversationLoader : context is null");
                return null;
            }
            for (HistoryCall call : history) {
                //Log.w(TAG, "History call : " + call.getNumber() + " " + call.call_start + " " + call.call_end + " " + call.getEndDate().toString());

                CallContact contact = null;
                if (call.getContactID() <= CallContact.DEFAULT_ID) {
                    contact = CallContact.ContactBuilder.buildUnknownContact(call.getNumber());
                } else {
                    contact = localContactCache.get(call.getContactID());
                    if (contact == null) {
                        contact = findById(getContentResolver(), call.getContactID());
                        if (contact != null)
                            contact.addPhoneNumber(call.getNumber(), 0);
                        else {
                            Log.w(TAG, "Can't find contact with id " + call.getContactID() + ", number " + call.getNumber());
                            contact = CallContact.ContactBuilder.buildUnknownContact(call.getNumber());
                        }
                        localContactCache.put(call.getContactID(), contact);
                    }
                }

                Map.Entry<String, Conversation> merge = null;
                for (Map.Entry<String, Conversation> ce : ret.entrySet()) {
                    Conversation c = ce.getValue();
                    if ((contact.getId() > 0 && contact.getId() == c.contact.getId()) || c.contact.hasNumber(call.getNumber())) {
                        merge = ce;
                        break;
                    }
                }
                if (merge != null) {
                    Conversation c = merge.getValue();
                    //Log.w(TAG, "        Join to " + merge.getKey() + " " + c.getContact().getDisplayName() + " " + call.getNumber());
                    if (c.getContact().getId() <= 0 && contact.getId() > 0) {
                        c.contact = contact;
                        ret.remove(merge.getKey());
                        ret.put(contact.getIds().get(0), c);
                    }
                    c.addHistoryCall(call);
                    continue;
                }
                String key = contact.getIds().get(0);
                if (ret.containsKey(key)) {
                    ret.get(key).addHistoryCall(call);
                } else {
                    Conversation c = new Conversation(contact);
                    c.addHistoryCall(call);
                    ret.put(key, c);
                }
            }
            context.clear();
            ctx = null;
            for (Map.Entry<String, Conference> c : confs.entrySet()) {
                //Log.w(TAG, "ConversationLoader handling " + c.getKey() + " " + c.getValue().getId());
                Conference conf = c.getValue();
                ArrayList<SipCall> calls = conf.getParticipants();
                if (calls.size() >= 1) {
                    CallContact contact = calls.get(0).getContact();
                    //Log.w(TAG, "Contact : " + contact.getId() + " " + contact.getDisplayName());
                    Conversation conv = null;
                    ArrayList<String> ids = contact.getIds();
                    for (String id : ids) {
                        //Log.w(TAG, "    uri attempt : " + id);
                        conv = ret.get(id);
                        if (conv != null) break;
                    }
                    if (conv != null) {
                        //Log.w(TAG, "Adding conference to existing conversation ");
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
        sendBroadcast(new Intent(ACTION_CONF_UPDATE));
    }

    public class AccountsLoader extends AsyncTaskLoader<ArrayList<Account>> {
        public static final String ACCOUNTS = "accounts";
        public static final String ACCOUNT_IP2IP = "IP2IP";
        public AccountsLoader(Context context) {
            super(context);
            Log.w(TAG, "AccountsLoader constructor");
        }
        @SuppressWarnings("unchecked")
        @Override
        public ArrayList<Account> loadInBackground() {
            Log.w(TAG, "AccountsLoader loadInBackground");
            ArrayList<Account> accounts = new ArrayList<>();
            Account IP2IP = null;
            try {
                ArrayList<String> accountIDs = (ArrayList<String>) mService.getAccountList();
                Map<String, String> details;
                ArrayList<Map<String, String>> credentials;
                Map<String, String> state;
                for (String id : accountIDs) {
                    details = (Map<String, String>) mService.getAccountDetails(id);
                    state = (Map<String, String>) mService.getVolatileAccountDetails(id);
                    if (id.contentEquals(ACCOUNT_IP2IP)) {
                        IP2IP = new Account(ACCOUNT_IP2IP, details, new ArrayList<Map<String, String>>(), state); // Empty credentials
                        //accounts.add(IP2IP);
                        continue;
                    }
                    credentials = (ArrayList<Map<String, String>>) mService.getCredentials(id);
                /*for (Map.Entry<String, String> entry : state.entrySet()) {
                    Log.i(TAG, "state:" + entry.getKey() + " -> " + entry.getValue());
                }*/
                    Account tmp = new Account(id, details, credentials, state);
                    accounts.add(tmp);
                    // Log.i(TAG, "account:" + tmp.getAlias() + " " + tmp.isEnabled());

                }
            } catch (RemoteException | NullPointerException e) {
                Log.e(TAG, e.toString());
            }
            accounts.add(IP2IP);
            return accounts;
        }
    }

    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().contentEquals(ConfigurationManagerCallback.ACCOUNT_STATE_CHANGED)) {
                Log.i(TAG, "Received " + intent.getAction() + " " + intent.getStringExtra("Account") + " " + intent.getStringExtra("state") + " " + intent.getIntExtra("code", 0));
                //accountStateChanged(intent.getStringExtra("Account"), intent.getStringExtra("state"), intent.getIntExtra("code", 0));
                for (Account a : accounts) {
                    if (a.getAccountID().contentEquals(intent.getStringExtra("Account"))) {
                        a.setRegistered_state(intent.getStringExtra("state"), intent.getIntExtra("code", 0));
                        //notifyDataSetChanged();
                        sendBroadcast(new Intent(ACTION_ACCOUNT_UPDATE));
                        return;
                    }
                }
            } else if (intent.getAction().contentEquals(ConfigurationManagerCallback.ACCOUNTS_CHANGED)) {
                Log.i(TAG, "Received" + intent.getAction());
                //accountsChanged();
                mAccountLoader.onContentChanged();
                mAccountLoader.startLoading();
            } else {
                Log.w(TAG, "onReceive " + intent.getAction() + " " + intent.getDataString());
                new ConversationLoader(context){
                    @Override
                    protected void onPostExecute(Map<String, Conversation> res) {
                        updated(res);
                    }
                }.execute();
            }

        }
    };

    public void startListener() {
        final WeakReference<LocalService> self = new WeakReference<>(this);
        new ConversationLoader(this){
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                Log.w(TAG, "onPreExecute");
            }

            @Override
            protected void onPostExecute(Map<String, Conversation> res) {
                Log.w(TAG, "onPostExecute");
                LocalService this_ = self.get();
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

        registerReceiver(receiver, intentFilter);
    }

    public void stopListener() {
        unregisterReceiver(receiver);
    }

}
