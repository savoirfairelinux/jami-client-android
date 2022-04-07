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
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.account.AccountEditionFragment
import cx.ring.databinding.FragLinkDeviceBinding
import cx.ring.mvp.BaseBottomSheetFragment
import cx.ring.utils.DeviceUtils.isTablet
import cx.ring.utils.KeyboardVisibilityManager.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint
import net.jami.account.LinkDevicePresenter
import net.jami.account.LinkDeviceView
import net.jami.model.Account

@AndroidEntryPoint
class LinkDeviceFragment : BaseBottomSheetFragment<LinkDevicePresenter>(), LinkDeviceView {
    private var mBinding: FragLinkDeviceBinding? = null
    private var mAccountHasPassword = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        mBinding = FragLinkDeviceBinding.inflate(inflater, container, false)
        return mBinding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let { arguments ->
            arguments.getString(AccountEditionFragment.ACCOUNT_ID_KEY)?.let { accountId ->
                presenter.setAccountId(accountId)
            }
        }
        mBinding!!.btnStartExport.setOnClickListener { onClickStart() }
        mBinding!!.password.setOnEditorActionListener { pwd: TextView, actionId: Int, event: KeyEvent? ->
            onPasswordEditorAction(pwd, actionId, event)
        }
        mBinding!!.passwordLayout.visibility = if (mAccountHasPassword) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        mBinding = null
        super.onDestroyView()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener {
            if (isTablet(requireContext())) {
                dialog.window!!.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
        }
        return dialog
    }

    override fun onResume() {
        super.onResume()
        addGlobalLayoutListener(requireView())
    }

    private fun addGlobalLayoutListener(view: View) {
        view.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(v: View, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                setPeekHeight(v.measuredHeight)
                v.removeOnLayoutChangeListener(this)
            }
        })
    }

    fun setPeekHeight(peekHeight: Int) {
        bottomSheetBehaviour?.peekHeight = peekHeight
    }

    private val bottomSheetBehaviour: BottomSheetBehavior<*>?
        get() {
            val layoutParams = (requireView().parent as View).layoutParams as CoordinatorLayout.LayoutParams
            val behavior = layoutParams.behavior
            return if (behavior is BottomSheetBehavior<*>) behavior else null
        }

    override fun showExportingProgress() {
        mBinding?.apply {
            progressBar.visibility = View.VISIBLE
            accountLinkInfo.visibility = View.GONE
            btnStartExport.visibility = View.GONE
            passwordLayout.visibility = View.GONE
        }
    }

    override fun dismissExportingProgress() {
        mBinding?.apply {
            progressBar.visibility = View.GONE
            accountLinkInfo.visibility = View.VISIBLE
            btnStartExport.visibility = View.VISIBLE
            passwordLayout.visibility = if (mAccountHasPassword) View.VISIBLE else View.GONE
        }
    }

    override fun accountChanged(account: Account?) {
        mAccountHasPassword = account!!.hasPassword()
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

    override fun showPIN(pin: String) {
        dismissExportingProgress()
        mBinding!!.password.setText("")
        mBinding!!.passwordLayout.visibility = View.GONE
        mBinding!!.btnStartExport.visibility = View.GONE
        val pined = getString(R.string.account_end_export_infos).replace("%%", pin)
        val styledResultText = SpannableString(pined)
        val pos = pined.lastIndexOf(pin)
        styledResultText.setSpan(
            AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
            pos,
            pos + pin.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        styledResultText.setSpan(StyleSpan(Typeface.BOLD), pos, pos + pin.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        styledResultText.setSpan(RelativeSizeSpan(2.8f), pos, pos + pin.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        mBinding!!.accountLinkInfo.text = styledResultText
        mBinding!!.accountLinkInfo.requestFocus()
        hideKeyboard(activity)
    }

    private fun onClickStart() {
        mBinding!!.passwordLayout.error = null
        val password = mBinding!!.password.text.toString()
        presenter.startAccountExport(password)
    }

    private fun onPasswordEditorAction(pwd: TextView, actionId: Int, event: KeyEvent?): Boolean {
        Log.i(TAG, "onEditorAction " + actionId + " " + event?.toString())
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
        fun newInstance(accountId: String): LinkDeviceFragment {
            val fragment = LinkDeviceFragment()
            val args = Bundle()
            args.putString(AccountEditionFragment.ACCOUNT_ID_KEY, accountId)
            fragment.arguments = args
            return fragment
        }
    }
}