package com.savoirfairelinux.sflphone.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.savoirfairelinux.sflphone.client.SFLphoneApplication;

public class SipService extends Service {

    static final String TAG = "SipService";
    static final int DELAY = 5000; /* 5 sec */
    private boolean runFlag = false;
    private SipServiceThread sipServiceThread;
    private SFLphoneApplication sflphone;
    private final IBinder mBinder = new LocalBinder();

    /* called once by startService() */
    @Override
    public void onCreate() {
        Log.i(TAG, "onCreated");
        super.onCreate();
        this.sflphone = (SFLphoneApplication) getApplication();
        this.sipServiceThread = new SipServiceThread();
        Log.i(TAG, "onCreated");
    }

    /* called for each startService() */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStarted");
        super.onStartCommand(intent, flags, startId);
//        if(intent != null) {
//            Parcelable p = intent.getParcelableExtra(ServiceConstants.EXTRA_OUTGOING_ACTIVITY);
//            Log.i(TAG, "unmarshalled outgoing_activity");
//        }
        this.runFlag = true;
        this.sipServiceThread.start();
        this.sflphone.setServiceRunning(true);
        Toast.makeText(this, "Sflphone Service started", Toast.LENGTH_SHORT).show();
        
        Log.i(TAG, "onStarted");
        return START_STICKY; /* started and stopped explicitly */
    }

    @Override
    public void onDestroy() {
        /* called once by stopService() */
        super.onDestroy();
        this.runFlag = false;
        this.sipServiceThread.interrupt();
        this.sipServiceThread = null;
        this.sflphone.setServiceRunning(false);
        Toast.makeText(this, "Sflphone Service stopped", Toast.LENGTH_SHORT).show();
        
        Log.i(TAG, "onDestroyed");
    }

    @Override
    public IBinder onBind(Intent arg0) {
        Log.i(TAG, "onBound");
        return mBinder;
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        SipService getService() {
            // Return this instance of LocalService so clients can call public methods
            return SipService.this;
        }
    }

    private class SipServiceThread extends Thread {
        
        public SipServiceThread() {
            super("sipServiceThread");
        }
        
        @Override
        public void run() {
            SipService sipService = SipService.this;
            while(sipService.runFlag) {
                try {
                    //Log.i(TAG, "SipService thread running...");
                    Thread.sleep(DELAY);
                } catch (InterruptedException e) {
                    sipService.runFlag = false;
                    Log.w(TAG, "service thread interrupted!");
                }
            }
        }
    }
}
