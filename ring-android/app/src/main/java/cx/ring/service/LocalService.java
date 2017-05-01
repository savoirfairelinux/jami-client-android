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
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.LruCache;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import cx.ring.BuildConfig;
import cx.ring.application.RingApplication;
import cx.ring.facades.ConversationFacade;
import cx.ring.model.ServiceEvent;
import cx.ring.services.AccountService;
import cx.ring.services.ContactService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.NotificationService;
import cx.ring.services.PreferencesService;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class LocalService extends Service implements Observer<ServiceEvent> {
    static final String TAG = LocalService.class.getSimpleName();

    // Emitting events
    static public final String ACTION_CONF_UPDATE = BuildConfig.APPLICATION_ID + ".action.CONF_UPDATE";
    static public final String ACTION_CONF_LOADED = BuildConfig.APPLICATION_ID + ".action.CONF_LOADED";

    static public final String ACTION_CONV_READ = BuildConfig.APPLICATION_ID + ".action.CONV_READ";

    // Receiving commands
    static public final String ACTION_CALL_ACCEPT = BuildConfig.APPLICATION_ID + ".action.CALL_ACCEPT";
    static public final String ACTION_CALL_REFUSE = BuildConfig.APPLICATION_ID + ".action.CALL_REFUSE";
    static public final String ACTION_CALL_END = BuildConfig.APPLICATION_ID + ".action.CALL_END";
    static public final String ACTION_CONV_ACCEPT = BuildConfig.APPLICATION_ID + ".action.CONV_ACCEPT";

    @Inject
    PreferencesService mPreferencesService;

    @Inject
    AccountService mAccountService;

    @Inject
    ContactService mContactService;

    @Inject
    NotificationService mNotificationService;

    @Inject
    ConversationFacade mConversationFacade;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    private IDRingService mService = null;
    private boolean dringStarted = false;

    private final ContactsContentObserver contactContentObserver = new ContactsContentObserver();

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private LruCache<Long, Bitmap> mMemoryCache = null;
    private final ExecutorService mPool = Executors.newCachedThreadPool();

    public LruCache<Long, Bitmap> get40dpContactCache() {
        return mMemoryCache;
    }

    public ExecutorService getThreadPool() {
        return mPool;
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
        mPreferencesService.addObserver(this);
        mAccountService.addObserver(this);
        mContactService.addObserver(this);
        mConversationFacade.addObserver(this);

        // Clear any notifications from a previous app instance
        mNotificationService.cancelAll();

        startDRingService();
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
        mPreferencesService.removeObserver(this);
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

    public IDRingService getRemoteService() {
        return mService;
    }

    private void updateConnectivityState() {
        if (dringStarted) {
            try {
                getRemoteService().setAccountsActive(mPreferencesService.isConnectedWifiAndMobile());
                getRemoteService().connectivityChanged();
            } catch (RemoteException e) {
                Log.e(TAG, "updateConnectivityState", e);
            }
        }
    }

    public void startListener() {
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
        getContentResolver().unregisterContentObserver(contactContentObserver);
    }

    public void refreshContacts() {
        Log.d(TAG, "refreshContacts");
        mContactService.loadContacts(mAccountService.hasRingAccount(), mAccountService.hasSipAccount(), mAccountService.getCurrentAccount().getAccountID());
    }

    @Override
    public void update(Observable observable, ServiceEvent arg) {
        if (observable instanceof PreferencesService) {
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
                case CONTACT_ADDED:
                case CONTACT_REMOVED:
                    refreshContacts();
                    return;
            }
        }
    }
}