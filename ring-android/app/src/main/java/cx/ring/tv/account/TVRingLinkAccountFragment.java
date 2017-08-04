package cx.ring.tv.account;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.view.View;

import java.util.List;

import cx.ring.R;
import cx.ring.account.RingLinkAccountPresenter;
import cx.ring.account.RingLinkAccountView;
import cx.ring.application.RingApplication;
import cx.ring.utils.StringUtils;

public class TVRingLinkAccountFragment extends RingGuidedStepFragment<RingLinkAccountPresenter>
        implements RingLinkAccountView {
    private static final int PASSWORD = 0;
    private static final int PIN = 1;
    private static final int LINK = 2;

    public TVRingLinkAccountFragment() {
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getString(R.string.account_link_title);
        String breadcrumb = "";
        String description = "";

        Drawable icon = getActivity().getResources().getDrawable(R.drawable.ic_contact_picture);
        return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
    }

    @Override
    public void onCreateActions(List<GuidedAction> actions, Bundle savedInstanceState) {
        addMultilineActions(actions, getString(R.string.help_password_enter), "");
        addPasswordtAction(actions, PASSWORD, getString(R.string.account_enter_password), "", "");
        addMultilineActions(actions, getString(R.string.help_pin_enter), "");
        addPasswordtAction(actions, PIN, getString(R.string.account_link_prompt_pin), "", "");
        addDisabledAction(actions, LINK, getString(R.string.account_link_title), "");
    }

    @Override
    public int onProvideTheme() {
        return R.style.Theme_Ring_Leanback_GuidedStep_First;
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == LINK) {
            presenter.linkClicked();
        }
    }

    @Override
    public void enableLinkButton(boolean enable) {
        findActionById(LINK).setEnabled(enable);

        notifyActionChanged(findActionPositionById(LINK));
    }

    @Override
    public void goToLast() {
        //no op on tv
    }

    @Override
    public void createAccount() {
        ((TVAccountWizard) getActivity()).createAccount(
                null,
                presenter.getPin(),
                presenter.getPassword());

    }

    @Override
    public long onGuidedActionEditedAndProceed(GuidedAction action) {
        String password = action.getEditDescription().toString();
        if (password.length() > 0) {
            action.setDescription(StringUtils.toPassword(password));
        } else {
            action.setDescription(getString(R.string.account_enter_password));
        }
        if (action.getId() == PASSWORD) {
            notifyActionChanged(findActionPositionById(PASSWORD));
            presenter.passwordChanged(password);
        } else if (action.getId() == PIN) {
            notifyActionChanged(findActionPositionById(PIN));
            presenter.pinChanged(action.getEditDescription().toString());
        }
        return super.onGuidedActionEditedAndProceed(action);
    }

}
