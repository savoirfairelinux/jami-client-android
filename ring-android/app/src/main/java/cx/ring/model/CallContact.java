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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.model;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Profile;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import cx.ring.R;

public class CallContact implements Parcelable {
    static final String TAG = CallContact.class.getSimpleName();

    private static final int UNKNOWN_ID = -1;
    public static final int DEFAULT_ID = 0;

    private static final String[] PROFILE_PROJECTION = new String[]{Profile._ID, Profile.LOOKUP_KEY, Profile.DISPLAY_NAME_PRIMARY, Profile.PHOTO_ID};

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
        this(cID, null, null, UNKNOWN_ID);
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

    public static CallContact buildUnknown(SipUri to) {
        ArrayList<Phone> phones = new ArrayList<>();
        phones.add(new Phone(to, 0));
        return new CallContact(UNKNOWN_ID, null, to.getRawUriString(), 0, phones, "", false);
    }

    public static CallContact buildUnknown(String to) {
        ArrayList<Phone> phones = new ArrayList<>();
        phones.add(new Phone(to, 0));

        return new CallContact(UNKNOWN_ID, null, to, 0, phones, "", false);
    }

    public static CallContact buildUnknown(String to, int type) {
        ArrayList<Phone> phones = new ArrayList<>();
        phones.add(new Phone(to, type));
        return new CallContact(UNKNOWN_ID, null, to, 0, phones, "", false);
    }

