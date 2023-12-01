/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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
package cx.ring.service

import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.*
import android.net.ConnectivityManager.NetworkCallback
import android.os.*
import android.provider.ContactsContract
import android.util.Log
import androidx.core.app.RemoteInput
import androidx.legacy.content.WakefulBroadcastReceiver
import cx.ring.BuildConfig
import cx.ring.application.JamiApplication
import cx.ring.client.CallActivity
import cx.ring.client.ConversationActivity
import cx.ring.utils.ConversationPath
import cx.ring.utils.DebugUtils
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Conversation
import net.jami.model.Settings
import net.jami.services.*
import javax.inject.Inject
import javax.inject.Singleton


@AndroidEntryPoint
class DRingService : Service() {
    private val contactContentObserver = ContactsContentObserver()

    @Inject
    @Singleton
    lateinit var mDaemonService: DaemonService

    @Inject
    @Singleton
    lateinit var mCallService: CallService

    @Inject
    @Singleton
    lateinit var mAccountService: AccountService

    @Inject
    @Singleton
    lateinit var mHardwareService: HardwareService

    @Inject
    @Singleton
    lateinit var mHistoryService: HistoryService

    @Inject
    @Singleton
    lateinit var mDeviceRuntimeService: DeviceRuntimeService

    @Inject
    @Singleton
    lateinit var mNotificationService: NotificationService

    @Inject
    @Singleton
    lateinit var mContactService: ContactService

    @Inject
    @Singleton
    lateinit var mPreferencesService: PreferencesService

    @Inject
    @Singleton
    lateinit var mConversationFacade: ConversationFacade

    private val mHandler = Handler(Looper.myLooper()!!)
    private val mDisposableBag = CompositeDisposable()
    private val mConnectivityChecker = Runnable { updateConnectivityState() }

