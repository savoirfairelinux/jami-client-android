package com.savoirfairelinux.sflphone.account;

import java.util.ArrayList;

import android.util.Log;

import com.savoirfairelinux.sflphone.service.IntVect;

public class AudioHandler {

    private static final String TAG = AudioHandler.class.getSimpleName();

    public static ArrayList<Integer> convertSwigToNative(IntVect swigmap) {

        ArrayList<Integer> nativemap = new ArrayList<Integer>();

        Log.w(TAG, "size codecs list " + swigmap.size());

        for (int i = 0; i < swigmap.size(); ++i) {

            Integer t = swigmap.get(i);
            nativemap.add(t);
        }

        return nativemap;
    }

}
