package com.savoirfairelinux.sflphone.client;

import android.os.Handler;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.animation.Animation;
import android.widget.ImageButton;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.service.CallManagerCallBack;
import com.savoirfairelinux.sflphone.service.CallManagerJNI;
import com.savoirfairelinux.sflphone.service.ManagerImpl;
import com.savoirfairelinux.sflphone.service.SFLPhoneservice;

public class Manager {
	
	private static final String TAG = "Manager";
	private static int sipLogLevel = 6;
	static Handler handler;
	// private static ButtonSectionFragment buttonSecFragment;
	static String appPath;
	static Animation animation;
	static SFLPhoneHome uiThread;
	static ImageButton buttonCall;
	public static ManagerImpl managerImpl;
	public CallManagerJNI callmanagerJNI;
	public CallManagerCallBack callManagerCallBack;
	
	static {
	    managerImpl = SFLPhoneservice.instance();
		Log.i(TAG, "ManagerImpl::instance() = " + managerImpl);
	}

	public Manager() {}
	
	public Manager(Handler h) {
	    
		Manager.handler = h;
		callmanagerJNI = new CallManagerJNI();
		callManagerCallBack = new CallManagerCallBack();
		SFLPhoneservice.setCallbackObject(callManagerCallBack);
        Log.i(TAG, "callManagerCallBack = " + callManagerCallBack);
	}
	
	public static void callBack(String s) {
		Bundle b = new Bundle();
		Log.i(TAG, "callBack: " + s);
		b.putString("callback_string", s);
		Message m = Message.obtain();
		m.setData(b);
		m.setTarget(handler);
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

		uiThread.setIncomingCallID(accountID, callID, from);
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
