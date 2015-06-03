/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
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

package cx.ring.model;

import cx.ring.service.StringMap;
import cx.ring.service.StringVect;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.Map;

public class Codec implements Parcelable {
    long payload;
    String name;
    String type;
    String sampleRate;
    String bitRate;
    String channels;
    boolean enabled;

    public Codec(long i, StringMap audioCodecDetails, boolean b) {
        Log.d("CodecDetail", Long.toString(i));
        for (String s : audioCodecDetails.keys()) {
            Log.d("CodecDetail", s + " -> " + audioCodecDetails.get(s));
        }
        payload = i;
        name = audioCodecDetails.get("CodecInfo.name");
        type = audioCodecDetails.get("CodecInfo.type");
        if (audioCodecDetails.has_key("CodecInfo.sampleRate"))
            sampleRate = audioCodecDetails.get("CodecInfo.sampleRate");
        if (audioCodecDetails.has_key("CodecInfo.bitrate"))
            bitRate = audioCodecDetails.get("CodecInfo.bitrate");
        if (audioCodecDetails.has_key("CodecInfo.channelNumber"))
            channels = audioCodecDetails.get("CodecInfo.channelNumber");
        enabled = b;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(payload);
        out.writeString(name);
        out.writeString(sampleRate);
        out.writeString(bitRate);
        out.writeString(channels);
        out.writeByte((byte) (enabled ? 1 : 0));
    }

    public static final Parcelable.Creator<Codec> CREATOR = new Parcelable.Creator<Codec>() {
        public Codec createFromParcel(Parcel in) {
            return new Codec(in);
        }

        public Codec[] newArray(int size) {
            return new Codec[size];
        }
    };

    private Codec(Parcel in) {
        payload = in.readInt();
        name = in.readString();
        sampleRate = in.readString();
        bitRate = in.readString();
        channels = in.readString();
        enabled = in.readByte() == 1;
    }

    public Codec(Codec c) {
        payload = c.payload;
        name = c.name;
        sampleRate = c.sampleRate;
        bitRate = c.bitRate;
        channels = c.channels;
        enabled = c.enabled;
    }

    @Override
    public String toString() {
        return "Codec: " + name + "\n" + "Payload: " + payload + "\n" + "Sample Rate: " + sampleRate + "\n" + "Bit Rate: " + bitRate + "\n"
                + "Channels: " + channels;
    }

    public CharSequence getPayload() {
        return Long.toString(payload);
    }

    public CharSequence getName() {
        return name;
    }

    public String getSampleRate() {
        return sampleRate;
    }

    public String getBitRate() {
        return bitRate;
    }

    public String getChannels() {
        return channels;
    }

    public boolean isEnabled() {
       return enabled;
    }

    public void setEnabled(boolean b) {
        enabled = b;
    }

    public void toggleState() {
        enabled = !enabled;
        
    }
    
    @Override
    public boolean equals(Object o){
        return o instanceof Codec && ((Codec) o).payload == payload;
    }

    public boolean isSpeex() {
        return name.contentEquals("speex");
    }   

}
