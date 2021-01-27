/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Rayan Osseiran <rayan.osseiran@savoirfairelinux.com>
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package net.jami.model;

public class TextMessage extends Interaction {

    private boolean mNotified;

    public TextMessage(String author, String account, String daemonId, ConversationHistory conversation, String message) {
        mAuthor = author;
        mAccount = account;
        if (daemonId != null) {
            try {
                mDaemonId = Long.parseLong(daemonId);
            } catch (NumberFormatException e) {
                try {
                    mDaemonId = Long.parseLong(daemonId, 16);
                } catch (NumberFormatException e2) {
                     mDaemonId = 0L;
                }
            }
        }
        mTimestamp = System.currentTimeMillis();
        mType = InteractionType.TEXT.toString();
        mConversation = conversation;
        mIsIncoming = author != null;
        mBody = message;
    }

    public TextMessage(String author, String account, long timestamp, ConversationHistory conversation, String message, boolean isIncoming) {
        mAuthor = author;
        mAccount = account;
        mTimestamp = timestamp;
        mType = InteractionType.TEXT.toString();
        mConversation = conversation;
        mIsIncoming = isIncoming;
        mBody = message;
    }

    public TextMessage(Interaction interaction) {
        mId = interaction.getId();
        mAuthor = interaction.getAuthor();
        mTimestamp = interaction.getTimestamp();
        mType = interaction.getType().toString();
        mStatus = interaction.getStatus().toString();
        mConversation = interaction.getConversation();
        mIsIncoming = mAuthor != null;
        mDaemonId = interaction.getDaemonId();
        mBody = interaction.getBody();
        mIsRead = interaction.isRead() ? 1 : 0;
        mAccount = interaction.getAccount();
        mContact = interaction.getContact();
    }

    public boolean isNotified() {
        return mNotified;
    }

    public void setNotified(boolean notified) {
        mNotified = notified;
    }

    public void setStatus(int status) {
        if (status == 3)
            mIsRead = 1;

        mStatus = InteractionStatus.fromIntTextMessage(status).toString();
    }


}
