package cx.ring.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import cx.ring.IRemoteService
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
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

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "Service created")
        Log.d(tag, "AccountService injected: ${this::accountService.isInitialized}")
    }
    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "Service destroyed")
        compositeDisposable.clear()
    }

    // Inner Binder class implementing the AIDL interface
    private val binder = object : IRemoteService.Stub() {

        override fun createAccount(input: String): String {
            Log.d(tag, "Creating account with login: $input")
            return try {
                val map = fillMap(AccountData.data, input)
                Log.d(tag, "Generated account data map: $map")

                val accountId = accountService.addAccount(map)
                    .map { account -> account.accountId }
                    .doOnError { error -> Log.e(tag, "Error adding account: ${error.message}") }
                    .blockingFirst()

                Log.d(tag, "Account created successfully with ID: $accountId")
                accountId
            } catch (e: Exception) {
                Log.e(tag, "Failed to create account: ${e.message}", e)
                throw RuntimeException("Account creation failed", e)
            }
        }

        override fun getAccountId(): String {
            Log.d(tag, "Fetching account ID")
            return try {
                val accountId = accountService.currentAccount?.accountId
                if (accountId.isNullOrEmpty()) {
                    Log.e(tag, "Account ID is null or empty")
                    ""
                } else {
                    Log.d(tag, "Account ID: $accountId")
                    accountId
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to fetch account ID: ${e.message}", e)
                ""
            }
        }


        override fun initiateCall(userId: String, callback: IRemoteService.ICallback) {
            Log.d(tag, "Initiating call to user: $userId")
            try {
                val disposable = callService.placeCall(
                    account = getAccountId(),
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

        override fun hangUpCall() {
            Log.d(tag, "Hanging up the current call")
            try {
                val activeCall = callService.getActiveCall()
                    ?: throw IllegalStateException("No active call to hang up")

                callService.hangUp(
                    accountId = activeCall.account!!,
                    callId = activeCall.daemonIdString!!
                )
                Log.d(tag, "Call hung up successfully")
            } catch (e: Exception) {
                Log.e(tag, "Error hanging up call: ${e.message}", e)
                throw RuntimeException("Failed to hang up call", e)
            }
        }

        override fun acceptCall() {
            Log.d(tag, "Accepting an incoming call")
            try {
                val incomingCall = callService.getIncomingCall()
                    ?: throw IllegalStateException("No incoming call to accept")

                callService.accept(
                    accountId = incomingCall.account!!,
                    callId = incomingCall.daemonIdString!!,
                    hasVideo = true
                )
                Log.d(tag, "Call accepted successfully")
            } catch (e: Exception) {
                Log.e(tag, "Error accepting call: ${e.message}", e)
                throw RuntimeException("Failed to accept call", e)
            }
        }

        override fun rejectCall() {
            Log.d(tag, "Rejecting an incoming call")
            try {
                val incomingCall = callService.getIncomingCall()
                    ?: throw IllegalStateException("No incoming call to reject")

                callService.refuse(
                    accountId = incomingCall.account!!,
                    callId = incomingCall.daemonIdString!!
                )
                Log.d(tag, "Call rejected successfully")
            } catch (e: Exception) {
                Log.e(tag, "Error rejecting call: ${e.message}", e)
                throw RuntimeException("Failed to reject call", e)
            }
        }

        override fun getUserImage(): ByteArray {
            Log.d(tag, "Fetching user image")
            // Add actual implementation
            throw NotImplementedError("getUserImage not implemented yet")
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(tag, "Service bound")
        return binder
    }
}