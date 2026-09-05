/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
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
package cx.ring.views

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.WebView

/**
 * The WebView hosting the collaborative editor.
 *
 * Quill holds every DOM mutation back while the keyboard composes a word, from
 * compositionstart to compositionend, and only then emits the change the
 * editor pushes to the shared document. A predictive keyboard keeps a word in
 * composition until a space, a return or a suggestion being taken, so the other
 * editors would see this one type a word at a time.
 *
 * So the keyboard is asked to commit each character instead of composing.
 * TYPE_TEXT_FLAG_NO_SUGGESTIONS is the flag meant for that, but Samsung's
 * keyboard predicts and composes regardless of it; what every keyboard honours
 * is the visible-password variation. It costs the document predictive text,
 * and gains it being shared as it is written.
 */
class CollabEditorWebView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : WebView(context, attrs) {

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val connection = super.onCreateInputConnection(outAttrs) ?: return null
        val type = outAttrs.inputType
        if (type and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT) {
            // Keep the multi-line and capitalisation flags Chromium derived from
            // the page: only the variation and the suggestion flags change.
            outAttrs.inputType = (type
                and InputType.TYPE_MASK_VARIATION.inv()
                and InputType.TYPE_TEXT_FLAG_AUTO_CORRECT.inv()
                and InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE.inv()
                or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
        }
        return connection
    }
}
