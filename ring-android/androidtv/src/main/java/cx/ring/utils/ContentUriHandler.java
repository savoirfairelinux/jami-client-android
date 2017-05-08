/**
 * Copyright (C) 2016 by Savoir-faire Linux
 * Author : Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 * <p>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.utils;

import android.net.Uri;

import cx.ring.BuildConfig;

/**
 * This class distributes content uri used to pass along data in the app
 */
public class ContentUriHandler {

    private static final Uri AUTHORITY_URI = Uri.parse("content://" + BuildConfig.APPLICATION_ID);

    public static final Uri CONFERENCE_CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "conferences");
    public static final Uri CALL_CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "calls");
    public static final Uri CONVERSATION_CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "conversations");
    public static final Uri ACCOUNTS_CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "accounts");

    private ContentUriHandler() {
        // hidden constructor
    }
}
