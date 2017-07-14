/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Loïc Siret <loic.siret@savoirfairelinux.com>
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

import cx.ring.dependencyinjection.AndroidTVInjectionComponent;
import cx.ring.dependencyinjection.DaggerAndroidTVInjectionComponent;
import cx.ring.dependencyinjection.PresenterInjectionModule;
import cx.ring.dependencyinjection.RingInjectionComponent;
import cx.ring.dependencyinjection.RingInjectionModule;
import cx.ring.dependencyinjection.ServiceInjectionModule;

public class RingTVApplication extends RingApplication {

    private AndroidTVInjectionComponent mAndroidTVInjectionComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        // building injection dependency tree
        mAndroidTVInjectionComponent = DaggerAndroidTVInjectionComponent.builder()
                .ringInjectionModule(new RingInjectionModule(this))
                .presenterInjectionModule(new PresenterInjectionModule(this))
                .serviceInjectionModule(new ServiceInjectionModule(this))
                .build();
        mAndroidTVInjectionComponent.inject(this);
    }


    public AndroidTVInjectionComponent getAndroidTVInjectionComponent() {
        return mAndroidTVInjectionComponent;
    }

    @Override
    public RingInjectionComponent getRingInjectionComponent() {
        return mAndroidTVInjectionComponent;
    }
}
