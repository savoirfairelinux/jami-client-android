/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager

object KeyboardVisibilityManager {
    val TAG = KeyboardVisibilityManager::class.simpleName!!
    fun showKeyboard(viewToFocus: View?) {
        if (null == viewToFocus) {
            Log.d(TAG, "showKeyboard: no viewToFocus")
            return
        }
        Log.d(TAG, "showKeyboard: showing keyboard")
        viewToFocus.requestFocus()
        val imm = viewToFocus.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(viewToFocus, InputMethodManager.SHOW_IMPLICIT)
    }

    fun hideKeyboard(activity: Activity?) {
        if (null == activity) {
            Log.d(TAG, "hideKeyboard: no activity")
            return
        }
        val currentFocus = activity.currentFocus
        if (currentFocus != null) {
            Log.d(TAG, "hideKeyboard: hiding keyboard")
            currentFocus.clearFocus()
            val inputManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }
    }
}