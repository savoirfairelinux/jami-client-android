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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.ContentObserver;
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
import android.text.TextUtils;
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
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import cx.ring.BuildConfig;
import cx.ring.application.RingApplication;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.ConfigKey;
import cx.ring.model.Conversation;
import cx.ring.model.HistoryCall;
import cx.ring.model.HistoryEntry;
import cx.ring.model.HistoryText;
import cx.ring.model.SecureSipCall;
import cx.ring.model.ServiceEvent;
import cx.ring.model.Settings;
import cx.ring.model.SipCall;
import cx.ring.model.TextMessage;
import cx.ring.model.Uri;
import cx.ring.services.AccountService;
import cx.ring.services.ContactService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HistoryService;
import cx.ring.services.NotificationService;
import cx.ring.services.SettingsService;
import cx.ring.utils.ActionHelper;
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

    static public final String ACTION_CONV_READ = BuildConfig.APPLICATION_ID + ".action.CONV_READ";

    static public final String ACTION_CONF_UPDATE_EXTRA_MSG = ACTION_CONF_UPDATE + ".extra.message";

    // Receiving commands
    static public final String ACTION_CALL_ACCEPT = BuildConfig.APPLICATION_ID + ".action.CALL_ACCEPT";
    static public final String ACTION_CALL_REFUSE = BuildConfig.APPLICATION_ID + ".action.CALL_REFUSE";
    static public final String ACTION_CALL_END = BuildConfig.APPLICATION_ID + ".action.CALL_END";
    static public final String ACTION_CONV_ACCEPT = BuildConfig.APPLICATION_ID + ".action.CONV_ACCEPT";

    @Inject
    HistoryService mHistoryService;

    @Inject
    SettingsService mSettingsService;

    @Inject
    AccountService mAccountService;

    @Inject
    ContactService mContactService;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    @Inject
    NotificationService mNotificationService;

    private IDRingService mService = null;
    private boolean dringStarted = false;

    private final ContactsContentObserver contactContentObserver = new ContactsContentObserver();

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private Map<String, Conversation> conversations = new HashMap<>();
    private LongSparseArray<TextMessage> messages = new LongSparseArray<>();

    private LruCache<Long, Bitmap> mMemoryCache = null;
    private final ExecutorService mPool = Executors.newCachedThreadPool();

    private MediaManager mediaManager;

    private boolean isWifiConn = false;
    private boolean isMobileConn = false;

    private boolean canUseContacts = true;
    private boolean canUseMobile = false;

    private boolean mAreConversationsLoaded = false;

    public LruCache<Long, Bitmap> get40dpContactCache() {
        return mMemoryCache;
    }

    public ExecutorService getThreadPool() {
        return mPool;
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
            contact = mContactService.findContactByNumber(call.getNumberUri().getRawUriString());
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
        mNotificationService.cancel(conv.getContact().getDisplayName());
        updateTextNotifications();
    }

    private Conversation conversationFromMessage(TextMessage txt) {
        Conversation conv;
        String call = txt.getCallId();
        if (call != null && !call.isEmpty()) {
            conv = getConversationByCallId(call);
        } else {
            conv = startConversation(mContactService.findContactByNumber(txt.getNumberUri().getRawUriString()));
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
        new ConversationLoader(getApplicationContext().getContentResolver()) {
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
        mContactService.addObserver(this);

        // Clear any notifications from a previous app instance
        mNotificationService.cancelAll();

        Settings settings = mSettingsService.loadSettings();
        canUseContacts = settings.isAllowSystemContacts();
        canUseMobile = settings.isAllowMobileData();

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
        mContactService.removeObserver(this);
        stopListener();
        mMemoryCache.evictAll();
        mPool.shutdown();
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.w(TAG, "onServiceConnected " + className.getClassName());
            mService = IDRingService.Stub.asInterface(service);
            refreshConversations();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d(TAG, "onServiceDisconnected " + arg0.getClassName());

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
            contact = mContactService.findContactByNumber(contact.getPhones().get(0).getNumber().getRawUriString());
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

    public Conversation findConversationByNumber(Uri number) {
        if (number == null || number.isEmpty()) {
            return null;
        }
        for (Conversation conversation : conversations.values()) {
            if (conversation.getContact().hasNumber(number)) {
                return conversation;
            }
        }
        return startConversation(mContactService.findContactByNumber(number.getRawUriString()));
    }

    public void clearHistory() {
        mHistoryService.clearHistory();
        refreshConversations();
    }

    private class ConversationLoader extends AsyncTask<Void, Void, Map<String, Conversation>> {
        private final ContentResolver cr;

        public ConversationLoader(ContentResolver c) {
            cr = c;
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

        @Override
        protected Map<String, Conversation> doInBackground(Void... params) {
            final Map<String, Conversation> ret = new HashMap<>();

            if (mService == null) {
                return ret;
            }

            try {
                final List<HistoryCall> history = mHistoryService.getAll();
                final List<HistoryText> historyTexts = mHistoryService.getAllTextMessages();

                final Map<String, ArrayList<String>> confs = mService.getConferenceList();

                for (HistoryCall call : history) {
                    CallContact contact = mContactService.findContact(call.getContactID(), call.getContactKey(), call.getNumber());

                    String key = contact.getIds().get(0);
                    if (conversations.containsKey(key)) {
                        if (!conversations.get(key).getHistoryCalls().contains(call)) {
                            conversations.get(key).addHistoryCall(call);
                        }
                    } else {
                        Conversation conversation = new Conversation(contact);
                        conversation.addHistoryCall(call);
                        conversations.put(key, conversation);
                    }
                }

                for (HistoryText htext : historyTexts) {
                    CallContact contact = mContactService.findContact(htext.getContactID(), htext.getContactKey(), htext.getNumber());
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
                    if (conversations.containsKey(key)) {
                        if (!conversations.get(key).getTextMessages().contains(msg)) {
                            conversations.get(key).addTextMessage(msg);
                        }
                    } else {
                        Conversation c = new Conversation(contact);
                        c.addTextMessage(msg);
                        conversations.put(key, c);
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
                        CallContact contact = mContactService.findContact(-1, null, call.getNumber());
                        call.setContact(contact);

                        Conversation conv = null;
                        ArrayList<String> ids = contact.getIds();
                        for (String id : ids) {
                            conv = conversations.get(id);
                            if (conv != null) {
                                break;
                            }
                        }
                        if (conv != null) {
                            conv.addConference(conf);
                        } else {
                            conv = new Conversation(contact);
                            conv.addConference(conf);
                            conversations.put(ids.get(0), conv);
                        }
                    }
                }
                for (Conversation c : conversations.values()) {
                    Log.w(TAG, "Conversation : " + c.getContact().getId() + " " + c.getContact().getDisplayName() + " " + c.getLastNumberUsed(c.getLastAccountUsed()) + " " + c.getLastInteraction().toString());
                }

                for (CallContact contact : mContactService.getContacts()) {
                    String key = contact.getIds().get(0);
                    if (!conversations.containsKey(key)) {
                        conversations.put(key, new Conversation(contact));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "ConversationLoader doInBackground", e);
            }
            return conversations;
        }
    }

    private void updated(Map<String, Conversation> res) {
        for (Conversation conversation : conversations.values()) {
            mNotificationService.cancel(conversation.getContact().getDisplayName());

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
                mNotificationService.cancel(c.getContact().getDisplayName());
                continue;
            }

            TreeMap<Long, TextMessage> texts = c.getUnreadTextMessages();
            if (texts.isEmpty() || texts.lastEntry().getValue().isNotified()) {
                continue;
            } else {
                mNotificationService.cancel(c.getContact().getDisplayName());
            }

            CallContact contact = c.getContact();
            mNotificationService.showTextNotification(contact, c, texts);
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
                        conversation = startConversation(mContactService.findContactByNumber(txt.getNumberUri().getRawUriString()));
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
                    CallContact contact = mContactService.findContactByNumber(number.getRawUriString());
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
                    mNotificationService.showCallNotification(toAdd);

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
                            mNotificationService.cancel(call.getContact().getDisplayName());
                            found.removeParticipant(call);
                        } else {
                            mNotificationService.showCallNotification(found);
                        }
                        if (newState == SipCall.State.FAILURE || newState == SipCall.State.BUSY || newState == SipCall.State.HUNGUP) {
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
                case ConfigurationManagerCallback.NAME_LOOKUP_ENDED:
                    // no refresh here
                    break;
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
        mContactService.loadContacts(mAccountService.hasRingAccount(), mAccountService.hasSipAccount());
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

                    refreshContacts();
                    break;
            }
        }

        if (observable instanceof ContactService && arg != null) {
            switch (arg.getEventType()) {
                case CONTACTS_CHANGED:
                    refreshConversations();
                    break;
            }
        }
    }
}
