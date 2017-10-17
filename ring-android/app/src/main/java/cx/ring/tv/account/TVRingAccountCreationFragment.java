/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
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

import java.util.List;

import cx.ring.R;
import cx.ring.account.RingAccountCreationFragment;
import cx.ring.account.RingAccountCreationPresenter;
import cx.ring.account.RingAccountCreationView;
import cx.ring.account.RingAccountViewModelImpl;
import cx.ring.application.RingApplication;
import cx.ring.mvp.RingAccountViewModel;
import cx.ring.utils.Log;
import cx.ring.utils.StringUtils;

public class TVRingAccountCreationFragment
        extends RingGuidedStepFragment<RingAccountCreationPresenter>
        implements RingAccountCreationView {

    private static final String TAG = TVRingAccountCreationFragment.class.getSimpleName();
    private static final int USERNAME = 0;
    private static final int PASSWORD = 1;
    private static final int PASSWORD_CONFIRMATION = 2;
    private static final int CHECK = 3;
    private static final int CONTINUE = 4;
    private TextWatcher mUsernameWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            Log.d(TAG, "userNameChanged(" + s.toString() + ")");
            findActionById(USERNAME).setDescription(s.toString());
            boolean empty = s.toString().isEmpty();
            if (!empty) {
                presenter.userNameChanged(s.toString());
            }
            presenter.ringCheckChanged(!empty);
        }
    };

    public TVRingAccountCreationFragment() {
    }

    public static TVRingAccountCreationFragment newInstance(RingAccountViewModelImpl ringAccountViewModel) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(RingAccountCreationFragment.KEY_RING_ACCOUNT, ringAccountViewModel);
        TVRingAccountCreationFragment fragment = new TVRingAccountCreationFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        // Bind the presenter to the view
        super.onViewCreated(view, savedInstanceState);

        RingAccountViewModelImpl ringAccountViewModel = getArguments().getParcelable(RingAccountCreationFragment.KEY_RING_ACCOUNT);
        presenter.init(ringAccountViewModel);
        presenter.ringCheckChanged(false);
        if (ringAccountViewModel.getPhoto() != null) {
            getGuidanceStylist().getIconView().setImageBitmap(ringAccountViewModel.getPhoto());
        }
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
        addDisabledAction(actions, CHECK, "", "", null);
        addPasswordAction(actions, PASSWORD, getString(R.string.prompt_new_password_optional), getString(R.string.enter_password), "");
        addPasswordAction(actions, PASSWORD_CONFIRMATION, getString(R.string.prompt_new_password_repeat), getString(R.string.enter_password), "");
        addDisabledAction(actions, CONTINUE, getString(R.string.action_create), "", null, true);
    }

    @Override
    public void onGuidedActionFocused(GuidedAction action) {
        if (action.getId() == USERNAME) {
            ViewGroup view = (ViewGroup) getActionItemView(findActionPositionById(USERNAME));
            EditText text = (EditText) view.findViewById(R.id.guidedactions_item_description);
            text.removeTextChangedListener(mUsernameWatcher);
            text.addTextChangedListener(mUsernameWatcher);
        }
    }


    @Override
    public long onGuidedActionEditedAndProceed(GuidedAction action) {
        if (action.getId() == PASSWORD) {
            passwordChanged(action);
        } else if (action.getId() == PASSWORD_CONFIRMATION) {
            confirmPasswordChanged(action);
        }
        return GuidedAction.ACTION_ID_NEXT;
    }

    private void passwordChanged(GuidedAction action) {
        String password = action.getEditDescription().toString();
        if (password.length() > 0) {
            action.setDescription(StringUtils.toPassword(password));
        } else {
            action.setDescription(getString(R.string.account_enter_password));
        }
        notifyActionChanged(findActionPositionById(PASSWORD));
        presenter.passwordChanged(password);
    }

    private void confirmPasswordChanged(GuidedAction action) {
        String passwordConfirm = action.getEditDescription().toString();
        if (passwordConfirm.length() > 0) {
            action.setDescription(StringUtils.toPassword(passwordConfirm));
        } else {
            action.setDescription(getString(R.string.account_enter_password));
        }
        notifyActionChanged(findActionPositionById(PASSWORD_CONFIRMATION));
        presenter.passwordConfirmChanged(passwordConfirm);
    }

    @Override
    public int onProvideTheme() {
        return R.style.Theme_Ring_Leanback_GuidedStep_First;
    }

    @Override
    public void enableTextError() {
        Log.d(TAG, "enableTextError");
        findActionById(CHECK).setIcon(null);

        findActionById(CHECK).setTitle(getString(R.string.looking_for_username_availability));
        notifyActionChanged(findActionPositionById(CHECK));
    }

    @Override
    public void disableTextError() {
        Log.d(TAG, "disableTextError");
        findActionById(CHECK).setIcon(null);
        findActionById(CHECK).setDescription("");

        notifyActionChanged(findActionPositionById(CHECK));
    }

    @Override
    public void showExistingNameError() {
        Log.d(TAG, "showExistingNameError");
        findActionById(CHECK).setIcon(getResources().getDrawable(R.drawable.ic_error));
        findActionById(CHECK).setDescription(getString(R.string.username_already_taken));

        notifyActionChanged(findActionPositionById(CHECK));
    }

    @Override
    public void showInvalidNameError() {
        Log.d(TAG, "showInvalidNameError");
        findActionById(CHECK).setIcon(getResources().getDrawable(R.drawable.ic_error));
        findActionById(CHECK).setDescription(getString(R.string.invalid_username));
        notifyActionChanged(findActionPositionById(CHECK));
    }

    @Override
    public void showInvalidPasswordError(boolean display) {
        Log.d(TAG, "showInvalidPasswordError");
        if (display) {
            findActionById(CONTINUE).setIcon(getResources().getDrawable(R.drawable.ic_error));
            findActionById(CONTINUE).setDescription(getString(R.string.error_password_char_count));
            findActionById(CONTINUE).setEnabled(false);
        }
        notifyActionChanged(findActionPositionById(CONTINUE));
    }

    @Override
    public void showNonMatchingPasswordError(boolean display) {
        Log.d(TAG, "showNonMatchingPasswordError");
        if (display) {
            findActionById(CONTINUE).setIcon(getResources().getDrawable(R.drawable.ic_error));
            findActionById(CONTINUE).setDescription(getString(R.string.error_passwords_not_equals));
            findActionById(CONTINUE).setEnabled(false);
        }
        notifyActionChanged(findActionPositionById(CONTINUE));
    }

    @Override
    public void displayUsernameBox(boolean display) {
    }

    @Override
    public void enableNextButton(boolean enabled) {
        Log.d(TAG, "enableNextButton(" + enabled + ")");
        GuidedAction actionCheck = findActionById(CHECK);
        GuidedAction actionContinue = findActionById(CONTINUE);

        if (enabled) {
            actionCheck.setIcon(getResources().getDrawable(R.drawable.ic_good));
            actionCheck.setTitle(getString(R.string.no_registered_name_for_account));
            actionCheck.setDescription("");
            actionContinue.setIcon(null);
            actionCheck.setDescription("");
        }
        actionContinue.setEnabled(enabled);
        notifyActionChanged(findActionPositionById(CHECK));
        notifyActionChanged(findActionPositionById(CONTINUE));
    }

    @Override
    public void goToAccountCreation(RingAccountViewModel ringAccountViewModel) {
        Activity wizardActivity = getActivity();
        if (wizardActivity != null && wizardActivity instanceof TVAccountWizard) {
            TVAccountWizard wizard = (TVAccountWizard) wizardActivity;
            wizard.createAccount(ringAccountViewModel);
        }
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == CONTINUE) {
            presenter.createAccount();
        }
    }

}
