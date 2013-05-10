/*
 *  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package com.savoirfairelinux.sflphone.client;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.fragments.IncomingCallFragment;
import com.savoirfairelinux.sflphone.fragments.OngoingCallFragment;
import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.service.CallManagerCallBack;
import com.savoirfairelinux.sflphone.service.ISipService;
import com.savoirfairelinux.sflphone.service.SipService;

public class CallActivity extends Activity //implements IncomingCallFragment.ICallActionListener, OngoingCallFragment.ICallActionListener //OnClickListener
{
	static final String TAG = "CallActivity";
	private ISipService service;
	private SipCall mCall;

	public interface CallFragment
	{
		void setCall(SipCall c);
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String signalName = intent.getStringExtra(CallManagerCallBack.SIGNAL_NAME);
			Log.d(TAG, "Signal received: " + signalName);

			if (signalName.equals(CallManagerCallBack.NEW_CALL_CREATED)) {
			} else if (signalName.equals(CallManagerCallBack.CALL_STATE_CHANGED)) {
				processCallStateChangedSignal(intent);
			} else if (signalName.equals(CallManagerCallBack.INCOMING_CALL)) {
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_call_layout);

		Bundle b = getIntent().getExtras();
		// Parcelable value = b.getParcelable("CallInfo");
		SipCall.CallInfo info = b.getParcelable("CallInfo");
		Log.i(TAG, "Starting activity for call " + info.mCallID);
		mCall = new SipCall(info);

		Intent intent = new Intent(this, SipService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		setCallStateDisplay(mCall.getCallStateString());

		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(CallManagerCallBack.NEW_CALL_CREATED));
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(CallManagerCallBack.CALL_STATE_CHANGED));
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(CallManagerCallBack.INCOMING_CALL));
	}

	@Override
	protected void onDestroy()
	{
		Log.i(TAG, "Destroying Call Activity for call " + mCall.getCallId());
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
		unbindService(mConnection);
		super.onDestroy();
	}

	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder binder)
		{
			service = ISipService.Stub.asInterface(binder);
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0)
		{
		}
	};

	private void processCallStateChangedSignal(Intent intent)
	{
		Bundle bundle = intent.getBundleExtra("com.savoirfairelinux.sflphone.service.newstate");
		String callID = bundle.getString("CallID");
		String newState = bundle.getString("State");

		if (newState.equals("INCOMING")) {
			mCall.setCallState(SipCall.CALL_STATE_INCOMING);
			setCallStateDisplay(newState);
		} else if (newState.equals("RINGING")) {
			mCall.setCallState(SipCall.CALL_STATE_RINGING);
			setCallStateDisplay(newState);
		} else if (newState.equals("CURRENT")) {
			mCall.setCallState(SipCall.CALL_STATE_CURRENT);
			setCallStateDisplay(newState);
		} else if (newState.equals("HUNGUP")) {
			mCall.setCallState(SipCall.CALL_STATE_HUNGUP);
			setCallStateDisplay(newState);
			finish();
		} else if (newState.equals("BUSY")) {
			mCall.setCallState(SipCall.CALL_STATE_BUSY);
			setCallStateDisplay(newState);
		} else if (newState.equals("FAILURE")) {
			mCall.setCallState(SipCall.CALL_STATE_FAILURE);
			setCallStateDisplay(newState);
		} else if (newState.equals("HOLD")) {
			mCall.setCallState(SipCall.CALL_STATE_HOLD);
			setCallStateDisplay(newState);
		} else if (newState.equals("UNHOLD")) {
			mCall.setCallState(SipCall.CALL_STATE_CURRENT);
			setCallStateDisplay("CURRENT");
		} else {
			mCall.setCallState(SipCall.CALL_STATE_NONE);
			setCallStateDisplay(newState);
		}

		Log.w(TAG, "processCallStateChangedSignal " + newState);

	}

	private void setCallStateDisplay(String newState)
	{
		if (newState == null || newState.equals("NULL")) {
			newState = "INCOMING";
		}

		Log.w(TAG, "setCallStateDisplay " + newState);

		mCall.printCallInfo();

		FragmentManager fm = getFragmentManager();
		Fragment newf, f = fm.findFragmentByTag("call_fragment");
		boolean replace = true;
		if (newState.equals("INCOMING") && !(f instanceof IncomingCallFragment)) {
			newf = new IncomingCallFragment();
		} else if (!newState.equals("INCOMING") && !(f instanceof OngoingCallFragment)) {
			newf = new OngoingCallFragment();
		} else {
			replace = false;
			newf = f;
		}

		((CallFragment) newf).setCall(mCall);

		if (replace) {
			FragmentTransaction ft = fm.beginTransaction();
			if(f != null) // do not animate if there is no previous fragment
				ft.setCustomAnimations(R.animator.slide_in, R.animator.slide_out);
				//ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
			ft.replace(R.id.fragment_layout, newf, "call_fragment").commit();
		}
	}

	public void onCallAccepted()
	{
		mCall.notifyServiceAnswer(service);
	}

	public void onCallRejected()
	{
		if (mCall.notifyServiceHangup(service))
			finish();
	}

	public void onCallEnded()
	{
		if (mCall.notifyServiceHangup(service))
			finish();
	}

	public void onCallSuspended()
	{
		mCall.notifyServiceHold(service);
	}

	public void onCallResumed()
	{
		mCall.notifyServiceUnhold(service);
	}

    public void onCalltransfered(String to) {
        mCall.notifyServiceTransfer(service, to);
        
    }

    public void onRecordCall() {
        mCall.notifyServiceRecord(service);
        
    }

    public void onSendMessage(String msg) {
        mCall.notifyServiceSendMsg(service,msg);
        
    }

}
