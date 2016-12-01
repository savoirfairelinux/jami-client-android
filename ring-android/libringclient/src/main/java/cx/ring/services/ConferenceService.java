/*
 *  Copyright (C) 2016 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.daemon.Ringservice;
import cx.ring.daemon.StringVect;
import cx.ring.model.DaemonEvent;
import cx.ring.utils.FutureUtils;
import cx.ring.utils.Log;

public class ConferenceService extends Observable {

    private final static String TAG = ConferenceService.class.getName();

    @Inject
    @Named("DaemonExecutor")
    ExecutorService mExecutor;

    private ConferenceCallbackHandler mCallbackHandler;

    public ConferenceService() {
        mCallbackHandler = new ConferenceCallbackHandler();
    }

    public ConferenceCallbackHandler getCallbackHandler() {
        return mCallbackHandler;
    }

    public void removeConference(final String confID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "createConference() thread running...");
                Ringservice.removeConference(confID);
            }
        });
    }

    public void joinParticipant(final String selCallID, final String dragCallID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "joinParticipant() thread running...");
                Ringservice.joinParticipant(selCallID, dragCallID);
                // Generate a CONF_CREATED callback
            }
        });
    }

    public void addParticipant(final String callID, final String confID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "addParticipant() thread running...");
                Ringservice.addParticipant(callID, confID);
            }
        });
    }

    public void addMainParticipant(final String confID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "addMainParticipant() thread running...");
                Ringservice.addMainParticipant(confID);
            }
        });

    }

    public void detachParticipant(final String callID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "detachParticipant() thread running... " + callID);
                Ringservice.detachParticipant(callID);
            }
        });
    }

    public void joinConference(final String selConfID, final String dragConfID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "joinConference() thread running...");
                Ringservice.joinConference(selConfID, dragConfID);
            }
        });
    }

    public void hangUpConference(final String confID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "hangUpConference() thread running...");
                Ringservice.hangUpConference(confID);
            }
        });
    }

    public void holdConference(final String confID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "holdConference() thread running...");
                Ringservice.holdConference(confID);
            }
        });
    }

    public void unholdConference(final String confID) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "unholdConference() thread running...");
                Ringservice.unholdConference(confID);
            }
        });
    }

    public boolean isConferenceParticipant(final String callID) {
        Future<Boolean> result = mExecutor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Log.i(TAG, "isConferenceParticipant() thread running...");
                return Ringservice.isConferenceParticipant(callID);
            }
        });

        return FutureUtils.getFutureResult(result);
    }

    public Map<String, ArrayList<String>> getConferenceList() {

        Future<Map<String, ArrayList<String>>> result = mExecutor.submit(new Callable<Map<String, ArrayList<String>>>() {
            @Override
            public Map<String, ArrayList<String>> call() throws Exception {
                Log.i(TAG, "DRingService.getConferenceList() thread running...");
                StringVect callIds = Ringservice.getCallList();
                HashMap<String, ArrayList<String>> confs = new HashMap<>(callIds.size());
                for (int i = 0; i < callIds.size(); i++) {
                    String callId = callIds.get(i);
                    String confId = Ringservice.getConferenceId(callId);
                    if (confId == null || confId.isEmpty()) {
                        confId = callId;
                    }
                    ArrayList<String> calls = confs.get(confId);
                    if (calls == null) {
                        calls = new ArrayList<>();
                        confs.put(confId, calls);
                    }
                    calls.add(callId);
                }
                return confs;
            }
        });

        return FutureUtils.getFutureResult(result);
    }

    public List<String> getParticipantList(final String confID) {

        Future<List<String>> result = mExecutor.submit(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                Log.i(TAG, "getParticipantList() thread running...");
                return new ArrayList<>(Ringservice.getParticipantList(confID));
            }
        });

        return FutureUtils.getFutureResult(result);
    }

    public String getConferenceId(String callID) {
        return Ringservice.getConferenceId(callID);
    }

    public String getConferenceDetails(final String callID) {

        Future<String> result = mExecutor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Log.i(TAG, "getConferenceDetails() thread running...");
                return Ringservice.getConferenceDetails(callID).get("CONF_STATE");
            }
        });

        return FutureUtils.getFutureResult(result);
    }

    public Map<String, String> getConference(final String id) {
        Future<Map<String, String>> result = mExecutor.submit(new Callable<Map<String, String>>() {
            @Override
            public Map<String, String> call() throws Exception {
                Log.i(TAG, "getCredentials() thread running...");
                return Ringservice.getConferenceDetails(id).toNative();
            }
        });

        return FutureUtils.getFutureResult(result);
    }

    class ConferenceCallbackHandler {

        void conferenceCreated(final String confId) {
            Log.d(TAG, "conference created: " + confId);
            setChanged();
            DaemonEvent event = new DaemonEvent(DaemonEvent.EventType.CONFERENCE_CREATED);
            event.addEventInput(DaemonEvent.EventInput.CONF_ID, confId);
            notifyObservers(event);
        }

        void conferenceRemoved(String confId) {
            Log.d(TAG, "conference removed: " + confId);
            setChanged();
            DaemonEvent event = new DaemonEvent(DaemonEvent.EventType.CONFERENCE_REMOVED);
            event.addEventInput(DaemonEvent.EventInput.CONF_ID, confId);
            notifyObservers(event);
        }

        void conferenceChanged(String confId, String state) {
            Log.d(TAG, "conference changed: " + confId + ", " + state);
            setChanged();
            DaemonEvent event = new DaemonEvent(DaemonEvent.EventType.CONFERENCE_CHANGED);
            event.addEventInput(DaemonEvent.EventInput.CONF_ID, confId);
            event.addEventInput(DaemonEvent.EventInput.STATE, state);
            notifyObservers(event);
        }
    }

}
