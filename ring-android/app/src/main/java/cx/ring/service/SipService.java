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
package cx.ring.service;

import android.os.Handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.util.Log;
import cx.ring.history.HistoryManager;
import cx.ring.model.Codec;
import cx.ring.model.Conference;
import cx.ring.model.SecureSipCall;
import cx.ring.model.SipMessage;
import cx.ring.utils.MediaManager;
import cx.ring.utils.SipNotifications;
import cx.ring.utils.SwigNativeConverter;
import cx.ring.model.SipCall;


public class SipService extends Service {

    static final String TAG = "SipService";
    private SipServiceExecutor mExecutor;
    private static HandlerThread executorThread;

    private Handler handler = new Handler();
    private static int POLLING_TIMEOUT = 500;
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

    protected SipNotifications mNotificationManager;
    protected HistoryManager mHistoryManager;
    protected MediaManager mMediaManager;

    private HashMap<String, Conference> mConferences = new HashMap<>();
    private ConfigurationManagerCallback configurationCallback;
    private CallManagerCallBack callManagerCallBack;

    public HashMap<String, Conference> getConferences() {
        return mConferences;
    }

    public void addCallToConference(String confId, String callId) {
        if(mConferences.get(callId) != null){
            // We add a simple call to a conference
            Log.i(TAG, "// We add a simple call to a conference");
            mConferences.get(confId).addParticipant(mConferences.get(callId).getParticipants().get(0));
            mConferences.remove(callId);
        } else {
            Log.i(TAG, "addCallToConference");
            for (Entry<String, Conference> stringConferenceEntry : mConferences.entrySet()) {
                Conference tmp = stringConferenceEntry.getValue();
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
            for (Entry<String, Conference> stringConferenceEntry : getConferences().entrySet()) {
                Conference tmp = stringConferenceEntry.getValue();
                SipCall c = tmp.getCallById(callID);
                if (c != null)
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
    }

    private void stopDaemon() {
        handler.removeCallbacks(pollEvents);
        Ringservice.fini();
        isPjSipStackStarted = false;
    }

    private void startPjSipStack() throws SameThreadException {
        if (isPjSipStackStarted)
            return;

        try {
            /*System.loadLibrary("codec_ulaw");
            System.loadLibrary("codec_alaw");
            System.loadLibrary("codec_speex");
            System.loadLibrary("codec_g729");
            System.loadLibrary("codec_gsm");
            System.loadLibrary("codec_opus");*/
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
        Ringservice.init(configurationCallback, callManagerCallBack);
        handler.postDelayed(pollEvents, POLLING_TIMEOUT);
        Log.i(TAG, "PjSIPStack started");
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
                        SecureSipCall secureCall = new SecureSipCall(call);
                        toAdd = new Conference(secureCall);
                    } else {
                        toAdd = new Conference(call);
                    }
                    mConferences.put(toAdd.getId(), toAdd);
                    mMediaManager.obtainAudioFocus(false);
                    Ringservice.placeCall(call.getAccount().getAccountID(), call.getmContact().getPhones().get(0).getNumber());
                }
            });
        }

