/*
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 * Author: Regis Montoya <r3gis.3R@gmail.com>
 * Author: Emeric Vigier <emeric.vigier@savoirfairelinux.com>
 * Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 * Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.service;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.RemoteInput;
import androidx.legacy.content.WakefulBroadcastReceiver;

import net.jami.facades.ConversationFacade;
import net.jami.model.Settings;
import net.jami.model.Uri;
import net.jami.services.AccountService;
import net.jami.services.CallService;
import net.jami.services.ContactService;
import net.jami.services.DaemonService;
import net.jami.services.DeviceRuntimeService;
import net.jami.services.HardwareService;
import net.jami.services.HistoryService;
import net.jami.services.NotificationService;
import net.jami.services.PreferencesService;

import javax.inject.Inject;
import javax.inject.Singleton;

import cx.ring.BuildConfig;
import cx.ring.application.JamiApplication;
import cx.ring.client.CallActivity;
import cx.ring.client.ConversationActivity;
import cx.ring.tv.call.TVCallActivity;
import cx.ring.utils.ConversationPath;
import cx.ring.utils.DeviceUtils;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@AndroidEntryPoint
public class DRingService extends Service {
    private static final String TAG = DRingService.class.getSimpleName();

    public static final String ACTION_TRUST_REQUEST_ACCEPT = BuildConfig.APPLICATION_ID + ".action.TRUST_REQUEST_ACCEPT";
    public static final String ACTION_TRUST_REQUEST_REFUSE = BuildConfig.APPLICATION_ID + ".action.TRUST_REQUEST_REFUSE";
    public static final String ACTION_TRUST_REQUEST_BLOCK = BuildConfig.APPLICATION_ID + ".action.TRUST_REQUEST_BLOCK";

    static public final String ACTION_CALL_ACCEPT = BuildConfig.APPLICATION_ID + ".action.CALL_ACCEPT";
    static public final String ACTION_CALL_HOLD_ACCEPT = BuildConfig.APPLICATION_ID + ".action.CALL_HOLD_ACCEPT";
    static public final String ACTION_CALL_END_ACCEPT = BuildConfig.APPLICATION_ID + ".action.CALL_END_ACCEPT";
    static public final String ACTION_CALL_REFUSE = BuildConfig.APPLICATION_ID + ".action.CALL_REFUSE";
    static public final String ACTION_CALL_END = BuildConfig.APPLICATION_ID + ".action.CALL_END";
    static public final String ACTION_CALL_VIEW = BuildConfig.APPLICATION_ID + ".action.CALL_VIEW";

    static public final String ACTION_CONV_READ = BuildConfig.APPLICATION_ID + ".action.CONV_READ";
    static public final String ACTION_CONV_DISMISS = BuildConfig.APPLICATION_ID + ".action.CONV_DISMISS";
    static public final String ACTION_CONV_ACCEPT = BuildConfig.APPLICATION_ID + ".action.CONV_ACCEPT";
    static public final String ACTION_CONV_REPLY_INLINE = BuildConfig.APPLICATION_ID + ".action.CONV_REPLY";

    static public final String ACTION_FILE_ACCEPT = BuildConfig.APPLICATION_ID + ".action.FILE_ACCEPT";
    static public final String ACTION_FILE_CANCEL = BuildConfig.APPLICATION_ID + ".action.FILE_CANCEL";
    static public final String KEY_MESSAGE_ID = "messageId";
    static public final String KEY_TRANSFER_ID = "transferId";
    static public final String KEY_TEXT_REPLY = "textReply";

    private static final int NOTIFICATION_ID = 1;

    private final ContactsContentObserver contactContentObserver = new ContactsContentObserver();
    @Inject
    @Singleton
    protected DaemonService mDaemonService;
    @Inject
    @Singleton
    protected CallService mCallService;
    @Inject
    @Singleton
    protected AccountService mAccountService;
    @Inject
    @Singleton
    protected HardwareService mHardwareService;
    @Inject
    @Singleton
    protected HistoryService mHistoryService;
    @Inject
    @Singleton
    protected DeviceRuntimeService mDeviceRuntimeService;
    @Inject
    @Singleton
    protected NotificationService mNotificationService;
    @Inject
    @Singleton
    protected ContactService mContactService;
    @Inject
    @Singleton
    protected PreferencesService mPreferencesService;
    @Inject
    @Singleton
    protected ConversationFacade mConversationFacade;

    private final Handler mHandler = new Handler();
    private final CompositeDisposable mDisposableBag = new CompositeDisposable();
    private final Runnable mConnectivityChecker = this::updateConnectivityState;
    public static boolean isRunning = false;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                Log.w(TAG, "onReceive: received a null action on broadcast receiver");
                return;
            }
            Log.d(TAG, "receiver.onReceive: " + action);
            switch (action) {
                case ConnectivityManager.CONNECTIVITY_ACTION: {
                    updateConnectivityState();
                    break;
                }
                case PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED: {
                    mConnectivityChecker.run();
                    mHandler.postDelayed(mConnectivityChecker, 100);
                }
            }
        }
    };
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        isRunning = true;

        if (mDeviceRuntimeService.hasContactPermission()) {
            getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contactContentObserver);
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intentFilter.addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED);
        }
        registerReceiver(receiver, intentFilter);
        updateConnectivityState();

        mDisposableBag.add(mPreferencesService.getSettingsSubject().subscribe(this::showSystemNotification));

        JamiApplication.getInstance().bindDaemon();
        JamiApplication.getInstance().bootstrapDaemon();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");
        unregisterReceiver(receiver);
        getContentResolver().unregisterContentObserver(contactContentObserver);

        mHardwareService.unregisterCameraDetectionCallback();
        mDisposableBag.clear();
        isRunning = false;
    }

    private void showSystemNotification(Settings settings) {
        if (settings.isAllowPersistentNotification()) {
            startForeground(NOTIFICATION_ID, (Notification) mNotificationService.getServiceNotification());
        } else {
            stopForeground(true);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Log.i(TAG, "onStartCommand " + (intent == null ? "null" : intent.getAction()) + " " + flags + " " + startId);
        if (intent != null) {
            parseIntent(intent);
            WakefulBroadcastReceiver.completeWakefulIntent(intent);
        }
        return START_STICKY; /* started and stopped explicitly */
    }

    private final IBinder binder = new Binder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /* ************************************
     *
     * Implement public interface for the service
     *
     * *********************************
     */

    private void updateConnectivityState() {
        if (mDaemonService.isStarted()) {
            boolean isConnected = mPreferencesService.hasNetworkConnected();
            mAccountService.setAccountsActive(isConnected);
            // Execute connectivityChanged to reload UPnP
            // and reconnect active accounts if necessary.
            mHardwareService.connectivityChanged(isConnected);
        }
    }

    private void parseIntent(@NonNull Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        Bundle extras = intent.getExtras();
        switch (action) {
            case ACTION_TRUST_REQUEST_ACCEPT:
            case ACTION_TRUST_REQUEST_REFUSE:
            case ACTION_TRUST_REQUEST_BLOCK:
                handleTrustRequestAction(intent.getData(), action);
                break;
            case ACTION_CALL_ACCEPT:
            case ACTION_CALL_HOLD_ACCEPT:
            case ACTION_CALL_END_ACCEPT:
            case ACTION_CALL_REFUSE:
            case ACTION_CALL_END:
            case ACTION_CALL_VIEW:
                if (extras != null) {
                    handleCallAction(action, extras);
                }
                break;
            case ACTION_CONV_READ:
            case ACTION_CONV_ACCEPT:
            case ACTION_CONV_DISMISS:
            case ACTION_CONV_REPLY_INLINE:
                handleConvAction(intent, action);
                break;
            case ACTION_FILE_ACCEPT:
            case ACTION_FILE_CANCEL:
                if (extras != null) {
                    handleFileAction(intent.getData(), action, extras);
                }
                break;
            default:
                break;
        }
    }

    private void handleFileAction(android.net.Uri uri, String action, Bundle extras) {
        String messageId = extras.getString(KEY_MESSAGE_ID);
        String id = extras.getString(KEY_TRANSFER_ID);
        ConversationPath path = ConversationPath.fromUri(uri);
        if (action.equals(ACTION_FILE_ACCEPT)) {
            mNotificationService.removeTransferNotification(path.getAccountId(), path.getConversationUri(), id);
            mAccountService.acceptFileTransfer(path.getAccountId(), path.getConversationUri(), messageId, id);
        } else if (action.equals(ACTION_FILE_CANCEL)) {
            mConversationFacade.cancelFileTransfer(path.getAccountId(), path.getConversationUri(), messageId, id);
        }
    }

    private void handleTrustRequestAction(android.net.Uri uri, String action) {
        ConversationPath path = ConversationPath.fromUri(uri);
        if (path != null) {
            mNotificationService.cancelTrustRequestNotification(path.getAccountId());
            switch (action) {
                case ACTION_TRUST_REQUEST_ACCEPT:
                    mConversationFacade.acceptRequest(path.getAccountId(), path.getConversationUri());
                    break;
                case ACTION_TRUST_REQUEST_REFUSE:
                    mConversationFacade.discardRequest(path.getAccountId(), path.getConversationUri());
                    break;
                case ACTION_TRUST_REQUEST_BLOCK:
                    mConversationFacade.discardRequest(path.getAccountId(), path.getConversationUri());
                    mAccountService.removeContact(path.getAccountId(), path.getConversationUri().getRawRingId(), true);
                    break;
            }
        }
    }

    private void handleCallAction(String action, Bundle extras) {
        String callId = extras.getString(NotificationService.KEY_CALL_ID);
        if (callId == null || callId.isEmpty()) {
            return;
        }

        switch (action) {
            case ACTION_CALL_ACCEPT:
                mNotificationService.cancelCallNotification();
                startActivity(new Intent(ACTION_CALL_ACCEPT)
                        .putExtras(extras)
                        .setClass(getApplicationContext(), CallActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                break;
            case ACTION_CALL_HOLD_ACCEPT:
                String holdId = extras.getString(NotificationService.KEY_HOLD_ID);
                mNotificationService.cancelCallNotification();
                mCallService.hold(holdId);
                startActivity(new Intent(ACTION_CALL_ACCEPT)
                        .putExtras(extras)
                        .setClass(getApplicationContext(), CallActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                break;
            case ACTION_CALL_END_ACCEPT:
                String endId = extras.getString(NotificationService.KEY_END_ID);
                mNotificationService.cancelCallNotification();
                mCallService.hangUp(endId);
                startActivity(new Intent(ACTION_CALL_ACCEPT)
                        .putExtras(extras)
                        .setClass(getApplicationContext(), CallActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                break;
            case ACTION_CALL_REFUSE:
                mCallService.refuse(callId);
                mHardwareService.closeAudioState();
                break;
            case ACTION_CALL_END:
                mCallService.hangUp(callId);
                mHardwareService.closeAudioState();
                break;
            case ACTION_CALL_VIEW:
                mNotificationService.cancelCallNotification();
                if (DeviceUtils.isTv(this)) {
                    startActivity(new Intent(Intent.ACTION_VIEW)
                            .putExtras(extras)
                            .setClass(getApplicationContext(), TVCallActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));

                } else {
                    startActivity(new Intent(Intent.ACTION_VIEW)
                            .putExtras(extras)
                            .setClass(getApplicationContext(), CallActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                }
                break;
        }
    }

    private void handleConvAction(Intent intent, String action) {
        ConversationPath path = ConversationPath.fromIntent(intent);
        if (path == null || path.getConversationId().isEmpty()) {
            return;
        }

        switch (action) {
            case ACTION_CONV_READ:
                mConversationFacade.readMessages(path.getAccountId(), path.getConversationUri());
                break;
            case ACTION_CONV_DISMISS:
                break;
            case ACTION_CONV_REPLY_INLINE: {
                Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
                if (remoteInput != null) {
                    CharSequence reply = remoteInput.getCharSequence(KEY_TEXT_REPLY);
                    if (!TextUtils.isEmpty(reply)) {
                        Uri uri = path.getConversationUri();
                        String message = reply.toString();
                        mConversationFacade.startConversation(path.getAccountId(), uri)
                                .flatMapCompletable(c -> mConversationFacade.sendTextMessage(c, uri, message)
                                        .doOnComplete(() -> mNotificationService.showTextNotification(path.getAccountId(), c)))
                                .subscribe();
                    }
                }
                break;
            }
            case ACTION_CONV_ACCEPT:
                startActivity(new Intent(Intent.ACTION_VIEW, path.toUri(), getApplicationContext(), ConversationActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                break;
            default:
                break;
        }
    }

    public void refreshContacts() {
        if (mAccountService.getCurrentAccount() == null) {
            return;
        }
        mContactService.loadContacts(mAccountService.hasRingAccount(), mAccountService.hasSipAccount(), mAccountService.getCurrentAccount());
    }

    private static class ContactsContentObserver extends ContentObserver {

        ContactsContentObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange, android.net.Uri uri) {
            super.onChange(selfChange, uri);
            //mContactService.loadContacts(mAccountService.hasRingAccount(), mAccountService.hasSipAccount(), mAccountService.getCurrentAccount());
        }
    }
}
