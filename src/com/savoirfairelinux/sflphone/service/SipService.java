/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
 *
 *  Author: Regis Montoya <r3gis.3R@gmail.com>
 *  Author: Emeric Vigier <emeric.vigier@savoirfairelinux.com>
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
package com.savoirfairelinux.sflphone.service;

import java.lang.ref.WeakReference;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.savoirfairelinux.sflphone.service.ManagerImpl;
import com.savoirfairelinux.sflphone.client.SFLphoneApplication;
import com.savoirfairelinux.sflphone.service.ISipService;

public class SipService extends Service {

    static final String TAG = "SipService";
    static final int DELAY = 5000; /* 5 sec */
    private boolean runFlag = false;
    private SipServiceThread sipServiceThread;
    private SFLphoneApplication sflphoneApp;
    private SipServiceExecutor mExecutor;
    private static HandlerThread executorThread;
    private CallManagerJNI callManagerJNI;
    private CallManagerCallBack callManagerCallBack;
    private ManagerImpl managerImpl;
    private boolean isPjSipStackStarted = false;

    /* Implement public interface for the service */
    private final ISipService.Stub mBinder = new ISipService.Stub() {

        @Override
        public void placeCall(final String accountID, final String callID, final String to) {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.placeCall() thread running...");
                    callManagerJNI.placeCall(accountID, callID, to);
                }
            });
        }

        @Override
        public void refuse(final String callID) {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.refuse() thread running...");
                    callManagerJNI.refuse(callID);
                }
            });
        }

        @Override
        public void accept(final String callID) {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.placeCall() thread running...");
                    callManagerJNI.accept(callID);
                }
            });
        }

        @Override
        public void hangUp(final String callID) {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.hangUp() thread running...");
                    callManagerJNI.hangUp(callID);
                }
            });
        }
    };

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public SipService getService() {
            // Return this instance of LocalService so clients can call public methods
            return SipService.this;
        }
    }

    /* called once by startService() */
    @Override
    public void onCreate() {
        Log.i(TAG, "onCreated");
        super.onCreate();
        sflphoneApp = (SFLphoneApplication) getApplication();
        sipServiceThread = new SipServiceThread();
        getExecutor().execute(new StartRunnable());
    }

    /* called for each startService() */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStarted");
        super.onStartCommand(intent, flags, startId);

        runFlag = true;
        sipServiceThread.start();
        sflphoneApp.setServiceRunning(true);
        Toast.makeText(this, "Sflphone Service started", Toast.LENGTH_SHORT).show();

        Log.i(TAG, "onStarted");
        return START_STICKY; /* started and stopped explicitly */
    }

    @Override
    public void onDestroy() {
        /* called once by stopService() */
        super.onDestroy();
        runFlag = false;
        sipServiceThread.interrupt();
        sipServiceThread = null;
        sflphoneApp.setServiceRunning(false);
        Toast.makeText(this, "Sflphone Service stopped", Toast.LENGTH_SHORT).show();
        
        Log.i(TAG, "onDestroyed");
    }

    @Override
    public IBinder onBind(Intent arg0) {
        Log.i(TAG, "onBound");
        return mBinder;
    }

    private static Looper createLooper() {
        if(executorThread == null) {
            Log.d(TAG, "Creating new handler thread");
            // ADT gives a fake warning due to bad parse rule.
            executorThread = new HandlerThread("SipService.Executor");
            executorThread.start();
        }
        return executorThread.getLooper();
    }

    public SipServiceExecutor getExecutor() {
        // create mExecutor lazily
        if (mExecutor == null) {
            mExecutor = new SipServiceExecutor(this);
        }
        return mExecutor;
    }

    // Executes immediate tasks in a single executorThread.
    public static class SipServiceExecutor extends Handler {
        WeakReference<SipService> handlerService;

        SipServiceExecutor(SipService s) {
            super(createLooper());
            handlerService = new WeakReference<SipService>(s);
        }

        public void execute(Runnable task) {
            // TODO: add wakelock
            Message.obtain(this, 0/* don't care */, task).sendToTarget();
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
    }

    private void startPjSipStack() throws SameThreadException {
        if (isPjSipStackStarted)
            return;

        try {
            System.loadLibrary("gnustl_shared");
            System.loadLibrary("expat");
            System.loadLibrary("yaml");
            System.loadLibrary("ccgnu2");
            System.loadLibrary("crypto");
            System.loadLibrary("ssl");
            System.loadLibrary("ccrtp1");
            System.loadLibrary("dbus");
            System.loadLibrary("dbus-c++-1");
            System.loadLibrary("samplerate");
            System.loadLibrary("codec_ulaw");
            System.loadLibrary("codec_alaw");
            System.loadLibrary("speexresampler");
            System.loadLibrary("sflphone");
            isPjSipStackStarted = true;
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Problem with the current Pj stack...", e);
            isPjSipStackStarted = false;
            return;
        } catch (Exception e) {
            Log.e(TAG, "Problem with the current Pj stack...", e);
        }

        /* get unique instance of managerImpl */
        managerImpl = SFLPhoneservice.instance();
        Log.i(TAG, "ManagerImpl::instance() = " + managerImpl);
        /* set static AppPath before calling manager.init */
        managerImpl.setPath(sflphoneApp.getAppPath());
        callManagerJNI = new CallManagerJNI();
        Log.i(TAG, "startPjSipStack() callManagerJNI = " + callManagerJNI);

        callManagerCallBack = new CallManagerCallBack();
        SFLPhoneservice.setCallbackObject(callManagerCallBack);
        Log.i(TAG, "callManagerCallBack = " + callManagerCallBack);

        managerImpl.init("");
        return;
    }

    // Enforce same thread contract to ensure we do not call from somewhere else
    public class SameThreadException extends Exception {
        private static final long serialVersionUID = -905639124232613768L;

        public SameThreadException() {
            super("Should be launched from a single worker thread");
        }
    }

    public abstract static class SipRunnable implements Runnable {
        protected abstract void doRun() throws SameThreadException;

        public void run() {
            try {
                doRun();
            }catch(SameThreadException e) {
                Log.e(TAG, "Not done from same thread");
            }
        }
    }

    class StartRunnable extends SipRunnable {
        @Override
        protected void doRun() throws SameThreadException {
            startPjSipStack();
        }
    }

    private class SipServiceThread extends Thread {
        
        public SipServiceThread() {
            super("sipServiceThread");
        }
        
        @Override
        public void run() {
            Log.i(TAG, "SipService thread running...");
            SipService sipService = SipService.this;
            while(sipService.runFlag) {
                try {
                    Thread.sleep(DELAY);
                } catch (InterruptedException e) {
                    sipService.runFlag = false;
                    Log.w(TAG, "service thread interrupted!");
                }
            }
        }
    }
}
