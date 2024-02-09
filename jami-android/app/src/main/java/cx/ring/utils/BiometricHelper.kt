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
package cx.ring.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.account.AccountEditionFragment
import cx.ring.account.BackupAccountDialog
import ezvcard.util.org.apache.commons.codec.binary.Base64
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Account
import net.jami.model.AccountConfig
import net.jami.model.BiometricInfo
import net.jami.services.AccountService
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

object BiometricHelper {

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
                    setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
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
            Log.e("BiometryHelper", "Failed to get secret key", e)
            null
        }

    private fun getCipher(): Cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
            + KeyProperties.BLOCK_MODE_CBC + "/"
            + KeyProperties.ENCRYPTION_PADDING_PKCS7)

    fun startAccountAuthentication(fragment: Fragment, account: Account, callback: (scheme: String, password: String) -> Unit)  {
        if (!account.hasPassword()) {
            callback(AccountService.ACCOUNT_SCHEME_NONE, "")
            return
        }
        startBiometricLogin(fragment, account.accountId) {
            if (it != null) {
                callback(AccountService.ACCOUNT_SCHEME_KEY, Base64.encodeBase64String(it))
            } else {
                BackupAccountDialog().apply {
                    arguments = Bundle().apply {
                        putString(AccountEditionFragment.ACCOUNT_ID_KEY, account.accountId)
                    }
                    setListener(object : BackupAccountDialog.UnlockAccountListener {
                        override fun onUnlockAccount(accountId: String, scheme: String, password: String) {
                            callback(scheme, password)
                        }
                    })
                }.show(fragment.parentFragmentManager, FRAGMENT_DIALOG_PASSWORD)
            }
        }
    }

    fun startBiometricLogin(fragment: Fragment, accountId: String, callback: (ByteArray?) -> Unit) {
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
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Jami account")
            .setSubtitle("Add new device to your account")
            //.setNegativeButtonText("Use account password")
            .setAllowedAuthenticators(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            else
                BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        val biometricPrompt = BiometricPrompt(fragment, ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Toast.makeText(context.applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                    callback(null)
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    Toast.makeText(context.applicationContext, "Authentication succeeded!", Toast.LENGTH_SHORT).show()
                    val decryptedKey: ByteArray? = result.cryptoObject?.cipher?.doFinal(biometricInfo.key)
                    if (decryptedKey == null)
                        Toast.makeText(context.applicationContext, "Failed to decrypt account key", Toast.LENGTH_SHORT).show()
                    else
                        Log.d("BiometryHelper", "Decrypted information: " + decryptedKey.decodeToString())
                    callback(decryptedKey)
                }

                override fun onAuthenticationFailed() {
                    Toast.makeText(context.applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                    callback(null)
                }
            })

        try {
            val cipher = getCipher()
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(biometricInfo.iv))
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        } catch (e: Exception) {
            Log.e("BiometryHelper", "Failed to init cipher", e)
            callback(null)
        }
    }

    fun writeAccountKey(context: Context, accountId: String, info: BiometricInfo) {
        context.getSharedPreferences("biometric_${accountId}", Context.MODE_PRIVATE)
            .edit()
            .putString(BIOMETRIC_KEY_NAME, info.keyName)
            .putString(BIOMETRIC_KEY, Base64.encodeBase64String(info.key))
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
        context.deleteSharedPreferences("biometric_${accountId}")
    }

    class BiometricEnroll(val fragment: Fragment, val password: String, val account: Observable<Account>, accountService: AccountService, val callback: (BiometricInfo?) -> Unit) {
        private val applicationContext = fragment.requireContext().applicationContext
        private var mIsBiometric = false
        private var key: SecretKey? = null
        private var keyName: String? = null
        private var iv: ByteArray? = null
        private val disposable = CompositeDisposable()

        private val biometricPromptCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                callback(null)
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Toast.makeText(applicationContext, "Authentication succeeded!", Toast.LENGTH_SHORT).show()
                disposable.add(account.onErrorComplete()
                    .filter { a: Account -> a.registrationState != AccountConfig.RegistrationState.INITIALIZING }
                    .firstElement()
                    .flatMapSingle { a -> accountService.getAccountPasswordKey(a.accountId, password).map { Pair(a, it) } }
                    .map { (a, archiveKey) ->
                        if (archiveKey.isEmpty()) {
                            throw IllegalStateException("Failed to get account key")
                        }
                        val keys = result.cryptoObject?.cipher?.doFinal(archiveKey)!!
                        val bi = BiometricInfo(keyName!!, keys, iv!!)
                        writeAccountKey(applicationContext, a.accountId, bi)
                        bi
                    }
                    .observeOn(DeviceUtils.uiScheduler)
                    .subscribe({ bi ->
                        Log.d(TAG, "Encrypted information: " + bi.key.contentToString())
                        callback(bi)
                    }) {
                        Log.e(TAG, "Failed to encrypt account key", it)
                        callback(null)
                    })
            }

            override fun onAuthenticationFailed() {
                Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                callback(null)
            }
        }

        private val biometricPrompt by lazy { BiometricPrompt(fragment, ContextCompat.getMainExecutor(fragment.requireContext()), biometricPromptCallback) }

        private val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Add authentication for your Jami account")
            .setSubtitle("Add secure biometric or device authentication as an alternative to your password")
            //.setNegativeButtonText("Use account password")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        fun start() {
            if (password.isEmpty()) {
                callback(null)
                return
            }
            val context = fragment.requireContext()
            val biometricManager = BiometricManager.from(context)
            when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
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
                            val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                                putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                            }
                            fragment.startActivityForResult(enrollIntent, REQUEST_CODE_ENROLL)
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

        private fun startBiometricEnroll() {
            try {
                val newKeyName = generateKeyName()
                val newKey = generateSecretKey(newKeyName)
                val cipher = getCipher()
                cipher.init(Cipher.ENCRYPT_MODE, newKey)
                key = newKey
                keyName = newKeyName
                biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
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
                .setTitle("Use biometric authentication?")
                .setMessage("Your device supports secure biometric authentication.\nBiometric or device authentication adds a convenient way to locally unlock access to your Jami account. You will still be able to use your password.")
                .setPositiveButton("Use biometrics") { _, _ ->
                    mIsBiometric = true
                    start()
                }
                .setNegativeButton("No thanks") { _, _ ->
                    callback(null)
                }
                .setCancelable(false)
                .create()
                .show()
        }

        fun dispose() {
            disposable.dispose()
        }
    }

    const val REQUEST_CODE_ENROLL = 3232
    private const val TAG = "BiometricHelper"
    private const val BIOMETRIC_KEY_NAME = "keyName"
    private const val BIOMETRIC_KEY = "key"
    private const val BIOMETRIC_IV = "iv"
    private val FRAGMENT_DIALOG_PASSWORD = "${TAG}.dialog.password"
}