/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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

import android.app.Activity;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;
import cx.ring.R;
import cx.ring.dependencyinjection.RingInjectionComponent;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.mvp.AccountCreationModel;

public class RingLinkAccountFragment extends BaseSupportFragment<RingLinkAccountPresenter> implements RingLinkAccountView {

    public static final String TAG = RingLinkAccountFragment.class.getSimpleName();

    @BindView(R.id.pin_box)
    protected ViewGroup mPinBox;

    @BindView(R.id.pin_help_message)
    protected View mPinMessage;

    @BindView(R.id.ring_add_pin)
    protected EditText mPinTxt;

    @BindView(R.id.ring_existing_password)
    protected EditText mPasswordTxt;

    @BindView(R.id.link_button)
    protected Button mLinkAccountBtn;

    private AccountCreationModel model;

    public static RingLinkAccountFragment newInstance(AccountCreationModelImpl ringAccountViewModel) {
        RingLinkAccountFragment fragment = new RingLinkAccountFragment();
        fragment.model = ringAccountViewModel;
        return fragment;
    }

    @Override
    public int getLayout() {
        return R.layout.frag_acc_ring_link;
    }

    @Override
    public void injectFragment(RingInjectionComponent component) {
        component.inject(this);
    }

    @Override
    protected void initPresenter(RingLinkAccountPresenter presenter) {
        presenter.init(model);
    }

    @OnClick(R.id.link_button)
    public void onLinkClick() {
        presenter.linkClicked();
    }

    @OnTextChanged(value = R.id.ring_existing_password, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    public void afterPasswordChanged(Editable txt) {
        presenter.passwordChanged(txt.toString());
    }

    @OnTextChanged(value = R.id.ring_add_pin, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    public void afterPinChanged(Editable txt) {
        presenter.pinChanged(txt.toString());
    }

    @OnEditorAction(value = R.id.ring_add_pin)
    public boolean onPasswordConfirmDone(int keyCode) {
        if (keyCode == EditorInfo.IME_ACTION_DONE) {
            presenter.linkClicked();
        }
        return false;
    }

    @Override
    public void enableLinkButton(boolean enable) {
        mLinkAccountBtn.setEnabled(enable);
    }

    @Override
    public void showPin(boolean show) {
        mPinBox.setVisibility(show ? View.VISIBLE : View.GONE);
        mPinMessage.setVisibility(show ? View.VISIBLE : View.GONE);
        mLinkAccountBtn.setText(show ? R.string.account_link_button : R.string.account_link_archive_button);
    }

    @Override
    public void createAccount(AccountCreationModel accountCreationModel) {
        ((AccountWizardActivity) requireActivity()).createAccount(accountCreationModel);
    }

    @Override
    public void finish() {
        Activity activity = getActivity();
        if(activity != null) {
            activity.finish();
        }
    }
}