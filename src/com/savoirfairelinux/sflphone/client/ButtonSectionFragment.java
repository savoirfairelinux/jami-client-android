package com.savoirfairelinux.sflphone.client;

import android.app.Fragment;
import android.os.Bundle;
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
		static final String TAG = "ButtonSectionFragment";
		private TextView callVoidText, NewDataText, DataStringText;
		Button buttonCallVoid, buttonGetNewData, buttonGetDataString;

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

		public static final String ARG_SECTION_NUMBER = "section_number";

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState)
		{
			View view;
			
			Log.i(TAG, "onCreateView" );
			view = inflater.inflate(R.layout.test_layout, parent, false);

			callVoidText = (TextView) view.findViewById(R.id.callVoid_text);

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