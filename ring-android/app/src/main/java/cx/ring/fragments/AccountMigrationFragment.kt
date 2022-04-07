/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.fragments

import android.app.Activity
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.account.AccountEditionFragment
import cx.ring.databinding.FragAccountMigrationBinding
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.AccountConfig
import net.jami.model.ConfigKey
import net.jami.services.AccountService
import javax.inject.Inject

@AndroidEntryPoint
class AccountMigrationFragment : Fragment() {
    @Inject
    lateinit var mAccountService: AccountService
    private var binding: FragAccountMigrationBinding? = null
    private var mAccountId: String? = null
    private var mProgress: ProgressDialog? = null
    private var migratingAccount = false
    private val mDisposableBag = CompositeDisposable()
    override fun onDestroy() {
        super.onDestroy()
        mDisposableBag.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragAccountMigrationBinding.inflate(inflater, parent, false).apply {
            ringPassword.setOnEditorActionListener { v: TextView, actionId: Int, event: KeyEvent? ->
                actionId == EditorInfo.IME_ACTION_NEXT && checkPassword(v, null)
            }
            ringPassword.onFocusChangeListener = View.OnFocusChangeListener { v: View, hasFocus: Boolean ->
                if (!hasFocus) {
                    checkPassword(v as TextView, null)
                }
            }
            ringMigrateBtn.setOnClickListener { initAccountMigration(ringPassword.text.toString()) }
            deleteBtn.setOnClickListener { initAccountDelete() }
            binding = this
        }.root
    }

    override fun onResume() {
        super.onResume()
        if (arguments != null) {
            mAccountId = requireArguments().getString(AccountEditionFragment.ACCOUNT_ID_KEY)
        }
    }

    private fun initAccountDelete() {
        if (migratingAccount) return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.account_delete_dialog_title)
            .setMessage(R.string.account_delete_dialog_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.menu_delete) { d: DialogInterface?, w: Int -> deleteAccount() }
            .create()
            .show()
    }

    private fun deleteAccount() {
        mAccountService.removeAccount(mAccountId!!)
    }

    private fun initAccountMigration(password: String) {
        if (migratingAccount) return
        migratingAccount = true

        //orientation is locked during the migration of account to avoid the destruction of the thread
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        mProgress = ProgressDialog(requireContext()).apply {
            setTitle(R.string.dialog_wait_update)
            setMessage(getString(R.string.dialog_wait_update_details))
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            show()
        }
        val accountId = mAccountId!!
        val account = mAccountService.getAccount(accountId)!!
        val details = account.details
        details[ConfigKey.ARCHIVE_PASSWORD.key()] = password
        mAccountService.setAccountDetails(account.accountId, details)
        mDisposableBag.add(mAccountService.migrateAccount(accountId, password)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { newState: String -> handleMigrationState(newState) })
    }

    private fun checkPassword(pwd: TextView, confirm: TextView?): Boolean {
        var error = false
        if (pwd.text.isEmpty()) {
            error = true
        } else {
            if (pwd.text.length < 6) {
                pwd.error = getString(R.string.error_password_char_count)
                error = true
            } else {
                pwd.error = null
            }
        }
        if (confirm != null) {
            if (pwd.text.toString() != confirm.text.toString()) {
                confirm.error = getString(R.string.error_passwords_not_equals)
                confirm.requestFocus()
                error = true
            } else {
                confirm.error = null
            }
        }
        return error
    }

    private fun handleMigrationState(newState: String) {
        migratingAccount = false
        mProgress?.let { progress ->
            progress.dismiss()
            mProgress = null
        }
        if (TextUtils.isEmpty(newState)) {
            return
        }
        val dialogBuilder: AlertDialog.Builder = MaterialAlertDialogBuilder(requireContext())
        dialogBuilder.setPositiveButton(android.R.string.ok) { dialog: DialogInterface?, id: Int -> }
        var success = false
        if (AccountConfig.STATE_INVALID == newState) {
            dialogBuilder.setTitle(R.string.account_cannot_be_found_title)
                .setMessage(R.string.account_cannot_be_updated_message)
        } else {
            dialogBuilder.setTitle(R.string.account_device_updated_title)
                .setMessage(R.string.account_device_updated_message)
            success = true
        }
        val dialogSuccess = dialogBuilder.show()
        if (success) {
            dialogSuccess.setOnDismissListener {
                val activity: Activity? = activity
                if (activity != null) {
                    activity.setResult(Activity.RESULT_OK, Intent())
                    //unlock the screen orientation
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    activity.finish()
                }
            }
        }
    }

    companion object {
        val TAG = AccountMigrationFragment::class.simpleName!!
    }
}