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
import com.savoirfairelinux.sflphone.model.Conference;
import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.service.ISipService;

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
        public void onCallSelected(ArrayList<SipCall> call) {
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

        public void onCallSelected(ArrayList<SipCall> call);

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
            Log.w(TAG, "Updating");
            HashMap<String, SipCall> list = (HashMap<String, SipCall>) mCallbacks.getService().getCallList();

            Toast.makeText(getActivity(), "Calls: "+list.size(), Toast.LENGTH_SHORT).show();
            ArrayList<Conference> conferences = new ArrayList<Conference>();
            ArrayList<String> tmp = (ArrayList<String>) mCallbacks.getService().getConferenceList();
            for (String confid : tmp) {
                Log.w(TAG, "Conference:"+confid);
                Conference toAdd = new Conference(confid);
                
                toAdd.setState(mCallbacks.getService().getConferenceDetails(confid));
                Toast.makeText(getActivity(), "State of Conf: "+toAdd.getState(), Toast.LENGTH_SHORT).show();
                ArrayList<String> conf_participants = (ArrayList<String>) mCallbacks.getService().getParticipantList(confid);
                for (String part : conf_participants) {
                    Log.w(TAG, "participant:"+part);
                    toAdd.getParticipants().add(list.get(part));
                    list.remove(part);
                }
                conferences.add(toAdd);
            }

            ArrayList<SipCall> simple_calls = new ArrayList<SipCall>(list.values());
            for (SipCall call : simple_calls) {
                Log.w(TAG, "SimpleCall:"+call.getCallId());
                Conference confOne = new Conference("-1");
                confOne.getParticipants().add(call);
                conferences.add(confOne);
            }
            
            if(conferences.isEmpty()){
                mCallbacks.onCallsTerminated();
            }

            mAdapter.update(conferences);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString());
        }

    }

    private void makeTransferDialog(int groupPosition) {
        FragmentManager fm = getFragmentManager();
        TransferDFragment editNameDialog = new TransferDFragment();

        if (mAdapter.getGroup(groupPosition).getParticipants().size() == 1) {
            Bundle b = new Bundle();
            b.putParcelableArrayList("calls", mAdapter.getConcurrentCalls(groupPosition));
            b.putParcelable("call_selected", mAdapter.getGroup(groupPosition).getParticipants().get(0));
            editNameDialog.setArguments(b);
            editNameDialog.setTargetFragment(this, REQUEST_TRANSFER);
            editNameDialog.show(fm, "dialog");
        } else {
            Toast.makeText(getActivity(), "Transfer a Conference ?", Toast.LENGTH_SHORT).show();
        }

    }

    private void makeConferenceDialog(int groupPosition) {
        FragmentManager fm = getFragmentManager();
        ConferenceDFragment confDialog = ConferenceDFragment.newInstance();

        if (mAdapter.getGroup(groupPosition).getParticipants().size() == 1) {
            Bundle b = new Bundle();
            b.putParcelableArrayList("calls", mAdapter.getConcurrentCalls(groupPosition));
            b.putParcelable("call_selected", mAdapter.getGroup(groupPosition));
            confDialog.setArguments(b);
            confDialog.setTargetFragment(this, REQUEST_CONF);
            confDialog.show(fm, "dialog");
        } else {
            Toast.makeText(getActivity(), "Already a Conference", Toast.LENGTH_SHORT).show();
        }

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
                    Toast.makeText(getActivity(), "Transferring " + transfer.getContact().getmDisplayName() + " to " + to, Toast.LENGTH_SHORT).show();
                    mCallbacks.getService().transfer(transfer.getCallId(), to);

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
                Conference call1 = data.getParcelableExtra("call1");
                Conference call2 = data.getParcelableExtra("call2");
                try {

                    mCallbacks.getService().joinParticipant(call1.getParticipants().get(0).getCallId(), call2.getParticipants().get(0).getCallId());

                    // ArrayList<String> tmp = new ArrayList<String>();
                    // tmp.add(call1.getCallId());
                    // tmp.add(call2.getCallId());
                    // mCallbacks.getService().createConfFromParticipantList(tmp);

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

        private ArrayList<Conference> calls;

        private Context mContext;
        private int lastExpandedGroupPosition;

        public CallListAdapter(Context activity) {
            calls = new ArrayList<Conference>();
            mContext = activity;
        }

        public void remove(SipCall transfer) {
            calls.remove(transfer);

        }

        // public String getCurrentCall() {
        // for (int i = 0; i < calls.size(); ++i) {
        // if (calls.get(i).getCallStateInt() == SipCall.state.CALL_STATE_CURRENT)
        // return calls.get(i).getCallId();
        // }
        // return "";
        // }

        public ArrayList<Conference> getConcurrentCalls(int position) {
            ArrayList<Conference> toReturn = new ArrayList<Conference>();
            for (int i = 0; i < calls.size(); ++i) {
                if (position != i)
                    toReturn.add(calls.get(i));
            }
            return toReturn;
        }

        public ArrayList<Conference> getCalls() {
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
            
            convertView.setAnimation(AnimationUtils.loadAnimation(getActivity(), R.animator.slide_down));
            
            

            ((ImageButton) convertView.findViewById(R.id.action_hangup)).setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    try {
                        if (getGroup(groupPosition).getParticipants().size() == 1) {
                            mCallbacks.getService().hangUp(getGroup(groupPosition).getParticipants().get(0).getCallId());
                        } else {
                            mCallbacks.getService().hangUpConference(getGroup(groupPosition).getId());
                        }
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
                            if (getGroup(groupPosition).getParticipants().size() == 1) {
                                mCallbacks.getService().hold(getGroup(groupPosition).getParticipants().get(0).getCallId());
                            } else {
                                mCallbacks.getService().holdConference(getGroup(groupPosition).getId());
                            }

                            ((Button) v).setText("Unhold");
                        } else {
                            if (getGroup(groupPosition).getParticipants().size() == 1) {
                                mCallbacks.getService().unhold(getGroup(groupPosition).getParticipants().get(0).getCallId());
                            } else {
                                mCallbacks.getService().unholdConference(getGroup(groupPosition).getId());
                            }
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
        public Conference getGroup(int groupPosition) {
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

            Conference call = getGroup(groupPosition);
            if (call.getParticipants().size() == 1) {
                ((TextView) convertView.findViewById(R.id.call_title)).setText(call.getParticipants().get(0).getContact().getmDisplayName());
                ((TextView) convertView.findViewById(R.id.call_status)).setText(call.getParticipants().get(0).getCallStateString());
            } else {
                ((TextView) convertView.findViewById(R.id.call_title)).setText("Conference with "+call.getParticipants().size()+" participants");
            }

            ((RelativeLayout) convertView.findViewById(R.id.call_entry)).setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    mCallbacks.onCallSelected(getGroup(groupPosition).getParticipants());

                }
            });

            ((ImageButton) convertView.findViewById(R.id.expand_button)).setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {

                    if (isExpanded) {
                        list.collapseGroup(groupPosition);

                        final Animation animRotate = AnimationUtils.loadAnimation(getActivity(), R.animator.reverse);
                        ((ImageButton) v).startAnimation(animRotate);
                        ((ImageButton) v).setRotation(0);
                    } else {
                        list.expandGroup(groupPosition);
                        final Animation animRotate = AnimationUtils.loadAnimation(getActivity(), R.animator.reverse);
                        ((ImageButton) v).startAnimation(animRotate);
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

        public void update(ArrayList<Conference> list) {
            calls.clear();
            calls.addAll(list);
            notifyDataSetChanged();

        }

    }

}
