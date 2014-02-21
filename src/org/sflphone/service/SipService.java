/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
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
package org.sflphone.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.sflphone.account.AccountDetailSrtp;
import org.sflphone.history.HistoryManager;
import org.sflphone.model.*;
import org.sflphone.utils.MediaManager;
import org.sflphone.utils.SipNotifications;
import org.sflphone.utils.SwigNativeConverter;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

public class SipService extends Service {

    static final String TAG = "SipService";
    private SipServiceExecutor mExecutor;
    private static HandlerThread executorThread;

    private CallManager callManagerJNI;
    private ManagerImpl managerImpl;
    private CallManagerCallBack callManagerCallBack;
    
    private ConfigurationManager configurationManagerJNI;
    private ConfigurationManagerCallback configurationManagerCallback;
    private boolean isPjSipStackStarted = false;

    protected SipNotifications mNotificationManager;
    protected HistoryManager mHistoryManager;
    protected MediaManager mMediaManager;

    private HashMap<String, Conference> mConferences = new HashMap<String, Conference>();

    public HashMap<String, Conference> getConferences() {
        return mConferences;
    }

    public CallManager getCallManagerJNI() {
        return callManagerJNI;
    }

    public ConfigurationManager getConfigurationManagerJNI() {
        return configurationManagerJNI;
    }

    public void addCallToConference(String confId, String callId) {
        if(mConferences.get(callId) != null){
            // We add a simple call to a conference
            Log.i(TAG, "// We add a simple call to a conference");
            mConferences.get(confId).addParticipant(mConferences.get(callId).getParticipants().get(0));
            mConferences.remove(callId);
        } else {
            Log.i(TAG, "addCallToConference");
            Iterator<Map.Entry<String, Conference>> it = mConferences.entrySet().iterator();
            while (it.hasNext()) {
                Conference tmp = it.next().getValue();
                for (SipCall c : tmp.getParticipants()) {
                    if (c.getCallId().contentEquals(callId)) {
                        mConferences.get(confId).addParticipant(c);
                        mConferences.get(tmp.getId()).removeParticipant(c);
                    }
                }
            }
        }

    }

    public void detachCallFromConference(String confId, SipCall call) {
        Log.i(TAG, "detachCallFromConference");
        Conference separate = new Conference(call);
        mConferences.put(separate.getId(), separate);
        mConferences.get(confId).removeParticipant(call);
    }


    @Override
    public boolean onUnbind(Intent i) {
        super.onUnbind(i);
        Log.i(TAG, "onUnbind(intent)");
        return true;
    }

    @Override
    public void onRebind(Intent i) {
        super.onRebind(i);
    }

    /* called once by startService() */
    @Override
    public void onCreate() {
        Log.i(TAG, "onCreated");
        super.onCreate();

        getExecutor().execute(new StartRunnable());

        mNotificationManager = new SipNotifications(this);
        mMediaManager = new MediaManager(this);
        mHistoryManager = new HistoryManager(this);

        mNotificationManager.onServiceCreate();
        mMediaManager.startService();

    }

