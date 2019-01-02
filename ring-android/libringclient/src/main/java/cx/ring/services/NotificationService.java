/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.services;

import java.util.TreeMap;

import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Conference;
import cx.ring.model.Conversation;
import cx.ring.model.DataTransfer;
import cx.ring.model.DataTransferEventCode;
import cx.ring.model.SipCall;
import cx.ring.model.TextMessage;
import cx.ring.model.Uri;

public abstract class NotificationService {

    public static final String TRUST_REQUEST_NOTIFICATION_ACCOUNT_ID = "trustRequestNotificationAccountId";
    public static final String TRUST_REQUEST_NOTIFICATION_FROM = "trustRequestNotificationFrom";

    public static final String KEY_CALL_ID = "callId";

    public abstract void showCallNotification(Conference conference);

    public abstract void showTextNotification(String accountId, Conversation conversation);

    public abstract void cancelCallNotification(int notificationId);

    public abstract void cancelTextNotification(Uri contact);

    public abstract void cancelTextNotification(String ringId);

    public abstract void cancelAll();

    public abstract void showIncomingTrustRequestNotification(Account account);

    public abstract void cancelTrustRequestNotification(String accountID);

    public abstract void showFileTransferNotification(DataTransfer info, CallContact contact);

    public abstract void showMissedCallNotification(SipCall call);

    public abstract void cancelFileNotification(long id);
}
