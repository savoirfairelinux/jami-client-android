/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 * <p>
 * Author: Regis Montoya <r3gis.3R@gmail.com>
 * Author: Emeric Vigier <emeric.vigier@savoirfairelinux.com>
 * Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 * Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * If you own a pjsip commercial license you can also redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public License
 * as an android library.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.BuildConfig;
import cx.ring.application.RingApplication;
import cx.ring.client.CallActivity;
import cx.ring.daemon.StringMap;
import cx.ring.model.Codec;
import cx.ring.model.Conference;
import cx.ring.services.AccountService;
import cx.ring.services.CallService;
import cx.ring.services.ConferenceService;
import cx.ring.services.ContactService;
import cx.ring.services.DaemonService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
import cx.ring.services.NotificationService;
import cx.ring.services.NotificationServiceImpl;
import cx.ring.utils.ActionHelper;
import cx.ring.utils.ContentUriHandler;


public class DRingService extends Service {

    public static final String ACTION_TRUST_REQUEST_ACCEPT = BuildConfig.APPLICATION_ID + ".action.TRUST_REQUEST_ACCEPT";
    public static final String ACTION_TRUST_REQUEST_REFUSE = BuildConfig.APPLICATION_ID + ".action.TRUST_REQUEST_REFUSE";
    public static final String ACTION_TRUST_REQUEST_BLOCK = BuildConfig.APPLICATION_ID + ".action.TRUST_REQUEST_BLOCK";

    static public final String ACTION_CALL_ACCEPT = BuildConfig.APPLICATION_ID + ".action.CALL_ACCEPT";
    static public final String ACTION_CALL_REFUSE = BuildConfig.APPLICATION_ID + ".action.CALL_REFUSE";
    static public final String ACTION_CALL_END = BuildConfig.APPLICATION_ID + ".action.CALL_END";
    static public final String ACTION_CALL_VIEW = BuildConfig.APPLICATION_ID + ".action.CALL_VIEW";

    private static final String TAG = DRingService.class.getName();

    @Inject
    protected DaemonService mDaemonService;

    @Inject
    protected CallService mCallService;

    @Inject
    protected ConferenceService mConferenceService;

    @Inject
    protected AccountService mAccountService;

    @Inject
    protected HardwareService mHardwareService;

    @Inject
    protected DeviceRuntimeService mDeviceRuntimeService;

    @Inject
    protected NotificationService mNotificationService;

    @Inject
    protected ContactService mContactService;

    @Inject
    @Named("DaemonExecutor")
    protected ExecutorService mExecutor;

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreated");
        super.onCreate();

        // dependency injection
        ((RingApplication) getApplication()).getRingInjectionComponent().inject(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand " + (intent == null ? "null" : intent.getAction()) + " " + flags + " " + startId);

        if (intent != null && intent.getAction() != null) {
            parseIntent(intent);
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

    protected final IDRingService.Stub mBinder = new IDRingService.Stub() {

        @Override
        public String placeCall(final String account, final String number, final boolean video) {
            return mCallService.placeCall(account, number, video);
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
            return mAccountService.getAccountTemplate(accountType);
        }

        @SuppressWarnings("unchecked")
        // Hashmap runtime cast
        @Override
        public String addAccount(final Map map) {
            return mAccountService.addAccount(map).getAccountID();
        }

        @Override
        public void removeAccount(final String accountId) {
            mAccountService.removeAccount(accountId);
        }

        @Override
        public String exportOnRing(final String accountId, final String password) {
            return mAccountService.exportOnRing(accountId, password);
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
        public boolean toggleRecordingCall(final String id) throws RemoteException {
            return mCallService.toggleRecordingCall(id);
        }

        @Override
        public boolean startRecordedFilePlayback(final String filepath) throws RemoteException {
            return mCallService.startRecordedFilePlayback(filepath);
        }

        @Override
        public void stopRecordedFilePlayback(final String filepath) throws RemoteException {
            mCallService.stopRecordedFilePlayback(filepath);
        }

        @Override
        public void setRecordPath(final String path) throws RemoteException {
            mCallService.setRecordPath(path);
        }

        @Override
        public void sendTextMessage(final String callID, final String msg) throws RemoteException {
            mCallService.sendTextMessage(callID, msg);
        }

        @Override
        public long sendAccountTextMessage(final String accountID, final String to, final String msg) {
            return mCallService.sendAccountTextMessage(accountID, to, msg);
        }

        @Override
        public List<Codec> getCodecList(final String accountID) throws RemoteException {
            return mAccountService.getCodecList(accountID);
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
            mAccountService.setActiveCodecList(codecs, accountID);
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

    private void parseIntent(Intent intent) {
        Bundle extras;
        switch (intent.getAction()) {
            case ACTION_TRUST_REQUEST_ACCEPT:
            case ACTION_TRUST_REQUEST_REFUSE:
            case ACTION_TRUST_REQUEST_BLOCK:
                extras = intent.getExtras();
                if (extras != null) {
                    handleTrustRequestAction(intent.getAction(), extras);
                }
                break;
            case ACTION_CALL_ACCEPT:
            case ACTION_CALL_REFUSE:
            case ACTION_CALL_END:
            case ACTION_CALL_VIEW:
                extras = intent.getExtras();
                if (extras != null) {
                    handleCallAction(intent.getAction(), extras);
                }
                break;
            default:
                break;
        }
    }

    private void handleTrustRequestAction(String action, Bundle extras) {
        String account = extras.getString(NotificationServiceImpl.TRUST_REQUEST_NOTIFICATION_ACCOUNT_ID);
        String from = extras.getString(NotificationServiceImpl.TRUST_REQUEST_NOTIFICATION_FROM);
        if (account != null && from != null) {
            mNotificationService.cancelTrustRequestNotification(account);
            switch (action) {
                case ACTION_TRUST_REQUEST_ACCEPT:
                    mAccountService.acceptTrustRequest(account, from);
                    break;
                case ACTION_TRUST_REQUEST_REFUSE:
                    mAccountService.discardTrustRequest(account, from);
                    break;
                case ACTION_TRUST_REQUEST_BLOCK:
                    mAccountService.discardTrustRequest(account, from);
                    mContactService.removeContact(account, from);
                    break;
            }
        }
    }

    private void handleCallAction(String action, Bundle extras) {
        String callId = extras.getString(NotificationServiceImpl.KEY_CALL_ID);

        if(callId == null || callId.isEmpty()) {
            return;
        }

        switch (action) {
            case ACTION_CALL_ACCEPT:
                mCallService.accept(callId);
                mNotificationService.cancelCallNotification(callId.hashCode());
                startActivity(new Intent(Intent.ACTION_VIEW)
                        .putExtras(extras)
                        .setClass(getApplicationContext(), CallActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_NEW_TASK));
                break;
            case ACTION_CALL_REFUSE:
                mCallService.refuse(callId);
                mNotificationService.cancelCallNotification(callId.hashCode());
                break;
            case ACTION_CALL_END:
                mCallService.hangUp(callId);
                mNotificationService.cancelCallNotification(callId.hashCode());
                break;
            case ACTION_CALL_VIEW:
                startActivity(new Intent(Intent.ACTION_VIEW)
                        .putExtras(extras)
                        .setClass(getApplicationContext(), CallActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_NEW_TASK));
                break;
        }
    }
}
