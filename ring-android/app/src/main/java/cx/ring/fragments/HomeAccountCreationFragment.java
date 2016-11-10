/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
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

package cx.ring.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.client.AccountWizard;

public class HomeAccountCreationFragment extends Fragment {

    @BindView(R.id.ring_add_account)
    Button mLinkButton;

    @BindView(R.id.ring_create_btn)
    Button mCreateButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.frag_acc_home_create, parent, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @OnClick(R.id.ring_add_account)
    public void linkAccountClicked() {
        ((AccountWizard) getActivity()).newAccount(true);
    }

    @OnClick(R.id.ring_create_btn)
    public void createAccountClicked() {
        ((AccountWizard) getActivity()).newAccount(false);
    }
}
