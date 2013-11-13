package org.sflphone.views;

import java.lang.reflect.Field;

import org.sflphone.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;

public class NumberPickerPreference extends DialogPreference {
    private int mMin, mMax, mDefault;

    private String mMaxExternalKey, mMinExternalKey;

    private NumberPicker mNumberPicker;

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TypedArray dialogType = context.obtainStyledAttributes(attrs,
        // com.android.internal.R.styleable.DialogPreference, 0, 0);
        TypedArray numberPickerType = context.obtainStyledAttributes(attrs, R.styleable.NumberPickerPreference, 0, 0);

        mMaxExternalKey = numberPickerType.getString(R.styleable.NumberPickerPreference_maxExternal);
        mMinExternalKey = numberPickerType.getString(R.styleable.NumberPickerPreference_minExternal);

        mMax = numberPickerType.getInt(R.styleable.NumberPickerPreference_max, 5);
        mMin = numberPickerType.getInt(R.styleable.NumberPickerPreference_min, 0);

        // mDefault = dialogType.getInt(com.android.internal.R.styleable.Preference_defaultValue, mMin);
        mDefault = mMin;
        // dialogType.recycle();
        numberPickerType.recycle();
    }

    @Override
    protected View onCreateDialogView() {
        int max = mMax;
        int min = mMin;

        // External values
        if (mMaxExternalKey != null) {
            max = getSharedPreferences().getInt(mMaxExternalKey, mMax);
        }
        if (mMinExternalKey != null) {
            min = getSharedPreferences().getInt(mMinExternalKey, mMin);
        }

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.number_picker_dialog, null);

        mNumberPicker = (NumberPicker) view.findViewById(R.id.number_picker);

        if (mNumberPicker == null) {
            throw new RuntimeException("mNumberPicker is null!");
        }

        // Initialize state
        mNumberPicker.setWrapSelectorWheel(false);
        mNumberPicker.setMaxValue(max);
        mNumberPicker.setMinValue(min);
        mNumberPicker.setValue(getPersistedInt(mDefault));

        // No keyboard popup
        disableTextInput(mNumberPicker);
        // EditText textInput = (EditText) mNumberPicker.findViewById(com.android.internal.R.id.numberpicker_input);
        // if (textInput != null) {
        // textInput.setCursorVisible(false);
        // textInput.setFocusable(false);
        // textInput.setFocusableInTouchMode(false);
        // }

        return view;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            persistInt(mNumberPicker.getValue());
            getOnPreferenceChangeListener().onPreferenceChange(this, String.valueOf(mNumberPicker.getValue()));
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
            Log.d("trebuchet", "NumberPickerPreference disableTextInput error", e);
        }
    }
}