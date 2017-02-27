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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.daemon.Ringservice;
import cx.ring.daemon.StringVect;
import cx.ring.model.ServiceEvent;
import cx.ring.utils.FutureUtils;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;

public class ConferenceService extends Observable {

    private final static String TAG = ConferenceService.class.getName();

    @Inject
    @Named("DaemonExecutor")
    ExecutorService mExecutor;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    private ConferenceCallbackHandler mCallbackHandler;

    public ConferenceService() {
        mCallbackHandler = new ConferenceCallbackHandler();
    }

    public ConferenceCallbackHandler getCallbackHandler() {
        return mCallbackHandler;
    }

    public void removeConference(final String confId) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "removeConference() thread running...");
                        Ringservice.removeConference(confId);
                        return true;
                    }
                }
        );
    }

    public void joinParticipant(final String selCallId, final String dragCallId) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "joinParticipant() thread running...");
                        Ringservice.joinParticipant(selCallId, dragCallId);
                        // Generate a CONF_CREATED callback
                        return true;
                    }
                }
        );
    }

    public void addParticipant(final String callId, final String confId) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "addParticipant() thread running...");
                        Ringservice.addParticipant(callId, confId);
                        return true;
                    }
                }
        );
    }

    public void addMainParticipant(final String confId) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "addMainParticipant() thread running...");
                        Ringservice.addMainParticipant(confId);
                        return true;
                    }
                }
        );
    }

    public void detachParticipant(final String callId) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "detachParticipant() thread running... " + callId);
                        Ringservice.detachParticipant(callId);
                        return true;
                    }
                }
        );
    }

    public void joinConference(final String selConfId, final String dragConfId) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "joinConference() thread running...");
                        Ringservice.joinConference(selConfId, dragConfId);
                        return true;
                    }
                }
        );
    }

    public void hangUpConference(final String confId) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "hangUpConference() thread running...");
                        Ringservice.hangUpConference(confId);
                        return true;
                    }
                }
        );
    }

    public void holdConference(final String confId) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "holdConference() thread running...");
                        Ringservice.holdConference(confId);
                        return true;
                    }
                }
        );
    }

    public void unholdConference(final String confId) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "unholdConference() thread running...");
                        Ringservice.unholdConference(confId);
                        return true;
                    }
                }
        );
    }

    @SuppressWarnings("ConstantConditions")
    public boolean isConferenceParticipant(final String callId) {
        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Log.i(TAG, "isConferenceParticipant() thread running...");
                        return Ringservice.isConferenceParticipant(callId);
                    }
                }
        );
    }

    public Map<String, ArrayList<String>> getConferenceList() {
        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<Map<String, ArrayList<String>>>() {
                    @Override
                    public Map<String, ArrayList<String>> call() throws Exception {
                        Log.i(TAG, "getConferenceList() thread running...");
                        StringVect callIds = Ringservice.getCallList();
                        HashMap<String, ArrayList<String>> confs = new HashMap<>(callIds.size());
                        for (int i = 0; i < callIds.size(); i++) {
                            String callId = callIds.get(i);
                            String confId = Ringservice.getConferenceId(callId);

                            Map<String, String> callDetails = Ringservice.getCallDetails(callId).toNative();

                            //todo remove condition when callDetails does not contains sips ids anymore
                            if(!callDetails.get("PEER_NUMBER").contains("sips")) {
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
                        }
                        return confs;
                    }
                }
        );
    }

    public List<String> getParticipantList(final String confId) {
        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<List<String>>() {
                    @Override
                    public List<String> call() throws Exception {
                        Log.i(TAG, "getParticipantList() thread running...");
                        return new ArrayList<>(Ringservice.getParticipantList(confId));
                    }
                }
        );
    }

    public String getConferenceId(String callId) {
        return Ringservice.getConferenceId(callId);
    }

    public String getConferenceDetails(final String callId) {
        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        Log.i(TAG, "getConferenceDetails() thread running...");
                        return Ringservice.getConferenceDetails(callId).get("CONF_STATE");
                    }
                }
        );
    }

    public Map<String, String> getConference(final String id) {
        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<Map<String, String>>() {
                    @Override
                    public Map<String, String> call() throws Exception {
                        Log.i(TAG, "getCredentials() thread running...");
                        return Ringservice.getConferenceDetails(id).toNative();
                    }
                }
        );
    }

    class ConferenceCallbackHandler {

        void conferenceCreated(final String confId) {
            Log.d(TAG, "conference created: " + confId);
            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.CONFERENCE_CREATED);
            event.addEventInput(ServiceEvent.EventInput.CONF_ID, confId);
            notifyObservers(event);
        }

        void conferenceRemoved(String confId) {
            Log.d(TAG, "conference removed: " + confId);
            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.CONFERENCE_REMOVED);
            event.addEventInput(ServiceEvent.EventInput.CONF_ID, confId);
            notifyObservers(event);
        }

        void conferenceChanged(String confId, String state) {
            Log.d(TAG, "conference changed: " + confId + ", " + state);
            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.CONFERENCE_CHANGED);
            event.addEventInput(ServiceEvent.EventInput.CONF_ID, confId);
            event.addEventInput(ServiceEvent.EventInput.STATE, state);
            notifyObservers(event);
        }
    }

}
