/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
 */

package cx.ring.utils;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;

public class SettingsContentObserver extends ContentObserver {
    private static final String TAG = "Settings";

    private final AudioManager audioManager;
    private double previousVolume;

    public SettingsContentObserver(Context c, Handler handler) {
        super(handler);
        audioManager = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
    }

    @Override
    public boolean deliverSelfNotifications() {
        return super.deliverSelfNotifications();
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        double currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        double delta = previousVolume - currentVolume;
        if(delta>0)  {
            Log.d(TAG,"Decreased");
            previousVolume=currentVolume;
//            context.changeVolume(currentVolume);
        } else if(delta<0) {
            Log.d(TAG,"Increased");
            previousVolume=currentVolume;
//            context.changeVolume(currentVolume);
        }
    }
}
