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
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.LruCache;
import android.util.Pair;

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

import javax.inject.Inject;

import cx.ring.BuildConfig;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.client.ConversationActivity;
import cx.ring.loaders.ContactsLoader;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.ConfigKey;
import cx.ring.model.Conversation;
import cx.ring.model.ServiceEvent;
import cx.ring.model.HistoryCall;
import cx.ring.model.HistoryEntry;
import cx.ring.model.HistoryText;
import cx.ring.model.SecureSipCall;
import cx.ring.model.Settings;
import cx.ring.model.SipCall;
import cx.ring.model.TextMessage;
import cx.ring.model.Uri;
import cx.ring.services.AccountService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HistoryService;
import cx.ring.services.SettingsService;
import cx.ring.utils.ActionHelper;
import cx.ring.utils.BitmapUtils;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.MediaManager;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import cx.ring.utils.Tuple;

public class LocalService extends Service implements Observer<ServiceEvent> {
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

    @Inject
    HistoryService mHistoryService;

    @Inject
    SettingsService mSettingsService;

    @Inject
    AccountService mAccountService;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    private IDRingService mService = null;
    private boolean dringStarted = false;

    private final ContactsContentObserver contactContentObserver = new ContactsContentObserver();

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private Map<String, Conversation> conversations = new HashMap<>();
    private LongSparseArray<TextMessage> messages = new LongSparseArray<>();

    private final LongSparseArray<CallContact> systemContactCache = new LongSparseArray<>();
    private ContactsLoader.Result lastContactLoaderResult = new ContactsLoader.Result();

    private ContactsLoader mSystemContactLoader = null;

    private LruCache<Long, Bitmap> mMemoryCache = null;
    private final ExecutorService mPool = Executors.newCachedThreadPool();

    private NotificationManagerCompat notificationManager;
    private MediaManager mediaManager;

    private boolean isWifiConn = false;
    private boolean isMobileConn = false;

    private boolean canUseContacts = true;
    private boolean canUseMobile = false;

