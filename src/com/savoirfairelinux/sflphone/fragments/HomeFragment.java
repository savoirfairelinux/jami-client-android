/*
 *  Copyright (C) 2004-2013 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
package com.savoirfairelinux.sflphone.fragments;

import java.util.HashMap;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.model.Conference;
import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.service.ISipService;

public class HomeFragment extends Fragment {
    private static final String TAG = HomeFragment.class.getSimpleName();

    private Callbacks mCallbacks = sDummyCallbacks;
    Button access_calls;
    TextView nb_calls, nb_confs;

    /**
     * A dummy implementation of the {@link Callbacks} interface that does nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {

        @Override
        public ISipService getService() {
            Log.i(TAG, "I'm a dummy");
            return null;
        }

        @Override
        public void resumeCallActivity() {            
        }
    };

    /**
     * The Activity calling this fragment has to implement this interface
     * 
     */
    public interface Callbacks {

        public ISipService getService();

        public void resumeCallActivity();

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;

    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCallbacks.getService() != null) {
            try {
                HashMap<String, SipCall> calls = (HashMap<String, SipCall>) mCallbacks.getService().getCallList();
                HashMap<String, Conference> confs = (HashMap<String, Conference>) mCallbacks.getService().getConferenceList();
                Log.i(TAG, "Call size " + calls.size());
                nb_calls.setText(""+calls.size());
                nb_confs.setText(""+confs.size());
                
                if(!calls.isEmpty() || !confs.isEmpty()){
                    access_calls.setVisibility(View.VISIBLE);
                } else {
                    access_calls.setVisibility(View.GONE);
                }
//                access_calls.setText(calls.size() + " on going calls and "+confs.size()+" conferences");

            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // mAdapter = new CallElementAdapter(getActivity(), new ArrayList<SipCall>());

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Give some text to display if there is no data. In a real
        // application this would come from a resource.
        // setEmptyText("No phone numbers");

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.call_element_menu, menu);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View inflatedView = inflater.inflate(R.layout.frag_home, container, false);

        access_calls = (Button) inflatedView.findViewById(R.id.access_callactivity);

        nb_calls = (TextView) inflatedView.findViewById(R.id.calls_counter);
        nb_confs = (TextView) inflatedView.findViewById(R.id.confs_counter);
        access_calls.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                try {
                    HashMap<String, SipCall> calls = (HashMap<String, SipCall>) mCallbacks.getService().getCallList();
                    HashMap<String, Conference> confs = (HashMap<String, Conference>) mCallbacks.getService().getConferenceList();
                    if (calls.isEmpty() && confs.isEmpty()) {
                        Toast.makeText(getActivity(), "No calls", Toast.LENGTH_SHORT).show();
                    } else {
                        
                       mCallbacks.resumeCallActivity();
                        
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, e.toString());
                }

            }
        });

        return inflatedView;
    }

}
