package cx.ring.utils;


import android.view.KeyEvent;

public class MediaButtonsHelper {

    public static void handleMediaKeyCode(int keyCode, MediaButtonsHelperCallback mediaButtonsHelperCallback) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CALL:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_HOME:
                mediaButtonsHelperCallback.positiveButtonClicked();
                break;
            case KeyEvent.KEYCODE_ENDCALL:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_DEL:
            case KeyEvent.KEYCODE_BUTTON_B:
            case KeyEvent.KEYCODE_MEDIA_STOP:
                mediaButtonsHelperCallback.negativeButtonClicked();
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                mediaButtonsHelperCallback.toggleButtonClicked();
                break;
        }
    }

    public interface MediaButtonsHelperCallback {
        void positiveButtonClicked();
        void negativeButtonClicked();
        void toggleButtonClicked();
    }
}
