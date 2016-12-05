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
package cx.ring.tests.dependencyinjection;

import javax.inject.Singleton;

import cx.ring.about.AboutPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.CallService;
import cx.ring.services.ConferenceService;
import cx.ring.services.DaemonService;
import cx.ring.services.HardwareService;
import cx.ring.settings.SettingsPresenter;
import cx.ring.share.SharePresenter;
import cx.ring.tests.services.DeviceRuntimeServiceImpl;
import cx.ring.tests.services.HistoryServiceImpl;
import cx.ring.tests.services.SettingsServiceImpl;
import dagger.Component;

@Singleton
@Component(modules = {TestRingInjectionModule.class, TestPresenterInjectionModule.class, TestServiceInjectionModule.class})
public interface TestRingInjectionComponent {
    void inject(TestApplication app);

    void inject(DaemonService service);

    void inject(CallService service);

    void inject(ConferenceService service);

    void inject(AccountService service);

    void inject(DeviceRuntimeServiceImpl service);

    void inject(HardwareService service);

    void inject(AboutPresenter presenter);

    void inject(SharePresenter presenter);

    void inject(SettingsPresenter presenter);

    void inject(SettingsServiceImpl service);

    void inject(HistoryServiceImpl service);
}
