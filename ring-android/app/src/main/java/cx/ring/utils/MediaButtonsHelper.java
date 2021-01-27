/*
 * Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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

package cx.ring.utils;

import android.view.KeyEvent;

public class MediaButtonsHelper {

    public static boolean handleMediaKeyCode(int keyCode, MediaButtonsHelperCallback mediaButtonsHelperCallback) {
        boolean isHandledKey = false;
        switch (keyCode) {
            case KeyEvent.KEYCODE_CALL:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_HOME:
                mediaButtonsHelperCallback.positiveMediaButtonClicked();
                isHandledKey = true;
                break;
            case KeyEvent.KEYCODE_ENDCALL:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_DEL:
            case KeyEvent.KEYCODE_BUTTON_B:
            case KeyEvent.KEYCODE_MEDIA_STOP:
                mediaButtonsHelperCallback.negativeMediaButtonClicked();
                isHandledKey = true;
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_HEADSETHOOK:
                mediaButtonsHelperCallback.toggleMediaButtonClicked();
                isHandledKey = true;
                break;
        }
        return isHandledKey;
    }

    /**
     * Media buttons actions table:
     * <table>
     * <tr><th></th>                <th>positive btn</th>    <th>negative btn</th>	  <th>toggle btn</th></tr>
     * <tr><th>conversation</th>	   <td>redirect</td>       <td>redirect</td>	    <td>redirect</td></tr>
     * <tr><th>incoming call</th>      <td>accept</td>	       <td>refuse</td>          <td>/</td></tr>
     * <tr><th>outgoing call</th>      <td>hangup</td>         <td>hangup</td>	        <td>hangup</td></tr>
     * <tr><th>calling</th>	           <td>hangup</td>         <td>hangup</td>	        <td>hangup</td></tr>
     * </table>
     */

    public interface MediaButtonsHelperCallback {
        void positiveMediaButtonClicked();

        void negativeMediaButtonClicked();

        void toggleMediaButtonClicked();
    }
}
