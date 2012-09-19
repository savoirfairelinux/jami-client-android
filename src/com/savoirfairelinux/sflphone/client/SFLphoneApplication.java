package com.savoirfairelinux.sflphone.client;

import android.app.Application;
import android.util.Log;

public class SFLphoneApplication extends Application {
    
    static final String TAG = "SFLphoneApplication";
    private boolean serviceRunning;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.i(TAG, "onTerminate");
    }
    
    public boolean isServiceRunning() {
        return serviceRunning;
    }
    
    public void setServiceRunning(boolean r) {
        this.serviceRunning = r;
    }
}
