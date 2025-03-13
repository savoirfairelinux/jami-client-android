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

import android.os.Bundle
import android.util.Log
import androidx.leanback.app.GuidedStepSupportFragment
import cx.ring.application.JamiApplication
import cx.ring.linkdevice.view.ImportSideStep3Fragment
import cx.ring.linkdevice.view.ImportSideStep3Fragment.OnResultCallback
import cx.ring.mvp.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import net.jami.account.AccountImportPresenter
import net.jami.account.AccountImportView
import net.jami.services.AccountService

@AndroidEntryPoint
class TVImportWizard : BaseActivity<AccountImportPresenter>(), AccountImportView, OnResultCallback {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        JamiApplication.instance?.startDaemon(this)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(android.R.id.content, TVAccountImportStep1Fragment())
                .commitNow()
        }
        presenter.bindView(this)
        presenter.init()
    }

    override fun onDestroy() {
        presenter.unbindView()
        super.onDestroy()
    }

    override fun showToken(token: String) {
        Log.w(TAG, "showToken: $token")
        val fragment =
            supportFragmentManager.findFragmentById(android.R.id.content) as? TVAccountImportStep1Fragment
                ?: TVAccountImportStep1Fragment().also {
                    supportFragmentManager.beginTransaction()
                        .add(android.R.id.content, it)
                        .commitNow()
                }
        fragment.showToken(token)
    }

    override fun showConnecting() {
        val fragment =
            supportFragmentManager.findFragmentById(android.R.id.content) as? TVAccountImportStep1Fragment
        fragment?.showConnecting()
    }

    fun onAuthentication(password: String) {
        presenter.onAuthentication(password)
    }

    fun onCancel() {
        presenter.onCancel()
    }

    override fun showAuthenticating(
        needPassword: Boolean,
        jamiId: String,
        registeredName: String,
        error: String?
    ) {
        Log.w(TAG, "showAuthenticating: $needPassword, $jamiId, $registeredName, $error")
        GuidedStepSupportFragment.addAsRoot(
            this,
            TVAccountImportStep2Fragment().apply {
                arguments = Bundle().apply {
                    putBoolean("needPassword", needPassword)
                    putString("jamiId", jamiId)
                    putString("registeredName", registeredName)
                    putString("error", error)
                }
            },
            android.R.id.content
        )
    }

    override fun showInProgress() {
        Log.w(TAG, "showInProgress")
        val fragment =
            supportFragmentManager.findFragmentById(android.R.id.content) as? ImportSideStep3Fragment
                ?: ImportSideStep3Fragment().also {
                    supportFragmentManager.beginTransaction()
                        .add(android.R.id.content, it)
                        .commitNow()
                }
        fragment.showLoading()
    }

    override fun showDone(error: AccountService.AuthError?) {
        Log.w(TAG, "showDone $error")
        val fragment =
            supportFragmentManager.findFragmentById(android.R.id.content) as? ImportSideStep3Fragment
                ?: ImportSideStep3Fragment().also {
                    supportFragmentManager.beginTransaction()
                        .add(android.R.id.content, it)
                        .commitNow()
                }
        if (error == null) {
            fragment.showDone()
        } else {
            fragment.showError(error)
        }
    }

    override fun showExit() {
        Log.w(TAG, "showExit")
        finish()
    }

    override fun onExit(returnCode: Int) {
        Log.w(TAG, "showExit")
        finish()
    }

    companion object {
        private const val TAG = "TVImportWizard"
    }
}