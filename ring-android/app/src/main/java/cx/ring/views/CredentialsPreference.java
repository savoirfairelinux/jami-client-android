/*
 *  Copyright (C) 2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *          Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cx.ring.views;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;

import cx.ring.R;
import cx.ring.model.account.AccountCredentials;

public class CredentialsPreference extends DialogPreference {
    private AccountCredentials creds;

    public CredentialsPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
    public CredentialsPreference(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs, defStyle, 0);
    }
    public CredentialsPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.support.v7.preference.R.attr.dialogPreferenceStyle);
    }
    public CredentialsPreference(Context context) {
        this(context, null);
    }

    public AccountCredentials getCreds() {
        return creds;
    }
    public void setCreds(AccountCredentials c) {
        creds = c;
        if (creds != null) {
            setTitle(creds.getUsername());
            setSummary(creds.getRealm().isEmpty() ? "*" : creds.getRealm());
            setDialogTitle(R.string.account_credentials_edit);
            setPositiveButtonText(android.R.string.ok);
            setNegativeButtonText(android.R.string.cancel);
        }
    }

    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        if(this.isPersistent()) {
            return superState;
        } else {
            CredentialsPreference.SavedState myState = new CredentialsPreference.SavedState(superState);
            myState.creds = getCreds();
            return myState;
        }
    }

    protected void onRestoreInstanceState(Parcelable state) {
        if(state != null && state.getClass().equals(CredentialsPreference.SavedState.class)) {
            CredentialsPreference.SavedState myState = (CredentialsPreference.SavedState)state;
            super.onRestoreInstanceState(myState.getSuperState());
            setCreds(myState.creds);
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    private static class SavedState extends BaseSavedState {
        AccountCredentials creds;
        public static final Creator<CredentialsPreference.SavedState> CREATOR = new Creator<CredentialsPreference.SavedState>() {
            public CredentialsPreference.SavedState createFromParcel(Parcel in) {
                return new CredentialsPreference.SavedState(in);
            }

            public CredentialsPreference.SavedState[] newArray(int size) {
                return new CredentialsPreference.SavedState[size];
            }
        };

        public SavedState(Parcel source) {
            super(source);
            creds = source.readParcelable(AccountCredentials.class.getClassLoader());
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeParcelable(creds, 0);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }
    }
}
