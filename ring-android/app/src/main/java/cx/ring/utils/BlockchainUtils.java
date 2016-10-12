package cx.ring.utils;

import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

import cx.ring.service.LocalService;

/**
 * Created by twittemberg on 16-10-28.
 */

public class BlockchainUtils {

    private final static String TAG = BlockchainUtils.class.getName();

    public static TextWatcher attachUsernameTextWatcher (final LocalService.Callbacks callbacks, final TextInputLayout inputLayout, final EditText inputText) {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                inputText.setError(null);
            }

            @Override
            public void afterTextChanged(final Editable txt) {
                final String name = txt.toString();
                Log.w(TAG, "afterTextChanged name lookup " + name);
                final LocalService s = callbacks.getService();
                if (s == null)
                    return;
                s.lookupName("", name, new LocalService.NameLookupCallback() {
                    @Override
                    public void onFound(String name, String address) {
                        Log.w(TAG, "Name lookup UI : onFound " + name + " " + address + " (current " + txt.toString() + ")");
                        if (name.equals(txt.toString())) {
                            inputLayout.setErrorEnabled(true);
                            inputLayout.setError("Username already taken");
                        }
                    }

                    @Override
                    public void onInvalidName(String name) {
                        Log.w(TAG, "Name lookup UI : onInvalidName " + name + " (current " + txt.toString() + ")");
                        if (name.equals(txt.toString())) {
                            inputLayout.setErrorEnabled(true);
                            inputLayout.setError("Invalid username");
                        }
                    }

                    @Override
                    public void onError(String name, String address) {
                        Log.w(TAG, "Name lookup UI : onError " + name + " " + address + " (current " + txt.toString() + ")");
                        if (name.equals(txt.toString())) {
                            inputLayout.setErrorEnabled(false);
                            inputLayout.setError(null);
                        }
                    }
                });
            }
        };

        inputText.addTextChangedListener(textWatcher);

        return textWatcher;
    }

    public static void attachUsernameTextFilter (final EditText inputText) {
        InputFilter filter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end,
                                       Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    char charToTest = source.charAt(i);

                    if (!Character.isLowerCase(charToTest)) {
                        return "";
                    }

                    if (!Character.isLetterOrDigit(charToTest) && charToTest != '-' && charToTest != '_') {
                        return "";
                    }
                }
                return null;
            }
        };

        inputText.setFilters(new InputFilter[]{filter});
    }
}
