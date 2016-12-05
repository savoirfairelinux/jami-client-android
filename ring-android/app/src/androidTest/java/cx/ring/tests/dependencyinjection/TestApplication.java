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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.inject.Inject;

import cx.ring.daemon.Callback;
import cx.ring.daemon.ConfigurationCallback;
import cx.ring.daemon.VideoCallback;
import cx.ring.services.AccountService;
import cx.ring.services.CallService;
import cx.ring.services.ConferenceService;
import cx.ring.services.DaemonService;
import cx.ring.services.HardwareService;
import cx.ring.services.LogService;
import cx.ring.utils.Log;

public class TestApplication {

    private final static String TAG = TestApplication.class.getName();

    private TestRingInjectionComponent mTestRingInjectionComponent;

    @Inject
    DaemonService mDaemonService;

    @Inject
    CallService mCallService;

    @Inject
    ConferenceService mConferenceService;

    @Inject
    AccountService mAccountService;

    @Inject
    HardwareService mHardwareService;

    @Inject
    LogService mLogService;

    @Inject
    ExecutorService mExecutor;

    private CallManagerCallBack mCallManagerCallBack;
    private VideoManagerCallback mVideoManagerCallback;
    private ConfigurationManagerCallback mConfigurationCallback;
    private Callback mCallAndConferenceCallbackHandler;
    private ConfigurationCallback mAccountCallbackHandler;
    private VideoCallback mHardwareCallbackHandler;

    public TestApplication() {
        // building injection dependency tree
        mTestRingInjectionComponent = DaggerTestRingInjectionComponent.builder()
                .testRingInjectionModule(new TestRingInjectionModule(this))
                .testPresenterInjectionModule(new TestPresenterInjectionModule(this))
                .testServiceInjectionModule(new TestServiceInjectionModule(this))
                .build();

        // we can now inject in our self whatever modules define
        mTestRingInjectionComponent.inject(this);
        // Injecting LogService into the app logger
        // as it is a static class, the injection is done manually
        Log.injectLogService(mLogService);

        Future<Boolean> startResult = mExecutor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                // Android specific callbacks handlers (rely on pure Java low level Services callbacks handlers as they
                // observe them)
                mConfigurationCallback = new ConfigurationManagerCallback();
                mCallManagerCallBack = new CallManagerCallBack();
                mVideoManagerCallback = new VideoManagerCallback();

                // mCallAndConferenceCallbackHandler is a wrapper to handle CallCallbacks and ConferenceCallbacks
                mCallAndConferenceCallbackHandler = mDaemonService.getDaemonCallbackHandler(
                        mCallService.getCallbackHandler(),
                        mConferenceService.getCallbackHandler());
                mAccountCallbackHandler = mAccountService.getCallbackHandler();
                mHardwareCallbackHandler = mHardwareService.getCallbackHandler();

                // Android specific Low level Services observers
                mCallService.addObserver(mCallManagerCallBack);
                mConferenceService.addObserver(mCallManagerCallBack);
                mAccountService.addObserver(mConfigurationCallback);
                mHardwareService.addObserver(mVideoManagerCallback);

                mDaemonService.startDaemon(
                        mCallAndConferenceCallbackHandler,
                        mAccountCallbackHandler,
                        mHardwareCallbackHandler);

                return true;
            }
        });

        try {
            startResult.get();
        } catch (Exception e) {
            android.util.Log.e(TAG, "DRingService start failed", e);
        }
    }

    TestRingInjectionComponent getRingInjectionComponent() {
        return mTestRingInjectionComponent;
    }

}
