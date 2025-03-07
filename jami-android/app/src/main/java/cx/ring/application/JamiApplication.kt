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
import android.os.IBinder
import android.system.Os
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import android.view.WindowManager
import androidx.core.content.getSystemService
import com.bumptech.glide.Glide
import cx.ring.BuildConfig
import cx.ring.service.ConnectionService
import cx.ring.R
import cx.ring.service.DRingService
import cx.ring.service.JamiJobService
import cx.ring.services.CallServiceImpl.Companion.CONNECTION_SERVICE_TELECOM_API_SDK_COMPATIBILITY
import cx.ring.utils.AndroidFileUtils
import cx.ring.views.AvatarFactory
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.daemon.JamiService
import net.jami.services.*
import java.io.File
import java.util.concurrent.ScheduledExecutorService
import javax.inject.Inject
import javax.inject.Named


abstract class JamiApplication : Application() {
    companion object {
        private val TAG = JamiApplication::class.java.simpleName
        const val DRING_CONNECTION_CHANGED = BuildConfig.APPLICATION_ID + ".event.DRING_CONNECTION_CHANGE"
        const val PERMISSIONS_REQUEST = 57
        private val RINGER_FILTER = IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
        var instance: JamiApplication? = null
        var eventServiceInstance: EventService? = null
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

    @Inject lateinit
    var eventService: EventService

    private val ringerModeListener: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            ringerModeChanged(intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL))
        }
    }
    abstract val pushToken: String
    abstract val pushPlatform: String

    var androidPhoneAccountHandle: PhoneAccountHandle? = null

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
    }

    fun bootstrapDaemon() {
        if (daemon.isStarted) {
            return
        }
        Log.d(TAG, "bootstrapDaemon")
        mExecutor.execute {
            try {
                Log.d(TAG, "bootstrapDaemon: START")
                if (daemon.isStarted) {
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
                    pushToken.let { token -> if (!token.isEmpty()) JamiService.setPushNotificationToken(token) }
                } else {
                    JamiService.setPushNotificationToken("")
                }
                sendBroadcast(Intent(DRING_CONNECTION_CHANGED).apply {
                    putExtra("connected", daemon.isStarted)
                })
                scheduleRefreshJob()
            } catch (e: Exception) {
                Log.e(TAG, "DRingService start failed", e)
            }
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
        eventServiceInstance = eventService

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