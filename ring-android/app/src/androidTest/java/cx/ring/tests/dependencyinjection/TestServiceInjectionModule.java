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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Singleton;

import cx.ring.services.AccountService;
import cx.ring.services.CallService;
import cx.ring.services.ConferenceService;
import cx.ring.services.DaemonService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
import cx.ring.services.HistoryService;
import cx.ring.services.LogService;
import cx.ring.services.SettingsService;
import cx.ring.tests.services.DeviceRuntimeServiceImpl;
import cx.ring.tests.services.HistoryServiceImpl;
import cx.ring.tests.services.LogServiceImpl;
import cx.ring.tests.services.SettingsServiceImpl;
import dagger.Module;
import dagger.Provides;

@Module
public class TestServiceInjectionModule {

    TestApplication mTestApplication;

    public TestServiceInjectionModule(TestApplication app) {
        mTestApplication = app;
    }

    @Provides
    @Singleton
    LogService provideLogService() {
        return new LogServiceImpl();
    }

    @Provides
    @Singleton
    DeviceRuntimeService provideDeviceRuntimeService() {
        DeviceRuntimeServiceImpl runtimeService = new DeviceRuntimeServiceImpl();
        mTestApplication.getRingInjectionComponent().inject(runtimeService);
        runtimeService.loadNativeLibrary();
        return runtimeService;
    }

    @Provides
    @Singleton
    SettingsService provideSettingsService() {
        SettingsServiceImpl settingsService = new SettingsServiceImpl();
        mTestApplication.getRingInjectionComponent().inject(settingsService);
        return settingsService;
    }

    @Provides
    @Singleton
    HistoryService provideHistoryService() {
        HistoryServiceImpl historyService = new HistoryServiceImpl();
        mTestApplication.getRingInjectionComponent().inject(historyService);
        return historyService;
    }

    @Provides
    @Singleton
    DaemonService provideDaemonService(DeviceRuntimeService deviceRuntimeService) {
        DaemonService daemonService = new DaemonService();
        mTestApplication.getRingInjectionComponent().inject(daemonService);
        return daemonService;
    }

    @Provides
    @Singleton
    CallService provideCallService(DaemonService daemonService) {
        CallService callService = new CallService();
        mTestApplication.getRingInjectionComponent().inject(callService);
        return callService;
    }

    @Provides
    @Singleton
    ConferenceService provideConferenceService(DaemonService daemonService) {
        ConferenceService conferenceService = new ConferenceService();
        mTestApplication.getRingInjectionComponent().inject(conferenceService);
        return conferenceService;
    }

    @Provides
    @Singleton
    AccountService provideAccountService(DaemonService daemonService) {
        AccountService accountService = new AccountService();
        mTestApplication.getRingInjectionComponent().inject(accountService);
        return accountService;
    }

    @Provides
    @Singleton
    HardwareService provideHardwareService(DaemonService daemonService) {
        HardwareService hardwareService = new HardwareService();
        mTestApplication.getRingInjectionComponent().inject(hardwareService);
        return hardwareService;
    }

    @Provides
    @Singleton
    ExecutorService provideExecutorService() {
        return Executors.newSingleThreadExecutor();
    }

    @Provides
    @Singleton
    ScheduledExecutorService provideScheduledExecutorService() {
        return Executors.newSingleThreadScheduledExecutor();
    }
}
