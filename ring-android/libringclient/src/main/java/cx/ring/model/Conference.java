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
import java.util.Random;

public class Conference {

    private String mId;
    private SipCall.CallStatus mConfState;
    private ArrayList<SipCall> mParticipants;
    private boolean mRecording;
    private int uuid;

    private static String DEFAULT_ID = "-1";

    public Conference(SipCall call) {
        this(DEFAULT_ID);
        mParticipants.add(call);
        uuid = new Random().nextInt();
    }

    public Conference(String cID) {
        mId = cID;
        mParticipants = new ArrayList<>();
        mRecording = false;
        uuid = new Random().nextInt();
    }

    public Conference(Conference c) {
        mId = c.mId;
        mConfState = c.mConfState;
        mParticipants = new ArrayList<>(c.mParticipants);
        mRecording = c.mRecording;
        uuid = c.getUuid();
    }

    public boolean isRinging() {
        return !mParticipants.isEmpty() && mParticipants.get(0).isRinging();
    }

    public int getUuid() {
        return uuid;
    }

    public String getId() {
        if (mParticipants.size() == 1) {
            return mParticipants.get(0).getDaemonIdString();
        } else {
            return mId;
        }
    }

    public SipCall.CallStatus getState() {
        if (mParticipants.size() == 1) {
            return mParticipants.get(0).getCallStatus();
        }
        return mConfState;
    }
    public ArrayList<SipCall> getParticipants() {
        return mParticipants;
    }

    public void addParticipant(SipCall part) {
        mParticipants.add(part);
    }

    public boolean removeParticipant(SipCall toRemove) {
        return mParticipants.remove(toRemove);
    }

    boolean contains(String callID) {
        for (SipCall participant : mParticipants) {
            if (participant.getDaemonIdString().contentEquals(callID))
                return true;
        }
        return false;
    }

    public SipCall getCallById(String callID) {
        for (SipCall participant : mParticipants) {
            if (participant.getDaemonIdString().contentEquals(callID))
                return participant;
        }
        return null;
    }

    /**
     * Compare conferences based on confID/participants
     */
    @Override
    public boolean equals(Object c) {
        if (c instanceof Conference) {
            if (((Conference) c).mId.contentEquals(mId) && !mId.contentEquals("-1")) {
                return true;
            } else {
                if (((Conference) c).mId.contentEquals(mId)) {
                    for (SipCall participant : mParticipants) {
                        if (!((Conference) c).contains(participant.getDaemonIdString())) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isIncoming() {
        return mParticipants.size() == 1 && mParticipants.get(0).isIncoming();
    }

    public boolean isOnGoing() {
        return mParticipants.size() == 1 && mParticipants.get(0).isOnGoing() || mParticipants.size() > 1;
    }

}
