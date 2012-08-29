package com.savoirfairelinux.sflphone.client;

import android.os.Handler;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

public class ManagerImpl {
	
	private static final String TAG = "ManagerImpl";
	private static String sipLogLevel;
	static Handler h; 
	
	public ManagerImpl () {}
	
	public ManagerImpl(Handler h) {
		this.h = h;
	}
	
	public static void callBack(String s) {
		Bundle b = new Bundle();
		Log.i(TAG, "callBack: " + s);
		b.putString("callback_string", s);
		Message m = Message.obtain();
		m.setData(b);
		m.setTarget(h);
		m.sendToTarget();
	}

	public String getSipLogLevel() {
		return sipLogLevel;
	}

	public static native void callVoid();
	public static native Data getNewData(int i, String s);
	public static native String getDataString(Data d);
	
	public static native void setSipLogLevel(String level);
    public static native String getJniString();
	public static native void initN(String config_file);
}
