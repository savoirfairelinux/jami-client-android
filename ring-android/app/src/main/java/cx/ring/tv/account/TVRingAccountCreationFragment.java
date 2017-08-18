package cx.ring.tv.account;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.List;

import cx.ring.R;
import cx.ring.account.RingAccountCreationPresenter;
import cx.ring.account.RingAccountCreationView;
import cx.ring.application.RingApplication;
import cx.ring.utils.Log;

public class TVRingAccountCreationFragment
        extends RingGuidedStepFragment<RingAccountCreationPresenter>
        implements RingAccountCreationView {

    private static final String TAG = TVRingAccountCreationFragment.class.getSimpleName();
    private static final int USERNAME = 0;
    private static final int CONTINUE = 3;
    TextWatcher usernameWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            presenter.passwordChanged("password");
            presenter.passwordConfirmChanged("password");
            Log.d(TAG, "userNameChanged(" + s.toString() + ")");
            findActionById(USERNAME).setDescription(s.toString());
            presenter.ringCheckChanged(!s.toString().isEmpty());
            presenter.userNameChanged(s.toString());
        }
    };

    public TVRingAccountCreationFragment() {
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    @NonNull
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getString(R.string.account_create_title);
        String breadcrumb = "";
        String description = getString(R.string.help_ring);

        Drawable icon = getActivity().getResources().getDrawable(R.drawable.ic_contact_picture);
        return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        addEditTextAction(actions, USERNAME, getString(R.string.register_username), getString(R.string.prompt_new_username), "");
        addAction(actions, CONTINUE, getString(R.string.action_create), "");

    }

    //FIXME: Leanback doesn't provide methode to know when action are initialised
    // so we use this, the down effect is what we initialise several time the textwatcher
    @Override
    public void onGuidedActionFocused(GuidedAction action) {
        super.onGuidedActionFocused(action);
        //FIXME: Leanback doesn't provide access to the EditText Textwatcher
        //So we need to access it by parsing children, this code will break if
        //view architecture change
        ViewGroup view = (ViewGroup) getActionItemView(findActionPositionById(USERNAME));
        for (int index = 0; index < view.getChildCount(); ++index) {
            View nextChild = view.getChildAt(index);
            if (nextChild instanceof LinearLayout) {
                ViewGroup editContainer = ((ViewGroup) nextChild);
                if (editContainer.getChildCount() >= 2 && editContainer.getChildAt(1) instanceof EditText) {
                    EditText text = (EditText) editContainer.getChildAt(1);
                    text.removeTextChangedListener(usernameWatcher);
                    text.addTextChangedListener(usernameWatcher);
                }
            }
        }
    }

    @Override
    public int onProvideTheme() {
        return R.style.Theme_Ring_Leanback_GuidedStep_First;
    }

    @Override
    public void enableTextError() {
        Log.d(TAG, "enableTextError");
        findActionById(CONTINUE).setTitle(getString(R.string.looking_for_username_availability));
        notifyActionChanged(findActionPositionById(CONTINUE));
    }

    @Override
    public void disableTextError() {
        Log.d(TAG, "disableTextError");
    }

    @Override
    public void showExistingNameError() {
        Log.d(TAG, "showExistingNameError");
        findActionById(CONTINUE).setIcon(getResources().getDrawable(R.drawable.ic_error));
        findActionById(CONTINUE).setDescription(getString(R.string.username_already_taken));

        findActionById(CONTINUE).setEnabled(false);
        notifyActionChanged(findActionPositionById(CONTINUE));
    }

    @Override
    public void showInvalidNameError() {
        Log.d(TAG, "showInvalidNameError");
        findActionById(CONTINUE).setIcon(getResources().getDrawable(R.drawable.ic_error));
        findActionById(CONTINUE).setDescription(getString(R.string.invalid_username));
        notifyActionChanged(findActionPositionById(CONTINUE));
    }

    @Override
    public void showInvalidPasswordError(boolean display) {
        //NOOP on TV
    }

    @Override
    public void showNonMatchingPasswordError(boolean display) {
        //NOOP on TV
    }

    @Override
    public void displayUsernameBox(boolean display) {
        //NOOP on TV
    }

    @Override
    public void enableNextButton(boolean enabled) {
        Log.d(TAG, "enableNextButton(" + enabled + ")");
        GuidedAction actionContinue = findActionById(CONTINUE);

        if (enabled) {
            actionContinue.setIcon(getResources().getDrawable(R.drawable.ic_good));
            actionContinue.setTitle(getString(R.string.action_create));
            actionContinue.setDescription("");
            actionContinue.setEnabled(true);
        }
        actionContinue.setEnabled(enabled);
        notifyActionChanged(findActionPositionById(CONTINUE));
    }

    @Override
    public void goToAccountCreation(String username, String password) {
        Activity wizardActivity = getActivity();
        if (wizardActivity != null && wizardActivity instanceof TVAccountWizard) {
            TVAccountWizard wizard = (TVAccountWizard) wizardActivity;
            //Currently it's not possible to create a username without password
            //causing a 400 error
            wizard.createAccount(username, null, password);
        }
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == CONTINUE) {
            presenter.createAccount();
        }
    }

}
