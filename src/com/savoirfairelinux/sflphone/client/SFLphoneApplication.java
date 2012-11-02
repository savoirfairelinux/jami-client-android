package com.savoirfairelinux.sflphone.client;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.savoirfairelinux.sflphone.service.ISipService;

public class SFLphoneApplication extends Application {

    static final String TAG = "SFLphoneApplication";
    private boolean serviceRunning;
    private ISipService sipService;

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

    public ISipService getSipService() {
        return sipService;
    }

    public void setSipService(ISipService service) {
        sipService = service;
    }

    public String getAppPath() {
        PackageManager pkgMng = getPackageManager();
        String pkgName = getPackageName();

        try {
            PackageInfo pkgInfo = pkgMng.getPackageInfo(pkgName, 0);
            pkgName = pkgInfo.applicationInfo.dataDir;
        } catch (NameNotFoundException e) {
            Log.w(TAG, "Error Package name not found ", e);
        }

        Log.d(TAG, "Application path: " + pkgName);
        return pkgName;
    }
}
