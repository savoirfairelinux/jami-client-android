/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
import androidx.annotation.NonNull;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.List;

import cx.ring.R;
import cx.ring.account.AccountCreationModelImpl;
import cx.ring.account.RingAccountCreationPresenter;
import cx.ring.account.RingAccountCreationView;
import cx.ring.application.RingApplication;
import cx.ring.mvp.AccountCreationModel;
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
    private AccountCreationModelImpl model;

    private TextWatcher mUsernameWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            String newName = s.toString();
            if (!newName.equals(getResources().getString(R.string.register_username))) {
                boolean empty = newName.isEmpty();
                /** If the username is empty make sure to set isRegisterUsernameChecked
                 *  to False, this allows to create an account with an empty username */
                presenter.ringCheckChanged(!empty);
                /** Send the newName even when empty (in order to reset the views) */
                presenter.userNameChanged(newName);
            }
        }
    };

    public TVRingAccountCreationFragment() {
    }

    public static TVRingAccountCreationFragment newInstance(AccountCreationModelImpl ringAccountViewModel) {
        TVRingAccountCreationFragment fragment = new TVRingAccountCreationFragment();
        fragment.model = ringAccountViewModel;
        return fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        // Bind the presenter to the view
        super.onViewCreated(view, savedInstanceState);

        if (model == null) {
            Log.e(TAG, "Not able to get model");
            return;
        }

        presenter.init(model);
        presenter.ringCheckChanged(false);
    }

    @Override
    @NonNull
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getString(R.string.account_create_title);
        String breadcrumb = "";
        String description = getString(R.string.help_ring);

        Drawable icon = getActivity().getResources().getDrawable(R.drawable.ic_contact_picture_fallback);
        return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        addEditTextAction(getActivity(), actions, USERNAME, R.string.register_username, R.string.prompt_new_username);
        addDisabledNonFocusableAction(getActivity(), actions, CHECK, "", "", null);
        addPasswordAction(getActivity(), actions, PASSWORD, getString(R.string.prompt_new_password_optional), getString(R.string.enter_password), "");
        addPasswordAction(getActivity(), actions, PASSWORD_CONFIRMATION, getString(R.string.prompt_new_password_repeat), getString(R.string.enter_password), "");
        addDisabledAction(getActivity(), actions, CONTINUE, getString(R.string.action_create), "", null, true);
    }

    @Override
    public boolean isFocusOutStartAllowed() {
        return super.isFocusOutStartAllowed();
    }

    @Override
    public void onGuidedActionFocused(GuidedAction action) {
        ViewGroup view = (ViewGroup) getActionItemView(findActionPositionById(USERNAME));
        if (view != null) {
            EditText text = view.findViewById(R.id.guidedactions_item_title);
            text.removeTextChangedListener(mUsernameWatcher);
            if (action.getId() == USERNAME) {
                text.addTextChangedListener(mUsernameWatcher);
            }
        }
    }

    @Override
    public long onGuidedActionEditedAndProceed(GuidedAction action) {
        int id = (int) action.getId();
        switch (id){
            case PASSWORD:
                passwordChanged(action);
            break;
            case PASSWORD_CONFIRMATION:
                confirmPasswordChanged(action);
                break;
            case USERNAME:
                ViewGroup view = (ViewGroup) getActionItemView(findActionPositionById(USERNAME));
                if (view != null) {
                    EditText text = view.findViewById(R.id.guidedactions_item_title);
                    text.removeTextChangedListener(mUsernameWatcher);
                }
                String username = action.getEditTitle().toString();
                boolean empty = username.isEmpty();
                if(empty)
                    action.setTitle(getString(R.string.register_username));
                else
                    action.setTitle(username);
                GuidedAction a = findActionById(CHECK);
                a.setEnabled(!empty);
                notifyActionChanged(findActionPositionById(CHECK));
                break;
        }

        return GuidedAction.ACTION_ID_NEXT;
    }

    @Override
    public void onGuidedActionEditCanceled(GuidedAction action) {
        int id = (int) action.getId();
        switch (id){
            case USERNAME:
                usernameChanged(action);
                break;
            case PASSWORD:
                passwordChanged(action);
                break;
            case PASSWORD_CONFIRMATION:
                confirmPasswordChanged(action);
                break;

        }
    }

    private void usernameChanged(GuidedAction action) {
        String username = action.getEditTitle().toString();
        ViewGroup view = (ViewGroup) getActionItemView(findActionPositionById(USERNAME));
        if (view != null) {
            EditText text = view.findViewById(R.id.guidedactions_item_title);
            text.removeTextChangedListener(mUsernameWatcher);
        }
        boolean empty = username.isEmpty();
        if(empty)
            action.setTitle(getString(R.string.register_username));
        else
            action.setTitle(username);

        notifyActionChanged(findActionPositionById(PASSWORD));
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
    public void updateUsernameAvailability(UsernameAvailabilityStatus status) {
        GuidedAction actionCheck = findActionById(CHECK);
        switch (status){
            case ERROR:
                actionCheck.setTitle(getResources().getString(R.string.generic_error));
                showUsernameAvailabilityError(actionCheck, getString(R.string.unknown_error));
                break;
            case ERROR_USERNAME_INVALID:
                showUsernameAvailabilityError(actionCheck,getString(R.string.invalid_username));
                break;
            case ERROR_USERNAME_TAKEN:
                showUsernameAvailabilityError(actionCheck,
                        getString(R.string.username_already_taken));
                break;
            case LOADING:
                actionCheck.setIcon(null);
                actionCheck.setTitle(getResources().
                        getString(R.string.looking_for_username_availability));
                break;
            case AVAILABLE:
                actionCheck.setTitle(getString(R.string.username_available));
                actionCheck.setIcon(getResources().getDrawable(R.drawable.ic_good_green));
                break;
            case RESET:
                actionCheck.setIcon(null);
                actionCheck.setTitle("");
                enableNextButton(false);
            default:
                actionCheck.setIcon(null);
                break;
        }

        notifyActionChanged(findActionPositionById(CHECK));
    }

    @Override
    public void showInvalidPasswordError(boolean display) {
        GuidedAction action = findActionById(CONTINUE);
        if (display) {
            action.setIcon(getResources().getDrawable(R.drawable.ic_error_red));
            action.setDescription(getString(R.string.error_password_char_count));
            action.setEnabled(false);
        } else {
            action.setDescription("");
        }
        notifyActionChanged(findActionPositionById(CONTINUE));
    }

    @Override
    public void showNonMatchingPasswordError(boolean display) {
        GuidedAction action = findActionById(CONTINUE);
        if (display) {
            action.setIcon(getResources().getDrawable(R.drawable.ic_error_red));
            action.setDescription(getString(R.string.error_passwords_not_equals));
            action.setEnabled(false);
        } else {
            action.setDescription("");
        }
        notifyActionChanged(findActionPositionById(CONTINUE));
    }


    @Override
    public void displayUsernameBox(boolean display) {
    }

    @Override
    public void enableNextButton(boolean enabled) {
        Log.d(TAG, "enableNextButton: " + enabled);
        GuidedAction actionContinue = findActionById(CONTINUE);
        if (enabled)
            actionContinue.setIcon(null);
        actionContinue.setEnabled(enabled);
        notifyActionChanged(findActionPositionById(CONTINUE));
    }

    @Override
    public void goToAccountCreation(AccountCreationModel accountCreationModel) {
        Activity wizardActivity = getActivity();
        if (wizardActivity instanceof TVAccountWizard) {
            TVAccountWizard wizard = (TVAccountWizard) wizardActivity;
            wizard.createAccount(accountCreationModel);
        }
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == CONTINUE) {
            presenter.createAccount();
        }
    }

    public void showUsernameAvailabilityError(GuidedAction action, String title) {
        action.setIcon(getResources().getDrawable(R.drawable.ic_error_red));
        action.setTitle(title);
    }


}
