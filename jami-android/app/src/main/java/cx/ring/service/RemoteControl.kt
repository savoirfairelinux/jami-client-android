package cx.ring.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
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
import io.reactivex.rxjava3.subjects.BehaviorSubject
import net.jami.model.Account
import net.jami.model.Contact
import net.jami.model.Uri
import net.jami.services.AccountService
import net.jami.services.CallService
import net.jami.utils.Log
import javax.inject.Inject

@AndroidEntryPoint
class RemoteControl : Service() {

    @Inject
    lateinit var accountService: AccountService

    @Inject
    lateinit var callService: CallService

    private val tag = "JamiRemoteControlService"
    private val compositeDisposable = CompositeDisposable()

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "RemoteControlChannel"
        private const val NOTIFICATION_ID = 101
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
            Log.d(tag, "Initiating call to user: $userId")
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
                val contact = accountService.currentAccount?.getContactFromCache(Uri.fromString(userId))
                contact?.profile?.firstElement()?.blockingGet()?.avatar as? Bitmap
            } catch (e: Exception) {
                Log.e(tag, "Error fetching caller image for $userId: ${e.message}", e)
                null
            }
        }

    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(tag, "Service bound")
        val disposable = callService.callsUpdates.subscribe { call ->
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