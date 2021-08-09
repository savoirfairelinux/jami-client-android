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
package cx.ring.utils

import android.content.Context
import net.jami.services.AccountService
import com.google.android.material.textfield.TextInputLayout
import android.widget.EditText
import android.text.TextWatcher
import net.jami.utils.NameLookupInputHandler
import android.text.Editable
import android.text.TextUtils
import cx.ring.R
import java.lang.ref.WeakReference

class RegisteredNameTextWatcher(
    context: Context,
    accountService: AccountService,
    accountId: String,
    inputLayout: TextInputLayout,
    inputText: EditText
) : TextWatcher {
    private val mInputLayout: WeakReference<TextInputLayout> = WeakReference(inputLayout)
    private val mInputText: WeakReference<EditText> = WeakReference(inputText)
    private val mNameLookupInputHandler = NameLookupInputHandler(accountService, accountId)
    private val mLookingForAvailability = context.getString(R.string.looking_for_username_availability)

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        mInputText.get()?.apply { error = null }
    }

    override fun afterTextChanged(txt: Editable) {
        val name = txt.toString()
        mInputLayout.get()?.let { inputLayout ->
            if (TextUtils.isEmpty(name)) {
                inputLayout.isErrorEnabled = false
                inputLayout.error = null
            } else {
                inputLayout.isErrorEnabled = true
                inputLayout.error = mLookingForAvailability
            }
        }
        if (!TextUtils.isEmpty(name)) {
            mNameLookupInputHandler.enqueueNextLookup(name)
        }
    }

}