    /* called for each startService() */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStarted");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY; /* started and stopped explicitly */
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        /* called once by stopService() */
        mNotificationManager.onServiceDestroy();
        mMediaManager.stopService();
        getExecutor().execute(new FinalizeRunnable());
        super.onDestroy();

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
            mExecutor = new SipServiceExecutor();
        }
        return mExecutor;
    }

    public SipCall getCallById(String callID) {
        if (getConferences().get(callID) != null) {
            return getConferences().get(callID).getCallById(callID);
        } else {
            // Check if call is in a conference
            Iterator<Map.Entry<String, Conference>> it = getConferences().entrySet().iterator();
            while (it.hasNext()) {
                Conference tmp = it.next().getValue();
                SipCall c = tmp.getCallById(callID);
                if(c != null)
                     return c;
            }
        }
        return null;
    }

    // Executes immediate tasks in a single executorThread.
    public static class SipServiceExecutor extends Handler {

        SipServiceExecutor() {
            super(createLooper());
        }

        public void execute(Runnable task) {
            // TODO: add wakelock
            Message.obtain(SipServiceExecutor.this, 0/* don't care */, task).sendToTarget();
            Log.w(TAG, "SenT!");
        }

        @Override
        public void handleMessage(Message msg) {
            Log.w(TAG, "handleMessage");
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

    private void stopDaemon() {
        if (managerImpl != null) {
            managerImpl.finish();
            isPjSipStackStarted = false;
        }
    }

    private void startPjSipStack() throws SameThreadException {
        if (isPjSipStackStarted)
            return;

        try {
            System.loadLibrary("gnustl_shared");
            System.loadLibrary("crypto");
            System.loadLibrary("ssl");
            System.loadLibrary("sflphone");
            isPjSipStackStarted = true;

        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Problem with the current Pj stack...", e);
            isPjSipStackStarted = false;
            return;
        } catch (Exception e) {
            Log.e(TAG, "Problem with the current Pj stack...", e);
            isPjSipStackStarted = false;
        }

        Log.i(TAG, "PjSIPStack started");
        managerImpl = SFLPhoneservice.instance();

        /* set static AppPath before calling manager.init */
        // managerImpl.setPath(getApplication().getFilesDir().getAbsolutePath());

        callManagerJNI = new CallManager();
        callManagerCallBack = new CallManagerCallBack(this);
        SFLPhoneservice.setCallbackObject(callManagerCallBack);

        configurationManagerJNI = new ConfigurationManager();
        configurationManagerCallback = new ConfigurationManagerCallback(this);
        SFLPhoneservice.setConfigurationCallbackObject(configurationManagerCallback);
        managerImpl.init("");

        Log.i(TAG, "->startPjSipStack");
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

    public abstract class SipRunnableWithReturn implements Runnable {
        Object obj = null;
        boolean done = false;

        protected abstract Object doRun() throws SameThreadException, RemoteException;

        public Object getVal() {
            return obj;
        }

        public boolean isDone() {
            return done;
        }

        @Override
        public void run() {
            try {
                if (isPjSipStackStarted)
                    obj = doRun();
                done = true;
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

    private final ISipService.Stub mBinder = new ISipService.Stub() {

        @Override
        public void placeCall(final SipCall call) {

            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.placeCall() thread running...");
                    Conference toAdd;
                    if(call.getAccount().useSecureLayer()){
                        Bundle secureArgs = new Bundle();
                        secureArgs.putBoolean(SecureSipCall.DISPLAY_SAS, call.getAccount().getSrtpDetails().getDetailBoolean(AccountDetailSrtp.CONFIG_ZRTP_DISPLAY_SAS));
                        secureArgs.putBoolean(SecureSipCall.DISPLAY_SAS_ONCE, call.getAccount().getSrtpDetails().getDetailBoolean(AccountDetailSrtp.CONFIG_ZRTP_DISPLAY_SAS_ONCE));
                        secureArgs.putBoolean(SecureSipCall.DISPLAY_WARNING_ZRTP_NOT_SUPPORTED, call.getAccount().getSrtpDetails().getDetailBoolean(AccountDetailSrtp.CONFIG_ZRTP_NOT_SUPP_WARNING));
                        SecureSipCall secureCall = new SecureSipCall(call, secureArgs);
                        toAdd = new Conference(secureCall);
                    } else {
                        toAdd = new Conference(call);
                    }
                    mConferences.put(toAdd.getId(), toAdd);
                    mMediaManager.obtainAudioFocus(false);
                    callManagerJNI.placeCall(call.getAccount().getAccountID(), call.getCallId(), call.getmContact().getPhones().get(0).getNumber());
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
            mMediaManager.stopRing();
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.accept() thread running...");
                    callManagerJNI.accept(callID);
                    mMediaManager.RouteToInternalSpeaker();
                }
            });
        }

        @Override
        public void hangUp(final String callID) {
            mMediaManager.stopRing();
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.hangUp() thread running...");
                    callManagerJNI.hangUp(callID);
                    removeCall(callID);
                    Log.i(TAG, "mConferences.size():"+mConferences.size());
                    if(mConferences.size() == 0) {
                        Log.i(TAG, "No more calls!");
                        mMediaManager.abandonAudioFocus();
                    }
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
        public HashMap<String, String> getCallDetails(String callID) throws RemoteException {
            class CallDetails extends SipRunnableWithReturn {
                private String id;

                CallDetails(String callID) {
                    id = callID;
                }

                @Override
                protected StringMap doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.getCallDetails() thread running...");
                    return callManagerJNI.getCallDetails(id);
                }
            }

            CallDetails runInstance = new CallDetails(callID);
            getExecutor().execute(runInstance);

            while (!runInstance.isDone()) {
            }
            StringMap swigmap = (StringMap) runInstance.getVal();

            HashMap<String, String> nativemap = SwigNativeConverter.convertCallDetailsToNative(swigmap);

            return nativemap;

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
            AccountList runInstance = new AccountList();
            Log.i(TAG, "SipService.getAccountList() thread running...");
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
        public void setAccountOrder(final String order) {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.setAccountsOrder() thread running...");
                    configurationManagerJNI.setAccountsOrder(order);
                }
            });
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

            HashMap<String, String> nativemap = SwigNativeConverter.convertAccountToNative(swigmap);

            return nativemap;
        }

        @SuppressWarnings("unchecked")
        // Hashmap runtime cast
        @Override
        public void setAccountDetails(final String accountId, final Map map) {
            HashMap<String, String> nativemap = (HashMap<String, String>) map;

            final StringMap swigmap = SwigNativeConverter.convertFromNativeToSwig(nativemap);

            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {

                    configurationManagerJNI.setAccountDetails(accountId, swigmap);
                    Log.i(TAG, "SipService.setAccountDetails() thread running...");
                }

            });
        }

        @Override
        public Map getAccountTemplate() throws RemoteException {
            class AccountTemplate extends SipRunnableWithReturn {

                @Override
                protected StringMap doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.getAccountTemplate() thread running...");
                    return configurationManagerJNI.getAccountTemplate();
                }
            }

            AccountTemplate runInstance = new AccountTemplate();
            getExecutor().execute(runInstance);

            while (!runInstance.isDone()) {
            }
            StringMap swigmap = (StringMap) runInstance.getVal();

            HashMap<String, String> nativemap = SwigNativeConverter.convertAccountToNative(swigmap);

            return nativemap;
        }

        @SuppressWarnings("unchecked")
        // Hashmap runtime cast
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

            final StringMap swigmap = SwigNativeConverter.convertFromNativeToSwig((HashMap<String, String>) map);

            AddAccount runInstance = new AddAccount(swigmap);
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
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
                    // Generate a CONF_CREATED callback
                }
            });
            Log.i(TAG, "After joining participants");
        }

        @Override
        public void addParticipant(final SipCall call, final String confID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.addParticipant() thread running...");
                    callManagerJNI.addParticipant(call.getCallId(), confID);
                    mConferences.get(confID).getParticipants().add(call);
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
                    Log.i(TAG, "Detaching " + callID);
                    Iterator<Entry<String, Conference>> it = mConferences.entrySet().iterator();
                    Log.i(TAG, "mConferences size " + mConferences.size());
                    while (it.hasNext()) {
                        Conference tmp = it.next().getValue();
                        Log.i(TAG, "conf has " + tmp.getParticipants().size() + " participants");
                        if (tmp.contains(callID)) {
                            Conference toDetach = new Conference(tmp.getCallById(callID));
                            mConferences.put(toDetach.getId(), toDetach);
                            Log.i(TAG, "Call found and put in current_calls");
                        }
                    }
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
        public boolean isConferenceParticipant(final String callID) throws RemoteException {
            class IsParticipant extends SipRunnableWithReturn {

                @Override
                protected Boolean doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.isRecording() thread running...");
                    return callManagerJNI.isConferenceParticipant(callID);
                }
            }

            IsParticipant runInstance = new IsParticipant();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
            }

            return (Boolean) runInstance.getVal();
        }

        @Override
        public HashMap<String, Conference> getConferenceList() throws RemoteException {
            // class ConfList extends SipRunnableWithReturn {
            // @Override
            // protected StringVect doRun() throws SameThreadException {
            // Log.i(TAG, "SipService.getConferenceList() thread running...");
            // return callManagerJNI.getConferenceList();
            // }
            // }
            // ;
            // ConfList runInstance = new ConfList();
            // getExecutor().execute(runInstance);
            // while (!runInstance.isDone()) {
            // // Log.w(TAG, "Waiting for getConferenceList");
            // }
            // StringVect swigvect = (StringVect) runInstance.getVal();
            //
            // ArrayList<String> nativelist = new ArrayList<String>();
            //
            // for (int i = 0; i < swigvect.size(); i++)
            // nativelist.add(swigvect.get(i));
            //
            // return nativelist;
            return mConferences;
        }

        @Override
        public List getParticipantList(final String confID) throws RemoteException {
            class PartList extends SipRunnableWithReturn {
                @Override
                protected StringVect doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.getParticipantList() thread running...");
                    return callManagerJNI.getParticipantList(confID);
                }
            }
            ;
            PartList runInstance = new PartList();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
                Log.w(TAG, "getParticipantList");
            }
            StringVect swigvect = (StringVect) runInstance.getVal();
            Log.w(TAG, "After that");
            ArrayList<String> nativelist = new ArrayList<String>();

            for (int i = 0; i < swigvect.size(); i++)
                nativelist.add(swigvect.get(i));

            return nativelist;
        }

        @Override
        public String getConferenceId(String callID) throws RemoteException {
            Log.e(TAG, "getConferenceList not implemented");
            return null;
        }

        @Override
        public String getConferenceDetails(final String callID) throws RemoteException {
            class ConfDetails extends SipRunnableWithReturn {
                @Override
                protected StringMap doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.getAccountList() thread running...");
                    return callManagerJNI.getConferenceDetails(callID);
                }
            }
            ;
            ConfDetails runInstance = new ConfDetails();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
                // Log.w(TAG, "Waiting for getConferenceList");
            }
            StringMap swigvect = (StringMap) runInstance.getVal();

            return swigvect.get("CONF_STATE");
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
        public boolean toggleRecordingCall(final String id) throws RemoteException {

            class ToggleRecording extends SipRunnableWithReturn {

                @Override
                protected Boolean doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.toggleRecordingCall() thread running...");
                    boolean result = callManagerJNI.toggleRecording(id);

                    if (getConferences().containsKey(id)) {
                        getConferences().get(id).setRecording(result);
                    } else {
                        Iterator<Conference> it = getConferences().values().iterator();
                        while (it.hasNext()) {
                            Conference c = it.next();
                            if (c.getCallById(id) != null)
                                c.getCallById(id).setRecording(result);
                        }
                    }
                    return result;
                }
            }

            ToggleRecording runInstance = new ToggleRecording();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
            }

            return (Boolean) runInstance.getVal();

        }

        @Override
        public boolean startRecordedFilePlayback(final String filepath) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.setRecordingCall() thread running...");
                    callManagerJNI.startRecordedFilePlayback(filepath);
                }
            });
            return false;
        }

        @Override
        public void stopRecordedFilePlayback(final String filepath) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.stopRecordedFilePlayback() thread running...");
                    callManagerJNI.stopRecordedFilePlayback(filepath);
                }
            });
        }

        @Override
        public void setRecordPath(final String path) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.setRecordPath() " + path + " thread running...");
                    configurationManagerJNI.setRecordPath(path);
                }
            });
        }

        @Override
        public void sendTextMessage(final String callID, final SipMessage message) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.sendTextMessage() thread running...");
                    callManagerJNI.sendTextMessage(callID, message.comment);
                    if (getConferences().get(callID) != null)
                        getConferences().get(callID).addSipMessage(message);
                }
            });

        }

        @Override
        public List getAudioCodecList(final String accountID) throws RemoteException {
            class AudioCodecList extends SipRunnableWithReturn {

                @Override
                protected ArrayList<Codec> doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.getAudioCodecList() thread running...");
                    ArrayList<Codec> results = new ArrayList<Codec>();

                    IntVect active_payloads = configurationManagerJNI.getActiveAudioCodecList(accountID);
                    for (int i = 0; i < active_payloads.size(); ++i) {

                        results.add(new Codec(active_payloads.get(i), configurationManagerJNI.getAudioCodecDetails(active_payloads.get(i)), true));

                    }
                    IntVect payloads = configurationManagerJNI.getAudioCodecList();

                    for (int i = 0; i < payloads.size(); ++i) {
                        boolean isActive = false;
                        for (Codec co : results) {
                            if (co.getPayload().toString().contentEquals(String.valueOf(payloads.get(i))))
                                isActive = true;

                        }
                        if (isActive)
                            continue;
                        else
                            results.add(new Codec(payloads.get(i), configurationManagerJNI.getAudioCodecDetails(payloads.get(i)), false));

                    }

                    return results;
                }
            }

            AudioCodecList runInstance = new AudioCodecList();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
            }
            ArrayList<Codec> codecs = (ArrayList<Codec>) runInstance.getVal();
            return codecs;
        }

        @Override
        public Map getRingtoneList() throws RemoteException {
            class RingtoneList extends SipRunnableWithReturn {

                @Override
                protected StringMap doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.getRingtoneList() thread running...");
                    return configurationManagerJNI.getRingtoneList();
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
        public void setActiveCodecList(final List codecs, final String accountID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.setActiveAudioCodecList() thread running...");
                    StringVect list = new StringVect();
                    for (int i = 0; i < codecs.size(); ++i) {
                        list.add((String) codecs.get(i));
                    }
                    configurationManagerJNI.setActiveAudioCodecList(list, accountID);
                }
            });
        }

        /***********************
         * Notification API
         ***********************/
        @Override
        public void createNotification() throws RemoteException {

        }

        @Override
        public void destroyNotification() throws RemoteException {

        }

        @Override
        public Conference getCurrentCall() throws RemoteException {
            for (Conference conf : mConferences.values()) {
                if (conf.isIncoming())
                    return conf;
            }

            for (Conference conf : mConferences.values()) {
                if (conf.isOnGoing())
                    return conf;
            }

            return null;
        }

        @Override
        public void playDtmf(final String key) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.playDtmf() thread running...");
                    callManagerJNI.playDTMF(key);
                }
            });
        }

        @Override
        public List getConcurrentCalls() throws RemoteException {
            return new ArrayList(mConferences.values());
        }

        @Override
        public Conference getConference(String id) throws RemoteException {
            return mConferences.get(id);
        }

        @Override
        public String getCurrentAudioCodecName(String callID) throws RemoteException {
            return callManagerJNI.getCurrentAudioCodecName(callID);
        }

        @Override
        public void setMuted(final boolean mute) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.setMuted() thread running...");
                    configurationManagerJNI.muteCapture(mute);
                }
            });
        }

        @Override
        public boolean isCaptureMuted() throws RemoteException {
            class IsMuted extends SipRunnableWithReturn {

                @Override
                protected Boolean doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.isCaptureMuted() thread running...");
                    return configurationManagerJNI.isCaptureMuted();
                }
            }

            IsMuted runInstance = new IsMuted();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
            }

            return (Boolean) runInstance.getVal();
        }

        @Override
        public void confirmSAS(final String callID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.confirmSAS() thread running...");
                    SecureSipCall call = (SecureSipCall) getCallById(callID);
                    call.setSASConfirmed(true);
                    callManagerJNI.setSASVerified(callID);
                }
            });
        }

        @Override
        public List getCredentials(final String accountID) throws RemoteException {
            class Credentials extends SipRunnableWithReturn {

                @Override
                protected List doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.getCredentials() thread running...");
                    VectMap map = configurationManagerJNI.getCredentials(accountID);
                    ArrayList<HashMap<String, String>> result = SwigNativeConverter.convertCredentialsToNative(map);
                    return result;
                }
            }

            Credentials runInstance = new Credentials();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
            }
            return (List) runInstance.getVal();
        }

        @Override
        public void setCredentials(final String accountID, final List creds) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.setCredentials() thread running...");
                    ArrayList<HashMap<String, String>> list = (ArrayList<HashMap<String, String>>) creds;
                    configurationManagerJNI.setCredentials(accountID, SwigNativeConverter.convertFromNativeToSwig(creds));
                }
            });
        }

        @Override
        public void registerAllAccounts() throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.registerAllAccounts() thread running...");
                    configurationManagerJNI.registerAllAccounts();
                }
            });
        }

        @Override
        public void toggleSpeakerPhone(boolean toggle) throws RemoteException {
            if (toggle)
                mMediaManager.RouteToSpeaker();
            else
                mMediaManager.RouteToInternalSpeaker();
        }

    };

    private void removeCall(String callID) {
        Conference conf = findConference(callID);
        if(conf == null)
            return;
        if(conf.getParticipants().size() == 1)
            getConferences().remove(conf.getId());
        else
            conf.removeParticipant(conf.getCallById(callID));
    }

    protected Conference findConference(String callID) {
        Conference result = null;
        if (getConferences().get(callID) != null) {
            result = getConferences().get(callID);
        } else {
            Iterator<Map.Entry<String, Conference>> it = getConferences().entrySet().iterator();
            while (it.hasNext()) {
                Conference tmp = it.next().getValue();
                for (SipCall c : tmp.getParticipants()) {
                    if (c.getCallId().contentEquals(callID)) {
                        result = tmp;
                    }
                }
            }
        }
        return result;
    }
}
