/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package net.jami.services;

import net.jami.model.Account;
import net.jami.model.Contact;
import net.jami.model.Conference;
import net.jami.model.Conversation;
import net.jami.model.DataTransfer;
import net.jami.model.Call;
import net.jami.model.Uri;

public interface NotificationService {
    String TRUST_REQUEST_NOTIFICATION_ACCOUNT_ID = "trustRequestNotificationAccountId";
    String TRUST_REQUEST_NOTIFICATION_FROM = "trustRequestNotificationFrom";
    String KEY_CALL_ID = "callId";
    String KEY_HOLD_ID = "holdId";
    String KEY_END_ID = "endId";
    String KEY_NOTIFICATION_ID = "notificationId";

    Object showCallNotification(int callId);
    void cancelCallNotification();
    void handleCallNotification(Conference conference, boolean remove);
    void showMissedCallNotification(Call call);

    void showTextNotification(String accountId, Conversation conversation);
    void cancelTextNotification(String accountId, Uri contact);

    void cancelAll();

    void showIncomingTrustRequestNotification(Account account);
    void cancelTrustRequestNotification(String accountID);

    void showFileTransferNotification(Conversation conversation, DataTransfer info);
    void cancelFileNotification(int id, boolean isMigratingToService);
    void handleDataTransferNotification(DataTransfer transfer, Conversation contact, boolean remove);
    void removeTransferNotification(String accountId, Uri conversationUri, String fileId);
    Object getDataTransferNotification(int notificationId);

    //void updateNotification(Object notification, int notificationId);

    Object getServiceNotification();

    void onConnectionUpdate(Boolean b);

    void showLocationNotification(Account first, Contact contact);
    void cancelLocationNotification(Account first, Contact contact);

}
