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

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Conference implements Parcelable {

    private String id;
    private int mConfState;
    private ArrayList<SipCall> participants;
    private boolean recording;
    private ArrayList<TextMessage> messages;

    public int notificationId;

    private final static Random rand = new Random();

    public static String DEFAULT_ID = "-1";

    public boolean isRinging() {
        return participants.get(0).isRinging();
    }

    public void removeParticipant(SipCall toRemove) {
        participants.remove(toRemove);
    }

    /*public boolean useSecureLayer() {
        for(SipCall call : participants){
            if(call.getAccount().useSecureLayer())
                return true;
        }
        return false;
    }*/

    public interface state {
        int ACTIVE_ATTACHED = 0;
        int ACTIVE_DETACHED = 1;
        int ACTIVE_ATTACHED_REC = 2;
        int ACTIVE_DETACHED_REC = 3;
        int HOLD = 4;
        int HOLD_REC = 5;
    }

    public void setCallState(String callID, int newState) {
        if(id.contentEquals(callID))
            mConfState = newState;
        else {
            getCallById(callID).setCallState(newState);
        }
    }

    public void setCallState(String confID, String newState) {
        if (newState.equals("ACTIVE_ATTACHED")) {
            setCallState(confID, state.ACTIVE_ATTACHED);
        } else if (newState.equals("ACTIVE_DETACHED")) {
            setCallState(confID, state.ACTIVE_DETACHED);
        } else if (newState.equals("ACTIVE_ATTACHED_REC")) {
            setCallState(confID, state.ACTIVE_ATTACHED_REC);
        } else if (newState.equals("ACTIVE_DETACHED_REC")) {
            setCallState(confID, state.ACTIVE_DETACHED_REC);
        } else if (newState.equals("HOLD")) {
            setCallState(confID, state.HOLD);
        } else if (newState.equals("HOLD_REC")) {
            setCallState(confID, state.HOLD_REC);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(id);
        out.writeInt(mConfState);
        ArrayList<SipCall> normal_calls = new ArrayList<>();
        ArrayList<SecureSipCall> secure_calls = new ArrayList<>();

        for(SipCall part : participants){
            if(part instanceof SecureSipCall)
                secure_calls.add((SecureSipCall) part);
            else
                normal_calls.add(part);
        }
        out.writeTypedList(secure_calls);
        out.writeTypedList(normal_calls);
        out.writeByte((byte) (recording ? 1 : 0));
        out.writeTypedList(messages);
        out.writeInt(notificationId);
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
        participants = new ArrayList<>();
        id = in.readString();
        mConfState = in.readInt();
        ArrayList<SecureSipCall> tmp = new ArrayList<>();
        in.readTypedList(tmp, SecureSipCall.CREATOR);
        in.readTypedList(participants, SipCall.CREATOR);
        participants.addAll(tmp);
        recording = in.readByte() == 1;
        messages = new ArrayList<>();
        in.readTypedList(messages, TextMessage.CREATOR);
        notificationId = in.readInt();
    }

    public Conference(SipCall call) {
        this(DEFAULT_ID);
        participants.add(call);
        notificationId = rand.nextInt();
    }

    public Conference(String cID) {
        id = cID;
        participants = new ArrayList<>();
        recording = false;
        messages = new ArrayList<>();
    }

    public Conference(Conference c) {
        id = c.id;
        mConfState = c.mConfState;
        participants = new ArrayList<>(c.participants);
        recording = c.recording;
        messages = new ArrayList<>();
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
        return getConferenceStateString();
    }

    public String getConferenceStateString() {

        String text_state;

        switch (mConfState) {
            case state.ACTIVE_ATTACHED:
                text_state = "ACTIVE_ATTACHED";
                break;
            case state.ACTIVE_DETACHED:
                text_state = "ACTIVE_DETACHED";
                break;
            case state.ACTIVE_ATTACHED_REC:
                text_state = "ACTIVE_ATTACHED_REC";
                break;
            case state.ACTIVE_DETACHED_REC:
                text_state = "ACTIVE_DETACHED_REC";
                break;
            case state.HOLD:
                text_state = "HOLD";
                break;
            case state.HOLD_REC:
                text_state = "HOLD_REC";
                break;
            default:
                text_state = "NULL";
        }

        return text_state;
    }

    public ArrayList<SipCall> getParticipants() {
        return participants;
    }

    public boolean contains(String callID) {
        for (SipCall participant : participants) {
            if (participant.getCallId().contentEquals(callID))
                return true;
        }
        return false;
    }

    public SipCall getCallById(String callID) {
        for (SipCall participant : participants) {
            if (participant.getCallId().contentEquals(callID))
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
            if (((Conference) c).id.contentEquals(id) && !id.contentEquals("-1")) {
                return true;
            } else {
                if (((Conference) c).id.contentEquals(id)) {
                    for (SipCall participant : participants) {
                        if (!((Conference) c).contains(participant.getCallId()))
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
        return participants.size() == 1 && participants.get(0).isOnHold() || getConferenceStateString().contentEquals("HOLD");
    }

    public boolean isIncoming() {
        return participants.size() == 1 && participants.get(0).isIncoming();
    }


    public void setRecording(boolean b) {
        recording = b;
    }

    public boolean isRecording() {
            return recording;
    }


    public boolean isOnGoing() {
        return participants.size() == 1 && participants.get(0).isOngoing() || participants.size() > 1;
    }

    public ArrayList<TextMessage> getMessages() {
        return messages;
    }

    public void addSipMessage(TextMessage sipMessage) {
        messages.add(sipMessage);
    }

    public void addParticipant(SipCall part) {
        participants.add(part);
    }

}
