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

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

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
    private ConfigurationManagerJNI configurationManagerJNI;
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

        @Override
        public void setAudioPlugin(final String audioPlugin) {
            getExecutor().execute(new SipRunnable() {
               @Override
               protected void doRun() throws SameThreadException {
                   Log.i(TAG, "SipService.setAudioPlugin() thread running...");
                   configurationManagerJNI.setAudioPlugin(audioPlugin);
               }
           });
        }

        @Override
        public ArrayList<String> getAccountList() {
            StringVect swigvect = configurationManagerJNI.getAccountList();
            ArrayList<String> nativelist = new ArrayList<String>();

            for(int i = 0; i < swigvect.size(); i++)
               nativelist.add(swigvect.get(i));

            return nativelist;
        } 

        @Override
        public HashMap<String,String> getAccountDetails(final String accountID) {
            StringMap swigmap = configurationManagerJNI.getAccountDetails(accountID);

            Log.i(TAG, "===================== Swig Map Size " + swigmap.size());

            HashMap<String, String> nativemap = new HashMap<String, String>();

            nativemap.put(ServiceConstants.CONFIG_ACCOUNT_ALIAS, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_ALIAS));
            nativemap.put(ServiceConstants.CONFIG_ACCOUNT_HOSTNAME, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_HOSTNAME));
            nativemap.put(ServiceConstants.CONFIG_ACCOUNT_USERNAME, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_USERNAME));
            nativemap.put(ServiceConstants.CONFIG_ACCOUNT_ROUTESET, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_ROUTESET));
            nativemap.put(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_EXPIRE, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_EXPIRE));
            nativemap.put(ServiceConstants.CONFIG_LOCAL_INTERFACE, swigmap.get(ServiceConstants.CONFIG_LOCAL_INTERFACE));
            nativemap.put(ServiceConstants.CONFIG_STUN_SERVER, swigmap.get(ServiceConstants.CONFIG_STUN_SERVER));
            nativemap.put(ServiceConstants.CONFIG_TLS_ENABLE, swigmap.get(ServiceConstants.CONFIG_TLS_ENABLE));
            nativemap.put(ServiceConstants.CONFIG_SRTP_ENABLE, swigmap.get(ServiceConstants.CONFIG_SRTP_ENABLE));

            nativemap.put(ServiceConstants.CONFIG_ACCOUNT_TYPE, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_TYPE));
            nativemap.put(ServiceConstants.CONFIG_ACCOUNT_MAILBOX, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_MAILBOX));
            nativemap.put(ServiceConstants.CONFIG_ACCOUNT_ENABLE, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_ENABLE));
            nativemap.put(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATUS, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATUS));
            nativemap.put(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATE_CODE, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATE_CODE));
            nativemap.put(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATE_DESC, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATE_DESC));

            nativemap.put(ServiceConstants.CONFIG_ACCOUNT_AUTOANSWER, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_AUTOANSWER));
            nativemap.put(ServiceConstants.CONFIG_ACCOUNT_DTMF_TYPE, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_DTMF_TYPE));
            nativemap.put(ServiceConstants.CONFIG_KEEP_ALIVE_ENABLED, swigmap.get(ServiceConstants.CONFIG_KEEP_ALIVE_ENABLED));
            nativemap.put(ServiceConstants.CONFIG_LOCAL_PORT, swigmap.get(ServiceConstants.CONFIG_LOCAL_PORT));
            nativemap.put(ServiceConstants.CONFIG_PUBLISHED_ADDRESS, swigmap.get(ServiceConstants.CONFIG_PUBLISHED_ADDRESS));

            nativemap.put(ServiceConstants.CONFIG_PUBLISHED_PORT, swigmap.get(ServiceConstants.CONFIG_PUBLISHED_PORT));
            nativemap.put(ServiceConstants.CONFIG_PUBLISHED_SAMEAS_LOCAL, swigmap.get(ServiceConstants.CONFIG_PUBLISHED_SAMEAS_LOCAL));
            nativemap.put(ServiceConstants.CONFIG_RINGTONE_ENABLED, swigmap.get(ServiceConstants.CONFIG_RINGTONE_ENABLED));
            nativemap.put(ServiceConstants.CONFIG_RINGTONE_PATH, swigmap.get(ServiceConstants.CONFIG_RINGTONE_PATH));
            nativemap.put(ServiceConstants.CONFIG_ACCOUNT_USERAGENT, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_USERAGENT));
            
            nativemap.put(ServiceConstants.CONFIG_SRTP_KEY_EXCHANGE, swigmap.get(ServiceConstants.CONFIG_SRTP_KEY_EXCHANGE));
            nativemap.put(ServiceConstants.CONFIG_SRTP_RTP_FALLBACK, swigmap.get(ServiceConstants.CONFIG_SRTP_RTP_FALLBACK));
            nativemap.put(ServiceConstants.CONFIG_STUN_ENABLE, swigmap.get(ServiceConstants.CONFIG_STUN_ENABLE));
            nativemap.put(ServiceConstants.CONFIG_TLS_CERTIFICATE_FILE, swigmap.get(ServiceConstants.CONFIG_TLS_CERTIFICATE_FILE));
            nativemap.put(ServiceConstants.CONFIG_TLS_CA_LIST_FILE, swigmap.get(ServiceConstants.CONFIG_TLS_CA_LIST_FILE));

            nativemap.put(ServiceConstants.CONFIG_TLS_CIPHERS, swigmap.get(ServiceConstants.CONFIG_TLS_CIPHERS));
            nativemap.put(ServiceConstants.CONFIG_TLS_LISTENER_PORT, swigmap.get(ServiceConstants.CONFIG_TLS_LISTENER_PORT));
            nativemap.put(ServiceConstants.CONFIG_TLS_METHOD, swigmap.get(ServiceConstants.CONFIG_TLS_METHOD));
            nativemap.put(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC, swigmap.get(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_MSEC));
            nativemap.put(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC, swigmap.get(ServiceConstants.CONFIG_TLS_NEGOTIATION_TIMEOUT_SEC));

            nativemap.put(ServiceConstants.CONFIG_TLS_PASSWORD, swigmap.get(ServiceConstants.CONFIG_TLS_PASSWORD));
            nativemap.put(ServiceConstants.CONFIG_TLS_PRIVATE_KEY_FILE, swigmap.get(ServiceConstants.CONFIG_TLS_PRIVATE_KEY_FILE));
            nativemap.put(ServiceConstants.CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE, swigmap.get(ServiceConstants.CONFIG_TLS_REQUIRE_CLIENT_CERTIFICATE));
            nativemap.put(ServiceConstants.CONFIG_TLS_SERVER_NAME, swigmap.get(ServiceConstants.CONFIG_TLS_SERVER_NAME));
            nativemap.put(ServiceConstants.CONFIG_TLS_VERIFY_CLIENT, swigmap.get(ServiceConstants.CONFIG_TLS_VERIFY_CLIENT));

            nativemap.put(ServiceConstants.CONFIG_TLS_VERIFY_SERVER, swigmap.get(ServiceConstants.CONFIG_TLS_VERIFY_SERVER));
            nativemap.put(ServiceConstants.CONFIG_ZRTP_DISPLAY_SAS, swigmap.get(ServiceConstants.CONFIG_ZRTP_DISPLAY_SAS));
            nativemap.put(ServiceConstants.CONFIG_ZRTP_DISPLAY_SAS_ONCE, swigmap.get(ServiceConstants.CONFIG_ZRTP_DISPLAY_SAS_ONCE));
            nativemap.put(ServiceConstants.CONFIG_ZRTP_HELLO_HASH, swigmap.get(ServiceConstants.CONFIG_ZRTP_HELLO_HASH));
            nativemap.put(ServiceConstants.CONFIG_ZRTP_NOT_SUPP_WARNING, swigmap.get(ServiceConstants.CONFIG_ZRTP_NOT_SUPP_WARNING));

            /*
            nativemap.put(ServiceConstants.CONFIG_CREDENTIAL_NUMBER, swigmap.get(ServiceConstants.CONFIG_CREDENTIAL_NUMBER));
            nativemap.put(ServiceConstants.CONFIG_ACCOUNT_PASSWORD, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_PASSWORD));
            nativemap.put(ServiceConstants.CONFIG_ACCOUNT_REALM, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_REALM));
            */

            /*
            nativemap.put(ServiceConstants.CONFIG_ACCOUNT_DEFAULT_REALM, swigmap.get(ServiceConstants.CONFIG_ACCOUNT_DEFAULT_REALM));
            nativemap.put(ServiceConstants.CONFIG_INTERFACE, swigmap.get(ServiceConstants.CONFIG_INTERFACE));
            nativemap.put(ServiceConstants.CONFIG_DEFAULT_INTERFACE, swigmap.get(ServiceConstants.CONFIG_DEFAULT_INTERFACE));
            nativemap.put(ServiceConstants.CONFIG_DISPLAY_NAME, swigmap.get(ServiceConstants.CONFIG_DISPLAY_NAME));
            nativemap.put(ServiceConstants.CONFIG_DEFAULT_ADDRESS, swigmap.get(ServiceConstants.CONFIG_DEFAULT_ADDRESS));
            nativemap.put(ServiceConstants.CONFIG_SRTP_ENCRYPTION_ALGO, swigmap.get(ServiceConstants.CONFIG_SRTP_ENCRYPTION_ALGO));
            */

            return nativemap;
        }

        @Override
        public void setAccountDetails(String accountId, Map map) {
            HashMap<String,String> nativemap = (HashMap<String,String>) map;

            StringMap swigmap = new StringMap();

            swigmap.set(ServiceConstants.CONFIG_ACCOUNT_ALIAS, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_ALIAS));
            swigmap.set(ServiceConstants.CONFIG_ACCOUNT_HOSTNAME, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_HOSTNAME));
            swigmap.set(ServiceConstants.CONFIG_ACCOUNT_USERNAME, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_USERNAME));
            swigmap.set(ServiceConstants.CONFIG_ACCOUNT_ROUTESET, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_ROUTESET));
            swigmap.set(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_EXPIRE, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_EXPIRE));
            swigmap.set(ServiceConstants.CONFIG_LOCAL_INTERFACE, nativemap.get(ServiceConstants.CONFIG_LOCAL_INTERFACE));
            swigmap.set(ServiceConstants.CONFIG_STUN_SERVER, nativemap.get(ServiceConstants.CONFIG_STUN_SERVER));
            swigmap.set(ServiceConstants.CONFIG_TLS_ENABLE, nativemap.get(ServiceConstants.CONFIG_TLS_ENABLE));
            swigmap.set(ServiceConstants.CONFIG_SRTP_ENABLE, nativemap.get(ServiceConstants.CONFIG_SRTP_ENABLE));

            swigmap.set(ServiceConstants.CONFIG_ACCOUNT_TYPE, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_TYPE));
            swigmap.set(ServiceConstants.CONFIG_ACCOUNT_ALIAS, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_ALIAS));
            swigmap.set(ServiceConstants.CONFIG_ACCOUNT_MAILBOX, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_MAILBOX));
            swigmap.set(ServiceConstants.CONFIG_ACCOUNT_ENABLE, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_ENABLE));
            swigmap.set(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_EXPIRE, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_EXPIRE));
            swigmap.set(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATUS, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATUS));
            swigmap.set(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATE_CODE, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATE_CODE));
            swigmap.set(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATE_DESC, nativemap.get(ServiceConstants.CONFIG_ACCOUNT_REGISTRATION_STATE_DESC));

            configurationManagerJNI.setAccountDetails(accountId, swigmap);
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

        configurationManagerJNI = new ConfigurationManagerJNI();

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
