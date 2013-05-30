/*
 *  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
 *
 *  Author: Adrien Beraud <adrien.beraud@gmail.com>
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
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.adapters.CallElementAdapter;
import com.savoirfairelinux.sflphone.client.SFLPhoneHomeActivity;
import com.savoirfairelinux.sflphone.client.SFLPhonePreferenceActivity;
import com.savoirfairelinux.sflphone.model.SipCall;
import com.savoirfairelinux.sflphone.service.ISipService;

/**
 * Main list of Call Elements. We don't manage contacts ourself so they are
 */
public class CallElementListFragment extends ListFragment {
    private static final String TAG = CallElementListFragment.class.getSimpleName();
    private CallElementAdapter mAdapter;


    private Callbacks mCallbacks = sDummyCallbacks;
    Button access_calls;

    /**
     * A dummy implementation of the {@link Callbacks} interface that does nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onCallSelected(SipCall c) {
        }

        @Override
        public ISipService getService() {
            Log.i(TAG, "I'm a dummy");
            return null;
        }
    };

    /**
     * The Activity calling this fragment has to implement this interface
     * 
     */
    public interface Callbacks {
        public void onCallSelected(SipCall c);

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
    
    @Override
    public void onResume(){
        super.onResume();
        Log.i(TAG,"RESUMING MAIN FRAG BORDEL");
        if (mCallbacks.getService() != null) {
            try {
                HashMap<String, SipCall> calls = (HashMap<String, SipCall>) mCallbacks.getService().getCallList();
                Log.i(TAG, "Call size "+calls.size());
                access_calls.setText(calls.size()+" on going calls");

                
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

    /**
     * Runnable that fill information in a contact card asynchroniously.
     */
    /*
     * public static class InfosLoader implements Runnable { private View view; private long cid; private ContentResolver cr;
     * 
     * public InfosLoader(Context context, View element, long contact_id) { cid = contact_id; cr = context.getContentResolver(); view = element; }
     * 
     * public static Bitmap loadContactPhoto(ContentResolver cr, long id) { Uri uri =
     * ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id); InputStream input =
     * ContactsContract.Contacts.openContactPhotoInputStream(cr, uri); if (input == null) { return null; } return BitmapFactory.decodeStream(input); }
     * 
     * @Override public void run() { final Bitmap photo_bmp = loadContactPhoto(cr, cid);
     * 
     * Cursor phones = cr.query(CommonDataKinds.Phone.CONTENT_URI, CONTACTS_PHONES_PROJECTION, CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]
     * { Long.toString(cid) }, null);
     * 
     * final List<String> numbers = new ArrayList<String>(); while (phones.moveToNext()) { String number =
     * phones.getString(phones.getColumnIndex(CommonDataKinds.Phone.NUMBER)); // int type =
     * phones.getInt(phones.getColumnIndex(CommonDataKinds.Phone.TYPE)); numbers.add(number); } phones.close(); // TODO: same for SIP adresses.
     * 
     * final Bitmap bmp = photo_bmp; view.post(new Runnable() {
     * 
     * @Override public void run() { } }); } }
     */

    public void addCall(SipCall c) {
        // Log.i(TAG, "Adding call " + c.mCallInfo.mDisplayName);
        if (mAdapter == null) {
            Log.w(TAG, "mAdapter is null");
            return;
        }
        mAdapter.add(c);
    }

    // public void removeCall(SipCall c) {
    // Log.i(TAG, "Removing call " + c.mCallInfo.mDisplayName);
    // mAdapter.remove(c);
    // }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new CallElementAdapter(getActivity(), new ArrayList<SipCall>());
        
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Give some text to display if there is no data. In a real
        // application this would come from a resource.
        // setEmptyText("No phone numbers");

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        final Context context = getActivity();
        ListView lv = getListView();
        lv.setAdapter(mAdapter);
        lv.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                Log.i(TAG, "On Long Click");
                final CharSequence[] items = { "Hang up Call", "Send Message", "Add to Conference" };
                final SipCall call = mAdapter.getItem(pos);
                // // FIXME
                // service = sflphoneApplication.getSipService();
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Action to perform with " + call.getContacts().get(0).getmDisplayName()).setCancelable(true)
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int item) {
                                Log.i(TAG, "Selected " + items[item]);
                                // switch (item) {
                                // case 0:
                                // call.notifyServiceHangup(service);
                                // break;
                                // case 1:
                                // call.sendTextMessage();
                                // // Need to hangup this call immediately since no way to do it after this action
                                // call.notifyServiceHangup(service);
                                // break;
                                // case 2:
                                // call.addToConference();
                                // // Need to hangup this call immediately since no way to do it after this action
                                // call.notifyServiceHangup(service);
                                // break;
                                // default:
                                // break;
                                // }
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();

                return true;
            }
        });

        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int pos, long arg3) {
                mCallbacks.onCallSelected(mAdapter.getItem(pos));

            }

        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.call_element_menu, menu);

    }

    private static final int REQUEST_CODE_PREFERENCES = 1;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "onOptionsItemSelected " + item.getItemId());
        switch (item.getItemId()) {
        case R.id.menu_settings:
            Intent launchPreferencesIntent = new Intent().setClass(getActivity(), SFLPhonePreferenceActivity.class);
            startActivityForResult(launchPreferencesIntent, SFLPhoneHomeActivity.REQUEST_CODE_PREFERENCES);
            break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void updateCall(String iD, String newState) {
        if (mAdapter == null) {
            Log.w(TAG, "mAdapter is null");
            return;
        }
        mAdapter.update(iD, newState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View inflatedView = inflater.inflate(R.layout.frag_call_element, container, false);

        access_calls = (Button) inflatedView.findViewById(R.id.access_callactivity);

        access_calls.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                HashMap<String, SipCall> calls;
                try {
                    calls = (HashMap<String, SipCall>) mCallbacks.getService().getCallList();
                    if (calls.size() == 0) {
                        Toast.makeText(getActivity(), "No calls", Toast.LENGTH_SHORT).show();
                    } else {
                        mCallbacks.onCallSelected((SipCall) calls.values().toArray()[0]);
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, e.toString());
                }

            }
        });
        
        

        // ((Button) inflatedView.findViewById(R.id.button_attended)).setOnClickListener(new OnClickListener() {
        //
        // @Override
        // public void onClick(View v) {
        // if (mAdapter.getCount() == 2) {
        // try {
        // service.attendedTransfer(mAdapter.getItem(0).getCallId(), mAdapter.getItem(1).getCallId());
        // mAdapter.clear();
        // } catch (RemoteException e) {
        // Log.e(TAG, e.toString());
        // }
        // } else {
        // Toast.makeText(getActivity(), "You need two calls one on Hold the other current to bind them", Toast.LENGTH_LONG).show();
        // }
        //
        // }
        // });

        // ((Button) inflatedView.findViewById(R.id.button_conf)).setOnClickListener(new OnClickListener() {
        //
        // @Override
        // public void onClick(View v) {
        // if (mAdapter.getCount() == 2) {
        // try {
        // service.joinParticipant(mAdapter.getItem(0).getCallId(), mAdapter.getItem(1).getCallId());
        // } catch (RemoteException e) {
        // Log.e(TAG, e.toString());
        // }
        // } else {
        // Toast.makeText(getActivity(), "You need two calls one on Hold the other current to create a conference", Toast.LENGTH_LONG)
        // .show();
        // }
        // }
        // });

        // ((ToggleButton) inflatedView.findViewById(R.id.switch_hold)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
        //
        // @Override
        // public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // try {
        // ArrayList<String> confList = (ArrayList<String>) service.getConferenceList();
        // if (!confList.isEmpty()) {
        // if (isChecked) {
        // service.holdConference(confList.get(0));
        // } else {
        // service.unholdConference(confList.get(0));
        // }
        // }
        // } catch (RemoteException e) {
        // Log.e(TAG, e.toString());
        // }
        //
        // }
        // });

        return inflatedView;
    }


}
