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

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import cx.ring.dependencyinjection.DaggerRingInjectionComponent;
import cx.ring.dependencyinjection.PresenterInjectionModule;
import cx.ring.dependencyinjection.RingInjectionComponent;
import cx.ring.dependencyinjection.RingInjectionModule;
import cx.ring.dependencyinjection.ServiceInjectionModule;
import cx.ring.services.LogService;
import cx.ring.utils.Log;

public class RingApplication extends Application {

    private final static String TAG = RingApplication.class.getName();

    private RingInjectionComponent mRingInjectionComponent;
    private Map<String, Boolean> mPermissionsBeingAsked;

    /**
     * Handler to run tasks that needs to be on main thread (UI updates)
     */
    public static final Handler uiHandler = new Handler(Looper.getMainLooper());

    @Inject
    LogService mLogService;

    private void setDefaultUncaughtExceptionHandler() {
        try {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    android.util.Log.e(TAG, "Uncaught Exception detected in thread ", e);
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(2);
                }
            });
        } catch (SecurityException e) {
            android.util.Log.e(TAG, "Could not set the Default Uncaught Exception Handler", e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        setDefaultUncaughtExceptionHandler();

        mPermissionsBeingAsked = new HashMap<>();

        // building injection dependency tree
        mRingInjectionComponent = DaggerRingInjectionComponent.builder()
                .ringInjectionModule(new RingInjectionModule(this))
                .presenterInjectionModule(new PresenterInjectionModule(this))
                .serviceInjectionModule(new ServiceInjectionModule(this))
                .build();

        // we can now inject in our self whatever modules define
        mRingInjectionComponent.inject(this);
        // Injecting LogService into the app logger
        // as it is a static class, the injection is done manually
        Log.injectLogService(mLogService);

        Log.d(TAG, "Dependency Injection finished");
    }

    public RingInjectionComponent getRingInjectionComponent() {
        return mRingInjectionComponent;
    }

    public boolean canAskForPermission(String permission) {

        Boolean isBeingAsked = mPermissionsBeingAsked.get(permission);

        if (isBeingAsked != null && isBeingAsked) {
            return false;
        }

        mPermissionsBeingAsked.put(permission, true);

        return true;
    }

    public void permissionHasBeenAsked(String permission) {
        mPermissionsBeingAsked.remove(permission);
    }
}