        @Override
        public void refuse(final String callID) {

            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.refuse() thread running...");
                    Ringservice.refuse(callID);
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
                    Ringservice.accept(callID);
                    mMediaManager.RouteToInternalSpeaker();
                }
            });
        }

        @Override
        public void hangUp(final String callID) {
            mMediaManager.stopRing();
            Log.e(TAG, "HANGING UP");
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.hangUp() thread running...");
                    Ringservice.hangUp(callID);
                    removeCall(callID);
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
                    Ringservice.hold(callID);
                }
            });
        }

        @Override
        public void unhold(final String callID) {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.unhold() thread running...");
                    Ringservice.unhold(callID);
                }
            });
        }

        @Override
        public Map<String, String> getCallDetails(String callID) throws RemoteException {
            class CallDetails extends SipRunnableWithReturn {
                private String id;

                CallDetails(String callID) {
                    id = callID;
                }

                @Override
                protected Map<String, String> doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.getCallDetails() thread running...");
                    return Ringservice.getCallDetails(id).toNative();
                }
            }

            CallDetails runInstance = new CallDetails(callID);
            getExecutor().execute(runInstance);

            while (!runInstance.isDone()) {
            }
            return (Map<String, String>)runInstance.getVal();
        }

        @Override
        public void setAudioPlugin(final String audioPlugin) {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.setAudioPlugin() thread running...");
                    Ringservice.setAudioPlugin(audioPlugin);
                }
            });
        }

        @Override
        public String getCurrentAudioOutputPlugin() {
            class CurrentAudioPlugin extends SipRunnableWithReturn {
                @Override
                protected String doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.getCurrentAudioOutputPlugin() thread running...");
                    return Ringservice.getCurrentAudioOutputPlugin();
                }
            }

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
                    return Ringservice.getAccountList();
                }
            }
            AccountList runInstance = new AccountList();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
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
                    Ringservice.setAccountsOrder(order);
                }
            });
        }

        @Override
        public Map<String, String> getAccountDetails(final String accountID) {
            class AccountDetails extends SipRunnableWithReturn {
                private String id;

                AccountDetails(String accountId) {
                    id = accountId;
                }

                @Override
                protected Map<String, String> doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.getAccountDetails() thread running...");
                    return Ringservice.getAccountDetails(id).toNative();
                }
            }

            AccountDetails runInstance = new AccountDetails(accountID);
            getExecutor().execute(runInstance);

            while (!runInstance.isDone()) {
            }
            return (Map<String, String>) runInstance.getVal();
        }

        @SuppressWarnings("unchecked")
        // Hashmap runtime cast
        @Override
        public void setAccountDetails(final String accountId, final Map map) {
            final StringMap swigmap = StringMap.toSwig(map);

            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {

                    Ringservice.setAccountDetails(accountId, swigmap);
                    Log.i(TAG, "SipService.setAccountDetails() thread running...");
                }

            });
        }

        @Override
        public Map<String, String> getAccountTemplate(final String accountType) throws RemoteException {
            class AccountTemplate extends SipRunnableWithReturn {

                @Override
                protected Map<String, String> doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.getAccountTemplate() thread running...");
                    return Ringservice.getAccountTemplate(accountType).toNative();
                }
            }

            AccountTemplate runInstance = new AccountTemplate();
            getExecutor().execute(runInstance);

            while (!runInstance.isDone()) {
            }
            return (Map<String, String>) runInstance.getVal();
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
                    Log.i(TAG, "SipService.addAccount() thread running...");
                    return Ringservice.addAccount(map);
                }
            }

            final StringMap swigmap = StringMap.toSwig(map);

            AddAccount runInstance = new AddAccount(swigmap);
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
            }
            return (String) runInstance.getVal();
        }

        @Override
        public void removeAccount(final String accountId) {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.setAccountDetails() thread running...");
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
                    Log.i(TAG, "SipService.transfer() thread running...");
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
                    Log.i(TAG, "SipService.attendedTransfer() thread running...");
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
                    Log.i(TAG, "SipService.createConference() thread running...");
                    Ringservice.removeConference(confID);
                }
            });

        }

        @Override
        public void joinParticipant(final String sel_callID, final String drag_callID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.joinParticipant() thread running...");
                    Ringservice.joinParticipant(sel_callID, drag_callID);
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
                    Ringservice.addParticipant(call.getCallId(), confID);
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
                    Ringservice.addMainParticipant(confID);
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
                    Ringservice.detachParticipant(callID);
                }
            });

        }

        @Override
        public void joinConference(final String sel_confID, final String drag_confID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.joinConference() thread running...");
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
                    Log.i(TAG, "SipService.joinConference() thread running...");
                    Ringservice.hangUpConference(confID);
                }
            });

        }

        @Override
        public void holdConference(final String confID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.holdConference() thread running...");
                    Ringservice.holdConference(confID);
                }
            });

        }

        @Override
        public void unholdConference(final String confID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.unholdConference() thread running...");
                    Ringservice.unholdConference(confID);
                }
            });

        }

        @Override
        public boolean isConferenceParticipant(final String callID) throws RemoteException {
            class IsParticipant extends SipRunnableWithReturn {

                @Override
                protected Boolean doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.isRecording() thread running...");
                    return Ringservice.isConferenceParticipant(callID);
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
                    return Ringservice.getParticipantList(confID);
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
                protected String doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.getConferenceDetails() thread running...");
                    return Ringservice.getConferenceDetails(callID).get("CONF_STATE");
                }
            }
            ConfDetails runInstance = new ConfDetails();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
                // Log.w(TAG, "Waiting for getConferenceList");
            }
            return (String) runInstance.getVal();
        }

        @Override
        public String getRecordPath() throws RemoteException {
            class RecordPath extends SipRunnableWithReturn {

                @Override
                protected String doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.getRecordPath() thread running...");
                    return Ringservice.getRecordPath();
                }
            }

            RecordPath runInstance = new RecordPath();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
                // Log.w(TAG, "Waiting for getRecordPath");
            }

            return (String) runInstance.getVal();
        }

        @Override
        public boolean toggleRecordingCall(final String id) throws RemoteException {

            class ToggleRecording extends SipRunnableWithReturn {

                @Override
                protected Boolean doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.toggleRecordingCall() thread running...");
                    boolean result = Ringservice.toggleRecording(id);

                    if (getConferences().containsKey(id)) {
                        getConferences().get(id).setRecording(result);
                    } else {
                        for (Conference c : getConferences().values()) {
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
                    Log.i(TAG, "SipService.stopRecordedFilePlayback() thread running...");
                    Ringservice.stopRecordedFilePlayback(filepath);
                }
            });
        }

        @Override
        public void setRecordPath(final String path) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.setRecordPath() " + path + " thread running...");
                    Ringservice.setRecordPath(path);
                }
            });
        }

        @Override
        public void sendTextMessage(final String callID, final SipMessage message) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.sendTextMessage() thread running...");
                    Ringservice.sendTextMessage(callID, message.comment);
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

                    UintVect active_payloads = Ringservice.getActiveCodecList(accountID);
                    for (int i = 0; i < active_payloads.size(); ++i) {
                        Log.i(TAG, "SipService.getCodecDetails(" + accountID +", "+ active_payloads.get(i) +")");
                        results.add(new Codec(active_payloads.get(i), Ringservice.getCodecDetails(accountID, active_payloads.get(i)), true));

                    }
                    UintVect payloads = Ringservice.getCodecList();

                    for (int i = 0; i < payloads.size(); ++i) {
                        boolean isActive = false;
                        for (Codec co : results) {
                            if (co.getPayload() == payloads.get(i))
                                isActive = true;
                        }
                        if (isActive)
                            continue;
                        else {
                            StringMap details = Ringservice.getCodecDetails(accountID, payloads.get(i));
                            if (details.size() > 1)
                                results.add(new Codec(payloads.get(i), Ringservice.getCodecDetails(accountID, payloads.get(i)), false));
                            else
                                Log.i(TAG, "Error loading codec " + i);
                        }

                    }

                    return results;
                }
            }

            AudioCodecList runInstance = new AudioCodecList();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
            }
            return (ArrayList<Codec>) runInstance.getVal();
        }

        /*
        @Override
        public Map getRingtoneList() throws RemoteException {
            class RingtoneList extends SipRunnableWithReturn {

                @Override
                protected StringMap doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.getRingtoneList() thread running...");
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
                    Log.i(TAG, "SipService.isCaptureMuted() thread running...");
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
                    Log.i(TAG, "SipService.isCaptureMuted() thread running...");
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
                    Log.i(TAG, "SipService.isCaptureMuted() thread running...");
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
        public Map<String, String> validateCertificate(final String accountID, final String certificatePath, final String privateKeyPath) throws RemoteException {
            class isValid extends SipRunnableWithReturn {

                @Override
                protected Map<String, String> doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.isCaptureMuted() thread running...");
                    return Ringservice.validateCertificate(accountID, certificatePath, privateKeyPath).toNative();
                }
            }

            isValid runInstance = new isValid();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
            }

            return (Map<String, String>) runInstance.getVal();
        }

        @Override
        public Map<String, String> validateCertificateRaw(final String accountID, final byte[] certificateRaw) throws RemoteException {
            class isValid extends SipRunnableWithReturn {

                @Override
                protected Map<String, String> doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.isCaptureMuted() thread running...");
                    return Ringservice.validateCertificateRaw(accountID, SwigNativeConverter.convertFromNativeToSwig(certificateRaw)).toNative();
                }
            }

            isValid runInstance = new isValid();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
            }

            return (Map<String, String>) runInstance.getVal();
        }

        @Override
        public Map<String, String> getCertificateDetails(final String certificatePath) throws RemoteException {
            class isValid extends SipRunnableWithReturn {

                @Override
                protected Map<String, String> doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.isCaptureMuted() thread running...");
                    return Ringservice.getCertificateDetails(certificatePath).toNative();
                }
            }

            isValid runInstance = new isValid();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
            }

            return (Map<String, String>) runInstance.getVal();
        }

        @Override
        public Map<String, String> getCertificateDetailsRaw(final byte[] certificateRaw) throws RemoteException {
            class isValid extends SipRunnableWithReturn {

                @Override
                protected Map<String, String> doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.isCaptureMuted() thread running...");
                    return Ringservice.getCertificateDetailsRaw(SwigNativeConverter.convertFromNativeToSwig(certificateRaw)).toNative();
                }
            }

            isValid runInstance = new isValid();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
            }

            return (Map<String, String>) runInstance.getVal();
        }

        @Override
        public void setActiveCodecList(final List codecs, final String accountID) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.setActiveAudioCodecList() thread running...");
                    UintVect list = new UintVect();
                    for (Object codec : codecs) {
                        list.add((Long) codec);
                    }
                    Ringservice.setActiveCodecList(accountID, list);
                }
            });
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
                    Ringservice.playDTMF(key);
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
        public void setMuted(final boolean mute) throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.setMuted() thread running...");
                    Ringservice.muteCapture(mute);
                }
            });
        }

        @Override
        public boolean isCaptureMuted() throws RemoteException {
            class IsMuted extends SipRunnableWithReturn {

                @Override
                protected Boolean doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.isCaptureMuted() thread running...");
                    return Ringservice.isCaptureMuted();
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
                    Ringservice.setSASVerified(callID);
                }
            });
        }


        @Override
        public List getTlsSupportedMethods(){
            class TlsMethods extends SipRunnableWithReturn {

                @Override
                protected List doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.getCredentials() thread running...");
                    StringVect map = Ringservice.getSupportedTlsMethod();
                    return SwigNativeConverter.convertSwigToNative(map);
                }
            }

            TlsMethods runInstance = new TlsMethods();
            getExecutor().execute(runInstance);
            while (!runInstance.isDone()) {
            }
            return (List) runInstance.getVal();
        }

        @Override
        public List getCredentials(final String accountID) throws RemoteException {
            class Credentials extends SipRunnableWithReturn {

                @Override
                protected List doRun() throws SameThreadException {
                    Log.i(TAG, "SipService.getCredentials() thread running...");
                    return Ringservice.getCredentials(accountID).toNative();
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
                    Ringservice.setCredentials(accountID, SwigNativeConverter.convertFromNativeToSwig(creds));
                }
            });
        }

        @Override
        public void registerAllAccounts() throws RemoteException {
            getExecutor().execute(new SipRunnable() {
                @Override
                protected void doRun() throws SameThreadException, RemoteException {
                    Log.i(TAG, "SipService.registerAllAccounts() thread running...");
                    Ringservice.registerAllAccounts();
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
            for (Entry<String, Conference> stringConferenceEntry : getConferences().entrySet()) {
                Conference tmp = stringConferenceEntry.getValue();
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
