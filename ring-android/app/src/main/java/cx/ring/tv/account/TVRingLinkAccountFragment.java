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
import android.view.View;

import java.util.List;

import cx.ring.R;
import cx.ring.account.AccountCreationModelImpl;
import cx.ring.account.RingLinkAccountPresenter;
import cx.ring.account.RingLinkAccountView;
import cx.ring.application.JamiApplication;
import cx.ring.mvp.AccountCreationModel;
import cx.ring.utils.StringUtils;

public class TVRingLinkAccountFragment extends RingGuidedStepFragment<RingLinkAccountPresenter>
        implements RingLinkAccountView {
    private static final long PASSWORD = 1L;
    private static final long PIN = 2L;
    private static final long LINK = 3L;
    private AccountCreationModelImpl model;

    public TVRingLinkAccountFragment() {
    }

    public static TVRingLinkAccountFragment newInstance(AccountCreationModelImpl ringAccountViewModel) {
        TVRingLinkAccountFragment fragment = new TVRingLinkAccountFragment();
        fragment.model = ringAccountViewModel;
        return fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ((JamiApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);
        super.onViewCreated(view, savedInstanceState);

        presenter.init(model);
        if (model.getPhoto() != null) {
            getGuidanceStylist().getIconView().setImageBitmap(model.getPhoto());
        }
    }

    @Override
    @NonNull
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        String title = getString(R.string.account_link_title);
        String breadcrumb = "";
        String description = getString(R.string.help_password_enter) + "\n" + getString(R.string.help_pin_enter);

        Drawable icon = getResources().getDrawable(R.drawable.ic_contact_picture_fallback);
        return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        addPasswordAction(getActivity(), actions, PASSWORD, getString(R.string.account_enter_password), "", "");
        addPasswordAction(getActivity(), actions, PIN, getString(R.string.account_link_prompt_pin), "", "");
        addDisabledAction(getActivity(), actions, LINK, getString(R.string.account_link_title), "", null, true);
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
    public void showPin(boolean show) {
        // TODO
    }

    @Override
    public void createAccount(AccountCreationModel accountCreationModel) {
        ((TVAccountWizard) getActivity()).createAccount(accountCreationModel);
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
        return GuidedAction.ACTION_ID_NEXT;
    }

    @Override
    public void cancel() {
        Activity wizardActivity = getActivity();
        if (wizardActivity != null) {
            wizardActivity.onBackPressed();
        }
    }
}
