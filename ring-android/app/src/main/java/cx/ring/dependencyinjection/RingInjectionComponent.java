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

import cx.ring.about.AboutFragment;
import cx.ring.about.AboutPresenter;
import cx.ring.application.RingApplication;
import cx.ring.client.AccountEditionActivity;
import cx.ring.client.AccountWizard;
import cx.ring.client.CallActivity;
import cx.ring.client.ConversationActivity;
import cx.ring.client.HomeActivity;
import cx.ring.fragments.AccountCreationFragment;
import cx.ring.fragments.AccountMigrationFragment;
import cx.ring.fragments.CallFragment;
import cx.ring.fragments.DeviceAccountFragment;
import cx.ring.fragments.MediaPreferenceFragment;
import cx.ring.fragments.ProfileCreationFragment;
import cx.ring.fragments.RegisterNameDialog;
import cx.ring.fragments.RingAccountCreationFragment;
import cx.ring.fragments.SecurityAccountFragment;
import cx.ring.fragments.SmartListFragment;
import cx.ring.navigation.RingNavigationFragment;
import cx.ring.navigation.RingNavigationPresenter;
import cx.ring.service.BootReceiver;
import cx.ring.service.DRingService;
import cx.ring.service.LocalService;
import cx.ring.service.VideoManagerCallback;
import cx.ring.services.AccountService;
import cx.ring.services.CallService;
import cx.ring.services.ConferenceService;
import cx.ring.services.DaemonService;
import cx.ring.services.DeviceRuntimeServiceImpl;
import cx.ring.services.HardwareService;
import cx.ring.services.HistoryServiceImpl;
import cx.ring.services.SettingsServiceImpl;
import cx.ring.settings.SettingsFragment;
import cx.ring.settings.SettingsPresenter;
import cx.ring.share.ShareFragment;
import cx.ring.share.SharePresenter;
import dagger.Component;

@Singleton
@Component(modules = {RingInjectionModule.class, PresenterInjectionModule.class, ServiceInjectionModule.class})
public interface RingInjectionComponent {
    void inject(RingApplication app);

    void inject(RingNavigationFragment view);

    void inject(CallActivity activity);

    void inject(ConversationActivity activity);

    void inject(HomeActivity activity);

    void inject(AccountWizard activity);

    void inject(AccountEditionActivity activity);

    void inject(AccountMigrationFragment fragment);

    void inject(AccountCreationFragment fragment);

    void inject(DeviceAccountFragment fragment);

    void inject(CallFragment fragment);

    void inject(AboutFragment fragment);

    void inject(SmartListFragment fragment);

    void inject(RingAccountCreationFragment fragment);

    void inject(MediaPreferenceFragment fragment);

    void inject(SecurityAccountFragment fragment);

    void inject(ShareFragment fragment);

    void inject(SettingsFragment fragment);

    void inject(ProfileCreationFragment fragment);

    void inject(RegisterNameDialog dialog);

    void inject(LocalService service);

    void inject(DRingService service);

    void inject(VideoManagerCallback callback);

    void inject(DeviceRuntimeServiceImpl service);

    void inject(DaemonService service);

    void inject(CallService service);

    void inject(ConferenceService service);

    void inject(AccountService service);

    void inject(HardwareService service);

    void inject(SettingsServiceImpl service);

    void inject(HistoryServiceImpl service);

    void inject(BootReceiver receiver);

    void inject(AboutPresenter presenter);

    void inject(RingNavigationPresenter presenter);

    void inject(SharePresenter presenter);

    void inject(SettingsPresenter presenter);
}
