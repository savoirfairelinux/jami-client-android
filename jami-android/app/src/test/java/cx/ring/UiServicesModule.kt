package cx.ring

import cx.ring.dependencyinjection.ServiceInjectionModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler
import net.jami.services.AccountService
import net.jami.services.CallService
import net.jami.services.CollaborationService
import net.jami.services.ContactService
import net.jami.services.ConversationFacade
import net.jami.services.DaemonService
import net.jami.services.DeviceRuntimeService
import net.jami.services.ExposedServicesService
import net.jami.services.HardwareService
import net.jami.services.HistoryService
import net.jami.services.LogService
import net.jami.services.NotificationService
import net.jami.services.PeerServicesService
import net.jami.services.PreferencesService
import net.jami.services.VCardService
import org.mockito.Mockito.mock
import java.util.concurrent.ScheduledExecutorService
import javax.inject.Named
import javax.inject.Singleton

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [ServiceInjectionModule::class])
object UiServicesModule {
    @Provides @Singleton
    fun accounts(): AccountService = mock(AccountService::class.java)

    @Provides @Singleton
    fun calls(): CallService = mock(CallService::class.java)

    @Provides @Singleton
    fun collaboration(): CollaborationService = mock(CollaborationService::class.java)

    @Provides @Singleton
    fun contacts(): ContactService = mock(ContactService::class.java)

    @Provides @Singleton
    fun conversations(): ConversationFacade = mock(ConversationFacade::class.java)

    @Provides @Singleton
    fun daemon(): DaemonService = mock(DaemonService::class.java)

    @Provides @Singleton
    fun device(): DeviceRuntimeService = mock(DeviceRuntimeService::class.java)

    @Provides @Singleton
    fun exposedServices(): ExposedServicesService = mock(ExposedServicesService::class.java)

    @Provides @Singleton
    fun hardware(): HardwareService = mock(HardwareService::class.java)

    @Provides @Singleton
    fun history(): HistoryService = mock(HistoryService::class.java)

    @Provides @Singleton
    fun logs(): LogService = mock(LogService::class.java)

    @Provides @Singleton
    fun notifications(): NotificationService = mock(NotificationService::class.java)

    @Provides @Singleton
    fun peers(): PeerServicesService = mock(PeerServicesService::class.java)

    @Provides @Singleton
    fun preferences(): PreferencesService = mock(PreferencesService::class.java)

    @Provides @Singleton
    fun profiles(): VCardService = mock(VCardService::class.java)

    @Provides @Singleton @Named("DaemonExecutor")
    fun executor(): ScheduledExecutorService = mock(ScheduledExecutorService::class.java)

    @Provides @Singleton @Named("UiScheduler")
    fun uiScheduler(): Scheduler = AndroidSchedulers.mainThread()
}