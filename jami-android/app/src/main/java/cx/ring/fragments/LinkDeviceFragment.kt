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
package cx.ring.fragments

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.format.DateUtils
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.account.AccountEditionFragment
import cx.ring.databinding.FragLinkDeviceBinding
import cx.ring.mvp.BaseBottomSheetFragment
import cx.ring.utils.KeyboardVisibilityManager.hideKeyboard
import cx.ring.utils.TextUtils.copyAndShow
import dagger.hilt.android.AndroidEntryPoint
import net.jami.account.LinkDevicePresenter
import net.jami.account.LinkDeviceView
import net.jami.model.Account
import net.jami.utils.QRCodeUtils

@AndroidEntryPoint
class LinkDeviceFragment : BaseBottomSheetFragment<LinkDevicePresenter>(), LinkDeviceView {
    private var mBinding: FragLinkDeviceBinding? = null
    private var mAccountHasPassword = true
    private var counter: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragLinkDeviceBinding.inflate(inflater, container, false).apply {
        btnStartExport.setOnClickListener { startAccountExport() }
        password.setOnEditorActionListener { pwd: TextView, actionId: Int, event: KeyEvent? ->
            onPasswordEditorAction(pwd, actionId, event)
        }
        pin.setOnClickListener { copyAndShow(requireContext(), PIN_LABEL, pin.text.toString()) }

        mBinding = this
        pageContainer.visibility = View.GONE
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let { arguments ->
            arguments.getString(AccountEditionFragment.ACCOUNT_ID_KEY)?.let { accountId ->
                presenter.setAccountId(accountId)
            }
        }
        // go directly to the qr and pin page if there is no account password
        if (!mAccountHasPassword) startAccountExport()
    }

    override fun onDestroyView() {
        mBinding = null
        super.onDestroyView()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        (dialog as BottomSheetDialog).behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
        return dialog
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
            accountLinkInfo.visibility = if (mAccountHasPassword) View.VISIBLE else View.GONE
            btnStartExport.visibility = View.VISIBLE
            passwordLayout.visibility = if (mAccountHasPassword) View.VISIBLE else View.GONE
        }
    }

    fun regeneratePin() {
        mBinding?.apply {
            progressBar.visibility = View.GONE
            btnStartExport.visibility = View.VISIBLE
            passwordLayout.visibility = if (mAccountHasPassword) View.VISIBLE else View.GONE
            if (!mAccountHasPassword) {
                mBinding!!.accountLinkInfo.text = getString(R.string.account_generate_export_invalid_two)
            }
            accountLinkInfo.visibility = View.VISIBLE
        }
    }

    override fun accountChanged(account: Account) {
        mAccountHasPassword = account.hasPassword()
    }

    override fun showNetworkError() {
        dismiss()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.account_export_end_network_title)
            .setMessage(R.string.account_export_end_network_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun showPasswordError() {
        mBinding!!.passwordLayout.error = getString(R.string.account_export_end_decryption_message)
        mBinding!!.password.setText("")
        mBinding!!.pageContainer.visibility = View.GONE
    }

    override fun showGenericError() {
        dismiss()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.account_export_end_error_title)
            .setMessage(R.string.account_export_end_error_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun onStop() {
        super.onStop()
        counter?.cancel()
        counter = null
    }

    override fun showPIN(pin: String) {
        val binding = mBinding ?: return
        dismissExportingProgress()
        // encode the qr code with the pin generated
        val qrCodeData = QRCodeUtils.encodeStringAsQRCodeData(
            pin,
            resources.getColor(R.color.black, null),
            resources.getColor(R.color.white, null)
        )
        var bitmap = qrCodeData?.let {
            Bitmap.createBitmap(it.width, qrCodeData.height, Bitmap.Config.ARGB_8888).apply {
                setPixels(qrCodeData.data, 0, qrCodeData.width, 0, 0, qrCodeData.width, qrCodeData.height)
            }
        }
        binding.qrImage.setImageBitmap(bitmap)
        // show the pin generated in the interface
        binding.pin.text = pin
        val start = System.currentTimeMillis()
        // to have the count down of 10 min
        val duration = 10 * DateUtils.MINUTE_IN_MILLIS
        counter = object : CountDownTimer(duration, DateUtils.MINUTE_IN_MILLIS) {
            override fun onTick(millisUntilFinished: Long) {
                val expIn = DateUtils.getRelativeTimeSpanString(
                    start + duration,
                    System.currentTimeMillis(),
                    0L
                )
                binding.pinTimeValid.text = getString(R.string.account_link_time_valid, expIn)
            }

            override fun onFinish() {
                // return to the generate pin page
                hideKeyboard(activity)
                // change the text because the pin is now invalid
                if (!mAccountHasPassword) mBinding!!.accountLinkInfo.text =
                    R.string.account_generate_export_invalid.toString()
                if (mAccountHasPassword) {
                    val infoText = getString(R.string.account_generate_export_invalid)
                    mBinding!!.accountLinkInfo.text = infoText
                }
                mBinding!!.pageContainer.visibility = View.GONE
                regeneratePin()
                // liberate the memory
                bitmap?.recycle()
                bitmap = null
            }
        }.start()
        mBinding!!.pin.visibility = View.VISIBLE
        mBinding?.apply {
            pageContainer.visibility = View.VISIBLE
            accountLinkInfo.visibility = View.GONE
            btnStartExport.visibility = View.GONE
            passwordLayout.visibility = View.GONE
            accountLinkInfo.visibility = View.GONE
            password.text = null
        }
        hideKeyboard(activity)
    }

    private fun startAccountExport() {
        mBinding?.let { binding ->
            binding.passwordLayout.error = null
            presenter.startAccountExport(binding.password.text.toString())
        }
    }

    private fun onPasswordEditorAction(pwd: TextView, actionId: Int, event: KeyEvent?): Boolean {
        Log.i(TAG, "onEditorAction " + actionId + " " + event?.toString())
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            if (pwd.text.isEmpty()) {
                pwd.error = getString(R.string.account_enter_password)
            } else {
                startAccountExport()
                return true
            }
        }
        return false
    }

    companion object {
        val TAG = LinkDeviceFragment::class.simpleName!!
        const val PIN_LABEL = "PIN_LABEL"
        fun newInstance(accountId: String) = LinkDeviceFragment().apply {
            arguments = Bundle().apply {
                putString(AccountEditionFragment.ACCOUNT_ID_KEY, accountId)
            }
        }
    }
}