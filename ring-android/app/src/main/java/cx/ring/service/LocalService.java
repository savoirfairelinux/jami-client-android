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
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.LruCache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import cx.ring.BuildConfig;
import cx.ring.application.RingApplication;
import cx.ring.facades.ConversationFacade;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.ConfigKey;
import cx.ring.model.Conversation;
import cx.ring.model.HistoryCall;
import cx.ring.model.SecureSipCall;
import cx.ring.model.ServiceEvent;
import cx.ring.model.Settings;
import cx.ring.model.SipCall;
import cx.ring.model.TextMessage;
import cx.ring.model.Uri;
import cx.ring.services.AccountService;
import cx.ring.services.ContactService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
import cx.ring.services.HistoryService;
import cx.ring.services.NotificationService;
import cx.ring.services.SettingsService;
import cx.ring.utils.ActionHelper;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.MediaManager;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

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

    @Inject
    HardwareService mHardwareService;

    @Inject
    ConversationFacade mConversationFacade;

    private IDRingService mService = null;
    private boolean dringStarted = false;

    private final ContactsContentObserver contactContentObserver = new ContactsContentObserver();

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private LruCache<Long, Bitmap> mMemoryCache = null;
    private final ExecutorService mPool = Executors.newCachedThreadPool();

    private MediaManager mediaManager;

    private boolean isWifiConn = false;
    private boolean isMobileConn = false;

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

    public boolean isMobileNetworkConnectedButNotGranted() {
        return (!canUseMobile && isMobileConn);
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
        mConversationFacade.addObserver(this);

        // Clear any notifications from a previous app instance
        mNotificationService.cancelAll();

        Settings settings = mSettingsService.loadSettings();
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
        mConversationFacade.removeObserver(this);
        stopListener();
        mMemoryCache.evictAll();
        mPool.shutdown();
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.w(TAG, "onServiceConnected " + className.getClassName());
            mService = IDRingService.Stub.asInterface(service);
            mConversationFacade.refreshConversations();
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

    private void updated(Map<String, Conversation> res) {
        for (Conversation conversation : mConversationFacade.getConversations().values()) {
            boolean isConversationVisible = conversation.isVisible();
            String conversationKey = conversation.getContact().getIds().get(0);
            Conversation newConversation = res.get(conversationKey);
            if (newConversation != null) {
                newConversation.setVisible(isConversationVisible);
            }
        }
        updateAudioState();
        mConversationFacade.updateTextNotifications();
        sendBroadcast(new Intent(ACTION_CONF_UPDATE));
        sendBroadcast(new Intent(ACTION_CONF_LOADED));
        mAreConversationsLoaded = true;
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

    private void updateAudioState() {
        boolean current = false;
        Conference ringing = null;
        for (Conversation c : mConversationFacade.getConversations().values()) {
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
                    Conversation conversation = mConversationFacade.getConversationById(convId);
                    if (conversation != null) {
                        mConversationFacade.readConversation(conversation);
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
                    Conference conf = mConversationFacade.getConference(callId);
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
                        conversation = mConversationFacade.getConversationByCallId(call);
                    } else {
                        conversation = mConversationFacade.startConversation(mContactService.findContactByNumber(txt.getNumberUri().getRawUriString()));
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
                case CallManagerCallBack.INCOMING_CALL: {
                    String callId = intent.getStringExtra("call");
                    String accountId = intent.getStringExtra("account");
                    Uri number = new Uri(intent.getStringExtra("from"));
                    CallContact contact = mContactService.findContactByNumber(number.getRawUriString());
                    Conversation conversation = mConversationFacade.startConversation(contact);

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

                    for (Conversation conv : mConversationFacade.getConversations().values()) {
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
                            mNotificationService.cancelCallNotification(call);
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
                    mConversationFacade.refreshConversations();
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

    @Override
    public void update(Observable observable, ServiceEvent arg) {

        if (observable instanceof HistoryService) {
            mConversationFacade.refreshConversations();
        }

        if (observable instanceof SettingsService) {
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
                    mConversationFacade.refreshConversations();
                    break;
            }
        }

        if (observable instanceof ConversationFacade && arg != null) {
            switch (arg.getEventType()) {
                case CONVERSATIONS_CHANGED:
                    Map<String, Conversation> conversations = mConversationFacade.getConversations();
                    updated(conversations);
                    break;
            }
        }
    }
}
