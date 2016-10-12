/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
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

package cx.ring.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.client.AccountWizard;

public class RingAccountLoginFragment extends Fragment {
    static final String TAG = RingAccountLoginFragment.class.getSimpleName();

    @BindView(R.id.ring_add_pin)
    EditText mPinTxt;

    @BindView(R.id.ring_password)
    EditText mPasswordTxt;

    @BindView(R.id.link_button)
    AppCompatButton mLinkAccountBtn;

    void checkNextState() {
        mLinkAccountBtn.setEnabled(!mPinTxt.getText().toString().isEmpty() && !mPasswordTxt.getText().toString().isEmpty());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.frag_acc_ring_login, parent, false);

        ButterKnife.bind(this, view);

        mPinTxt.addTextChangedListener(inputWatcher);
        mPasswordTxt.addTextChangedListener(inputWatcher);

        checkNextState();
        return view;
    }

    @OnClick(R.id.link_button)
    public void onLinkClick(View view) {
        ((AccountWizard) getActivity()).initAccountCreation(true, null, mPinTxt.getText().toString(), mPasswordTxt.getText().toString(), null);
    }

    private final TextWatcher inputWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            checkNextState();
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        AccountWizard account = (AccountWizard) getActivity();
        if (account != null) {
            account.getSupportActionBar().setTitle(R.string.account_import_title);
        }
    }
}