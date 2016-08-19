/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Regis Montoya <r3gis.3R@gmail.com>
 *  Author: Emeric Vigier <emeric.vigier@savoirfairelinux.com>
 *          Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *          Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  If you own a pjsip commercial license you can also redistribute it
 *  and/or modify it under the terms of the GNU Lesser General Public License
 *  as an android library.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Handler;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.util.Log;

import cx.ring.BuildConfig;

import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import cx.ring.model.Codec;
import cx.ring.utils.SwigNativeConverter;


public class DRingService extends Service {

    static final String TAG = "DRingService";
    private SipServiceExecutor mExecutor;
    private static HandlerThread executorThread;

    static public final String DRING_CONNECTION_CHANGED = BuildConfig.APPLICATION_ID + ".event.DRING_CONNECTION_CHANGE";
    static public final String VIDEO_EVENT = BuildConfig.APPLICATION_ID + ".event.VIDEO_EVENT";

    private Handler handler = new Handler();
    private static int POLLING_TIMEOUT = 50;
    private Runnable pollEvents = new Runnable() {
        @Override
        public void run() {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Ringservice.pollEvents();
                }
            });
            handler.postDelayed(this, POLLING_TIMEOUT);
        }
    };
    private boolean isPjSipStackStarted = false;

    private ConfigurationManagerCallback configurationCallback;
    private CallManagerCallBack callManagerCallBack;
    private VideoManagerCallback videoManagerCallback;

    class Shm {
        String id;
        String path;
        int w, h;
        boolean mixer;
        long window = 0;
    };

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
        Log.d(TAG, (mute ? "Muting." : "Unmuting.") + " ringtone.");
        Ringservice.muteRingtone(mute);
    }

    /* called once by startService() */
    @Override
    public void onCreate() {
        Log.i(TAG, "onCreated");
        super.onCreate();

        getExecutor().execute(new StartRunnable());
    }

    /* called for each startService() */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand " + (intent == null ? "null" : intent.getAction()) + " " + flags + " " + startId);
        return START_STICKY; /* started and stopped explicitly */
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        getExecutor().execute(new FinalizeRunnable());
        super.onDestroy();

    }

    @Override
    public IBinder onBind(Intent arg0) {
        Log.i(TAG, "onBound");
        Intent intent = new Intent(DRING_CONNECTION_CHANGED);
        intent.putExtra("connected", isPjSipStackStarted);
        sendBroadcast(intent);
        return mBinder;
    }

    private static Looper createLooper() {
        if (executorThread == null) {
            Log.d(TAG, "Creating new handler thread");
            // ADT gives a fake warning due to bad parse rule.
            executorThread = new HandlerThread("DRingService.Executor");
            executorThread.start();
        }
        return executorThread.getLooper();
    }

    public SipServiceExecutor getExecutor() {
        // create mExecutor lazily
        if (mExecutor == null) {
            mExecutor = new SipServiceExecutor();
        }
        return mExecutor;
    }

    public void decodingStarted(String id, String shm_path, int w, int h, boolean is_mixer) {
        Log.i(TAG, "DRingService.decodingStarted() " + id + " " + w + "x" + h);
        Shm shm = new Shm();
        shm.id = id;
        shm.path = shm_path;
        shm.w = w;
        shm.h = h;
        shm.mixer = is_mixer;
        videoInputs.put(id, shm);
        WeakReference<SurfaceHolder> w_holder = videoSurfaces.get(id);
        if (w_holder != null) {
            SurfaceHolder holder = w_holder.get();
            if (holder != null)
                startVideo(shm, holder);
        }
    }

    public void decodingStopped(String id) {
        Log.i(TAG, "DRingService.decodingStopped() " + id);
        Shm shm = videoInputs.remove(id);
        if (shm != null)
            stopVideo(shm);
    }

    private void startVideo(Shm input, SurfaceHolder holder) {
        Log.i(TAG, "DRingService.startVideo() " + input.id);
        input.window = RingserviceJNI.acquireNativeWindow(holder.getSurface());
        if (input.window == 0) {
            Log.i(TAG, "DRingService.startVideo() no window ! " + input.id);
            Intent intent = new Intent(VIDEO_EVENT);
            intent.putExtra("start", true);
            sendBroadcast(intent);
            return;
        }
        RingserviceJNI.setNativeWindowGeometry(input.window, input.w, input.h);
        RingserviceJNI.registerVideoCallback(input.id, input.window);

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
            RingserviceJNI.unregisterVideoCallback(input.id, input.window);
            RingserviceJNI.releaseNativeWindow(input.window);
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
        public VideoParams(VideoParams p) {
            this.id = p.id;
            this.format = p.format;
            this.width = p.width;
            this.height = p.height;
            this.rate = p.rate;
        }

        public int id;
        public int format;

        // size as captured by Android
        public int width;
        public int height;

        //size, rotated, as seen by the daemon
        public int rot_width;
        public int rot_height;

        public int rate;
        public int rotation;
    }

    static public int rotationToDegrees(int r) {
        switch (r) {
            case Surface.ROTATION_0: return 0;
            case Surface.ROTATION_90: return 90;
            case Surface.ROTATION_180: return 180;
            case Surface.ROTATION_270: return 270;
        }
        return 0;
    }

    public void setVideoRotation(VideoParams p, Camera.CameraInfo info) {
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        int rotation = rotationToDegrees(windowManager.getDefaultDisplay().getRotation());
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            p.rotation =  (info.orientation + rotation + 360) % 360;
        } else {
            p.rotation =  (info.orientation - rotation + 360) % 360;
        }
    }

    public void setCameraDisplayOrientation(int cam_id, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cam_id, info);
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

    public void startCapture(final VideoParams p) {
        stopCapture();

        SurfaceHolder surface = mCameraPreviewSurface.get();
        if (surface == null) {
            Log.w(TAG, "Can't start capture: no surface registered.");
            previewParams = p;
            Intent intent = new Intent(VIDEO_EVENT);
            intent.putExtra("start", true);
            sendBroadcast(intent);
            return;
        }

        if (p == null) {
            Log.w(TAG, "startCapture: no video parameters ");
            return;
        }
        Log.d(TAG, "startCapture " + p.id);

        final Camera preview;
        try {
            preview = Camera.open(p.id);
            setCameraDisplayOrientation(p.id, preview);
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
        parameters.setPreviewFormat(p.format);
        parameters.setPreviewSize(p.width, p.height);
        for (int[] fps : parameters.getSupportedPreviewFpsRange()) {
            if (p.rate >= fps[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] &&
                    p.rate <= fps[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]) {
                parameters.setPreviewFpsRange(fps[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                        fps[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
            }
        }

        try {
            preview.setParameters(parameters);
        } catch (RuntimeException e) {
            Log.e(TAG, e.getMessage());
        }

        preview.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                long ptr = RingserviceJNI.obtainFrame(data.length);
                if (ptr != 0)
                    RingserviceJNI.setVideoFrame(data, data.length, ptr, p.width, p.height, p.rotation);
                RingserviceJNI.releaseFrame(ptr);
            }
        });
        preview.setErrorCallback(new Camera.ErrorCallback() {
            @Override
            public void onError(int error, Camera cam) {
                Log.w(TAG, "Camera onError " + error);
                if (preview == cam)
                    stopCapture();
            }
        });
        preview.startPreview();

        previewCamera = preview;
        previewParams = p;

        Intent intent = new Intent(VIDEO_EVENT);
        intent.putExtra("camera", p.id == 1);
        intent.putExtra("started", true);
        intent.putExtra("width", p.rot_width);
        intent.putExtra("height", p.rot_height);
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

    // Executes immediate tasks in a single executorThread.
    public static class SipServiceExecutor extends Handler {

        SipServiceExecutor() {
            super(createLooper());
        }

        public void execute(Runnable task) {
            // TODO: add wakelock
            Message.obtain(SipServiceExecutor.this, 0/* don't care */, task).sendToTarget();
            //Log.w(TAG, "SenT!");
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.obj instanceof Runnable) {
                executeInternal((Runnable) msg.obj);
            } else {
                Log.w(TAG, "can't handle msg: " + msg);
            }
        }

        private void executeInternal(Runnable task) {
            try {
                task.run();
            } catch (Throwable t) {
                Log.e(TAG, "run task: " + task, t);
            }
        }

        public final boolean executeSynced(final Runnable r) {
            if (r == null) {
                throw new IllegalArgumentException("runnable must not be null");
            }
            if (Looper.myLooper() == getLooper()) {
                r.run();
                return true;
            }

            BlockingRunnable br = new BlockingRunnable(r);
            return br.postAndWait(this, 0);
        }
        public final <T> T executeAndReturn(final SipRunnableWithReturn<T> r) {
            if (r == null) {
                throw new IllegalArgumentException("runnable must not be null");
            }
            if (Looper.myLooper() == getLooper()) {
                r.run();
                return r.getVal();
            }

            BlockingRunnable br = new BlockingRunnable(r);
            if (!br.postAndWait(this, 0))
                throw new RuntimeException("Can't execute runnable");
            return r.getVal();
        }

        private static final class BlockingRunnable implements Runnable {
            private final Runnable mTask;
            private boolean mDone;

            public BlockingRunnable(Runnable task) {
                mTask = task;
            }

            @Override
            public void run() {
                try {
                    mTask.run();
                } catch(Exception e){
                    e.printStackTrace();
                } finally {
                    synchronized (this) {
                        mDone = true;
                        notifyAll();
                    }
                }
            }

            public boolean postAndWait(Handler handler, long timeout) {
                if (!handler.post(this)) {
                    return false;
                }

                synchronized (this) {
                    if (timeout > 0) {
                        final long expirationTime = SystemClock.uptimeMillis() + timeout;
                        while (!mDone) {
                            long delay = expirationTime - SystemClock.uptimeMillis();
                            if (delay <= 0) {
                                return false; // timeout
                            }
                            try {
                                wait(delay);
                            } catch (InterruptedException ex) {
                            }
                        }
                    } else {
                        while (!mDone) {
                            try {
                                wait();
                            } catch (InterruptedException ex) {
                            }
                        }
                    }
                }
                return true;
            }
        }
    }

    private void stopDaemon() {
        unregisterReceiver(ringerModeListener);
        handler.removeCallbacks(pollEvents);
        if (isPjSipStackStarted) {
            Ringservice.fini();
            configurationCallback = null;
            callManagerCallBack = null;
            videoManagerCallback = null;
            isPjSipStackStarted = false;
            Log.i(TAG, "PjSIPStack stopped");
            Intent intent = new Intent(DRING_CONNECTION_CHANGED);
            intent.putExtra("connected", isPjSipStackStarted);
            sendBroadcast(intent);
        }
    }

    private void startPjSipStack() throws SameThreadException {
        if (isPjSipStackStarted)
            return;

        try {
            System.loadLibrary("ringjni");
            isPjSipStackStarted = true;

        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Problem with the current Pj stack...", e);
            isPjSipStackStarted = false;
            return;
        } catch (Exception e) {
            Log.e(TAG, "Problem with the current Pj stack...", e);
            isPjSipStackStarted = false;
        }

        configurationCallback = new ConfigurationManagerCallback(this);
        callManagerCallBack = new CallManagerCallBack(this);
        videoManagerCallback = new VideoManagerCallback(this);
        Ringservice.init(configurationCallback, callManagerCallBack, videoManagerCallback);

        ringerModeChanged(((AudioManager) getSystemService(Context.AUDIO_SERVICE)).getRingerMode());
        registerReceiver(ringerModeListener, RINGER_FILTER);

        handler.postDelayed(pollEvents, POLLING_TIMEOUT);
        Log.i(TAG, "PjSIPStack started");
        Intent intent = new Intent(DRING_CONNECTION_CHANGED);
        intent.putExtra("connected", isPjSipStackStarted);
        sendBroadcast(intent);

        videoManagerCallback.init();
    }

    // Enforce same thread contract to ensure we do not call from somewhere else
    public class SameThreadException extends Exception {
        private static final long serialVersionUID = -905639124232613768L;

        public SameThreadException() {
            super("Should be launched from a single worker thread");
        }
    }

    public abstract static class SipRunnable implements Runnable {
        protected abstract void doRun() throws SameThreadException, RemoteException;

        @Override
        public void run() {
            try {
                doRun();
            } catch (SameThreadException e) {
                Log.e(TAG, "Not done from same thread");
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    public abstract class SipRunnableWithReturn<T> implements Runnable {
        private T obj = null;

        protected abstract T doRun() throws SameThreadException, RemoteException;

        public T getVal() {
            return obj;
        }

        @Override
        public void run() {
            try {
                if (isPjSipStackStarted)
                    obj = doRun();
                else
                    Log.e(TAG, "Can't perform operation: daemon not started.");
                //done = true;
            } catch (SameThreadException e) {
                Log.e(TAG, "Not done from same thread");
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    class StartRunnable extends SipRunnable {
        @Override
        protected void doRun() throws SameThreadException {
            startPjSipStack();
        }
    }

    class FinalizeRunnable extends SipRunnable {
        @Override
        protected void doRun() throws SameThreadException {
            stopDaemon();
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
            return getExecutor().executeAndReturn(new SipRunnableWithReturn<String>() {
                @Override
                protected String doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.placeCall() thread running... " + number + " video: " + video);
                    String call_id = Ringservice.placeCall(account, number);
                    if (!video)
                        Ringservice.muteLocalMedia(call_id, "MEDIA_TYPE_VIDEO", true);
                    return call_id;
                }
            });
        }

        @Override
        public void refuse(final String callID) {
            Log.e(TAG, "REFUSE");
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.refuse() thread running...");
                    Ringservice.refuse(callID);
                    Ringservice.hangUp(callID);
                }
            });
        }

        @Override
        public void accept(final String callID) {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.accept() thread running...");
                    Ringservice.accept(callID);
                }
            });
        }

        @Override
        public void hangUp(final String callID) {
            Log.e(TAG, "HANGING UP " + callID);
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.hangUp() thread running...");
                    Ringservice.hangUp(callID);
                }
            });
        }

        @Override
        public void hold(final String callID) {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.hold() thread running...");
                    Ringservice.hold(callID);
                }
            });
        }

        @Override
        public void unhold(final String callID) {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.unhold() thread running...");
                    Ringservice.unhold(callID);
                }
            });
        }

        @Override
        public boolean isStarted() throws RemoteException {
            return isPjSipStackStarted;
        }

        @Override
        public Map<String, String> getCallDetails(final String callID) throws RemoteException {
            return getExecutor().executeAndReturn(new SipRunnableWithReturn<Map<String, String>>() {
                @Override
                protected Map<String, String> doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.getCallDetails() thread running...");
                    return Ringservice.getCallDetails(callID).toNative();
                }
            });
        }

        @Override
        public void setAudioPlugin(final String audioPlugin) {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.setAudioPlugin() thread running...");
                    Ringservice.setAudioPlugin(audioPlugin);
                }
            });
        }

        @Override
        public String getCurrentAudioOutputPlugin() {
            return getExecutor().executeAndReturn(new SipRunnableWithReturn<String>() {
                @Override
                protected String doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.getCurrentAudioOutputPlugin() thread running...");
                    return Ringservice.getCurrentAudioOutputPlugin();
                }
            });
        }

        @Override
        public List<String> getAccountList() {
            return getExecutor().executeAndReturn(new SipRunnableWithReturn<List<String>>() {
                @Override
                protected List<String> doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.getAccountList() thread running...");
                    return new ArrayList<>(Ringservice.getAccountList());
                }
            });
        }

        @Override
        public void setAccountOrder(final String order) {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.setAccountsOrder() " + order + " thread running...");
                    Ringservice.setAccountsOrder(order);
                }
            });
        }

        @Override
        public Map<String, String> getAccountDetails(final String accountID) {
            return getExecutor().executeAndReturn(new SipRunnableWithReturn<Map<String, String>>() {
                @Override
                protected Map<String, String> doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.getAccountDetails() thread running...");
                    return Ringservice.getAccountDetails(accountID).toNative();
                }
            });
        }

        @SuppressWarnings("unchecked")
        // Hashmap runtime cast
        @Override
        public void setAccountDetails(final String accountId, final Map map) {
            Log.i(TAG, "DRingService.setAccountDetails() " + map.get("Account.hostname"));
            final StringMap swigmap = StringMap.toSwig(map);

            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {

                    Ringservice.setAccountDetails(accountId, swigmap);
                    Log.i(TAG, "DRingService.setAccountDetails() thread running... " + swigmap.get("Account.hostname"));
                }

            });
        }

        @Override
        public void setAccountActive(final String accountId, final boolean active) {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.setAccountActive() thread running... " + accountId + " -> " + active);
                    Ringservice.setAccountActive(accountId, active);
                }
            });
        }

        @Override
        public void setAccountsActive(final boolean active) {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.setAccountsActive() thread running... " + active);
                    StringVect list = Ringservice.getAccountList();
                    for (int i=0, n=list.size(); i<n; i++)
                        Ringservice.setAccountActive(list.get(i), active);
                }
            });
        }

        @Override
        public Map<String, String> getVolatileAccountDetails(final String accountId) {
            return getExecutor().executeAndReturn(new SipRunnableWithReturn<Map<String, String>>() {
                @Override
                protected Map<String, String> doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.getVolatileAccountDetails() thread running...");
                    return Ringservice.getVolatileAccountDetails(accountId).toNative();
                }
            });
        }

        @Override
        public Map<String, String> getAccountTemplate(final String accountType) throws RemoteException {
            Log.i(TAG, "DRingService.getAccountTemplate() " + accountType);
            return Ringservice.getAccountTemplate(accountType).toNative();
        }

        @SuppressWarnings("unchecked")
        // Hashmap runtime cast
        @Override
        public String addAccount(final Map map) {
            return getExecutor().executeAndReturn(new SipRunnableWithReturn<String>() {
                @Override
                protected String doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.addAccount() thread running...");
                    return Ringservice.addAccount(StringMap.toSwig(map));
                }
            });
        }

        @Override
        public void removeAccount(final String accountId) {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.removeAccount() thread running...");
                    Ringservice.removeAccount(accountId);
                }
            });
        }

        /*************************
         * Transfer related API
         *************************/

        @Override
        public void transfer(final String callID, final String to) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "DRingService.transfer() thread running...");
                    if (Ringservice.transfer(callID, to)) {
                        Bundle bundle = new Bundle();
                        bundle.putString("CallID", callID);
                        bundle.putString("State", "HUNGUP");
                        Intent intent = new Intent(CallManagerCallBack.CALL_STATE_CHANGED);
                        intent.putExtra("com.savoirfairelinux.sflphone.service.newstate", bundle);
                        sendBroadcast(intent);
                    } else
                        Log.i(TAG, "NOT OK");
                }
            });

        }

        @Override
        public void attendedTransfer(final String transferID, final String targetID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "DRingService.attendedTransfer() thread running...");
                    if (Ringservice.attendedTransfer(transferID, targetID)) {
                        Log.i(TAG, "OK");
                    } else
                        Log.i(TAG, "NOT OK");
                }
            });

        }

        /*************************
         * Conference related API
         *************************/

        @Override
        public void removeConference(final String confID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "DRingService.createConference() thread running...");
                    Ringservice.removeConference(confID);
                }
            });

        }

        @Override
        public void joinParticipant(final String sel_callID, final String drag_callID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "DRingService.joinParticipant() thread running...");
                    Ringservice.joinParticipant(sel_callID, drag_callID);
                    // Generate a CONF_CREATED callback
                }
            });
            Log.i(TAG, "After joining participants");
        }

        @Override
        public void addParticipant(final String callID, final String confID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "DRingService.addParticipant() thread running...");
                    Ringservice.addParticipant(callID, confID);
                }
            });

        }

        @Override
        public void addMainParticipant(final String confID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "DRingService.addMainParticipant() thread running...");
                    Ringservice.addMainParticipant(confID);
                }
            });

        }

        @Override
        public void detachParticipant(final String callID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "DRingService.detachParticipant() thread running... " + callID);
                    Ringservice.detachParticipant(callID);
                }
            });

        }

        @Override
        public void joinConference(final String sel_confID, final String drag_confID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "DRingService.joinConference() thread running...");
                    Ringservice.joinConference(sel_confID, drag_confID);
                }
            });

        }

        @Override
        public void hangUpConference(final String confID) throws RemoteException {
            Log.e(TAG, "HANGING UP CONF");
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "DRingService.hangUpConference() thread running...");
                    Ringservice.hangUpConference(confID);
                }
            });

        }

        @Override
        public void holdConference(final String confID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "DRingService.holdConference() thread running...");
                    Ringservice.holdConference(confID);
                }
            });

        }

        @Override
        public void unholdConference(final String confID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "DRingService.unholdConference() thread running...");
                    Ringservice.unholdConference(confID);
                }
            });

        }

        @Override
        public boolean isConferenceParticipant(final String callID) throws RemoteException {
            return getExecutor().executeAndReturn(new SipRunnableWithReturn<Boolean>() {
                @Override
                protected Boolean doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.isRecording() thread running...");
                    return Ringservice.isConferenceParticipant(callID);
                }
            });
        }

        @Override
        public Map<String, ArrayList<String>> getConferenceList() throws RemoteException {
            return getExecutor().executeAndReturn(new SipRunnableWithReturn<Map<String, ArrayList<String>>>() {
                @Override
                protected Map<String, ArrayList<String>> doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.getConferenceList() thread running...");
                    StringVect call_ids = Ringservice.getCallList();
                    HashMap<String, ArrayList<String>> confs = new HashMap<>(call_ids.size());
                    for (int i=0; i<call_ids.size(); i++) {
                        String call_id = call_ids.get(i);
                        String conf_id = Ringservice.getConferenceId(call_id);
                        if (conf_id == null || conf_id.isEmpty())
                            conf_id = call_id;
                        ArrayList<String> calls = confs.get(conf_id);
                        if (calls == null) {
                            calls = new ArrayList<>();
                            confs.put(conf_id, calls);
                        }
                        calls.add(call_id);
                    }
                    return confs;
                }
            });
        }

        @Override
        public List<String> getParticipantList(final String confID) throws RemoteException {
            return getExecutor().executeAndReturn(new SipRunnableWithReturn<List<String>>() {
                @Override
                protected List<String> doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.getParticipantList() thread running...");
                    return new ArrayList<>(Ringservice.getParticipantList(confID));
                }
            });
        }

        @Override
        public String getConferenceId(String callID) throws RemoteException {
            Log.e(TAG, "getConferenceId not implemented");
            return Ringservice.getConferenceId(callID);
        }

        @Override
        public String getConferenceDetails(final String callID) throws RemoteException {
            return getExecutor().executeAndReturn(new SipRunnableWithReturn<String>() {
                @Override
                protected String doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.getConferenceDetails() thread running...");
                    return Ringservice.getConferenceDetails(callID).get("CONF_STATE");
                }
            });
        }

        @Override
        public String getRecordPath() throws RemoteException {
            return getExecutor().executeAndReturn(new SipRunnableWithReturn<String>() {
                @Override
                protected String doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.getRecordPath() thread running...");
                    return Ringservice.getRecordPath();
                }
            });
        }

        @Override
        public boolean toggleRecordingCall(final String id) throws RemoteException {
            return getExecutor().executeAndReturn(new SipRunnableWithReturn<Boolean>() {
                @Override
                protected Boolean doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.toggleRecordingCall() thread running...");
                    return Ringservice.toggleRecording(id);
                }
            });
        }

        @Override
        public boolean startRecordedFilePlayback(final String filepath) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "DRingService.setRecordingCall() thread running...");
                    Ringservice.startRecordedFilePlayback(filepath);
                }
            });
            return false;
        }

        @Override
        public void stopRecordedFilePlayback(final String filepath) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "DRingService.stopRecordedFilePlayback() thread running...");
                    Ringservice.stopRecordedFilePlayback(filepath);
                }
            });
        }

        @Override
        public void setRecordPath(final String path) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "DRingService.setRecordPath() " + path + " thread running...");
                    Ringservice.setRecordPath(path);
                }
            });
        }

        @Override
        public void sendTextMessage(final String callID, final String msg) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "DRingService.sendTextMessage() thread running...");
                    StringMap messages  = new StringMap();
                    messages.setRaw("text/plain", Blob.fromString(msg));
                    Ringservice.sendTextMessage(callID, messages, "", false);
                }
            });
        }

        @Override
        public long sendAccountTextMessage(final String accountID, final String to, final String msg) {
            return getExecutor().executeAndReturn(new SipRunnableWithReturn<Long>() {
                @Override
                protected Long doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "DRingService.sendAccountTextMessage() thread running... " + accountID + " " + to + " " + msg);
                    StringMap msgs = new StringMap();
                    msgs.setRaw("text/plain", Blob.fromString(msg));
                    return Ringservice.sendAccountTextMessage(accountID, to, msgs);
                }
            });
        }

        @Override
        public List<Codec> getCodecList(final String accountID) throws RemoteException {
            return getExecutor().executeAndReturn(new SipRunnableWithReturn<ArrayList<Codec>>() {
                @Override
                protected ArrayList<Codec> doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.getCodecList() thread running...");
                    ArrayList<Codec> results = new ArrayList<>();

                    UintVect active_payloads = Ringservice.getActiveCodecList(accountID);
                    for (int i = 0; i < active_payloads.size(); ++i) {
                        Log.i(TAG, "DRingService.getCodecDetails(" + accountID +", "+ active_payloads.get(i) +")");
                        results.add(new Codec(active_payloads.get(i), Ringservice.getCodecDetails(accountID, active_payloads.get(i)), true));

                    }
                    UintVect payloads = Ringservice.getCodecList();

                    cl : for (int i = 0; i < payloads.size(); ++i) {
                        for (Codec co : results)
                            if (co.getPayload() == payloads.get(i))
                                continue cl;
                        StringMap details = Ringservice.getCodecDetails(accountID, payloads.get(i));
                        if (details.size() > 1)
                            results.add(new Codec(payloads.get(i), details, false));
                        else
                            Log.i(TAG, "Error loading codec " + i);
                    }
                    return results;
                }
            });
        }

        /*
        @Override
        public Map getRingtoneList() throws RemoteException {
            class RingtoneList extends SipRunnableWithReturn {

                @Override
                protected StringMap doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.getRingtoneList() thread running...");
                    return Ringservice.getR();
                }
            }

            RingtoneList runInstance = new RingtoneList();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
            }
            StringMap ringtones = (StringMap) runInstance.getVal();

            for (int i = 0; i < ringtones.size(); ++i) {
                // Log.i(TAG,"ringtones "+i+" "+ ringtones.);
            }

            return null;
        }


        @Override
        public boolean checkForPrivateKey(final String pemPath) throws RemoteException {
            class hasPrivateKey extends SipRunnableWithReturn {

                @Override
                protected Boolean doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.isCaptureMuted() thread running...");
                    return Ringservice.sflph_config_check_for_private_key(pemPath);
                }
            }

            hasPrivateKey runInstance = new hasPrivateKey();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
            }

            return (Boolean) runInstance.getVal();
        }

        @Override
        public boolean checkCertificateValidity(final String pemPath) throws RemoteException {
            class isValid extends SipRunnableWithReturn {

                @Override
                protected Boolean doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.isCaptureMuted() thread running...");
                    return Ringservice.sflph_config_check_certificate_validity(pemPath, pemPath);
                }
            }

            isValid runInstance = new isValid();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
            }

            return (Boolean) runInstance.getVal();
        }

        @Override
        public boolean checkHostnameCertificate(final String certificatePath, final String host, final String port) throws RemoteException {
            class isValid extends SipRunnableWithReturn {

                @Override
                protected Boolean doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.isCaptureMuted() thread running...");
                    return Ringservice.sflph_config_check_hostname_certificate(host, port);
                }
            }

            isValid runInstance = new isValid();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
            }

            return (Boolean) runInstance.getVal();
        }
*/

        @Override
        public Map<String, String> validateCertificatePath(final String accountID, final String certificatePath, final String privateKeyPath, final String privateKeyPass) throws RemoteException {
            return getExecutor().executeAndReturn(new SipRunnableWithReturn<Map<String, String>>() {
                @Override
                protected Map<String, String> doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.validateCertificatePath() thread running...");
                    return Ringservice.validateCertificatePath(accountID, certificatePath, privateKeyPath, "", "").toNative();
                }
            });
        }

        @Override
        public Map<String, String> validateCertificate(final String accountID, final String certificate) throws RemoteException {
            return getExecutor().executeAndReturn(new SipRunnableWithReturn<Map<String, String>>() {
                @Override
                protected Map<String, String> doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.validateCertificate() thread running...");
                    return Ringservice.validateCertificate(accountID, certificate).toNative();
                }
            });
        }

        @Override
        public Map<String, String> getCertificateDetailsPath(final String certificatePath) throws RemoteException {
            return getExecutor().executeAndReturn(new SipRunnableWithReturn<Map<String, String>>() {
                @Override
                protected Map<String, String> doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.getCertificateDetailsPath() thread running...");
                    return Ringservice.getCertificateDetails(certificatePath).toNative();
                }
            });
        }

        @Override
        public Map<String, String> getCertificateDetails(final String certificateRaw) throws RemoteException {
            return getExecutor().executeAndReturn(new SipRunnableWithReturn<Map<String, String>>() {
                @Override
                protected Map<String, String> doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.getCertificateDetails() thread running...");
                    return Ringservice.getCertificateDetails(certificateRaw).toNative();
                }
            });
        }

        @Override
        public void setActiveCodecList(final List codecs, final String accountID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "DRingService.setActiveCodecList() thread running...");
                    UintVect list = new UintVect(codecs.size());
                    for (Object codec : codecs)
                        list.add((Long) codec);
                    Ringservice.setActiveCodecList(accountID, list);
                }
            });
        }

        @Override
        public void playDtmf(final String key) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "DRingService.playDtmf() thread running...");
                    Ringservice.playDTMF(key);
                }
            });
        }

        @Override
        public Map<String, String> getConference(final String id) throws RemoteException {
            return getExecutor().executeAndReturn(new SipRunnableWithReturn<Map<String, String>>() {
                @Override
                protected Map<String, String> doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.getCredentials() thread running...");
                    return Ringservice.getConferenceDetails(id).toNative();
                }
            });
        }

        @Override
        public void setMuted(final boolean mute) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "DRingService.setMuted() thread running...");
                    Ringservice.muteCapture(mute);
                }
            });
        }

        @Override
        public boolean isCaptureMuted() throws RemoteException {
            return getExecutor().executeAndReturn(new SipRunnableWithReturn<Boolean>() {

                @Override
                protected Boolean doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.isCaptureMuted() thread running...");
                    return Ringservice.isCaptureMuted();
                }
            });
        }

        @Override
        public List<String> getTlsSupportedMethods(){
            Log.i(TAG, "DRingService.getTlsSupportedMethods()");
            return SwigNativeConverter.convertSwigToNative(Ringservice.getSupportedTlsMethod());
        }

        @Override
        public List getCredentials(final String accountID) throws RemoteException {
            class Credentials extends SipRunnableWithReturn {

                @Override
                protected List doRun() throws SameThreadException {
                    Log.i(TAG, "DRingService.getCredentials() thread running...");
                    return Ringservice.getCredentials(accountID).toNative();
                }
            }

            Credentials runInstance = new Credentials();
            getExecutor().executeSynced(runInstance);
            return (List) runInstance.getVal();
        }

        @Override
        public void setCredentials(final String accountID, final List creds) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "DRingService.setCredentials() thread running...");
                    Ringservice.setCredentials(accountID, SwigNativeConverter.convertFromNativeToSwig(creds));
                }
            });
        }

        @Override
        public void registerAllAccounts() throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "DRingService.registerAllAccounts() thread running...");
                    Ringservice.registerAllAccounts();
                }
            });
        }

        public void videoSurfaceAdded(String id)
        {
            Log.d(TAG, "DRingService.videoSurfaceAdded() " + id);
            Shm shm = videoInputs.get(id);
            SurfaceHolder holder = videoSurfaces.get(id).get();
            if (shm != null && holder != null && shm.window == 0)
                startVideo(shm, holder);
        }

        public void videoSurfaceRemoved(String id)
        {
            Log.d(TAG, "DRingService.videoSurfaceRemoved() " + id);
            Shm shm = videoInputs.get(id);
            if (shm != null)
                stopVideo(shm);
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
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    int cam_id = (front ? videoManagerCallback.cameraFront : videoManagerCallback.cameraBack);
                    String uri = "camera://" + cam_id;
                    Log.i(TAG, "DRingService.switchInput() " + uri);
                    Ringservice.applySettings(id, videoManagerCallback.getNativeParams(cam_id).toMap(getResources().getConfiguration().orientation));
                    Ringservice.switchInput(id, uri);
                }
            });
        }

        public void setPreviewSettings() {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    for (int i=0, n=Camera.getNumberOfCameras(); i<n; i++) {
                        Ringservice.applySettings(Integer.toString(i), videoManagerCallback.getNativeParams(i).toMap(getResources().getConfiguration().orientation));
                    }
                }
            });
        }

        public int exportAccounts(final List accountIDs, final String toDir, final String password) {
            return getExecutor().executeAndReturn(new SipRunnableWithReturn<Integer>() {
                @Override
                protected Integer doRun() throws SameThreadException, RemoteException {
                    StringVect ids = new StringVect();
                    for (Object s : accountIDs)
                        ids.add((String)s);
                    return Ringservice.exportAccounts(ids, toDir, password);
                }
            });
        }

        public int importAccounts(final String archivePath, final String password) {
            return getExecutor().executeAndReturn(new SipRunnableWithReturn<Integer>() {
                @Override
                protected Integer doRun() throws SameThreadException, RemoteException {
                    return Ringservice.importAccounts(archivePath, password);
                }
            });
        }

        public void connectivityChanged() {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Ringservice.connectivityChanged();
                }
            });
        }
    };
}
