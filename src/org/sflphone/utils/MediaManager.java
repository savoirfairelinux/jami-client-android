package org.sflphone.utils;

import org.sflphone.service.SipService;

import android.os.Handler;
import android.util.Log;

public class MediaManager {

    private static final String TAG = MediaManager.class.getSimpleName();
    private SipService mService;
    private SettingsContentObserver mSettingsContentObserver;

    public MediaManager(SipService aService) {
        mService = aService;
        mSettingsContentObserver = new SettingsContentObserver(mService, new Handler());
    }

    public void startService() {
        mService.getApplicationContext().getContentResolver()
                .registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, mSettingsContentObserver);
    }

    public void stopService() {
        Log.i(TAG, "Remove media manager....");
        mService.getApplicationContext().getContentResolver().unregisterContentObserver(mSettingsContentObserver);
    }

}
