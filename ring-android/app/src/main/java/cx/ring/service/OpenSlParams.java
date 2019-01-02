/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.service;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

/**
 * This class illustrates how to query OpenSL config parameters on Jelly Bean MR1 while maintaining
 * backward compatibility with older versions of Android. The trick is to place the new API calls in
 * an inner class that will only be loaded if we're running on JB MR1 or later.
 */
public abstract class OpenSlParams {

    private OpenSlParams() {
        // Not meant to be instantiated except here.
    }

    /**
     * @param context, e.g., the current activity.
     * @return OpenSlParams instance for the given context.
     */
    public static OpenSlParams createInstance(Context context) {
        return new JellyBeanMr1OpenSlParams(context);
    }

    /**
     * @return The recommended sample rate in Hz.
     */
    public abstract int getSampleRate();

    /**
     * @return The recommended buffer size in frames.
     */
    public abstract int getBufferSize();

    // Implementation for Jelly Bean MR1 or later.
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private static class JellyBeanMr1OpenSlParams extends OpenSlParams {

        private final int sampleRate;
        private final int bufferSize;

        private JellyBeanMr1OpenSlParams(Context context) {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            // Provide default values in case config lookup fails.
            int sr = 44100;
            int bs = 64;
            try {
                // If possible, query the native sample rate and buffer size.
                sr = Integer.parseInt(am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE));
                bs = Integer.parseInt(am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER));
            } catch (NumberFormatException e) {
                Log.w(getClass().getName(), "Failed to read native OpenSL config: " + e);
            }
            sampleRate = sr;
            bufferSize = bs;
        }

        @Override
        public int getSampleRate() {
            return sampleRate;
        }

        @Override
        public int getBufferSize() {
            return bufferSize;
        }
    }

}
