package com.savoirfairelinux.sflphone.client;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.model.SipCall;

public class OngoingCallFragment extends Fragment implements CallActivity.CallFragment, OnClickListener
{
	private CallActivity listener;
	private Button end_btn, suspend_btn;
	private TextView callstatus_txt;
	private TextView calllength_txt;
	private TextView contact_name_txt;

	private SipCall mCall = null;

	public void setCall(SipCall call)
	{
		mCall = call;
		if(isAdded())
			updateUI();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		ViewGroup v = (ViewGroup) inflater.inflate(R.layout.frag_call_ongoing, container, false);
		
		contact_name_txt = (TextView) v.findViewById(R.id.contact_name_txt);
		end_btn = (Button) v.findViewById(R.id.end_btn);
		suspend_btn = (Button) v.findViewById(R.id.suspend_btn);
		callstatus_txt = (TextView) v.findViewById(R.id.callstatus_txt);
		calllength_txt = (TextView) v.findViewById(R.id.calllength_txt);

		end_btn.setOnClickListener(this);
		suspend_btn.setOnClickListener(this);
		
		updateUI();
		return v;
	}

	private void updateUI()
	{
		if (mCall == null)
			return;
		contact_name_txt.setText(mCall.getDisplayName());
		callstatus_txt.setText(mCall.getCallStateString());
		
		int state = mCall.getCallStateInt();
		if(state == SipCall.CALL_STATE_HOLD) {
			suspend_btn.setText("Resume");
		} else {
			suspend_btn.setText("Suspend");
		}
		/*
		switch(mCall.getCallStateInt()) {
		case SipCall.CALL_STATE_HOLD:
			suspend_btn.setText("Resume");
			break;
		case SipCall.CALL_STATE_HOLD:
			suspend_btn.setText("Resume");
			break;

		}*/
	}

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		listener = (CallActivity) activity;
	}

	@Override
	public void onClick(View v)
	{
		if (v == end_btn) {
			listener.onCallEnded();
		} else if (v == suspend_btn) {
			if(mCall.getCallStateInt() == SipCall.CALL_STATE_HOLD)
				listener.onCallResumed();
			else
				listener.onCallSuspended();
		}
	}
}