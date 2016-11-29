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

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import cx.ring.R;
import cx.ring.client.AccountWizard;

public class RingLinkAccountFragment extends Fragment {
    static final String TAG = RingLinkAccountFragment.class.getSimpleName();

    @BindView(R.id.ring_add_pin)
    EditText mPinTxt;

    @BindView(R.id.ring_existing_password)
    EditText mPasswordTxt;

    @BindView(R.id.link_button)
    Button mLinkAccountBtn;

    @BindView(R.id.next_create_account)
    Button mNextButton;

    @BindView(R.id.last_create_account)
    Button mLastButton;

    @OnTextChanged({R.id.ring_existing_password, R.id.ring_add_pin})
    public void checkNextState() {
        mLinkAccountBtn.setEnabled(!mPinTxt.getText().toString().isEmpty() && !mPasswordTxt.getText().toString().isEmpty());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.frag_acc_ring_link, parent, false);

        ButterKnife.bind(this, view);

        if (((AccountWizard) getActivity()).isFirstAccount()) {
            mLinkAccountBtn.setVisibility(View.GONE);
        } else {
            mNextButton.setVisibility(View.GONE);
        }
        checkNextState();
        return view;
    }

    @OnClick(R.id.link_button)
    public void onLinkClick(View view) {
        ((AccountWizard) getActivity()).createAccount(null, mPinTxt.getText().toString(), mPasswordTxt.getText().toString());
    }

    @OnClick(R.id.next_create_account)
    public void onNextClick() {
        ((AccountWizard) getActivity()).accountNext(null, mPinTxt.getText().toString(), mPasswordTxt.getText().toString());
    }

    @OnClick(R.id.last_create_account)
    public void onLastClick() {
        ((AccountWizard) getActivity()).accountLast();
    }
}