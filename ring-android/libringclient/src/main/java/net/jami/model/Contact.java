/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package net.jami.model;

import net.jami.utils.StringUtils;

import java.util.ArrayList;
import java.util.Date;

import io.reactivex.rxjava3.core.Emitter;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.Subject;

public class Contact {
    protected static final String TAG = Contact.class.getSimpleName();

    public static final int UNKNOWN_ID = -1;
    public static final int DEFAULT_ID = 0;
    public static final String PREFIX_RING = Uri.RING_URI_SCHEME;

    public enum Status {BANNED, REQUEST_SENT, CONFIRMED, NO_REQUEST}

    private final Uri mUri;

    private String mUsername = null;
    private long mPhotoId;
    private final ArrayList<Phone> mPhones = new ArrayList<>();
    private final boolean isUser;
    private boolean stared = false;
    private boolean isFromSystem = false;
    private Status mStatus = Status.NO_REQUEST;
    private Date mAddedDate = null;
    private boolean mOnline = false;

    private long mId;
    private String mLookupKey;

    private boolean usernameLoaded = false;
    public boolean detailsLoaded = false;
    private final BehaviorSubject<Uri> mConversationUri;

    // Profile
    private String mDisplayName;
    private Object mContactPhoto = null;

    private final Subject<Contact> mContactUpdates = BehaviorSubject.create();
    private Observable<Contact> mContactObservable;

    private Observable<Boolean> mContactPresenceObservable;
    private Emitter<Boolean> mContactPresenceEmitter;

    public Contact(Uri uri) {
        this(uri, false);
    }

    public Contact(Uri uri, boolean user) {
        this(uri, null, user);
    }

    private Contact(Uri uri, String displayName, boolean user) {
        mUri = uri;
        mDisplayName = displayName;
        isUser = user;
        mConversationUri = BehaviorSubject.createDefault(mUri);
        /*if (cID != UNKNOWN_ID && (displayName == null || !displayName.contains(PREFIX_RING))) {
            mStatus = Status.CONFIRMED;
        }*/
    }

    public void setConversationUri(Uri conversationUri) {
        mConversationUri.onNext(conversationUri);
    }

    public Observable<Uri> getConversationUri() {
        return mConversationUri;
    }

    public static Contact buildSIP(Uri to) {
        Contact contact = new Contact(to);
        contact.usernameLoaded = true;
        return contact;
    }

    public static Contact build(String uri, boolean isUser) {
        return new Contact(Uri.fromString(uri), isUser);
    }
    public static Contact build(String uri) {
        return build(uri, false);
    }

    public Observable<Contact> getUpdatesSubject() {
        return mContactUpdates;
    }
    public Observable<Contact> getUpdates() {
        return mContactObservable;
    }
    public void setUpdates(Observable<Contact> observable) {
        mContactObservable = observable;
    }

    public Observable<Boolean> getPresenceUpdates() {
        return mContactPresenceObservable;
    }
    public void setPresenceUpdates(Observable<Boolean> observable) {
        mContactPresenceObservable = observable;
    }
    public void setPresenceEmitter(Emitter<Boolean> emitter) {
        if (mContactPresenceEmitter != null && mContactPresenceEmitter != emitter) {
            mContactPresenceEmitter.onComplete();
        }
        mContactPresenceEmitter = emitter;
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
        if (mContactPresenceEmitter != null)
            mContactPresenceEmitter.onNext(present);
    }

    public void setSystemId(long id) {
        mId = id;
    }

    public void setSystemContactInfo(long id, String k, String displayName, long photo_id) {
        mId = id;
        mLookupKey = k;
        mDisplayName = displayName;
        this.mPhotoId = photo_id;
        if (mUsername == null && displayName.contains(PREFIX_RING)) {
            mUsername = displayName;
        }
    }

    public static String canonicalNumber(String number) {
        if (number == null || number.isEmpty())
            return null;
        return Uri.fromString(number).getRawUriString();
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
        return hasNumber(Uri.fromString(number));
    }

    public boolean hasNumber(Uri number) {
        if (number == null || number.isEmpty())
            return false;
        for (Phone p : mPhones)
            if (p.getNumber().toString().equals(number.toString()))
                return true;
        return false;
    }

    @Override
    public String toString() {
        if (!StringUtils.isEmpty(mUsername)) {
            return mUsername;
        } else {
            return getUri().getRawUriString();
        }
    }

    public void setId(long id) {
        this.mId = id;
    }

    /*public String getKey() {
        return mKey;
    }*/

    public String getPrimaryNumber() {
        return getUri().getRawRingId();
    }
    public Uri getUri() {
        return mUri;
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
        } else if (usernameLoaded) {
            return getUri().getRawUriString();
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
}
