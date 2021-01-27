/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.views;

import android.content.Context;
import android.os.Build;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;
import androidx.appcompat.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

public class MessageEditText extends AppCompatEditText {
    static private final String[] SUPPORTED_MIME_TYPES = new String[]{"image/png", "image/jpg", "image/gif", "image/webp"};

    public interface MediaListener {
        void onMedia(InputContentInfoCompat mediaUri);
    }

    private MediaListener listener = null;

    public MessageEditText(Context context) {
        super(context);
    }

    public MessageEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setMediaListener(MediaListener l) {
        listener = l;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
        final InputConnection ic = super.onCreateInputConnection(editorInfo);
        EditorInfoCompat.setContentMimeTypes(editorInfo, SUPPORTED_MIME_TYPES);
        return InputConnectionCompat.createWrapper(ic, editorInfo, (inputContentInfo, flags, opts) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1
                    && (flags & InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0) {
                try {
                    inputContentInfo.requestPermission();
                } catch (Exception e) {
                    return false;
                }
            }
            if (listener != null)
                listener.onMedia(inputContentInfo);
            return true;
        });
    }
}
