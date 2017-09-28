/*
 *  Copyright (C) 2004-2017 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.account;

import android.app.Fragment;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import butterknife.BindView;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.dependencyinjection.RingInjectionComponent;
import cx.ring.mvp.BaseFragment;

public class HomeAccountCreationFragment extends BaseFragment<HomeAccountCreationPresenter> implements HomeAccountCreationView {

    public static final String TAG = HomeAccountCreationFragment.class.getSimpleName();

    @BindView(R.id.ring_add_account)
    protected Button mLinkButton;

    @BindView(R.id.ring_create_btn)
    protected Button mCreateButton;

    @Override
    public int getLayout() {
        return R.layout.frag_acc_home_create;
    }

    @Override
    public void injectFragment(RingInjectionComponent component) {
        component.inject(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setRetainInstance(true);
    }

    @OnClick(R.id.ring_add_account)
    public void linkAccountClicked() {
        presenter.clickOnLinkAccount();
    }

    @OnClick(R.id.ring_create_btn)
    public void createAccountClicked() {
        presenter.clickOnCreateAccount();
    }

    @Override
    public void goToAccountCreation() {
        Fragment fragment = ProfileCreationFragment.newInstance(false);
        replaceFragmentWithSlide(fragment, R.id.wizard_container);
    }

    @Override
    public void goToAccountLink() {
        Fragment fragment = ProfileCreationFragment.newInstance(true);
        replaceFragmentWithSlide(fragment, R.id.wizard_container);
    }
}
