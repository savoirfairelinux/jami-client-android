/*
 *  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
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
package com.savoirfairelinux.sflphone.model;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract.Profile;
import android.util.Log;

public class CallContact implements Parcelable {

    private long id;
    private String mDisplayName;
    private long photo_id;
    private ArrayList<Phone> phones, sip_phones;
    private String mEmail;

    private CallContact(long cID, String displayName, long photoID, ArrayList<Phone> p, ArrayList<Phone> sip, String mail) {
        id = cID;
        mDisplayName = displayName;
        phones = p;
        sip_phones = sip;
        mEmail = mail;
        photo_id = photoID;
    }

    public CallContact(Parcel in) {
        readFromParcel(in);
    }

    public long getId() {
        return id;
    }

    public String getmDisplayName() {
        return mDisplayName;
    }

    public void setmDisplayName(String mDisplayName) {
        this.mDisplayName = mDisplayName;
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

    public String getmEmail() {
        return mEmail;
    }

    public void setmEmail(String mEmail) {
        this.mEmail = mEmail;
    }

    @Override
    public String toString() {
        return mDisplayName;
    }

    public static class ContactBuilder {

        long contactID;
        String contactName;
        long contactPhoto;
        ArrayList<Phone> phones;
        ArrayList<Phone> sip;
        String contactMail;
        boolean hasPhoto;

        public ContactBuilder startNewContact(long id, String displayName, long photo_id) {
            contactID = id;
            contactName = displayName;
            contactPhoto = photo_id;
            phones = new ArrayList<Phone>();
            sip = new ArrayList<Phone>();
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
            return new CallContact(contactID, contactName, contactPhoto, phones, sip, contactMail);
        }

        public static ContactBuilder getInstance() {
            return new ContactBuilder();
        }

        public static CallContact buildUnknownContact(String to) {
            ArrayList<Phone> phones = new ArrayList<Phone>();
            phones.add(new Phone(to, 0));

            return new CallContact(-1, to, 0, phones, new ArrayList<CallContact.Phone>(), "");
        }

        public static CallContact buildUserContact(ContentResolver cr, String displayName) {
            String[] mProjection = new String[] { Profile._ID, Profile.PHOTO_ID };
            Cursor mProfileCursor = cr.query(Profile.CONTENT_URI, mProjection, null, null, null);
            CallContact result = null;
            if (mProfileCursor.getCount() > 0) {
                mProfileCursor.moveToFirst();
                Log.i("CallContact", "THERE IS AN ENTRY");
                result = new CallContact(mProfileCursor.getLong(mProfileCursor.getColumnIndex(Profile._ID)), displayName,
                        mProfileCursor.getLong(mProfileCursor.getColumnIndex(Profile.PHOTO_ID)), new ArrayList<Phone>(),
                        new ArrayList<CallContact.Phone>(), "");
            } else {
                result = new CallContact(-1, displayName, 0, new ArrayList<Phone>(), new ArrayList<CallContact.Phone>(), "");
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

    }

    private void readFromParcel(Parcel in) {

        id = in.readLong();
        mDisplayName = in.readString();
        photo_id = in.readLong();
        phones = new ArrayList<CallContact.Phone>();
        sip_phones = new ArrayList<CallContact.Phone>();
        in.readTypedList(phones, Phone.CREATOR);
        in.readTypedList(sip_phones, Phone.CREATOR);
        mEmail = in.readString();
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public CallContact createFromParcel(Parcel in) {
            return new CallContact(in);
        }

        @Override
        public CallContact[] newArray(int size) {
            return new CallContact[size];
        }
    };

    public static class Phone implements Parcelable {

        int type;
        String number;

        public Phone(String num, int ty) {
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
            dest.writeInt(type);
            dest.writeString(number);
        }

        private void readFromParcel(Parcel in) {
            type = in.readInt();
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

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
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

}
