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
package cx.ring.application

import android.app.Activity
import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.*
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.system.Os
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import android.view.WindowManager
import androidx.core.content.getSystemService
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.bumptech.glide.Glide
import cx.ring.BuildConfig
import cx.ring.service.ConnectionService
import cx.ring.R
import cx.ring.service.DRingService
import cx.ring.service.JamiJobService
import cx.ring.service.PushForegroundService
import cx.ring.linkpreview.LinkPreview
import cx.ring.services.CallServiceImpl.Companion.CONNECTION_SERVICE_TELECOM_API_SDK_COMPATIBILITY
import cx.ring.utils.AndroidFileUtils
import cx.ring.views.AvatarFactory
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.daemon.JamiService
import net.jami.services.*
import java.io.File
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named


abstract class JamiApplication : Application() {
    companion object {
        private val TAG = JamiApplication::class.java.simpleName
        const val DRING_CONNECTION_CHANGED = BuildConfig.APPLICATION_ID + ".event.DRING_CONNECTION_CHANGE"
        const val PERMISSIONS_REQUEST = 57
        private val RINGER_FILTER = IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
        var instance: JamiApplication? = null
    }

    @Inject
    @Named("DaemonExecutor") lateinit
    var mExecutor: ScheduledExecutorService

    @Inject lateinit
    var daemon: DaemonService

    @Inject lateinit
    var mAccountService: AccountService

    @Inject lateinit
    var mNotificationService: NotificationService

    @Inject lateinit
    var mCallService: CallService

    @Inject lateinit
    var hardwareService: HardwareService

    @Inject lateinit
    var mPreferencesService: PreferencesService

    @Inject lateinit
    var mDeviceRuntimeService: DeviceRuntimeService

    @Inject lateinit
    var mContactService: ContactService

    @Inject lateinit
    var mConversationFacade: ConversationFacade

