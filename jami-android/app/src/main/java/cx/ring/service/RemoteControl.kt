package cx.ring.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import cx.ring.IRemoteService
import dagger.hilt.android.AndroidEntryPoint
import net.jami.services.AccountService
import net.jami.utils.Log
import javax.inject.Inject

@AndroidEntryPoint
class RemoteControl : Service() {

    @Inject
    lateinit var accountService: AccountService

    private val tag = "JamiRemoteControlService"

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "Service created")
        Log.d(tag, "AccountService injected: ${this::accountService.isInitialized}")
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

        override fun initiateCall(userId: String?) {
            Log.d(tag, "Initiating call to user: $userId")
            // Add actual implementation
            throw NotImplementedError("initiateCall not implemented yet")
        }

        override fun hangUpCall() {
            Log.d(tag, "Hanging up the current call")
            // Add actual implementation
            throw NotImplementedError("hangUpCall not implemented yet")
        }

        override fun acceptCall() {
            Log.d(tag, "Accepting an incoming call")
            // Add actual implementation
            throw NotImplementedError("acceptCall not implemented yet")
        }

        override fun rejectCall() {
            Log.d(tag, "Rejecting an incoming call")
            // Add actual implementation
            throw NotImplementedError("rejectCall not implemented yet")
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