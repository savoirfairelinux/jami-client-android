/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.dependencyinjection

import android.content.Context
import cx.ring.services.*
import cx.ring.utils.DeviceUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.reactivex.rxjava3.core.Scheduler
import net.jami.services.ConversationFacade
import net.jami.services.*
import net.jami.utils.Log
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceInjectionModule {
    @Provides
    @Singleton
    fun provideSettingsService(@ApplicationContext appContext: Context, accountService: AccountService, deviceService: DeviceRuntimeService): PreferencesService {
        return SharedPreferencesServiceImpl(appContext, accountService, deviceService)
    }

    @Provides
    @Singleton
    fun provideHistoryService(@ApplicationContext appContext: Context): HistoryService {
        return HistoryServiceImpl(appContext)
    }

    @Provides
    @Singleton
    fun provideLogService(): LogService {
        val service: LogService = LogServiceImpl()
        Log.injectLogService(service)
        return service
    }

    @Provides
    @Singleton
    fun provideNotificationService(@ApplicationContext appContext: Context, accountService: AccountService,
                                   contactService: ContactService,
                                   preferencesService: PreferencesService,
                                   deviceRuntimeService: DeviceRuntimeService,
                                   callService: CallService): NotificationService {
        return NotificationServiceImpl(appContext, accountService, contactService, preferencesService, deviceRuntimeService, callService)
    }

    @Provides
    @Singleton
    fun provideDeviceRuntimeService(
        @ApplicationContext appContext: Context, @Named("DaemonExecutor") executor: ScheduledExecutorService, logService: LogService
    ): DeviceRuntimeService {
        val runtimeService = DeviceRuntimeServiceImpl(appContext, executor, logService)
        runtimeService.loadNativeLibrary()
        return runtimeService
    }

    @Provides
    @Singleton
    fun provideDaemonService(deviceRuntimeService: DeviceRuntimeService,
                             @Named("DaemonExecutor") executor: ScheduledExecutorService,
                             callService: CallService,
                             hardwareService: HardwareService,
                             accountService: AccountService): DaemonService {
        return DaemonService(deviceRuntimeService, executor, callService, hardwareService, accountService)
    }

    @Provides
    @Singleton
    fun provideCallService(@ApplicationContext appContext: Context,
                           @Named("DaemonExecutor") executor : ScheduledExecutorService,
                           contactService: ContactService,
                           accountService: AccountService,
                           deviceRuntimeService: DeviceRuntimeService): CallService {
        return CallServiceImpl(appContext, executor, contactService, accountService, deviceRuntimeService)
    }

    @Provides
    @Singleton
    fun provideAccountService(@Named("DaemonExecutor") executor : ScheduledExecutorService,
                              historyService : HistoryService,
                              deviceRuntimeService : DeviceRuntimeService,
                              vCardService : VCardService): AccountService {
        return AccountService(executor, historyService, deviceRuntimeService, vCardService)
    }

    @Provides
    @Singleton
    fun provideHardwareService(@ApplicationContext appContext: Context,
                               @Named("DaemonExecutor") executor : ScheduledExecutorService,
                               preferenceService: PreferencesService,
                               @Named("UiScheduler") uiScheduler: Scheduler): HardwareService {
        return HardwareServiceImpl(appContext, executor, preferenceService, uiScheduler)
    }

    @Provides
    @Singleton
    fun provideContactService(@ApplicationContext appContext: Context,
                              preferenceService: PreferencesService,
                              accountService: AccountService): ContactService {
        return ContactServiceImpl(appContext, preferenceService, accountService)
    }

    @Provides
    @Singleton
    fun provideConversationFacade(
        historyService: HistoryService,
        callService: CallService,
        contactService: ContactService,
        accountService: AccountService,
        notificationService: NotificationService,
        hardwareService: HardwareService,
        deviceRuntimeService: DeviceRuntimeService,
        preferencesService: PreferencesService
    ): ConversationFacade {
        return ConversationFacade(
            historyService,
            callService,
            accountService,
            contactService,
            notificationService,
            hardwareService,
            deviceRuntimeService,
            preferencesService
        )
    }

    @Provides
    @Singleton
    fun provideVCardService(@ApplicationContext appContext: Context): VCardService {
        return VCardServiceImpl(appContext)
    }

    @Provides
    @Named("DaemonExecutor")
    @Singleton
    fun provideDaemonExecutorService(): ScheduledExecutorService {
        return Executors.newSingleThreadScheduledExecutor { r: Runnable? -> Thread(r, "DRing") }
    }

    @Provides
    @Named("UiScheduler")
    @Singleton
    fun provideUiScheduler(): Scheduler {
        return DeviceUtils.uiScheduler
    }
}