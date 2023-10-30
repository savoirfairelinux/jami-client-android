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
package cx.ring.views

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.preference.EditTextPreferenceDialogFragmentCompat

class EditTextPreferenceDialog : EditTextPreferenceDialogFragmentCompat() {
    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        val text = view.findViewById<EditText>(android.R.id.edit)
        text.inputType = requireArguments().getInt(ARG_TYPE)
    }

    companion object {
        private const val ARG_TYPE = "inputType"
        fun newInstance(key: String?, type: Int): EditTextPreferenceDialog {
            val fragment = EditTextPreferenceDialog()
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            b.putInt(ARG_TYPE, type)
            fragment.arguments = b
            return fragment
        }
    }
}
