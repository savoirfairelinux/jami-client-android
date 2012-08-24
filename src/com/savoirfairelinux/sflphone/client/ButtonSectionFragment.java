package com.savoirfairelinux.sflphone.client;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;

public class ButtonSectionFragment extends Fragment
{
		//private SFLPhoneHome myButtonSectionFragment;
		static final String TAG = "ButtonSectionFragment";
		public TextView callVoidText, NewDataText, DataStringText;
		Button buttonCallVoid, buttonGetNewData, buttonGetDataString;
		Handler callbackHandler;
		ManagerImpl managerImpl;

		public ButtonSectionFragment()
		{
			setRetainInstance(true);
		}
		
		public TextView getcallVoidText() {
			return callVoidText;
		}
		
		public TextView getNewDataText() {
			return NewDataText;
		}
		
		public TextView getDataStringText() {
			return DataStringText;
		}
		
//		public ButtonSectionFragment(SFLPhoneHome sflPhoneHome)
//		{
//			myButtonSectionFragment = sflPhoneHome;
//			setRetainInstance(true);
//		}

		public static final String ARG_SECTION_NUMBER = "section_number";

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState)
		{
			View view;
			
			Log.i(TAG, "onCreateView" );
			view = inflater.inflate(R.layout.test_layout, parent, false);

			callVoidText = (TextView) view.findViewById(R.id.callVoid_text);
			if (callVoidText == null)
				Log.e(TAG, "callVoidText is " + callVoidText);
			callbackHandler = new Handler() {
				public void handlerMessage(Message msg) {
					Bundle b = msg.getData();
					callVoidText.setText(b.getString("callback_string"));
					Log.i(TAG, "handlerMessage: " + b.getString("callback_string"));
				}
			};
			managerImpl = new ManagerImpl(callbackHandler);
			
		    NewDataText = (TextView) view.findViewById(R.id.NewData_text);  
		    buttonGetNewData = (Button) view.findViewById(R.id.buttonGetNewData);

		    DataStringText = (TextView) view.findViewById(R.id.DataString_text);
		    buttonGetDataString = (Button) view.findViewById(R.id.buttonGetDataString);
			
		    try {
				inflater.inflate(R.layout.test_layout, parent, false);
			} catch (InflateException e) {
				Log.e(TAG, "Error inflating test_layout ", e);
				return null;
			}
			return view;
		}
}