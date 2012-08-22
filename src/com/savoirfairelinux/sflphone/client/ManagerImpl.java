package com.savoirfairelinux.sflphone.client;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class ManagerImpl {
	
	private static final String TAG = "ManagerImpl";
	private static String sipLogLevel;

	public static boolean outgoingCallJ(String account_id) {
		Log.i(TAG, "account_id:" + account_id);
		return true;
	}
	
	/* native implementation */
	static {
		System.setProperty("SIPLOGLEVEL", "4");
		sipLogLevel = System.getProperty("SIPLOGLEVEL");
		Log.i(TAG, "SIPLOGLEVEL: " + sipLogLevel);

		// FIXME
		System.loadLibrary("gnustl_shared");
		System.loadLibrary("expat");
		System.loadLibrary("yaml");
		System.loadLibrary("ccgnu2");
		System.loadLibrary("crypto");
		System.loadLibrary("ssl");
		System.loadLibrary("ccrtp1");
		System.loadLibrary("dbus");
		System.loadLibrary("dbus-c++-1");
		System.loadLibrary("samplerate");
		System.loadLibrary("codec_ulaw");
		System.loadLibrary("codec_alaw");
		System.loadLibrary("speexresampler");
		System.loadLibrary("sflphone");
	}

	public String getSipLogLevel() {
		return sipLogLevel;
	}

	//public static native JNI_OnLoad(JavaVM* vm, void* reserved);

	public static native void setSipLogLevel(String level);

    public static native String getJniString();
	
	public static native boolean outgoingCallN(String account_id);
	
	public static native void initN(String config_file);
}
