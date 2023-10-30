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

import android.view.KeyEvent

object MediaButtonsHelper {
    fun handleMediaKeyCode(keyCode: Int, mediaButtonsHelperCallback: MediaButtonsHelperCallback): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_CALL, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_HOME -> {
            mediaButtonsHelperCallback.positiveMediaButtonClicked()
            true
        }
        KeyEvent.KEYCODE_ENDCALL, KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_MEDIA_STOP -> {
            mediaButtonsHelperCallback.negativeMediaButtonClicked()
            true
        }
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> {
            mediaButtonsHelperCallback.toggleMediaButtonClicked()
            true
        }
        else -> false
    }

    interface MediaButtonsHelperCallback {
        fun positiveMediaButtonClicked()
        fun negativeMediaButtonClicked()
        fun toggleMediaButtonClicked()
    }
}