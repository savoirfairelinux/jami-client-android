package cx.ring.account;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import cx.ring.mvp.RingAccountViewModel;

/**
 * Created by hdesousa on 22/08/17.
 */

public class RingAccountViewModelImpl extends RingAccountViewModel implements Parcelable {

    private Bitmap photo;

    public Bitmap getPhoto() {
        return photo;
    }

    public void setPhoto(Bitmap photo) {
        this.photo = photo;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.photo, flags);
        dest.writeString(this.mFullName);
        dest.writeString(this.mUsername);
        dest.writeString(this.mPassword);
        dest.writeString(this.mPin);
        dest.writeByte(this.link ? (byte) 1 : (byte) 0);
    }

    public RingAccountViewModelImpl() {
    }

    protected RingAccountViewModelImpl(Parcel in) {
        this.photo = in.readParcelable(Bitmap.class.getClassLoader());
        this.mFullName = in.readString();
        this.mUsername = in.readString();
        this.mPassword = in.readString();
        this.mPin = in.readString();
        this.link = in.readByte() != 0;
    }

    public static final Creator<RingAccountViewModelImpl> CREATOR = new Creator<RingAccountViewModelImpl>() {
        @Override
        public RingAccountViewModelImpl createFromParcel(Parcel source) {
            return new RingAccountViewModelImpl(source);
        }

        @Override
        public RingAccountViewModelImpl[] newArray(int size) {
            return new RingAccountViewModelImpl[size];
        }
    };
}
