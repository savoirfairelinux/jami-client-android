/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.tv.account

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.leanback.app.GuidedStepSupportFragment
import cx.ring.R
import cx.ring.account.AccountCreationViewModel
import cx.ring.application.JamiApplication
import cx.ring.mvp.BaseActivity
import cx.ring.services.VCardServiceImpl
import dagger.hilt.android.AndroidEntryPoint
import ezvcard.VCard
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.account.AccountWizardPresenter
import net.jami.account.AccountWizardView
import net.jami.model.Account
import net.jami.model.AccountConfig
import net.jami.utils.VCardUtils

@AndroidEntryPoint
class TVAccountWizard : BaseActivity<AccountWizardPresenter>(), AccountWizardView {
    private var mProgress: ProgressDialog? = null
    private var mLinkAccount = false
    private var mAccountType: String? = null
    private var mAlertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            GuidedStepSupportFragment.addAsRoot(this, TVHomeAccountCreationFragment(), android.R.id.content)
        } else {
            mLinkAccount = savedInstanceState.getBoolean("mLinkAccount")
        }
        presenter.init(getIntent().action ?: AccountConfig.ACCOUNT_TYPE_JAMI)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("mLinkAccount", mLinkAccount)
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
        if (model.isLink) {
            presenter.initJamiAccountLink(model, getText(R.string.ring_account_default_name).toString())
        } else {
            presenter.initJamiAccountCreation(model, getText(R.string.ring_account_default_name).toString())
        }
    }

    override fun goToHomeCreation() {}
    override fun goToSipCreation() {}
    override fun onBackPressed() {
        when (GuidedStepSupportFragment.getCurrentGuidedStepSupportFragment(supportFragmentManager)) {
            is TVProfileCreationFragment -> finish()
            is TVHomeAccountCreationFragment -> finishAffinity()
            is TVJamiAccountCreationFragment -> supportFragmentManager.popBackStack()
            else -> super.onBackPressed()
        }
    }

    override fun goToProfileCreation() {
        GuidedStepSupportFragment.add(supportFragmentManager, TVProfileCreationFragment())
    }

    override fun displayProgress(display: Boolean) {
        if (display) {
            mProgress = ProgressDialog(this).apply {
                setTitle(R.string.dialog_wait_create)
                setMessage(getString(R.string.dialog_wait_create_details))
                setCancelable(false)
                setCanceledOnTouchOutside(false)
                show()
            }
        } else {
            mProgress?.let { progress ->
                if (progress.isShowing)
                    progress.dismiss()
                mProgress = null
            }
        }
    }

    override fun displayCreationError() {
        Toast.makeText(this@TVAccountWizard, "Error creating account", Toast.LENGTH_SHORT).show()
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

    override fun saveProfile(account: Account): Single<VCard> {
        val filedir = filesDir
        val model: AccountCreationViewModel by viewModels()
        return model.model.toVCard()
            .flatMap { vcard ->
                account.loadedProfile = Single.fromCallable { VCardServiceImpl.readData(vcard) }.cache()
                VCardUtils.saveLocalProfileToDisk(vcard, account.accountId, filedir)
            }
            .subscribeOn(Schedulers.io())
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
        if (mAlertDialog != null && mAlertDialog!!.isShowing) {
            return
        }
        mAlertDialog = AlertDialog.Builder(this@TVAccountWizard)
            .setPositiveButton(android.R.string.ok, null)
            .setTitle(R.string.account_cannot_be_found_title)
            .setMessage(R.string.account_cannot_be_found_message)
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