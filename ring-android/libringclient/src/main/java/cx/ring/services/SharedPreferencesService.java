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
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.services;

import java.util.Set;

import cx.ring.model.Settings;
import cx.ring.utils.Observable;

public abstract class SharedPreferencesService extends Observable {

    protected Settings mUserSettings;

    public abstract void saveSettings(Settings settings);

    public abstract Settings loadSettings();

    public Settings getUserSettings() {
        return mUserSettings;
    }

    public abstract boolean isConnectedWifiAndMobile();

    public abstract void saveRequestPreferences(String accountId, String contactId);

    public abstract Set<String> loadRequestsPreferences(String accountId);

    public abstract void removeRequestPreferences(String accountId, String contactId);
}
