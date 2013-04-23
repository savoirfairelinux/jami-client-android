package com.savoirfairelinux.sflphone.service;

interface ISipClient {
    void incomingCall(in Intent call);
    void callStateChanged(in Intent callState);
}