/*
 *  Copyright (C) 2015-2016 Savoir-faire Linux Inc.
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package cx.ring.service;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.LruCache;
import android.util.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cx.ring.BuildConfig;
import cx.ring.R;
import cx.ring.client.ConversationActivity;
import cx.ring.history.HistoryCall;
import cx.ring.history.HistoryEntry;
import cx.ring.history.HistoryManager;
import cx.ring.history.HistoryText;
import cx.ring.loaders.AccountsLoader;
import cx.ring.loaders.ContactsLoader;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.SecureSipCall;
import cx.ring.model.SipCall;
import cx.ring.model.SipUri;
import cx.ring.model.TextMessage;
import cx.ring.model.account.Account;
import cx.ring.model.account.AccountDetailAdvanced;
import cx.ring.model.account.AccountDetailSrtp;
import cx.ring.model.account.AccountDetailTls;
import cx.ring.utils.MediaManager;

public class LocalService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener
{
    static final String TAG = LocalService.class.getSimpleName();

    // Emitting events
    static public final String ACTION_CONF_UPDATE = BuildConfig.APPLICATION_ID + ".action.CONF_UPDATE";
    static public final String ACTION_CONF_LOADED = BuildConfig.APPLICATION_ID + ".action.CONF_LOADED";
    static public final String ACTION_ACCOUNT_UPDATE = BuildConfig.APPLICATION_ID + ".action.ACCOUNT_UPDATE";
    static public final String ACTION_CONV_READ = BuildConfig.APPLICATION_ID + ".action.CONV_READ";

    static public final String ACTION_CONF_UPDATE_EXTRA_MSG = ACTION_CONF_UPDATE + ".extra.message";

    // Receiving commands
    static public final String ACTION_CALL_ACCEPT = BuildConfig.APPLICATION_ID + ".action.CALL_ACCEPT";
    static public final String ACTION_CALL_REFUSE = BuildConfig.APPLICATION_ID + ".action.CALL_REFUSE";
    static public final String ACTION_CALL_END = BuildConfig.APPLICATION_ID + ".action.CALL_END";


    public static final Uri AUTHORITY_URI = Uri.parse("content://" + BuildConfig.APPLICATION_ID);
    public static final int PERMISSIONS_REQUEST = 57;

    public final static String[] REQUIRED_RUNTIME_PERMISSIONS = {Manifest.permission.RECORD_AUDIO};

    private IDRingService mService = null;
    private boolean dringStarted = false;

    private final ContactsContentObserver contactContentObserver = new ContactsContentObserver();

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private Map<String, Conversation> conversations = new HashMap<>();
    private LongSparseArray<TextMessage> messages = new LongSparseArray<>();

    private List<Account> accounts = new ArrayList<>();

    private HistoryManager historyManager;

    private final LongSparseArray<CallContact> systemContactCache = new LongSparseArray<>();
    private ContactsLoader.Result lastContactLoaderResult = new ContactsLoader.Result();

    private ContactsLoader mSystemContactLoader = null;
    private AccountsLoader mAccountLoader = null;

    private LruCache<Long, Bitmap> mMemoryCache = null;
    private final ExecutorService mPool = Executors.newCachedThreadPool();

    private NotificationManagerCompat notificationManager;
    private MediaManager mediaManager;

    private boolean isWifiConn = false;
    private boolean isMobileConn = false;

    private boolean canUseContacts = true;
    private boolean canUseMobile = false;

    private boolean mAreConversationsLoaded = false;

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
        return isWifiConn || (canUseMobile && isMobileConn);
    }
    public boolean isWifiConnected() {
        return isWifiConn;
    }
    public boolean isMobileNetworkConnectedButNotGranted() {
        return (!canUseMobile && isMobileConn);
    }

    public Conference placeCall(SipCall call) {
        Conference conf = null;
        CallContact contact = call.getContact();
        if (contact == null)
            contact = findContactByNumber(call.getNumberUri());
        Conversation conv = startConversation(contact);
        try {
            mService.setPreviewSettings();
            SipUri number = call.getNumberUri();
            if (number == null || number.isEmpty())
                number = contact.getPhones().get(0).getNumber();
            String callId = mService.placeCall(call.getAccount(), number.getUriString(), !call.isVideoMuted());
            if (callId == null || callId.isEmpty()) {
                return null;
            }
            call.setCallID(callId);
            Account acc = getAccount(call.getAccount());
            if(acc.isRing()
                    || acc.getSrtpDetails().getDetailBoolean(AccountDetailSrtp.CONFIG_SRTP_ENABLE)
                    || acc.getTlsDetails().getDetailBoolean(AccountDetailTls.CONFIG_TLS_ENABLE)) {
                Log.i(TAG, "placeCall() call is secure");
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

    public void sendTextMessage(String account, SipUri to, String txt) {
        try {
            long id = mService.sendAccountTextMessage(account, to.getRawUriString(), txt);
            Log.w(TAG, "sendAccountTextMessage " + txt + " got id " + id);
            TextMessage message = new TextMessage(false, txt, to, null, account);
            message.setID(id);
            message.read();
            historyManager.insertNewTextMessage(message);
            messages.put(id, message);
            textMessageSent(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void sendTextMessage(Conference conf, String txt) {
        try {
            mService.sendTextMessage(conf.getId(), txt);
            SipCall call = conf.getParticipants().get(0);
            TextMessage message = new TextMessage(false, txt, call.getNumberUri(), conf.getId(), call.getAccount());
            message.read();
            historyManager.insertNewTextMessage(message);
            textMessageSent(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void readTextMessage(TextMessage message) {
        message.read();
        HistoryText ht = new HistoryText(message);
        historyManager.updateTextMessage(ht);
    }

    public void readConversation(Conversation conv) {
        for (HistoryEntry h : conv.getRawHistory().values()) {
            NavigableMap<Long, TextMessage> messages = h.getTextMessages();
            for (TextMessage msg : messages.descendingMap().values()) {
                if (msg.isRead())
                    break;
                readTextMessage(msg);
            }
        }
        notificationManager.cancel(conv.notificationId);
        updateTextNotifications();
    }

    private Conversation conversationFromMessage(TextMessage txt) {
        Conversation conv;
        String call = txt.getCallId();
        if (call != null && !call.isEmpty()) {
            conv = getConversationByCallId(call);
        } else {
            conv = startConversation(findContactByNumber(txt.getNumberUri()));
            txt.setContact(conv.getContact());
        }
        return conv;
    }

    private void textMessageSent(TextMessage txt) {
        Log.d(TAG, "Sent text messsage " + txt.getAccount() + " " + txt.getCallId() + " " + txt.getNumberUri() + " " + txt.getMessage());
        Conversation conv = conversationFromMessage(txt);
        conv.addTextMessage(txt);
        if (conv.mVisible)
            txt.read();
        else
            updateTextNotifications();
        sendBroadcast(new Intent(ACTION_CONF_UPDATE));
    }

    public void refreshConversations() {
        Log.d(TAG, "refreshConversations()");
        new ConversationLoader(getApplicationContext().getContentResolver(), systemContactCache){
            @Override
            protected void onPostExecute(Map<String, Conversation> res) {
                updated(res);
            }
        }.execute();
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
        Log.d(TAG, "onCreate");
        super.onCreate();

        mediaManager = new MediaManager(this);

        notificationManager = NotificationManagerCompat.from(this);
        // Clear any notifications from a previous app instance
        notificationManager.cancelAll();

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<Long, Bitmap>(cacheSize){
            @Override
            protected int sizeOf(Long key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        historyManager = new HistoryManager(this);
        startListener();
        Intent intent = new Intent(this, DRingService.class);
        startService(intent);
        bindService(intent, mConnection, BIND_AUTO_CREATE | BIND_IMPORTANT | BIND_ABOVE_CLIENT);

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        isWifiConn = ni != null && ni.isConnected();
        ni = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        isMobileConn = ni != null && ni.isConnected();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        canUseContacts = sharedPreferences.getBoolean(getString(R.string.pref_systemContacts_key), true);
        canUseMobile = sharedPreferences.getBoolean(getString(R.string.pref_mobileData_key), false);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
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
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
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
            accounts = data;
            mAccountLoader.stopLoading();
            boolean haveSipAccount = false;
            boolean haveRingAccount = false;
            for (Account acc : accounts) {
                //~ Sipinfo is forced for any sipaccount since overrtp is not supported yet.
                //~ This will have to be removed when it will be supported.
                Log.d(TAG, "Settings SIP DTMF type to sipinfo");
                acc.getAdvancedDetails().setDetailString(
                        AccountDetailAdvanced.CONFIG_ACCOUNT_DTMF_TYPE,
                        getString(R.string.account_sip_dtmf_type_sipinfo)
                );

                try {
                    final IDRingService remote = getRemoteService();
                    remote.setAccountDetails(acc.getAccountID(),acc.getDetails());
                }
                catch (android.os.RemoteException exception) {
                    exception.printStackTrace();
                }

                if (!acc.isEnabled())
                    continue;
                if (acc.isSip()) {
                    haveSipAccount = true;
                }
                else if (acc.isRing())
                    haveRingAccount = true;
            }

            mSystemContactLoader.loadRingContacts = haveRingAccount;
            mSystemContactLoader.loadSipContacts = haveSipAccount;

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(LocalService.this);
            sharedPreferences.edit()
                    .putBoolean(OutgoingCallHandler.KEY_CACHE_HAVE_RINGACCOUNT, haveRingAccount)
                    .putBoolean(OutgoingCallHandler.KEY_CACHE_HAVE_SIPACCOUNT, haveSipAccount).apply();

            updateConnectivityState();
            mSystemContactLoader.startLoading();
            mSystemContactLoader.forceLoad();
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

            refreshConversations();
        }
    };

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_systemContacts_key))) {
            canUseContacts = sharedPreferences.getBoolean(key, true);
            mSystemContactLoader.onContentChanged();
            mSystemContactLoader.startLoading();
        } else if (key.equals(getString(R.string.pref_mobileData_key))) {
            canUseMobile = sharedPreferences.getBoolean(key, true);
            updateConnectivityState();
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.w(TAG, "onServiceConnected " + className.getClassName());
            mService = IDRingService.Stub.asInterface(service);
            mAccountLoader = new AccountsLoader(LocalService.this, mService);
            mAccountLoader.registerListener(1, onAccountsLoaded);
            try {
                if (mService.isStarted()) {
                    mAccountLoader.startLoading();
                    mAccountLoader.onContentChanged();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            mSystemContactLoader = new ContactsLoader(LocalService.this);
            mSystemContactLoader.registerListener(1, onSystemContactsLoaded);
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null && mService != null)
            receiver.onReceive(this, intent);
        return super.onStartCommand(intent, flags, startId);
    }

    public static boolean checkPermission(Context c, String permission) {
        return ContextCompat.checkSelfPermission(c, permission) == PackageManager.PERMISSION_GRANTED;
    }

    @NonNull
    public static String[] checkRequiredPermissions(Context c) {
        ArrayList<String> perms = new ArrayList<>();
        for (String p : REQUIRED_RUNTIME_PERMISSIONS) {
            if (!checkPermission(c, p))
                perms.add(p);
        }
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(c);
        boolean contact_perm = sharedPref.getBoolean(c.getString(R.string.pref_systemContacts_key), true);
        if (contact_perm && !checkPermission(c, Manifest.permission.READ_CONTACTS))
            perms.add(Manifest.permission.READ_CONTACTS);
        boolean camera_perm = sharedPref.getBoolean(c.getString(R.string.pref_systemCamera_key), true);
        if (camera_perm && !checkPermission(c, Manifest.permission.CAMERA))
            perms.add(Manifest.permission.CAMERA);
        boolean sys_dialer = sharedPref.getBoolean(c.getString(R.string.pref_systemDialer_key), false);
        if (sys_dialer && !checkPermission(c, Manifest.permission.WRITE_CALL_LOG))
            perms.add(Manifest.permission.WRITE_CALL_LOG);
        return perms.toArray(new String[perms.size()]);
    }

    public IDRingService getRemoteService() {
        return mService;
    }

    public List<Account> getAccounts() { return accounts; }
    public Account getAccount(String account_id) {
        if (account_id == null || account_id.isEmpty())
            return null;
        for (Account acc : accounts)
            if (acc.getAccountID().equals(account_id))
                return acc;
        return null;
    }
    public void setAccountOrder(List<String> accountOrder) {
        ArrayList<Account> newlist = new ArrayList<>(accounts.size());
        String order = "";
        for (String acc_id : accountOrder) {
            Account acc = getAccount(acc_id);
            if (acc != null)
                newlist.add(acc);
            order += acc_id + File.separator;
        }
        accounts = newlist;
        try {
            mService.setAccountOrder(order);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        sendBroadcast(new Intent(ACTION_ACCOUNT_UPDATE));
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

    public Pair<Conference, SipCall> getCall(String id) {
        for (Conversation conv : conversations.values()) {
            ArrayList<Conference> confs = conv.getCurrentCalls();
            for (Conference c : confs) {
                SipCall call = c.getCallById(id);
                if (call != null)
                    return new Pair<>(c, call);
            }
        }
        return new Pair<>(null, null);
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
            contact = findContactByNumber(contact.getPhones().get(0).getNumber());
        Conversation c = getByContact(contact);
        if (c == null) {
            c = new Conversation(contact);
            conversations.put(contact.getIds().get(0), c);
        }
        return c;
    }

    public CallContact findContactByNumber(SipUri number) {
        for (Conversation conv : conversations.values()) {
            if (conv.contact.hasNumber(number))
                return conv.contact;
        }
        return canUseContacts ? findContactByNumber(getContentResolver(), number.getRawUriString()) : CallContact.buildUnknown(number);
    }

    public Conversation findConversationByNumber(SipUri number) {
        if (number == null || number.isEmpty())
            return null;
        for (Conversation conv : conversations.values()) {
            if (conv.contact.hasNumber(number))
                return conv;
        }
        return startConversation(canUseContacts ? findContactByNumber(getContentResolver(), number.getRawUriString()) : CallContact.buildUnknown(number));
    }

    public CallContact findContactById(long id) {
        if (id <= 0)
            return null;
        CallContact c = systemContactCache.get(id);
        if (c == null) {
            Log.w(TAG, "getContactById : cache miss for " + id);
            c = findById(getContentResolver(), id, null);
            systemContactCache.put(id, c);
        }
        return c;
    }

    public Account guessAccount(SipUri uri) {
        if (uri.isRingId()) {
            for (Account a : accounts)
                if (a.isRing())
                    return a;
            // ring ids must be called with ring accounts
            return null;
        }
        for (Account a : accounts)
            if (a.isSip() && a.getHost().equals(uri.host))
                return a;
        if (uri.isSingleIp()) {
            for (Account a : accounts)
                if (a.isIP2IP())
                    return a;
        }
        return accounts.get(0);
    }

    public void clearHistory() {
        historyManager.clearDB();
        refreshConversations();
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
        //Log.w(TAG, "lookupDetails " + c.getKey());
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
                    if (!mime.contentEquals(Im.CONTENT_ITEM_TYPE) || new SipUri(number).isRingId() || "ring".equalsIgnoreCase(cSip.getString(iLabel)))
                        c.addNumber(number, cSip.getInt(iType), cSip.getString(iLabel), CallContact.NumberType.SIP);
                    Log.w(TAG, "SIP phone:" + number + " " + mime + " ");
                }
                cSip.close();
            }
        } catch(Exception e) {
            Log.w(TAG, e);
        }
    }

    public static CallContact findById(@NonNull ContentResolver res, long id, String key) {
        CallContact contact = null;
        try {
            Uri contentUri;
            if (key != null)
                contentUri = ContactsContract.Contacts.lookupContact(res, ContactsContract.Contacts.getLookupUri(id, key));
            else
                contentUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);

            Cursor result = res.query(contentUri, CONTACT_PROJECTION, null, null, null);
            if (result == null)
                return null;

            if (result.moveToFirst()) {
                int iID = result.getColumnIndex(ContactsContract.Data._ID);
                int iKey = result.getColumnIndex(ContactsContract.Data.LOOKUP_KEY);
                int iName = result.getColumnIndex(ContactsContract.Data.DISPLAY_NAME);
                int iPhoto = result.getColumnIndex(ContactsContract.Data.PHOTO_ID);
                int iStared = result.getColumnIndex(ContactsContract.Contacts.STARRED);
                long cid = result.getLong(iID);

                Log.w(TAG, "Contact name: " + result.getString(iName) + " id:" + cid + " key:" + result.getString(iKey));

                contact = new CallContact(cid, result.getString(iKey), result.getString(iName), result.getLong(iPhoto));
                if (result.getInt(iStared) != 0)
                    contact.setStared();
                lookupDetails(res, contact);
            }
            result.close();
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        if (contact == null)
            Log.w(TAG, "findById " + id + " can't find contact.");
        return contact;
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
        if (contacts.isEmpty() || contacts.get(0).getPhones().isEmpty()) {
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
        private final HashMap<String, CallContact> localNumberCache = new HashMap<>(64);

        public ConversationLoader(ContentResolver c, LongSparseArray<CallContact> cache) {
            cr = c;
            localContactCache = (cache == null) ? new LongSparseArray<CallContact>(64) : cache;
        }

        private CallContact getByNumber(HashMap<String, CallContact> cache, String number) {
            if (number == null || number.isEmpty())
                return null;
            number = CallContact.canonicalNumber(number);
            CallContact c = cache.get(number);
            if (c == null) {
                c = canUseContacts ? findContactByNumber(cr, number) : CallContact.buildUnknown(number);
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

        CallContact getCreateContact(long contact_id, String contact_key, String cnumber) {
            String number = CallContact.canonicalNumber(cnumber);
            //Log.w(TAG, "getCreateContact : " + cnumber + " " + number + " " + contact_id + " " + contact_key);
            CallContact contact;
            if (contact_id <= CallContact.DEFAULT_ID) {
                contact = getByNumber(localNumberCache, number);
            } else {
                contact = localContactCache.get(contact_id);
                if (contact == null) {
                    contact = canUseContacts ? findById(cr, contact_id, contact_key) : CallContact.buildUnknown(number);
                    if (contact != null)
                        contact.addPhoneNumber(cnumber);
                    else {
                        Log.w(TAG, "Can't find contact with id " + contact_id);
                        contact = getByNumber(localNumberCache, number);
                    }
                    localContactCache.put(contact.getId(), contact);
                }
            }
            return contact;
        }

        @Override
        protected Map<String, Conversation> doInBackground(Void... params) {
            final Map<String, Conversation> ret = new HashMap<>();
            try {
                final List<HistoryCall> history = historyManager.getAll();
                final List<HistoryText> historyTexts = historyManager.getAllTextMessages();
                final Map<String, ArrayList<String>> confs = mService.getConferenceList();

                for (HistoryCall call : history) {
                    //Log.w(TAG, "History call : " + call.getNumber() + " " + call.call_start + " " + call.getEndDate().toString() + " " + call.getContactID());
                    CallContact contact = getCreateContact(call.getContactID(), call.getContactKey(), call.getNumber());

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
                    //Log.w(TAG, "History text : " + htext.getNumber() + " " + htext.getDate() + " " + htext.getCallId() + " " + htext.getAccountID() + " " + htext.getMessage());
                    CallContact contact = getCreateContact(htext.getContactID(), htext.getContactKey(), htext.getNumber());
                    Pair<HistoryEntry, HistoryCall> p = findHistoryByCallId(ret, htext.getCallId());

                    if (contact == null && p != null)
                        contact = p.first.getContact();
                    if (contact == null)
                        continue;

                    TextMessage msg = new TextMessage(htext);
                    msg.setContact(contact);

                    if (p  != null) {
                        if (msg.getNumberUri() == null)
                            msg.setNumber(new SipUri(p.second.getNumber()));
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

                for (Map.Entry<String, ArrayList<String>> c : confs.entrySet()) {
                    Conference conf = new Conference(c.getKey());
                    for (String call_id : c.getValue()) {
                        SipCall call = getCall(call_id).second;
                        if (call == null)
                            call = new SipCall(call_id, mService.getCallDetails(call_id));
                        Account acc = getAccount(call.getAccount());
                        if(acc.isRing()
                                || acc.getSrtpDetails().getDetailBoolean(AccountDetailSrtp.CONFIG_SRTP_ENABLE)
                                || acc.getTlsDetails().getDetailBoolean(AccountDetailTls.CONFIG_TLS_ENABLE)) {
                            call = new SecureSipCall(call, acc.getSrtpDetails().getDetailString(AccountDetailSrtp.CONFIG_SRTP_KEY_EXCHANGE));
                        }
                        conf.addParticipant(call);
                    }
                    List<SipCall> calls = conf.getParticipants();
                    if (calls.size() == 1) {
                        SipCall call = calls.get(0);
                        CallContact contact = getCreateContact(-1, null, call.getNumber());
                        call.setContact(contact);

                        Conversation conv = null;
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
                    Log.w(TAG, "Conversation : " + c.getContact().getId() + " " + c.getContact().getDisplayName() + " " + c.getLastNumberUsed(c.getLastAccountUsed()) + " " + c.getLastInteraction().toString());
                for (int i=0; i<localContactCache.size(); i++) {
                    CallContact contact = localContactCache.valueAt(i);
                    String key = contact.getIds().get(0);
                    if (!ret.containsKey(key))
                        ret.put(key, new Conversation(contact));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ret;
        }
    }

    private void updated(Map<String, Conversation> res) {
        for (Conversation conv : conversations.values()) {
            for (Conference c : conv.current_calls) {
                notificationManager.cancel(c.notificationId);
            }
        }
        conversations = res;
        updateAudioState();
        updateTextNotifications();
        sendBroadcast(new Intent(ACTION_CONF_UPDATE));
        sendBroadcast(new Intent(ACTION_CONF_LOADED));
        this.mAreConversationsLoaded = true;
    }

    private void updateConnectivityState() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo ni = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        Log.w(TAG, "ActiveNetworkInfo (Wifi): " + (ni == null ? "null" : ni.toString()));
        isWifiConn = ni != null && ni.isConnected();

        ni = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        Log.w(TAG, "ActiveNetworkInfo (mobile): " + (ni == null ? "null" : ni.toString()));
        isMobileConn = ni != null && ni.isConnected();

        if (dringStarted) {
            try {
                getRemoteService().setAccountsActive(isConnected());
                getRemoteService().connectivityChanged();
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            Log.d(TAG, "Sending broadcast");
            sendBroadcast(new Intent(ACTION_ACCOUNT_UPDATE));
        }
    }

    public void updateTextNotifications()
    {
        Log.d(TAG, "updateTextNotifications()");

        for (Conversation c : conversations.values()) {
            TreeMap<Long, TextMessage> texts = c.getUnreadTextMessages();
            if (texts.isEmpty() || texts.lastEntry().getValue().isNotified()) {
                continue;
            } else
                notificationManager.cancel(c.notificationId);

            CallContact contact = c.getContact();
            if (c.notificationBuilder == null) {
                c.notificationBuilder = new NotificationCompat.Builder(getApplicationContext());
                c.notificationBuilder.setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(contact.getDisplayName());
            }
            NotificationCompat.Builder noti = c.notificationBuilder;
            Intent c_intent = new Intent(Intent.ACTION_VIEW)
                    .setClass(this, ConversationActivity.class)
                    .setData(Uri.withAppendedPath(ConversationActivity.CONTENT_URI, contact.getIds().get(0)));
            Intent d_intent = new Intent(ACTION_CONV_READ)
                    .setClass(this, LocalService.class)
                    .setData(Uri.withAppendedPath(ConversationActivity.CONTENT_URI, contact.getIds().get(0)));
            noti.setContentIntent(PendingIntent.getActivity(this, new Random().nextInt(), c_intent, 0))
                    .setDeleteIntent(PendingIntent.getService(this, new Random().nextInt(), d_intent, 0));

            if (contact.getPhoto() != null) {
                Resources res = getResources();
                int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
                int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
                noti.setLargeIcon(Bitmap.createScaledBitmap(contact.getPhoto(), width, height, false));
            }
            if (texts.size() == 1) {
                TextMessage txt = texts.firstEntry().getValue();
                txt.setNotified(true);
                noti.setContentText(txt.getMessage());
                noti.setStyle(null);
                noti.setWhen(txt.getTimestamp());
            } else {
                NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
                for (TextMessage s : texts.values()) {
                    inboxStyle.addLine(Html.fromHtml("<b>" + DateUtils.formatDateTime(this, s.getTimestamp(), DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL) + "</b> " + s.getMessage()));
                    s.setNotified(true);
                }
                noti.setContentText(texts.lastEntry().getValue().getMessage());
                noti.setStyle(inboxStyle);
                noti.setWhen(texts.lastEntry().getValue().getTimestamp());
            }
            notificationManager.notify(c.notificationId, noti.build());
        }
    }

    private void updateAudioState() {
        boolean current = false;
        Conference ringing = null;
        for (Conversation c : conversations.values()) {
            Conference conf = c.getCurrentCall();
            if (conf != null) {
                current = true;
                if (conf.isRinging() && conf.isIncoming()) {
                    ringing = conf;
                    break;
                }
            }
        }
        if (current)
            mediaManager.obtainAudioFocus(ringing != null);

        if (ringing != null) {
            //Log.w(TAG, "updateAudioState Ringing ");
            mediaManager.audioManager.setMode(AudioManager.MODE_RINGTONE);
            mediaManager.startRing(null);
        } else if (current) {
            //Log.w(TAG, "updateAudioState communication ");
            mediaManager.stopRing();
            mediaManager.audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        } else {
            //Log.w(TAG, "updateAudioState normal ");
            mediaManager.stopRing();
            mediaManager.abandonAudioFocus();
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w(TAG, "BroadcastReceiver onReceive " + intent.getAction());
            switch(intent.getAction()) {
                case DRingService.DRING_CONNECTION_CHANGED: {
                    boolean connected = intent.getBooleanExtra("connected", false);
                    if (connected) {
                        dringStarted = true;
                        if (mService != null && mAccountLoader != null) {
                            mAccountLoader.startLoading();
                            mAccountLoader.onContentChanged();
                        }
                    } else {
                        Log.w(TAG, "DRing connection lost ");
                        dringStarted = false;
                    }
                    break;
                }
                case ACTION_CONV_READ: {
                    String convId = intent.getData().getLastPathSegment();
                    Conversation conversation = getConversation(convId);
                    if (conversation != null) {
                        readConversation(conversation);
                    }
                    sendBroadcast(new Intent(ACTION_CONF_UPDATE).setData(Uri.withAppendedPath(ConversationActivity.CONTENT_URI, convId)));
                    break;
                }
                case ACTION_CALL_ACCEPT: {
                    String callId = intent.getData().getLastPathSegment();
                    try {
                        mService.accept(callId);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    updateAudioState();
                    Conference conf = getConference(callId);
                    if (conf != null && !conf.mVisible)
                        startActivity(conf.getViewIntent(LocalService.this).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    break;
                }
                case ACTION_CALL_REFUSE: {
                    String call_id = intent.getData().getLastPathSegment();
                    try {
                        mService.refuse(call_id);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    updateAudioState();
                    break;
                }
                case ACTION_CALL_END: {
                    String call_id = intent.getData().getLastPathSegment();
                    try {
                        mService.hangUp(call_id);
                        mService.hangUpConference(call_id);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    updateAudioState();
                    break;
                }
                case ConnectivityManager.CONNECTIVITY_ACTION:
                    Log.w(TAG, "ConnectivityManager.CONNECTIVITY_ACTION " + " " + intent.getStringExtra(ConnectivityManager.EXTRA_EXTRA_INFO) + " " + intent.getStringExtra(ConnectivityManager.EXTRA_EXTRA_INFO));
                    updateConnectivityState();
                    break;
                case ConfigurationManagerCallback.ACCOUNT_STATE_CHANGED:
                    Log.w(TAG, "Received " + intent.getAction() + " " + intent.getStringExtra("account") + " " + intent.getStringExtra("state") + " " + intent.getIntExtra("code", 0));
                    if (mAccountLoader != null && mAccountLoader.isStarted()) {
                        mAccountLoader.cancelLoad();
                        mAccountLoader.stopLoading();
                        mAccountLoader.startLoading();
                        mAccountLoader.onContentChanged();
                    } else {
                        for (Account a : accounts) {
                            if (a.getAccountID().contentEquals(intent.getStringExtra("account"))) {
                                a.setRegistrationState(intent.getStringExtra("state"), intent.getIntExtra("code", 0));
                                sendBroadcast(new Intent(ACTION_ACCOUNT_UPDATE));
                                break;
                            }
                        }
                    }
                    break;
                case ConfigurationManagerCallback.ACCOUNTS_CHANGED:
                    if (mAccountLoader != null) {
                        mAccountLoader.startLoading();
                        mAccountLoader.onContentChanged();
                    }
                    break;
                case CallManagerCallBack.INCOMING_TEXT:
                case ConfigurationManagerCallback.INCOMING_TEXT: {
                    String message = intent.getStringExtra("txt");
                    String number = intent.getStringExtra("from");
                    String call = intent.getStringExtra("call");
                    String account = intent.getStringExtra("account");
                    TextMessage txt = new TextMessage(true, message, new SipUri(number), call, account);
                    Log.w(TAG, "New text messsage " + txt.getAccount() + " " + txt.getCallId() + " " + txt.getMessage());

                    Conversation conv;
                    if (call != null && !call.isEmpty()) {
                        conv = getConversationByCallId(call);
                    } else {
                        conv = startConversation(findContactByNumber(txt.getNumberUri()));
                        txt.setContact(conv.getContact());
                    }
                    if (conv.mVisible)
                        txt.read();
                    historyManager.insertNewTextMessage(txt);

                    conv.addTextMessage(txt);
                    if (!conv.mVisible)
                        updateTextNotifications();

                    sendBroadcast(new Intent(ACTION_CONF_UPDATE));
                    break;
                }
                case ConfigurationManagerCallback.MESSAGE_STATE_CHANGED: {
                    long id = intent.getLongExtra(ConfigurationManagerCallback.MESSAGE_STATE_CHANGED_EXTRA_ID, 0);
                    int status = intent.getIntExtra(
                            ConfigurationManagerCallback.MESSAGE_STATE_CHANGED_EXTRA_STATUS,
                            TextMessage.Status.UNKNOWN.toInt()
                    );
                    TextMessage msg = messages.get(id);
                    if (msg != null) {
                        Log.w(TAG, "Message status changed " + id + " " + status);
                        msg.setStatus(status);
                        sendBroadcast(new Intent(ACTION_CONF_UPDATE).
                                putExtra(ACTION_CONF_UPDATE_EXTRA_MSG, id)
                        );
                    }
                    break;
                }
                case CallManagerCallBack.INCOMING_CALL: {
                    String callId = intent.getStringExtra("call");
                    String accountId = intent.getStringExtra("account");
                    SipUri number = new SipUri(intent.getStringExtra("from"));
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
                    updateAudioState();

                    try {
                        mService.setPreviewSettings();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    sendBroadcast(new Intent(ACTION_CONF_UPDATE).setData(Uri.withAppendedPath(SipCall.CONTENT_URI, callId)));
                    break;
                }
                case CallManagerCallBack.CALL_STATE_CHANGED: {
                    String call_id = intent.getStringExtra("call");
                    Conversation conversation = null;
                    Conference found = null;

                    for (Conversation conv : conversations.values()) {
                        Conference tconf = conv.getConference(call_id);
                        if (tconf != null) {
                            conversation = conv;
                            found = tconf;
                            break;
                        }
                    }

                    if (found == null) {
                        Log.w(TAG, "CALL_STATE_CHANGED : Can't find conference " + call_id);
                    } else {
                        SipCall call = found.getCallById(call_id);
                        int old_state = call.getCallState();
                        int new_state = SipCall.stateFromString(intent.getStringExtra("state"));

                        Log.w(TAG, "Call state change for " + call_id + " : " + SipCall.stateToString(old_state) + " -> " + SipCall.stateToString(new_state));

                        if (new_state != old_state) {
                            Log.w(TAG, "CALL_STATE_CHANGED : updating call state to " + new_state);
                            if ((call.isRinging() || new_state == SipCall.State.CURRENT) && call.getTimestampStart() == 0) {
                                call.setTimestampStart(System.currentTimeMillis());
                            }
                            call.setCallState(new_state);
                        }

                        try {
                            call.setDetails((HashMap<String, String>) intent.getSerializableExtra("details"));
                        } catch (Exception e) {
                            Log.w(TAG, "Can't set call details.", e);
                        }

                        if (new_state == SipCall.State.HUNGUP
                                || new_state == SipCall.State.BUSY
                                || new_state == SipCall.State.FAILURE
                                || new_state == SipCall.State.INACTIVE
                                || new_state == SipCall.State.OVER) {
                            if (new_state == SipCall.State.HUNGUP) {
                                call.setTimestampEnd(System.currentTimeMillis());
                            }
                            historyManager.insertNewEntry(found);
                            conversation.addHistoryCall(new HistoryCall(call));
                            notificationManager.cancel(found.notificationId);
                            found.removeParticipant(call);
                        } else {
                            found.showCallNotification(LocalService.this);
                        }
                        if (new_state == SipCall.State.FAILURE || new_state == SipCall.State.BUSY) {
                            try {
                                mService.hangUp(call_id);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                        if (found.getParticipants().isEmpty()) {
                            conversation.removeConference(found);
                        }
                    }
                    updateAudioState();
                    sendBroadcast(new Intent(ACTION_CONF_UPDATE));
                    break;
                }
                default:
                    refreshConversations();
            }
        }
    };

    public void startListener() {
        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(ACTION_CONV_READ);

        intentFilter.addAction(DRingService.DRING_CONNECTION_CHANGED);

        intentFilter.addAction(ConfigurationManagerCallback.ACCOUNT_STATE_CHANGED);
        intentFilter.addAction(ConfigurationManagerCallback.ACCOUNTS_CHANGED);
        intentFilter.addAction(ConfigurationManagerCallback.INCOMING_TEXT);
        intentFilter.addAction(ConfigurationManagerCallback.MESSAGE_STATE_CHANGED);

        intentFilter.addAction(CallManagerCallBack.INCOMING_CALL);
        intentFilter.addAction(CallManagerCallBack.INCOMING_TEXT);
        intentFilter.addAction(CallManagerCallBack.CALL_STATE_CHANGED);
        intentFilter.addAction(CallManagerCallBack.CONF_CREATED);
        intentFilter.addAction(CallManagerCallBack.CONF_CHANGED);
        intentFilter.addAction(CallManagerCallBack.CONF_REMOVED);

        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        registerReceiver(receiver, intentFilter);

        getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contactContentObserver);
    }

    private class ContactsContentObserver extends ContentObserver {

        public ContactsContentObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            Log.d(TAG, "ContactsContentObserver.onChange");
            refreshContacts();
        }
    }

    public void stopListener() {
        unregisterReceiver(receiver);
        getContentResolver().unregisterContentObserver(contactContentObserver);
    }

    public boolean areConversationsLoaded() {
        return mAreConversationsLoaded;
    }

    public void refreshContacts() {
        Log.d(TAG, "refreshContacts");
        mSystemContactLoader.onContentChanged();
        mSystemContactLoader.startLoading();
    }

    public void deleteConversation(Conversation conversation) {
        historyManager.clearHistoryForConversation(conversation);
        refreshConversations();
    }
}
