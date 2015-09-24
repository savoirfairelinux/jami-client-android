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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract.Profile;

import cx.ring.client.ConversationActivity;

public class CallContact implements Parcelable {
    static public final Pattern ANGLE_BRACKETS_PATTERN = Pattern.compile("(?:[^<>]+<)?([^<>]+)>?\\s*");

    public static int DEFAULT_ID = 0;

    private long id;
    private String mDisplayName;
    private long photo_id;
    private ArrayList<Phone> phones, sip_phones;
    private String mEmail;
    private boolean isUser;
    private WeakReference<Bitmap> contact_photo = new WeakReference<>(null);
    private String displayName;

    private CallContact(long cID, String displayName, long photoID, ArrayList<Phone> p, ArrayList<Phone> sip, String mail, boolean user) {
        id = cID;
        mDisplayName = displayName;
        phones = p;
        sip_phones = sip;
        mEmail = mail;
        photo_id = photoID;
        isUser = user;
    }

    public static String canonicalNumber(String number) {
        Matcher m = ANGLE_BRACKETS_PATTERN.matcher(number);
        if (m.find())
            return m.group(1);
        return number;
    }

    public ArrayList<String> getIds() {
        ArrayList<String> ret = new ArrayList<>(1+phones.size()+sip_phones.size());
        if (id != -1)
            ret.add("c:" + Long.toHexString(id));
        for (Phone p : phones)
            ret.add(canonicalNumber(p.getNumber()));
        for (Phone p : sip_phones)
            ret.add(canonicalNumber(p.getNumber()));
        return ret;
    }

    public CallContact(Parcel in) {
        readFromParcel(in);
    }

    public long getId() {
        return id;
    }

    public String getDisplayName() {
        if (!mDisplayName.isEmpty())
            return mDisplayName;
        if (!phones.isEmpty())
            return phones.get(0).getNumber();
        if (!sip_phones.isEmpty())
            return sip_phones.get(0).getNumber();
        return "";
    }

    public long getPhoto_id() {
        return photo_id;
    }

    public void setPhoto_id(long photo_id) {
        this.photo_id = photo_id;
    }

    public ArrayList<Phone> getPhones() {
        return phones;
    }

    public void setPhones(ArrayList<Phone> phones) {
        this.phones = phones;
    }

    public ArrayList<Phone> getSip_phones() {
        return sip_phones;
    }

    public void setSip_phones(ArrayList<Phone> sip_phones) {
        this.sip_phones = sip_phones;
    }

    public Phone getSipPhone() {
        if (sip_phones.size() > 0) {
            return sip_phones.get(0);
        }
        if (phones.size() > 0) {
            return phones.get(0);
        }
        return null;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String mEmail) {
        this.mEmail = mEmail;
    }

    public boolean hasNumber(String number) {
        number = canonicalNumber(number);
        for (Phone p : phones)
            if (canonicalNumber(p.getNumber()).equals(number))
                return true;
        for (Phone p : sip_phones)
            if (canonicalNumber(p.getNumber()).equals(number))
                return true;
        return false;
    }