    private val monitor = object : NetworkCallback() {
        private val networkRequest: NetworkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
            //.addTransportType(NetworkCapabilities.TRANSPORT_USB)
            .build()

        fun enable(context: Context) {
            val connectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager?
            connectivityManager?.registerNetworkCallback(networkRequest, this)
        }

        fun disable(context: Context) {
            val connectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager?
            connectivityManager?.unregisterNetworkCallback(this)
        }

        override fun onAvailable(network: Network) {
            Log.w(TAG, "onAvailable ${network}")
            updateConnectivityState(true)
        }

        override fun onUnavailable() {
            Log.w(TAG, "onUnavailable")
            updateConnectivityState(false)
        }
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == null) {
                Log.w(TAG, "onReceive: received a null action on broadcast receiver")
                return
            }
            Log.d(TAG, "receiver.onReceive: $action")
            when (action) {
                ConnectivityManager.CONNECTIVITY_ACTION -> {
                    updateConnectivityState()
                }
                PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED -> {
                    mConnectivityChecker.run()
                    mHandler.postDelayed(mConnectivityChecker, 100)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        DebugUtils.appendDebugPut(applicationContext, "$TAG:onCreate()")
        Log.i(TAG, "onCreate")
        isRunning = true
        if (mDeviceRuntimeService.hasContactPermission()) {
            contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contactContentObserver)
        }
        val intentFilter = IntentFilter().apply {
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
        }
        registerReceiver(receiver, intentFilter)
        updateConnectivityState()
        mDisposableBag.add(mPreferencesService.settingsSubject.subscribe { settings: Settings ->
            showSystemNotification(settings)
        })
        monitor.enable(this)
        JamiApplication.instance!!.apply {
            bindDaemon()
            bootstrapDaemon()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy()")
        unregisterReceiver(receiver)
        contentResolver.unregisterContentObserver(contactContentObserver)
        monitor.disable(this)
        mHardwareService.unregisterCameraDetectionCallback()
        mDisposableBag.clear()
        isRunning = false
    }

    private fun showSystemNotification(settings: Settings) {
        try {
            if (settings.enablePermanentService) {
                startForeground(NOTIFICATION_ID, mNotificationService.serviceNotification as Notification)
            } else {
                stopForeground(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Can't start or stop foreground service: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Log.i(TAG, "onStartCommand " + (intent == null ? "null" : intent.getAction()) + " " + flags + " " + startId);
        if (intent != null) {
            parseIntent(intent)
            WakefulBroadcastReceiver.completeWakefulIntent(intent)
        }
        return START_STICKY /* started and stopped explicitly */
    }

    private val binder: IBinder = Binder()
    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    /* ************************************
     *
     * Implement public interface for the service
     *
     * *********************************
     */
    private fun updateConnectivityState() {
        updateConnectivityState(mPreferencesService.hasNetworkConnected())
    }

    private fun updateConnectivityState(isConnected: Boolean) {
        if (mDaemonService.isStarted) {
            mAccountService.setAccountsActive(isConnected)
            mHardwareService.connectivityChanged(isConnected)
        }
    }

    private fun parseIntent(intent: Intent) {
        val action = intent.action ?: return
        val extras = intent.extras
        when (action) {
            ACTION_TRUST_REQUEST_ACCEPT, ACTION_TRUST_REQUEST_REFUSE, ACTION_TRUST_REQUEST_BLOCK ->
                handleTrustRequestAction(intent.data, action)
            ACTION_CALL_ACCEPT, ACTION_CALL_REFUSE, ACTION_CALL_END -> extras?.let {
                handleCallAction(action, it)
            }
            ACTION_CONV_READ, ACTION_CONV_ACCEPT, ACTION_CONV_DISMISS, ACTION_CONV_REPLY_INLINE ->
                handleConvAction(intent, action, extras)
            ACTION_FILE_ACCEPT, ACTION_FILE_CANCEL -> extras?.let {
                handleFileAction(intent.data, action, it)
            }
        }
    }

    private fun handleFileAction(uri: Uri?, action: String, extras: Bundle) {
        Log.w(TAG, "handleFileAction $extras")
        val messageId = extras.getString(KEY_MESSAGE_ID)
        val id = extras.getString(KEY_TRANSFER_ID)!!
        val path = ConversationPath.fromUri(uri)!!
        if (action == ACTION_FILE_ACCEPT) {
            mNotificationService.removeTransferNotification(path.accountId, path.conversationUri, id)
            mAccountService.acceptFileTransfer(path.accountId, path.conversationUri, messageId, id)
        } else if (action == ACTION_FILE_CANCEL) {
            mConversationFacade.cancelFileTransfer(path.accountId, path.conversationUri, messageId, id)
        }
    }

    private fun handleTrustRequestAction(uri: Uri?, action: String) {
        ConversationPath.fromUri(uri)?.let { path ->
            mNotificationService.cancelTrustRequestNotification(path.accountId)
            when (action) {
                ACTION_TRUST_REQUEST_ACCEPT -> mConversationFacade.acceptRequest(path.accountId, path.conversationUri)
                ACTION_TRUST_REQUEST_REFUSE -> mConversationFacade.discardRequest(path.accountId, path.conversationUri)
                ACTION_TRUST_REQUEST_BLOCK -> {
                    mConversationFacade.discardRequest(path.accountId, path.conversationUri)
                    mConversationFacade.banConversation(path.accountId, path.conversationUri)
                }
            }
        }
    }

    private fun handleCallAction(action: String, extras: Bundle) {
        val callId = extras.getString(NotificationService.KEY_CALL_ID) ?: return
        val accountId = extras.getString(ConversationPath.KEY_ACCOUNT_ID) ?: return
        if (callId.isEmpty() || accountId.isEmpty()) {
            return
        }
        when (action) {
            ACTION_CALL_ACCEPT -> {
                startActivity(Intent(ACTION_CALL_ACCEPT)
                        .putExtras(extras)
                        .setClass(applicationContext, CallActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            ACTION_CALL_REFUSE -> {
                mCallService.refuse(accountId, callId)
                mHardwareService.closeAudioState()
            }
            ACTION_CALL_END -> {
                mCallService.hangUpAny(accountId, callId)
                mHardwareService.closeAudioState()
            }
        }
    }

    private fun handleConvAction(intent: Intent, action: String, extras: Bundle?) {
        val path = ConversationPath.fromIntent(intent)
        if (path == null || path.conversationId.isEmpty()) {
            return
        }
        when (action) {
            ACTION_CONV_READ -> mConversationFacade.readMessages(path.accountId, path.conversationUri)
            ACTION_CONV_DISMISS -> {
                extras?.getString(KEY_MESSAGE_ID)?.let { messageId ->
                    mConversationFacade.messageNotified(path.accountId, path.conversationUri, messageId)
                }
            }
            ACTION_CONV_REPLY_INLINE -> {
                val remoteInput = RemoteInput.getResultsFromIntent(intent)
                if (remoteInput != null) {
                    val reply = remoteInput.getCharSequence(KEY_TEXT_REPLY)
                    if (!reply.isNullOrEmpty()) {
                        val uri = path.conversationUri
                        val message = reply.toString()
                        mConversationFacade.startConversation(path.accountId, uri)
                            .flatMapCompletable { c: Conversation ->
                                mConversationFacade.sendTextMessage(c, uri, message)
                                    .doOnComplete { mNotificationService.showTextNotification(c)}
                            }
                            .subscribe()
                    }
                }
            }
            ACTION_CONV_ACCEPT -> startActivity(Intent(Intent.ACTION_VIEW, path.toUri(), applicationContext, ConversationActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            else -> {
            }
        }
    }

    fun refreshContacts() {
        if (mAccountService.currentAccount == null) {
            return
        }
        mContactService.loadContacts(
            mAccountService.hasJamiAccount(),
            mAccountService.hasSipAccount(),
            mAccountService.currentAccount
        )
    }

    private class ContactsContentObserver internal constructor() : ContentObserver(null) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            //mContactService.loadContacts(mAccountService.hasRingAccount(), mAccountService.hasSipAccount(), mAccountService.getCurrentAccount());
        }
    }

    companion object {
        private val TAG = DRingService::class.java.simpleName
        const val ACTION_TRUST_REQUEST_ACCEPT = BuildConfig.APPLICATION_ID + ".action.TRUST_REQUEST_ACCEPT"
        const val ACTION_TRUST_REQUEST_REFUSE = BuildConfig.APPLICATION_ID + ".action.TRUST_REQUEST_REFUSE"
        const val ACTION_TRUST_REQUEST_BLOCK = BuildConfig.APPLICATION_ID + ".action.TRUST_REQUEST_BLOCK"
        const val ACTION_CALL_ACCEPT = BuildConfig.APPLICATION_ID + ".action.CALL_ACCEPT"
        const val ACTION_CALL_REFUSE = BuildConfig.APPLICATION_ID + ".action.CALL_REFUSE"
        const val ACTION_CALL_END = BuildConfig.APPLICATION_ID + ".action.CALL_END"
        const val ACTION_CONV_READ = BuildConfig.APPLICATION_ID + ".action.CONV_READ"
        const val ACTION_CONV_DISMISS = BuildConfig.APPLICATION_ID + ".action.CONV_DISMISS"
        const val ACTION_CONV_ACCEPT = BuildConfig.APPLICATION_ID + ".action.CONV_ACCEPT"
        const val ACTION_CONV_REPLY_INLINE = BuildConfig.APPLICATION_ID + ".action.CONV_REPLY"
        const val ACTION_FILE_ACCEPT = BuildConfig.APPLICATION_ID + ".action.FILE_ACCEPT"
        const val ACTION_FILE_CANCEL = BuildConfig.APPLICATION_ID + ".action.FILE_CANCEL"
        const val KEY_MESSAGE_ID = "messageId"
        const val KEY_TRANSFER_ID = "transferId"
        const val KEY_TEXT_REPLY = "textReply"
        private const val NOTIFICATION_ID = 1
        var isRunning = false
    }
}