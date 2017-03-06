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
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.LruCache;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import cx.ring.BuildConfig;
import cx.ring.application.RingApplication;
import cx.ring.facades.ConversationFacade;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.ServiceEvent;
import cx.ring.model.Settings;
import cx.ring.services.AccountService;
import cx.ring.services.ContactService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
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
                case ConfigurationManagerCallback.NAME_LOOKUP_ENDED:
                    // no refresh here
                    break;
                default:
                    break;
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
        intentFilter.addAction(ConfigurationManagerCallback.MESSAGE_STATE_CHANGED);
        intentFilter.addAction(ConfigurationManagerCallback.NAME_LOOKUP_ENDED);
        intentFilter.addAction(ConfigurationManagerCallback.NAME_REGISTRATION_ENDED);

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

    public void refreshContacts() {
        Log.d(TAG, "refreshContacts");
        mContactService.loadContacts(mAccountService.hasRingAccount(), mAccountService.hasSipAccount());
    }

    @Override
    public void update(Observable observable, ServiceEvent arg) {
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
                    return;
            }
        }

        if (observable instanceof ContactService && arg != null) {
            switch (arg.getEventType()) {
                case CONTACTS_CHANGED:
                    mConversationFacade.refreshConversations();
                    return;
            }
        }

        if (observable instanceof ConversationFacade && arg != null) {
            switch (arg.getEventType()) {
                case CALL_STATE_CHANGED:
                case INCOMING_CALL:
                    Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            updateAudioState();
                        }
                    });
                    return;
            }
        }
    }
}
