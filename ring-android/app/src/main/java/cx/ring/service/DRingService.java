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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.inject.Inject;

import cx.ring.BuildConfig;
import cx.ring.application.RingApplication;
import cx.ring.daemon.StringMap;
import cx.ring.model.Codec;
import cx.ring.services.CallService;
import cx.ring.services.ConferenceService;
import cx.ring.services.DaemonService;


public class DRingService extends Service {

    @Inject
    DaemonService mDaemonService;

    @Inject
    CallService mCallService;

    @Inject
    ConferenceService mConferenceService;

    @Inject
    ExecutorService mExecutor;

    static final String TAG = DRingService.class.getName();

    private static HandlerThread executorThread;

    static public final String DRING_CONNECTION_CHANGED = BuildConfig.APPLICATION_ID + ".event.DRING_CONNECTION_CHANGE";
    static public final String VIDEO_EVENT = BuildConfig.APPLICATION_ID + ".event.VIDEO_EVENT";

    private ConfigurationManagerCallback configurationCallback;
    private CallManagerCallBack callManagerCallBack;
    private VideoManagerCallback videoManagerCallback;

    class Shm {
        String id;
        String path;
        int w, h;
        boolean mixer;
        long window = 0;
    }

    static public WeakReference<SurfaceHolder> mCameraPreviewSurface = new WeakReference<>(null);
    static public Map<String, WeakReference<SurfaceHolder>> videoSurfaces = Collections.synchronizedMap(new HashMap<String, WeakReference<SurfaceHolder>>());
    private final Map<String, Shm> videoInputs = new HashMap<>();
    private Camera previewCamera = null;
    private VideoParams previewParams = null;

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

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreated");
        super.onCreate();

        // dependency injection
        ((RingApplication) getApplication()).getRingInjectionComponent().inject(this);

        Future<Boolean> startResult = mExecutor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                configurationCallback = new ConfigurationManagerCallback(DRingService.this);

                callManagerCallBack = new CallManagerCallBack(DRingService.this);
                mCallService.addObserver(callManagerCallBack);
                mConferenceService.addObserver(callManagerCallBack);

                videoManagerCallback = new VideoManagerCallback(DRingService.this);

                mDaemonService.startDaemon(
                        mCallService.getCallbackHandler(),
                        mConferenceService.getCallbackHandler(),
                        configurationCallback,
                        videoManagerCallback);

                ringerModeChanged(((AudioManager) getSystemService(Context.AUDIO_SERVICE)).getRingerMode());
                registerReceiver(ringerModeListener, RINGER_FILTER);

                Intent intent = new Intent(DRING_CONNECTION_CHANGED);
                intent.putExtra("connected", mDaemonService.isStarted());
                sendBroadcast(intent);

                videoManagerCallback.init();

