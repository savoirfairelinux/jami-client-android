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
package cx.ring.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.account.AccountEditionFragment
import cx.ring.account.AccountPasswordDialog
import ezvcard.util.org.apache.commons.codec.binary.Base64
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Account
import net.jami.model.AccountConfig
import net.jami.services.AccountService
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

object BiometricHelper {
    class BiometricInfo(val keyName: String, val encryptedKey: ByteArray, val iv: ByteArray)

    private fun generateKeyName(): String = UUID.randomUUID().toString()

    fun generateSecretKey(name: String): SecretKey =
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(
                name,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(true)
                .apply { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    setUserAuthenticationParameters(0, KEY_AUTH)
                }
                .setInvalidatedByBiometricEnrollment(true)
                .build())
        }.generateKey()

    private fun getSecretKey(name: String): SecretKey? =
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            keyStore.getKey(name, null) as SecretKey?
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get secret key", e)
            null
        }

    private fun deleteSecretKey(name: String) {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            keyStore.deleteEntry(name)
            Log.d(TAG, "Secret key $name deleted")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete secret key", e)
        }
    }

    /** From https://developer.android.com/training/sign-in/biometric-auth */
    private fun getCipher(): Cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
            + KeyProperties.BLOCK_MODE_CBC + "/"
            + KeyProperties.ENCRYPTION_PADDING_PKCS7)

    fun startAccountAuthentication(fragment: Fragment, account: Account, reason: String?, callback: (scheme: String, password: String) -> Unit)  {
        if (!account.hasPassword()) {
            callback(AccountService.ACCOUNT_SCHEME_NONE, "")
            return
        }
        startBiometricLogin(fragment, account.accountId, reason) {
            if (it != null) {
                callback(AccountService.ACCOUNT_SCHEME_KEY, Base64.encodeBase64String(it))
            } else {
                AccountPasswordDialog().apply {
                    arguments = Bundle().apply {
                        putString(AccountEditionFragment.ACCOUNT_ID_KEY, account.accountId)
                    }
                    listener = AccountPasswordDialog.UnlockAccountListener { scheme, password -> callback(scheme, password) }
                }.show(fragment.parentFragmentManager, FRAGMENT_DIALOG_PASSWORD)
            }
        }
    }

    private fun startBiometricLogin(fragment: Fragment, accountId: String, reason: String?, callback: (ByteArray?) -> Unit) {
        val context = fragment.requireContext()
        val biometricInfo = loadAccountKey(context, accountId)
        if (biometricInfo == null) {
            callback(null)
            return
        }
        val secretKey = getSecretKey(biometricInfo.keyName)
        if (secretKey == null) {
            callback(null)
            return
        }

        try {
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(context.getText(R.string.account_biometry_auth_title))
                .setSubtitle(reason)
                .setNegativeButtonText(context.getText(R.string.account_auth_password))
                .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                .build()
            val cipher = getCipher().apply { init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(biometricInfo.iv)) }
            BiometricPrompt(fragment, ContextCompat.getMainExecutor(context),
                object: BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        Toast.makeText(context.applicationContext, context.getString(R.string.account_auth_error, errString), Toast.LENGTH_SHORT).show()
                        callback(null)
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        val decryptedKey: ByteArray? = result.cryptoObject?.cipher?.doFinal(biometricInfo.encryptedKey)
                        Toast.makeText(context.applicationContext,
                            if (decryptedKey == null) R.string.account_auth_key_error else R.string.account_auth_success, Toast.LENGTH_SHORT).show()
                        callback(decryptedKey)
                    }

                    override fun onAuthenticationFailed() {}
                }).authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init cipher", e)
            callback(null)
        }
    }

    fun writeAccountKey(context: Context, accountId: String, info: BiometricInfo) {
        context.getSharedPreferences("biometric_${accountId}", Context.MODE_PRIVATE)
            .edit()
            .putString(BIOMETRIC_KEY_NAME, info.keyName)
            .putString(BIOMETRIC_KEY, Base64.encodeBase64String(info.encryptedKey))
            .putString(BIOMETRIC_IV, Base64.encodeBase64String(info.iv))
            .apply()
    }

    fun loadAccountKey(context: Context, accountId: String): BiometricInfo? =
        try {
            val prefs = context.getSharedPreferences("biometric_${accountId}", Context.MODE_PRIVATE)
            val keyName = prefs.getString(BIOMETRIC_KEY_NAME, null)
            val key = prefs.getString(BIOMETRIC_KEY, null)
            val iv = prefs.getString(BIOMETRIC_IV, null)
            if (keyName == null || key == null || iv == null)
                null
            else
                BiometricInfo(keyName, Base64.decodeBase64(key), Base64.decodeBase64(iv))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load account key", e)
            null
        }

    fun deleteAccountKey(context: Context, accountId: String) {
        loadAccountKey(context, accountId)?.let {
            deleteSecretKey(it.keyName)
        }
        context.deleteSharedPreferences("biometric_${accountId}")
    }

    class BiometricEnroll(
        private val accountId: String?,
        private val fragment: Fragment,
        private val password: String,
        accountService: AccountService,
        private val callback: (BiometricInfo?) -> Unit,
        private val account: Observable<Account> = accountService.getObservableAccount(accountId!!),
        private val launcher: ActivityResultLauncher<Intent>
    ) {
        private val applicationContext = fragment.requireContext().applicationContext
        private var mIsBiometric = false
        private var key: SecretKey? = null
        private var keyName: String? = null
        private var iv: ByteArray? = null
        private val disposable = CompositeDisposable()

        private val biometricPromptCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Toast.makeText(applicationContext, applicationContext.getString(R.string.account_auth_error, errString), Toast.LENGTH_SHORT).show()
                callback(null)
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                disposable.add(account.onErrorComplete()
                    .filter { a: Account -> a.registrationState != AccountConfig.RegistrationState.INITIALIZING }
                    .firstElement()
                    .flatMapSingle { a -> accountService.getAccountPasswordKey(a.accountId, password).map { Pair(a, it) } }
                    .map { (a, archiveKey) ->
                        if (archiveKey.isEmpty()) {
                            throw IllegalStateException("Failed to get account key")
                        }
                        val encryptedKey = result.cryptoObject?.cipher?.doFinal(archiveKey)!!
                        val bi = BiometricInfo(keyName!!, encryptedKey, iv!!)
                        writeAccountKey(applicationContext, a.accountId, bi)
                        Pair(a, bi)
                    }
                    .observeOn(DeviceUtils.uiScheduler)
                    .subscribe({ (a, bi) ->
                        Log.d(TAG, "Encrypted information: " + bi.encryptedKey.contentToString())
                        Toast.makeText(applicationContext, R.string.account_auth_success, Toast.LENGTH_SHORT).show()
                        accountService.refreshAccount(a.accountId)
                        callback(bi)
                    }) {
                        Log.e(TAG, "Failed to encrypt account key", it)
                        Toast.makeText(applicationContext, R.string.account_auth_key_error, Toast.LENGTH_SHORT).show()
                        callback(null)
                    })
            }

            override fun onAuthenticationFailed() {}
        }

        fun start() {
            if (password.isEmpty()) {
                callback(null)
                return
            }
            val context = fragment.requireContext()
            val biometricManager = BiometricManager.from(context)
            when (biometricManager.canAuthenticate(ALLOWED_AUTHENTICATORS)) {
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    askBiometric()
                    return
                }
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Log.e(TAG, "No biometric features available on this device.")
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> Log.e(TAG, "Biometric features are currently unavailable.")
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> Log.e(TAG, "Security update required.")
                BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> Log.e(TAG, "Biometric authentication is not supported.")
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (mIsBiometric) {
                            launcher.launch(Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                                putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, ALLOWED_AUTHENTICATORS)
                            })
                        } else {
                            askBiometric()
                        }
                        return
                    }
                }
                BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> Log.e(TAG, "Biometric authentication status is unknown.")
            }
            callback(null)
            return
        }

        private fun tryLoadExistingKey(): Boolean {
            if (accountId != null) {
                loadAccountKey(applicationContext, accountId)?.let { keyInfo ->
                    getSecretKey(keyInfo.keyName)?.let {
                        key = it
                        keyName = keyInfo.keyName
                        Log.w(TAG, "Reusing existing key ${keyInfo.keyName}")
                        return true
                    }
                }
            }
            return false
        }

        private fun startBiometricEnroll() {
            try {
                if (!tryLoadExistingKey()) {
                    val newKeyName = generateKeyName()
                    val newKey = generateSecretKey(newKeyName)
                    key = newKey
                    keyName = newKeyName
                    Log.w(TAG, "New key generated: $newKeyName")
                }
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(applicationContext.getText(R.string.account_biometry_enroll_auth_title))
                    //.setSubtitle(applicationContext.getText(R.string.account_biometry_enroll_message))
                    .setDescription(fragment.getText(R.string.account_biometry_enroll_auth_message))
                    .setNegativeButtonText(fragment.getText(android.R.string.cancel))
                    .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                    .build()
                val cipher = getCipher().apply { init(Cipher.ENCRYPT_MODE, key) }
                BiometricPrompt(fragment, ContextCompat.getMainExecutor(fragment.requireContext()), biometricPromptCallback)
                    .authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
                iv = cipher.iv
                Log.w(TAG, "iv: $iv")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to init cipher", e)
                callback(null)
            }
        }

        private fun askBiometric() {
            if (mIsBiometric) {
                startBiometricEnroll()
                return
            }
            MaterialAlertDialogBuilder(fragment.requireContext(), R.style.BiometricMaterialAlertDialog)
                .setIcon(R.drawable.fingerprint_24)
                .setTitle(R.string.account_biometry_enroll_title)
                .setMessage(R.string.account_biometry_enroll_message)
                .setPositiveButton(R.string.account_biometry_enroll_accept) { _, _ ->
                    mIsBiometric = true
                    start()
                }
                .setNegativeButton(R.string.no_thanks) { _, _ ->
                    accountId?.let { deleteAccountKey(applicationContext, it) }
                    callback(null)
                }
                .setCancelable(false)
                .create()
                .show()
        }

        fun dispose() {
            disposable.dispose()
        }

        fun onActivityResult(resultCode: Int, data: Intent?) {
            if (resultCode == Activity.RESULT_OK) {
                start()
            } else {
                callback(null)
            }
        }
    }

    private const val TAG = "BiometricHelper"
    private const val BIOMETRIC_KEY_NAME = "keyName"
    private const val BIOMETRIC_KEY = "key"
    private const val BIOMETRIC_IV = "iv"
    private const val FRAGMENT_DIALOG_PASSWORD = "${TAG}.dialog.password"

    private val ALLOWED_AUTHENTICATORS = /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    else
        */BiometricManager.Authenticators.BIOMETRIC_STRONG

    @RequiresApi(Build.VERSION_CODES.R)
    private const val KEY_AUTH = KeyProperties.AUTH_BIOMETRIC_STRONG/* or KeyProperties.AUTH_DEVICE_CREDENTIAL*/
}
