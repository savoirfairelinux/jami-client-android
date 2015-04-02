/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package cx.ring.model.account;

import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.util.Log;

public class SRTPManager {
    PreferenceScreen mScreen;
    private Account mAccount;

    public void onCreate(PreferenceScreen preferenceScreen, Account acc) {
        mScreen = preferenceScreen;
        mAccount = acc;
        
        setDetails();
    }

    private void setDetails() {
        for (int i = 0; i < mScreen.getPreferenceCount(); ++i) {
            ((CheckBoxPreference) mScreen.getPreference(i)).setChecked(mAccount.getSrtpDetails().getDetailBoolean(mScreen.getPreference(i).getKey()));
        }
    }

    public void setSDESListener() {
        mScreen.findPreference("SRTP.rtpFallback").setOnPreferenceChangeListener(toggleFallbackListener);
    }

    private OnPreferenceChangeListener toggleFallbackListener = new OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            mAccount.getSrtpDetails().setDetailString(AccountDetailSrtp.CONFIG_SRTP_RTP_FALLBACK, Boolean.toString((Boolean) newValue));
            mAccount.notifyObservers();
            return true;
        }
    };

    public void setZRTPListener() {
        for (int i = 0; i < mScreen.getPreferenceCount(); ++i) {
            mScreen.getPreference(i).setOnPreferenceChangeListener(zrtpListener);
        }
    }

    private OnPreferenceChangeListener zrtpListener = new OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Log.i("SRTP", "Setting " + preference.getKey() + " to" + (Boolean) newValue);
            mAccount.getSrtpDetails().setDetailString(preference.getKey(), Boolean.toString((Boolean) newValue));
            mAccount.notifyObservers();
            return true;
        }
    };

}
