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
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.service.ISipService;

public class CallListFragment extends Fragment {
    static final String TAG = "CallFragment";

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
        public void onCallSelected(SipCall call) {
        }
    };

    /**
     * The Activity calling this fragment has to implement this interface
     * 
     */
    public interface Callbacks {
        public ISipService getService();

        public void onCallSelected(SipCall call);

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

    ExpandableListView list;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.frag_call_list, container, false);

        list = (ExpandableListView) rootView.findViewById(R.id.call_list);
        list.setDividerHeight(2);
        list.setGroupIndicator(null);
        list.setAdapter(mAdapter);

        list.setClickable(true);
        list.setItemsCanFocus(true);
        return rootView;
    }

    public void update() {
        try {
            HashMap<String, SipCall> list = (HashMap<String, SipCall>) mCallbacks.getService().getCallList();
            mAdapter.update(list);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }

    }

    private void makeTransferDialog(int groupPosition) {
        FragmentManager fm = getFragmentManager();
        TransferDFragment editNameDialog = new TransferDFragment();

        Bundle b = new Bundle();
        b.putParcelableArrayList("calls", mAdapter.getConcurrentCalls(groupPosition));
        b.putParcelable("call_selected", mAdapter.getGroup(groupPosition));
        editNameDialog.setArguments(b);
        editNameDialog.setTargetFragment(this, REQUEST_TRANSFER);
        editNameDialog.show(fm, "dialog");

    }

    private void makeConferenceDialog(int groupPosition) {
        FragmentManager fm = getFragmentManager();
        ConferenceDFragment confDialog = new ConferenceDFragment();

        Bundle b = new Bundle();
        b.putParcelableArrayList("calls", mAdapter.getConcurrentCalls(groupPosition));
        b.putParcelable("call_selected", mAdapter.getGroup(groupPosition));
        confDialog.setArguments(b);
        confDialog.setTargetFragment(this, REQUEST_CONF);
        confDialog.show(fm, "dialog");

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SipCall transfer = null;
        if (requestCode == REQUEST_TRANSFER) {
            switch (resultCode) {
            case 0:
                SipCall c = data.getParcelableExtra("target");
                transfer = data.getParcelableExtra("transfer");
                try {

                    mCallbacks.getService().attendedTransfer(transfer.getCallId(), c.getCallId());
                    mAdapter.remove(transfer);

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
                    Toast.makeText(getActivity(), "Transferring " + transfer.getContacts().get(0).getmDisplayName() + " to " + to, Toast.LENGTH_SHORT)
                            .show();
                    mCallbacks.getService().transfer(transfer.getCallId(), to);

                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                break;

            default:
                break;
            }
        } else if(requestCode == REQUEST_CONF){
            switch (resultCode) {
            case 0:
                SipCall call1 = data.getParcelableExtra("call1");
                SipCall call2 = data.getParcelableExtra("call2");
                try {

                    mCallbacks.getService().createConference(call1.getCallId(), call2.getCallId());

                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                Toast.makeText(getActivity(), "Conference created", Toast.LENGTH_LONG).show();
                break;

            default:
                break;
            }
        }
    }

    /**
     * A simple adapter which maintains an ArrayList of photo resource Ids. Each photo is displayed as an image. This adapter supports clearing the
     * list of photos and adding a new photo.
     * 
     */
    public class CallListAdapter extends BaseExpandableListAdapter {
        // Sample data set. children[i] contains the children (String[]) for groups[i].
        private ArrayList<SipCall> calls;

        private Context mContext;
        private int lastExpandedGroupPosition;

        public CallListAdapter(Context activity) {
            calls = new ArrayList<SipCall>();
            mContext = activity;
        }

        public void remove(SipCall transfer) {
            calls.remove(transfer);

        }

        public String getCurrentCall() {
            for (int i = 0; i < calls.size(); ++i) {
                if (calls.get(i).getCallStateInt() == SipCall.state.CALL_STATE_CURRENT)
                    return calls.get(i).getCallId();
            }
            return "";
        }

        public ArrayList<SipCall> getConcurrentCalls(int position) {
            ArrayList<SipCall> toReturn = new ArrayList<SipCall>();
            for (int i = 0; i < calls.size(); ++i) {
                if (position != i)
                    toReturn.add(calls.get(i));
            }
            return toReturn;
        }

        public ArrayList<SipCall> getCalls() {
            return calls;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return null;
        }

        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        public int getChildrenCount(int groupPosition) {
            return 1;
        }

        public View getChildView(final int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

            if (convertView == null)
                convertView = LayoutInflater.from(mContext).inflate(R.layout.expandable_child, null);

            ((ImageButton) convertView.findViewById(R.id.action_hangup)).setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    try {
                        mCallbacks.getService().hangUp(getGroup(groupPosition).getCallId());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                }
            });

            ((Button) convertView.findViewById(R.id.action_hold)).setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {

                    try {
                        if (((Button) v).getText().toString().contentEquals("Hold")) {
                            mCallbacks.getService().hold(getGroup(groupPosition).getCallId());
                            ((Button) v).setText("Unhold");
                        } else {
                            mCallbacks.getService().unhold(getGroup(groupPosition).getCallId());
                            ((Button) v).setText("Hold");
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                }
            });

            ((Button) convertView.findViewById(R.id.action_conf)).setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    makeConferenceDialog(groupPosition);
                }

            });

            ((Button) convertView.findViewById(R.id.action_transfer)).setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    makeTransferDialog(groupPosition);
                }

            });

            return convertView;
        }

        @Override
        public SipCall getGroup(int groupPosition) {
            return calls.get(groupPosition);
        }

        @Override
        public int getGroupCount() {
            return calls.size();
        }

        @Override
        public void onGroupCollapsed(int groupPosition) {
            super.onGroupCollapsed(groupPosition);
        }

        @Override
        public void onGroupExpanded(int groupPosition) {

            // collapse the old expanded group, if not the same
            // as new group to expand
            if (groupPosition != lastExpandedGroupPosition) {
                list.collapseGroup(lastExpandedGroupPosition);
            }

            super.onGroupExpanded(groupPosition);
            lastExpandedGroupPosition = groupPosition;
        }

        @Override
        public long getGroupId(int groupPosition) {
            return 0;
        }

        public View getGroupView(final int groupPosition, final boolean isExpanded, View convertView, ViewGroup parent) {

            if (convertView == null)
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_calllist, null);

            SipCall call = getGroup(groupPosition);
            ((TextView) convertView.findViewById(R.id.call_title)).setText(call.getContacts().get(0).getmDisplayName());
            ((TextView) convertView.findViewById(R.id.call_status)).setText("" + call.getCallStateString());

            ((RelativeLayout) convertView.findViewById(R.id.call_entry)).setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    mCallbacks.onCallSelected(getGroup(groupPosition));

                }
            });

            ((ImageButton) convertView.findViewById(R.id.expand_button)).setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    final Animation animRotate = AnimationUtils.loadAnimation(getActivity(), R.animator.anim_rotate);
                    if (isExpanded) {
                        list.collapseGroup(groupPosition);
                        // ((ImageButton) v).startAnimation(animRotate);
                        ((ImageButton) v).setRotation(0);
                    } else {
                        list.expandGroup(groupPosition);
                        // ((ImageButton) v).startAnimation(animRotate);
                        ((ImageButton) v).setRotation(180);

                    }
                }
            });

            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }

        public void update(HashMap<String, SipCall> list) {
            calls.clear();
            calls.addAll(list.values());
            notifyDataSetChanged();

        }

    }

}
