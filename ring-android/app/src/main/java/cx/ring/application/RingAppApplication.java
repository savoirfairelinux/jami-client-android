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
package cx.ring.application;

import cx.ring.dependencyinjection.DaggerRingAppInjectionComponent;
import cx.ring.dependencyinjection.PresenterInjectionModule;
import cx.ring.dependencyinjection.RingAppInjectionComponent;
import cx.ring.dependencyinjection.RingInjectionModule;
import cx.ring.dependencyinjection.ServiceInjectionModule;

public class RingAppApplication extends RingApplication {

    private RingAppInjectionComponent mRingAppInjectionComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        // building injection dependency tree
        mRingAppInjectionComponent = DaggerRingAppInjectionComponent.builder()
                .ringInjectionModule(new RingInjectionModule(this))
                .presenterInjectionModule(new PresenterInjectionModule(this))
                .serviceInjectionModule(new ServiceInjectionModule(this))
                .build();

        // we can now inject in our self whatever modules define
        mRingAppInjectionComponent.inject(this);

    }

    @Override
    public RingAppInjectionComponent getRingInjectionComponent() {
        return mRingAppInjectionComponent;
    }

}
