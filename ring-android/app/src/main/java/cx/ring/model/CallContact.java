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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Profile;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.support.annotation.NonNull;

public class CallContact implements Parcelable {
    public static int DEFAULT_ID = 0;

    private long id;
    private String key;
    private String mDisplayName;
    private long photo_id;
    private final ArrayList<Phone> phones;
    private String mEmail;
    private boolean isUser;
    private WeakReference<Bitmap> contact_photo = new WeakReference<>(null);
    private boolean stared = false;

    public CallContact(long cID) {
        this(cID, null, null, -1);
    }

    public CallContact(long cID, String k, String displayName, long photoID) {
        this(cID, k, displayName, photoID, new ArrayList<Phone>(), null, false);
    }

    public CallContact(long cID, String k, String displayName, long photoID, ArrayList<Phone> p, String mail, boolean user) {
        id = cID;
        key = k;
        mDisplayName = displayName;
        phones = p;
        mEmail = mail;
        photo_id = photoID;
        isUser = user;
    }

    public void setContactInfos(String k, String displayName, long photo_id) {
        key = k;
        mDisplayName = displayName;
        this.photo_id = photo_id;
    }

    public static String canonicalNumber(String number) {
        if (number == null || number.isEmpty())
            return null;
        return new SipUri(number).getRawUriString();
    }

    public ArrayList<String> getIds() {
        ArrayList<String> ret = new ArrayList<>(1+phones.size());
        if (id != -1)
            ret.add("c:" + Long.toHexString(id));
        for (Phone p : phones)
            ret.add(canonicalNumber(p.getNumber()));
        return ret;
    }

    public static long contactIdFromId(String id) {
        if (!id.startsWith("c:"))
            return -1;
        try {
            return Long.parseLong(id.substring(2), 16);
        } catch (Exception e) {
            return -1;
        }
    }

    public CallContact(Parcel in) {
        phones = new ArrayList<>();
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
        return "";
    }

    public long getPhotoId() {
        return photo_id;
    }

    public void setPhotoId(long photo_id) {
        this.photo_id = photo_id;
    }

    public ArrayList<Phone> getPhones() {
        return phones;
    }

    /*public void setPhones(ArrayList<Phone> phones) {
        this.phones = phones;
    }*/

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String mEmail) {
        this.mEmail = mEmail;
    }

    public boolean hasNumber(String number) {
        if (number == null || number.isEmpty())
            return false;
        number = canonicalNumber(number);
        for (Phone p : phones)
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
        this.mDisplayName = displayName;
    }

    public String getKey() {
        return key;
    }

    public void setStared(boolean stared) {
        this.stared = stared;
    }
    public void setStared() {
        this.stared = true;
    }

    public boolean isStared() {
        return stared;
    }

    public static class ContactBuilder {

        long contactID;
        String key;
        String contactName;
        long contactPhoto;
        ArrayList<Phone> phones;
        String contactMail;

        public ContactBuilder startNewContact(long id, String k, String displayName, long photo_id) {
            contactID = id;
            key = k;
            contactName = displayName;
            contactPhoto = photo_id;
            phones = new ArrayList<>();
            return this;
        }

        public ContactBuilder addPhoneNumber(String num, int type, String label) {
            phones.add(new Phone(num, type, label));
            return this;
        }

        public ContactBuilder addSipNumber(String num, int type, String label) {
            phones.add(new Phone(num, type, label, NumberType.SIP));
            return this;
        }

        public CallContact build() {
            return new CallContact(contactID, key, contactName, contactPhoto, phones, contactMail, false);
        }

        public static ContactBuilder getInstance() {
            return new ContactBuilder();
        }

        public static CallContact buildUnknownContact(String to) {
            ArrayList<Phone> phones = new ArrayList<>();
            phones.add(new Phone(to, 0));

            return new CallContact(-1, null, to, 0, phones, "", false);
        }
        public static CallContact buildUnknownContact(String to, int type) {
            ArrayList<Phone> phones = new ArrayList<>();
            phones.add(new Phone(to, type));
            return new CallContact(-1, null, to, 0, phones, "", false);
        }

        public static CallContact buildUserContact(ContentResolver c) {
            String[] mProjection = new String[] { Profile._ID, Profile.LOOKUP_KEY, Profile.DISPLAY_NAME_PRIMARY, Profile.PHOTO_ID };
            Cursor mProfileCursor = c.query(Profile.CONTENT_URI, mProjection, null, null, null);
            CallContact result;
            if (mProfileCursor.getCount() > 0) {
                mProfileCursor.moveToFirst();
                String key = mProfileCursor.getString(mProfileCursor.getColumnIndex(Profile.LOOKUP_KEY));
                String displayName = mProfileCursor.getString(mProfileCursor.getColumnIndex(Profile.DISPLAY_NAME_PRIMARY));

                result = new CallContact(mProfileCursor.getLong(mProfileCursor.getColumnIndex(Profile._ID)), key, displayName,
                        mProfileCursor.getLong(mProfileCursor.getColumnIndex(Profile.PHOTO_ID)), new ArrayList<Phone>(), "", true);
            } else {
                result = new CallContact(-1, null, "Me", 0, new ArrayList<Phone>(), "", true);
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
        dest.writeString(key);
        dest.writeString(mDisplayName);
        dest.writeLong(photo_id);
        dest.writeTypedList(phones);
        dest.writeString(mEmail);
        dest.writeByte((byte) (isUser ? 1 : 0));
        dest.writeByte(stared ? (byte)1 : (byte)0);
    }

    private void readFromParcel(Parcel in) {
        id = in.readLong();
        key = in.readString();
        mDisplayName = in.readString();
        photo_id = in.readLong();
        phones.clear();
        in.readTypedList(phones, Phone.CREATOR);
        mEmail = in.readString();
        isUser = in.readByte() != 0;
        stared = in.readByte() != 0;
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
    }

    public static class Phone implements Parcelable {
        NumberType ntype;
        String number;
        int category; // Home, work, custom etc.
        String label;

        public Phone(String num, int cat) {
            this(num, cat, null);
        }

        public Phone(String num, int cat, String label) {
            ntype = NumberType.UNKNOWN;
            category = cat;
            number = num;
            this.label = label;
        }
        public Phone(String num, int cat, String label, NumberType nty) {
            ntype = nty;
            number = num;
            this.label = label;
            category = cat;
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
            dest.writeInt(ntype.type);
            dest.writeString(number);
            dest.writeInt(category);
        }

        private void readFromParcel(Parcel in) {
            ntype = NumberType.fromInteger(in.readInt());
            number = in.readString();
            category = in.readInt();
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
            return ntype;
        }

        public void setType(int type) {
            this.ntype = NumberType.fromInteger(type);
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public CharSequence getTypeString(Resources r) {
            return ContactsContract.CommonDataKinds.Phone.getTypeLabel(r, category, label);
        }
    }

    public void addPhoneNumber(String tel, int car) {
        phones.add(new Phone(tel, car));

    }
    public void addNumber(String tel, int cat, String label, NumberType type) {
        phones.add(new Phone(tel, cat, label, type));

    }

    public boolean isUser() {
        return isUser;
    }

    public boolean hasPhoto() {
        return contact_photo.get() != null;
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
