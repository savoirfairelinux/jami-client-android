package com.savoirfairelinux.sflphone.interfaces;

import android.content.Intent;

public interface CallInterface {

    public void incomingCall(Intent call);

    public void callStateChanged(Intent callState);

    public void incomingText(Intent msg);
    
}
