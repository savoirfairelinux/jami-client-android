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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package net.jami.model;

import net.jami.utils.StringUtils;

import java.util.Map;

import ezvcard.Ezvcard;
import ezvcard.VCard;

public class TrustRequest {
    private static final String TAG = TrustRequest.class.getSimpleName();

    private final String mAccountId;
    private String mContactUsername = null;
    private final Uri mRequestUri;
    private String mConversationId;
    private VCard mVcard;
    private String mMessage;
    private final long mTimestamp;
    private boolean mUsernameResolved = false;

    public TrustRequest(String accountId, Uri uri, long received, String payload, String conversationId) {
        mAccountId = accountId;
        mRequestUri = uri;
        mConversationId = StringUtils.isEmpty(conversationId) ? null : conversationId;
        mTimestamp = received;
        mVcard = payload == null ? null : Ezvcard.parse(payload).first();
        mMessage = null;
    }

    public TrustRequest(String accountId, Map<String, String> info) {
        this(accountId, Uri.fromId(info.get("from")), Long.decode(info.get("received")) * 1000L, info.get("payload"), info.get("conversationId"));
    }

    public TrustRequest(String accountId, Uri contactUri, String conversationId) {
        mAccountId = accountId;
        mRequestUri = contactUri;
        mConversationId = conversationId;
        mTimestamp = 0;
    }

    public String getAccountId() {
        return mAccountId;
    }

    public Uri getUri() {
        return mRequestUri;
    }

    public String getConversationId() {
        return mConversationId;
    }

    public String getFullname() {
        String fullname = "";
        if (mVcard != null && mVcard.getFormattedName() != null) {
            fullname = mVcard.getFormattedName().getValue();
        }
        return fullname;
    }

    public String getDisplayname() {
        boolean hasUsername = mContactUsername != null && !mContactUsername.isEmpty();
        return hasUsername ? mContactUsername : mRequestUri.toString();
    }

    public boolean isNameResolved() {
        return mUsernameResolved;
    }

    public void setUsername(String username) {
        mContactUsername = username;
        mUsernameResolved = true;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public VCard getVCard() {
        return mVcard;
    }

    public void setVCard(VCard vcard) {
        mVcard = vcard;
    }

    public String getMessage() {
        return mMessage;
    }

    public void setMessage(String message) {
        mMessage = message;
    }

    public void setConversationId(String conversationId) {
        mConversationId = conversationId;
    }
}
