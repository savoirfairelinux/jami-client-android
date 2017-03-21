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


import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class CallContact {


    public static final int UNKNOWN_ID = -1;
    public static final int DEFAULT_ID = 0;
    public static final String PREFIX_RING = "ring:";

    private long mId;
    private String mKey;
    private String mDisplayName;
    private long mPhotoId;
    private final ArrayList<Phone> mPhones;
    private boolean isUser;
    private WeakReference<byte[]> mContactPhoto = new WeakReference<>(null);
    private boolean stared = false;
    private boolean isFromSystem = false;
    private boolean isBanned = false;

    public CallContact(long cID) {
        this(cID, null, null, UNKNOWN_ID);
    }

    public CallContact(long cID, String k, String displayName, long photoID) {
        this(cID, k, displayName, photoID, new ArrayList<Phone>(), null, false);
    }

    public CallContact(long cID, String k, String displayName, long photoID, ArrayList<Phone> p, String mail, boolean user) {
        mId = cID;
        mKey = k;
        mDisplayName = displayName;
        mPhones = p;
        mPhotoId = photoID;
        isUser = user;
    }

    public static CallContact buildUnknown(Uri to) {
        ArrayList<Phone> phones = new ArrayList<>();
        phones.add(new Phone(to, 0));
        return new CallContact(UNKNOWN_ID, null, to.getRawUriString(), 0, phones, "", false);
    }

    public static CallContact buildUnknown(String to) {
        ArrayList<Phone> phones = new ArrayList<>();
        phones.add(new Phone(to, 0));

        return new CallContact(UNKNOWN_ID, null, to, 0, phones, "", false);
    }

    public static CallContact buildUnknown(String to, String address) {
        ArrayList<Phone> phones = new ArrayList<>();
        if (address != null) {
            phones.add(new Phone(address, 0));
        } else {
            phones.add(new Phone(to, 0));
        }

        return new CallContact(UNKNOWN_ID, null, to, 0, phones, "", false);
    }

    public static CallContact buildUnknown(String to, int type) {
        ArrayList<Phone> phones = new ArrayList<>();
        phones.add(new Phone(to, type));
        return new CallContact(UNKNOWN_ID, null, to, 0, phones, "", false);
    }

    public void setContactInfos(String k, String displayName, long photo_id) {
        mKey = k;
        mDisplayName = displayName;
        this.mPhotoId = photo_id;
    }

    public static String canonicalNumber(String number) {
        if (number == null || number.isEmpty())
            return null;
        return new Uri(number).getRawUriString();
    }

    public ArrayList<String> getIds() {
        ArrayList<String> ret = new ArrayList<>(1 + mPhones.size());
        if (mId != UNKNOWN_ID)
            ret.add("c:" + Long.toHexString(mId));
        for (Phone p : mPhones)
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

    public long getId() {
        return mId;
    }

    public String getDisplayName() {
        if (mDisplayName != null && !mDisplayName.isEmpty())
            return mDisplayName;
        if (!mPhones.isEmpty())
            return mPhones.get(0).getNumber().getRawUriString();
        return "";
    }

    public long getPhotoId() {
        return mPhotoId;
    }

    public ArrayList<Phone> getPhones() {
        return mPhones;
    }

    public boolean hasNumber(String number) {
        return hasNumber(new Uri(number));
    }

    public boolean hasNumber(Uri number) {
        if (number == null || number.isEmpty())
            return false;
        for (Phone p : mPhones)
            if (p.getNumber().equals(number))
                return true;
        return false;
    }

    @Override
    public String toString() {
        return mDisplayName;
    }

    public void setId(long id) {
        this.mId = id;
    }

    public String getKey() {
        return mKey;
    }

    public void setStared() {
        this.stared = true;
    }

    public boolean isStared() {
        return stared;
    }

    public void addPhoneNumber(String tel) {
        if (!hasNumber(tel))
            mPhones.add(new Phone(tel, 0));
    }

    public void addPhoneNumber(String tel, int cat, String label) {
        if (!hasNumber(tel))
            mPhones.add(new Phone(tel, cat, label));
    }

    public void addNumber(String tel, int cat, String label, Phone.NumberType type) {
        if (!hasNumber(tel))
            mPhones.add(new Phone(tel, cat, label, type));
    }

    public boolean isUser() {
        return isUser;
    }

    public boolean hasPhoto() {
        return mContactPhoto.get() != null;
    }

    public byte[] getPhoto() {
        return mContactPhoto.get();
    }

    public void setPhoto(byte[] externalArray) {
        mContactPhoto = new WeakReference<>(externalArray);
    }

    public boolean isFromSystem() {
        return isFromSystem;
    }

    public boolean isBanned() {
        return isBanned;
    }

    public void setFromSystem(boolean fromSystem) {
        isFromSystem = fromSystem;
    }

    public void setBanned(boolean banned) {
        isBanned = banned;
    }

    /**
     * A contact is Unknown when his name == his phone number
     *
     * @return true when Name == Number
     */
    public boolean isUnknown() {
        return mDisplayName == null || mDisplayName.contentEquals(mPhones.get(0).getNumber().getRawUriString());
    }

    public void resetDisplayName() {
        mDisplayName = null;
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

}
