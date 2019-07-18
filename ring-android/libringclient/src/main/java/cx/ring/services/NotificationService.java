/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.services;

import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.DataTransfer;
import cx.ring.model.SipCall;
import cx.ring.model.Uri;

public interface NotificationService {
    String TRUST_REQUEST_NOTIFICATION_ACCOUNT_ID = "trustRequestNotificationAccountId";
    String TRUST_REQUEST_NOTIFICATION_FROM = "trustRequestNotificationFrom";
    String KEY_CALL_ID = "callId";

    Object[] showCallNotification();

    void updateNotification(Object[] notificationData);

    void startForegroundService();

    void stopForegroundService();

    void showTextNotification(String accountId, Conversation conversation);

    void cancelCallNotification(int notificationId);

    void cancelTextNotification(Uri contact);

    void cancelTextNotification(String ringId);

    void cancelAll();

    void showIncomingTrustRequestNotification(Account account);

    void cancelTrustRequestNotification(String accountID);

    void showFileTransferNotification(DataTransfer info, CallContact contact);

    void showMissedCallNotification(SipCall call);

    void cancelFileNotification(long id);

    void setConference(Conference conference);

    Object getServiceNotification();
}
