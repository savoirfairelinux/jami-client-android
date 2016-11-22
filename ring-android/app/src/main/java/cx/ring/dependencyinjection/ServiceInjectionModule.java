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

import javax.inject.Singleton;

import cx.ring.application.RingApplication;
import cx.ring.services.CallService;
import cx.ring.services.ConferenceService;
import cx.ring.services.DaemonService;
import cx.ring.services.DaemonServiceImpl;
import cx.ring.services.HistoryService;
import cx.ring.services.HistoryServiceImpl;
import cx.ring.services.LogService;
import cx.ring.services.LogServiceImpl;
import cx.ring.services.SettingsService;
import cx.ring.services.SettingsServiceImpl;
import cx.ring.services.StateService;
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
    StateService provideStateService() {
        return new StateService();
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
        return new LogServiceImpl();
    }

    @Provides
    @Singleton
    DaemonService provideDaemonService() {
        DaemonServiceImpl daemonService = new DaemonServiceImpl();
        mRingApplication.getRingInjectionComponent().inject(daemonService);
        daemonService.loadNativeLibrary();
        return daemonService;
    }

    @Provides
    @Singleton
    CallService provideCallService(DaemonService daemonService) {
        CallService callService = new CallService(daemonService);
        mRingApplication.getRingInjectionComponent().inject(callService);
        return callService;
    }

    @Provides
    @Singleton
    ConferenceService provideConferenceService(DaemonService daemonService) {
        ConferenceService conferenceService = new ConferenceService(daemonService);
        mRingApplication.getRingInjectionComponent().inject(conferenceService);
        return conferenceService;
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
