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
package cx.ring.tests.services;

import cx.ring.model.Settings;
import cx.ring.services.SettingsService;

public class SettingsServiceImpl extends SettingsService {

    public static final String RING_SETTINGS = "ring_settings";
    public static final String RING_MOBILE_DATA = "mobile_data";
    public static final String RING_SYSTEM_CONTACTS = "system_contacts";
    public static final String RING_PLACE_CALLS = "place_calls";
    public static final String RING_ON_STARTUP = "on_startup";

    @Override
    public void saveSettings(Settings settings) {
        // notify the observers
        setChanged();
        notifyObservers();
    }

    @Override
    public Settings loadSettings() {
        Settings settings = new Settings();

        return settings;
    }
}
