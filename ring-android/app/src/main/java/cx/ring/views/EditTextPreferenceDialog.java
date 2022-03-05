/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.views;

import android.os.Bundle;
import androidx.preference.EditTextPreferenceDialogFragmentCompat;

import android.view.View;
import android.widget.EditText;

public class EditTextPreferenceDialog extends EditTextPreferenceDialogFragmentCompat {
    private static final String ARG_TYPE = "inputType";

    public static EditTextPreferenceDialog newInstance(String key, int type) {
        final EditTextPreferenceDialog fragment = new EditTextPreferenceDialog();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        b.putInt(ARG_TYPE, type);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        EditText text = view.findViewById(android.R.id.edit);
        text.setInputType(getArguments().getInt(ARG_TYPE));
    }
}
