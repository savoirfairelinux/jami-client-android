/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package cx.ring.services;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.daemon.PresenceCallback;
import cx.ring.daemon.Ringservice;
import cx.ring.daemon.StringVect;
import cx.ring.daemon.VectMap;
import cx.ring.model.ServiceEvent;
import cx.ring.utils.FutureUtils;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;

public class PresenceService extends Observable {
    private static final String TAG = PresenceService.class.getName();

    @Inject
    @Named("DaemonExecutor")
    ExecutorService mExecutor;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    private PresenceCallbackHandler mCallbackHandler;

    public PresenceService() {
        mCallbackHandler = new PresenceCallbackHandler();
    }

    public PresenceCallbackHandler getCallbackHandler() {
        return mCallbackHandler;
    }

    public void publish(final String accountID, final boolean status, final String note) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Ringservice.publish(accountID, status, note);
                        return true;
                    }
                }
        );
    }

    public void answerServerRequest(final String uri, final boolean flag) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Ringservice.answerServerRequest(uri, flag);
                        return true;
                    }
                }
        );
    }

    public void subscribeBuddy(final String accountID, final String uri, final boolean flag) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Ringservice.subscribeBuddy(accountID, uri, flag);
                        return true;
                    }
                }
        );
    }

    public VectMap getSubscriptions(final String accountID) {
        return FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                true,
                new Callable<VectMap>() {
                    @Override
                    public VectMap call() throws Exception {
                        return Ringservice.getSubscriptions(accountID);
                    }
                }
        );
    }

    public void setSubscriptions(final String accountID, final StringVect uris) {
        FutureUtils.executeDaemonThreadCallable(
                mExecutor,
                mDeviceRuntimeService.provideDaemonThreadId(),
                false,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Ringservice.setSubscriptions(accountID, uris);
                        return true;
                    }
                }
        );
    }

    class PresenceCallbackHandler extends PresenceCallback {

        @Override
        public void newServerSubscriptionRequest(String remote) {
            Log.d(TAG, "newServerSubscriptionRequest: " + remote);

            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.NEW_SERVER_SUBSCRIPTION_REQUEST);
            event.addEventInput(ServiceEvent.EventInput.REMOTE, remote);
            notifyObservers(event);
        }

        @Override
        public void serverError(String accountId, String error, String message) {
            Log.d(TAG, "serverError: " + accountId + ", " + error + ", " + message);

            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.SERVER_ERROR);
            event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
            event.addEventInput(ServiceEvent.EventInput.ERROR, error);
            event.addEventInput(ServiceEvent.EventInput.MESSAGE, message);
            notifyObservers(event);
        }

        @Override
        public void newBuddyNotification(String accountId, String buddyUri, int status, String lineStatus) {
            Log.d(TAG, "newBuddyNotification: " + accountId + ", " + buddyUri + ", " + status + ", " + lineStatus);

            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.NEW_BUDDY_NOTIFICATION);
            event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
            event.addEventInput(ServiceEvent.EventInput.BUDDY_URI, buddyUri);
            event.addEventInput(ServiceEvent.EventInput.STATE, status);
            event.addEventInput(ServiceEvent.EventInput.LINE_STATE, lineStatus);
            notifyObservers(event);
        }

        @Override
        public void subscriptionStateChanged(String accountId, String buddyUri, int state) {
            Log.d(TAG, "subscriptionStateChanged: " + accountId + ", " + buddyUri + ", " + state);

            setChanged();
            ServiceEvent event = new ServiceEvent(ServiceEvent.EventType.SUBSCRIPTION_STATE_CHANGED);
            event.addEventInput(ServiceEvent.EventInput.ACCOUNT_ID, accountId);
            event.addEventInput(ServiceEvent.EventInput.BUDDY_URI, buddyUri);
            event.addEventInput(ServiceEvent.EventInput.STATE, state);
            notifyObservers(event);
        }
    }
}
