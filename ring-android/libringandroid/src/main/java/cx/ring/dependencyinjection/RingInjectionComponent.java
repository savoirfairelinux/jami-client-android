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
package cx.ring.dependencyinjection;

import javax.inject.Singleton;

import cx.ring.about.AboutPresenter;
import cx.ring.account.RingAccountSummaryPresenter;
import cx.ring.application.RingApplication;

import cx.ring.call.CallPresenter;
import cx.ring.facades.ConversationFacade;
import cx.ring.navigation.RingNavigationPresenter;
import cx.ring.service.BootReceiver;
import cx.ring.service.DRingService;
import cx.ring.services.AccountService;
import cx.ring.services.CallService;
import cx.ring.services.ConferenceService;
import cx.ring.services.ContactServiceImpl;
import cx.ring.services.DaemonService;
import cx.ring.services.DeviceRuntimeServiceImpl;
import cx.ring.services.HardwareService;
import cx.ring.services.HistoryServiceImpl;
import cx.ring.services.NotificationServiceImpl;
import cx.ring.services.PresenceService;
import cx.ring.services.SharedPreferencesServiceImpl;
import cx.ring.settings.SettingsPresenter;
import cx.ring.share.SharePresenter;
import dagger.Component;

@Singleton
@Component(modules = {RingInjectionModule.class, PresenterInjectionModule.class, ServiceInjectionModule.class})
public interface RingInjectionComponent {
    void inject(RingApplication app);

    void inject(DRingService service);

    void inject(DeviceRuntimeServiceImpl service);

    void inject(DaemonService service);

    void inject(CallService service);

    void inject(ConferenceService service);

    void inject(AccountService service);

    void inject(HardwareService service);

    void inject(SharedPreferencesServiceImpl service);

    void inject(HistoryServiceImpl service);

    void inject(ContactServiceImpl service);

    void inject(NotificationServiceImpl service);

    void inject(ConversationFacade service);

    void inject(PresenceService service);

    void inject(AboutPresenter presenter);

    void inject(RingNavigationPresenter presenter);

    void inject(SharePresenter presenter);

    void inject(SettingsPresenter presenter);

    void inject(RingAccountSummaryPresenter presenter);

    void inject(CallPresenter presenter);

    void inject(BootReceiver receiver);
}
