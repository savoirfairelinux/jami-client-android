/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
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
import java.util.Date;

import cx.ring.utils.Log;
import cx.ring.utils.StringUtils;
import cx.ring.utils.Tuple;
import ezvcard.VCard;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class CallContact {
    protected static final String TAG = CallContact.class.getSimpleName();

    public static final int UNKNOWN_ID = -1;
    public static final int DEFAULT_ID = 0;
    public static final String PREFIX_RING = Uri.RING_URI_SCHEME;

    public enum Status {BANNED, REQUEST_SENT, CONFIRMED, NO_REQUEST}

    private long mId;
    private String mKey;
    private String mDisplayName;
    private String mUsername = null;
    private long mPhotoId;
    private final ArrayList<Phone> mPhones;
    private boolean isUser;
    private WeakReference<byte[]> mContactPhoto = new WeakReference<>(null);
    private boolean stared = false;
    private boolean isFromSystem = false;
    private Status mStatus = Status.NO_REQUEST;
    private Date mAddedDate = null;
    private boolean mOnline = false;

    public boolean detailsLoaded = false;
    public VCard vcard = null;

    private Subject<CallContact> mContactUpdates = BehaviorSubject.create();

    public Observable<CallContact> getUpdates() {
        return mContactUpdates;
    }

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
        if (cID != UNKNOWN_ID && (displayName == null || !displayName.contains(PREFIX_RING))) {
            mStatus = Status.CONFIRMED;
        }
    }

    public static CallContact buildUnknown(Uri to) {
        ArrayList<Phone> phones = new ArrayList<>();
        phones.add(new Phone(to, 0));
        return new CallContact(UNKNOWN_ID, null, null, 0, phones, "", false);
    }

    public static CallContact buildUnknown(String to) {
        ArrayList<Phone> phones = new ArrayList<>();
        phones.add(new Phone(to, 0));

        return new CallContact(UNKNOWN_ID, null, null, 0, phones, "", false);
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

    public static CallContact buildRingContact(Uri ringId, String username) {
        CallContact contact = buildUnknown(ringId);
        contact.setUsername(username);
        return contact;
    }

    public boolean matches(String query) {
        return (mDisplayName != null && mDisplayName.contains(query))
                || (mUsername != null && mUsername.contains(query))
                || (getPrimaryNumber().contains(query));
    }

    public boolean isOnline() {
        return mOnline;
    }

    public void setOnline(boolean present) {
        mOnline = present;
    }

    public void setContactInfos(String k, String displayName, long photo_id) {
        mKey = k;
        mDisplayName = displayName;
        this.mPhotoId = photo_id;
        if (mUsername == null && displayName.contains(PREFIX_RING)) {
            mUsername = displayName;
        }
    }

    public static String canonicalNumber(String number) {
        if (number == null || number.isEmpty())
            return null;
        return new Uri(number).getRawUriString();
    }

    public ArrayList<String> getIds() {
        ArrayList<String> ret = new ArrayList<>(mPhones.size() + (mId == UNKNOWN_ID ? 0 : 1));
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
        return !StringUtils.isEmpty(mDisplayName) ? mDisplayName : getRingUsername();
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
        return getRingUsername();
    }

    public void setId(long id) {
        this.mId = id;
    }

    public String getKey() {
        return mKey;
    }

    public String getPrimaryNumber() {
        return getPrimaryUri().getRawRingId();
    }
    public Uri getPrimaryUri() {
        return getPhones().get(0).getNumber();
    }

    public void setStared() {
        this.stared = true;
    }

    public boolean isStared() {
        return stared;
    }

    public void addPhoneNumber(Uri uri) {
        if (!hasNumber(uri))
            mPhones.add(new Phone(uri, 0));
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

    public Status getStatus() {
        return mStatus;
    }

    public void setStatus(Status status) {
        mStatus = status;
    }

    public boolean isBanned() { return  mStatus == Status.BANNED; }

    public void setFromSystem(boolean fromSystem) {
        isFromSystem = fromSystem;
    }

    public void setAddedDate(Date addedDate) {
        mAddedDate = addedDate;
    }
    public Date getAddedDate() {
        return mAddedDate;
    }

    /**
     * A contact is Unknown when his name == his phone number
     *
     * @return true when Name == Number
     */
    public boolean isUnknown() {
        return mDisplayName == null || mDisplayName.contentEquals(mPhones.get(0).getNumber().getRawUriString());
    }

    public void setDisplayName(String displayName) {
        mDisplayName = displayName;
    }

    public String getRingUsername() {
        if (!StringUtils.isEmpty(mUsername)) {
            return mUsername;
        } else if (!mPhones.isEmpty()) {
            return getPrimaryUri().getRawUriString();
        } else {
            return "";
        }
    }

    public String getUsername() {
        return mUsername;
    }

    public boolean setUsername(String name) {
        if (!name.equals(mUsername)) {
            mUsername = name;
            mContactUpdates.onNext(this);
            return true;
        }
        return false;
    }

    private static Tuple<String, byte[]> readVCardContactData(VCard vcard) {
        String contactName = null;
        byte[] photo = null;
        if (vcard != null) {
            if (!vcard.getPhotos().isEmpty()) {
                try {
                    photo = vcard.getPhotos().get(0).getData();
                } catch (Exception e) {
                    Log.w(TAG, "Can't read photo from VCard", e);
                    photo = null;
                }
            }
            if (vcard.getFormattedName() != null) {
                if (!StringUtils.isEmpty(vcard.getFormattedName().getValue())) {
                    contactName = vcard.getFormattedName().getValue();
                }
            }
        }
        return new Tuple<>(contactName, photo);
    }

    public void setProfile(String name, byte[] photo) {
        if (!StringUtils.isEmpty(name) && !name.startsWith(Uri.RING_URI_SCHEME)) {
            setDisplayName(name);
        }
        if (photo != null && photo.length > 0) {
            setPhoto(photo);
        }
        detailsLoaded = true;
        mContactUpdates.onNext(this);
    }

    public void setVCardProfile(VCard vcard) {
        this.vcard = vcard;
        Tuple<String, byte[]> info = readVCardContactData(vcard);
        setProfile(info.first, info.second);
    }

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

}
