package cx.ring.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnEditorAction;
import cx.ring.R;
import cx.ring.service.LocalService;
import cx.ring.utils.BlockchainUtils;

public class RegisterNameDialog extends DialogFragment {
    static final String TAG = RegisterNameDialog.class.getSimpleName();

    public interface RegisterNameDialogListener {
        void onRegisterName(String name, String password);
    }

    @BindView(R.id.ring_username_txt_box)
    public TextInputLayout mUsernameTxtBox;

    @BindView(R.id.ring_username)
    public EditText mUsernameTxt;

    @BindView(R.id.ring_password_txt_box)
    public TextInputLayout mPasswordTxtBox;

    @BindView(R.id.ring_password_txt)
    public EditText mPasswordTxt;

    @BindString(R.string.prompt_new_username)
    public String mPromptUsername;

    private TextWatcher mUsernameTextWatcher;

    private RegisterNameDialogListener mListener = null;

    void setListener(RegisterNameDialogListener l) {
        mListener = l;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        View view = getActivity().getLayoutInflater().inflate(R.layout.frag_register_name, null);

        ButterKnife.bind(this, view);

        BlockchainUtils.attachUsernameTextFilter(mUsernameTxt);
        mUsernameTextWatcher = BlockchainUtils.attachUsernameTextWatcher((LocalService.Callbacks) getActivity(), mUsernameTxtBox, mUsernameTxt);

        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            dialog.setView(view);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        builder.setView(view);

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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (mUsernameTxt != null) {
            mUsernameTextWatcher = BlockchainUtils.attachUsernameTextWatcher((LocalService.Callbacks) getActivity(), mUsernameTxtBox, mUsernameTxt);
        }
    }

    @Override
    public void onDetach() {
        if (mUsernameTxt != null) {
            mUsernameTxt.removeTextChangedListener(mUsernameTextWatcher);
        }
        super.onDetach();
    }

    public String getUsername() {
        return mUsernameTxt.getText().toString();
    }

    public boolean checkInput() {
        if (mUsernameTxt.getText().toString().isEmpty()) {
            mUsernameTxtBox.setErrorEnabled(true);
            mUsernameTxtBox.setError("Enter username");
            return false;
        } else {
            mUsernameTxtBox.setErrorEnabled(false);
            mUsernameTxtBox.setError(null);
        }
        if (mPasswordTxt.getText().toString().isEmpty()) {
            mPasswordTxtBox.setErrorEnabled(true);
            mPasswordTxtBox.setError("Enter password");
            return false;
        } else {
            mPasswordTxtBox.setErrorEnabled(false);
            mPasswordTxtBox.setError(null);
        }
        return true;
    }

    boolean validate() {
        if (checkInput() && mListener != null) {
            final String username = mUsernameTxt.getText().toString();
            final String password = mPasswordTxt.getText().toString();
            getDialog().dismiss();
            mListener.onRegisterName(username, password);
            return true;
        }
        return false;
    }

    @OnEditorAction({R.id.ring_username, R.id.ring_password_txt})
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (v == mPasswordTxt) {
            if (actionId == EditorInfo.IME_ACTION_DONE)
                return validate();
        }
        return false;
    }
}
