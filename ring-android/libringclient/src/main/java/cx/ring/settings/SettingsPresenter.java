/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
import javax.inject.Named;

import cx.ring.model.Settings;
import cx.ring.mvp.GenericView;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.HistoryService;
import cx.ring.services.PreferencesService;
import io.reactivex.Scheduler;

public class SettingsPresenter extends RootPresenter<GenericView<Settings>>  {

    private final PreferencesService mPreferencesService;
    private final HistoryService mHistoryService;
    private final Scheduler mUiScheduler;
    private final AccountService mAccountService;

    @Inject
    public SettingsPresenter(PreferencesService preferencesService, HistoryService historyService, AccountService accountService, @Named("UiScheduler") Scheduler uiScheduler) {
        mPreferencesService = preferencesService;
        mHistoryService = historyService;
        mAccountService = accountService;
        mUiScheduler = uiScheduler;
    }

    @Override
    public void bindView(GenericView<Settings> view) {
        super.bindView(view);
        mCompositeDisposable.add(mPreferencesService.getSettingsSubject()
                .observeOn(mUiScheduler)
                .subscribe(settings -> getView().showViewModel(settings)));
    }

    public void loadSettings() {
        mPreferencesService.getSettings();
    }

    public void saveSettings(Settings settings) {
        mPreferencesService.setSettings(settings);
    }

    public void clearHistory() {
        mCompositeDisposable.add(mHistoryService
                .clearHistory(mAccountService.getAccounts())
                .subscribe(()->{}, e -> {}));
    }
}
