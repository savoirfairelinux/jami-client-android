package com.savoirfairelinux.sflphone.model;

import java.util.ArrayList;
import java.util.HashMap;

import android.os.Parcel;
import android.os.Parcelable;

public class Conference implements Parcelable {

    private String id;
    private String state = "";
    private ArrayList<SipCall> participants;
    private boolean recording;

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

    }

    public Conference(String cID) {
        id = cID;
        participants = new ArrayList<SipCall>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getState() {
        if(participants.size() == 1){
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
        for (int i = 0 ; i < participants.size() ; ++i){
            if(participants.get(i).getCallId().contentEquals(callID))
                return true;
        }
        return false;
    }

    public SipCall getCall(String callID) {
        for (int i = 0 ; i < participants.size() ; ++i){
            if(participants.get(i).getCallId().contentEquals(callID))
                return participants.get(i);
        }
        return null;
    }

    public boolean hasMultipleParticipants() {
        return participants.size() > 1;
    }

    public boolean isOnHold() {
        if(participants.size() == 1 && participants.get(0).isOnHold())
            return true;
        return state.contentEquals("HOLD");
    }

    public void setRecording(boolean b) {
        recording = b;
    }

    public boolean isRecording() {
        return recording;
    }

    public boolean isOnGoing() {
        if(participants.size() == 1 && participants.get(0).isOngoing())
            return true;
        
        if (participants.size() > 1)
            return true;
        
        return false;
    }

}
