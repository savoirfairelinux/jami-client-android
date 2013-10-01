package org.sflphone.account;

import java.util.ArrayList;

import org.sflphone.service.IntVect;

import android.util.Log;

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