                return true;
            }
        });

        try {
            startResult.get();
        } catch (Exception e) {
            Log.e(TAG, "DRingService start failed", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand " + (intent == null ? "null" : intent.getAction()) + " " + flags + " " + startId);
        return START_STICKY; /* started and stopped explicitly */
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");

        Future<Boolean> stopResult = mExecutor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                unregisterReceiver(ringerModeListener);
                mDaemonService.stopDaemon();
                configurationCallback = null;
                callManagerCallBack = null;
                videoManagerCallback = null;
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

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        Log.i(TAG, "onBound");
        Intent intent = new Intent(DRING_CONNECTION_CHANGED);
        intent.putExtra("connected", mDaemonService.isStarted());
        sendBroadcast(intent);
        return mBinder;
    }

    private static Looper createLooper() {
        if (executorThread == null) {
            Log.d(TAG, "Creating new mHandler thread");
            // ADT gives a fake warning due to bad parse rule.
            executorThread = new HandlerThread("DRingService.Executor");
            executorThread.start();
        }
        return executorThread.getLooper();
    }

    public void decodingStarted(String id, String shmPath, int width, int height, boolean isMixer) {
        Log.i(TAG, "DRingService.decodingStarted() " + id + " " + width + "x" + height);
        Shm shm = new Shm();
        shm.id = id;
        shm.path = shmPath;
        shm.w = width;
        shm.h = height;
        shm.mixer = isMixer;
        videoInputs.put(id, shm);
        WeakReference<SurfaceHolder> weakSurfaceHolder = videoSurfaces.get(id);
        if (weakSurfaceHolder != null) {
            SurfaceHolder holder = weakSurfaceHolder.get();
            if (holder != null) {
                startVideo(shm, holder);
            }
        }
    }

    public void decodingStopped(String id) {
        Log.i(TAG, "DRingService.decodingStopped() " + id);
        Shm shm = videoInputs.remove(id);
        if (shm != null) {
            stopVideo(shm);
        }
    }

    private void startVideo(Shm input, SurfaceHolder holder) {
        Log.i(TAG, "DRingService.startVideo() " + input.id);

        input.window = mDaemonService.startVideo(input.id, holder.getSurface(), input.w, input.h);

        if (input.window == 0) {
            Log.i(TAG, "DRingService.startVideo() no window ! " + input.id);
            Intent intent = new Intent(VIDEO_EVENT);
            intent.putExtra("start", true);
            sendBroadcast(intent);
            return;
        }

        Intent intent = new Intent(VIDEO_EVENT);
        intent.putExtra("started", true);
        intent.putExtra("call", input.id);
        intent.putExtra("width", input.w);
        intent.putExtra("height", input.h);
        sendBroadcast(intent);
    }

    private void stopVideo(Shm input) {
        Log.i(TAG, "DRingService.stopVideo() " + input.id);
        if (input.window != 0) {
            mDaemonService.stopVideo(input.id, input.window);
            input.window = 0;
        }

        Intent intent = new Intent(VIDEO_EVENT);
        intent.putExtra("started", false);
        intent.putExtra("call", input.id);
        sendBroadcast(intent);
    }

    static public class VideoParams {
        public VideoParams(int id, int format, int width, int height, int rate) {
            this.id = id;
            this.format = format;
            this.width = width;
            this.height = height;
            this.rate = rate;
        }

        public int id;
        public int format;

        // size as captured by Android
        public int width;
        public int height;

        //size, rotated, as seen by the daemon
        public int rotWidth;
        public int rotHeight;

        public int rate;
        public int rotation;
    }

    static public int rotationToDegrees(int rotation) {
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }

    public void setVideoRotation(VideoParams videoParams, Camera.CameraInfo info) {
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        int rotation = rotationToDegrees(windowManager.getDefaultDisplay().getRotation());
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            videoParams.rotation = (info.orientation + rotation + 360) % 360;
        } else {
            videoParams.rotation = (info.orientation - rotation + 360) % 360;
        }
    }

    public void setCameraDisplayOrientation(int camId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(camId, info);
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        int rotation = rotationToDegrees(windowManager.getDefaultDisplay().getRotation());
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + rotation) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - rotation + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    public void startCapture(final VideoParams videoParams) {
        stopCapture();

        SurfaceHolder surface = mCameraPreviewSurface.get();
        if (surface == null) {
            Log.w(TAG, "Can't start capture: no surface registered.");
            previewParams = videoParams;
            Intent intent = new Intent(VIDEO_EVENT);
            intent.putExtra("start", true);
            sendBroadcast(intent);
            return;
        }

        if (videoParams == null) {
            Log.w(TAG, "startCapture: no video parameters ");
            return;
        }
        Log.d(TAG, "startCapture " + videoParams.id);

        final Camera preview;
        try {
            preview = Camera.open(videoParams.id);
            setCameraDisplayOrientation(videoParams.id, preview);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return;
        }

        try {
            surface.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            preview.setPreviewDisplay(surface);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return;
        }

        Camera.Parameters parameters = preview.getParameters();
        parameters.setPreviewFormat(videoParams.format);
        parameters.setPreviewSize(videoParams.width, videoParams.height);
        for (int[] fps : parameters.getSupportedPreviewFpsRange()) {
            if (videoParams.rate >= fps[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] &&
                    videoParams.rate <= fps[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]) {
                parameters.setPreviewFpsRange(fps[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                        fps[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
            }
        }

        try {
            preview.setParameters(parameters);
        } catch (RuntimeException e) {
            Log.e(TAG, "Error while settings preview parameters", e);
        }

        preview.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                mDaemonService.setVideoFrame(data, videoParams.width, videoParams.height, videoParams.rotation);
            }
        });
        preview.setErrorCallback(new Camera.ErrorCallback() {
            @Override
            public void onError(int error, Camera cam) {
                Log.w(TAG, "Camera onError " + error);
                if (preview == cam) {
                    stopCapture();
                }
            }
        });
        preview.startPreview();

        previewCamera = preview;
        previewParams = videoParams;

        Intent intent = new Intent(VIDEO_EVENT);
        intent.putExtra("camera", videoParams.id == 1);
        intent.putExtra("started", true);
        intent.putExtra("width", videoParams.rotWidth);
        intent.putExtra("height", videoParams.rotHeight);
        sendBroadcast(intent);
    }

    public void stopCapture() {
        Log.d(TAG, "stopCapture " + previewCamera);
        if (previewCamera != null) {
            final Camera preview = previewCamera;
            final VideoParams p = previewParams;
            previewCamera = null;
            preview.setPreviewCallback(null);
            preview.setErrorCallback(null);
            preview.stopPreview();
            preview.release();

            Intent intent = new Intent(VIDEO_EVENT);
            intent.putExtra("camera", p.id == 1);
            intent.putExtra("started", false);
            intent.putExtra("width", p.width);
            intent.putExtra("height", p.height);
            sendBroadcast(intent);
        }
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

        @Override
        public void sendProfile(final String callID) {
            mDaemonService.sendProfile(callID);
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
            return mDaemonService.getAccountList();
        }

        @Override
        public void setAccountOrder(final String order) {
            mDaemonService.setAccountOrder(order);
        }

        @Override
        public Map<String, String> getAccountDetails(final String accountID) {
            return mDaemonService.getAccountDetails(accountID);
        }

        @SuppressWarnings("unchecked")
        // Hashmap runtime cast
        @Override
        public void setAccountDetails(final String accountId, final Map map) {
            mDaemonService.setAccountDetails(accountId, map);
        }

        @Override
        public void setAccountActive(final String accountId, final boolean active) {
            mDaemonService.setAccountActive(accountId, active);
        }

        @Override
        public void setAccountsActive(final boolean active) {
            mDaemonService.setAccountsActive(active);
        }

        @Override
        public Map<String, String> getVolatileAccountDetails(final String accountId) {
            return mDaemonService.getVolatileAccountDetails(accountId);
        }

        @Override
        public Map<String, String> getAccountTemplate(final String accountType) throws RemoteException {
            return mDaemonService.getAccountTemplate(accountType);
        }

        @SuppressWarnings("unchecked")
        // Hashmap runtime cast
        @Override
        public String addAccount(final Map map) {
            return mDaemonService.addAccount(map);
        }

        @Override
        public void removeAccount(final String accountId) {
            mDaemonService.removeAccount(accountId);
        }

        @Override
        public String exportOnRing(final String accountId, final String password) {
            return mDaemonService.exportOnRing(accountId, password);
        }

        public Map<String, String> getKnownRingDevices(final String accountId) {
            return mDaemonService.getKnownRingDevices(accountId);
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
            return mDaemonService.getCodecList(accountID);
        }

        @Override
        public Map<String, String> validateCertificatePath(final String accountID, final String certificatePath, final String privateKeyPath, final String privateKeyPass) throws RemoteException {
            return mDaemonService.validateCertificatePath(accountID, certificatePath, privateKeyPath, privateKeyPass);
        }

        @Override
        public Map<String, String> validateCertificate(final String accountID, final String certificate) throws RemoteException {
            return mDaemonService.validateCertificate(accountID, certificate);
        }

        @Override
        public Map<String, String> getCertificateDetailsPath(final String certificatePath) throws RemoteException {
            return mDaemonService.getCertificateDetailsPath(certificatePath);
        }

        @Override
        public Map<String, String> getCertificateDetails(final String certificateRaw) throws RemoteException {
            return mDaemonService.getCertificateDetails(certificateRaw);
        }

        @Override
        public void setActiveCodecList(final List codecs, final String accountID) throws RemoteException {
            mDaemonService.setActiveCodecList(codecs, accountID);
        }

        @Override
        public void playDtmf(final String key) throws RemoteException {
            mCallService.playDtmf(key);
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
            return mDaemonService.getTlsSupportedMethods();
        }

        @Override
        public List getCredentials(final String accountID) throws RemoteException {
            return mDaemonService.getCredentials(accountID);
        }

        @Override
        public void setCredentials(final String accountID, final List creds) throws RemoteException {
            mDaemonService.setCredentials(accountID, creds);
        }

        @Override
        public void registerAllAccounts() throws RemoteException {
            mDaemonService.registerAllAccounts();
        }

        public void videoSurfaceAdded(String id) {
            Log.d(TAG, "DRingService.videoSurfaceAdded() " + id);
            Shm shm = videoInputs.get(id);
            SurfaceHolder holder = videoSurfaces.get(id).get();
            if (shm != null && holder != null && shm.window == 0) {
                startVideo(shm, holder);
            }
        }

        public void videoSurfaceRemoved(String id) {
            Log.d(TAG, "DRingService.videoSurfaceRemoved() " + id);
            Shm shm = videoInputs.get(id);
            if (shm != null) {
                stopVideo(shm);
            }
        }

        public void videoPreviewSurfaceAdded() {
            Log.i(TAG, "DRingService.videoPreviewSurfaceChanged()");
            startCapture(previewParams);
        }

        public void videoPreviewSurfaceRemoved() {
            Log.i(TAG, "DRingService.videoPreviewSurfaceChanged()");
            stopCapture();
        }

        public void switchInput(final String id, final boolean front) {
            final int camId = (front ? videoManagerCallback.cameraFront : videoManagerCallback.cameraBack);
            final String uri = "camera://" + camId;
            final cx.ring.daemon.StringMap map = videoManagerCallback.getNativeParams(camId).toMap(getResources().getConfiguration().orientation);
            mDaemonService.switchInput(id, uri, map);
        }

        public void setPreviewSettings() {
            Map<String, StringMap> camSettings = new HashMap<>();
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                camSettings.put(Integer.toString(i), videoManagerCallback.getNativeParams(i).toMap(getResources().getConfiguration().orientation));
            }

            mDaemonService.setPreviewSettings(camSettings);
        }

        public int backupAccounts(final List accountIDs, final String toDir, final String password) {
            return mDaemonService.backupAccounts(accountIDs, toDir, password);
        }

        public int restoreAccounts(final String archivePath, final String password) {
            return mDaemonService.restoreAccounts(archivePath, password);
        }

        public void connectivityChanged() {
            mDaemonService.connectivityChanged();
        }

        public void lookupName(final String account, final String nameserver, final String name) {
            mDaemonService.lookupName(account, nameserver, name);
        }

        public void lookupAddress(final String account, final String nameserver, final String address) {
            mDaemonService.lookupAddress(account, nameserver, address);
        }

        public void registerName(final String account, final String password, final String name) {
            mDaemonService.registerName(account, password, name);
        }
    };
}
