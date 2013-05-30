/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
 *
 *  Author: Regis Montoya <r3gis.3R@gmail.com>
 *  Author: Emeric Vigier <emeric.vigier@savoirfairelinux.com>
 *          Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.account.AccountDetailsHandler;
import com.savoirfairelinux.sflphone.account.AudioHandler;
import com.savoirfairelinux.sflphone.account.HistoryHandler;
import com.savoirfairelinux.sflphone.client.SFLPhoneHomeActivity;
import com.savoirfairelinux.sflphone.client.SFLphoneApplication;
import com.savoirfairelinux.sflphone.client.receiver.IncomingReceiver;
import com.savoirfairelinux.sflphone.model.SipCall;

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
    private ConfigurationManagerCallback configurationManagerCallback;
    private ManagerImpl managerImpl;
    private boolean isPjSipStackStarted = false;
    
    public static final String NOTIF_CREATION = "notif_creation";
    public static final String NOTIF_DELETION = "notif_deletion";

    private HashMap<String, SipCall> current_calls = new HashMap<String, SipCall>();

    private IncomingReceiver receiver = new IncomingReceiver(this);

    @Override
    public boolean onUnbind(Intent i) {
        super.onUnbind(i);
        Log.i(TAG, "onUnbind(intent)");
        return false;

    }

    /* called once by startService() */
    @Override
    public void onCreate() {
        Log.i(TAG, "onCreated");
        super.onCreate();

        sflphoneApp = (SFLphoneApplication) getApplication();
        sipServiceThread = new SipServiceThread();
        
        

        IntentFilter callFilter = new IntentFilter(CallManagerCallBack.CALL_STATE_CHANGED);
        callFilter.addAction(CallManagerCallBack.INCOMING_CALL);
        callFilter.addAction(CallManagerCallBack.NEW_CALL_CREATED);
        callFilter.addAction(ConfigurationManagerCallback.ACCOUNT_STATE_CHANGED);
        callFilter.addAction(ConfigurationManagerCallback.ACCOUNTS_CHANGED);
        callFilter.addAction(ConfigurationManagerCallback.ACCOUNTS_LOADED);
        callFilter.addAction(CallManagerCallBack.INCOMING_TEXT);

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, callFilter);
        
        getExecutor().execute(new StartRunnable());
        
    }
    


    /* called for each startService() */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStarted");
        super.onStartCommand(intent, flags, startId);

        if (!runFlag) {
            sipServiceThread.start();
            runFlag = true;
            sflphoneApp.setServiceRunning(true);
            Toast.makeText(this, "Sflphone Service started", Toast.LENGTH_SHORT).show();
        }

        return START_STICKY; /* started and stopped explicitly */
    }

    @Override
    public void onDestroy() {
        /* called once by stopService() */
        sipServiceThread.interrupt();
        sipServiceThread = null;
        runFlag = false;
        unregisterReceiver(receiver);

        sflphoneApp.setServiceRunning(false);
        Toast.makeText(this, "Sflphone Service stopped", Toast.LENGTH_SHORT).show();
        super.onDestroy();

        Log.i(TAG, "onDestroyed");
    }

    @Override
    public IBinder onBind(Intent arg0) {
        Log.i(TAG, "onBound");
        return mBinder;
    }

    private static Looper createLooper() {
        if (executorThread == null) {
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
            System.loadLibrary("codec_g722");
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

        /* set static AppPath before calling manager.init */
        managerImpl.setPath(sflphoneApp.getAppPath());

        callManagerJNI = new CallManagerJNI();
        callManagerCallBack = new CallManagerCallBack(this);
        SFLPhoneservice.setCallbackObject(callManagerCallBack);

        configurationManagerJNI = new ConfigurationManagerJNI();
        configurationManagerCallback = new ConfigurationManagerCallback(this);
        SFLPhoneservice.setConfigurationCallbackObject(configurationManagerCallback);

        managerImpl.init("");
        return;
    }

    public HashMap<String, SipCall> getCurrent_calls() {
        return current_calls;
    }

    public void setCurrent_calls(HashMap<String, SipCall> current_calls) {
        this.current_calls = current_calls;
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

    public abstract static class SipRunnableWithReturn implements Runnable {
        Object obj = null;
        boolean done = false;

        protected abstract Object doRun() throws SameThreadException;

        public Object getVal() {
            return obj;
        }

        public boolean isDone() {
            return done;
        }

        public void run() {
            try {
                obj = doRun();
                done = true;
            } catch (SameThreadException e) {
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
            while (sipService.runFlag) {
                try {
                    Thread.sleep(DELAY);
                } catch (InterruptedException e) {
                    sipService.runFlag = false;
                    Log.w(TAG, "service thread interrupted!");
                }
            }
        }
    }

    /* ************************************
     * 
     * Implement public interface for the service
     * 
     * 
     * **********************************
     */
    private final ISipService.Stub mBinder = new ISipService.Stub() {

        @Override
        public void placeCall(final SipCall call) {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.placeCall() thread running...");
                    callManagerJNI.placeCall(call.getAccountID(), call.getCallId(), call.getContacts().get(0).getPhones().get(0).getNumber());
                    getCurrent_calls().put(call.getCallId(), call);
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
        public void hold(final String callID) {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.hold() thread running...");
                    callManagerJNI.hold(callID);
                }
            });
        }

        @Override
        public void unhold(final String callID) {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.unhold() thread running...");
                    callManagerJNI.unhold(callID);
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
        public String getCurrentAudioOutputPlugin() {
            class CurrentAudioPlugin extends SipRunnableWithReturn {
                @Override
                protected String doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.getCurrentAudioOutputPlugin() thread running...");
                    return configurationManagerJNI.getCurrentAudioOutputPlugin();
                }
            }
            ;

            CurrentAudioPlugin runInstance = new CurrentAudioPlugin();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
                // Log.e(TAG, "Waiting for Nofing");
            }
            return (String) runInstance.getVal();
        }

        @Override
        public ArrayList<String> getAccountList() {
            class AccountList extends SipRunnableWithReturn {
                @Override
                protected StringVect doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.getAccountList() thread running...");
                    return configurationManagerJNI.getAccountList();
                }
            }
            ;
            AccountList runInstance = new AccountList();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
                // Log.e(TAG, "Waiting for Nofing");
            }
            StringVect swigvect = (StringVect) runInstance.getVal();

            ArrayList<String> nativelist = new ArrayList<String>();

            for (int i = 0; i < swigvect.size(); i++)
                nativelist.add(swigvect.get(i));

            return nativelist;
        }

        @Override
        public HashMap<String, String> getAccountDetails(final String accountID) {
            class AccountDetails extends SipRunnableWithReturn {
                private String id;

                AccountDetails(String accountId) {
                    id = accountId;
                }

                @Override
                protected StringMap doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.getAccountDetails() thread running...");
                    return configurationManagerJNI.getAccountDetails(id);
                }
            }

            AccountDetails runInstance = new AccountDetails(accountID);
            getExecutor().execute(runInstance);

            while (!runInstance.isDone()) {
            }
            StringMap swigmap = (StringMap) runInstance.getVal();

            HashMap<String, String> nativemap = AccountDetailsHandler.convertSwigToNative(swigmap);

            return nativemap;
        }

        @Override
        public void setAccountDetails(final String accountId, Map map) {
            HashMap<String, String> nativemap = (HashMap<String, String>) map;

            final StringMap swigmap = AccountDetailsHandler.convertFromNativeToSwig(nativemap);

            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.setAccountDetails() thread running...");
                    configurationManagerJNI.setAccountDetails(accountId, swigmap);
                }
            });
        }

        @Override
        public String addAccount(Map map) {
            class AddAccount extends SipRunnableWithReturn {
                StringMap map;

                AddAccount(StringMap m) {
                    map = m;
                }

                @Override
                protected String doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.getAccountDetails() thread running...");
                    return configurationManagerJNI.addAccount(map);
                }
            }

            final StringMap swigmap = AccountDetailsHandler.convertFromNativeToSwig((HashMap<String, String>) map);

            AddAccount runInstance = new AddAccount(swigmap);
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
                // Log.e(TAG, "Waiting for Nofing");
            }
            String accountId = (String) runInstance.getVal();

            return accountId;
        }

        @Override
        public void removeAccount(final String accountId) {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.setAccountDetails() thread running...");
                    configurationManagerJNI.removeAccount(accountId);
                }
            });
        }

        @Override
        public ArrayList<HashMap<String, String>> getHistory() throws RemoteException {
            class History extends SipRunnableWithReturn {

                @Override
                protected VectMap doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.getHistory() thread running...");

                    return configurationManagerJNI.getHistory();
                }
            }

            History runInstance = new History();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
                // Log.w(TAG, "Waiting for getHistory");
            }
            VectMap swigmap = (VectMap) runInstance.getVal();

            ArrayList<HashMap<String, String>> nativemap = HistoryHandler.convertSwigToNative(swigmap);

            return nativemap;
        }

        /*************************
         * Transfer related API
         *************************/

        @Override
        public void transfer(final String callID, final String to) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.transfer() thread running...");
                    if (callManagerJNI.transfer(callID, to)) {
                        Bundle bundle = new Bundle();
                        bundle.putString("CallID", callID);
                        bundle.putString("State", "HUNGUP");
                        Intent intent = new Intent(CallManagerCallBack.CALL_STATE_CHANGED);
                        intent.putExtra(CallManagerCallBack.SIGNAL_NAME, CallManagerCallBack.CALL_STATE_CHANGED);
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
                    Log.i(TAG, "SipService.attendedTransfer() thread running...");
                    if (callManagerJNI.attendedTransfer(transferID, targetID)) {
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
                    Log.i(TAG, "SipService.createConference() thread running...");
                    callManagerJNI.removeConference(confID);
                }
            });

        }

        @Override
        public void joinParticipant(final String sel_callID, final String drag_callID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.joinParticipant() thread running...");
                    callManagerJNI.joinParticipant(sel_callID, drag_callID);
                }
            });

        }

        @Override
        public void createConfFromParticipantList(final List participants) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.createConfFromParticipantList() thread running...");
                    // callManagerJNI.createConfFromParticipantList(participants);
                }
            });

        }

        @Override
        public void addParticipant(final String callID, final String confID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.addParticipant() thread running...");
                    callManagerJNI.addParticipant(callID, confID);
                }
            });

        }

        @Override
        public void addMainParticipant(final String confID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.addMainParticipant() thread running...");
                    callManagerJNI.addMainParticipant(confID);
                }
            });

        }

        @Override
        public void detachParticipant(final String callID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.detachParticipant() thread running...");
                    callManagerJNI.detachParticipant(callID);
                }
            });

        }

        @Override
        public void joinConference(final String sel_confID, final String drag_confID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.joinConference() thread running...");
                    callManagerJNI.joinConference(sel_confID, drag_confID);
                }
            });

        }

        @Override
        public void hangUpConference(final String confID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.joinConference() thread running...");
                    callManagerJNI.hangUpConference(confID);
                }
            });

        }

        @Override
        public void holdConference(final String confID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.holdConference() thread running...");
                    callManagerJNI.holdConference(confID);
                }
            });

        }

        @Override
        public void unholdConference(final String confID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.unholdConference() thread running...");
                    callManagerJNI.unholdConference(confID);
                }
            });

        }

        @Override
        public List getConferenceList() throws RemoteException {
            class ConfList extends SipRunnableWithReturn {
                @Override
                protected StringVect doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.getAccountList() thread running...");
                    return callManagerJNI.getConferenceList();
                }
            }
            ;
            ConfList runInstance = new ConfList();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
                // Log.w(TAG, "Waiting for getConferenceList");
            }
            StringVect swigvect = (StringVect) runInstance.getVal();

            ArrayList<String> nativelist = new ArrayList<String>();

            for (int i = 0; i < swigvect.size(); i++)
                nativelist.add(swigvect.get(i));

            return nativelist;
        }

        @Override
        public List getParticipantList(String confID) throws RemoteException {
            Log.e(TAG, "getConferenceList not implemented");
            return null;
        }

        @Override
        public String getConferenceId(String callID) throws RemoteException {
            Log.e(TAG, "getConferenceList not implemented");
            return null;
        }

        @Override
        public Map getConferenceDetails(String callID) throws RemoteException {
            Log.e(TAG, "getConferenceList not implemented");
            return null;
        }

        @Override
        public String getRecordPath() throws RemoteException {
            class RecordPath extends SipRunnableWithReturn {

                @Override
                protected String doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.getRecordPath() thread running...");
                    return configurationManagerJNI.getRecordPath();
                }
            }

            RecordPath runInstance = new RecordPath();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
                // Log.w(TAG, "Waiting for getRecordPath");
            }
            String path = (String) runInstance.getVal();

            return path;
        }

        @Override
        public void setRecordingCall(final String id) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.setRecordingCall() thread running...");
                    callManagerJNI.setRecordingCall(id);
                }
            });

        }

        @Override
        public void setRecordPath(final String path) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.setRecordingCall() thread running...");
                    configurationManagerJNI.setRecordPath(path);
                }
            });
        }

        @Override
        public void sendTextMessage(final String callID, final String message, final String from) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.sendTextMessage() thread running...");
                    callManagerJNI.sendTextMessage(callID, message, from);
                }
            });

        }

        @Override
        public List getAudioCodecList(String accountID) throws RemoteException {
            class AudioCodecList extends SipRunnableWithReturn {

                @Override
                protected IntVect doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.getAudioCodecList() thread running...");
                    return configurationManagerJNI.getAudioCodecList();
                }
            }

            AudioCodecList runInstance = new AudioCodecList();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
                Log.w(TAG, "Waiting for getAudioCodecList");
            }
            IntVect swigmap = (IntVect) runInstance.getVal();

            ArrayList<Integer> codecs = AudioHandler.convertSwigToNative(swigmap);

            return codecs;
        }

        @Override
        public HashMap<String, SipCall> getCallList() throws RemoteException {
            // class CallList extends SipRunnableWithReturn {
            //
            // @Override
            // protected StringVect doRun() throws SameThreadException {
            // Log.i(TAG, "SipService.getCallList() thread running...");
            // return callManagerJNI.getCallList();
            // }
            // }
            //
            // CallList runInstance = new CallList();
            // getExecutor().execute(runInstance);
            // while (!runInstance.isDone()) {
            // Log.w(TAG, "Waiting for getAudioCodecList");
            // }
            // StringVect swigmap = (StringVect) runInstance.getVal();
            //
            // ArrayList<String> nativemap = new ArrayList<String>();
            // for (int i = 0; i < swigmap.size(); ++i) {
            //
            // String t = swigmap.get(i);
            // nativemap.add(t);
            // }

            return getCurrent_calls();
        }

        @Override
        public SipCall getCall(String callID) throws RemoteException {
            return getCurrent_calls().get(callID);
        }

        /***********************
         * Notification API
         ***********************/
        @Override
        public void createNotification() throws RemoteException {
            makeNotification();
            
        }

        @Override
        public void destroyNotification() throws RemoteException {
            removeNotification();
            
        }
        
        private int NOTIFICATION_ID = new Random().nextInt(1000);
        
        private void makeNotification() {
            if(current_calls.size() == 0){
                return;
            }
            Intent notificationIntent = new Intent(getApplicationContext(), SFLPhoneHomeActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 007, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationManager nm = (NotificationManager) getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(getBaseContext());
    //
//            builder.setContent(view);
            builder.setContentIntent(contentIntent).setOngoing(true).setSmallIcon(R.drawable.ic_launcher).setContentTitle(getCurrent_calls().size()+" ongoing calls").setTicker("Pending calls").setWhen(System.currentTimeMillis()).setAutoCancel(false);
            builder.setPriority(NotificationCompat.PRIORITY_MAX);
            Notification n = builder.build();

            nm.notify(NOTIFICATION_ID, n);
            
        }

        public void removeNotification() {
            NotificationManager nm = (NotificationManager) getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(NOTIFICATION_ID);
            
        }

    };
}
