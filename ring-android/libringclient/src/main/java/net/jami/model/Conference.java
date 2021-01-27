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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class Conference {

    public static class ParticipantInfo {
        public Contact contact;
        public int x, y, w, h;
        public boolean videoMuted, audioMuted, isModerator;

        public ParticipantInfo(Contact c, Map<String, String> i) {
            contact = c;
            x = Integer.parseInt(i.get("x"));
            y = Integer.parseInt(i.get("y"));
            w = Integer.parseInt(i.get("w"));
            h = Integer.parseInt(i.get("h"));
            videoMuted = Boolean.parseBoolean(i.get("videoMuted"));
            audioMuted = Boolean.parseBoolean(i.get("audioMuted"));
            isModerator = Boolean.parseBoolean(i.get("isModerator"));
        }
    }
    private final Subject<List<ParticipantInfo>> mParticipantInfo = BehaviorSubject.createDefault(Collections.emptyList());

    private final Set<Contact> mParticipantRecordingSet = new HashSet<>();
    private final Subject<Set<Contact>> mParticipantRecording = BehaviorSubject.createDefault(Collections.emptySet());

    private final String mId;
    private Call.CallStatus mConfState;
    private final ArrayList<Call> mParticipants;
    private boolean mRecording;
    private Call mMaximizedCall;

    public Conference(Call call) {
        this(call.getDaemonIdString());
        mParticipants.add(call);
    }

    public Conference(String cID) {
        mId = cID;
        mParticipants = new ArrayList<>();
        mRecording = false;
    }

    public Conference(Conference c) {
        mId = c.mId;
        mConfState = c.mConfState;
        mParticipants = new ArrayList<>(c.mParticipants);
        mRecording = c.mRecording;
    }

    public boolean isRinging() {
        return !mParticipants.isEmpty() && mParticipants.get(0).isRinging();
    }

    public boolean isConference() {
        return mParticipants.size() > 1;
    }

    public Call getCall() {
        if (!isConference()) {
            return getFirstCall();
        }
        return null;
    }
    public Call getFirstCall() {
        if (!mParticipants.isEmpty()) {
            return mParticipants.get(0);
        }
        return null;
    }

    public String getId() {
        return mId;
    }

    public void setMaximizedCall(Call call) {
        mMaximizedCall = call;
    }

    public Call getMaximizedCall() {
        return mMaximizedCall;
    }

    public String getPluginId() {
        return "local";
    }

    public String getConfId() {
        return mId;
    }

    public Call.CallStatus getState() {
        if (isSimpleCall()) {
            return mParticipants.get(0).getCallStatus();
        }
        return mConfState;
    }

    public Call.CallStatus getConfState() {
        if (mParticipants.size() == 1) {
            return mParticipants.get(0).getCallStatus();
        }
        return mConfState;
    }

    public boolean isSimpleCall() {
        return mParticipants.size() == 1 && mId.equals(mParticipants.get(0).getDaemonIdString());
    }

    public void setState(String state) {
        mConfState = Call.CallStatus.fromConferenceString(state);
    }

    public List<Call> getParticipants() {
        return mParticipants;
    }

    public void addParticipant(Call part) {
        mParticipants.add(part);
    }

    public boolean removeParticipant(Call toRemove) {
        return mParticipants.remove(toRemove);
    }

    public boolean contains(String callID) {
        for (Call participant : mParticipants) {
            if (participant.getDaemonIdString().contentEquals(callID))
                return true;
        }
        return false;
    }

    public Call getCallById(String callID) {
        for (Call participant : mParticipants) {
            if (participant.getDaemonIdString().contentEquals(callID))
                return participant;
        }
        return null;
    }

    public Call findCallByContact(Uri uri) {
        for (Call call : mParticipants) {
            if (call.getContact().getUri().toString().equals(uri.toString()))
                return call;
        }
        return null;
    }

    public boolean isIncoming() {
        return mParticipants.size() == 1 && mParticipants.get(0).isIncoming();
    }

    public boolean isOnGoing() {
        return mParticipants.size() == 1 && mParticipants.get(0).isOnGoing() || mParticipants.size() > 1;
    }

    public boolean hasVideo() {
        for (Call call : mParticipants)
            if (!call.isAudioOnly())
                return true;
        return false;
    }

    public long getTimestampStart() {
        long t = Long.MAX_VALUE;
        for (Call call : mParticipants)
            t = Math.min(call.getTimestamp(), t);
        return t;
    }

    public void removeParticipants() {
        mParticipants.clear();
    }

    public void setInfo(List<ParticipantInfo> info) {
        mParticipantInfo.onNext(info);
    }

    public Observable<List<ParticipantInfo>> getParticipantInfo() {
        return mParticipantInfo;
    }
    public Observable<Set<Contact>> getParticipantRecording() {
        return mParticipantRecording;
    }

    public void setParticipantRecording(Contact contact, boolean state) {
        if (state) {
            mParticipantRecordingSet.add(contact);
        } else {
            mParticipantRecordingSet.remove(contact);
        }
        mParticipantRecording.onNext(mParticipantRecordingSet);
    }

}
