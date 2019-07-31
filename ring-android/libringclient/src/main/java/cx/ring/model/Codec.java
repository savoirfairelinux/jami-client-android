/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.model;

import java.util.Map;

import cx.ring.utils.Log;

public class Codec {

    public enum Type {AUDIO, VIDEO}

    private long mPayload;
    private String mName;
    private Type mType;
    private String mSampleRate;
    private String mBitRate;
    private String mChannels;
    private boolean mIsEnabled;

    public Codec(long i, Map<String, String> audioCodecDetails, boolean enabled) {
        mPayload = i;
        mName = audioCodecDetails.get("CodecInfo.name");
        mType = audioCodecDetails.get("CodecInfo.type").contentEquals("AUDIO") ? Type.AUDIO : Type.VIDEO;
        if (audioCodecDetails.containsKey("CodecInfo.sampleRate")) {
            mSampleRate = audioCodecDetails.get("CodecInfo.sampleRate");
        }
        if (audioCodecDetails.containsKey("CodecInfo.bitrate")) {
            mBitRate = audioCodecDetails.get("CodecInfo.bitrate");
        }
        if (audioCodecDetails.containsKey("CodecInfo.channelNumber")) {
            mChannels = audioCodecDetails.get("CodecInfo.channelNumber");
        }
        mIsEnabled = enabled;
    }

    @Override
    public String toString() {
        return "Codec: " + getName()
                + "\n" + "Payload: " + getPayload()
                + "\n" + "Sample Rate: " + getSampleRate()
                + "\n" + "Bit Rate: " + getBitRate()
                + "\n" + "Channels: " + getChannels();
    }

    public Type getType() {
        return mType;
    }

    public Long getPayload() {
        return mPayload;
    }

    public CharSequence getName() {
        return mName;
    }

    public String getSampleRate() {
        return mSampleRate;
    }

    public String getBitRate() {
        return mBitRate;
    }

    public String getChannels() {
        return mChannels;
    }

    public boolean isEnabled() {
        return mIsEnabled;
    }

    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;
    }

    public void toggleState() {
        mIsEnabled = !mIsEnabled;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Codec && ((Codec) o).mPayload == mPayload;
    }

    public boolean isSpeex() {
        return mName.contentEquals("speex");
    }

}
