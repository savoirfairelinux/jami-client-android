package org.sflphone.interfaces;

import android.content.Intent;

public interface CallInterface {

    public void incomingCall(Intent call);

    public void callStateChanged(Intent callState);

    public void incomingText(Intent msg);

    public void confCreated(Intent intent);

    public void confRemoved(Intent intent);

    public void confChanged(Intent intent);
    
    public void recordingChanged(Intent intent);
    
}
