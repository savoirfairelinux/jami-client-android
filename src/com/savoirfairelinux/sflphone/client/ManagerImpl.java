package com.savoirfairelinux.sflphone.client;

import android.os.Handler;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;

import com.savoirfairelinux.sflphone.R;

public class ManagerImpl {
	
	private static final String TAG = "ManagerImpl";
	private static int sipLogLevel = 6;
	static Handler h;
	private static ButtonSectionFragment buttonSecFragment;
	static String appPath;
	static Animation animation;
	static SFLPhoneHome uiThread;
	static ImageButton buttonCall;
	
	public ManagerImpl () {}
	
	public ManagerImpl(Handler h) {		
		// Change alpha from fully visible to invisible
	    animation = new AlphaAnimation(1, 0);
	    // duration - half a second
	    animation.setDuration(500);
	    // do not alter animation rate
	    animation.setInterpolator(new LinearInterpolator());
	    // Repeat animation infinitely
	    animation.setRepeatCount(Animation.INFINITE);
	    // Reverse
	    animation.setRepeatMode(Animation.REVERSE);
	    
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
	
	public static void incomingCall(String accountID, String callID, String from) {
		Log.i(TAG, "incomingCall(" + accountID + ", " + callID + ", " + from + ")");
		buttonCall = (ImageButton) buttonSecFragment.getCallButton();
		
		// FIXME that's ugly...
		uiThread.runOnUiThread(new Runnable() {
		     public void run() {
		 		try {
					buttonCall.startAnimation(animation);
					buttonCall.setImageResource(R.drawable.ic_incomingcall);
				} catch (Exception e) {
					Log.w(TAG, "exception in runOnUiThread ", e);
				}
		    }
		});

		uiThread.setIncomingCallID(callID);
	}
	
	// FIXME
	public static void setButtonFragment(ButtonSectionFragment f) {
		buttonSecFragment = f;
	}
	
	public static void setActivity(SFLPhoneHome a) {
		uiThread = a;
	}

	public static String getAppPath() {
		return appPath;
	}
	
	public static void setAppPath(String path) {
		appPath = path;
	}

	public static int getSipLogLevel() {
		return sipLogLevel;
	}

	public static native void callVoid();
	public static native Data getNewData(int i, String s);
	public static native String getDataString(Data d);
	
	public static native void setSipLogLevel(String level);
    public static native String getJniString();
	public static native void initN(String config_file);
	public static native void placeCall(String accountID, String callID, String to);
	public static native void hangUp(String callID);
	public static native void answerCall(String callID);
	public static native void refuseCall(String callID);
}
