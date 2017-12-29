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
                mediaButtonsHelperCallback.positiveButtonClicked();
                isHandledKey = true;
                break;
            case KeyEvent.KEYCODE_ENDCALL:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_DEL:
            case KeyEvent.KEYCODE_BUTTON_B:
            case KeyEvent.KEYCODE_MEDIA_STOP:
                mediaButtonsHelperCallback.negativeButtonClicked();
                isHandledKey = true;
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_HEADSETHOOK:
                mediaButtonsHelperCallback.toggleButtonClicked();
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
        void positiveButtonClicked();

        void negativeButtonClicked();

        void toggleButtonClicked();
    }
}
