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
import cx.ring.settings.SettingsPresenter;
import cx.ring.share.SharePresenter;
import dagger.Module;
import dagger.Provides;

@Module
public class TestPresenterInjectionModule {

    TestApplication mTestApplication;

    public TestPresenterInjectionModule(TestApplication app) {
        mTestApplication = app;
    }

    @Provides
    @Singleton
    AboutPresenter provideAboutPresenter() {
        AboutPresenter presenter = new AboutPresenter();
        mTestApplication.getRingInjectionComponent().inject(presenter);
        presenter.afterInjection();
        return presenter;
    }

    @Provides
    @Singleton
    SharePresenter provideSharePresenter() {
        SharePresenter presenter = new SharePresenter();
        mTestApplication.getRingInjectionComponent().inject(presenter);
        presenter.afterInjection();
        return presenter;
    }

    @Provides
    @Singleton
    SettingsPresenter provideSettingsPresenter() {
        SettingsPresenter presenter = new SettingsPresenter();
        mTestApplication.getRingInjectionComponent().inject(presenter);
        presenter.afterInjection();
        return presenter;
    }
}
