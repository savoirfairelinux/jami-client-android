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
    private Ringer ringer;

    public MediaManager(SipService aService) {
        mService = aService;
        mSettingsContentObserver = new SettingsContentObserver(mService, new Handler());
        mAudioManager = (AudioManager) aService.getSystemService(Context.AUDIO_SERVICE);
        
        ringer = new Ringer(aService);
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

    public void obtainAudioFocus(boolean requestSpeakerOn) {
        mAudioManager.requestAudioFocus(this, Compatibility.getInCallStream(false), AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        if(requestSpeakerOn && !mAudioManager.isWiredHeadsetOn()){
            RouteToSpeaker();
        }
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
    
    
    /**
     * Start ringing announce for a given contact.
     * It will also focus audio for us.
     * @param remoteContact the contact to ring for. May resolve the contact ringtone if any.
     */
    synchronized public void startRing(String remoteContact) {
        
        if(!ringer.isRinging()) {
            ringer.ring(remoteContact, "USELESS");
        }else {
            Log.d(TAG, "Already ringing ....");
        }
        
    }
    
    /**
     * Stop all ringing. <br/>
     * Warning, this will not unfocus audio.
     */
    synchronized public void stopRing() {
        if(ringer.isRinging()) {
            ringer.stopRing();
        }
    }
}
