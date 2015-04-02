/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
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
package cx.ring.fragments;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.*;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnDragListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import cx.ring.client.CallActivity;
import cx.ring.client.HomeActivity;
import cx.ring.model.Conference;
import cx.ring.service.ISipService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

public class CallListFragment extends CallableWrapperFragment {

    private static final String TAG = CallListFragment.class.getSimpleName();

    private Callbacks mCallbacks = sDummyCallbacks;
    private TextView mConversationsTitleTextView;
    CallListAdapter mConferenceAdapter;

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

    };

    @Override
    public void callStateChanged(Conference c, String callID, String state) {
        Log.i(TAG, "callStateChanged" + callID + "    " + state);
        updateLists();
    }

    @Override
    public void confCreated(Conference c, String id) {
        Log.i(TAG, "confCreated");
        updateLists();
    }

    @Override
    public void confRemoved(Conference c, String id) {
        Log.i(TAG, "confRemoved");
        updateLists();
    }

    @Override
    public void confChanged(Conference c, String id, String state) {
        Log.i(TAG, "confChanged");
        updateLists();
    }

    @Override
    public void recordingChanged(Conference c, String callID, String filename) {
        Log.i(TAG, "confChanged");
        updateLists();
    }

    /**
     * The Activity calling this fragment has to implement this interface
     */
    public interface Callbacks {
        public ISipService getService();
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

            mConferenceAdapter.notifyDataSetChanged();
            mHandler.postAtTime(this, start + (((minutes * 60) + seconds + 1) * 1000));
        }
    };

    private Handler mHandler = new Handler();

    @Override
    public void onResume() {
        super.onResume();
        if (mCallbacks.getService() != null) {

            updateLists();
            if (!mConferenceAdapter.isEmpty()) {
                mHandler.postDelayed(mUpdateTimeTask, 0);
            }
        }

    }

    @SuppressWarnings("unchecked")
    // No proper solution with HashMap runtime cast
    public void updateLists() {
        try {
            HashMap<String, Conference> confs = (HashMap<String, Conference>) mCallbacks.getService().getConferenceList();
            String newTitle = getResources().getQuantityString(cx.ring.R.plurals.home_conferences_title, confs.size(), confs.size());
            mConversationsTitleTextView.setText(newTitle);
            mConferenceAdapter.updateDataset(new ArrayList<Conference>(confs.values()));
        } catch (RemoteException e) {
            e.printStackTrace();
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
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mUpdateTimeTask);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View inflatedView = inflater.inflate(cx.ring.R.layout.frag_call_list, container, false);

        mConversationsTitleTextView = (TextView) inflatedView.findViewById(cx.ring.R.id.confs_counter);

        mConferenceAdapter = new CallListAdapter(getActivity());
        ((ListView) inflatedView.findViewById(cx.ring.R.id.confs_list)).setAdapter(mConferenceAdapter);
        ((ListView) inflatedView.findViewById(cx.ring.R.id.confs_list)).setOnItemClickListener(callClickListener);
        ((ListView) inflatedView.findViewById(cx.ring.R.id.confs_list)).setOnItemLongClickListener(mItemLongClickListener);

        return inflatedView;
    }

    OnItemClickListener callClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> arg0, View v, int arg2, long arg3) {
            Intent intent = new Intent().setClass(getActivity(), CallActivity.class);
            intent.putExtra("resuming", true);
            intent.putExtra("conference", (Conference) v.getTag());
            startActivityForResult(intent, HomeActivity.REQUEST_CODE_CALL);
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
                convertView = LayoutInflater.from(mContext).inflate(cx.ring.R.layout.item_calllist, null);

            Conference call = calls.get(position);
            if (call.getParticipants().size() == 1) {
                ((TextView) convertView.findViewById(cx.ring.R.id.call_title)).setText(call.getParticipants().get(0).getmContact().getmDisplayName());

                long duration = (System.currentTimeMillis() - (call.getParticipants().get(0).getTimestampStart_())) / 1000;

                ((TextView) convertView.findViewById(cx.ring.R.id.call_time)).setText(String.format("%d:%02d:%02d", duration / 3600, (duration % 3600) / 60,
                        (duration % 60)));
            } else {
//                String tmp = "Conference with " + call.getParticipants().size() + " participants";
                ((TextView) convertView.findViewById(cx.ring.R.id.call_title)).setText(getString(cx.ring.R.string.home_conf_item, call.getParticipants().size()));
            }
            // ((TextView) convertView.findViewById(R.id.num_participants)).setText("" + call.getParticipants().size());
            ((TextView) convertView.findViewById(cx.ring.R.id.call_status)).setText(call.getState());

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
                    // Log.w(TAG, "ACTION_DRAG_STARTED");
                    break;
                case DragEvent.ACTION_DRAG_ENTERED:
                    // Log.w(TAG, "ACTION_DRAG_ENTERED");
                    v.setBackgroundColor(Color.GREEN);
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    // Log.w(TAG, "ACTION_DRAG_EXITED");
                    v.setBackgroundDrawable(getResources().getDrawable(cx.ring.R.drawable.item_generic_selector));
                    break;
                case DragEvent.ACTION_DROP:
                    // Log.w(TAG, "ACTION_DROP");
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
                    dialog.setTargetFragment(CallListFragment.this, 0);
                    dialog.show(getFragmentManager(), "dialog");

                    // view.setBackgroundColor(Color.WHITE);
                    // v.setBackgroundColor(Color.BLACK);
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    // Log.w(TAG, "ACTION_DRAG_ENDED");
                    View view1 = (View) event.getLocalState();
                    view1.setVisibility(View.VISIBLE);
                    v.setBackgroundDrawable(getResources().getDrawable(cx.ring.R.drawable.item_generic_selector));
                default:
                    break;
            }
            return true;
        }

    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Conference transfer;
        if (requestCode == REQUEST_TRANSFER) {
            switch (resultCode) {
                case 0:
                    Conference c = data.getParcelableExtra("target");
                    transfer = data.getParcelableExtra("transfer");
                    try {
                        mCallbacks.getService().attendedTransfer(transfer.getParticipants().get(0).getCallId(), c.getParticipants().get(0).getCallId());
                        mConferenceAdapter.notifyDataSetChanged();
                    } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    Toast.makeText(getActivity(), getString(cx.ring.R.string.home_transfer_complet), Toast.LENGTH_LONG).show();
                    break;

                case 1:
                    String to = data.getStringExtra("to_number");
                    transfer = data.getParcelableExtra("transfer");
                    try {
                        Toast.makeText(getActivity(), getString(cx.ring.R.string.home_transfering, transfer.getParticipants().get(0).getmContact().getmDisplayName(), to),
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

            Log.i(TAG, "joining calls:" + call_to_add.getId() + " and " + call_target.getId());

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
