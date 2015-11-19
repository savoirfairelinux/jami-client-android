/*
 *  Copyright (C) 2015 Savoir-faire Linux Inc.
 *
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

package cx.ring.service;

import android.Manifest;
import android.app.Service;
import android.content.AsyncTaskLoader;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.LruCache;
import android.util.Pair;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cx.ring.BuildConfig;
import cx.ring.history.HistoryCall;
import cx.ring.history.HistoryEntry;
import cx.ring.history.HistoryManager;
import cx.ring.history.HistoryText;
import cx.ring.loaders.ContactsLoader;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.SecureSipCall;
import cx.ring.model.SipCall;
import cx.ring.model.SipUri;
import cx.ring.model.TextMessage;
import cx.ring.model.account.Account;
import cx.ring.model.account.AccountDetailSrtp;
import cx.ring.model.account.AccountDetailTls;


public class LocalService extends Service
{
    static final String TAG = LocalService.class.getSimpleName();
    static public final String ACTION_CONF_UPDATE = BuildConfig.APPLICATION_ID + ".action.CONF_UPDATE";
    static public final String ACTION_ACCOUNT_UPDATE = BuildConfig.APPLICATION_ID + ".action.ACCOUNT_UPDATE";

    public static final String AUTHORITY = "cx.ring";
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);
    public static final int PERMISSIONS_REQUEST = 57;

    public final static String[] REQUIRED_RUNTIME_PERMISSIONS = {Manifest.permission.READ_CONTACTS, Manifest.permission.RECORD_AUDIO};

    private IDRingService mService = null;
    private final ContactsContentObserver contactContentObserver = new ContactsContentObserver();

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private Map<String, Conversation> conversations = new HashMap<>();
    private ArrayList<Account> all_accounts = new ArrayList<>();
    private List<Account> accounts = all_accounts;
    private List<Account> ip2ip_account = all_accounts;

    private HistoryManager historyManager;

    private final LongSparseArray<CallContact> systemContactCache = new LongSparseArray<>();
    private ContactsLoader.Result lastContactLoaderResult = new ContactsLoader.Result();

    private ContactsLoader mSystemContactLoader = null;
    private AccountsLoader mAccountLoader = null;

    private LruCache<Long, Bitmap> mMemoryCache = null;
    private final ExecutorService mPool = Executors.newCachedThreadPool();

    private boolean isWifiConn = false;
    private boolean isMobileConn = false;

    public ContactsLoader.Result getSortedContacts() {
        Log.w(TAG, "getSortedContacts " + lastContactLoaderResult.contacts.size() + " contacts, " + lastContactLoaderResult.starred.size() + " starred.");
        return lastContactLoaderResult;
    }

    public LruCache<Long, Bitmap> get40dpContactCache() {
        return mMemoryCache;
    }

    public ExecutorService getThreadPool() {
        return mPool;
    }

    public LongSparseArray<CallContact> getContactCache() {
        return systemContactCache;
    }

    public boolean isConnected() {
        return isWifiConn || isMobileConn;
    }
    public boolean isWifiConnected() {
        return isWifiConn;
    }

    public Conference placeCall(SipCall call) {
        Conference conf = null;
        CallContact contact = call.getContact();
        Conversation conv = startConversation(contact);
        try {
            String callId = mService.placeCall(call);
            if (callId == null || callId.isEmpty()) {
                //CallActivity.this.terminateCall();
                return null;
            }
            call.setCallID(callId);
            Account acc = getAccount(call.getAccount());
            if(acc.isRing()
                    || acc.getSrtpDetails().getDetailBoolean(AccountDetailSrtp.CONFIG_SRTP_ENABLE)
                    || acc.getTlsDetails().getDetailBoolean(AccountDetailTls.CONFIG_TLS_ENABLE)) {
                Log.i(TAG, "DRingService.placeCall() call is secure");
                SecureSipCall secureCall = new SecureSipCall(call, acc.getSrtpDetails().getDetailString(AccountDetailSrtp.CONFIG_SRTP_KEY_EXCHANGE));
                conf = new Conference(secureCall);
            } else {
                conf = new Conference(call);
            }
            conf.getParticipants().get(0).setContact(contact);
            conv.addConference(conf);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return conf;
    }

    public interface Callbacks {
        IDRingService getRemoteService();
        LocalService getService();
    }
    public static class DummyCallbacks implements Callbacks {
        @Override
        public IDRingService getRemoteService() {
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

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<Long, Bitmap>(cacheSize){
            @Override
            protected int sizeOf(Long key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        historyManager = new HistoryManager(this);
        Intent intent = new Intent(this, DRingService.class);
        startService(intent);
        bindService(intent, mConnection, BIND_AUTO_CREATE | BIND_IMPORTANT | BIND_ABOVE_CLIENT);

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        isWifiConn = ni != null && ni.isConnected();
        ni = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        isMobileConn = ni != null && ni.isConnected();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMemoryCache.evictAll();
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        stopListener();
        mMemoryCache.evictAll();
        mPool.shutdown();
        systemContactCache.clear();
        lastContactLoaderResult = null;
        mAccountLoader.abandon();
        mAccountLoader = null;
    }

    private final Loader.OnLoadCompleteListener<ArrayList<Account>> onAccountsLoaded = new Loader.OnLoadCompleteListener<ArrayList<Account>>() {
        @Override
        public void onLoadComplete(Loader<ArrayList<Account>> loader, ArrayList<Account> data) {
            Log.w(TAG, "AccountsLoader Loader.OnLoadCompleteListener " + data.size());
            all_accounts = data;
            accounts = all_accounts.subList(0,data.size()-1);
            ip2ip_account = all_accounts.subList(data.size()-1,data.size());
            updateConnectivityState();
        }
    };
    private final Loader.OnLoadCompleteListener<ContactsLoader.Result> onSystemContactsLoaded = new Loader.OnLoadCompleteListener<ContactsLoader.Result>() {
        @Override
        public void onLoadComplete(Loader<ContactsLoader.Result> loader, ContactsLoader.Result data) {
            Log.w(TAG, "ContactsLoader Loader.OnLoadCompleteListener " + data.contacts.size() + " contacts, " + data.starred.size() + " starred.");

            lastContactLoaderResult = data;
            systemContactCache.clear();
            for (CallContact c : data.contacts)
                systemContactCache.put(c.getId(), c);

            new ConversationLoader(LocalService.this, systemContactCache){
                @Override
                protected void onPostExecute(Map<String, Conversation> res) {
                    updated(res);
                }
            }.execute();
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.w(TAG, "onServiceConnected " + className.getClassName());
            mService = IDRingService.Stub.asInterface(service);
            //mBound = true;
            mAccountLoader = new AccountsLoader(LocalService.this);
            mAccountLoader.registerListener(1, onAccountsLoaded);
            try {
                if (mService.isStarted()) {
                    mAccountLoader.startLoading();
                    mAccountLoader.forceLoad();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            mSystemContactLoader = new ContactsLoader(LocalService.this);
            mSystemContactLoader.registerListener(1, onSystemContactsLoaded);
            mSystemContactLoader.startLoading();
            mSystemContactLoader.forceLoad();

            startListener();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.w(TAG, "onServiceDisconnected " + arg0.getClassName());
            if (mAccountLoader != null) {
                mAccountLoader.unregisterListener(onAccountsLoaded);
                mAccountLoader.cancelLoad();
                mAccountLoader.stopLoading();
                mAccountLoader = null;
            }
            if (mSystemContactLoader != null) {
                mSystemContactLoader.unregisterListener(onSystemContactsLoaded);
                mSystemContactLoader.cancelLoad();
                mSystemContactLoader.stopLoading();
                mSystemContactLoader = null;
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

    public static boolean checkPermission(Context c, String permission) {
        return ContextCompat.checkSelfPermission(c, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static String[] checkRequiredPermissions(Context c) {
        ArrayList<String> perms = new ArrayList<>();
        for (String p : REQUIRED_RUNTIME_PERMISSIONS) {
            if (!checkPermission(c, p))
                perms.add(p);
        }
        return perms.toArray(new String[perms.size()]);
    }

    public IDRingService getRemoteService() {
        return mService;
    }

    public List<Account> getAccounts() { return accounts; }
    public List<Account> getIP2IPAccount() { return ip2ip_account; }
    public Account getAccount(String account_id) {
        if (account_id == null || account_id.isEmpty())
            return null;
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

    public Conference getConference(String id) {
        for (Conversation conv : conversations.values()) {
            Conference conf = conv.getConference(id);
            if (conf != null)
                return conf;
        }
        return null;
    }

    public Conversation getByContact(CallContact contact) {
        ArrayList<String> keys = contact.getIds();
        for (String k : keys) {
            Conversation c = conversations.get(k);
            if (c != null)
                return c;
        }
        Log.w(TAG, "getByContact failed");
        return null;
    }
    public Conversation getConversationByCallId(String callId) {
        for (Conversation conv : conversations.values()) {
            Conference conf = conv.getConference(callId);
            if (conf != null)
                return conv;
        }
        return null;
    }

    public Conversation startConversation(CallContact contact) {
        if (contact.isUnknown())
            contact = findContactByNumber(CallContact.canonicalNumber(contact.getPhones().get(0).getNumber()));
        Conversation c = getByContact(contact);
        if (c == null) {
            c = new Conversation(contact);
            conversations.put(contact.getIds().get(0), c);
        }
        return c;
    }

    public CallContact findContactByNumber(String number) {
        for (Conversation conv : conversations.values()) {
            if (conv.contact.hasNumber(number))
                return conv.contact;
        }
        return findContactByNumber(getContentResolver(), number);
    }

    public CallContact findContactById(long id) {
        if (id <= 0)
            return null;
        CallContact c = systemContactCache.get(id);
        if (c == null) {
            Log.w(TAG, "getContactById : cache miss for " + id);
            c = findById(getContentResolver(), id);
            systemContactCache.put(id, c);
        }
        return c;
    }

    public Account guessAccount(CallContact c, String number) {
        SipUri uri = new SipUri(number);
        if (uri.isRingId()) {
            for (Account a : all_accounts)
                if (a.isRing())
                    return a;
            // ring ids must be called with ring accounts
            return null;
        }
        for (Account a : all_accounts)
            if (a.isSip() && a.getHost().equals(uri.host))
                return a;
        if (uri.isSingleIp())
            return ip2ip_account.get(0);
        return accounts.get(0);
    }

    public void clearHistory() {
        historyManager.clearDB();
        new ConversationLoader(this, systemContactCache){
            @Override
            protected void onPostExecute(Map<String, Conversation> res) {
                updated(res);
            }
        }.execute();
    }

    public static final String[] DATA_PROJECTION = {
            ContactsContract.Data._ID,
            ContactsContract.RawContacts.CONTACT_ID,
            ContactsContract.Data.LOOKUP_KEY,
            ContactsContract.Data.DISPLAY_NAME_PRIMARY,
            ContactsContract.Data.PHOTO_ID,
            ContactsContract.Data.PHOTO_THUMBNAIL_URI,
            ContactsContract.Data.STARRED
    };
    public static final String[] CONTACT_PROJECTION = {
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.PHOTO_ID,
            ContactsContract.Contacts.STARRED
    };

    public static final String[] PHONELOOKUP_PROJECTION = {
            ContactsContract.PhoneLookup._ID,
            ContactsContract.PhoneLookup.LOOKUP_KEY,
            ContactsContract.PhoneLookup.PHOTO_ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
    };

    private static final String[] CONTACTS_PHONES_PROJECTION = {
            Phone.NUMBER,
            Phone.TYPE,
            Phone.LABEL
    };

    private static final String[] CONTACTS_SIP_PROJECTION = {
            ContactsContract.Data.MIMETYPE,
            SipAddress.SIP_ADDRESS,
            SipAddress.TYPE,
            SipAddress.LABEL
    };

    private static final String ID_SELECTION = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?";

    private static void lookupDetails(@NonNull ContentResolver res, @NonNull CallContact c) {
        Log.w(TAG, "lookupDetails " + c.getKey());
        try {
            Cursor cPhones = res.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    CONTACTS_PHONES_PROJECTION, ID_SELECTION,
                    new String[]{String.valueOf(c.getId())}, null);
            if (cPhones != null) {
                final int iNum =  cPhones.getColumnIndex(Phone.NUMBER);
                final int iType =  cPhones.getColumnIndex(Phone.TYPE);
                final int iLabel =  cPhones.getColumnIndex(Phone.LABEL);
                while (cPhones.moveToNext()) {
                    c.addNumber(cPhones.getString(iNum), cPhones.getInt(iType), cPhones.getString(iLabel), CallContact.NumberType.TEL);
                    Log.w(TAG,"Phone:"+cPhones.getString(cPhones.getColumnIndex(Phone.NUMBER)));
                }
                cPhones.close();
            }

            Uri baseUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, c.getId());
            Uri targetUri = Uri.withAppendedPath(baseUri, ContactsContract.Contacts.Data.CONTENT_DIRECTORY);
            Cursor cSip = res.query(targetUri,
                    CONTACTS_SIP_PROJECTION,
                    ContactsContract.Data.MIMETYPE + "=? OR " + ContactsContract.Data.MIMETYPE + " =?",
                    new String[]{SipAddress.CONTENT_ITEM_TYPE, Im.CONTENT_ITEM_TYPE}, null);
            if (cSip != null) {
                final int iMime = cSip.getColumnIndex(ContactsContract.Data.MIMETYPE);
                final int iSip =  cSip.getColumnIndex(SipAddress.SIP_ADDRESS);
                final int iType =  cSip.getColumnIndex(SipAddress.TYPE);
                final int iLabel =  cSip.getColumnIndex(SipAddress.LABEL);
                while (cSip.moveToNext()) {
                    String mime = cSip.getString(iMime);
                    String number = cSip.getString(iSip);
                    if (mime.contentEquals(Im.CONTENT_ITEM_TYPE) && new SipUri(number).isRingId())
                        c.addNumber(number, cSip.getInt(iType), cSip.getString(iLabel), CallContact.NumberType.SIP);
                    Log.w(TAG, "SIP phone:" + number);
                }
                cSip.close();
            }
        } catch(Exception e) {
            Log.w(TAG, e);
        }
    }

    public static CallContact findByKey(@NonNull ContentResolver res, String key) {
        Log.e(TAG, "findByKey " + key);
        CallContact contact = null;
        try {
            Cursor result = res.query(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, key), CONTACT_PROJECTION,
                    null, null, null);
            if (result == null)
                return null;
            if (result.moveToFirst()) {
                int iID = result.getColumnIndex(ContactsContract.Data._ID);
                int iKey = result.getColumnIndex(ContactsContract.Data.LOOKUP_KEY);
                int iName = result.getColumnIndex(ContactsContract.Data.DISPLAY_NAME);
                int iPhoto = result.getColumnIndex(ContactsContract.Data.PHOTO_ID);
                int iStared = result.getColumnIndex(ContactsContract.Data.STARRED);
                long cid = result.getLong(iID);

                Log.w(TAG, "Contact id:" + cid + " key:" + result.getString(iKey));

                contact = new CallContact(cid, result.getString(iKey), result.getString(iName), result.getLong(iPhoto));
                if (result.getInt(iStared) != 0)
                    contact.setStared();
                lookupDetails(res, contact);
            }
            result.close();
        } catch(Exception e) {
            Log.w(TAG, e);
        }
        return contact;
    }

     public static CallContact findById(@NonNull ContentResolver res, long id) {
         Log.e(TAG, "findById " + id);
         CallContact contact = null;
         try {
             Cursor result = res.query(ContactsContract.Contacts.CONTENT_URI, CONTACT_PROJECTION,
                     ContactsContract.Contacts._ID + " = ?",
                     new String[]{String.valueOf(id)}, null);
             if (result == null)
                 return null;

             if (result.moveToFirst()) {
                 int iID = result.getColumnIndex(ContactsContract.Data._ID);
                 int iKey = result.getColumnIndex(ContactsContract.Data.LOOKUP_KEY);
                 int iName = result.getColumnIndex(ContactsContract.Data.DISPLAY_NAME);
                 int iPhoto = result.getColumnIndex(ContactsContract.Data.PHOTO_ID);
                 int iStared = result.getColumnIndex(ContactsContract.Contacts.STARRED);
                 long cid = result.getLong(iID);

                 Log.w(TAG, "Contact id:" + cid + " key:" + result.getString(iKey));

                 contact = new CallContact(cid, result.getString(iKey), result.getString(iName), result.getLong(iPhoto));
                 if (result.getInt(iStared) != 0)
                     contact.setStared();
                 lookupDetails(res, contact);
             }
             result.close();
         } catch(Exception e) {
             Log.w(TAG, e);
         }
         return contact;
    }

    public CallContact getContactById(long id) {
        if (id <= 0)
            return null;
        CallContact c = systemContactCache.get(id);
        /*if (c == null) {
            Log.w(TAG, "getContactById : cache miss for " + id);
            c = findById(getContentResolver(), id);
        }*/
        return c;
    }


    @NonNull
    public static CallContact findContactBySipNumber(@NonNull ContentResolver res, String number) {
        ArrayList<CallContact> contacts = new ArrayList<>(1);
        try {
            Cursor result = res.query(ContactsContract.Data.CONTENT_URI,
                    DATA_PROJECTION,
                    SipAddress.SIP_ADDRESS + "=?" + " AND (" + ContactsContract.Data.MIMETYPE + "=? OR " + ContactsContract.Data.MIMETYPE + "=?)",
                    new String[]{number, SipAddress.CONTENT_ITEM_TYPE, Im.CONTENT_ITEM_TYPE}, null);
            if (result == null)  {
                Log.w(TAG, "findContactBySipNumber " + number + " can't find contact.");
                return CallContact.buildUnknown(number);
            }
            int icID = result.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID);
            int iKey = result.getColumnIndex(ContactsContract.Data.LOOKUP_KEY);
            int iName = result.getColumnIndex(ContactsContract.Data.DISPLAY_NAME);
            int iPhoto = result.getColumnIndex(ContactsContract.Data.PHOTO_ID);
            int iPhotoThumb = result.getColumnIndex(ContactsContract.Data.PHOTO_THUMBNAIL_URI);
            int iStared = result.getColumnIndex(ContactsContract.Contacts.STARRED);

            while (result.moveToNext()) {
                long cid = result.getLong(icID);
                CallContact contact = new CallContact(cid, result.getString(iKey), result.getString(iName), result.getLong(iPhoto));
                if (result.getInt(iStared) != 0)
                    contact.setStared();
                lookupDetails(res, contact);
                contacts.add(contact);
            }
            result.close();
            //lookupDetails(res, contact);
        } catch(Exception e) {
            Log.w(TAG, e);
        }
        if (contacts.isEmpty()) {
            Log.w(TAG, "findContactBySipNumber " + number + " can't find contact.");
            return CallContact.buildUnknown(number);
        }
        return contacts.get(0);
    }

    @NonNull
    public static CallContact findContactByNumber(@NonNull ContentResolver res, String number) {
        //Log.w(TAG, "findContactByNumber " + number);
        CallContact c = null;
        try {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            Cursor result = res.query(uri, PHONELOOKUP_PROJECTION, null, null, null);
            if (result == null)  {
                Log.w(TAG, "findContactByNumber " + number + " can't find contact.");
                return findContactBySipNumber(res, number);
            }
            if (result.moveToFirst())  {
                int iID = result.getColumnIndex(ContactsContract.Contacts._ID);
                int iKey = result.getColumnIndex(ContactsContract.Data.LOOKUP_KEY);
                int iName = result.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                int iPhoto = result.getColumnIndex(ContactsContract.Contacts.PHOTO_ID);
                c = new CallContact(result.getLong(iID), result.getString(iKey), result.getString(iName), result.getLong(iPhoto));
                lookupDetails(res, c);
                Log.w(TAG, "findContactByNumber " + number + " found " + c.getDisplayName());
            }
            result.close();
        } catch(Exception e) {
            Log.w(TAG, e);
        }
        if (c == null) {
            Log.w(TAG, "findContactByNumber " + number + " can't find contact.");
            c = findContactBySipNumber(res, number);
        }
        return c;
    }

    private class ConversationLoader extends AsyncTask<Void, Void, Map<String, Conversation>> {
        private final ContentResolver cr;
        private final LongSparseArray<CallContact> localContactCache;

        public ConversationLoader(Context c, LongSparseArray<CallContact> cache) {
            cr = c.getContentResolver();
            localContactCache = (cache == null) ? new LongSparseArray<CallContact>(64) : cache;
        }

        private CallContact getByNumber(HashMap<String, CallContact> cache, String number) {
            if (number == null || number.isEmpty())
                return null;
            number = CallContact.canonicalNumber(number);
            CallContact c = cache.get(number);
            if (c == null) {
                c = findContactByNumber(cr, number);
                //if (c != null)
                cache.put(number, c);
            }
            return c;
        }

        Pair<HistoryEntry, HistoryCall> findHistoryByCallId(final Map<String, Conversation> confs, String id) {
            for (Conversation c : confs.values()) {
                Pair<HistoryEntry, HistoryCall> h = c.findHistoryByCallId(id);
                if (h != null)
                    return h;
            }
            return null;
        }

        @Override
        protected Map<String, Conversation> doInBackground(Void... params) {
            List<HistoryCall> history = null;
            List<HistoryText> historyTexts = null;
            Map<String, Map<String, String>> confs = null;
            final Map<String, Conversation> ret = new HashMap<>();
            final HashMap<String, CallContact> localNumberCache = new HashMap<>(64);

            try {
                history = historyManager.getAll();
                historyTexts = historyManager.getAllTextMessages();
                confs = mService.getConferenceList();

                for (HistoryCall call : history) {
                    //Log.w(TAG, "History call : " + call.getNumber() + " " + call.call_start + " " + call.call_end + " " + call.getEndDate().toString());
                    CallContact contact;
                    if (call.getContactID() <= CallContact.DEFAULT_ID) {
                        contact = getByNumber(localNumberCache, call.getNumber());
                    } else {
                        contact = localContactCache.get(call.getContactID());
                        if (contact == null) {
                            contact = findById(cr, call.getContactID());
                            if (contact != null)
                                contact.addPhoneNumber(call.getNumber());
                            else {
                                Log.w(TAG, "Can't find contact with id " + call.getContactID());
                                contact = getByNumber(localNumberCache, call.getNumber());
                            }
                            localContactCache.put(contact.getId(), contact);
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

                for (HistoryText htext : historyTexts) {
                    CallContact contact;

                    if (htext.getContactID() <= CallContact.DEFAULT_ID) {
                        contact = getByNumber(localNumberCache, htext.getNumber());
                    } else {
                        contact = localContactCache.get(htext.getContactID());
                        if (contact == null) {
                            contact = findById(cr, htext.getContactID());
                            if (contact != null)
                                contact.addPhoneNumber(htext.getNumber());
                            else {
                                Log.w(TAG, "Can't find contact with id " + htext.getContactID());
                                contact = getByNumber(localNumberCache, htext.getNumber());
                            }
                            localContactCache.put(contact.getId(), contact);
                        }
                    }

                    Pair<HistoryEntry, HistoryCall> p = findHistoryByCallId(ret, htext.getCallId());

                    if (contact == null && p != null)
                        contact = p.first.getContact();
                    if (contact == null)
                        continue;

                    TextMessage msg = new TextMessage(htext);
                    msg.setContact(contact);

                    if (p  != null) {
                        if (msg.getNumber() == null || msg.getNumber().isEmpty())
                            msg.setNumber(p.second.getNumber());
                        p.first.addTextMessage(msg);
                    }

                    String key = contact.getIds().get(0);
                    if (ret.containsKey(key)) {
                        ret.get(key).addTextMessage(msg);
                    } else {
                        Conversation c = new Conversation(contact);
                        c.addTextMessage(msg);
                        ret.put(key, c);
                    }
                }

                for (Map.Entry<String, Map<String, String>> c : confs.entrySet()) {
                    String conf_id = c.getKey();
                    Map<String, String> conf_details = c.getValue();
                    List<String> callsIds = mService.getParticipantList(conf_id);
                    Conference conf = new Conference(conf_id);
                    for (String callId : callsIds) {
                        Map<String, String> call_details = mService.getCallDetails(callId);
                        SipCall call = new SipCall(callId, call_details);
                        Account acc = getAccount(call.getAccount());
                        if(acc.isRing()
                                || acc.getSrtpDetails().getDetailBoolean(AccountDetailSrtp.CONFIG_SRTP_ENABLE)
                                || acc.getTlsDetails().getDetailBoolean(AccountDetailTls.CONFIG_TLS_ENABLE)) {
                            Log.i(TAG, "DRingService.placeCall() call is secure");
                            call = new SecureSipCall(call, acc.getSrtpDetails().getDetailString(AccountDetailSrtp.CONFIG_SRTP_KEY_EXCHANGE));
                        }
                        conf.addParticipant(call);
                    }
                    List<SipCall> calls = conf.getParticipants();
                    if (calls.size() == 1) {
                        SipCall call = calls.get(0);
                        Conversation conv = null;
                        String number = call.getNumber();
                        CallContact contact = findContactByNumber(number);
                        ArrayList<String> ids = contact.getIds();
                        for (String id : ids) {
                            //Log.w(TAG, "    uri attempt : " + id);
                            conv = ret.get(id);
                            if (conv != null) break;
                        }
                        if (conv != null) {
                            conv.addConference(conf);
                        } else {
                            conv = new Conversation(contact);
                            conv.addConference(conf);
                            ret.put(ids.get(0), conv);
                        }
                    }
                }
                for (Conversation c : ret.values())
                    Log.w(TAG, "Conversation : " + c.getContact().getId() + " " + c.getContact().getDisplayName() + " " + c.getContact().getPhones().get(0).getNumber() + " " + c.getLastInteraction().toString());
            } catch (RemoteException | SQLException e) {
                e.printStackTrace();
            }
            return ret;
        }
    }

    private void updated(Map<String, Conversation> res) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        for (Conversation conv : conversations.values()) {
            for (Conference c : conv.current_calls) {
                notificationManager.cancel(c.notificationId);
            }
        }
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
                /*for (Map.Entry<String, String> entry : State.entrySet()) {
                    Log.i(TAG, "State:" + entry.getKey() + " -> " + entry.getValue());
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

    private void updateConnectivityState() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo ni = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        Log.w(TAG, "ActiveNetworkInfo (Wifi): " + (ni == null ? "null" : ni.toString()));
        isWifiConn = ni != null && ni.isConnected();

        ni = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        Log.w(TAG, "ActiveNetworkInfo (mobile): " + (ni == null ? "null" : ni.toString()));
        isMobileConn = ni != null && ni.isConnected();

        try {
            getRemoteService().setAccountsActive(isWifiConn);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        // if account list loaded
        if (!ip2ip_account.isEmpty())
            sendBroadcast(new Intent(ACTION_ACCOUNT_UPDATE));
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w(TAG, "BroadcastReceiver onReceive " + intent.getAction());
            switch(intent.getAction()) {
                case DRingService.DRING_CONNECTION_CHANGED: {
                    boolean connected = intent.getBooleanExtra("connected", false);
                    if (connected) {
                        mAccountLoader.onContentChanged();
                        mAccountLoader.startLoading();
                        mAccountLoader.forceLoad();
                    } else {
                        Log.w(TAG, "DRing connection lost ");
                    }
                    break;
                }
                case ConnectivityManager.CONNECTIVITY_ACTION:
                    Log.w(TAG, "ConnectivityManager.CONNECTIVITY_ACTION " + " " + intent.getStringExtra(ConnectivityManager.EXTRA_EXTRA_INFO) + " " + intent.getStringExtra(ConnectivityManager.EXTRA_EXTRA_INFO));
                    updateConnectivityState();
                    break;
                case ConfigurationManagerCallback.ACCOUNT_STATE_CHANGED:
                    Log.w(TAG, "Received " + intent.getAction() + " " + intent.getStringExtra("account") + " " + intent.getStringExtra("state") + " " + intent.getIntExtra("code", 0));
                    //accountStateChanged(intent.getStringExtra("Account"), intent.getStringExtra("State"), intent.getIntExtra("code", 0));
                    for (Account a : accounts) {
                        if (a.getAccountID().contentEquals(intent.getStringExtra("account"))) {
                            a.setRegistrationState(intent.getStringExtra("state"), intent.getIntExtra("code", 0));
                            //notifyDataSetChanged();
                            sendBroadcast(new Intent(ACTION_ACCOUNT_UPDATE));
                            break;
                        }
                    }
                    break;
                case ConfigurationManagerCallback.ACCOUNTS_CHANGED:
                    //accountsChanged();
                    mAccountLoader.onContentChanged();
                    mAccountLoader.startLoading();
                    mAccountLoader.forceLoad();
                    break;
                case CallManagerCallBack.INCOMING_TEXT:
                case ConfigurationManagerCallback.INCOMING_TEXT: {
                    TextMessage txt = intent.getParcelableExtra("txt");
                    String call = txt.getCallId();
                    Conversation conv;
                    if (call != null && !call.isEmpty()) {
                        conv = getConversationByCallId(call);
                        conv.addTextMessage(txt);
                    } else {
                        conv = startConversation(findContactByNumber(txt.getNumber()));
                        txt.setContact(conv.getContact());
                        Log.w(TAG, "New text messsage " + txt.getAccount() + " " + txt.getContact().getId() + " " + txt.getMessage());
                        conv.addTextMessage(txt);
                    }
                    sendBroadcast(new Intent(ACTION_CONF_UPDATE));
                    break;
                }
                case CallManagerCallBack.INCOMING_CALL: {
                    String callId = intent.getStringExtra("call");
                    String accountId = intent.getStringExtra("account");
                    String number = intent.getStringExtra("from");
                    CallContact contact = findContactByNumber(number);
                    Conversation conv = startConversation(contact);

                    SipCall call = new SipCall(callId, accountId, number, SipCall.Direction.INCOMING);
                    call.setContact(contact);

                    Account account = getAccount(accountId);

                    Conference toAdd;
                    if (account.useSecureLayer()) {
                        SecureSipCall secureCall = new SecureSipCall(call, account.getSrtpDetails().getDetailString(AccountDetailSrtp.CONFIG_SRTP_KEY_EXCHANGE));
                        toAdd = new Conference(secureCall);
                    } else {
                        toAdd = new Conference(call);
                    }

                    conv.addConference(toAdd);
                    toAdd.showCallNotification(LocalService.this);
                    break;
                }
                case CallManagerCallBack.CALL_STATE_CHANGED: {
                    String callid = intent.getStringExtra("call");
                    Conversation conversation = null;
                    Conference found = null;

                    for (Conversation conv : conversations.values()) {
                        Conference tconf = conv.getConference(callid);
                        if (tconf != null) {
                            conversation = conv;
                            found = tconf;
                            break;
                        }
                    }

                    if (found == null) {
                        Log.w(TAG, "CALL_STATE_CHANGED : Can't find conference " + callid);
                    } else {
                        SipCall call = found.getCallById(callid);
                        int old_state = call.getCallState();
                        int new_state = SipCall.stateFromString(intent.getStringExtra("state"));

                        if (new_state != old_state) {
                            Log.w(TAG, "CALL_STATE_CHANGED : updating call state to " + new_state);
                            call.setCallState(new_state);
                        }
                        if (new_state == SipCall.State.HUNGUP
                                || new_state == SipCall.State.BUSY
                                || new_state == SipCall.State.FAILURE
                                || new_state == SipCall.State.INACTIVE) {
                            Log.w(TAG, "Removing call and notification " + found.getId());
                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(LocalService.this);
                            notificationManager.cancel(found.notificationId);
                            found.removeParticipant(call);
                        } else {
                            found.showCallNotification(LocalService.this);
                        }
                        if (new_state == SipCall.State.HUNGUP) {
                            call.setTimestampEnd(System.currentTimeMillis());
                            conversation.addHistoryCall(new HistoryCall(call));
                        }
                        if (found.getParticipants().isEmpty()) {
                            conversation.removeConference(found);
                        }
                    }
                    sendBroadcast(new Intent(ACTION_CONF_UPDATE));
                    break;
                }
                default:
                    new ConversationLoader(context, systemContactCache){
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
        new ConversationLoader(this, systemContactCache){
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

        intentFilter.addAction(DRingService.DRING_CONNECTION_CHANGED);

        intentFilter.addAction(ConfigurationManagerCallback.ACCOUNT_STATE_CHANGED);
        intentFilter.addAction(ConfigurationManagerCallback.ACCOUNTS_CHANGED);
        intentFilter.addAction(ConfigurationManagerCallback.INCOMING_TEXT);

        intentFilter.addAction(CallManagerCallBack.INCOMING_CALL);
        intentFilter.addAction(CallManagerCallBack.INCOMING_TEXT);
        intentFilter.addAction(CallManagerCallBack.CALL_STATE_CHANGED);
        intentFilter.addAction(CallManagerCallBack.CONF_CREATED);
        intentFilter.addAction(CallManagerCallBack.CONF_CHANGED);
        intentFilter.addAction(CallManagerCallBack.CONF_REMOVED);

        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        registerReceiver(receiver, intentFilter);

        getContentResolver().registerContentObserver(Contacts.People.CONTENT_URI, true, contactContentObserver);
    }

    private class ContactsContentObserver extends ContentObserver {

        public ContactsContentObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.w(TAG, "ContactsContentObserver.onChange");
            super.onChange(selfChange);
            mSystemContactLoader.onContentChanged();
            mSystemContactLoader.startLoading();
        }
    }

    public void stopListener() {
        unregisterReceiver(receiver);
        getContentResolver().unregisterContentObserver(contactContentObserver);
    }

}
