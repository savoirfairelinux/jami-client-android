/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.utils;

import android.content.Context;
import com.google.android.material.textfield.TextInputLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;

import java.lang.ref.WeakReference;

import cx.ring.R;
import net.jami.services.AccountService;
import net.jami.utils.NameLookupInputHandler;

public class RegisteredNameTextWatcher implements TextWatcher {

    private WeakReference<TextInputLayout> mInputLayout;
    private WeakReference<EditText> mInputText;
    private NameLookupInputHandler mNameLookupInputHandler;
    private String mLookingForAvailability;

    public RegisteredNameTextWatcher(Context context, AccountService accountService, String accountId, TextInputLayout inputLayout, EditText inputText) {
        mInputLayout = new WeakReference<>(inputLayout);
        mInputText = new WeakReference<>(inputText);
        mLookingForAvailability = context.getString(R.string.looking_for_username_availability);
        mNameLookupInputHandler = new net.jami.utils.NameLookupInputHandler(accountService, accountId);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (mInputText.get() != null) {
            mInputText.get().setError(null);
        }
    }

    @Override
    public void afterTextChanged(final Editable txt) {
        final String name = txt.toString();

        if (mInputLayout.get() == null || mInputText.get() == null) {
            return;
        }

        if (TextUtils.isEmpty(name)) {
            mInputLayout.get().setErrorEnabled(false);
            mInputLayout.get().setError(null);
        } else {
            mInputLayout.get().setErrorEnabled(true);
            mInputLayout.get().setError(mLookingForAvailability);
        }

        if (!TextUtils.isEmpty(name)) {
            mNameLookupInputHandler.enqueueNextLookup(name);
        }
    }
}