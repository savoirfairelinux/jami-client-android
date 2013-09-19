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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.Vibrator;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnDragListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.model.Conference;
import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.service.ISipService;
import com.savoirfairelinux.sflphone.views.SwipeListViewTouchListener;

public class CallListFragment extends Fragment {
    static final String TAG = CallListFragment.class.getSimpleName();

    private Callbacks mCallbacks = sDummyCallbacks;

    CallListAdapter mAdapter;

    public static final int REQUEST_TRANSFER = 10;
    public static final int REQUEST_CONF = 20;

    @Override
    public void onCreate(Bundle savedBundle) {
        super.onCreate(savedBundle);

        mAdapter = new CallListAdapter(getActivity());

    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {

        @Override
        public ISipService getService() {
            return null;
        }

        @Override
        public void onCallSelected(Conference conf) {
        }

        @Override
        public void onCallsTerminated() {
        }
    };

    /**
     * The Activity calling this fragment has to implement this interface
     * 
     */
    public interface Callbacks {
        public ISipService getService();

        public void onCallSelected(Conference conf);

        public void onCallsTerminated();

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
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    ListView list;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.frag_call_list, container, false);

        list = (ListView) rootView.findViewById(R.id.call_list);

        list.setDivider(getResources().getDrawable(android.R.drawable.divider_horizontal_dark));
        list.setDividerHeight(10);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(mItemClickListener);
        list.setOnTouchListener(new SwipeListViewTouchListener(list, new SwipeListViewTouchListener.OnSwipeCallback() {
            @Override
            public void onSwipeLeft(ListView listView, int[] reverseSortedPositions) {
                // Log.i(this.getClass().getName(), "swipe left : pos="+reverseSortedPositions[0]);
                // TODO : YOUR CODE HERE FOR LEFT ACTION
                Conference tmp = mAdapter.getItem(reverseSortedPositions[0]);
                try {
                    if (tmp.hasMultipleParticipants()) {
                        mCallbacks.getService().hangUpConference(tmp.getId());
                    } else {
                        mCallbacks.getService().hangUp(tmp.getParticipants().get(0).getCallId());
                    }
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

            @Override
            public void onSwipeRight(ListView listView, int[] reverseSortedPositions) {
                // Log.i(ProfileMenuActivity.class.getClass().getName(), "swipe right : pos="+reverseSortedPositions[0]);
                // TODO : YOUR CODE HERE FOR RIGHT ACTION

                Conference tmp = mAdapter.getItem(reverseSortedPositions[0]);
                try {
                    if (tmp.hasMultipleParticipants()) {
                        if (tmp.isOnHold()) {
                            mCallbacks.getService().unholdConference(tmp.getId());
                        } else {
                            mCallbacks.getService().holdConference(tmp.getId());
                        }
                    } else {
                        if (tmp.isOnHold()) {
                            Toast.makeText(getActivity(), "call is on hold,  unholding", Toast.LENGTH_SHORT).show();
                            mCallbacks.getService().unhold(tmp.getParticipants().get(0).getCallId());
                        } else {
                            Toast.makeText(getActivity(), "call is current,  holding", Toast.LENGTH_SHORT).show();
                            mCallbacks.getService().hold(tmp.getParticipants().get(0).getCallId());
                        }
                    }
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }, true, // example : left action = dismiss
                false)); // example : right action without dismiss animation);
        list.setOnItemLongClickListener(mItemLongClickListener);

        return rootView;
    }

    OnDragListener dragListener = new OnDragListener() {

        @SuppressWarnings("deprecation") // deprecated in API 16....
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
                dialog.setTargetFragment(CallListFragment.this, 0);
                dialog.show(getFragmentManager(), "dialog");

                Toast.makeText(
                        getActivity(),
                        "Dropped " + initial.getParticipants().get(0).getContact().getmDisplayName() + " on "
                                + target.getParticipants().get(0).getContact().getmDisplayName(), Toast.LENGTH_SHORT).show();
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

    private OnItemLongClickListener mItemLongClickListener = new OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> arg0, View view, int pos, long arg3) {
            final Vibrator vibe = (Vibrator) view.getContext().getSystemService(Context.VIBRATOR_SERVICE);
            vibe.vibrate(80);
            Intent i = new Intent();
            Bundle b = new Bundle();
            b.putParcelable("conference", mAdapter.getItem(pos));
            i.putExtra("bconference", b);

            DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
            ClipData data = ClipData.newIntent("conference", i);
            view.startDrag(data, shadowBuilder, view, 0);
            return false;
        }

    };

    private OnItemClickListener mItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
            mCallbacks.onCallSelected(mAdapter.getItem(pos));

        }
    };

    @SuppressWarnings("unchecked") // No proper solution with HashMap runtime cast
    public void update() {
        try {
            HashMap<String, SipCall> list = (HashMap<String, SipCall>) mCallbacks.getService().getCallList();

            // Toast.makeText(getActivity(), "Calls: " + list.size(), Toast.LENGTH_SHORT).show();
            ArrayList<Conference> conferences = new ArrayList<Conference>();
            HashMap<String, Conference> tmp = (HashMap<String, Conference>) mCallbacks.getService().getConferenceList();
            conferences.addAll(tmp.values());

            ArrayList<SipCall> simple_calls = new ArrayList<SipCall>(list.values());
            for (SipCall call : simple_calls) {
                Conference confOne = new Conference("-1");
                confOne.getParticipants().add(call);
                conferences.add(confOne);
            }

            if (conferences.isEmpty()) {
                mCallbacks.onCallsTerminated();
            }

            mAdapter.update(conferences);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }

    }

    // private void makeTransferDialog(int groupPosition) {
    // FragmentManager fm = getFragmentManager();
    // TransferDFragment editNameDialog = new TransferDFragment();
    //
    // if (!mAdapter.getItem(groupPosition).hasMultipleParticipants()) {
    // Bundle b = new Bundle();
    // b.putParcelableArrayList("calls", mAdapter.getConcurrentCalls(groupPosition));
    // b.putParcelable("call_selected", mAdapter.getItem(groupPosition));
    // editNameDialog.setArguments(b);
    // editNameDialog.setTargetFragment(this, REQUEST_TRANSFER);
    // editNameDialog.show(fm, "dialog");
    // } else {
    // Toast.makeText(getActivity(), "Transfer a Conference ?", Toast.LENGTH_SHORT).show();
    // }
    //
    // }
    //
    // private void makeConferenceDialog(int groupPosition) {
    // FragmentManager fm = getFragmentManager();
    // ConferenceDFragment confDialog = ConferenceDFragment.newInstance();
    //
    // Bundle b = new Bundle();
    // b.putParcelableArrayList("calls", mAdapter.getConcurrentCalls(groupPosition));
    // b.putParcelable("call_selected", mAdapter.getItem(groupPosition));
    // confDialog.setArguments(b);
    // confDialog.setTargetFragment(this, REQUEST_CONF);
    // confDialog.show(fm, "dialog");
    //
    // }

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
                    mAdapter.remove(transfer);
                    mAdapter.remove(c);
                    mAdapter.notifyDataSetChanged();
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                Toast.makeText(getActivity(), "Transfer complete", Toast.LENGTH_LONG).show();
                break;

            case 1:
                String to = data.getStringExtra("to_number");
                transfer = data.getParcelableExtra("transfer");
                try {
                    Toast.makeText(getActivity(), "Transferring " + transfer.getParticipants().get(0).getContact().getmDisplayName() + " to " + to,
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

    public class CallListAdapter extends BaseAdapter {

        private ArrayList<Conference> calls;

        private Context mContext;

        public CallListAdapter(Context act) {
            super();
            mContext = act;
            calls = new ArrayList<Conference>();

        }

        public ArrayList<Conference> getConcurrentCalls(int position) {
            ArrayList<Conference> toReturn = new ArrayList<Conference>();
            for (int i = 0; i < calls.size(); ++i) {
                if (position != i)
                    toReturn.add(calls.get(i));
            }
            return toReturn;
        }

        public void remove(Conference transfer) {
            calls.remove(transfer);
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
                
                long duration = System.currentTimeMillis() / 1000 - (call.getParticipants().get(0).getTimestamp_start());

                ((TextView) convertView.findViewById(R.id.call_time)).setText(String.format("%d:%02d:%02d", duration/3600, (duration%3600)/60, (duration%60)));
            } else {
                String tmp = "Conference with " + call.getParticipants().size() + " participants";

                ((TextView) convertView.findViewById(R.id.call_title)).setText(tmp);
            }
            
            ((TextView) convertView.findViewById(R.id.call_status)).setText(call.getState());
            convertView.setOnDragListener(dragListener);

            convertView.setTag(call);
            return convertView;
        }

    }

    public static class DropActionsChoice extends DialogFragment {

        ListAdapter mAdapter;
        private Bundle args;

        /**
         * Create a new instance of CallActionsDFragment
         */
        public static DropActionsChoice newInstance() {
            DropActionsChoice f = new DropActionsChoice();
            return f;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Pick a style based on the num.
            int style = DialogFragment.STYLE_NORMAL, theme = 0;
            setStyle(style, theme);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            ListView rootView = new ListView(getActivity());

            args = getArguments();
            mAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, getResources().getStringArray(
                    R.array.drop_actions));

            // ListView list = (ListView) rootView.findViewById(R.id.concurrent_calls);
            rootView.setAdapter(mAdapter);
            rootView.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                    Intent in = new Intent();

                    in.putExtra("transfer", args.getParcelable("call_initial"));
                    in.putExtra("target", args.getParcelable("call_targeted"));

                    switch (pos) {
                    case 0: // Transfer
                        getTargetFragment().onActivityResult(REQUEST_TRANSFER, 0, in);
                        break;
                    case 1: // Conference
                        getTargetFragment().onActivityResult(REQUEST_CONF, 0, in);
                        break;
                    }
                    dismiss();

                }
            });

            final AlertDialog a = new AlertDialog.Builder(getActivity()).setView(rootView).setTitle("Choose Action")
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dismiss();
                        }
                    }).create();

            return a;
        }
    }

}
