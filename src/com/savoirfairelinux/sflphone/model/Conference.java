package com.savoirfairelinux.sflphone.model;

import java.util.ArrayList;

public class Conference {
    
    private long id;
    private String state;
    private ArrayList<SipCall> participants;
    
    
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getState() {
        return state;
    }
    public void setState(String state) {
        this.state = state;
    }
    public ArrayList<SipCall> getParticipants() {
        return participants;
    }
    public void setParticipants(ArrayList<SipCall> participants) {
        this.participants = participants;
    }

}
