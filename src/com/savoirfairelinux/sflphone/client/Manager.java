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

public class Manager {
	
	private static final String TAG = "Manager";
	private static int sipLogLevel = 6;
	static Handler h;
	// private static ButtonSectionFragment buttonSecFragment;
	static String appPath;
	static Animation animation;
	static SFLPhoneHome uiThread;
	static ImageButton buttonCall;
	public static ManagerImpl managerImpl;
	
	static {
		// FIXME: this is the 2nd time we call ManagerImpl's constructor.
		//        First time was at JNI_OnLoad... 
	    managerImpl = new ManagerImpl(sflphoneserviceJNI.instance(), true);
		Log.i(TAG, "ManagerImpl::instance() = " + managerImpl);
	}

	public Manager() {}
	
	public Manager(Handler h) {
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
	public static void setCallButton(ImageButton b) {
                buttonCall = b;
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
    public static native String getJniString();
}
