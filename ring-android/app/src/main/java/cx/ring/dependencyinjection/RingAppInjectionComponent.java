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
import cx.ring.account.AccountEditionActivity;
import cx.ring.account.HomeAccountCreationFragment;
import cx.ring.account.ProfileCreationFragment;
import cx.ring.account.RegisterNameDialog;
import cx.ring.account.RingAccountCreationFragment;
import cx.ring.account.RingAccountSummaryFragment;
import cx.ring.account.RingLinkAccountFragment;
import cx.ring.client.AccountWizard;
import cx.ring.client.HomeActivity;
import cx.ring.contactrequests.BlackListFragment;
import cx.ring.contactrequests.ContactRequestsFragment;
import cx.ring.fragments.AccountMigrationFragment;
import cx.ring.fragments.AccountsManagementFragment;
import cx.ring.fragments.CallFragment;
import cx.ring.fragments.ConversationFragment;
import cx.ring.fragments.SIPAccountCreationFragment;
import cx.ring.fragments.SmartListFragment;
import cx.ring.navigation.RingNavigationFragment;
import cx.ring.settings.AdvancedAccountFragment;
import cx.ring.settings.GeneralAccountFragment;
import cx.ring.settings.MediaPreferenceFragment;
import cx.ring.settings.SecurityAccountFragment;
import cx.ring.settings.SettingsFragment;
import cx.ring.share.ShareFragment;
import cx.ring.tv.call.CallActivity;
import cx.ring.tv.client.MainFragment;
import cx.ring.tv.search.RingSearchFragment;
import cx.ring.tv.wizard.WizardActivity;
import dagger.Component;

@Singleton
@Component(modules = {RingInjectionModule.class, PresenterInjectionModule.class, ServiceInjectionModule.class})
public interface RingAppInjectionComponent extends RingInjectionComponent {

    void inject(RingNavigationFragment view);

    void inject(HomeActivity activity);

    void inject(AccountWizard activity);

    void inject(AccountEditionActivity activity);

    void inject(AccountMigrationFragment fragment);

    void inject(SIPAccountCreationFragment fragment);

    void inject(AccountsManagementFragment fragment);

    void inject(RingAccountSummaryFragment fragment);

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

    void inject(ConversationFragment fragment);

    void inject(ContactRequestsFragment fragment);

    void inject(BlackListFragment fragment);

    void inject(AdvancedAccountFragment fragment);

    void inject(GeneralAccountFragment fragment);

    void inject(HomeAccountCreationFragment fragment);

    void inject(RingLinkAccountFragment fragment);

//   AndroidTV injection

    void inject(cx.ring.tv.call.CallFragment fragment);

    void inject(MainFragment fragment);

    void inject(RingSearchFragment fragment);

    void inject(cx.ring.tv.client.HomeActivity activity);

    void inject(WizardActivity activity);

    void inject(CallActivity activity);
}
