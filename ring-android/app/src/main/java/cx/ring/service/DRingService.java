/*
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.RemoteInput;
import androidx.legacy.content.WakefulBroadcastReceiver;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import cx.ring.BuildConfig;
import cx.ring.application.RingApplication;
import cx.ring.client.CallActivity;
import cx.ring.client.ConversationActivity;
import cx.ring.facades.ConversationFacade;
import cx.ring.fragments.ConversationFragment;
import cx.ring.model.Account;
import cx.ring.model.Codec;
import cx.ring.model.Settings;
import cx.ring.model.Uri;
import cx.ring.services.AccountService;
import cx.ring.services.CallService;
import cx.ring.services.ConferenceService;
import cx.ring.services.ContactService;
import cx.ring.services.DaemonService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
import cx.ring.services.HistoryService;
import cx.ring.services.NotificationService;
import cx.ring.services.PreferencesService;
import cx.ring.tv.call.TVCallActivity;
import cx.ring.utils.DeviceUtils;
import io.reactivex.disposables.CompositeDisposable;

public class DRingService extends Service {
    private static final String TAG = DRingService.class.getSimpleName();

    public static final String ACTION_TRUST_REQUEST_ACCEPT = BuildConfig.APPLICATION_ID + ".action.TRUST_REQUEST_ACCEPT";
    public static final String ACTION_TRUST_REQUEST_REFUSE = BuildConfig.APPLICATION_ID + ".action.TRUST_REQUEST_REFUSE";
    public static final String ACTION_TRUST_REQUEST_BLOCK = BuildConfig.APPLICATION_ID + ".action.TRUST_REQUEST_BLOCK";

    static public final String ACTION_CALL_ACCEPT = BuildConfig.APPLICATION_ID + ".action.CALL_ACCEPT";
    static public final String ACTION_CALL_REFUSE = BuildConfig.APPLICATION_ID + ".action.CALL_REFUSE";
    static public final String ACTION_CALL_END = BuildConfig.APPLICATION_ID + ".action.CALL_END";
    static public final String ACTION_CALL_VIEW = BuildConfig.APPLICATION_ID + ".action.CALL_VIEW";

    static public final String ACTION_CONV_READ = BuildConfig.APPLICATION_ID + ".action.CONV_READ";
    static public final String ACTION_CONV_DISMISS = BuildConfig.APPLICATION_ID + ".action.CONV_DISMISS";
    static public final String ACTION_CONV_ACCEPT = BuildConfig.APPLICATION_ID + ".action.CONV_ACCEPT";
    static public final String ACTION_CONV_REPLY_INLINE = BuildConfig.APPLICATION_ID + ".action.CONV_REPLY";

    static public final String ACTION_FILE_ACCEPT = BuildConfig.APPLICATION_ID + ".action.FILE_ACCEPT";
    static public final String ACTION_FILE_CANCEL = BuildConfig.APPLICATION_ID + ".action.FILE_CANCEL";
    static public final String KEY_TRANSFER_ID = "transferId";
    static public final String KEY_TEXT_REPLY = "textReply";

    public static final String ACTION_PUSH_RECEIVED = BuildConfig.APPLICATION_ID + ".action.PUSH_RECEIVED";
    public static final String ACTION_PUSH_TOKEN_CHANGED = BuildConfig.APPLICATION_ID + ".push.PUSH_TOKEN_CHANGED";
    public static final String PUSH_RECEIVED_FIELD_FROM = "from";
    public static final String PUSH_RECEIVED_FIELD_DATA = "data";
    public static final String PUSH_TOKEN_FIELD_TOKEN = "token";

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
    protected ConferenceService mConferenceService;
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
    @Inject
    @Singleton
    @Named("DaemonExecutor")
    protected ScheduledExecutorService mExecutor;

    private final Handler mHandler = new Handler();
    private final CompositeDisposable mDisposableBag = new CompositeDisposable();
    private final Runnable mConnectivityChecker = this::updateConnectivityState;
    public static boolean isRunning = false;

    protected final IDRingService.Stub mBinder = new IDRingService.Stub() {

        @Override
        public String placeCall(final String account, final String number, final boolean video) {
            return mConversationFacade.placeCall(account, number, video).blockingGet().getCallId();
        }

        @Override
        public void refuse(final String callID) {
            mCallService.refuse(callID);
        }

        @Override
        public void accept(final String callID) {
            mCallService.accept(callID);
        }

        @Override
        public void hangUp(final String callID) {
            mCallService.hangUp(callID);
        }

        @Override
        public void hold(final String callID) {
            mCallService.hold(callID);
        }

        @Override
        public void unhold(final String callID) {
            mCallService.unhold(callID);
        }

        public void sendProfile(final String callId, final String accountId) {
            mAccountService.sendProfile(callId, accountId);
        }

        @Override
        public boolean isStarted() throws RemoteException {
            return mDaemonService.isStarted();
        }

        @Override
        public Map<String, String> getCallDetails(final String callID) throws RemoteException {
            return mCallService.getCallDetails(callID);
        }

        @Override
        public void setAudioPlugin(final String audioPlugin) {
            mCallService.setAudioPlugin(audioPlugin);
        }

        @Override
        public String getCurrentAudioOutputPlugin() {
            return mCallService.getCurrentAudioOutputPlugin();
        }

        @Override
        public List<String> getAccountList() {
            return mAccountService.getAccountList();
        }

        @Override
        public void setAccountOrder(final String order) {
            String[] accountIds = order.split(File.separator);
            mAccountService.setAccountOrder(Arrays.asList(accountIds));
        }

        @Override
        public Map<String, String> getAccountDetails(final String accountID) {
            return mAccountService.getAccountDetails(accountID);
        }

        @SuppressWarnings("unchecked")
        // Hashmap runtime cast
        @Override
        public void setAccountDetails(final String accountId, final Map map) {
            mAccountService.setAccountDetails(accountId, map);
        }

        @Override
        public void setAccountActive(final String accountId, final boolean active) {
            mAccountService.setAccountActive(accountId, active);
        }

        @Override
        public void setAccountsActive(final boolean active) {
            mAccountService.setAccountsActive(active);
        }

        @Override
        public Map<String, String> getVolatileAccountDetails(final String accountId) {
            return mAccountService.getVolatileAccountDetails(accountId);
        }

        @Override
        public Map<String, String> getAccountTemplate(final String accountType) throws RemoteException {
            return mAccountService.getAccountTemplate(accountType).blockingGet();
        }

        @SuppressWarnings("unchecked")
        // Hashmap runtime cast
        @Override
        public String addAccount(final Map map) {
            return mAccountService.addAccount((Map<String, String>) map).blockingFirst().getAccountID();
        }

        @Override
        public void removeAccount(final String accountId) {
            mAccountService.removeAccount(accountId);
        }

        @Override
        public void exportOnRing(final String accountId, final String password) {
            mAccountService.exportOnRing(accountId, password);
        }

        public Map<String, String> getKnownRingDevices(final String accountId) {
            return mAccountService.getKnownRingDevices(accountId);
        }

        /*************************
         * Transfer related API
         *************************/

        @Override
        public void transfer(final String callID, final String to) throws RemoteException {
            mCallService.transfer(callID, to);
        }

        @Override
        public void attendedTransfer(final String transferID, final String targetID) throws RemoteException {
            mCallService.attendedTransfer(transferID, targetID);
        }

        /*************************
         * Conference related API
         *************************/

        @Override
        public void removeConference(final String confID) throws RemoteException {
            mConferenceService.removeConference(confID);
        }

        @Override
        public void joinParticipant(final String selCallID, final String dragCallID) throws RemoteException {
            mConferenceService.joinParticipant(selCallID, dragCallID);
        }

        @Override
        public void addParticipant(final String callID, final String confID) throws RemoteException {
            mConferenceService.addParticipant(callID, confID);
        }

        @Override
        public void addMainParticipant(final String confID) throws RemoteException {
            mConferenceService.addMainParticipant(confID);
        }

        @Override
        public void detachParticipant(final String callID) throws RemoteException {
            mConferenceService.detachParticipant(callID);
        }

        @Override
        public void joinConference(final String selConfID, final String dragConfID) throws RemoteException {
            mConferenceService.joinConference(selConfID, dragConfID);
        }

        @Override
        public void hangUpConference(final String confID) throws RemoteException {
            mConferenceService.hangUpConference(confID);
        }

        @Override
        public void holdConference(final String confID) throws RemoteException {
            mConferenceService.holdConference(confID);
        }

        @Override
        public void unholdConference(final String confID) throws RemoteException {
            mConferenceService.unholdConference(confID);
        }

        @Override
        public boolean isConferenceParticipant(final String callID) throws RemoteException {
            return mConferenceService.isConferenceParticipant(callID);
        }

        @Override
        public Map<String, ArrayList<String>> getConferenceList() throws RemoteException {
            return mConferenceService.getConferenceList();
        }

        @Override
        public List<String> getParticipantList(final String confID) throws RemoteException {
            return mConferenceService.getParticipantList(confID);
        }

        @Override
        public String getConferenceId(String callID) throws RemoteException {
            return mConferenceService.getConferenceId(callID);
        }

        @Override
        public String getConferenceDetails(final String callID) throws RemoteException {
            return mConferenceService.getConferenceDetails(callID);
        }

        @Override
        public String getRecordPath() throws RemoteException {
            return mCallService.getRecordPath();
        }

        @Override
        public void setRecordPath(final String path) throws RemoteException {
            mCallService.setRecordPath(path);
        }

        @Override
        public boolean toggleRecordingCall(final String id) throws RemoteException {
            return mCallService.toggleRecordingCall(id);
        }

        @Override
        public boolean startRecordedFilePlayback(final String filepath) throws RemoteException {
            return mCallService.startRecordedFilePlayback(filepath);
        }

        @Override
        public void stopRecordedFilePlayback(final String filepath) throws RemoteException {
            mCallService.stopRecordedFilePlayback();
        }

        @Override
        public void sendTextMessage(final String callID, final String msg) throws RemoteException {
            mCallService.sendTextMessage(callID, msg);
        }

        @Override
        public long sendAccountTextMessage(final String accountID, final String to, final String msg) {
            return mCallService.sendAccountTextMessage(accountID, to, msg).blockingGet();
        }

        @Override
        public List<Codec> getCodecList(final String accountID) throws RemoteException {
            return mAccountService.getCodecList(accountID).blockingGet();
        }

        @Override
        public Map<String, String> validateCertificatePath(final String accountID, final String certificatePath, final String privateKeyPath, final String privateKeyPass) throws RemoteException {
            return mAccountService.validateCertificatePath(accountID, certificatePath, privateKeyPath, privateKeyPass);
        }

        @Override
        public Map<String, String> validateCertificate(final String accountID, final String certificate) throws RemoteException {
            return mAccountService.validateCertificate(accountID, certificate);
        }

        @Override
        public Map<String, String> getCertificateDetailsPath(final String certificatePath) throws RemoteException {
            return mAccountService.getCertificateDetailsPath(certificatePath);
        }

        @Override
        public Map<String, String> getCertificateDetails(final String certificateRaw) throws RemoteException {
            return mAccountService.getCertificateDetails(certificateRaw);
        }

        @Override
        public void setActiveCodecList(final List codecs, final String accountID) throws RemoteException {
            mAccountService.setActiveCodecList(accountID, codecs);
        }

        @Override
        public void playDtmf(final String key) throws RemoteException {

        }

        @Override
        public Map<String, String> getConference(final String id) throws RemoteException {
            return mConferenceService.getConference(id);
        }

        @Override
        public void setMuted(final boolean mute) throws RemoteException {
            mCallService.setMuted(mute);
        }

        @Override
        public boolean isCaptureMuted() throws RemoteException {
            return mCallService.isCaptureMuted();
        }

        @Override
        public List<String> getTlsSupportedMethods() {
            return mAccountService.getTlsSupportedMethods();
        }

        @Override
        public List getCredentials(final String accountID) throws RemoteException {
            return mAccountService.getCredentials(accountID);
        }

        @Override
        public void setCredentials(final String accountID, final List creds) throws RemoteException {
            mAccountService.setCredentials(accountID, creds);
        }

        @Override
        public void registerAllAccounts() throws RemoteException {
            mAccountService.registerAllAccounts();
        }

        @Override
        @Deprecated
        public void videoSurfaceAdded(String id) {

        }

        @Override
        @Deprecated
        public void videoSurfaceRemoved(String id) {

        }

        @Override
        @Deprecated
        public void videoPreviewSurfaceAdded() {

        }

        @Override
        @Deprecated
        public void videoPreviewSurfaceRemoved() {

        }

        @Override
        @Deprecated
        public void switchInput(final String id, final boolean front) {
        }

        @Override
        @Deprecated
        public void setPreviewSettings() {

        }

        @Override
        public int backupAccounts(final List accountIDs, final String toDir, final String password) {
            return mAccountService.backupAccounts(accountIDs, toDir, password);
        }

        @Override
        public int restoreAccounts(final String archivePath, final String password) {
            return mAccountService.restoreAccounts(archivePath, password);
        }

        @Override
        public void connectivityChanged() {
            mHardwareService.connectivityChanged();
        }

        @Override
        public void lookupName(final String account, final String nameserver, final String name) {
            mAccountService.lookupName(account, nameserver, name);
        }

        @Override
        public void lookupAddress(final String account, final String nameserver, final String address) {
            mAccountService.lookupAddress(account, nameserver, address);
        }

        @Override
        public void registerName(final String account, final String password, final String name) {
            mAccountService.registerName(account, password, name);
        }
    };

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
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                case UsbManager.ACTION_USB_DEVICE_DETACHED: {
                    mHardwareService.initVideo();
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
        Log.i(TAG, "onCreated");
        super.onCreate();

        // dependency injection
        RingApplication.getInstance().getRingInjectionComponent().inject(this);
        isRunning = true;

        if (mDeviceRuntimeService.hasContactPermission()) {
            getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contactContentObserver);
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intentFilter.addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED);
        }
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(receiver, intentFilter);
        updateConnectivityState();

        mDisposableBag.add(mPreferencesService.getSettingsSubject().subscribe(s -> {
            showSystemNotification(s);
        }));

        RingApplication.getInstance().bindDaemon();
        RingApplication.getInstance().bootstrapDaemon();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");
        unregisterReceiver(receiver);
        getContentResolver().unregisterContentObserver(contactContentObserver);

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

    @Override
    public IBinder onBind(Intent arg0) {
        Log.i(TAG, "onBound");
        return mBinder;
    }

    /* ************************************
     *
     * Implement public interface for the service
     *
     * *********************************
     */

    private void updateConnectivityState() {
        if (mDaemonService.isStarted()) {
            mAccountService.setAccountsActive(mPreferencesService.hasNetworkConnected());
            // Execute connectivityChanged to reload UPnP
            // and reconnect active accounts if necessary.
            mHardwareService.connectivityChanged();
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
                if (extras != null) {
                    handleTrustRequestAction(action, extras);
                }
                break;
            case ACTION_CALL_ACCEPT:
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
                if (extras != null) {
                    handleConvAction(intent, action, extras);
                }
                break;
            case ACTION_FILE_ACCEPT:
            case ACTION_FILE_CANCEL:
                if (extras != null) {
                    handleFileAction(action, extras);
                }
                break;
            case ACTION_PUSH_TOKEN_CHANGED:
                if (extras != null) {
                    handlePushTokenChanged(extras);
                }
                break;
            case ACTION_PUSH_RECEIVED:
                if (extras != null) {
                    handlePushReceived(extras);
                }
                break;
            default:
                break;
        }
    }

    private void handleFileAction(String action, Bundle extras) {
        Long id = extras.getLong(KEY_TRANSFER_ID);
        if (action.equals(ACTION_FILE_ACCEPT)) {
            mAccountService.acceptFileTransfer(id);
        } else if (action.equals(ACTION_FILE_CANCEL)) {
            mConversationFacade.cancelFileTransfer(id);
        }
    }

    private void handlePushReceived(Bundle extras) {
        try {
            PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wake:push");
            wl.setReferenceCounted(false);
            wl.acquire(20 * 1000);
        } catch (Exception e) {
            Log.w(TAG, "Can't acquire wake lock", e);
        }

        String from = extras.getString(PUSH_RECEIVED_FIELD_FROM);
        Bundle data = extras.getBundle(PUSH_RECEIVED_FIELD_DATA);
        if (from == null || data == null) {
            return;
        }
        Map<String, String> map = new HashMap<>(data.size());
        for (String key : data.keySet()) {
            map.put(key, data.get(key).toString());
        }
        mAccountService.pushNotificationReceived(from, map);
    }

    private void handlePushTokenChanged(Bundle extras) {
        mAccountService.setPushNotificationToken(extras.getString(PUSH_TOKEN_FIELD_TOKEN, ""));
    }

    private void handleTrustRequestAction(String action, Bundle extras) {
        String account = extras.getString(NotificationService.TRUST_REQUEST_NOTIFICATION_ACCOUNT_ID);
        Uri from = new Uri(extras.getString(NotificationService.TRUST_REQUEST_NOTIFICATION_FROM));
        if (account != null) {
            mNotificationService.cancelTrustRequestNotification(account);
            switch (action) {
                case ACTION_TRUST_REQUEST_ACCEPT:
                    mConversationFacade.acceptRequest(account, from);
                    break;
                case ACTION_TRUST_REQUEST_REFUSE:
                    mConversationFacade.discardRequest(account, from);
                    break;
                case ACTION_TRUST_REQUEST_BLOCK:
                    mConversationFacade.discardRequest(account, from);
                    mAccountService.removeContact(account, from.getRawRingId(), true);
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

    private void handleConvAction(Intent intent, String action, Bundle extras) {
        String accountId = extras.getString(ConversationFragment.KEY_ACCOUNT_ID);
        String ringId = extras.getString(ConversationFragment.KEY_CONTACT_RING_ID);

        if (ringId == null || ringId.isEmpty()) {
            return;
        }

        switch (action) {
            case ACTION_CONV_READ:
                mConversationFacade.readMessages(accountId, new Uri(ringId));
                break;
            case ACTION_CONV_DISMISS:
                break;
            case ACTION_CONV_REPLY_INLINE: { Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
                if (remoteInput != null) {
                    CharSequence reply = remoteInput.getCharSequence(KEY_TEXT_REPLY);
                    if (!TextUtils.isEmpty(reply)) {
                        Uri uri = new Uri(ringId);
                        String message = reply.toString();
                        mConversationFacade.startConversation(accountId, uri)
                                .flatMap(c -> mConversationFacade.sendTextMessage(accountId, c, uri, message)
                                        .doOnSuccess(msg -> mNotificationService.showTextNotification(accountId, c)))
                                .subscribe();
                    }
                }
                break;
            }
            case ACTION_CONV_ACCEPT:
                startActivity(new Intent(Intent.ACTION_VIEW)
                        .putExtras(extras)
                        .setClass(getApplicationContext(), ConversationActivity.class)
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

    private class ContactsContentObserver extends ContentObserver {

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
