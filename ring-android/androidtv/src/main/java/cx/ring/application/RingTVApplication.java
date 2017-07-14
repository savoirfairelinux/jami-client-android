package cx.ring.application;

import cx.ring.dependencyinjection.AndroidTVInjectionComponent;
import cx.ring.dependencyinjection.DaggerAndroidTVInjectionComponent;
import cx.ring.dependencyinjection.PresenterInjectionModule;
import cx.ring.dependencyinjection.RingInjectionComponent;
import cx.ring.dependencyinjection.RingInjectionModule;
import cx.ring.dependencyinjection.ServiceInjectionModule;

/**
 * Created by lsiret on 17-07-13.
 */

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
