package cx.ring.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import cx.ring.IRemoteService
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.model.Account
import net.jami.model.Contact
import net.jami.model.Profile
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.CallService
import net.jami.services.ContactService
import net.jami.utils.Log
import javax.inject.Inject

@AndroidEntryPoint
class RemoteControl : Service() {

    @Inject
    lateinit var accountService: AccountService

    @Inject
    lateinit var contactService: ContactService

    @Inject
    lateinit var callService: CallService

    @Inject
    lateinit var connectionService: ConnectionService

    private val eventListenerList = mutableListOf<IRemoteService.IEventListener>()

    private val tag = "JamiRemoteControlService"
    private val compositeDisposable = CompositeDisposable()

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "RemoteControlChannel"
        private const val NOTIFICATION_ID = 101
        internal const val INCOMING_CALL_RECEIVED_EVENT = "INCOMING_CALL_RECEIVED"
        internal const val INCOMING_CALL_ACCEPTED_EVENT = "INCOMING_CALL_ACCEPTED"
        internal const val INCOMING_CALL_REJECT_EVENT = "INCOMING_CALL_REJECTED"
        internal const val OUTGOING_CALL_REQUESTED_EVENT = "OUTGOING_CALL_REQUESTED"
        internal const val OUTGOING_CALL_ESTABLISHED_EVENT = "OUTGOING_CALL_ESTABLISHED"
        internal const val OUTGOING_CALL_REJECTED_EVENT = "OUTGOING_CALL_REJECTED"
        internal const val CALL_HUNG_UP_EVENT = "CALL_HANGED_UP"
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
                val contacts : Map<String, Contact>? = accountService.currentAccount?.contacts
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
                Log.e(tag, "Failed to send trust request to: $contactId from account: $accountId", e)
                throw RemoteException("Failed to send trust request: ${e.message}")
            }
        }

        override fun initiateCall(fromAccount: String, userId: String, callback: IRemoteService.ICallback) {
            Log.d(tag, "Initiating call")
            notifyEventListeners(OUTGOING_CALL_REQUESTED_EVENT)
            try {
                val disposable = callService.placeCall(
                    account = fromAccount,
                    conversationUri = null,
                    numberUri = Uri.fromString(userId),
                    hasVideo = true
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ call ->
                        Log.d(tag, "Call initiated successfully: $call")
                        notifyEventListeners(OUTGOING_CALL_ESTABLISHED_EVENT)
                        callback.onSuccess()
                    }, { error ->
                        Log.e(tag, "Failed to initiate call: ${error.message}", error)
                        callback.onError(error.message)
                    })

                compositeDisposable.add(disposable)
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
            notifyEventListeners(CALL_HUNG_UP_EVENT)
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
            notifyEventListeners(INCOMING_CALL_ACCEPTED_EVENT)
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
            notifyEventListeners(INCOMING_CALL_REJECT_EVENT)
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
                val contact = accountService.currentAccount?.getContactFromCache(Uri.fromString(userId))
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
                    val image: Bitmap = contentResolver.openInputStream(android.net.Uri.parse(imageUri)).use {
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

        override fun registerEventListener(listener: IRemoteService.IEventListener) {
            eventListenerList.add(listener)
            connectionService.registerEventListener(listener)
        }

        override fun unregisterEventListener(listener: IRemoteService.IEventListener) {
            eventListenerList.remove(listener)
            connectionService.unregisterEventListener(listener)
        }

        private fun notifyEventListeners(name: String, data: Map<String, String>? = null) {
            eventListenerList.forEach { listener ->
                try {
                    listener.onEventReceived(name, data)
                } catch (e: RemoteException) {
                    Log.e(tag, "Error notifying event listener", e)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(tag, "Service bound")
        val disposable = callService.callsUpdates.onErrorComplete({
            Log.e(tag, "Error observing call updates", it)
            true
        }).subscribe { call ->
            Log.i("RemoteControl", "Call state changed: ${call.callStatus}")
            binder.callbacks.forEach { callback ->
                Log.i("RemoteControl", "Notifying callback: $callback")
                callback.newCallState(call.callStatus.toString())
            }
        }
        compositeDisposable.add(disposable)
        return binder
    }
}