/*
 * Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 * Author: Pierre Duchemin <pierre.duchemin@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.utils

import android.view.KeyEvent

object MediaButtonsHelper {
    fun handleMediaKeyCode(keyCode: Int, mediaButtonsHelperCallback: MediaButtonsHelperCallback): Boolean {
        var isHandledKey = false
        when (keyCode) {
            KeyEvent.KEYCODE_CALL, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_HOME -> {
                mediaButtonsHelperCallback.positiveMediaButtonClicked()
                isHandledKey = true
            }
            KeyEvent.KEYCODE_ENDCALL, KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_MEDIA_STOP -> {
                mediaButtonsHelperCallback.negativeMediaButtonClicked()
                isHandledKey = true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> {
                mediaButtonsHelperCallback.toggleMediaButtonClicked()
                isHandledKey = true
            }
        }
        return isHandledKey
    }

    /**
     * Media buttons actions table:
     * <table>
     * <tr><th></th>                <th>positive btn</th>    <th>negative btn</th>	  <th>toggle btn</th></tr>
     * <tr><th>conversation</th>	   <td>redirect</td>       <td>redirect</td>	    <td>redirect</td></tr>
     * <tr><th>incoming call</th>      <td>accept</td>	       <td>refuse</td>          <td>/</td></tr>
     * <tr><th>outgoing call</th>      <td>hangup</td>         <td>hangup</td>	        <td>hangup</td></tr>
     * <tr><th>calling</th>	           <td>hangup</td>         <td>hangup</td>	        <td>hangup</td></tr>
    </table> *
     */
    interface MediaButtonsHelperCallback {
        fun positiveMediaButtonClicked()
        fun negativeMediaButtonClicked()
        fun toggleMediaButtonClicked()
    }
}