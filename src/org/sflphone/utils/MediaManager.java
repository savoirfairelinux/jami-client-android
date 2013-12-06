package org.sflphone.utils;

import org.sflphone.service.SipService;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Handler;
import android.util.Log;

public class MediaManager implements OnAudioFocusChangeListener {

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

    public void obtainAudioFocus() {
        mAudioManager.requestAudioFocus(this, Compatibility.getInCallStream(false), AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
    }

    @Override
    public void onAudioFocusChange(int arg0) {

    }

    public void abandonAudioFocus() {
        mAudioManager.abandonAudioFocus(this);
        if (mAudioManager.isSpeakerphoneOn()) {
            mAudioManager.setSpeakerphoneOn(false);
        }
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
    }

    public void RouteToSpeaker() {
        mAudioManager.setSpeakerphoneOn(true);
    }

    public void RouteToInternalSpeaker() {
        mAudioManager.setSpeakerphoneOn(false);
    }
}
