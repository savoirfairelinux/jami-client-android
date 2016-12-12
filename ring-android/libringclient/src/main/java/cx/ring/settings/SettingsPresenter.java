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
package cx.ring.settings;

import javax.inject.Inject;

import cx.ring.model.ServiceEvent;
import cx.ring.model.Settings;
import cx.ring.mvp.GenericView;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.HistoryService;
import cx.ring.services.SettingsService;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class SettingsPresenter extends RootPresenter<GenericView<SettingsViewModel>> implements Observer<ServiceEvent> {

    @Inject
    SettingsService mSettingsService;

    @Inject
    HistoryService mHistoryService;

    @Override
    public void afterInjection() {
        // We observe the application settings changes
        mSettingsService.addObserver(this);
        // no need to observe the history changes
        // only the smartlist should do so
    }

    public void loadSettings() {
        if (getView() == null) {
            return;
        }

        // load the app settings
        Settings settings = mSettingsService.loadSettings();

        // let the view display the associated ViewModel
        getView().showViewModel(new SettingsViewModel(settings));
    }

    public void saveSettings(Settings settings) {
        mSettingsService.saveSettings(settings);
    }

    public void clearHistory() {
        mHistoryService.clearHistory();
    }

    @Override
    public void update(Observable observable, ServiceEvent o) {
        if (observable instanceof SettingsService) {
            loadSettings();
        }
    }
}