    private val ringerModeListener: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            ringerModeChanged(intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL))
        }
    }
    abstract val pushToken: Pair<String, String>?
    abstract val pushPlatform: String

    var androidPhoneAccountHandle: PhoneAccountHandle? = null

    private val bootstrapLock = Object()
    @Volatile private var bootstrapDone = false
    @Volatile private var bootstrapInProgress = false

    var isInForeground = false
        private set

    private val idleHandler = Handler(Looper.getMainLooper())
    private val idleShutdownDelayMs = 90_000L

    private val idleShutdownRunnable = Runnable {
        if (isInForeground || mPreferencesService.settings.enablePermanentService) return@Runnable
        if (mCallService.currentConferences().isNotEmpty()) return@Runnable
        if (activePushCount.get() > 0) return@Runnable
        Log.i(TAG, "Idle shutdown: deactivating all accounts (including proxy)")
        mAccountService.setAccountsActive(false, forceAll = true)
    }

    // Push lifecycle tracking. Push processing is asynchronous inside the daemon
    // (decryption, proxy reconnect, message fetch, call signaling) so we must
    // hold a WakeLock + foreground service until either a daemon event signals
    // completion or a maximum window elapses.
    private val activePushCount = AtomicInteger(0)
    private var pushWakeLock: PowerManager.WakeLock? = null
    private var pushEventDisposable: Disposable? = null
    private val pushReleaseHandler = Handler(Looper.getMainLooper())
    private val pushMaxWindowMs = 25_000L
    private val pushEventBufferMs = 5_000L

    private val pushReleaseRunnable = Runnable {
        Log.i(TAG, "Push: release window elapsed")
        releasePushHold()
    }

    open fun activityInit(activityContext: Context) {}

    private var mBound = false
    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, s: IBinder) {
            Log.d(TAG, "onServiceConnected: " + className.className)
            mBound = true
            // bootstrap Daemon
            //bootstrapDaemon();
        }

        override fun onServiceDisconnected(className: ComponentName) {
            Log.d(TAG, "onServiceDisconnected: " + className.className)
            mBound = false
        }
    }

    private fun ringerModeChanged(newMode: Int) {
        val mute = newMode == AudioManager.RINGER_MODE_VIBRATE || newMode == AudioManager.RINGER_MODE_SILENT
        mCallService.muteRingTone(mute)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        AvatarFactory.clearCache()
        Glide.get(this).clearMemory()
        LinkPreview.clearCache()
    }

    fun bootstrapDaemon() {
        if (daemon.isStarted) {
            synchronized(bootstrapLock) {
                bootstrapDone = true
                bootstrapLock.notifyAll()
            }
            return
        }
        synchronized(bootstrapLock) {
            if (bootstrapInProgress) return
            bootstrapInProgress = true
            bootstrapDone = false
        }
        Log.d(TAG, "bootstrapDaemon")
        mExecutor.execute {
            try {
                Log.d(TAG, "bootstrapDaemon: START")
                if (daemon.isStarted) {
                    synchronized(bootstrapLock) {
                        bootstrapDone = true
                        bootstrapInProgress = false
                        bootstrapLock.notifyAll()
                    }
                    return@execute
                }
                daemon.startDaemon()

                // Check if the camera hardware feature is available.
                if (mDeviceRuntimeService.hasVideoPermission()) {
                    //initVideo is called here to give time to the application to initialize hardware cameras
                    Log.d(TAG, "bootstrapDaemon: At least one camera available. Initializing video...")
                    hardwareService.initVideo()
                            .onErrorComplete()
                            .subscribe()
                } else {
                    Log.d(TAG, "bootstrapDaemon: No camera available")
                }
                ringerModeChanged((getSystemService(AUDIO_SERVICE) as AudioManager).ringerMode)
                registerReceiver(ringerModeListener, RINGER_FILTER)

                // load accounts from Daemon
                mAccountService.loadAccountsFromDaemon(mPreferencesService.hasNetworkConnected())
                if (mPreferencesService.settings.enablePushNotifications) {
                    pushToken.let { token -> if (token != null) mAccountService.setPushNotificationConfig(token.first, token.second, pushPlatform) }
                } else {
                    JamiService.setPushNotificationToken("")
                }
                sendBroadcast(Intent(DRING_CONNECTION_CHANGED).apply {
                    putExtra("connected", daemon.isStarted)
                })
                scheduleRefreshJob()
                synchronized(bootstrapLock) {
                    bootstrapDone = true
                    bootstrapInProgress = false
                    bootstrapLock.notifyAll()
                }
            } catch (e: Exception) {
                Log.e(TAG, "DRingService start failed", e)
                synchronized(bootstrapLock) {
                    bootstrapDone = true
                    bootstrapInProgress = false
                    bootstrapLock.notifyAll()
                }
            }
        }
    }

    /**
     * Ensures the daemon is fully started (accounts loaded, push token set).
     * Blocks the calling thread up to 25 seconds waiting for bootstrap to complete.
     * Safe to call from any thread except the DaemonExecutor thread.
     * @return true if daemon is ready, false if timeout or start failure
     */
    fun ensureDaemonStarted(): Boolean {
        if (daemon.isStarted) return true
        bootstrapDaemon()
        synchronized(bootstrapLock) {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(25)
            while (!bootstrapDone) {
                val remaining = deadline - System.nanoTime()
                if (remaining <= 0) return false
                try {
                    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                    (bootstrapLock as java.lang.Object).wait(remaining / 1_000_000)
                } catch (e: InterruptedException) {
                    Log.e(TAG, "ensureDaemonStarted: interrupted", e)
                    return false
                }
            }
        }
        return daemon.isStarted
    }

    /**
     * Schedules account deactivation after [IDLE_SHUTDOWN_DELAY_MS] if the app stays in background
     * with no active calls. Skipped when permanent service is enabled.
     */
    fun scheduleIdleShutdown() {
        if (mPreferencesService.settings.enablePermanentService) return
        idleHandler.removeCallbacks(idleShutdownRunnable)
        idleHandler.postDelayed(idleShutdownRunnable, idleShutdownDelayMs)
    }

    fun cancelIdleShutdown() {
        idleHandler.removeCallbacks(idleShutdownRunnable)
    }

    /**
     * Called by push handlers as soon as a push payload arrives.
     *
     * Performs all the work needed for the daemon to be able to process the push:
     *  - cancels any pending idle shutdown
     *  - acquires an app-level WakeLock (covers the full processing window,
     *    independent of the push service lifetime)
     *  - starts [PushForegroundService] so Android does not kill the process
     *  - **synchronously** reactivates accounts (otherwise the daemon would drop
     *    the push because [setAccountActive] is queued on the daemon executor)
     *  - subscribes to daemon "interesting events" (incoming call, message,
     *    swarm message, trust request) so we can release resources shortly
     *    after the push has actually been consumed, falling back to a
     *    [pushMaxWindowMs] timeout
     */
    fun onPushReceived() {
        cancelIdleShutdown()
        activePushCount.incrementAndGet()

        try {
            if (pushWakeLock == null) {
                val pm = getSystemService(POWER_SERVICE) as PowerManager
                pushWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jami:push-app").apply {
                    setReferenceCounted(false)
                    acquire(pushMaxWindowMs + 10_000L)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Can't acquire app-level push wakelock", e)
        }

        if (!isInForeground) {
            try {
                startForegroundService(Intent(this, PushForegroundService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "Can't start push foreground service", e)
            }
        }

        // Reactivate accounts BEFORE the native push dispatch happens. This must
        // be synchronous: the dispatch runs on a different thread and would
        // otherwise race with the daemon executor and the daemon would silently
        // drop the push because all accounts are still inactive.
        if (daemon.isStarted) {
            mAccountService.setAccountsActive(true, forceAll = false, awaitCompletion = true)
        }

        // (Re)arm the max-window timer.
        pushReleaseHandler.removeCallbacks(pushReleaseRunnable)
        pushReleaseHandler.postDelayed(pushReleaseRunnable, pushMaxWindowMs)

        // Subscribe to interesting daemon signals only once per release cycle.
        if (pushEventDisposable == null || pushEventDisposable?.isDisposed == true) {
            try {
                pushEventDisposable = Observable.merge(listOf(
                    mAccountService.incomingMessages.cast(Any::class.java),
                    mAccountService.incomingSwarmMessages.cast(Any::class.java),
                    mAccountService.incomingRequests.cast(Any::class.java),
                    mCallService.callsUpdates.cast(Any::class.java)
                )).subscribe({
                    Log.i(TAG, "Push: daemon event observed, will release in ${pushEventBufferMs}ms")
                    pushReleaseHandler.removeCallbacks(pushReleaseRunnable)
                    pushReleaseHandler.postDelayed(pushReleaseRunnable, pushEventBufferMs)
                }, { e -> Log.e(TAG, "Push event subscription error", e) })
            } catch (e: Exception) {
                Log.e(TAG, "Can't subscribe to push events", e)
            }
        }
    }

    /**
     * Called by push handlers after the synchronous dispatch returned.
     *
     * The native [JamiService.pushNotificationReceived] only **schedules**
     * decryption/fetch inside the daemon, so we must NOT tear anything down
     * here. Resources are released by [releasePushHold], driven by either a
     * daemon event or [pushMaxWindowMs].
     */
    fun onPushProcessed() {
        activePushCount.decrementAndGet()
    }

    private fun releasePushHold() {
        pushEventDisposable?.dispose()
        pushEventDisposable = null

        // If a new push came in while we were waiting, defer release.
        if (activePushCount.get() > 0) {
            Log.d(TAG, "Push: still ${activePushCount.get()} in flight, deferring release")
            pushReleaseHandler.postDelayed(pushReleaseRunnable, pushMaxWindowMs)
            return
        }

        try { pushWakeLock?.release() } catch (_: Exception) {}
        pushWakeLock = null

        if (!isInForeground) {
            scheduleIdleShutdown()
        }
        try {
            startForegroundService(Intent(this, PushForegroundService::class.java)
                .setAction(PushForegroundService.ACTION_PUSH_DONE))
        } catch (e: Exception) {
            Log.w(TAG, "Can't signal push done to foreground service", e)
        }
    }

    private fun scheduleRefreshJob() {
        Log.w(TAG, "JobScheduler: scheduling job")
        getSystemService(JobScheduler::class.java)
            .schedule(JobInfo.Builder(JamiJobService.JOB_ID, ComponentName(this, JamiJobService::class.java))
                .setPersisted(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(JamiJobService.JOB_INTERVAL, JamiJobService.JOB_FLEX)
                .build())
    }

    private fun terminateDaemon() {
        val stopResult = mExecutor.submit<Boolean> {
            unregisterReceiver(ringerModeListener)
            daemon.stopDaemon()
            val intent = Intent(DRING_CONNECTION_CHANGED)
            intent.putExtra("connected", daemon.isStarted)
            sendBroadcast(intent)
            true
        }
        try {
            stopResult.get()
            mExecutor.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "DRingService stop failed", e)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        LinkPreview.init(this)

        // Launch logging if previously set up by user (info is stored in shared preferences).
        // Subscribe on it (first element) to initialize pipe construction.
        if (hardwareService.mPreferenceService.isLogActive)
            hardwareService.startLogs().firstElement().subscribe()

        if (!BuildConfig.DEBUG) {
            // Set a default exception handler for RxJava.
            // Most of these errors bubble up here because the original Rx flow was normally disposed, and can be
            // safely ignored. In some cases this might hide real bugs so we only do that in production.
            RxJavaPlugins.setErrorHandler { e -> Log.e(TAG, "Unhandled RxJava error", e) }
        }

        // Initialize the Android Telecom API if available
        if (Build.VERSION.SDK_INT >= CONNECTION_SERVICE_TELECOM_API_SDK_COMPATIBILITY) {
            Schedulers.computation().scheduleDirect {
                getSystemService<TelecomManager>()?.let { telecomService ->
                    try {
                        val componentName = ComponentName(this, ConnectionService::class.java)
                        val handle = PhoneAccountHandle(componentName, ConnectionService.HANDLE_ID)
                        //telecomService.unregisterPhoneAccount(handle)
                        telecomService.registerPhoneAccount(
                            PhoneAccount.Builder(handle, getString(R.string.app_name))
                                .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED)
                                //.setCapabilities(PhoneAccount.CAPABILITY_SUPPORTS_VIDEO_CALLING)
                                .setHighlightColor(getColor(R.color.color_primary_dark))
                                .addSupportedUriScheme("ring")
                                .addSupportedUriScheme("jami")
                                .addSupportedUriScheme("swarm")
                                .addSupportedUriScheme(PhoneAccount.SCHEME_SIP)
                                .build())
                        androidPhoneAccountHandle = handle
                        Log.d(TAG, "Registered Telecom API with handle $handle")
                    } catch (e: Exception) {
                        Log.e(TAG, "Can't register the Telecom API", e)
                    }
                }
            }
        }

        bootstrapDaemon()
        mPreferencesService.loadDarkMode()

        // Track app foreground/background state for battery optimization.
        // When the app goes to background with no active calls (and permanent service
        // is disabled), we schedule deactivation of all accounts after a grace period
        // to close network connections and save battery.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                isInForeground = true
                cancelIdleShutdown()
                if (daemon.isStarted) {
                    mAccountService.setAccountsActive(true)
                }
            }
            override fun onStop(owner: LifecycleOwner) {
                isInForeground = false
                if (mCallService.currentConferences().isEmpty()
                    && !mPreferencesService.settings.enablePermanentService) {
                    scheduleIdleShutdown()
                }
            }
        })
        Completable.fromAction {
            val caRootFile = getString(R.string.ca_root_file)
            val dest = File(filesDir, caRootFile)
            AndroidFileUtils.copyAsset(assets, caRootFile, dest)
            Os.setenv("CA_ROOT_FILE", dest.absolutePath, true)

            val path = AndroidFileUtils.ringtonesPath(this)
            val defaultRingtone = File(path, getString(R.string.ringtone_default_name))
            val defaultLink = File(path, "default.opus")
            if (!defaultRingtone.exists()) {
                AndroidFileUtils.copyAssetFolder(assets, "ringtones", path)
            }
            if (!defaultLink.exists()) {
                AndroidFileUtils.linkOrCopy(defaultRingtone.absolutePath, defaultLink.absolutePath)
            }
        }
            .subscribeOn(Schedulers.io())
            .subscribe()
        setupActivityListener()
    }

    fun startDaemon(activityContext: Context) {
        if (!DRingService.isRunning) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        && mPreferencesService.settings.enablePermanentService) {
                    startForegroundService(Intent(this, DRingService::class.java))
                } else {
                    startService(Intent(this, DRingService::class.java))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error starting daemon service")
            }
        }
        bindDaemon()
        activityInit(activityContext)
    }

    fun bindDaemon() {
        if (!mBound) {
            try {
                bindService(Intent(this, DRingService::class.java), mConnection, BIND_AUTO_CREATE or BIND_IMPORTANT or BIND_ABOVE_CLIENT)
            } catch (e: Exception) {
                Log.w(TAG, "Error binding daemon service")
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        terminateDaemon()
        instance = null
    }

    private fun setupActivityListener() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
                if (mPreferencesService.settings.isRecordingBlocked) {
                    activity.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}