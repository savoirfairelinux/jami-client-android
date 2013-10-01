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
import java.util.Observable;
import java.util.Observer;

import android.app.Activity;
import android.app.Fragment;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.ClipData.Item;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.DragShadowBuilder;
import android.view.View.OnDragListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.fragments.CallListFragment.DropActionsChoice;
import com.savoirfairelinux.sflphone.model.CallTimer;
import com.savoirfairelinux.sflphone.model.Conference;
import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.service.ISipService;

public class HomeFragment extends Fragment {
    private static final String TAG = HomeFragment.class.getSimpleName();

    private Callbacks mCallbacks = sDummyCallbacks;
    private TextView nb_calls, nb_confs;
    CallListAdapter confs_adapter, calls_adapter;
    CallTimer timer;
    
    public static final int REQUEST_TRANSFER = 10;
    public static final int REQUEST_CONF = 20;

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

    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            final long start = SystemClock.uptimeMillis();
            long millis = SystemClock.uptimeMillis() - start;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            calls_adapter.notifyDataSetChanged();
            confs_adapter.notifyDataSetChanged();
            mHandler.postAtTime(this, start + (((minutes * 60) + seconds + 1) * 1000));
        }
    };

    private Handler mHandler = new Handler();

    @Override
    public void onResume() {
        super.onResume();
        if (mCallbacks.getService() != null) {
            try {
                updateLists();
                if (!calls_adapter.isEmpty() || !confs_adapter.isEmpty()) {
                    mHandler.postDelayed(mUpdateTimeTask, 0);
                }

            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        }

    }

    @SuppressWarnings("unchecked")
    // No proper solution with HashMap runtime cast
    public void updateLists() throws RemoteException {
        HashMap<String, SipCall> calls = (HashMap<String, SipCall>) mCallbacks.getService().getCallList();
        HashMap<String, Conference> confs = (HashMap<String, Conference>) mCallbacks.getService().getConferenceList();

        updateCallList(calls);
        updateConferenceList(confs);
    }

    private void updateConferenceList(HashMap<String, Conference> confs) {
        nb_confs.setText("" + confs.size());
        confs_adapter.updateDataset(new ArrayList<Conference>(confs.values()));
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

        calls_adapter.updateDataset(conferences);

    }

    @Override
    public void onDetach() {

        super.onDetach();
        mCallbacks = sDummyCallbacks;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mUpdateTimeTask);
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

        ((ListView) inflatedView.findViewById(R.id.calls_list)).setOnItemLongClickListener(mItemLongClickListener);
        ((ListView) inflatedView.findViewById(R.id.confs_list)).setOnItemLongClickListener(mItemLongClickListener);

        return inflatedView;
    }

    OnItemClickListener callClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> arg0, View v, int arg2, long arg3) {
            mCallbacks.selectedCall((Conference) v.getTag());
        }
    };

    private OnItemLongClickListener mItemLongClickListener = new OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> adptv, View view, int pos, long arg3) {
            final Vibrator vibe = (Vibrator) view.getContext().getSystemService(Context.VIBRATOR_SERVICE);
            vibe.vibrate(80);
            Intent i = new Intent();
            Bundle b = new Bundle();
            b.putParcelable("conference", (Conference) adptv.getAdapter().getItem(pos));
            i.putExtra("bconference", b);

            DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
            ClipData data = ClipData.newIntent("conference", i);
            view.startDrag(data, shadowBuilder, view, 0);
            return false;
        }

    };

    public class CallListAdapter extends BaseAdapter implements Observer {

        private ArrayList<Conference> calls;

        private Context mContext;

        public CallListAdapter(Context act) {
            super();
            mContext = act;
            calls = new ArrayList<Conference>();

        }

        public ArrayList<Conference> getDataset() {
            return calls;
        }

        public void remove(Conference transfer) {

        }

        public void updateDataset(ArrayList<Conference> list) {
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

                long duration = System.currentTimeMillis() / 1000 - (call.getParticipants().get(0).getTimestamp_start());

                ((TextView) convertView.findViewById(R.id.call_time)).setText(String.format("%d:%02d:%02d", duration / 3600, (duration % 3600) / 60,
                        (duration % 60)));
            } else {
//                String tmp = "Conference with " + call.getParticipants().size() + " participants";
                ((TextView) convertView.findViewById(R.id.call_title)).setText(getString(R.string.home_conf_item, call.getParticipants().size()));
            }
            // ((TextView) convertView.findViewById(R.id.num_participants)).setText("" + call.getParticipants().size());
            ((TextView) convertView.findViewById(R.id.call_status)).setText(call.getState());

            convertView.setOnDragListener(dragListener);
            convertView.setTag(call);

            return convertView;
        }

        @Override
        public void update(Observable observable, Object data) {
            Log.i(TAG, "Updating views...");
            notifyDataSetChanged();
        }

    }

    OnDragListener dragListener = new OnDragListener() {

        @SuppressWarnings("deprecation")
        // deprecated in API 16....
        @Override
        public boolean onDrag(View v, DragEvent event) {
            switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                // Do nothing
                Log.w(TAG, "ACTION_DRAG_STARTED");
                break;
            case DragEvent.ACTION_DRAG_ENTERED:
                Log.w(TAG, "ACTION_DRAG_ENTERED");
                v.setBackgroundColor(Color.GREEN);
                break;
            case DragEvent.ACTION_DRAG_EXITED:
                Log.w(TAG, "ACTION_DRAG_EXITED");
                v.setBackgroundDrawable(getResources().getDrawable(R.drawable.item_call_selector));
                break;
            case DragEvent.ACTION_DROP:
                Log.w(TAG, "ACTION_DROP");
                View view = (View) event.getLocalState();

                Item i = event.getClipData().getItemAt(0);
                Intent intent = i.getIntent();
                intent.setExtrasClassLoader(Conference.class.getClassLoader());

                Conference initial = (Conference) view.getTag();
                Conference target = (Conference) v.getTag();

                if (initial == target) {
                    return true;
                }

                DropActionsChoice dialog = DropActionsChoice.newInstance();
                Bundle b = new Bundle();
                b.putParcelable("call_initial", initial);
                b.putParcelable("call_targeted", target);
                dialog.setArguments(b);
                dialog.setTargetFragment(HomeFragment.this, 0);
                dialog.show(getFragmentManager(), "dialog");

                // view.setBackgroundColor(Color.WHITE);
                // v.setBackgroundColor(Color.BLACK);
                break;
            case DragEvent.ACTION_DRAG_ENDED:
                Log.w(TAG, "ACTION_DRAG_ENDED");
                View view1 = (View) event.getLocalState();
                view1.setVisibility(View.VISIBLE);
                v.setBackgroundDrawable(getResources().getDrawable(R.drawable.item_call_selector));
            default:
                break;
            }
            return true;
        }

    };
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Conference transfer = null;
        if (requestCode == REQUEST_TRANSFER) {
            switch (resultCode) {
            case 0:
                Conference c = data.getParcelableExtra("target");
                transfer = data.getParcelableExtra("transfer");
                try {

                    mCallbacks.getService().attendedTransfer(transfer.getParticipants().get(0).getCallId(), c.getParticipants().get(0).getCallId());
                    calls_adapter.remove(transfer);
                    calls_adapter.remove(c);
                    calls_adapter.notifyDataSetChanged();
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                Toast.makeText(getActivity(), getString(R.string.home_transfer_complet), Toast.LENGTH_LONG).show();
                break;

            case 1:
                String to = data.getStringExtra("to_number");
                transfer = data.getParcelableExtra("transfer");
                try {
                    Toast.makeText(getActivity(), getString(R.string.home_transfering,transfer.getParticipants().get(0).getContact().getmDisplayName(),to),
                            Toast.LENGTH_SHORT).show();
                    mCallbacks.getService().transfer(transfer.getParticipants().get(0).getCallId(), to);
                    mCallbacks.getService().hangUp(transfer.getParticipants().get(0).getCallId());
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                break;

            default:
                break;
            }
        } else if (requestCode == REQUEST_CONF) {
            switch (resultCode) {
            case 0:
                Conference call_to_add = data.getParcelableExtra("transfer");
                Conference call_target = data.getParcelableExtra("target");

                bindCalls(call_to_add, call_target);
                break;

            default:
                break;
            }
        }
    }
    
    private void bindCalls(Conference call_to_add, Conference call_target) {
        try {

            if (call_target.hasMultipleParticipants() && !call_to_add.hasMultipleParticipants()) {

                mCallbacks.getService().addParticipant(call_to_add.getParticipants().get(0), call_target.getId());

            } else if (call_target.hasMultipleParticipants() && call_to_add.hasMultipleParticipants()) {

                // We join two conferences
                mCallbacks.getService().joinConference(call_to_add.getId(), call_target.getId());

            } else if (!call_target.hasMultipleParticipants() && call_to_add.hasMultipleParticipants()) {

                mCallbacks.getService().addParticipant(call_target.getParticipants().get(0), call_to_add.getId());

            } else {
                // We join two single calls to create a conf
                mCallbacks.getService().joinParticipant(call_to_add.getParticipants().get(0).getCallId(),
                        call_target.getParticipants().get(0).getCallId());
            }

        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
