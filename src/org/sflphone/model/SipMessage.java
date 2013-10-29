package org.sflphone.model;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class SipMessage implements Parcelable {
    public boolean left;
    public String comment;

    public SipMessage(boolean left, String comment) {
        super();
        this.left = left;
        this.comment = comment;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeByte((byte) (left ? 1 : 0));
        out.writeString(comment);
    }

    public static final Parcelable.Creator<SipMessage> CREATOR = new Parcelable.Creator<SipMessage>() {
        public SipMessage createFromParcel(Parcel in) {
            return new SipMessage(in);
        }

        public SipMessage[] newArray(int size) {
            return new SipMessage[size];
        }
    };

    private SipMessage(Parcel in) {
        left = (in.readByte() == 1) ? true : false;
        comment = in.readString();
    }

}