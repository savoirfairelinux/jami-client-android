package org.sflphone.client;

import org.sflphone.service.ISipService;
import org.sflphone.service.SipService;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Intent;
import android.util.Log;

public class SFLphoneApplication extends Application
{
	static final String TAG = "SFLphoneApplication";
	private boolean serviceRunning = false;
	private ISipService sipService;

	private void startSipService()
	{
		Thread thread = new Thread("StartSFLphoneService") {
			public void run()
			{
				Log.i(TAG, "SipService launching thread");
				Intent sipServiceIntent = new Intent(SFLphoneApplication.this, SipService.class);
				//sipServiceIntent.putExtra(ServiceConstants.EXTRA_OUTGOING_ACTIVITY, new ComponentName(SFLPhoneHome.this, SFLPhoneHome.class));
				startService(sipServiceIntent);
				serviceRunning = true;
			};
		};
		try {
			thread.start();
		} catch (IllegalThreadStateException e) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Cannot start SFLPhone SipService!");
			AlertDialog alert = builder.create();
			alert.show();
			//TODO exit application
		}
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		Log.i(TAG, "onCreate");

		if (!serviceRunning) {
			Log.i(TAG, "starting SipService");
			startSipService();
		}
	}

	@Override
	public void onTerminate()
	{
		super.onTerminate();
		Log.i(TAG, "onTerminate");

		if (serviceRunning) {
			Log.i(TAG, "onDestroy: stopping SipService...");
			stopService(new Intent(this, SipService.class));
			serviceRunning = false;
		}
	}

	public boolean isServiceRunning()
	{
		return serviceRunning;
	}

	public void setServiceRunning(boolean r)
	{
		this.serviceRunning = r;
	}

	public ISipService getSipService()
	{
		return sipService;
	}

	public void setSipService(ISipService service)
	{
		sipService = service;
	}
}
