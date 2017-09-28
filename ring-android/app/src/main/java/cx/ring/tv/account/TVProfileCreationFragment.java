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

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.view.View;

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