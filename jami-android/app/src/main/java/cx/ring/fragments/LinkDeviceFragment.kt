/*
 * Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 * Authors:    AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.fragments

import android.app.Dialog
import android.graphics.Typeface
import android.os.Bundle
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AlignmentSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.account.AccountEditionFragment
import cx.ring.databinding.FragLinkDeviceBinding
import cx.ring.mvp.BaseBottomSheetFragment
import cx.ring.utils.KeyboardVisibilityManager.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint
import net.jami.account.LinkDevicePresenter
import net.jami.account.LinkDeviceView
import net.jami.model.Account

@AndroidEntryPoint
class LinkDeviceFragment : BaseBottomSheetFragment<LinkDevicePresenter>(), LinkDeviceView {
    private var mBinding: FragLinkDeviceBinding? = null
    private var mAccountHasPassword = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View =
        // Inflate from XML layout (using bindings).
        FragLinkDeviceBinding.inflate(inflater, container, false).apply {
            btnStartExport.setOnClickListener { onClickStart() }
            // Password of the account.
            password.setOnEditorActionListener { pwd: TextView, actionId: Int, event: KeyEvent? ->
                onPasswordEditorAction(pwd, actionId, event)
            }
            mBinding = this
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let { arguments ->
            arguments.getString(AccountEditionFragment.ACCOUNT_ID_KEY)?.let { accountId ->
                presenter.setAccountId(accountId)
            }
        }
        // Display password editor only if account has a password protection.
        mBinding?.apply {
            passwordLayout.visibility = if (mAccountHasPassword) View.VISIBLE else View.GONE
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        (dialog as BottomSheetDialog).behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
        return dialog
    }

    override fun onDestroyView() {
        mBinding = null
        super.onDestroyView()
    }

    /**
     * Show progress bar (make user think that app proceeds to the exportation of his account).
     */
    override fun showExportingProgress() {
        mBinding?.apply {
            progressBar.visibility = View.VISIBLE
            accountLinkInfo.visibility = View.GONE
            btnStartExport.visibility = View.GONE
            passwordLayout.visibility = View.GONE
        }
    }

    /**
     * Hide progress bar.
     */
    override fun dismissExportingProgress() {
        mBinding?.apply {
            progressBar.visibility = View.GONE
            accountLinkInfo.visibility = View.VISIBLE
            btnStartExport.visibility = View.VISIBLE
            passwordLayout.visibility = if (mAccountHasPassword) View.VISIBLE else View.GONE
        }
    }

    override fun accountChanged(account: Account) {
        mAccountHasPassword = account.hasPassword()
    }

    override fun showNetworkError() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.account_export_end_network_title)
            .setMessage(R.string.account_export_end_network_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun showPasswordError() {
        mBinding!!.passwordLayout.error = getString(R.string.account_export_end_decryption_message)
        mBinding!!.password.setText("")
    }

    override fun showGenericError() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.account_export_end_error_title)
            .setMessage(R.string.account_export_end_error_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    /**
     * Display generated PIN.
     * Using this PIN, user will be able to link his account to other devices.
     */
    override fun showPIN(pin: String) {
        dismissExportingProgress()
        val messagePIN = getString(R.string.account_end_export_infos).replace("%%", pin)
        mBinding?.let { binding ->
            binding.password.setText("")
            binding.passwordLayout.visibility = View.GONE
            binding.btnStartExport.visibility = View.GONE
            binding.accountLinkInfo.text = SpannableString(messagePIN).apply {
                val pos = messagePIN.lastIndexOf(pin)
                setSpan(
                    AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), pos,
                    pos + pin.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                setSpan(
                    StyleSpan(Typeface.BOLD), pos, pos + pin.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                setSpan(
                    RelativeSizeSpan(2.8f), pos, pos + pin.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            binding.accountLinkInfo.requestFocus()
        }
        hideKeyboard(activity)
    }

    /**
     * Called when user click on button to generate the PIN code.
     */
    private fun onClickStart() {
        mBinding?.let { binding ->
            binding.passwordLayout.error = null
            presenter.startAccountExport(binding.password.text.toString())
        }
    }

    /**
     * Called when password is edited.
     */
    private fun onPasswordEditorAction(pwd: TextView, actionId: Int, event: KeyEvent?): Boolean {
        Log.i(TAG, "onEditorAction " + actionId + " " + event?.toString())
        // Check password if is not empty.
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            if (pwd.text.isEmpty()) {
                pwd.error = getString(R.string.account_enter_password)
            } else {
                onClickStart()
                return true
            }
        }
        return false
    }

    companion object {
        val TAG = LinkDeviceFragment::class.simpleName!!
        fun newInstance(accountId: String) = LinkDeviceFragment().apply {
            arguments = Bundle().apply {
                putString(AccountEditionFragment.ACCOUNT_ID_KEY, accountId)
            }
        }
    }
}