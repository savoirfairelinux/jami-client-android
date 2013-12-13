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
package org.sflphone.fragments;

import org.sflphone.R;
import org.sflphone.adapters.DiscussArrayAdapter;
import org.sflphone.model.SipMessage;
import org.sflphone.service.ISipService;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class IMFragment extends Fragment {
    static final String TAG = IMFragment.class.getSimpleName();

    private Callbacks mCallbacks = sDummyCallbacks;

    DiscussArrayAdapter mAdapter;
    ListView list;

    private EditText sendTextField;

    public static final int REQUEST_TRANSFER = 10;
    public static final int REQUEST_CONF = 20;

    @Override
    public void onCreate(Bundle savedBundle) {
        super.onCreate(savedBundle);

        mAdapter = new DiscussArrayAdapter(getActivity(), getArguments());

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
        public boolean sendIM(SipMessage msg) {
            return false;
        }

    };

    /**
     * The Activity calling this fragment has to implement this interface
     * 
     */
    public interface Callbacks {
        public ISipService getService();

        public boolean sendIM(SipMessage msg);
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.frag_imessaging, container, false);

        list = (ListView) rootView.findViewById(R.id.message_list);
        list.setAdapter(mAdapter);

        sendTextField = (EditText) rootView.findViewById(R.id.send_im_edittext);

        sendTextField.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    if (sendTextField.getText().toString().length() > 0) {
                        SipMessage toSend = new SipMessage(false, sendTextField.getText().toString());
                        putMessage(toSend);
                        sendTextField.setText("");
                        mCallbacks.sendIM(toSend);
                    }
                }
                return true;
            }
        });

        ((Button) rootView.findViewById(R.id.send_im_button)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (sendTextField.getText().toString().length() > 0) {
                    SipMessage toSend = new SipMessage(false, sendTextField.getText().toString());
                    putMessage(toSend);
                    sendTextField.setText("");
                    mCallbacks.sendIM(toSend);
                }
            }
        });

        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void putMessage(SipMessage msg) {
        mAdapter.add(msg);
        Log.i(TAG, "Messages" + mAdapter.getCount());
    }
}
