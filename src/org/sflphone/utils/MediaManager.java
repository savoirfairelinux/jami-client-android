/*
 *  Copyright (C) 2004-2013 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

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
