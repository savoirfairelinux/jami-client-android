/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package org.sflphone.views;

/*
 * Copyright (C) 2011 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import org.sflphone.R;

import java.lang.reflect.Field;

/*
 * @author Danesh
 * @author nebkat
 */

public class QuadNumberPickerPreference extends DialogPreference {
    private int mMin1, mMax1, mDefault1;
    private int mMin2, mMax2, mDefault2;
    private int mMin3, mMax3, mDefault3;
    private int mMin4, mMax4, mDefault4;

    private String mPickerTitle1;
    private String mPickerTitle2;
    private String mPickerTitle3;
    private String mPickerTitle4;

    private NumberPicker mNumberPicker1;
    private NumberPicker mNumberPicker2;
    private NumberPicker mNumberPicker3;
    private NumberPicker mNumberPicker4;

    public QuadNumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TypedArray dialogType = context.obtainStyledAttributes(attrs,
        // com.android.internal.R.styleable.DialogPreference, 0, 0);
        TypedArray doubleNumberPickerType = context.obtainStyledAttributes(attrs, R.styleable.QuadNumberPickerPreference, 0, 0);

        mPickerTitle1 = doubleNumberPickerType.getString(R.styleable.QuadNumberPickerPreference_pickerTitle1);
        mPickerTitle2 = doubleNumberPickerType.getString(R.styleable.QuadNumberPickerPreference_pickerTitle2);
        mPickerTitle3 = doubleNumberPickerType.getString(R.styleable.QuadNumberPickerPreference_pickerTitle3);
        mPickerTitle4 = doubleNumberPickerType.getString(R.styleable.QuadNumberPickerPreference_pickerTitle4);


        mMax1 = doubleNumberPickerType.getInt(R.styleable.QuadNumberPickerPreference_max1, 5);
        mMin1 = doubleNumberPickerType.getInt(R.styleable.QuadNumberPickerPreference_min1, 0);
        mMax2 = doubleNumberPickerType.getInt(R.styleable.QuadNumberPickerPreference_max2, 5);
        mMin2 = doubleNumberPickerType.getInt(R.styleable.QuadNumberPickerPreference_min2, 0);
        mMax3 = doubleNumberPickerType.getInt(R.styleable.QuadNumberPickerPreference_max3, 5);
        mMin3 = doubleNumberPickerType.getInt(R.styleable.QuadNumberPickerPreference_min3, 0);
        mMax4 = doubleNumberPickerType.getInt(R.styleable.QuadNumberPickerPreference_max4, 5);
        mMin4 = doubleNumberPickerType.getInt(R.styleable.QuadNumberPickerPreference_min4, 0);


        mDefault1 = doubleNumberPickerType.getInt(R.styleable.QuadNumberPickerPreference_defaultValue1, mMin1);
        mDefault2 = doubleNumberPickerType.getInt(R.styleable.QuadNumberPickerPreference_defaultValue2, mMin2);
        mDefault3 = doubleNumberPickerType.getInt(R.styleable.QuadNumberPickerPreference_defaultValue3, mMin3);
        mDefault4 = doubleNumberPickerType.getInt(R.styleable.QuadNumberPickerPreference_defaultValue4, mMin4);

        // dialogType.recycle();
        doubleNumberPickerType.recycle();
    }

    @Override
    protected View onCreateDialogView() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.quad_number_picker_dialog, null);

        mNumberPicker1 = (NumberPicker) view.findViewById(R.id.number_picker_1);
        mNumberPicker2 = (NumberPicker) view.findViewById(R.id.number_picker_2);
        mNumberPicker3 = (NumberPicker) view.findViewById(R.id.number_picker_3);
        mNumberPicker4 = (NumberPicker) view.findViewById(R.id.number_picker_4);

        if (mNumberPicker1 == null || mNumberPicker2 == null || mNumberPicker3 == null || mNumberPicker4 == null) {
            throw new RuntimeException("mNumberPicker1 or mNumberPicker2 is null!");
        }

        // Initialize state
        mNumberPicker1.setWrapSelectorWheel(false);
        mNumberPicker1.setMaxValue(mMax1);
        mNumberPicker1.setMinValue(mMin1);
        mNumberPicker1.setValue(getPersistedValue(mDefault1));
        mNumberPicker2.setWrapSelectorWheel(false);
        mNumberPicker2.setMaxValue(mMax2);
        mNumberPicker2.setMinValue(mMin2);
        mNumberPicker1.setValue(getPersistedValue(mDefault2));
        mNumberPicker3.setWrapSelectorWheel(false);
        mNumberPicker3.setMaxValue(mMax3);
        mNumberPicker3.setMinValue(mMin3);
        mNumberPicker1.setValue(getPersistedValue(mDefault3));
        mNumberPicker4.setWrapSelectorWheel(false);
        mNumberPicker4.setMaxValue(mMax4);
        mNumberPicker4.setMinValue(mMin4);
        mNumberPicker1.setValue(getPersistedValue(mDefault4));

        // Titles
        TextView pickerTitle1 = (TextView) view.findViewById(R.id.picker_title_1);
        TextView pickerTitle2 = (TextView) view.findViewById(R.id.picker_title_2);
        TextView pickerTitle3 = (TextView) view.findViewById(R.id.picker_title_3);
        TextView pickerTitle4 = (TextView) view.findViewById(R.id.picker_title_4);

        if (pickerTitle1 != null && pickerTitle2 != null) {
            pickerTitle1.setText(mPickerTitle1);
            pickerTitle2.setText(mPickerTitle2);
            pickerTitle3.setText(mPickerTitle3);
            pickerTitle4.setText(mPickerTitle4);
        }

        // No keyboard popup
        disableTextInput(mNumberPicker1);
        disableTextInput(mNumberPicker2);
        disableTextInput(mNumberPicker3);
        disableTextInput(mNumberPicker4);
        return view;
    }

    private int getPersistedValue(int value) {
        String[] values = getPersistedString(mDefault1 + "|" + mDefault2 + "|" + mDefault3 + "|" + mDefault4).split("\\|");
        if (value == 1) {
            try {
                return Integer.parseInt(values[0]);
            } catch (NumberFormatException e) {
                return mDefault1;
            }
        } else {
            try {
                return Integer.parseInt(values[1]);
            } catch (NumberFormatException e) {
                return mDefault2;
            }
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            persistString(mNumberPicker1.getValue() + "|" + mNumberPicker2.getValue() + "|" + mNumberPicker3.getValue() + "|" + mNumberPicker4.getValue());
            getOnPreferenceChangeListener().onPreferenceChange(this, mNumberPicker1.getValue() + "" + mNumberPicker2.getValue() + "" + mNumberPicker3.getValue() + "" + mNumberPicker4.getValue());
        }
    }

    /*
     * reflection of NumberPicker.java verified in 4.1, 4.2
     */
    private void disableTextInput(NumberPicker np) {
        if (np == null)
            return;
        Class<?> classType = np.getClass();
        Field inputTextField;
        try {
            inputTextField = classType.getDeclaredField("mInputText");
            inputTextField.setAccessible(true);
            EditText textInput = (EditText) inputTextField.get(np);
            if (textInput != null) {
                textInput.setCursorVisible(false);
                textInput.setFocusable(false);
                textInput.setFocusableInTouchMode(false);
            }
        } catch (Exception e) {
            Log.d("QuadNumberPicker", "disableTextInput error", e);
        }
    }

}
