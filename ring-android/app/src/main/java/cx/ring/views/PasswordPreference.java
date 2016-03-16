/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package cx.ring.views;

import android.content.Context;
import android.support.v7.preference.EditTextPreference;
import android.util.AttributeSet;

public class PasswordPreference extends EditTextPreference {

    public PasswordPreference(Context context) {
        this(context, null);
    }

    public PasswordPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PasswordPreference(Context context, AttributeSet attrs, int defStyleAttr,
                              int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public PasswordPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

}