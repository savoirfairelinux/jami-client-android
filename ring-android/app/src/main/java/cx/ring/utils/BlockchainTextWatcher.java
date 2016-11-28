/*
 *  Copyright (C) 2016 Savoir-faire Linux Inc.
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

import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

import java.lang.ref.WeakReference;

import cx.ring.R;
import cx.ring.interfaces.NameLookupCallback;
import cx.ring.service.LocalService;

public class BlockchainTextWatcher implements TextWatcher, NameLookupCallback {

    private static final String TAG = BlockchainTextWatcher.class.getName();

    private WeakReference<TextInputLayout> mInputLayout;
    private WeakReference<EditText> mInputText;
    private WeakReference<LocalService> mLocalService;
    private BlockchainInputHandler mBlockchainInputHandler;
    private String mUserNameAlreadyTaken;
    private String mInvalidUsername;
    private String mLookinForAvailability;

    public BlockchainTextWatcher(final LocalService.Callbacks callbacks, final TextInputLayout inputLayout, final EditText inputText) {
        mInputLayout = new WeakReference<>(inputLayout);
        mInputText = new WeakReference<>(inputText);

        if (callbacks != null && callbacks.getService() != null) {
            mLocalService = new WeakReference<>(callbacks.getService());
            LocalService localService = callbacks.getService();
            mUserNameAlreadyTaken = localService.getString(R.string.username_already_taken);
            mInvalidUsername = localService.getString(R.string.invalid_username);
            mLookinForAvailability = localService.getString(R.string.looking_for_username_availability);
            mBlockchainInputHandler = new BlockchainInputHandler(mLocalService, this);
        } else {
            mLocalService = new WeakReference<>(null);
            mBlockchainInputHandler = new BlockchainInputHandler(mLocalService, this);
        }
    }

    @Override
    public void onFound(String name, String address) {
        if (mInputText.get() != null) {
            String searchedText = mInputText.get().getText().toString();
            Log.w(TAG, "Name lookup UI : onFound " + name + " " + address + " (current " + searchedText + ")");
            if (name.equals(searchedText)) {
                mInputLayout.get().setErrorEnabled(true);
                mInputLayout.get().setError(mUserNameAlreadyTaken);
            }
        }
    }

    @Override
    public void onInvalidName(String name) {
        if (mInputText.get() != null) {
            String searchedText = mInputText.get().getText().toString();
            Log.w(TAG, "Name lookup UI : onInvalidName " + name + " (current " + searchedText + ")");
            if (name.equals(searchedText)) {

                if (TextUtils.isEmpty(name)) {
                    mInputLayout.get().setErrorEnabled(false);
                    mInputLayout.get().setError(null);
                } else {
                    mInputLayout.get().setErrorEnabled(true);
                    mInputLayout.get().setError(mInvalidUsername);
                }
            }
        }
    }

    @Override
    public void onError(String name, String address) {
        if (mInputText.get() != null) {
            String searchedText = mInputText.get().getText().toString();
            Log.w(TAG, "Name lookup UI : onError " + name + " " + address + " (current " + searchedText + ")");
            if (name.equals(searchedText)) {
                mInputLayout.get().setErrorEnabled(false);
                mInputLayout.get().setError(null);
            }
        }
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
            mInputLayout.get().setError(mLookinForAvailability);
        }

        if (!mBlockchainInputHandler.isAlive()) {
            mBlockchainInputHandler = new BlockchainInputHandler(mLocalService, this);
        }

        mBlockchainInputHandler.enqueueNextLookup(name);
    }
}