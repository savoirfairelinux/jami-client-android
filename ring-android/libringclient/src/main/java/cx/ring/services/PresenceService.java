/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.daemon.Ringservice;
import cx.ring.daemon.StringVect;
import cx.ring.daemon.VectMap;
import cx.ring.model.CallContact;
import cx.ring.model.Uri;
import cx.ring.utils.Log;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class PresenceService {
    private static final String TAG = PresenceService.class.getSimpleName();

    @Inject
    @Named("DaemonExecutor")
    ScheduledExecutorService mExecutor;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    @Inject
    ContactService mContactService;

    private final Map<String, Boolean> mPresenceMap = new HashMap<>();
    private final PublishSubject<CallContact> presentSubject = PublishSubject.create();

    public Observable<CallContact> getPresenceUpdates() {
        return presentSubject;
    }

    /**
     * Check service cache for latest presence value
     *
     * @param uri URI of the contact
     * @return true if this URI is online according to latest daemon update
     */
    public boolean isBuddyOnline(String uri) {
        if (uri != null && mPresenceMap.containsKey(uri)) {
            return mPresenceMap.get(uri);
        }
        return false;
    }

    public void publish(final String accountID, final boolean status, final String note) {
        mExecutor.execute(() -> Ringservice.publish(accountID, status, note));
    }

    public void answerServerRequest(final String uri, final boolean flag) {
        mExecutor.execute(() -> Ringservice.answerServerRequest(uri, flag));
    }

    public void subscribeBuddy(final String accountID, final String uri, final boolean flag) {
        mExecutor.execute(() -> Ringservice.subscribeBuddy(accountID, uri, flag));
    }

    public VectMap getSubscriptions(final String accountID) {
        try {
            return mExecutor.submit(() -> Ringservice.getSubscriptions(accountID)).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setSubscriptions(final String accountID, final StringVect uris) {
        mExecutor.execute(() -> Ringservice.setSubscriptions(accountID, uris));
    }

    public void newServerSubscriptionRequest(String remote) {
        Log.d(TAG, "newServerSubscriptionRequest: " + remote);
    }

    public void serverError(String accountId, String error, String message) {
        Log.d(TAG, "serverError: " + accountId + ", " + error + ", " + message);
    }

    public void newBuddyNotification(String accountId, String buddyUri, int status, String lineStatus) {
        Log.d(TAG, "newBuddyNotification: " + accountId + ", " + buddyUri + ", " + status + ", " + lineStatus);
        mPresenceMap.put(CallContact.PREFIX_RING + buddyUri, status == 1);

        CallContact contact = mContactService.findContact(accountId, new Uri(CallContact.PREFIX_RING + buddyUri));
        contact.setOnline(status == 1);
        presentSubject.onNext(contact);
    }

    public void subscriptionStateChanged(String accountId, String buddyUri, int state) {
        Log.d(TAG, "subscriptionStateChanged: " + accountId + ", " + buddyUri + ", " + state);
    }
}