    private boolean mAreConversationsLoaded = false;
    private NotificationCompat.Builder mMessageNotificationBuilder;

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
        if (contact == null) {
            contact = findContactByNumber(call.getNumberUri());
        }
        Conversation conv = startConversation(contact);
        try {
            mService.setPreviewSettings();
            Uri number = call.getNumberUri();
            if (number == null || number.isEmpty()) {
                number = contact.getPhones().get(0).getNumber();
            }
            String callId = mService.placeCall(call.getAccount(), number.getUriString(), !call.isVideoMuted());
            if (callId == null || callId.isEmpty()) {
                return null;
            }
            call.setCallID(callId);
            Account account = mAccountService.getAccount(call.getAccount());
            if (account.isRing()
                    || account.getDetailBoolean(ConfigKey.SRTP_ENABLE)
                    || account.getDetailBoolean(ConfigKey.TLS_ENABLE)) {
                Log.i(TAG, "placeCall() call is secure");
                SecureSipCall secureCall = new SecureSipCall(call, account.getDetail(ConfigKey.SRTP_KEY_EXCHANGE));
                conf = new Conference(secureCall);
            } else {
                conf = new Conference(call);
            }
            conf.getParticipants().get(0).setContact(contact);
            conv.addConference(conf);
        } catch (RemoteException e) {
            Log.e(TAG, "placeCall", e);
        }
        return conf;
    }

    public void sendTextMessage(String account, Uri to, String txt) {
        try {
            long id = mService.sendAccountTextMessage(account, to.getRawUriString(), txt);
            Log.i(TAG, "sendAccountTextMessage " + txt + " got id " + id);
            TextMessage message = new TextMessage(false, txt, to, null, account);
            message.setID(id);
            message.read();
            mHistoryService.insertNewTextMessage(message);
            messages.put(id, message);
            textMessageSent(message);
        } catch (RemoteException e) {
            Log.e(TAG, "sendTextMessage", e);
        }
    }

    public void sendTextMessage(Conference conf, String txt) {
        try {
            mService.sendTextMessage(conf.getId(), txt);
            SipCall call = conf.getParticipants().get(0);
            TextMessage message = new TextMessage(false, txt, call.getNumberUri(), conf.getId(), call.getAccount());
            message.read();
            mHistoryService.insertNewTextMessage(message);
            textMessageSent(message);
        } catch (RemoteException e) {
            Log.e(TAG, "sendTextMessage", e);
        }
    }

    private void readTextMessage(TextMessage message) {
        message.read();
        HistoryText ht = new HistoryText(message);
        mHistoryService.updateTextMessage(ht);
    }

    public void readConversation(Conversation conv) {
        for (HistoryEntry h : conv.getRawHistory().values()) {
            NavigableMap<Long, TextMessage> messages = h.getTextMessages();
            for (TextMessage msg : messages.descendingMap().values()) {
                if (msg.isRead()) {
                    break;
                }
                readTextMessage(msg);
            }
        }
        notificationManager.cancel(conv.getUuid());
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
        if (conv.isVisible()) {
            txt.read();
        } else {
            updateTextNotifications();
        }
        sendBroadcast(new Intent(ACTION_CONF_UPDATE));
    }

    public void refreshConversations() {
        Log.d(TAG, "refreshConversations()");
        new ConversationLoader(getApplicationContext().getContentResolver(), systemContactCache) {
            @Override
            protected void onPostExecute(Map<String, Conversation> res) {
                updated(res);
            }
        }.execute();
    }

    public void reloadAccounts() {
        if (mService != null) {
            //initAccountLoader();
        } else {
            // start DRing service, reload account is part of onServiceConnected
            startDRingService();
        }
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
        mMemoryCache = new LruCache<Long, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(Long key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        // dependency injection
        ((RingApplication) getApplication()).getRingInjectionComponent().inject(this);

        // todo 
        // temporary listen for history modifications
        // When MVP/DI injection will be done, only the concerned presenters should listen
        // for model modifications
        mHistoryService.addObserver(this);
        mSettingsService.addObserver(this);
        mAccountService.addObserver(this);

        Settings settings = mSettingsService.loadSettings();
        canUseContacts = settings.isAllowSystemContacts();
        canUseMobile = settings.isAllowMobileData();

        mSystemContactLoader = new ContactsLoader(LocalService.this);
        mSystemContactLoader.setSystemContactPermission(canUseContacts && mDeviceRuntimeService.hasContactPermission());
        mSystemContactLoader.registerListener(1, onSystemContactsLoaded);

        startDRingService();

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        isWifiConn = ni != null && ni.isConnected();
        ni = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        isMobileConn = ni != null && ni.isConnected();
    }

    private void startDRingService() {
        // start Listener
        startListener();
        Intent intent = new Intent(this, DRingService.class);
        startService(intent);
        bindService(intent, mConnection, BIND_AUTO_CREATE | BIND_IMPORTANT | BIND_ABOVE_CLIENT);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMemoryCache.evictAll();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
        mHistoryService.removeObserver(this);
        mSettingsService.removeObserver(this);
        mAccountService.removeObserver(this);
        stopListener();
        mMemoryCache.evictAll();
        mPool.shutdown();
        systemContactCache.clear();
        lastContactLoaderResult = null;
    }

    private final Loader.OnLoadCompleteListener<ContactsLoader.Result> onSystemContactsLoaded = new Loader.OnLoadCompleteListener<ContactsLoader.Result>() {
        @Override
        public void onLoadComplete(Loader<ContactsLoader.Result> loader, ContactsLoader.Result data) {

            Log.d(TAG, "< Loader.OnLoadCompleteListener " + data.contacts.size() + " contacts, " + data.starred.size() + " starred.");

            lastContactLoaderResult = data;
            systemContactCache.clear();
            for (CallContact c : data.contacts) {
                systemContactCache.put(c.getId(), c);
            }

            refreshConversations();
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.w(TAG, "onServiceConnected " + className.getClassName());
            mService = IDRingService.Stub.asInterface(service);

            //initAccountLoader();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d(TAG, "onServiceDisconnected " + arg0.getClassName());

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
        Log.d(TAG, "onUnbind");
        if (mConnection != null) {
            unbindService(mConnection);
            mConnection = null;
        }
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null && mService != null) {
            receiver.onReceive(this, intent);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public IDRingService getRemoteService() {
        return mService;
    }

    public ArrayList<Conversation> getConversations() {
        ArrayList<Conversation> convs = new ArrayList<>(conversations.values());
        Collections.sort(convs, new Comparator<Conversation>() {
            @Override
            public int compare(Conversation lhs, Conversation rhs) {
                return (int) ((rhs.getLastInteraction().getTime() - lhs.getLastInteraction().getTime()) / 1000l);
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
            if (conf != null) {
                return conf;
            }
        }
        return null;
    }

    public Pair<Conference, SipCall> getCall(String id) {
        for (Conversation conv : conversations.values()) {
            ArrayList<Conference> confs = conv.getCurrentCalls();
            for (Conference c : confs) {
                SipCall call = c.getCallById(id);
                if (call != null) {
                    return new Pair<>(c, call);
                }
            }
        }
        return new Pair<>(null, null);
    }

    public Conversation getByContact(CallContact contact) {
        ArrayList<String> keys = contact.getIds();
        for (String key : keys) {
            Conversation conversation = conversations.get(key);
            if (conversation != null) {
                return conversation;
            }
        }
        Log.w(TAG, "getByContact failed");
        return null;
    }

    public Conversation getConversationByCallId(String callId) {
        for (Conversation conversation : conversations.values()) {
            Conference conf = conversation.getConference(callId);
            if (conf != null) {
                return conversation;
            }
        }
        return null;
    }

    public Conversation startConversation(CallContact contact) {
        if (contact.isUnknown()) {
            contact = findContactByNumber(contact.getPhones().get(0).getNumber());
        }
        Conversation conversation = getByContact(contact);
        if (conversation == null) {
            conversation = new Conversation(contact);
            conversations.put(contact.getIds().get(0), conversation);
        }
        return conversation;
    }

    public void updateConversationContactWithRingId(String oldId, String ringId) {

        if (TextUtils.isEmpty(oldId)) {
            return;
        }

        Uri uri = new Uri(oldId);
        if (uri.isRingId()) {
            return;
        }

        Conversation conversation = conversations.get(oldId);
        if (conversation == null) {
            return;
        }

        CallContact contact = conversation.getContact();

        if (contact == null) {
            return;
        }

        Uri ringIdUri = new Uri(ringId);
        contact.getPhones().clear();
        contact.getPhones().add(new cx.ring.model.Phone(ringIdUri, 0));
        contact.resetDisplayName();

        conversations.remove(oldId);
        conversations.put(contact.getIds().get(0), conversation);

        for (Map.Entry<String, HistoryEntry> entry : conversation.getHistory().entrySet()) {
            HistoryEntry historyEntry = entry.getValue();
            historyEntry.setContact(contact);
            NavigableMap<Long, TextMessage> messages = historyEntry.getTextMessages();
            for (TextMessage textMessage : messages.values()) {
                textMessage.setNumber(ringIdUri);
                textMessage.setContact(contact);
                mHistoryService.updateTextMessage(new HistoryText(textMessage));
            }
        }

        return;
    }

    public CallContact findContactByNumber(Uri number) {
        for (Conversation conversation : conversations.values()) {
            if (conversation.getContact().hasNumber(number)) {
                return conversation.getContact();
            }
        }
        return canUseContacts ? findContactByNumber(getContentResolver(), number.getRawUriString()) : CallContact.buildUnknown(number);
    }

    public Conversation findConversationByNumber(Uri number) {
        if (number == null || number.isEmpty()) {
            return null;
        }
        for (Conversation conversation : conversations.values()) {
            if (conversation.getContact().hasNumber(number)) {
                return conversation;
            }
        }
        return startConversation(canUseContacts ? findContactByNumber(getContentResolver(), number.getRawUriString()) : CallContact.buildUnknown(number));
    }

    public CallContact findContactById(long id) {
        if (id <= 0) {
            return null;
        }
        CallContact contact = systemContactCache.get(id);
        if (contact == null) {
            Log.w(TAG, "getContactById : cache miss for " + id);
            contact = findById(getContentResolver(), id, null);
            systemContactCache.put(id, contact);
        }
        return contact;
    }

    public void clearHistory() {
        mHistoryService.clearHistory();
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

    private static void lookupDetails(@NonNull ContentResolver res, @NonNull CallContact callContact) {
        //Log.w(TAG, "lookupDetails " + c.getKey());
        try {
            Cursor cPhones = res.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    CONTACTS_PHONES_PROJECTION, ID_SELECTION,
                    new String[]{String.valueOf(callContact.getId())}, null);
            if (cPhones != null) {
                final int iNum = cPhones.getColumnIndex(Phone.NUMBER);
                final int iType = cPhones.getColumnIndex(Phone.TYPE);
                final int iLabel = cPhones.getColumnIndex(Phone.LABEL);
                while (cPhones.moveToNext()) {
                    callContact.addNumber(cPhones.getString(iNum), cPhones.getInt(iType), cPhones.getString(iLabel), cx.ring.model.Phone.NumberType.TEL);
                    Log.w(TAG, "Phone:" + cPhones.getString(cPhones.getColumnIndex(Phone.NUMBER)));
                }
                cPhones.close();
            }

            android.net.Uri baseUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, callContact.getId());
            android.net.Uri targetUri = android.net.Uri.withAppendedPath(baseUri, ContactsContract.Contacts.Data.CONTENT_DIRECTORY);
            Cursor cSip = res.query(targetUri,
                    CONTACTS_SIP_PROJECTION,
                    ContactsContract.Data.MIMETYPE + "=? OR " + ContactsContract.Data.MIMETYPE + " =?",
                    new String[]{SipAddress.CONTENT_ITEM_TYPE, Im.CONTENT_ITEM_TYPE}, null);
            if (cSip != null) {
                final int iMime = cSip.getColumnIndex(ContactsContract.Data.MIMETYPE);
                final int iSip = cSip.getColumnIndex(SipAddress.SIP_ADDRESS);
                final int iType = cSip.getColumnIndex(SipAddress.TYPE);
                final int iLabel = cSip.getColumnIndex(SipAddress.LABEL);
                while (cSip.moveToNext()) {
                    String mime = cSip.getString(iMime);
                    String number = cSip.getString(iSip);
                    if (!mime.contentEquals(Im.CONTENT_ITEM_TYPE) || new Uri(number).isRingId() || "ring".equalsIgnoreCase(cSip.getString(iLabel))) {
                        callContact.addNumber(number, cSip.getInt(iType), cSip.getString(iLabel), cx.ring.model.Phone.NumberType.SIP);
                    }
                    Log.w(TAG, "SIP phone:" + number + " " + mime + " ");
                }
                cSip.close();
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public static CallContact findById(@NonNull ContentResolver res, long id, String key) {
        CallContact contact = null;
        try {
            android.net.Uri contentUri;
            if (key != null) {
                contentUri = ContactsContract.Contacts.lookupContact(res, ContactsContract.Contacts.getLookupUri(id, key));
            } else {
                contentUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
            }

            Cursor result = res.query(contentUri, CONTACT_PROJECTION, null, null, null);
            if (result == null) {
                return null;
            }

            if (result.moveToFirst()) {
                int iID = result.getColumnIndex(ContactsContract.Data._ID);
                int iKey = result.getColumnIndex(ContactsContract.Data.LOOKUP_KEY);
                int iName = result.getColumnIndex(ContactsContract.Data.DISPLAY_NAME);
                int iPhoto = result.getColumnIndex(ContactsContract.Data.PHOTO_ID);
                int iStared = result.getColumnIndex(ContactsContract.Contacts.STARRED);
                long cid = result.getLong(iID);

                Log.d(TAG, "Contact name: " + result.getString(iName) + " id:" + cid + " key:" + result.getString(iKey));

                contact = new CallContact(cid, result.getString(iKey), result.getString(iName), result.getLong(iPhoto));
                if (result.getInt(iStared) != 0) {
                    contact.setStared();
                }
                lookupDetails(res, contact);
            }
            result.close();
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        if (contact == null) {
            Log.w(TAG, "findById " + id + " can't find contact.");
        }
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
            if (result == null) {
                Log.w(TAG, "findContactBySipNumber " + number + " can't find contact.");
                return CallContact.buildUnknown(number);
            }
            int icID = result.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID);
            int iKey = result.getColumnIndex(ContactsContract.Data.LOOKUP_KEY);
            int iName = result.getColumnIndex(ContactsContract.Data.DISPLAY_NAME);
            int iPhoto = result.getColumnIndex(ContactsContract.Data.PHOTO_ID);
            int iStared = result.getColumnIndex(ContactsContract.Contacts.STARRED);

            while (result.moveToNext()) {
                long cid = result.getLong(icID);
                CallContact contact = new CallContact(cid, result.getString(iKey), result.getString(iName), result.getLong(iPhoto));
                if (result.getInt(iStared) != 0) {
                    contact.setStared();
                }
                lookupDetails(res, contact);
                contacts.add(contact);
            }
            result.close();
        } catch (Exception e) {
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
        CallContact callContact = null;
        try {
            android.net.Uri uri = android.net.Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, android.net.Uri.encode(number));
            Cursor result = res.query(uri, PHONELOOKUP_PROJECTION, null, null, null);
            if (result == null) {
                Log.w(TAG, "findContactByNumber " + number + " can't find contact.");
                return findContactBySipNumber(res, number);
            }
            if (result.moveToFirst()) {
                int iID = result.getColumnIndex(ContactsContract.Contacts._ID);
                int iKey = result.getColumnIndex(ContactsContract.Data.LOOKUP_KEY);
                int iName = result.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                int iPhoto = result.getColumnIndex(ContactsContract.Contacts.PHOTO_ID);
                callContact = new CallContact(result.getLong(iID), result.getString(iKey), result.getString(iName), result.getLong(iPhoto));
                lookupDetails(res, callContact);
                Log.w(TAG, "findContactByNumber " + number + " found " + callContact.getDisplayName());
            }
            result.close();
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        if (callContact == null) {
            Log.w(TAG, "findContactByNumber " + number + " can't find contact.");
            callContact = findContactBySipNumber(res, number);
        }
        return callContact;
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
            if (number == null || number.isEmpty()) {
                return null;
            }
            number = CallContact.canonicalNumber(number);
            CallContact callContact = cache.get(number);
            if (callContact == null) {
                callContact = canUseContacts ? findContactByNumber(cr, number) : CallContact.buildUnknown(number);
                cache.put(number, callContact);
            }
            return callContact;
        }

        Tuple<HistoryEntry, HistoryCall> findHistoryByCallId(final Map<String, Conversation> confs, String id) {
            for (Conversation c : confs.values()) {
                Tuple<HistoryEntry, HistoryCall> h = c.findHistoryByCallId(id);
                if (h != null) {
                    return h;
                }
            }
            return null;
        }

        CallContact getCreateContact(long contactId, String contactKey, String cnumber) {
            String number = CallContact.canonicalNumber(cnumber);
            CallContact contact;
            if (contactId <= CallContact.DEFAULT_ID) {
                contact = getByNumber(localNumberCache, number);
            } else {
                contact = localContactCache.get(contactId);
                if (contact == null) {
                    contact = canUseContacts ? findById(cr, contactId, contactKey) : CallContact.buildUnknown(number);
                    if (contact != null) {
                        contact.addPhoneNumber(cnumber);
                    } else {
                        Log.w(TAG, "Can't find contact with id " + contactId);
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
                final List<HistoryCall> history = mHistoryService.getAll();
                final List<HistoryText> historyTexts = mHistoryService.getAllTextMessages();

                final Map<String, ArrayList<String>> confs = mService.getConferenceList();

                for (HistoryCall call : history) {
                    CallContact contact = getCreateContact(call.getContactID(), call.getContactKey(), call.getNumber());

                    Map.Entry<String, Conversation> merge = null;
                    for (Map.Entry<String, Conversation> ce : ret.entrySet()) {
                        Conversation conversation = ce.getValue();
                        if ((contact.getId() > 0 && contact.getId() == conversation.getContact().getId()) || conversation.getContact().hasNumber(call.getNumber())) {
                            merge = ce;
                            break;
                        }
                    }
                    if (merge != null) {
                        Conversation conversation = merge.getValue();
                        if (conversation.getContact().getId() <= 0 && contact.getId() > 0) {
                            conversation.setContact(contact);
                            ret.remove(merge.getKey());
                            ret.put(contact.getIds().get(0), conversation);
                        }
                        conversation.addHistoryCall(call);
                        continue;
                    }
                    String key = contact.getIds().get(0);
                    if (ret.containsKey(key)) {
                        ret.get(key).addHistoryCall(call);
                    } else {
                        Conversation conversation = new Conversation(contact);
                        conversation.addHistoryCall(call);
                        ret.put(key, conversation);
                    }
                }

                for (HistoryText htext : historyTexts) {
                    CallContact contact = getCreateContact(htext.getContactID(), htext.getContactKey(), htext.getNumber());
                    Tuple<HistoryEntry, HistoryCall> p = findHistoryByCallId(ret, htext.getCallId());

                    if (contact == null && p != null) {
                        contact = p.first.getContact();
                    }
                    if (contact == null) {
                        continue;
                    }

                    TextMessage msg = new TextMessage(htext);
                    msg.setContact(contact);

                    if (p != null) {
                        if (msg.getNumberUri() == null) {
                            msg.setNumber(new Uri(p.second.getNumber()));
                        }
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
                        if (call == null) {
                            call = new SipCall(call_id, mService.getCallDetails(call_id));
                        }
                        Account acc = mAccountService.getAccount(call.getAccount());
                        if (acc.isRing()
                                || acc.getDetailBoolean(ConfigKey.SRTP_ENABLE)
                                || acc.getDetailBoolean(ConfigKey.TLS_ENABLE)) {
                            call = new SecureSipCall(call, acc.getDetail(ConfigKey.SRTP_KEY_EXCHANGE));
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
                            conv = ret.get(id);
                            if (conv != null) {
                                break;
                            }
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
                for (Conversation c : ret.values()) {
                    Log.w(TAG, "Conversation : " + c.getContact().getId() + " " + c.getContact().getDisplayName() + " " + c.getLastNumberUsed(c.getLastAccountUsed()) + " " + c.getLastInteraction().toString());
                }
                for (int i = 0; i < localContactCache.size(); i++) {
                    CallContact contact = localContactCache.valueAt(i);
                    String key = contact.getIds().get(0);
                    if (!ret.containsKey(key)) {
                        ret.put(key, new Conversation(contact));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "ConversationLoader doInBackground", e);
            }
            return ret;
        }
    }

    private void updated(Map<String, Conversation> res) {
        for (Conversation conversation : conversations.values()) {
            for (Conference conference : conversation.getCurrentCalls()) {
                notificationManager.cancel(conference.getUuid());
            }

            boolean isConversationVisible = conversation.isVisible();
            String conversationKey = conversation.getContact().getIds().get(0);
            Conversation newConversation = res.get(conversationKey);
            if (newConversation != null) {
                newConversation.setVisible(isConversationVisible);
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
                Log.e(TAG, "updateConnectivityState", e);
            }
        }
    }

    public void updateTextNotifications() {
        Log.d(TAG, "updateTextNotifications()");

        for (Conversation c : conversations.values()) {

            if (c.isVisible()) {
                notificationManager.cancel(c.getUuid());
                continue;
            }

            TreeMap<Long, TextMessage> texts = c.getUnreadTextMessages();
            if (texts.isEmpty() || texts.lastEntry().getValue().isNotified()) {
                continue;
            } else {
                notificationManager.cancel(c.getUuid());
            }

            CallContact contact = c.getContact();
            if (mMessageNotificationBuilder == null) {
                mMessageNotificationBuilder = new NotificationCompat.Builder(getApplicationContext());
                mMessageNotificationBuilder.setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setSmallIcon(R.drawable.ic_ring_logo_white)
                        .setContentTitle(contact.getDisplayName());
            }
            Intent c_intent = new Intent(Intent.ACTION_VIEW)
                    .setClass(this, ConversationActivity.class)
                    .setData(android.net.Uri.withAppendedPath(ContentUriHandler.CONVERSATION_CONTENT_URI, contact.getIds().get(0)));
            Intent d_intent = new Intent(ACTION_CONV_READ)
                    .setClass(this, LocalService.class)
                    .setData(android.net.Uri.withAppendedPath(ContentUriHandler.CONVERSATION_CONTENT_URI, contact.getIds().get(0)));
            mMessageNotificationBuilder.setContentIntent(PendingIntent.getActivity(this, new Random().nextInt(), c_intent, 0))
                    .setDeleteIntent(PendingIntent.getService(this, new Random().nextInt(), d_intent, 0));

            if (contact.getPhoto() != null) {
                Resources res = getResources();
                int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
                int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);

                Bitmap bmp = BitmapUtils.bytesToBitmap(contact.getPhoto());
                if (bmp != null) {
                    mMessageNotificationBuilder.setLargeIcon(Bitmap.createScaledBitmap(bmp, width, height, false));
                }
            }
            if (texts.size() == 1) {
                TextMessage txt = texts.firstEntry().getValue();
                txt.setNotified(true);
                mMessageNotificationBuilder.setContentText(txt.getMessage());
                mMessageNotificationBuilder.setStyle(null);
                mMessageNotificationBuilder.setWhen(txt.getTimestamp());
            } else {
                NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
                for (TextMessage s : texts.values()) {
                    inboxStyle.addLine(Html.fromHtml("<b>" + DateUtils.formatDateTime(this, s.getTimestamp(), DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL) + "</b> " + s.getMessage()));
                    s.setNotified(true);
                }
                mMessageNotificationBuilder.setContentText(texts.lastEntry().getValue().getMessage());
                mMessageNotificationBuilder.setStyle(inboxStyle);
                mMessageNotificationBuilder.setWhen(texts.lastEntry().getValue().getTimestamp());
            }
            notificationManager.notify(c.getUuid(), mMessageNotificationBuilder.build());
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
        if (current) {
            mediaManager.obtainAudioFocus(ringing != null);
        }

        if (ringing != null) {
            mediaManager.audioManager.setMode(AudioManager.MODE_RINGTONE);
            mediaManager.startRing(null);
        } else if (current) {
            mediaManager.stopRing();
            mediaManager.audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        } else {
            mediaManager.stopRing();
            mediaManager.abandonAudioFocus();
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "BroadcastReceiver onReceive " + intent.getAction());
            switch (intent.getAction()) {
                case RingApplication.DRING_CONNECTION_CHANGED: {
                    boolean connected = intent.getBooleanExtra("connected", false);
                    if (connected) {
                        dringStarted = true;
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

                    sendBroadcast(new Intent(ACTION_CONF_UPDATE).setData(android.net.Uri.withAppendedPath(ContentUriHandler.CONVERSATION_CONTENT_URI, convId)));
                    break;
                }
                case ACTION_CALL_ACCEPT: {
                    String callId = intent.getData().getLastPathSegment();
                    try {
                        mService.accept(callId);
                    } catch (RemoteException e) {
                        Log.e(TAG, "ACTION_CALL_ACCEPT", e);
                    }
                    updateAudioState();
                    Conference conf = getConference(callId);
                    if (conf != null && !conf.isVisible()) {
                        startActivity(ActionHelper.getViewIntent(LocalService.this, conf).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    }
                    break;
                }
                case ACTION_CALL_REFUSE: {
                    String call_id = intent.getData().getLastPathSegment();
                    try {
                        mService.refuse(call_id);
                    } catch (RemoteException e) {
                        Log.e(TAG, "ACTION_CALL_REFUSE", e);
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
                        Log.e(TAG, "ACTION_CALL_END", e);
                    }
                    updateAudioState();
                    break;
                }
                case ConnectivityManager.CONNECTIVITY_ACTION:
                    Log.w(TAG, "ConnectivityManager.CONNECTIVITY_ACTION " + " " + intent.getStringExtra(ConnectivityManager.EXTRA_EXTRA_INFO) + " " + intent.getStringExtra(ConnectivityManager.EXTRA_EXTRA_INFO));
                    updateConnectivityState();
                    break;
                case CallManagerCallBack.INCOMING_TEXT:
                case ConfigurationManagerCallback.INCOMING_TEXT: {
                    String message = intent.getStringExtra("txt");
                    String number = intent.getStringExtra("from");
                    String call = intent.getStringExtra("call");
                    String accountIncoming = intent.getStringExtra("account");
                    TextMessage txt = new TextMessage(true, message, new Uri(number), call, accountIncoming);
                    Log.w(TAG, "New text messsage " + txt.getAccount() + " " + txt.getCallId() + " " + txt.getMessage());

                    Conversation conversation;
                    if (call != null && !call.isEmpty()) {
                        conversation = getConversationByCallId(call);
                    } else {
                        conversation = startConversation(findContactByNumber(txt.getNumberUri()));
                        txt.setContact(conversation.getContact());
                    }
                    if (conversation.isVisible()) {
                        txt.read();
                    }

                    mHistoryService.insertNewTextMessage(txt);

                    conversation.addTextMessage(txt);

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
                        Log.d(TAG, "Message status changed " + id + " " + status);
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
                    Uri number = new Uri(intent.getStringExtra("from"));
                    CallContact contact = findContactByNumber(number);
                    Conversation conversation = startConversation(contact);

                    SipCall call = new SipCall(callId, accountId, number, SipCall.Direction.INCOMING);
                    call.setContact(contact);

                    Account accountCall = mAccountService.getAccount(accountId);

                    Conference toAdd;
                    if (accountCall.useSecureLayer()) {
                        SecureSipCall secureCall = new SecureSipCall(call, accountCall.getDetail(ConfigKey.SRTP_KEY_EXCHANGE));
                        toAdd = new Conference(secureCall);
                    } else {
                        toAdd = new Conference(call);
                    }

                    conversation.addConference(toAdd);
                    ActionHelper.showCallNotification(LocalService.this, toAdd);
                    updateAudioState();

                    try {
                        mService.setPreviewSettings();
                    } catch (RemoteException e) {
                        Log.e(TAG, "INCOMING_CALL", e);
                    }

                    // Sending VCard when receiving a call
                    try {
                        getRemoteService().sendProfile(callId, accountId);
                        Log.d(TAG, "send vcard");
                    } catch (Exception e) {
                        Log.e(TAG, "Error while sending profile", e);
                    }

                    sendBroadcast(new Intent(ACTION_CONF_UPDATE).setData(android.net.Uri.withAppendedPath(ContentUriHandler.CALL_CONTENT_URI, callId)));
                    break;
                }
                case CallManagerCallBack.CALL_STATE_CHANGED: {
                    String callId = intent.getStringExtra("call");
                    Conversation conversation = null;
                    Conference found = null;

                    for (Conversation conv : conversations.values()) {
                        Conference tconf = conv.getConference(callId);
                        if (tconf != null) {
                            conversation = conv;
                            found = tconf;
                            break;
                        }
                    }

                    if (found == null) {
                        Log.w(TAG, "CALL_STATE_CHANGED : Can't find conference " + callId);
                    } else {
                        SipCall call = found.getCallById(callId);
                        int oldState = call.getCallState();
                        int newState = SipCall.stateFromString(intent.getStringExtra("state"));

                        Log.w(TAG, "Call state change for " + callId + " : " + SipCall.stateToString(oldState) + " -> " + SipCall.stateToString(newState));

                        if (newState != oldState) {
                            Log.w(TAG, "CALL_STATE_CHANGED : updating call state to " + newState);
                            if ((call.isRinging() || newState == SipCall.State.CURRENT) && call.getTimestampStart() == 0) {
                                call.setTimestampStart(System.currentTimeMillis());
                            }
                            if (newState == SipCall.State.RINGING) {
                                try {
                                    getRemoteService().sendProfile(callId, call.getAccount());
                                    Log.d(TAG, "send vcard " + call.getAccount());
                                } catch (Exception e) {
                                    Log.e(TAG, "Error while sending profile", e);
                                }
                            }
                            call.setCallState(newState);
                        }

                        try {
                            call.setDetails((HashMap<String, String>) intent.getSerializableExtra("details"));
                        } catch (Exception e) {
                            Log.w(TAG, "Can't set call details.", e);
                        }

                        if (newState == SipCall.State.HUNGUP
                                || newState == SipCall.State.BUSY
                                || newState == SipCall.State.FAILURE
                                || newState == SipCall.State.INACTIVE
                                || newState == SipCall.State.OVER) {
                            if (newState == SipCall.State.HUNGUP) {
                                call.setTimestampEnd(System.currentTimeMillis());
                            }

                            mHistoryService.insertNewEntry(found);
                            conversation.addHistoryCall(new HistoryCall(call));
                            notificationManager.cancel(found.getUuid());
                            found.removeParticipant(call);
                        } else {
                            ActionHelper.showCallNotification(LocalService.this, found);
                        }
                        if (newState == SipCall.State.FAILURE || newState == SipCall.State.BUSY) {
                            try {
                                mService.hangUp(callId);
                            } catch (RemoteException e) {
                                Log.e(TAG, "hangUp", e);
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

        intentFilter.addAction(RingApplication.DRING_CONNECTION_CHANGED);

        intentFilter.addAction(ConfigurationManagerCallback.ACCOUNT_STATE_CHANGED);
        intentFilter.addAction(ConfigurationManagerCallback.ACCOUNTS_CHANGED);
        intentFilter.addAction(ConfigurationManagerCallback.ACCOUNTS_EXPORT_ENDED);
        intentFilter.addAction(ConfigurationManagerCallback.ACCOUNTS_DEVICES_CHANGED);
        intentFilter.addAction(ConfigurationManagerCallback.INCOMING_TEXT);
        intentFilter.addAction(ConfigurationManagerCallback.MESSAGE_STATE_CHANGED);
        intentFilter.addAction(ConfigurationManagerCallback.NAME_LOOKUP_ENDED);
        intentFilter.addAction(ConfigurationManagerCallback.NAME_REGISTRATION_ENDED);

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
        public void onChange(boolean selfChange, android.net.Uri uri) {
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
        mSystemContactLoader.setSystemContactPermission(canUseContacts && mDeviceRuntimeService.hasContactPermission());
        mSystemContactLoader.onContentChanged();
        mSystemContactLoader.startLoading();
    }

    public void deleteConversation(Conversation conversation) {
        mHistoryService.clearHistoryForConversation(conversation);
        refreshConversations();
    }

    @Override
    public void update(Observable observable, ServiceEvent arg) {

        if (observable instanceof HistoryService) {
            refreshConversations();
        }

        if (observable instanceof SettingsService) {
            canUseContacts = mSettingsService.loadSettings().isAllowSystemContacts();
            canUseMobile = mSettingsService.loadSettings().isAllowMobileData();
            refreshContacts();
            updateConnectivityState();
        }

        if (observable instanceof AccountService && arg != null) {
            switch (arg.getEventType()) {
                case ACCOUNTS_CHANGED:

                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(LocalService.this);
                    sharedPreferences.edit()
                            .putBoolean(OutgoingCallHandler.KEY_CACHE_HAVE_RINGACCOUNT, mAccountService.hasRingAccount())
                            .putBoolean(OutgoingCallHandler.KEY_CACHE_HAVE_SIPACCOUNT, mAccountService.hasSipAccount()).apply();

                    mSystemContactLoader.loadRingContacts = mAccountService.hasRingAccount();
                    mSystemContactLoader.loadSipContacts = mAccountService.hasSipAccount();
                    mSystemContactLoader.setSystemContactPermission(canUseContacts && mDeviceRuntimeService.hasContactPermission());
                    mSystemContactLoader.startLoading();
                    mSystemContactLoader.forceLoad();
                    break;
            }
        }
    }
}