    @Override
    public String toString() {
        return mDisplayName;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public static class ContactBuilder {

        long contactID;
        String contactName;
        long contactPhoto;
        ArrayList<Phone> phones;
        ArrayList<Phone> sip;
        String contactMail;

        public ContactBuilder startNewContact(long id, String displayName, long photo_id) {
            contactID = id;

            contactName = displayName;
            contactPhoto = photo_id;
            phones = new ArrayList<>();
            sip = new ArrayList<>();
            return this;
        }

        public ContactBuilder addPhoneNumber(String num, int type) {
            phones.add(new Phone(num, type));
            return this;
        }

        public ContactBuilder addSipNumber(String num, int type) {
            sip.add(new Phone(num, type));
            return this;
        }

        public CallContact build() {
            return new CallContact(contactID, contactName, contactPhoto, phones, sip, contactMail, false);
        }

        public static ContactBuilder getInstance() {
            return new ContactBuilder();
        }

        public static CallContact buildUnknownContact(String to) {
            ArrayList<Phone> phones = new ArrayList<>();
            phones.add(new Phone(to, 0));

            return new CallContact(-1, to, 0, phones, new ArrayList<CallContact.Phone>(), "", false);
        }
        public static CallContact buildUnknownContact(String to, int type) {
            ArrayList<Phone> phones = new ArrayList<>();
            phones.add(new Phone(to, type));

            return new CallContact(-1, to, 0, phones, new ArrayList<CallContact.Phone>(), "", false);
        }

        public static CallContact buildUserContact(ContentResolver cr) {
            String[] mProjection = new String[] { Profile._ID, Profile.DISPLAY_NAME_PRIMARY, Profile.PHOTO_ID };
            Cursor mProfileCursor = cr.query(Profile.CONTENT_URI, mProjection, null, null, null);
            CallContact result;
            if (mProfileCursor.getCount() > 0) {
                mProfileCursor.moveToFirst();
                String displayName = mProfileCursor.getString(mProfileCursor.getColumnIndex(Profile.DISPLAY_NAME_PRIMARY));

                result = new CallContact(mProfileCursor.getLong(mProfileCursor.getColumnIndex(Profile._ID)), displayName,
                        mProfileCursor.getLong(mProfileCursor.getColumnIndex(Profile.PHOTO_ID)), new ArrayList<Phone>(),
                        new ArrayList<CallContact.Phone>(), "", true);
            } else {
                result = new CallContact(-1, "Me", 0, new ArrayList<Phone>(), new ArrayList<CallContact.Phone>(), "", true);
            }
            mProfileCursor.close();
            return result;
        }

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(mDisplayName);
        dest.writeLong(photo_id);
        dest.writeTypedList(phones);

        dest.writeTypedList(sip_phones);

        dest.writeString(mEmail);
        dest.writeByte((byte) (isUser ? 1 : 0));

    }

    private void readFromParcel(Parcel in) {

        id = in.readLong();
        mDisplayName = in.readString();
        photo_id = in.readLong();
        phones = new ArrayList<>();
        sip_phones = new ArrayList<>();
        in.readTypedList(phones, Phone.CREATOR);
        in.readTypedList(sip_phones, Phone.CREATOR);
        mEmail = in.readString();
        isUser = in.readByte() == 1;
    }

    public static final Parcelable.Creator<CallContact> CREATOR = new Parcelable.Creator<CallContact>() {
        @Override
        public CallContact createFromParcel(Parcel in) {
            return new CallContact(in);
        }

        @Override
        public CallContact[] newArray(int size) {
            return new CallContact[size];
        }
    };

    public static final Pattern URI_NUMBER_REGEX = Pattern.compile("^\\s+(?:ring(?:[\\s\\:]+))?(\\p{XDigit}{40})\\s+$", Pattern.CASE_INSENSITIVE);
    public static final Pattern RING_NUMBER_REGEX = Pattern.compile("^\\s+(?:ring(?:[\\s\\:]+))?(\\p{XDigit}{40})\\s+$", Pattern.CASE_INSENSITIVE);

    public enum NumberType {
        UNKNOWN(0),
        TEL(1),
        SIP(2),
        IP(2),
        RING(3);

        private final int type;
        NumberType(int t) {
            type = t;
        }
        private static final NumberType[] VALS = NumberType.values();
        public static NumberType fromInteger(int _id)
        {
            for (NumberType v : VALS)
                if (v.type == _id)
                    return v;
            return UNKNOWN;
        }
        public static NumberType guess(String num) {
            String canon = canonicalNumber(num);
            Matcher m = URI_NUMBER_REGEX.matcher(canon);

            return UNKNOWN;
        }
    }

    public static class Phone implements Parcelable {
        NumberType type;
        String number;

        public Phone(String num, int ty) {
            type = NumberType.fromInteger(ty);
            number = num;
        }
        public Phone(String num, NumberType ty) {
            type = ty;
            number = num;
        }

        public Phone(Parcel in) {
            readFromParcel(in);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int arg1) {
            dest.writeInt(type.type);
            dest.writeString(number);
        }

        private void readFromParcel(Parcel in) {
            type = NumberType.fromInteger(in.readInt());
            number = in.readString();
        }

        public static final Parcelable.Creator<Phone> CREATOR = new Parcelable.Creator<Phone>() {
            @Override
            public Phone createFromParcel(Parcel in) {
                return new Phone(in);
            }

            @Override
            public Phone[] newArray(int size) {
                return new Phone[size];
            }
        };

        public NumberType getType() {
            return type;
        }

        public void setType(int type) {
            this.type = NumberType.fromInteger(type);
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

    }

    public void addPhoneNumber(String tel, int type) {
        phones.add(new Phone(tel, type));

    }

    public void addSipNumber(String tel, int type) {
        sip_phones.add(new Phone(tel, type));

    }

    public boolean isUser() {
        return isUser;
    }

    public boolean hasPhoto() {
        if (contact_photo.get() != null)
            return true;
        return false;
    }

    public Bitmap getPhoto() {
        return contact_photo.get();
    }

    public void setPhoto(Bitmap externalBMP) {
        contact_photo = new WeakReference<>(externalBMP);
    }

    /**
     * A contact is Unknown when his name == his phone number
     * @return true when Name == Number
     */
    public boolean isUnknown() {
       return mDisplayName.contentEquals(phones.get(0).getNumber());
    }

}
