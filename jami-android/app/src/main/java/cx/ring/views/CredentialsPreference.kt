/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.views

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import android.util.AttributeSet
import androidx.preference.DialogPreference
import cx.ring.R
import net.jami.model.AccountCredentials

class CredentialsPreference(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : DialogPreference(context, attrs, defStyleAttr, defStyleRes) {
    var creds: AccountCredentials? = null
        set(c) {
            field = c
            if (c != null) {
                title = c.username
                summary = if (TextUtils.isEmpty(c.realm)) "*" else c.realm
                setDialogTitle(R.string.account_credentials_edit)
                setPositiveButtonText(android.R.string.ok)
                setNegativeButtonText(android.R.string.cancel)
            }
        }

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = androidx.preference.R.attr.dialogPreferenceStyle) : this(context, attrs, defStyle, 0)

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        return if (this.isPersistent) {
            superState
        } else {
            val myState = SavedState(superState)
            myState.creds = creds
            myState
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state != null && state.javaClass == SavedState::class.java) {
            val myState = state as SavedState
            super.onRestoreInstanceState(myState.superState)
            creds = myState.creds
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    private class SavedState : BaseSavedState {
        var creds: AccountCredentials? = null

        constructor(source: Parcel) : super(source) {
            creds = source.readSerializable() as AccountCredentials?
        }

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeSerializable(creds)
        }

        companion object {
            @JvmField val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(i: Parcel): SavedState {
                    return SavedState(i)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }
}
