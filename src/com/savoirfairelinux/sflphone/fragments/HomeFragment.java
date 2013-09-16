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

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.model.Conference;
import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.service.ISipService;

public class HomeFragment extends Fragment {
    private static final String TAG = HomeFragment.class.getSimpleName();

    private Callbacks mCallbacks = sDummyCallbacks;
//    Button access_calls;
    TextView nb_calls, nb_confs;
    CallListAdapter confs_adapter;

    private CallListAdapter calls_adapter;

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
        public void selectedCall(Conference c) {
        }
    };

    /**
     * The Activity calling this fragment has to implement this interface
     * 
     */
    public interface Callbacks {

        public ISipService getService();

        public void selectedCall(Conference c);

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

                updateLists();

            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        }

    }

    public void updateLists() throws RemoteException {
        HashMap<String, SipCall> calls = (HashMap<String, SipCall>) mCallbacks.getService().getCallList();
        HashMap<String, Conference> confs = (HashMap<String, Conference>) mCallbacks.getService().getConferenceList();

        updateCallList(calls);
        updateConferenceList(confs);
    }

    private void updateConferenceList(HashMap<String, Conference> confs) {
        nb_confs.setText("" + confs.size());
        confs_adapter.update(new ArrayList<Conference>(confs.values()));
    }

    private void updateCallList(HashMap<String, SipCall> calls) {
        nb_calls.setText("" + calls.size());
        ArrayList<Conference> conferences = new ArrayList<Conference>();
        for (SipCall call : calls.values()) {
            Log.w(TAG, "SimpleCall:" + call.getCallId());
            Conference confOne = new Conference("-1");
            confOne.getParticipants().add(call);
            conferences.add(confOne);
        }

        calls_adapter.update(conferences);

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

        nb_calls = (TextView) inflatedView.findViewById(R.id.calls_counter);
        nb_confs = (TextView) inflatedView.findViewById(R.id.confs_counter);

        confs_adapter = new CallListAdapter(getActivity());
        ((ListView) inflatedView.findViewById(R.id.confs_list)).setAdapter(confs_adapter);

        calls_adapter = new CallListAdapter(getActivity());
        ((ListView) inflatedView.findViewById(R.id.calls_list)).setAdapter(calls_adapter);
        ((ListView) inflatedView.findViewById(R.id.calls_list)).setOnItemClickListener(callClickListener);
        ((ListView) inflatedView.findViewById(R.id.confs_list)).setOnItemClickListener(callClickListener);

        return inflatedView;
    }
    
    OnItemClickListener callClickListener= new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> arg0, View v, int arg2, long arg3) {
            mCallbacks.selectedCall((Conference)v.getTag());
        }
    };
   

    public class CallListAdapter extends BaseAdapter {

        private ArrayList<Conference> calls;

        private Context mContext;

        public CallListAdapter(Context act) {
            super();
            mContext = act;
            calls = new ArrayList<Conference>();

        }

        public void remove(Conference transfer) {

        }

        public void update(ArrayList<Conference> list) {
            calls.clear();
            calls.addAll(list);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return calls.size();
        }

        @Override
        public Conference getItem(int position) {
            return calls.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_calllist, null);

            Conference call = calls.get(position);
            if (call.getParticipants().size() == 1) {
                ((TextView) convertView.findViewById(R.id.call_title)).setText(call.getParticipants().get(0).getContact().getmDisplayName());
            } else {
                String tmp = "Conference with " + call.getParticipants().size() + " participants";
                ((TextView) convertView.findViewById(R.id.call_title)).setText(tmp);
            }
            // ((TextView) convertView.findViewById(R.id.num_participants)).setText("" + call.getParticipants().size());
            ((TextView) convertView.findViewById(R.id.call_status)).setText(call.getState());

            convertView.setTag(call);
            return convertView;
        }

    }

}
