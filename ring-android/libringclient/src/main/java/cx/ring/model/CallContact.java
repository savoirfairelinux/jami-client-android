/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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

import java.util.ArrayList;
import java.util.Date;

import cx.ring.utils.Log;
import cx.ring.utils.StringUtils;
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
    private String mUsername = null;
    private long mPhotoId;
    private final ArrayList<Phone> mPhones;
    private boolean isUser;
    private boolean stared = false;
    private boolean isFromSystem = false;
    private Status mStatus = Status.NO_REQUEST;
    private Date mAddedDate = null;
    private boolean mOnline = false;

    private boolean usernameLoaded = false;
    public boolean detailsLoaded = false;

    // Profile
    private VCard vcard = null;
    private String mDisplayName;
    private Object mContactPhoto = null;

    private final Subject<CallContact> mContactUpdates = BehaviorSubject.create();
    private Observable<CallContact> mContactObservable;

    public CallContact(long cID) {
        this(cID, null, null, UNKNOWN_ID);
    }

    public CallContact(long cID, String k, String displayName, long photoID) {
        this(cID, k, displayName, photoID, new ArrayList<>(), false);
    }

    private CallContact(long cID, String k, String displayName, long photoID, ArrayList<Phone> p, boolean user) {
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

    public static CallContact buildSIP(Uri to) {
        ArrayList<Phone> phones = new ArrayList<>();
        phones.add(new Phone(to, 0));
        CallContact contact = new CallContact(UNKNOWN_ID, null, null, 0, phones, false);
        contact.usernameLoaded = true;
        return contact;
    }

    public static CallContact build(String to) {
        ArrayList<Phone> phones = new ArrayList<>();
        phones.add(new Phone(to, 0));
        return new CallContact(UNKNOWN_ID, null, null, 0, phones, false);
    }

    public Observable<CallContact> getUpdatesSubject() {
        return mContactUpdates;
    }
    public Observable<CallContact> getUpdates() {
        return mContactObservable;
    }
    public void setUpdates(Observable<CallContact> observable) {
        mContactObservable = observable;
    }

    public boolean matches(String query) {
        return (mDisplayName != null && mDisplayName.toLowerCase().contains(query))
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

    public String getProfileName() {
        return mDisplayName;
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

    public void addPhoneNumber(Uri tel, int cat, String label) {
        if (!hasNumber(tel))
            mPhones.add(new Phone(tel, cat, label));
    }

    public void addNumber(String tel, int cat, String label, Phone.NumberType type) {
        if (!hasNumber(tel))
            mPhones.add(new Phone(tel, cat, label, type));
    }

    public void addNumber(Uri tel, int cat, String label, Phone.NumberType type) {
        if (!hasNumber(tel))
            mPhones.add(new Phone(tel, cat, label, type));
    }

    public boolean isUser() {
        return isUser;
    }

    public boolean hasPhoto() {
        return mContactPhoto != null;
    }

    public Object getPhoto() {
        return mContactPhoto;
    }

    public void setPhoto(Object externalArray) {
        mContactPhoto = externalArray;
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
        } else if (usernameLoaded && !mPhones.isEmpty()) {
            return getPrimaryUri().getRawUriString();
        } else {
            return "";
        }
    }

    public String getUsername() {
        return mUsername;
    }

    public boolean setUsername(String name) {
        if (!usernameLoaded || (name != null && !name.equals(mUsername))) {
            mUsername = name;
            usernameLoaded = true;
            mContactUpdates.onNext(this);
            return true;
        }
        return false;
    }

    public boolean isUsernameLoaded() {
        return usernameLoaded;
    }

    public void setProfile(String name, Object photo) {
        if (!StringUtils.isEmpty(name) && !name.startsWith(Uri.RING_URI_SCHEME)) {
            setDisplayName(name);
        }
        if (photo != null) {
            setPhoto(photo);
        }
        detailsLoaded = true;
        mContactUpdates.onNext(this);
    }

    public void setVCard(VCard vcard) {
        this.vcard = vcard;
    }

    public VCard getVCard() {
        return vcard;
    }
}
