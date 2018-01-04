/*
 *  Copyright (C) 2016-2017 Savoir-faire Linux Inc.
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Named;
import javax.inject.Singleton;

import cx.ring.application.RingApplication;
import cx.ring.facades.ConversationFacade;
import cx.ring.services.AccountService;
import cx.ring.services.CallService;
import cx.ring.services.ConferenceService;
import cx.ring.services.ContactService;
import cx.ring.services.ContactServiceImpl;
import cx.ring.services.DaemonService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.DeviceRuntimeServiceImpl;
import cx.ring.services.HardwareService;
import cx.ring.services.HardwareServiceImpl;
import cx.ring.services.HistoryService;
import cx.ring.services.HistoryServiceImpl;
import cx.ring.services.LogService;
import cx.ring.services.LogServiceImpl;
import cx.ring.services.NotificationService;
import cx.ring.services.NotificationServiceImpl;
import cx.ring.services.PreferencesService;
import cx.ring.services.PresenceService;
import cx.ring.services.SharedPreferencesServiceImpl;
import cx.ring.services.VCardService;
import cx.ring.services.VCardServiceImpl;
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
    PreferencesService provideSettingsService() {
        SharedPreferencesServiceImpl settingsService = new SharedPreferencesServiceImpl();
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
    NotificationService provideNotificationService() {
        NotificationServiceImpl service = new NotificationServiceImpl();
        mRingApplication.getRingInjectionComponent().inject(service);
        service.initHelper();
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
        DaemonService daemonService = new DaemonService(deviceRuntimeService);
        mRingApplication.getRingInjectionComponent().inject(daemonService);
        return daemonService;
    }

    @Provides
    @Singleton
    CallService provideCallService() {
        CallService callService = new CallService();
        mRingApplication.getRingInjectionComponent().inject(callService);
        return callService;
    }

    @Provides
    @Singleton
    ConferenceService provideConferenceService() {
        ConferenceService conferenceService = new ConferenceService();
        mRingApplication.getRingInjectionComponent().inject(conferenceService);
        return conferenceService;
    }

    @Provides
    @Singleton
    AccountService provideAccountService() {
        AccountService accountService = new AccountService();
        mRingApplication.getRingInjectionComponent().inject(accountService);
        return accountService;
    }

    @Provides
    @Singleton
    HardwareService provideHardwareService(Context context) {
        HardwareServiceImpl hardwareService = new HardwareServiceImpl(context);
        mRingApplication.getRingInjectionComponent().inject(hardwareService);
        return hardwareService;
    }

    @Provides
    @Singleton
    ContactService provideContactService(PreferencesService sharedPreferencesService) {
        ContactServiceImpl contactService = new ContactServiceImpl();
        mRingApplication.getRingInjectionComponent().inject(contactService);
        return contactService;
    }

    @Provides
    @Singleton
    PresenceService providePresenceService() {
        PresenceService presenceService = new PresenceService();
        mRingApplication.getRingInjectionComponent().inject(presenceService);
        return presenceService;
    }

    @Provides
    @Singleton
    ConversationFacade provideConversationFacade(
            ContactService contactService,
            HistoryService historyService,
            CallService callService,
            AccountService accountService) {
        ConversationFacade conversationFacade = new ConversationFacade(historyService, callService, contactService, accountService);
        mRingApplication.getRingInjectionComponent().inject(conversationFacade);
        return conversationFacade;
    }

    @Provides
    @Singleton
    VCardService provideVCardService(Context context) {
        return new VCardServiceImpl(context);
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
