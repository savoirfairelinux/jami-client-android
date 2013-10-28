package org.sflphone.utils;

import org.sflphone.service.SipService;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;

public class MediaManager {

    private static final String TAG = MediaManager.class.getSimpleName();
    private SipService mService;
    private SettingsContentObserver mSettingsContentObserver;
    AudioManager mAudioManager;

    public MediaManager(SipService aService) {
        mService = aService;
        mSettingsContentObserver = new SettingsContentObserver(mService, new Handler());
        mAudioManager = (AudioManager) aService.getSystemService(Context.AUDIO_SERVICE);
    }

    public void startService() {
        mService.getApplicationContext().getContentResolver()
                .registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, mSettingsContentObserver);
    }

    public void stopService() {
        Log.i(TAG, "Remove media manager....");
        mService.getApplicationContext().getContentResolver().unregisterContentObserver(mSettingsContentObserver);
    }

    public AudioManager getAudioManager() {
        return mAudioManager;
    }

}
