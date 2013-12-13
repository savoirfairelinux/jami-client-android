/*
 *  Copyright (C) 2004-2013 Savoir-Faire Linux Inc.
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

package org.sflphone.model;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class Conference implements Parcelable {

    private String id;
    private String state = "";
    private ArrayList<SipCall> participants;
    private boolean recording;
    private ArrayList<SipMessage> messages;

    public interface state {
        int ACTIVE_ATTACHED = 0;
        int ACTIVE_DETACHED = 1;
        int ACTIVE_ATTACHED_REC = 2;
        int ACTIVE_DETACHED_REC = 3;
        int HOLD = 4;
        int HOLD_REC = 5;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(id);
        out.writeString(state);
        out.writeTypedList(participants);
        out.writeByte((byte) (recording ? 1 : 0));
        out.writeTypedList(messages);
    }

    public static final Parcelable.Creator<Conference> CREATOR = new Parcelable.Creator<Conference>() {
        public Conference createFromParcel(Parcel in) {
            return new Conference(in);
        }

        public Conference[] newArray(int size) {
            return new Conference[size];
        }
    };

    private Conference(Parcel in) {
        participants = new ArrayList<SipCall>();
        id = in.readString();
        state = in.readString();
        in.readTypedList(participants, SipCall.CREATOR);
        recording = in.readByte() == 1 ? true : false;
        messages = new ArrayList<SipMessage>();
        in.readTypedList(messages, SipMessage.CREATOR);
    }

    public Conference(String cID) {
        id = cID;
        participants = new ArrayList<SipCall>();
        recording = false;
        messages = new ArrayList<SipMessage>();
    }

    public Conference(Conference c) {
        id = c.id;
        state = c.state;
        participants = new ArrayList<SipCall>(c.participants);
        recording = c.recording;
        messages = new ArrayList<SipMessage>();
    }

    public String getId() {
        if(hasMultipleParticipants())
            return id;
        else
            return participants.get(0).getCallId();
    }

    public String getState() {
        if (participants.size() == 1) {
            return participants.get(0).getCallStateString();
        }
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public ArrayList<SipCall> getParticipants() {
        return participants;
    }

    public boolean contains(String callID) {
        for (int i = 0; i < participants.size(); ++i) {
            if (participants.get(i).getCallId().contentEquals(callID))
                return true;
        }
        return false;
    }

    public SipCall getCall(String callID) {
        for (int i = 0; i < participants.size(); ++i) {
            if (participants.get(i).getCallId().contentEquals(callID))
                return participants.get(i);
        }
        return null;
    }

    /**
     * Compare conferences based on confID/participants
     */
    @Override
    public boolean equals(Object c) {
        if (c instanceof Conference) {
            if (((Conference) c).id.contentEquals(id) && !id.contentEquals("-1")) {
                return true;
            } else {
                if (((Conference) c).id.contentEquals(id)) {
                    for (int i = 0; i < participants.size(); ++i) {
                        if (!((Conference) c).contains(participants.get(i).getCallId()))
                            return false;
                    }
                    return true;
                }
            }
        }
        return false;

    }

    public boolean hasMultipleParticipants() {
        return participants.size() > 1;
    }

    public boolean isOnHold() {
        if (participants.size() == 1 && participants.get(0).isOnHold())
            return true;
        return state.contentEquals("HOLD");
    }

    public void setRecording(boolean b) {
        recording = b;
    }

    public boolean isRecording() {
        if(hasMultipleParticipants())
            return recording;
        else
            return participants.get(0).isRecording();
    }

    public boolean isOnGoing() {
        if (participants.size() == 1 && participants.get(0).isOngoing())
            return true;

        if (participants.size() > 1)
            return true;

        return false;
    }

    public ArrayList<SipMessage> getMessages() {
        if (hasMultipleParticipants())
            return messages;
        else
            return participants.get(0).getMessages();

    }

    public void addSipMessage(SipMessage sipMessage) {
        messages.add(sipMessage);
    }

}
