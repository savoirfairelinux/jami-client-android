/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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

import android.content.Context;

import cx.ring.application.JamiApplication;
import dagger.Module;
import dagger.Provides;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;

@Module
public class JamiInjectionModule {

    private final JamiApplication mJamiApplication;

    public JamiInjectionModule(JamiApplication app) {
        mJamiApplication = app;
    }

    @Provides
    JamiApplication provideRingApplication() {
        return mJamiApplication;
    }

    @Provides
    Context provideContext() {
        return mJamiApplication;
    }

    @Provides
    Scheduler provideMainSchedulers() {
        return AndroidSchedulers.mainThread();
    }

}
