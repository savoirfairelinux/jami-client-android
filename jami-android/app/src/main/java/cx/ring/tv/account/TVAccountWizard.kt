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

import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.leanback.app.GuidedStepSupportFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.account.AccountCreationViewModel
import cx.ring.application.JamiApplication
import cx.ring.databinding.ItemProgressDialogBinding
import cx.ring.mvp.BaseActivity
import cx.ring.utils.BitmapUtils
import dagger.hilt.android.AndroidEntryPoint
import net.jami.account.AccountWizardPresenter
import net.jami.account.AccountWizardView
import net.jami.model.Account
import net.jami.model.AccountConfig
import net.jami.model.AccountCreationModel

@AndroidEntryPoint
class TVAccountWizard : BaseActivity<AccountWizardPresenter>(), AccountWizardView {
    private var mProgress: AlertDialog? = null
    private var mAccountType: String? = null
    private var mAlertDialog: AlertDialog? = null
    private var mJamsAccount = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            onBackInvokedDispatcher.registerOnBackInvokedCallback(1) { onBackPressed() }
        JamiApplication.instance?.startDaemon(this)
        val model: AccountCreationViewModel by viewModels()
        val intent = intent
        if (intent != null) {
            mAccountType = intent.action
        }
        if (mAccountType == null) {
            mAccountType = AccountConfig.ACCOUNT_TYPE_JAMI
        }
        if (savedInstanceState == null) {
            GuidedStepSupportFragment.addAsRoot(
                this,
                TVHomeAccountCreationFragment(),
                android.R.id.content
            )
        }
        presenter.init(getIntent().action ?: AccountConfig.ACCOUNT_TYPE_JAMI)
    }

    override fun onDestroy() {
        mProgress?.let { progress ->
            progress.dismiss()
            mProgress = null
        }
        super.onDestroy()
    }

    fun createAccount() {
        val viewModel: AccountCreationViewModel by viewModels()
        val model = viewModel.model
        val defaultAccountName = getText(R.string.ring_account_default_name).toString()
        if (!model.managementServer.isNullOrEmpty()) {
            presenter.initJamiAccountConnect(model, defaultAccountName)
            mJamsAccount = true
        } else if (model.archive != null) {
            presenter.initJamiAccountBackup(model, getText(R.string.ring_account_default_name).toString())
        } else {
            presenter.initJamiAccountCreation(model, defaultAccountName)
            mJamsAccount = false
        }
    }

    override fun goToHomeCreation() {}
    override fun goToSipCreation() {}
    override fun onBackPressed() {
        when (GuidedStepSupportFragment.getCurrentGuidedStepSupportFragment(supportFragmentManager)) {
            is TVProfileCreationFragment -> finish()
            is TVHomeAccountCreationFragment -> finishAffinity()
            is TVJamiAccountCreationFragment -> supportFragmentManager.popBackStack()
            is TVJamiAccountConnectFragment -> supportFragmentManager.popBackStack()
            else -> super.onBackPressed()
        }
    }

    override fun goToProfileCreation() {
        if (mJamsAccount) {
            setResult(RESULT_OK, Intent())
            finish()
        } else {
            GuidedStepSupportFragment.add(supportFragmentManager, TVProfileCreationFragment())
        }
    }

    override fun displayProgress(display: Boolean) {
        if (display) {
            mProgress = MaterialAlertDialogBuilder(this)
                .setView(ItemProgressDialogBinding.inflate(layoutInflater).root)
                .setTitle(R.string.dialog_wait_create)
                .setMessage(R.string.dialog_wait_create_details)
                .setCancelable(false)
                .show()
        } else {
            mProgress?.let { progress ->
                if (progress.isShowing)
                    progress.dismiss()
                mProgress = null
            }
        }
    }

    override fun displayCreationError() {
        Toast.makeText(
            this@TVAccountWizard,
            getString(R.string.account_creation_error),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun blockOrientation() {
        //Noop on TV
    }

    override fun finish(affinity: Boolean) {
        if (affinity) {
            val fm = fragmentManager
            if (fm.backStackEntryCount >= 1) {
                fm.popBackStack()
            } else {
                finish()
            }
        } else {
            finishAffinity()
        }
    }

    override fun saveProfile(account: Account){
        val model: AccountCreationViewModel by viewModels()
        val base64img = BitmapUtils.bitmapToBase64(model.model.photo as? Bitmap)
        if (base64img != null) {
            presenter.updateProfile(account.accountId, model.model.fullName, base64img, "PNG")
        } else {
            presenter.updateProfile(account.accountId, model.model.fullName, "", "")
        }
    }

    override fun displayGenericError() {
        if (mAlertDialog != null && mAlertDialog!!.isShowing) {
            return
        }
        mAlertDialog = AlertDialog.Builder(this@TVAccountWizard)
            .setPositiveButton(android.R.string.ok, null)
            .setTitle(R.string.account_cannot_be_found_title)
            .setMessage(R.string.account_cannot_be_found_message)
            .show()
    }

    override fun displayNetworkError() {
        if (mAlertDialog != null && mAlertDialog!!.isShowing) {
            return
        }
        mAlertDialog = AlertDialog.Builder(this@TVAccountWizard)
            .setPositiveButton(android.R.string.ok, null)
            .setTitle(R.string.account_no_network_title)
            .setMessage(R.string.account_no_network_message)
            .show()
    }

    override fun displayCannotBeFoundError() {
        if (mAlertDialog != null && mAlertDialog!!.isShowing) return

        val message =
            if (mJamsAccount) getString(R.string.jams_account_cannot_be_found_message)
            else getString(R.string.account_cannot_be_found_message)
        mAlertDialog = AlertDialog.Builder(this@TVAccountWizard)
            .setPositiveButton(android.R.string.ok, null)
            .setTitle(R.string.account_cannot_be_found_title)
            .setMessage(message)
            .show()
    }

    override fun displaySuccessDialog() {
        if (mAlertDialog != null && mAlertDialog!!.isShowing) {
            return
        }
        setResult(RESULT_OK, Intent())
        //startActivity(new Intent(this, HomeActivity.class));
        finish()
    }

    fun profileCreated(saveProfile: Boolean) {
        val model: AccountCreationViewModel by viewModels()
        presenter.profileCreated(model.model, saveProfile)
    }
}