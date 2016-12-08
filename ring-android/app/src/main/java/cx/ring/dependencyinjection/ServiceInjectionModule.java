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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Named;
import javax.inject.Singleton;

import cx.ring.application.RingApplication;
import cx.ring.services.AccountService;
import cx.ring.services.CallService;
import cx.ring.services.ConferenceService;
import cx.ring.services.DaemonService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.DeviceRuntimeServiceImpl;
import cx.ring.services.HardwareService;
import cx.ring.services.HistoryService;
import cx.ring.services.HistoryServiceImpl;
import cx.ring.services.LogService;
import cx.ring.services.LogServiceImpl;
import cx.ring.services.SettingsService;
import cx.ring.services.SettingsServiceImpl;
import cx.ring.utils.Log;
import dagger.Module;
import dagger.Provides;

@Module
public class ServiceInjectionModule {

    RingApplication mRingApplication;

    public ServiceInjectionModule(RingApplication app) {
        mRingApplication = app;
    }

    @Provides
    @Singleton
    SettingsService provideSettingsService() {
        SettingsServiceImpl settingsService = new SettingsServiceImpl();
        mRingApplication.getRingInjectionComponent().inject(settingsService);
        return settingsService;
    }

    @Provides
    @Singleton
    HistoryService provideHistoryService() {
        HistoryServiceImpl historyService = new HistoryServiceImpl();
        mRingApplication.getRingInjectionComponent().inject(historyService);
        historyService.initHelper();
        return historyService;
    }

    @Provides
    @Singleton
    LogService provideLogService() {
        LogService service = new LogServiceImpl();
        Log.injectLogService(service);
        return service;
    }

    @Provides
    @Singleton
    DeviceRuntimeService provideDeviceRuntimeService(LogService logService) {
        DeviceRuntimeServiceImpl runtimeService = new DeviceRuntimeServiceImpl();
        mRingApplication.getRingInjectionComponent().inject(runtimeService);
        runtimeService.loadNativeLibrary();
        return runtimeService;
    }

    @Provides
    @Singleton
    DaemonService provideDaemonService(DeviceRuntimeService deviceRuntimeService) {
        DaemonService daemonService = new DaemonService();
        mRingApplication.getRingInjectionComponent().inject(daemonService);
        return daemonService;
    }

    @Provides
    @Singleton
    CallService provideCallService(DaemonService daemonService) {
        CallService callService = new CallService();
        mRingApplication.getRingInjectionComponent().inject(callService);
        return callService;
    }

    @Provides
    @Singleton
    ConferenceService provideConferenceService(DaemonService daemonService) {
        ConferenceService conferenceService = new ConferenceService();
        mRingApplication.getRingInjectionComponent().inject(conferenceService);
        return conferenceService;
    }

    @Provides
    @Singleton
    AccountService provideAccountService(DaemonService daemonService) {
        AccountService accountService = new AccountService();
        mRingApplication.getRingInjectionComponent().inject(accountService);
        return accountService;
    }

    @Provides
    @Singleton
    HardwareService provideHardwareService(DaemonService daemonService) {
        HardwareService hardwareService = new HardwareService();
        mRingApplication.getRingInjectionComponent().inject(hardwareService);
        return hardwareService;
    }

    @Provides
    @Named("DaemonExecutor")
    @Singleton
    ExecutorService provideDaemonExecutorService() {
        return Executors.newSingleThreadExecutor();
    }

    @Provides
    @Named("ApplicationExecutor")
    @Singleton
    ExecutorService provideApplicationExecutorService() {
        return Executors.newFixedThreadPool(5);
    }

    @Provides
    @Singleton
    ScheduledExecutorService provideScheduledExecutorService() {
        return Executors.newSingleThreadScheduledExecutor();
    }
}
