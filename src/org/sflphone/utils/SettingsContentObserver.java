package org.sflphone.utils;

import org.sflphone.service.SipService;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;

public class SettingsContentObserver extends ContentObserver {
    int previousVolume;
    SipService context;
    private static final String TAG = "Settings";

    public SettingsContentObserver(SipService c, Handler handler) {
        super(handler);
        context=c;
        
        

        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        
        Log.i(TAG,"audio mode: "+audio.getMode());
        
        previousVolume = audio.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
    }

    @Override
    public boolean deliverSelfNotifications() {
        return super.deliverSelfNotifications();
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = audio.getStreamVolume(AudioManager.STREAM_VOICE_CALL);

        int delta=previousVolume-currentVolume;

        if(delta>0)
        {
            Log.d(TAG,"Decreased");
            previousVolume=currentVolume;
            context.changeVolume(currentVolume);
        }
        else if(delta<0)
        {
            Log.d(TAG,"Increased");
            previousVolume=currentVolume;
            context.changeVolume(currentVolume);
        }
    }
}
