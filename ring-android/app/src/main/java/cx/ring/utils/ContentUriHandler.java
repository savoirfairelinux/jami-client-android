/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
package cx.ring.utils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.util.List;

import cx.ring.BuildConfig;
import cx.ring.fragments.ConversationFragment;

/**
 * This class distributes content uri used to pass along data in the app
 */
public class ContentUriHandler {

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID;
    public static final String AUTHORITY_FILES = AUTHORITY + ".file_provider";

    private static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    public static final Uri CONVERSATION_CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "conversation");
    public static final Uri ACCOUNTS_CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "accounts");
    public static final Uri CONTACT_CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "contact");

    private ContentUriHandler() {
        // hidden constructor
    }
}
