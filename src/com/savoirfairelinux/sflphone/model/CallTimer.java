package com.savoirfairelinux.sflphone.model;

import java.util.Observable;

import android.util.Log;

public class CallTimer extends Observable implements Runnable {

    boolean stop = false;
    private static final String TAG = CallTimer.class.getSimpleName();

    @Override
    public void run() {

        while (!stop) {
            try {
                synchronized (this) {
                    this.wait(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.i(TAG, "update!");
            notifyObservers();
        }

    }

    @Override
    public boolean hasChanged() {
        return true;
    }

}
