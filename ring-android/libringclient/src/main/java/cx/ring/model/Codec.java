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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.model;

import java.util.Map;

public class Codec{

    public enum Type {AUDIO, VIDEO}

    long payload;
    String name;
    Type type;
    String sampleRate;
    String bitRate;
    String channels;
    boolean enabled;

    public Codec(long i, Map<String, String> audioCodecDetails, boolean b) {
       /* Log.d("CodecDetail", Long.toString(i));
        for (String s : audioCodecDetails.keys()) {
            Log.d("CodecDetail", s + " -> " + audioCodecDetails.get(s));
        }*/
        payload = i;
        name = audioCodecDetails.get("CodecInfo.name");
        type = audioCodecDetails.get("CodecInfo.type").contentEquals("AUDIO") ? Type.AUDIO : Type.VIDEO;
        if (audioCodecDetails.containsKey("CodecInfo.sampleRate"))
            sampleRate = audioCodecDetails.get("CodecInfo.sampleRate");
        if (audioCodecDetails.containsKey("CodecInfo.bitrate"))
            bitRate = audioCodecDetails.get("CodecInfo.bitrate");
        if (audioCodecDetails.containsKey("CodecInfo.channelNumber"))
            channels = audioCodecDetails.get("CodecInfo.channelNumber");
        enabled = b;
    }

    @Override
    public String toString() {
        return "Codec: " + name + "\n" + "Payload: " + payload + "\n" + "Sample Rate: " + sampleRate + "\n" + "Bit Rate: " + bitRate + "\n"
                + "Channels: " + channels;
    }

    public Type getType() {
        return type;
    }

    public Long getPayload() {
        return payload;
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