    public static CallContact buildUserContact(Context c) {
        CallContact result = null;
        try {
            if (null != c) {
                //~ Checking the state of the READ_CONTACTS permission
                boolean hasReadContactsPermission = ContextCompat.checkSelfPermission(c,
                        Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
                if (hasReadContactsPermission) {
                    Cursor mProfileCursor = c.getContentResolver().query(Profile.CONTENT_URI, PROFILE_PROJECTION, null, null, null);
                    if (mProfileCursor != null) {
                        if (mProfileCursor.moveToFirst()) {
                            String key = mProfileCursor.getString(mProfileCursor.getColumnIndex(Profile.LOOKUP_KEY));
                            String displayName = mProfileCursor.getString(mProfileCursor.getColumnIndex(Profile.DISPLAY_NAME_PRIMARY));

                            if (displayName == null || displayName.isEmpty())
                                displayName = c.getResources().getString(R.string.me);
                            result = new CallContact(mProfileCursor.getLong(mProfileCursor.getColumnIndex(Profile._ID)), key, displayName,
                                    mProfileCursor.getLong(mProfileCursor.getColumnIndex(Profile.PHOTO_ID)), new ArrayList<Phone>(), "", true);
                        }
                        mProfileCursor.close();
                    }
                } else {
                    Log.d(TAG, "READ_CONTACTS permission is not granted.");
                }
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }

        //~ Returns the contact if not null
        if (null != result) {
            return result;
        }
        //~ Or returning a default one
        String displayName = (null != c) ? c.getResources().getString(R.string.me) : "Me";
        return new CallContact(UNKNOWN_ID, null, displayName, 0, new ArrayList<Phone>(), "", true);
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
        ArrayList<String> ret = new ArrayList<>(1 + phones.size());
        if (id != UNKNOWN_ID)
            ret.add("c:" + Long.toHexString(id));
        for (Phone p : phones)
            ret.add(p.getNumber().getRawUriString());
        return ret;
    }

    public static long contactIdFromId(String id) {
        if (!id.startsWith("c:"))
            return UNKNOWN_ID;
        try {
            return Long.parseLong(id.substring(2), 16);
        } catch (Exception e) {
            return UNKNOWN_ID;
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
        if (mDisplayName != null && !mDisplayName.isEmpty())
            return mDisplayName;
        if (!phones.isEmpty())
            return phones.get(0).getNumber().getRawUriString();
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

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String mEmail) {
        this.mEmail = mEmail;
    }


    public boolean hasNumber(String number) {
        return hasNumber(new SipUri(number));
    }

    public boolean hasNumber(SipUri number) {
        if (number == null || number.isEmpty())
            return false;
        for (Phone p : phones)
            if (p.getNumber().equals(number))
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
        dest.writeByte(stared ? (byte) 1 : (byte) 0);
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

        public static NumberType fromInteger(int _id) {
            for (NumberType v : VALS)
                if (v.type == _id)
                    return v;
            return UNKNOWN;
        }
    }

    public static class Phone implements Parcelable {
        NumberType ntype;
        SipUri number;
        int category; // Home, work, custom etc.
        String label;

        public Phone(SipUri num, int cat) {
            ntype = NumberType.UNKNOWN;
            number = num;
            label = null;
            category = cat;
        }

        public Phone(String num, int cat) {
            this(num, cat, null);
        }

        public Phone(String num, int cat, String label) {
            ntype = NumberType.UNKNOWN;
            category = cat;
            number = new SipUri(num);
            this.label = label;
        }

        public Phone(String num, int cat, String label, NumberType nty) {
            ntype = nty;
            number = new SipUri(num);
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
            dest.writeParcelable(number, 0);
            dest.writeInt(category);
        }

        private void readFromParcel(Parcel in) {
            ntype = NumberType.fromInteger(in.readInt());
            number = in.readParcelable(SipUri.class.getClassLoader());
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

        public SipUri getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = new SipUri(number);
        }

        public CharSequence getTypeString(Resources r) {
            return ContactsContract.CommonDataKinds.Phone.getTypeLabel(r, category, label);
        }

        public static String getShortenedNumber(String number) {
            if (!TextUtils.isEmpty(number) && number.length() > 18) {
                return number.substring(0, 18).concat("…");
            }
            return number;
        }
    }

    public void addPhoneNumber(String tel) {
        if (!hasNumber(tel))
            phones.add(new Phone(tel, 0));
    }

    public void addPhoneNumber(String tel, int cat, String label) {
        if (!hasNumber(tel))
            phones.add(new Phone(tel, cat, label));
    }

    public void addNumber(String tel, int cat, String label, NumberType type) {
        if (!hasNumber(tel))
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
     *
     * @return true when Name == Number
     */
    public boolean isUnknown() {
        return mDisplayName == null || mDisplayName.contentEquals(phones.get(0).getNumber().getRawUriString());
    }

    public Intent getAddNumberIntent() {
        final Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
        intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);

        ArrayList<ContentValues> data = new ArrayList<>();
        ContentValues values = new ContentValues();

        SipUri number = getPhones().get(0).getNumber();
        if (number.isRingId()) {
            values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE);
            values.put(ContactsContract.CommonDataKinds.Im.DATA, number.getRawUriString());
            values.put(ContactsContract.CommonDataKinds.Im.PROTOCOL, ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM);
            values.put(ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL, "Ring");
        } else {
            values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE);
            values.put(ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS, number.getRawUriString());
        }
        data.add(values);
        intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, data);
        return intent;
    }

    //region Equals
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof CallContact)) {
            return false;
        }
        CallContact contact = (CallContact) o;
        return contact.getId() == this.getId() && contact.getDisplayName().equals(this.getDisplayName());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
    //endregion

    //region Display
    public void displayContact(Context context) {
        if (context == null) {
            Log.d(TAG, "displayContact: context is null");
            return;
        }

        if (getId() == UNKNOWN_ID) {
            Log.d(TAG, "displayContact: contact is unknown");
            displayAddContactConfirmationDialog(context);
        } else {
            Log.d(TAG, "displayContact: contact is known, displaying...");
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI,
                        String.valueOf(getId()));
                intent.setData(uri);
                context.startActivity(intent);
            } catch (ActivityNotFoundException exc) {
                exc.printStackTrace();
            }
        }
    }

    private void displayAddContactConfirmationDialog(final Context context) {
        if (context == null) {
            Log.d(TAG, "displayAddContactConfirmationDialog: context is null");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.ab_action_contact_add_question)
                .setMessage(context.getString(R.string.add_call_contact_number_to_contacts,
                        this.getDisplayName()))
                .setPositiveButton(R.string.ab_action_contact_add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent intent = getAddNumberIntent();
                        context.startActivity(intent);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        /* Terminate with no action */
                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
    //endregion
}
