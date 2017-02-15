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
import cx.ring.navigation.RingNavigationPresenter;
import cx.ring.settings.SettingsPresenter;
import cx.ring.share.SharePresenter;
import cx.ring.trustrequests.PendingTrustRequestsPresenter;
import dagger.Module;
import dagger.Provides;

@Module
public class PresenterInjectionModule {

    RingApplication mRingApplication;

    public PresenterInjectionModule(RingApplication app) {
        mRingApplication = app;
    }

    @Provides
    @Singleton
    AboutPresenter provideAboutPresenter() {
        AboutPresenter presenter = new AboutPresenter();
        mRingApplication.getRingInjectionComponent().inject(presenter);
        presenter.afterInjection();
        return presenter;
    }

    @Provides
    @Singleton
    SharePresenter provideSharePresenter() {
        SharePresenter presenter = new SharePresenter();
        mRingApplication.getRingInjectionComponent().inject(presenter);
        presenter.afterInjection();
        return presenter;
    }

    @Provides
    @Singleton
    SettingsPresenter provideSettingsPresenter() {
        SettingsPresenter presenter = new SettingsPresenter();
        mRingApplication.getRingInjectionComponent().inject(presenter);
        presenter.afterInjection();
        return presenter;
    }

    @Provides
    @Singleton
    RingNavigationPresenter provideRingNavigationPresenter(){
        RingNavigationPresenter presenter = new RingNavigationPresenter();
        mRingApplication.getRingInjectionComponent().inject(presenter);
        presenter.afterInjection();
        return presenter;
    }

    @Provides
    @Singleton
    RingAccountSummaryPresenter provideRingAccountPresenter(){
        RingAccountSummaryPresenter presenter = new RingAccountSummaryPresenter();
        mRingApplication.getRingInjectionComponent().inject(presenter);
        presenter.afterInjection();
        return presenter;
    }

    @Provides
    PendingTrustRequestsPresenter provideTrustRequestsPresenter() {
        PendingTrustRequestsPresenter presenter = new PendingTrustRequestsPresenter();
        mRingApplication.getRingInjectionComponent().inject(presenter);
        presenter.afterInjection();
        return presenter;
    }
}
