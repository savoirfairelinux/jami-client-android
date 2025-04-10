package cx.ring.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import cx.ring.IRemoteService
import cx.ring.fragments.CallFragment
import cx.ring.tv.call.TVCallActivity
import cx.ring.utils.ConversationPath
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Job
import net.jami.daemon.JamiService
import net.jami.model.Account
import net.jami.model.Contact
import net.jami.model.Profile
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.CallService
import net.jami.services.ContactService
import net.jami.services.DeviceRuntimeService
import net.jami.services.EventService
import net.jami.services.HardwareService
import net.jami.services.NotificationService
import net.jami.utils.Log
import javax.inject.Inject

@AndroidEntryPoint
class RemoteControl : LifecycleService() {

    @Inject
    lateinit var accountService: AccountService

    @Inject
    lateinit var contactService: ContactService

    @Inject
    lateinit var callService: CallService

    @Inject
    lateinit var notificationService: NotificationService

    @Inject
    lateinit var eventService: EventService

    @Inject
    lateinit var deviceService: DeviceRuntimeService

    @Inject
    lateinit var hardwareService: HardwareService

    private val eventListeners = mutableMapOf<IRemoteService.IEventListener, Job>()

    private val tag = "JamiRemoteControl"
    private val compositeDisposable = CompositeDisposable()

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "RemoteControlChannel"
        private const val NOTIFICATION_ID = 101
        internal const val INCOMING_CALL_RECEIVED_EVENT = "INCOMING_CALL_RECEIVED"
        internal const val INCOMING_CALL_ACCEPTED_EVENT = "INCOMING_CALL_ACCEPTED"
        internal const val INCOMING_CALL_REJECT_EVENT = "INCOMING_CALL_REJECTED"
        internal const val OUTGOING_CALL_REQUESTED_EVENT = "OUTGOING_CALL_REQUESTED"
        //Not sure were to call them yet!
        internal const val OUTGOING_CALL_ESTABLISHED_EVENT = "OUTGOING_CALL_ESTABLISHED"
        internal const val OUTGOING_CALL_REJECTED_EVENT = "OUTGOING_CALL_REJECTED"
    }

    var accounts = listOf<Account>()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        compositeDisposable.add(accountService.observableAccountList.subscribe { accounts = it })
        Log.d(tag, "Service created")
        startForegroundServiceWithNotification()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startForegroundServiceWithNotification() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Remote Control Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)

        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Remote Control Service")
            .setContentText("Managing Jami calls and contacts")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        JamiService.setRingingTimeout(300)

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "Service destroyed")
        compositeDisposable.clear()
    }

    // Inner Binder class implementing the AIDL interface
    private val binder = object : IRemoteService.Stub() {
        val callbacks = mutableListOf<IRemoteService.StateCallback>()

        override fun createAccount(map: Map<String, String>): String {
            Log.d(tag, "Creating account with data: $map")
            return try {
                Log.d(tag, "Generated account data map: $map")

                val accountId = accountService.addAccount(map)
                    .map { account -> account.accountId }
                    .doOnError { error -> Log.e(tag, "Error adding account: ${error.message}") }
                    .blockingFirst()

                Log.d(tag, "Account created successfully with ID: $accountId")
                accountId
            } catch (e: Exception) {
                Log.e(tag, "Failed to create account: ${e.message}", e)
                throw RemoteException("Account creation failed: ${e.message}")
            }
        }

        override fun listAccounts(): List<String> {
            return accounts.map { it.accountId }
        }

        override fun getJamiUri(account: String?): String? {
            return accounts.find { it.accountId == account }?.displayUri
        }

        override fun getAccountId(): String {
            Log.d(tag, "Fetching account ID")
            return try {
                accountService.currentAccount?.accountId ?: ""
            } catch (e: Exception) {
                Log.e(tag, "Failed to fetch account ID: ${e.message}", e)
                ""
            }
        }

        override fun addContact(contactId: String) {
            try {
                val accountId = accountService.currentAccount?.accountId
                    ?: throw IllegalStateException("No account found")
                accountService.addContact(accountId, contactId)
                Log.d(tag, "Successfully added contact: $contactId to account: $accountId")
            } catch (e: Exception) {
                Log.e(tag, "Failed to add contact: $contactId to account: $accountId", e)
                throw RemoteException("Failed to add contact: ${e.message}")
            }
        }

        override fun isContactExist(contactId: String): Boolean {
            try {
                val contacts: Map<String, Contact>? = accountService.currentAccount?.contacts
                return contacts?.containsKey(contactId) ?: false
            } catch (e: Exception) {
                Log.e(tag, "Failed to check contact: $contactId", e)
                throw RemoteException("Failed to check contact: ${e.message}")
            }
        }

        override fun sendTrustRequest(contactId: String) {
            try {
                val accountId = accountService.currentAccount?.accountId
                    ?: throw IllegalStateException("No account found")
                accountService.sendTrustRequest(accountId, contactId)
                Log.i(tag, "Trust request sent to: $contactId from account: $accountId")
            } catch (e: Exception) {
                Log.e(
                    tag,
                    "Failed to send trust request to: $contactId from account: $accountId",
                    e
                )
                throw RemoteException("Failed to send trust request: ${e.message}")
            }
        }

        override fun initiateCall(
            fromAccount: String,
            userId: String,
            callback: IRemoteService.ICallback
        ) {
            Log.d(tag, "Initiating call")
            try {
                val account = accountService.getAccount(fromAccount)
                if (account != null) {
                    val contact = account.getContact(userId)
                    val conversation = contact?.conversationUri?.firstElement()?.blockingGet()

                    if (contact != null && conversation != null) {
                        val uri = if (conversation.isSwarm) contact.uri.uri else conversation.uri
                        startActivity(
                            Intent(Intent.ACTION_CALL)
                                .setClass(this@RemoteControl, TVCallActivity::class.java)
                                .putExtras(ConversationPath.toBundle(accountId, conversation.uri))
                                .putExtra(Intent.EXTRA_PHONE_NUMBER, uri)
                                .putExtra(CallFragment.KEY_HAS_VIDEO, true)
                                .apply {
                                    flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                        )
                    } else {
                        Log.e(tag, "Failed to initiate call to user: $userId")
                        callback.onError("Failed to initiate call, contact: $contact, conversation not found")
                    }
                } else {
                    callback.onError("Failed to initiate call, account not found")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error initiating call", e)
                callback.onError(e.message)
            }
        }

        override fun registerCallStateCallback(callback: IRemoteService.StateCallback) {
            callbacks.add(callback)
        }

        override fun unregisterCallStateCallback(callback: IRemoteService.StateCallback?) {
            callbacks.remove(callback)
        }

        override fun hangUpCall() {
            Log.d(tag, "Hanging up the current call")
            try {
                val activeCall = callService.getActiveCall() ?: return
                callService.hangUp(activeCall.account!!, activeCall.daemonIdString!!)
                Log.d(tag, "Call hung up successfully")
            } catch (e: Exception) {
                Log.e(tag, "Error hanging up call", e)
                throw RuntimeException("Failed to hang up call", e)
            }
        }

        override fun acceptCall() {
            Log.d(tag, "Accepting an incoming call")
            try {
                val incomingCall = callService.getIncomingCall() ?: return
                callService.accept(
                    incomingCall.account!!,
                    incomingCall.daemonIdString!!,
                    hasVideo = true
                )
                Log.d(tag, "Call accepted successfully")
            } catch (e: Exception) {
                Log.e(tag, "Error accepting call", e)
                throw RuntimeException("Failed to accept call", e)
            }
        }

        override fun rejectCall() {
            Log.d(tag, "Rejecting an incoming call")
            try {
                val incomingCall = callService.getIncomingCall() ?: return
                callService.refuse(incomingCall.account!!, incomingCall.daemonIdString!!)
                Log.d(tag, "Call rejected successfully")
            } catch (e: Exception) {
                Log.e(tag, "Error rejecting call", e)
                throw RuntimeException("Failed to reject call", e)
            }
        }

        override fun getCallerImage(userId: String): Bitmap? {
            return try {
                val contact =
                    accountService.currentAccount?.getContactFromCache(Uri.fromString(userId))
                contact?.profile?.firstElement()?.blockingGet()?.avatar as? Bitmap
            } catch (e: Exception) {
                Log.e(tag, "Error fetching caller image for $userId: ${e.message}", e)
                null
            }
        }

        override fun setProfileData(
            peerId: String,
            name: String?,
            imageUri: String?,
            fileType: String?
        ) {
            val account = accountService.currentAccount
            if (account != null) {
                val contact = account.getContactFromCache(Uri.fromString(peerId))
                Log.d(tag, "Setting profile data for user: $peerId")
                if (imageUri != null && fileType != null && name != null) {
                    val image: Bitmap =
                        contentResolver.openInputStream(android.net.Uri.parse(imageUri)).use {
                            BitmapFactory.decodeStream(it)
                        }
                    val newProfile = Profile(name, image)
                    Log.d(tag, "Storing picture of height ${image.height}")
                    contactService.storeContactData(contact, newProfile, account.accountId)
                } else if (name != null && imageUri == null) {
                    val profile = contact.profile.firstElement().blockingGet()
                    val newProfile = Profile(name, profile?.avatar)
                    contactService.storeContactData(contact, newProfile, account.accountId)
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.P)
        override fun registerEventListener(listener: IRemoteService.IEventListener) {
            Log.d(tag, "Registering event listener: $listener")
            val job = eventService.subscribeToEvents(lifecycleScope) {
                try {
                    listener.onEventReceived(
                        it.name,
                        it.data
                    )
                } catch (e: Exception) {
                    Log.e(tag, "Error notifying event listener: ${e.message}", e)
                    eventListeners[listener]?.cancel()
                    eventListeners.remove(listener)
                }
            }
            eventListeners[listener] = job
        }

        @RequiresApi(Build.VERSION_CODES.P)
        override fun unregisterEventListener(listener: IRemoteService.IEventListener) {
            Log.d(tag, "Unregistering event listener: $listener")
            eventListeners[listener]?.cancel()
            eventListeners.remove(listener)
        }

        override fun getAccountInfo(account: String): Map<String, String> {
            return JamiService.getAccountDetails(account)
        }

        override fun getPushToken(): String {
            return deviceService.pushToken
        }

        val compositeDisposable = CompositeDisposable()
        val connectionMonitors = mutableMapOf<IRemoteService.IConnectionMonitor, Disposable>()

        override fun registerConnectionMonitor(monitor: IRemoteService.IConnectionMonitor) {
            hardwareService.mPreferenceService.isLogActive = true
            compositeDisposable.add(hardwareService.startLogs()
                .observeOn(Schedulers.io())
                .subscribe({ messages: List<String> ->
                    if (messages.isNotEmpty()) {
                        monitor.onMessages(messages)
                    }
                }) { error: Throwable ->
                    val toRemove = mutableListOf<IRemoteService.IEventListener>()
                    eventListeners.forEach({ (listener, _) ->
                        try {
                            listener.onEventReceived("ERROR", mapOf("message" to error.message))
                        } catch (e: Exception) {
                            toRemove.add(listener)
                        }
                    })
                    toRemove.forEach {
                        eventListeners[it]?.cancel()
                        eventListeners.remove(it)
                    }
                }
                .apply {
                    compositeDisposable.add(this)
                    connectionMonitors[monitor] = this
                }
            )
        }

        override fun unregisterConnectionMonitor(monitor: IRemoteService.IConnectionMonitor?) {
            connectionMonitors[monitor]?.dispose()
            connectionMonitors.remove(monitor)
            hardwareService.mPreferenceService.isLogActive = connectionMonitors.isNotEmpty()
        }

        override fun getOrCreateAccount(data: Map<String, String>): String? {
            return if (accountService.accountsHaveBeenLoaded) {
                accountService.currentAccount?.accountId ?: createAccount(data)
            } else null
        }

        override fun connectivityChanged(isConnected: Boolean) {
            hardwareService.connectivityChanged(isConnected)
        }

        override fun setAccountData(accountId: String, accountData: MutableMap<String, String>) {
            accountService.setAccountDetails(accountId, accountData)
        }

        override fun startConversation(accountId: String, initialMembers: List<String>?) {
            accountService.startConversation(accountId, initialMembers ?: listOf())
        }

        override fun addConversationMember(
            accountId: String,
            conversationId: String,
            contactUri: String
        ) {
            accountService.addConversationMembers(accountId, conversationId, listOf(Uri.fromString(contactUri)))
        }

        override fun removeConversationMember(
            accountId: String,
            conversationId: String,
            contactUri: String
        ) {
            accountService.removeConversationMember(accountId, conversationId, Uri.fromString(contactUri))
        }

        override fun sendMessage(
            accountId: String,
            conversationId: String,
            message: String,
            replyTo: String,
            flag: Int
        ) {
            accountService.sendConversationMessage(accountId, Uri.fromString(conversationId), message, replyTo, flag)
        }

        override fun cycleAccount(accountId: String) {
            Log.d(tag, "Cycling $accountId")
            val callActive = callService.getIncomingCall() != null || callService.getActiveCall() != null
            if (!callActive) {
                accountService.setAccountEnabled(accountId, false)
                accountService.setAccountEnabled(accountId, true)
            } else {
                Log.d(tag, "Call is active, not cycling account")
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Log.d(tag, "Service bound")
        val disposable = callService.callsUpdates.subscribe { call ->
            Log.i("RemoteControl", "Call state changed: ${call.callStatus}")
            binder.callbacks.forEach { callback ->
                Log.i("RemoteControl", "Notifying callback: $callback")
                callback.newCallState(call.callStatus.toString())
            }
        }
        compositeDisposable.add(disposable)
        compositeDisposable.add(binder.compositeDisposable)
        return binder
    }
}