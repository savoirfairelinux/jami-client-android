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
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;
import cx.ring.R;
import cx.ring.dependencyinjection.JamiInjectionComponent;
import cx.ring.mvp.AccountCreationModel;
import cx.ring.mvp.BaseSupportFragment;

public class JamiAccountConnectFragment extends BaseSupportFragment<JamiAccountConnectPresenter> implements JamiConnectAccountView {

    public static final String TAG = JamiAccountConnectFragment.class.getSimpleName();

    @BindView(R.id.prompt_server)
    protected EditText mServerTxt;

    @BindView(R.id.username_txt)
    protected EditText mUsernameTxt;

    @BindView(R.id.password_txt)
    protected EditText mPasswordTxt;

    @BindView(R.id.connect_button)
    protected Button mConnectAccountBtn;

    private AccountCreationModel model;

    public static JamiAccountConnectFragment newInstance(AccountCreationModelImpl ringAccountViewModel) {
        JamiAccountConnectFragment fragment = new JamiAccountConnectFragment();
        fragment.model = ringAccountViewModel;
        return fragment;
    }

    @Override
    public int getLayout() {
        return R.layout.frag_acc_jami_connect;
    }

    @Override
    public void injectFragment(JamiInjectionComponent component) {
        component.inject(this);
    }

    @Override
    protected void initPresenter(JamiAccountConnectPresenter presenter) {
        presenter.init(model);
    }

    @OnClick(R.id.connect_button)
    public void onConnectClick() {
        presenter.connectClicked();
    }

    @OnTextChanged(value = R.id.username_txt, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void afterUsernameChanged(Editable txt) {
        presenter.usernameChanged(txt.toString());
    }

    @OnTextChanged(value = R.id.password_txt, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void afterPasswordChanged(Editable txt) {
        presenter.passwordChanged(txt.toString());
    }

    @OnTextChanged(value = R.id.prompt_server, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void afterServerChanged(Editable txt) {
        presenter.serverChanged(txt.toString());
    }

    @OnEditorAction(value = R.id.password_txt)
    public boolean onPasswordConfirmDone(int keyCode) {
        if (keyCode == EditorInfo.IME_ACTION_DONE) {
            presenter.connectClicked();
        }
        return false;
    }

    @Override
    public void enableConnectButton(boolean enable) {
        mConnectAccountBtn.setEnabled(enable);
    }

    @Override
    public void createAccount(AccountCreationModel accountCreationModel) {
        ((AccountWizardActivity) requireActivity()).createAccount(accountCreationModel);
    }

    @Override
    public void cancel() {
        Activity wizardActivity = getActivity();
        if (wizardActivity != null) {
            wizardActivity.onBackPressed();
        }
    }
}