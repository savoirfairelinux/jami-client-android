/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class KeyboardVisibilityManager {
    public final static String TAG = KeyboardVisibilityManager.class.getSimpleName();

    public static void showKeyboard(final Activity activity,
                                    final View viewToFocus,
                                    final int tag) {
        if (null == activity) {
            Log.d(TAG, "showKeyboard: no activity");
            return;
        }

        if (null == viewToFocus) {
            Log.d(TAG, "showKeyboard: no viewToFocus");
            return;
        }

        Log.d(TAG, "showKeyboard: showing keyboard");
        viewToFocus.requestFocus();
        InputMethodManager imm = (InputMethodManager)
                activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(viewToFocus, tag);
    }

    @SuppressWarnings("unused")
    public static void hideKeyboard(final Activity activity,
                                    final int tag) {
        if (null == activity) {
            Log.d(TAG, "hideKeyboard: no activity");
            return;
        }

        final View currentFocus = activity.getCurrentFocus();
        if (currentFocus != null) {
            Log.d(TAG, "hideKeyboard: hiding keyboard");
            currentFocus.clearFocus();
            InputMethodManager inputManager =
                    (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(
                    currentFocus.getWindowToken(),
                    tag
            );
        }
    }
}
