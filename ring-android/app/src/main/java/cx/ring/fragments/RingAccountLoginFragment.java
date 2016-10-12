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
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import cx.ring.R;
import cx.ring.client.AccountWizard;
import cx.ring.service.LocalService;

public class RingAccountLoginFragment extends Fragment
{
    static final String TAG = RingAccountLoginFragment.class.getSimpleName();

    private EditText pinTxt;
    private EditText passwordTxt;
    private Button nextBtn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    void checkNextState() {
        nextBtn.setEnabled(!pinTxt.getText().toString().isEmpty() && !passwordTxt.getText().toString().isEmpty());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.frag_acc_ring_login, parent, false);

        Toolbar bottomToolbar = (Toolbar) view.findViewById(R.id.toolbar_bottom);

        Button createBtn = (Button) view.findViewById(R.id.ring_create_btn);
        createBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AccountWizard a = (AccountWizard) getActivity();
                if (a != null)
                    a.ringCreate(true);
            }
        });

        pinTxt = (EditText) view.findViewById(R.id.ring_add_pin);
        pinTxt.addTextChangedListener(inputWatcher);
        passwordTxt = (EditText) view.findViewById(R.id.ring_password);
        passwordTxt.addTextChangedListener(inputWatcher);

        nextBtn = (Button) bottomToolbar.findViewById(R.id.btn_next);
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((AccountWizard)getActivity()).initAccountCreation(true, null, pinTxt.getText().toString(), passwordTxt.getText().toString(), null);
            }
        });
        checkNextState();
        return view;
    }

    private final TextWatcher inputWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            checkNextState();
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        AccountWizard a = (AccountWizard) getActivity();
        if (a != null)
            a.getSupportActionBar().setTitle(R.string.account_import_title);
    }
}