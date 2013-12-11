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
package org.sflphone.receivers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.sflphone.client.CallActivity;
import org.sflphone.model.Account;
import org.sflphone.model.CallContact;
import org.sflphone.model.Conference;
import org.sflphone.model.SipCall;
import org.sflphone.model.SipMessage;
import org.sflphone.service.CallManagerCallBack;
import org.sflphone.service.ConfigurationManagerCallback;
import org.sflphone.service.ISipService.Stub;
import org.sflphone.service.ServiceConstants;
import org.sflphone.service.SipService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

public class IncomingReceiver extends BroadcastReceiver {

    static final String TAG = IncomingReceiver.class.getSimpleName();

    SipService callback;
    Stub mBinder;

    public IncomingReceiver(SipService client, Stub bind) {
        callback = client;
        mBinder = bind;
    }

    @SuppressWarnings("unchecked")
    // Hashmap runtime cast
    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().contentEquals(ConfigurationManagerCallback.ACCOUNT_STATE_CHANGED)) {

            Log.i(TAG, "Received" + intent.getAction());
            callback.sendBroadcast(intent);

        } else if (intent.getAction().contentEquals(ConfigurationManagerCallback.ACCOUNTS_CHANGED)) {

            Log.i(TAG, "Received" + intent.getAction());
            callback.sendBroadcast(intent);

        } else if (intent.getAction().contentEquals(CallManagerCallBack.INCOMING_TEXT)) {

            Bundle extra = intent.getBundleExtra("com.savoirfairelinux.sflphone.service.newtext");
            Log.i(TAG, "Received" + intent.getAction());
            if (callback.getCurrent_calls().get(extra.getString("CallID")) != null) {
                callback.getCurrent_calls().get(extra.get("CallID")).addSipMessage(new SipMessage(true, extra.getString("Msg")));
            } else if (callback.getCurrent_confs().get(extra.getString("CallID")) != null) {
                callback.getCurrent_confs().get(extra.get("CallID")).addSipMessage(new SipMessage(true, extra.getString("Msg")));
            } else
                return;

            callback.sendBroadcast(intent);

        } else if (intent.getAction().contentEquals(CallManagerCallBack.INCOMING_CALL)) {
            Bundle b = intent.getBundleExtra("com.savoirfairelinux.sflphone.service.newcall");

            SipCall.SipCallBuilder callBuilder = SipCall.SipCallBuilder.getInstance();

            Account acc;
            try {
                HashMap<String, String> details = (HashMap<String, String>) mBinder.getAccountDetails(b.getString("AccountID"));
                ArrayList<HashMap<String, String>> credentials = (ArrayList<HashMap<String, String>>) mBinder
                        .getCredentials(b.getString("AccountID"));
                acc = new Account(b.getString("AccountID"), details, credentials);
                callBuilder.startCallCreation(b.getString("CallID")).setAccount(acc).setCallState(SipCall.state.CALL_STATE_RINGING)
                        .setCallType(SipCall.state.CALL_TYPE_INCOMING);
                callBuilder.setContact(CallContact.ContactBuilder.buildUnknownContact(b.getString("From")));

                Intent toSend = new Intent(CallManagerCallBack.INCOMING_CALL);
                toSend.setClass(callback, CallActivity.class);
                toSend.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                SipCall newCall = callBuilder.build();
                toSend.putExtra("newcall", newCall);
                HashMap<String, String> callDetails = (HashMap<String, String>) mBinder.getCallDetails(b.getString("CallID"));

                newCall.setTimestamp_start(Long.parseLong(callDetails.get(ServiceConstants.call.TIMESTAMP_START)));
                callback.getCurrent_calls().put(newCall.getCallId(), newCall);
//                callback.sendBroadcast(toSend);
                Bundle bundle = new Bundle();
                Conference tmp = new Conference("-1");

                tmp.getParticipants().add(newCall);

                bundle.putParcelable("conference", tmp);
                toSend.putExtra("resuming", false);
                toSend.putExtras(bundle);
                callback.startActivity(toSend);

                callback.mediaManager.obtainAudioFocus(true);
            } catch (RemoteException e1) {
                e1.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if (intent.getAction().contentEquals(CallManagerCallBack.CALL_STATE_CHANGED)) {

            Log.i(TAG, "Received " + intent.getAction());
            Bundle b = intent.getBundleExtra("com.savoirfairelinux.sflphone.service.newstate");
            String newState = b.getString("State");

            try {
                if (callback.getCurrent_calls().get(b.getString("CallID")) != null && mBinder.isConferenceParticipant(b.getString("CallID"))) {
                    callback.getCurrent_calls().remove(b.getString("CallID"));
                }
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }

            if (newState.equals("INCOMING")) {
                callback.getCurrent_calls().get(b.getString("CallID")).setCallState(SipCall.state.CALL_STATE_INCOMING);
            } else if (newState.equals("RINGING")) {
                try {
                    callback.getCurrent_calls().get(b.getString("CallID")).setCallState(SipCall.state.CALL_STATE_RINGING);
                } catch (NullPointerException e) {
                    if (callback.getCurrent_calls() == null) {

                        return;
                    }
                    if (callback.getCurrent_calls().get(b.getString("CallID")) == null) {
                        Log.e(TAG, "get(b.getString(callID)) null");
                        return;
                    }
                }

            } else if (newState.equals("CURRENT")) {
                if (callback.getCurrent_calls().get(b.getString("CallID")) != null) {
                    callback.getCurrent_calls().get(b.getString("CallID")).setCallState(SipCall.state.CALL_STATE_CURRENT);
                } else {
                    // Check if call is in a conference
                    Iterator<Entry<String, Conference>> it = callback.getCurrent_confs().entrySet().iterator();
                    while (it.hasNext()) {
                        Conference tmp = it.next().getValue();
                        for (SipCall c : tmp.getParticipants()) {
                            if (c.getCallId().contentEquals(b.getString("CallID")))
                                c.setCallState(SipCall.state.CALL_STATE_CURRENT);
                        }
                    }
                }

            } else if (newState.equals("HUNGUP")) {
                 
                if (callback.getCurrent_calls().get(b.getString("CallID")) != null) {
                    
                    if(callback.getCurrent_calls().get(b.getString("CallID")).isRinging())
                        callback.notificationManager.publishMissedCallNotification(callback.getCurrent_calls().get(b.getString("CallID")));
                    callback.getCurrent_calls().remove(b.getString("CallID"));
                } else {
                    ArrayList<Conference> it = new ArrayList<Conference>(callback.getCurrent_confs().values());

                    boolean found = false;
                    int i = 0;
                    while (!found && i < it.size()) {
                        Conference tmp = it.get(i);

                        for (int j = 0; j < tmp.getParticipants().size(); ++j) {
                            if (tmp.getParticipants().get(j).getCallId().contentEquals(b.getString("CallID"))) {
                                callback.getCurrent_confs().get(tmp.getId()).getParticipants().remove(tmp.getParticipants().get(j));
                                found = true;
                            }

                        }
                        ++i;

                    }
                }

                callback.sendBroadcast(intent);

            } else if (newState.equals("BUSY")) {
                callback.getCurrent_calls().remove(b.getString("CallID"));
            } else if (newState.equals("FAILURE")) {
                callback.getCurrent_calls().remove(b.getString("CallID"));
            } else if (newState.equals("HOLD")) {
                if (callback.getCurrent_calls().get(b.getString("CallID")) != null) {
                    callback.getCurrent_calls().get(b.getString("CallID")).setCallState(SipCall.state.CALL_STATE_HOLD);
                } else {
                    // Check if call is in a conference
                    Iterator<Entry<String, Conference>> it = callback.getCurrent_confs().entrySet().iterator();
                    while (it.hasNext()) {
                        Conference tmp = it.next().getValue();
                        for (SipCall c : tmp.getParticipants()) {
                            if (c.getCallId().contentEquals(b.getString("CallID")))
                                c.setCallState(SipCall.state.CALL_STATE_HOLD);
                        }
                    }
                }
            } else if (newState.equals("UNHOLD")) {

                if (callback.getCurrent_calls().get(b.getString("CallID")) != null) {
                    callback.getCurrent_calls().get(b.getString("CallID")).setCallState(SipCall.state.CALL_STATE_CURRENT);
                } else {
                    // Check if call is in a conference
                    Iterator<Entry<String, Conference>> it = callback.getCurrent_confs().entrySet().iterator();
                    while (it.hasNext()) {
                        Conference tmp = it.next().getValue();
                        for (SipCall c : tmp.getParticipants()) {
                            if (c.getCallId().contentEquals(b.getString("CallID")))
                                c.setCallState(SipCall.state.CALL_STATE_CURRENT);
                        }
                    }
                }
            } else {
                callback.getCurrent_calls().get(b.getString("CallID")).setCallState(SipCall.state.CALL_STATE_NONE);
            }

            callback.sendBroadcast(intent);

        } else if (intent.getAction().contentEquals(CallManagerCallBack.NEW_CALL_CREATED)) {

            Log.i(TAG, "Received" + intent.getAction());

        } else if (intent.getAction().contentEquals(CallManagerCallBack.CONF_CREATED)) {

            Log.i(TAG, "Received" + intent.getAction());
            Conference created = new Conference(intent.getStringExtra("confID"));

            try {
                ArrayList<String> all_participants = (ArrayList<String>) mBinder.getParticipantList(intent.getStringExtra("confID"));
                for (String participant : all_participants) {
                    created.getParticipants().add(callback.getCurrent_calls().get(participant));
                    callback.getCurrent_calls().remove(participant);
                }
                Intent toSend = new Intent(CallManagerCallBack.CONF_CREATED);
                toSend.putExtra("newconf", created);
                callback.getCurrent_confs().put(intent.getStringExtra("confID"), created);
                callback.sendBroadcast(toSend);
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
            Log.i(TAG, "current_confs size " + callback.getCurrent_confs().size());

        } else if (intent.getAction().contentEquals(CallManagerCallBack.CONF_REMOVED)) {

            Log.i(TAG, "Received" + intent.getAction());
            Conference toDestroy = callback.getCurrent_confs().get(intent.getStringExtra("confID"));
            for (int i = 0; i < toDestroy.getParticipants().size(); ++i) {
                callback.getCurrent_calls().put(toDestroy.getParticipants().get(i).getCallId(), toDestroy.getParticipants().get(i));
            }
            callback.getCurrent_confs().remove(intent.getStringExtra("confID"));
            callback.sendBroadcast(intent);

        } else if (intent.getAction().contentEquals(CallManagerCallBack.CONF_CHANGED)) {

            ArrayList<String> all_participants;
            try {
                all_participants = (ArrayList<String>) mBinder.getParticipantList(intent.getStringExtra("confID"));
                for (String participant : all_participants) {
                    if (callback.getCurrent_confs().get(intent.getStringExtra("confID")).getParticipants().size() < all_participants.size()
                            && callback.getCurrent_calls().get(participant) != null) { // We need to add the new participant to the conf
                        callback.getCurrent_confs().get(intent.getStringExtra("confID")).getParticipants()
                                .add(callback.getCurrent_calls().get(participant));
                        callback.getCurrent_calls().remove(participant);
                        callback.getCurrent_confs().get(intent.getStringExtra("confID")).setState(intent.getStringExtra("State"));
                        callback.sendBroadcast(intent);
                        return;
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            Log.i(TAG, "Received" + intent.getAction());
            if (callback.getCurrent_confs().get(intent.getStringExtra("confID")) != null) {

                callback.getCurrent_confs().get(intent.getStringExtra("confID")).setState(intent.getStringExtra("State"));
                callback.sendBroadcast(intent);
            }

        } else if (intent.getAction().contentEquals(CallManagerCallBack.RECORD_STATE_CHANGED)) {

            Log.i(TAG, "Received" + intent.getAction());

            // try {
            // if (callback.getCurrent_confs().get(intent.getStringExtra("id")) != null) {
            // callback.getCurrent_confs().get(intent.getStringExtra("id")).setRecording(mBinder.isRecording(intent.getStringExtra("id")));
            // } else if (callback.getCurrent_calls().get(intent.getStringExtra("id")) != null) {
            // callback.getCurrent_calls().get(intent.getStringExtra("id")).setRecording(mBinder.isRecording(intent.getStringExtra("id")));
            // } else {
            // // A call in a conference has been put on hold
            // Iterator<Conference> it = callback.getCurrent_confs().values().iterator();
            // while (it.hasNext()) {
            // Conference c = it.next();
            // if (c.getCall(intent.getStringExtra("id")) != null)
            // c.getCall(intent.getStringExtra("id")).setRecording(mBinder.isRecording(intent.getStringExtra("id")));
            // }
            // }
            // // Re sending the same intent to the app
            // callback.sendBroadcast(intent);
            // ;
            // } catch (RemoteException e) {
            // // TODO Auto-generated catch block
            // e.printStackTrace();
            // }

        }

    }
}
