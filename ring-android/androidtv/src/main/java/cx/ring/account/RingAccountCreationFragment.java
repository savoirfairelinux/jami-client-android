package cx.ring.account;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.view.View;

import java.util.List;

import cx.ring.R;
import cx.ring.application.RingTVApplication;
import cx.ring.fragments.RingGuidedStepFragment;

public class RingAccountCreationFragment
        extends RingGuidedStepFragment<RingAccountCreationPresenter>
        implements RingAccountCreationView {
    private static final int USERNAME = 0;
    private static final int CONTINUE = 3;

    public RingAccountCreationFragment() {
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ((RingTVApplication) getActivity().getApplication()).getAndroidTVInjectionComponent().inject(this);
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getString(R.string.account_create_title);
        String breadcrumb = "";
        String description = getString(R.string.help_ring);

        Drawable icon = getActivity().getDrawable(R.drawable.ic_main_icon);
        return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
    }

    @Override
    public void onCreateActions(List<GuidedAction> actions, Bundle savedInstanceState) {
        addEditTextAction(actions, USERNAME, getString(R.string.register_username), getString(R.string.prompt_new_username), "");
        addDisabledAction(actions, CONTINUE, getString(R.string.action_create), "");
    }

    @Override
    public int onProvideTheme() {
        return R.style.Theme_Ring_Leanback_GuidedStep_First;
    }

    @Override
    public long onGuidedActionEditedAndProceed(GuidedAction action) {
        if (action.getId() == USERNAME) {
            findActionById(USERNAME).setDescription(action.getEditDescription());
            notifyActionChanged(findActionPositionById(USERNAME));
            presenter.userNameChanged(action.getEditDescription().toString());
        }
        return super.onGuidedActionEditedAndProceed(action);
    }

    @Override
    public void enableTextError() {
        findActionById(CONTINUE).setTitle(getString(R.string.looking_for_username_availability));
        notifyActionChanged(findActionPositionById(CONTINUE));
    }

    @Override
    public void disableTextError() {
        GuidedAction actionContinue = findActionById(CONTINUE);
        actionContinue.setIcon(getResources().getDrawable(R.drawable.ic_good));
        actionContinue.setTitle(getString(R.string.action_create));
        actionContinue.setDescription("");
        actionContinue.setEnabled(true);
        notifyActionChanged(findActionPositionById(CONTINUE));
    }

    @Override
    public void showExistingNameError() {
        findActionById(CONTINUE).setIcon(getResources().getDrawable(R.drawable.ic_error));
        findActionById(CONTINUE).setDescription(getString(R.string.username_already_taken));

        findActionById(CONTINUE).setEnabled(false);
        notifyActionChanged(findActionPositionById(CONTINUE));
    }

    @Override
    public void showInvalidNameError() {
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
        disableTextError();
    }

    @Override
    public void goToAccountCreation(String username, String password) {
        Activity wizardActivity = getActivity();
        if (wizardActivity != null && wizardActivity instanceof AccountWizard) {
            AccountWizard wizard = (AccountWizard) wizardActivity;
            //Currently it's not possible to create a username without password
            //causing a 400 error
            wizard.createAccount(username, null, "password");
        }
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == CONTINUE) {
            presenter.createAccount();
        }
    }
}
