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
package cx.ring.account

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.client.HomeActivity
import cx.ring.databinding.ItemProgressDialogBinding
import cx.ring.fragments.AccountMigrationFragment
import cx.ring.fragments.SIPAccountCreationFragment
import cx.ring.mvp.BaseActivity
import cx.ring.utils.BiometricHelper
import cx.ring.utils.BitmapUtils
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.account.AccountWizardPresenter
import net.jami.account.AccountWizardView
import net.jami.model.Account
import net.jami.model.AccountConfig

@AndroidEntryPoint
class AccountWizardActivity : BaseActivity<AccountWizardPresenter>(), AccountWizardView {
    private var mProgress: AlertDialog? = null
    private var mAlertDialog: AlertDialog? = null
    private var biometricEnroll: BiometricHelper.BiometricEnroll? = null
    private val enrollBiometricLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        biometricEnroll?.onActivityResult(it.resultCode, it.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        JamiApplication.instance?.startDaemon(this)
        setContentView(R.layout.activity_wizard)

        // ======= check if migration is needed =======
        val path = intent?.data?.lastPathSegment
        if (path != null) {     // start migration
            val fragment = AccountMigrationFragment().apply {
                arguments = Bundle().apply { putString(AccountEditionFragment.ACCOUNT_ID_KEY, path) }
            }

            supportFragmentManager.beginTransaction()
                    .replace(R.id.wizard_container, fragment)
                    .commit()
        } else  // migration is not needed
            presenter.init(intent.action ?: AccountConfig.ACCOUNT_TYPE_JAMI)
    }

    override fun onDestroy() {
        mProgress?.let { progress ->
            progress.dismiss()
            mProgress = null
        }
        mAlertDialog?.let { alertDialog ->
            alertDialog.setOnDismissListener(null)
            alertDialog.dismiss()
            mAlertDialog = null
        }
        biometricEnroll?.let {
            it.dispose()
            biometricEnroll = null
        }
        super.onDestroy()
    }

    override fun saveProfile(account: Account) {
        val model: AccountCreationViewModel by viewModels()
        val name = model.model.fullName
        val photo = model.model.photo as? Bitmap?
        if (photo != null) {
            Single.just(photo).map { BitmapUtils.bitmapToBase64(it)!! }
                .observeOn(Schedulers.computation())
                .subscribe({ presenter.updateProfile(account.accountId, name, it, "PNG") })
                { e -> Log.e(TAG, "Error updating profile", e) }
        } else {
            presenter.updateProfile(account.accountId, model.model.fullName, "", "")
        }
    }

    fun createAccount() {
        val viewModel: AccountCreationViewModel by viewModels()
        val model = viewModel.model
        if (!TextUtils.isEmpty(model.managementServer)) {
            presenter.initJamiAccountConnect(model, getText(R.string.ring_account_default_name).toString())
        } else if (model.isLink) {
            presenter.initJamiAccountLink(model, getText(R.string.ring_account_default_name).toString())
        } else {
            presenter.initJamiAccountCreation(model, getText(R.string.ring_account_default_name).toString())
        }
    }

    override fun goToHomeCreation() {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.wizard_container, HomeAccountCreationFragment(), HomeAccountCreationFragment.TAG)
                .commit()
    }

    override fun goToSipCreation() {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.wizard_container, SIPAccountCreationFragment(), SIPAccountCreationFragment.TAG)
                .commit()
    }

    override fun goToProfileCreation() {
        val fragmentManager = supportFragmentManager
        val fragment = fragmentManager.fragments.firstOrNull() ?: return
        val model: AccountCreationViewModel by viewModels()
        if (model.model.managementServer.isNullOrBlank()) {
            biometricEnroll = BiometricHelper.BiometricEnroll(
                null,
                fragment,
                model.model.password,
                presenter.mAccountService,
                { doGoToProfileCreation() },
                model.model.accountObservable!!,
                enrollBiometricLauncher
            ).apply { start() }
        } else {
            doGoToProfileCreation()
        }
    }

    private fun doGoToProfileCreation() {
        val fragments = supportFragmentManager.fragments
        if (fragments.size > 0) {
            val fragment = fragments[0]
            if (fragment is JamiLinkAccountFragment || fragment is JamiAccountConnectFragment) {
                profileCreated(false)
            }
        }
    }

    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.wizard_container)
        if (fragment is ProfileCreationFragment) finish()
        else super.onBackPressed()
    }

    override fun displayProgress(display: Boolean) {
        if (display) {
            mProgress = MaterialAlertDialogBuilder(this@AccountWizardActivity)
                .setView(ItemProgressDialogBinding.inflate(layoutInflater).root)
                .setTitle(R.string.dialog_wait_create)
                .setMessage(getString(R.string.dialog_wait_create_details))
                .setCancelable(false)
                .show()
        } else {
            mProgress?.apply {
                if (isShowing) dismiss()
                mProgress = null
            }
        }
    }

    override fun displayCreationError() {
        Toast.makeText(
            this@AccountWizardActivity,
            getString(R.string.account_creation_error),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun blockOrientation() {
        //orientation is locked during the create of account to avoid the destruction of the thread
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
    }

    override fun finish(affinity: Boolean) {
        if (affinity) {
            startActivity(Intent(this@AccountWizardActivity, HomeActivity::class.java))
            finish()
        } else {
            finishAffinity()
        }
    }

    override fun displayGenericError() {
        if (mAlertDialog != null && mAlertDialog!!.isShowing) {
            return
        }
        mAlertDialog = MaterialAlertDialogBuilder(this@AccountWizardActivity)
            .setPositiveButton(android.R.string.ok, null)
            .setTitle(R.string.account_cannot_be_found_title)
            .setMessage(R.string.account_export_end_decryption_message)
            .show()
    }

    override fun displayNetworkError() {
        if (mAlertDialog != null && mAlertDialog!!.isShowing) {
            return
        }
        mAlertDialog = MaterialAlertDialogBuilder(this@AccountWizardActivity)
            .setPositiveButton(android.R.string.ok, null)
            .setTitle(R.string.account_no_network_title)
            .setMessage(R.string.account_no_network_message)
            .show()
    }

    override fun displayCannotBeFoundError() {
        if (mAlertDialog != null && mAlertDialog!!.isShowing) {
            return
        }
        mAlertDialog = MaterialAlertDialogBuilder(this@AccountWizardActivity)
            .setPositiveButton(android.R.string.ok, null)
            .setTitle(R.string.account_cannot_be_found_title)
            .setMessage(R.string.account_cannot_be_found_message)
            .setOnDismissListener { supportFragmentManager.popBackStack() }
            .show()
    }

    override fun displaySuccessDialog() {
        if (mAlertDialog != null && mAlertDialog!!.isShowing) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_PERMISSION_NOTIF)
        } else {
            finishCreation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSION_NOTIF)
            finishCreation()
        else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun finishCreation() {
        setResult(RESULT_OK, Intent())
        //unlock the screen orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        presenter.successDialogClosed()
    }

    fun profileCreated(saveProfile: Boolean) {
        val model: AccountCreationViewModel by viewModels()
        presenter.profileCreated(model.model, saveProfile)
    }

    companion object {
        val TAG = AccountWizardActivity::class.simpleName!!
        const val REQUEST_PERMISSION_NOTIF = 5
    }
}