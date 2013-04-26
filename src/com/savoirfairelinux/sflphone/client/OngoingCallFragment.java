package com.savoirfairelinux.sflphone.client;

import java.lang.ref.WeakReference;

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

public class OngoingCallFragment extends Fragment implements OnClickListener
{
	public interface ICallActionListener
	{
		public void onCallEnded();
		public void onCallSuspended();
		public void onCallResumed();
	}

	private ICallActionListener listener;
	private Button end_btn, suspend_btn;
	
	private WeakReference<SipCall> call = null;

	public void setCall(SipCall mCall)
	{
		call = new WeakReference<SipCall>(mCall);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		ViewGroup v = (ViewGroup) inflater.inflate(R.layout.frag_call_incoming, container, false);
		end_btn = (Button) v.findViewById(R.id.end_btn);
		suspend_btn = (Button) v.findViewById(R.id.suspend_btn);
		end_btn.setOnClickListener(this);
		suspend_btn.setOnClickListener(this);
		
		TextView contact_name_txt = (TextView) v.findViewById(R.id.contact_name_txt);
		if(call != null && call.get() != null) {
			contact_name_txt.setText(call.get().getDisplayName());
		}
		
		return v;
	}

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		listener = (ICallActionListener) activity;
	}

	@Override
	public void onClick(View v)
	{
		if(v == end_btn) {
			listener.onCallEnded();
		} else if (v == suspend_btn) {
			listener.onCallSuspended();
		}
	}
}