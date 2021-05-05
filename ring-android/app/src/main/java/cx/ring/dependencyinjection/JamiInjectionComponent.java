/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
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

import cx.ring.account.AccountEditionFragment;
import cx.ring.account.JamiAccountPasswordFragment;
import cx.ring.account.JamiAccountUsernameFragment;
import cx.ring.account.JamiLinkAccountPasswordFragment;
import cx.ring.contactrequests.BlockListFragment;
import cx.ring.fragments.GeneralAccountFragment;
import cx.ring.fragments.LinkDeviceFragment;
import cx.ring.fragments.ContactPickerFragment;
import cx.ring.fragments.LocationSharingFragment;
import cx.ring.account.AccountWizardActivity;
import cx.ring.account.HomeAccountCreationFragment;
import cx.ring.account.JamiAccountConnectFragment;
import cx.ring.account.ProfileCreationFragment;
import cx.ring.account.RegisterNameDialog;
import cx.ring.account.JamiAccountSummaryFragment;
import cx.ring.application.JamiApplication;
import cx.ring.client.ContactDetailsActivity;
import cx.ring.client.HomeActivity;
import cx.ring.client.RingtoneActivity;
import cx.ring.contactrequests.ContactRequestsFragment;
import cx.ring.facades.ConversationFacade;
import cx.ring.fragments.AccountMigrationFragment;
import cx.ring.fragments.AdvancedAccountFragment;
import cx.ring.fragments.CallFragment;
import cx.ring.fragments.ConversationFragment;
import cx.ring.client.ConversationSelectionActivity;
import cx.ring.fragments.MediaPreferenceFragment;
import cx.ring.fragments.SIPAccountCreationFragment;
import cx.ring.fragments.SecurityAccountFragment;
import cx.ring.fragments.ShareWithFragment;
import cx.ring.fragments.SmartListFragment;
import cx.ring.history.DatabaseHelper;
import cx.ring.launch.LaunchActivity;
import cx.ring.service.BootReceiver;
import cx.ring.service.CallNotificationService;
import cx.ring.service.DRingService;
import cx.ring.service.JamiJobService;
import cx.ring.services.AccountService;
import cx.ring.services.CallService;
import cx.ring.services.ConferenceService;
import cx.ring.services.ContactServiceImpl;
import cx.ring.services.DaemonService;
import cx.ring.services.DataTransferService;
import cx.ring.services.DeviceRuntimeServiceImpl;
import cx.ring.services.HardwareService;
import cx.ring.services.HistoryServiceImpl;
import cx.ring.services.LocationSharingService;
import cx.ring.services.NotificationServiceImpl;
import cx.ring.services.SharedPreferencesServiceImpl;
import cx.ring.services.SyncService;
import cx.ring.settings.AccountFragment;
import cx.ring.settings.SettingsFragment;
import cx.ring.share.ShareFragment;
import cx.ring.tv.account.TVAccountExport;
import cx.ring.tv.account.TVAccountWizard;
import cx.ring.tv.account.TVHomeAccountCreationFragment;
import cx.ring.tv.account.TVProfileCreationFragment;
import cx.ring.tv.account.TVProfileEditingFragment;
import cx.ring.tv.account.TVJamiAccountCreationFragment;
import cx.ring.tv.account.TVJamiLinkAccountFragment;
import cx.ring.tv.account.TVSettingsFragment;
import cx.ring.tv.account.TVShareFragment;
import cx.ring.tv.call.TVCallActivity;
import cx.ring.tv.call.TVCallFragment;
import cx.ring.tv.cards.iconcards.IconCardPresenter;
import cx.ring.tv.conversation.TvConversationFragment;
import cx.ring.tv.contact.TVContactFragment;
import cx.ring.tv.main.MainFragment;
import cx.ring.tv.search.ContactSearchFragment;
import dagger.Component;

@Singleton
@Component(modules = {JamiInjectionModule.class, ServiceInjectionModule.class})
public interface JamiInjectionComponent {
    void inject(JamiApplication app);

    void inject(HomeActivity activity);

    void inject(DatabaseHelper helper);

    void inject(AccountWizardActivity activity);

    void inject(AccountEditionFragment activity);

    void inject(RingtoneActivity activity);

    void inject(AccountMigrationFragment fragment);

    void inject(SIPAccountCreationFragment fragment);

    void inject(JamiAccountSummaryFragment fragment);

    void inject(CallFragment fragment);

    void inject(SmartListFragment fragment);

    void inject(ConversationSelectionActivity fragment);

    void inject(JamiAccountUsernameFragment fragment);

    void inject(JamiAccountPasswordFragment fragment);

    void inject(MediaPreferenceFragment fragment);

    void inject(SecurityAccountFragment fragment);

    void inject(ShareFragment fragment);

    void inject(SettingsFragment fragment);

    void inject(AccountFragment fragment);

    void inject(ProfileCreationFragment fragment);

    void inject(RegisterNameDialog dialog);

    void inject(ConversationFragment fragment);

    void inject(ContactRequestsFragment fragment);

    void inject(BlockListFragment fragment);

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

    void inject(CallNotificationService service);

    void inject(DataTransferService service);

    void inject(BootReceiver receiver);

    void inject(AdvancedAccountFragment fragment);

    void inject(GeneralAccountFragment fragment);

    void inject(HomeAccountCreationFragment fragment);

    void inject(JamiLinkAccountPasswordFragment fragment);

    void inject(JamiAccountConnectFragment fragment);

    void inject(LaunchActivity activity);

    //    AndroidTV section
    void inject(TVCallFragment fragment);

    void inject(MainFragment fragment);

    void inject(ContactSearchFragment fragment);

    void inject(cx.ring.tv.main.HomeActivity activity);

    void inject(TVCallActivity activity);

    void inject(TVAccountWizard activity);

    void inject(TVHomeAccountCreationFragment fragment);

    void inject(TVProfileCreationFragment fragment);

    void inject(TVJamiAccountCreationFragment fragment);

    void inject(TVJamiLinkAccountFragment fragment);

    void inject(TVAccountExport fragment);

    void inject(TVProfileEditingFragment activity);

    void inject(TVShareFragment activity);

    void inject(TVContactFragment fragment);

    void inject(TvConversationFragment fragment);

    void inject(TVSettingsFragment tvSettingsFragment);

    void inject(TVSettingsFragment.PrefsFragment prefsFragment);

    void inject(LocationSharingFragment service);

    void inject(JamiJobService service);

    void inject(ShareWithFragment fragment);

    void inject(ContactDetailsActivity fragment);

    void inject(IconCardPresenter presenter);

    void inject(LocationSharingService service);

    void inject(SyncService syncService);

    void inject(LinkDeviceFragment linkDeviceFragment);

    void inject(ContactPickerFragment contactPickerFragment);
}
