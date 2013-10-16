package org.sflphone.model;

import org.sflphone.service.StringVect;

import android.os.Parcel;
import android.os.Parcelable;

public class Codec implements Parcelable {
    int payload;
    String name;
    String sampleRate;
    String bitRate;
    String channels;
    boolean enabled;

    public Codec(int i, StringVect audioCodecDetails, boolean b) {
        payload = i;
        name = audioCodecDetails.get(0);
        sampleRate = audioCodecDetails.get(1);
        bitRate = audioCodecDetails.get(2);
        channels = audioCodecDetails.get(3);
        enabled = b;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(payload);
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
        enabled = in.readByte() == 1 ? true : false;
    }

    @Override
    public String toString() {
        String str = "Codec: " + name + "\n" + "Payload: " + payload + "\n" + "Sample Rate: " + sampleRate + "\n" + "Bit Rate: " + bitRate + "\n"
                + "Channels: " + channels;
        return str;
    }

    public CharSequence getPayload() {
        return Integer.toString(payload);
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

}
