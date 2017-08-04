package cx.ring.tv.account;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.view.View;

import java.util.List;

import cx.ring.R;
import cx.ring.account.HomeAccountCreationPresenter;
import cx.ring.account.HomeAccountCreationView;
import cx.ring.application.RingApplication;

public class TVHomeAccountCreationFragment
        extends RingGuidedStepFragment<HomeAccountCreationPresenter>
        implements HomeAccountCreationView {

    private static final int LINK_ACCOUNT = 0;
    private static final int CREATE_ACCOUNT = 1;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void goToAccountCreation() {
        ((TVAccountWizard) getActivity()).newAccount(true);
    }

    @Override
    public void goToAccountLink() {
        ((TVAccountWizard) getActivity()).newAccount(false);
    }

    @Override
    public int onProvideTheme() {
        return R.style.Theme_Ring_Leanback_GuidedStep_First;
    }

    @Override
    @NonNull
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getString(R.string.account_creation_home);
        String breadcrumb = "";
        String description = getString(R.string.help_ring);
        Drawable icon = getActivity().getResources().getDrawable(R.drawable.ic_logo_ring_white);
        return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        addAction(actions, LINK_ACCOUNT,
                getString(R.string.account_link_button),
                "");
        addAction(actions, CREATE_ACCOUNT,
                getString(R.string.account_create_title),
                null);
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (action.getId() == LINK_ACCOUNT) {
            presenter.clickOnLinkAccount();
        } else if (action.getId() == CREATE_ACCOUNT) {
            presenter.clickOnCreateAccount();
        } else {
            getActivity().finish();
        }
    }
}