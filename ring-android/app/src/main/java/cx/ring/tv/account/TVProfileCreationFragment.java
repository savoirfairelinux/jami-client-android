package cx.ring.tv.account;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import cx.ring.R;
import cx.ring.account.ProfileCreationPresenter;
import cx.ring.account.ProfileCreationView;
import cx.ring.account.RingAccountViewModelImpl;
import cx.ring.application.RingApplication;
import cx.ring.mvp.RingAccountViewModel;

public class TVProfileCreationFragment extends RingGuidedStepFragment<ProfileCreationPresenter>
        implements ProfileCreationView {

    private static final int USER_NAME = 1;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        super.onViewCreated(view, savedInstanceState);

        RingAccountViewModelImpl ringAccountViewModel = new RingAccountViewModelImpl();
        ringAccountViewModel.setLink(true);
        presenter.initPresenter(ringAccountViewModel);
    }

    @Override
    @NonNull
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getString(R.string.account_create_title);
        String breadcrumb = "";
        String description = getString(R.string.profile_message_warning);
        Drawable icon = getActivity().getResources().getDrawable(R.drawable.ic_contact_picture);
        return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
    }

    @Override
    public int onProvideTheme() {
        return R.style.Theme_Ring_Leanback_GuidedStep_First;
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        String desc = getString(R.string.account_creation_profile);
        String editdesc = getString(R.string.profile_name_hint);
        addEditTextAction(actions, USER_NAME, desc, editdesc, "");
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        presenter.nextClick();
    }

    @Override
    public void displayProfileName(String profileName) {
        findActionById(USER_NAME).setEditDescription(profileName);
        notifyActionChanged(findActionPositionById(USER_NAME));
    }

    @Override
    public void goToGallery() {
        //not implemented on TV
    }

    @Override
    public void goToPhotoCapture() {
        //not implemented on TV
    }

    @Override
    public void askStoragePermission() {
        //not implemented on TV
    }

    @Override
    public void askPhotoPermission() {
        //not implemented on TV
    }

    @Override
    public void goToNext(RingAccountViewModel ringAccountViewModel) {
        GuidedStepFragment.add(getFragmentManager(), TVRingLinkAccountFragment.newInstance((RingAccountViewModelImpl) ringAccountViewModel));
    }

    @Override
    public void photoUpdate(RingAccountViewModel ringAccountViewModel) {
        //not implemented on TV
    }

}