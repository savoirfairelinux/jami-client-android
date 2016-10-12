package cx.ring.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import cx.ring.R;
import cx.ring.service.LocalService;
import cx.ring.utils.BlockchainUtils;

public class RegisterNameDialog extends DialogFragment implements TextView.OnEditorActionListener {
    static final String TAG = RegisterNameDialog.class.getSimpleName();

    public interface RegisterNameDialogListener {
        void onRegisterName(String name, String password);
    }

    private TextInputLayout usernameTxtBox;
    private EditText usernameTxt;
    private TextInputLayout passwordTxtBox;
    private EditText passwordTxt;
    private TextWatcher mUsernameTextWatcher;

    private RegisterNameDialogListener listener = null;

    void setListener(RegisterNameDialogListener l) {
        listener = l;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.w(TAG, "onCreateDialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Choose a new Ring username to register in a public, blockchain-based directory.").setTitle("Register Ring username")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        validate();
                    }
                });
        return builder.create();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.w(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_register_name, container, false);
        usernameTxtBox = (TextInputLayout) view.findViewById(R.id.ring_username_txt_box);
        usernameTxt = usernameTxtBox.getEditText();
        BlockchainUtils.attachUsernameTextFilter(usernameTxt);
        mUsernameTextWatcher = BlockchainUtils.attachUsernameTextWatcher((LocalService.Callbacks) getActivity(), usernameTxtBox, usernameTxt);
        passwordTxtBox = (TextInputLayout) view.findViewById(R.id.ring_password_txt_box);
        passwordTxt = (EditText) view.findViewById(R.id.ring_password_txt);
        AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            d.setView(view);
            d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            usernameTxt.setOnEditorActionListener(this);
            passwordTxt.setOnEditorActionListener(this);
        }
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (usernameTxt != null) {
            mUsernameTextWatcher = BlockchainUtils.attachUsernameTextWatcher((LocalService.Callbacks) getActivity(), usernameTxtBox, usernameTxt);
        }
    }

    @Override
    public void onDetach() {
        if (usernameTxt != null) {
            usernameTxt.removeTextChangedListener(mUsernameTextWatcher);
        }
        super.onDetach();
    }

    public String getUsername() {
        return usernameTxt.getText().toString();
    }

    public boolean checkInput() {
        if (usernameTxt.getText().toString().isEmpty()) {
            usernameTxtBox.setErrorEnabled(true);
            usernameTxtBox.setError("Enter username");
            return false;
        } else {
            usernameTxtBox.setErrorEnabled(false);
            usernameTxtBox.setError(null);
        }
        if (passwordTxt.getText().toString().isEmpty()) {
            passwordTxtBox.setErrorEnabled(true);
            passwordTxtBox.setError("Enter password");
            return false;
        } else {
            passwordTxtBox.setErrorEnabled(false);
            passwordTxtBox.setError(null);
        }
        return true;
    }

    boolean validate() {
        if (checkInput() && listener != null) {
            final String username = usernameTxt.getText().toString();
            final String password = passwordTxt.getText().toString();
            getDialog().dismiss();
            listener.onRegisterName(username, password);
            return true;
        }
        return false;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (v == passwordTxt) {
            if (actionId == EditorInfo.IME_ACTION_DONE)
                return validate();
        }
        return false;
    }
}
