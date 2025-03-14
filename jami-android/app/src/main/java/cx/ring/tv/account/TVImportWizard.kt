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
package cx.ring.tv.account

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.linkdevice.view.ImportSideStep3Fragment
import cx.ring.linkdevice.view.ImportSideStep3Fragment.OnResultCallback
import cx.ring.linkdevice.viewmodel.AddDeviceImportState
import cx.ring.linkdevice.viewmodel.ImportSideViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.jami.services.AccountService

@AndroidEntryPoint
class TVImportWizard : AppCompatActivity(), OnResultCallback {
    private val importSideViewModel by lazy { ViewModelProvider(this)[ImportSideViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        JamiApplication.instance?.startDaemon(this)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(android.R.id.content, TVAccountImportStep1Fragment())
                .commitNow()
        }

        lifecycleScope.launch {
            importSideViewModel.uiState.collect {
                Log.d(TAG, "UI state: $it")
                when (it) {
                    is AddDeviceImportState.Init -> {}
                    is AddDeviceImportState.TokenAvailable -> showToken(it.token)
                    is AddDeviceImportState.Connecting -> showConnecting()
                    is AddDeviceImportState.Authenticating -> showAuthenticating(
                        it.needPassword,
                        it.id,
                        it.registeredName,
                        when (it.error) {
                            ImportSideViewModel.InputError.BAD_PASSWORD -> getString(R.string.link_device_error_bad_password)
                            ImportSideViewModel.InputError.UNKNOWN -> getString(R.string.link_device_error_unknown)
                            else -> null
                        }
                    )
                    is AddDeviceImportState.InProgress -> showInProgress()
                    is AddDeviceImportState.Done -> showDone(it.error)
                }
            }
        }
    }

    private fun showToken(token: String) {
        Log.w(TAG, "showToken: $token")
        val fragment =
            supportFragmentManager.findFragmentById(android.R.id.content) as? TVAccountImportStep1Fragment
                ?: TVAccountImportStep1Fragment().also {
                    supportFragmentManager.beginTransaction()
                        .replace(android.R.id.content, it)
                        .commitNow()
                }
        if (token.isEmpty())
            fragment.showLoading()
        else
            fragment.showToken(token)
    }

    private fun showConnecting() {
        val fragment =
            supportFragmentManager.findFragmentById(android.R.id.content) as? TVAccountImportStep1Fragment
        fragment?.showConnecting()
    }

    fun onAuthentication(password: String) {
        importSideViewModel.onAuthentication(password)
    }

    fun onCancel() {
        //presenter.onCancel()
    }

    private fun showAuthenticating(
        needPassword: Boolean,
        id: String,
        registeredName: String?,
        error: String?
    ) {
        Log.w(TAG, "showAuthenticating: $needPassword, $id, $registeredName, $error")
        val fragment =
            supportFragmentManager.findFragmentById(android.R.id.content) as? TVAccountImportStep2Fragment
        if (fragment == null) {
            GuidedStepSupportFragment.addAsRoot(
                this,
                TVAccountImportStep2Fragment.build(needPassword, id, registeredName, error),
                android.R.id.content
            )
        } else {
            fragment.update(needPassword, id, registeredName, error)
        }
    }

    private fun showInProgress() {
        Log.w(TAG, "showInProgress")
        val fragment =
            supportFragmentManager.findFragmentById(android.R.id.content) as? ImportSideStep3Fragment
                ?: ImportSideStep3Fragment().also {
                    supportFragmentManager.beginTransaction()
                        .replace(android.R.id.content, it)
                        .commitNow()
                }
        fragment.showLoading()
    }

    private fun showDone(error: AccountService.AuthError?) {
        Log.w(TAG, "showDone $error")
        val fragment =
            supportFragmentManager.findFragmentById(android.R.id.content) as? ImportSideStep3Fragment
                ?: ImportSideStep3Fragment().also {
                    supportFragmentManager.beginTransaction()
                        .replace(android.R.id.content, it)
                        .commitNow()
                }
        if (error == null) {
            fragment.showDone()
        } else {
            fragment.showError(error)
        }
    }

    override fun onExit(returnCode: Int) {
        Log.w(TAG, "showExit $returnCode")
        setResult(when(returnCode) {
            0 -> RESULT_OK
            else -> RESULT_CANCELED
        })
        finish()
    }

    companion object {
        private const val TAG = "TVImportWizard"
    }
}