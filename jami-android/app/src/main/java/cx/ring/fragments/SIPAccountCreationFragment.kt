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
package cx.ring.fragments

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.databinding.FragAccSipCreateBinding
import cx.ring.databinding.ItemProgressDialogBinding
import cx.ring.mvp.BaseSupportFragment
import dagger.hilt.android.AndroidEntryPoint
import net.jami.account.SIPCreationView
import net.jami.account.SIPCreationPresenter

@AndroidEntryPoint
class SIPAccountCreationFragment : BaseSupportFragment<SIPCreationPresenter, SIPCreationView>(),
    SIPCreationView {
    private var mProgress: AlertDialog? = null
    private var binding: FragAccSipCreateBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragAccSipCreateBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding!!.password.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding!!.createSipButton.callOnClick()
            }
            false
        }
        binding!!.createSipButton.setOnClickListener { v: View? -> createSIPAccount(false) }
    }

    /**
     * Start the creation process in the presenter
     *
     * @param bypassWarnings boolean stating if we want to display warning to the user or create the account anyway
     */
    private fun createSIPAccount(bypassWarnings: Boolean) {
        //orientation is locked during the create of account to avoid the destruction of the thread
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        val hostname = binding!!.hostname.text?.toString()
        val proxy = binding!!.proxy.text?.toString()
        val username = binding!!.username.text?.toString()
        val password = binding!!.password.text?.toString()
        presenter.startCreation(hostname, proxy, username, password, bypassWarnings)
    }

    override fun showUsernameError() {
        binding!!.username.error = getString(R.string.error_field_required)
        binding!!.username.requestFocus()
    }

    override fun showLoading() {
        mProgress = MaterialAlertDialogBuilder(requireContext())
            .setView(ItemProgressDialogBinding.inflate(layoutInflater).root)
            .setTitle(R.string.dialog_wait_create)
            .setMessage(R.string.dialog_wait_create_details)
            .setCancelable(false)
            .show()
    }

    override fun resetErrors() {
        binding!!.password.error = null
    }

    override fun showPasswordError() {
        binding!!.password.error = getString(R.string.error_field_required)
        binding!!.password.requestFocus()
    }

    override fun showIP2IPWarning() {
        showDialog(
            getString(R.string.dialog_warn_ip2ip_account_title),
            getString(R.string.dialog_warn_ip2ip_account_message),
            getString(android.R.string.ok),
            getString(android.R.string.cancel),
            { dialog: DialogInterface, which: Int ->
                dialog.dismiss()
                createSIPAccount(true)
            },
            null
        )
    }

    override fun showRegistrationError() {
        showDialog(getString(R.string.account_sip_cannot_be_registered),
            getString(R.string.account_sip_cannot_be_registered_message),
            getString(android.R.string.ok),
            getString(R.string.account_sip_register_anyway),
            { dialog: DialogInterface?, which: Int -> presenter.removeAccount() }
        ) { dialog: DialogInterface?, id: Int ->
            val activity: Activity = requireActivity()
            activity.setResult(Activity.RESULT_OK, Intent())
            activity.finish()
        }
    }

    override fun showRegistrationNetworkError() {
        showDialog(getString(R.string.account_no_network_title),
            getString(R.string.account_no_network_message),
            getString(android.R.string.ok),
            getString(R.string.account_sip_register_anyway),
            { dialog: DialogInterface?, which: Int -> presenter.removeAccount() }
        ) { dialog: DialogInterface?, id: Int ->
            val activity: Activity = requireActivity()
            activity.setResult(Activity.RESULT_OK, Intent())
            activity.finish()
        }
    }

    override fun showRegistrationSuccess() {
        showDialog(
            getString(R.string.account_sip_success_title),
            getString(R.string.account_sip_success_message),
            getString(android.R.string.ok),
            null,
            { dialog: DialogInterface?, which: Int ->
                val activity: Activity = requireActivity()
                activity.setResult(Activity.RESULT_OK, Intent())
                activity.finish()
            },
            null
        )
    }

    fun showDialog(
        title: String?,
        message: String?,
        positive: String?,
        negative: String?,
        listenerPositive: DialogInterface.OnClickListener?,
        listenerNegative: DialogInterface.OnClickListener?
    ) {
        if (mProgress != null && mProgress!!.isShowing) {
            mProgress!!.dismiss()
        }

        //orientation is locked during the create of account to avoid the destruction of the thread
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        MaterialAlertDialogBuilder(requireContext())
            .setPositiveButton(positive, listenerPositive)
            .setNegativeButton(negative, listenerNegative)
            .setTitle(title).setMessage(message)
            .setOnDismissListener { dialog: DialogInterface? ->
                //unlock the screen orientation
                requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            }
            .show()
    }

    companion object {
        val TAG = SIPAccountCreationFragment::class.simpleName!!
    }
}