/*
 *  Copyright (C) 2015-2016 Savoir-faire Linux Inc.
 *
 *  Author: Romain Bertozzi <romain.bertozzi@savoirfairelinux.com>
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

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import cx.ring.BuildConfig;

public class ClipboardHelper {
    public static final String TAG = ClipboardHelper.class.getSimpleName();
    public static final String COPY_CALL_CONTACT_NUMBER_CLIP_LABEL =
            BuildConfig.APPLICATION_ID + ".clipboard.contactNumber";

    public interface ClipboardHelperCallback {
        void clipBoardDidCopyNumber(String copiedNumber);
    }

    public static void copyNumberToClipboard(final Activity activity,
                                             final String number,
                                             final ClipboardHelperCallback callback) {
        if (TextUtils.isEmpty(number)) {
            Log.d(TAG, "copyNumberToClipboard: number is null");
            return;
        }

        if (activity == null) {
            Log.d(TAG, "copyNumberToClipboard: activity is null");
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) activity
                .getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = android.content.ClipData.newPlainText(COPY_CALL_CONTACT_NUMBER_CLIP_LABEL,
                number);
        clipboard.setPrimaryClip(clip);
        if (callback != null) {
            callback.clipBoardDidCopyNumber(number);
        }
    }
}
