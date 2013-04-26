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

public class IncomingCallFragment extends Fragment implements CallActivity.CallFragment, OnClickListener
{

	private CallActivity listener;
	private Button accept_btn, decline_btn;
	private TextView contact_name_txt;
	
	private SipCall mCall = null;

	public void setCall(SipCall call)
	{
		mCall = call; // = new WeakReference<SipCall>(mCall);
		if(isAdded())
			updateUI();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		ViewGroup v = (ViewGroup) inflater.inflate(R.layout.frag_call_incoming, container, false);
		
		contact_name_txt = (TextView) v.findViewById(R.id.contact_name_txt);
		decline_btn = (Button) v.findViewById(R.id.decline_btn);
		accept_btn = (Button) v.findViewById(R.id.accept_btn);
		
		decline_btn.setOnClickListener(this);
		accept_btn.setOnClickListener(this);
		
		updateUI();
		return v;
	}
	
	private void updateUI()
	{
		if (mCall == null)
			return;
		contact_name_txt.setText(mCall.getDisplayName());
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
		if(v == accept_btn) {
			listener.onCallAccepted();
		} else if (v == decline_btn) {
			listener.onCallRejected();
		}
	}
}
