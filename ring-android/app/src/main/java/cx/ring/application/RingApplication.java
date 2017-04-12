/*
 *  Copyright (C) 2016 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
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
package cx.ring.application;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.BuildConfig;
import cx.ring.daemon.Callback;
import cx.ring.daemon.ConfigurationCallback;
import cx.ring.daemon.VideoCallback;
import cx.ring.dependencyinjection.DaggerRingInjectionComponent;
import cx.ring.dependencyinjection.PresenterInjectionModule;
import cx.ring.dependencyinjection.RingInjectionComponent;
import cx.ring.dependencyinjection.RingInjectionModule;
import cx.ring.dependencyinjection.ServiceInjectionModule;
import cx.ring.service.CallManagerCallBack;
import cx.ring.service.ConfigurationManagerCallback;
import cx.ring.service.LocalService;
import cx.ring.services.AccountService;
import cx.ring.services.CallService;
import cx.ring.services.ConferenceService;
import cx.ring.services.ContactService;
import cx.ring.services.DaemonService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
import cx.ring.services.SettingsService;
import cx.ring.utils.Log;

public class RingApplication extends Application {

    private final static String TAG = RingApplication.class.getName();
    public final static String DRING_CONNECTION_CHANGED = BuildConfig.APPLICATION_ID + ".event.DRING_CONNECTION_CHANGE";
    public static final int PERMISSIONS_REQUEST = 57;

    private RingInjectionComponent mRingInjectionComponent;
    private Map<String, Boolean> mPermissionsBeingAsked;

    // Android Specific callbacks handlers. They rely on low level services notifications
    private ConfigurationManagerCallback mConfigurationCallback;
    private CallManagerCallBack mCallManagerCallBack;

    // true Daemon callbacks handlers. The notify the Android ones
    private Callback mCallAndConferenceCallbackHandler;
    private ConfigurationCallback mAccountAndContactCallbackHandler;
    private VideoCallback mHardwareCallbackHandler;
    /**
     * Handler to run tasks that needs to be on main thread (UI updates)
     */
    public static final Handler uiHandler = new Handler(Looper.getMainLooper());

    @Inject
    @Named("DaemonExecutor")
    ExecutorService mExecutor;

    @Inject
    DaemonService mDaemonService;

    @Inject
    AccountService mAccountService;

    @Inject
    CallService mCallService;

    @Inject
    ConferenceService mConferenceService;

    @Inject
    HardwareService mHardwareService;

    @Inject
    SettingsService mSettingsService;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    @Inject
    ContactService mContactService;

    static private final IntentFilter RINGER_FILTER = new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION);
    private final BroadcastReceiver ringerModeListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ringerModeChanged(intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL));
        }
    };

    private void ringerModeChanged(int newMode) {
        boolean mute = newMode == AudioManager.RINGER_MODE_VIBRATE || newMode == AudioManager.RINGER_MODE_SILENT;
        mCallService.muteRingTone(mute);
    }

    private void setDefaultUncaughtExceptionHandler() {
        try {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    Log.e(TAG, "Uncaught Exception detected in thread ", e);
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(2);
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Could not set the Default Uncaught Exception Handler", e);
        }
    }

    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder s) {
            Log.d(TAG, "onServiceConnected " + className.getClassName());

            // bootstrap Daemon
            bootstrapDaemon();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "onServiceDisconnected " + className.getClassName());
        }
    };

    public void bootstrapDaemon() {

        if (mDaemonService.isStarted()) {
            return;
        }

        Future<Boolean> startResult = mExecutor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                // Android specific callbacks handlers (rely on pure Java low level Services callbacks handlers as they
                // observe them)
                mConfigurationCallback = new ConfigurationManagerCallback(getApplicationContext());
                mCallManagerCallBack = new CallManagerCallBack(getApplicationContext());

                // mCallAndConferenceCallbackHandler is a wrapper to handle CallCallbacks and ConferenceCallbacks
                mCallAndConferenceCallbackHandler = mDaemonService.getDaemonCallbackHandler(
                        mCallService.getCallbackHandler(),
                        mConferenceService.getCallbackHandler());
                mAccountAndContactCallbackHandler = mDaemonService.getDaemonConfigurationCallbackHandler(
                        mAccountService.getCallbackHandler(),
                        mContactService.getCallbackHandler());
                mHardwareCallbackHandler = mHardwareService.getCallbackHandler();

                // Android specific Low level Services observers
                mCallService.addObserver(mCallManagerCallBack);
                mConferenceService.addObserver(mCallManagerCallBack);
                mAccountService.addObserver(mConfigurationCallback);
                mContactService.addObserver(mConfigurationCallback);

                mDaemonService.startDaemon(
                        mCallAndConferenceCallbackHandler,
                        mAccountAndContactCallbackHandler,
                        mHardwareCallbackHandler);

                ringerModeChanged(((AudioManager) getSystemService(Context.AUDIO_SERVICE)).getRingerMode());
                registerReceiver(ringerModeListener, RINGER_FILTER);

                return true;
            }
        });

        try {
            startResult.get();
        } catch (Exception e) {
            Log.e(TAG, "DRingService start failed", e);
        }

        Intent intent = new Intent(DRING_CONNECTION_CHANGED);
        intent.putExtra("connected", mDaemonService.isStarted());
        sendBroadcast(intent);

        // load accounts from Daemon
        mAccountService.loadAccountsFromDaemon(mSettingsService.isConnectedWifiAndMobile());
    }

    public void terminateDaemon() {
        Future<Boolean> stopResult = mExecutor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                unregisterReceiver(ringerModeListener);
                mDaemonService.stopDaemon();
                mConfigurationCallback = null;
                mCallManagerCallBack = null;
                Intent intent = new Intent(DRING_CONNECTION_CHANGED);
                intent.putExtra("connected", mDaemonService.isStarted());
                sendBroadcast(intent);

                return true;
            }
        });

        try {
            stopResult.get();
        } catch (Exception e) {
            Log.e(TAG, "DRingService stop failed", e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        setDefaultUncaughtExceptionHandler();

        mPermissionsBeingAsked = new HashMap<>();

        // building injection dependency tree
        mRingInjectionComponent = DaggerRingInjectionComponent.builder()
                .ringInjectionModule(new RingInjectionModule(this))
                .presenterInjectionModule(new PresenterInjectionModule(this))
                .serviceInjectionModule(new ServiceInjectionModule(this))
                .build();

        // we can now inject in our self whatever modules define
        mRingInjectionComponent.inject(this);

        // to bootstrap the daemon
        Intent intent = new Intent(this, LocalService.class);
        startService(intent);
        bindService(intent, mConnection, BIND_AUTO_CREATE | BIND_IMPORTANT | BIND_ABOVE_CLIENT);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        // todo decide when to stop the daemon
        terminateDaemon();
    }

    public RingInjectionComponent getRingInjectionComponent() {
        return mRingInjectionComponent;
    }

    public boolean canAskForPermission(String permission) {

        Boolean isBeingAsked = mPermissionsBeingAsked.get(permission);

        if (isBeingAsked != null && isBeingAsked) {
            return false;
        }

        mPermissionsBeingAsked.put(permission, true);

        return true;
    }

    public void permissionHasBeenAsked(String permission) {
        mPermissionsBeingAsked.remove(permission);
    }
}
