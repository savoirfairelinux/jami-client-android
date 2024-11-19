/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import cx.ring.account.AccountEditionFragment
import cx.ring.databinding.FragLinkDeviceBinding
import cx.ring.mvp.BaseBottomSheetFragment
import dagger.hilt.android.AndroidEntryPoint
import net.jami.account.LinkAccountExportPresenter
import net.jami.account.LinkAccountExportView
import net.jami.services.AccountService
import net.jami.utils.Log

@AndroidEntryPoint
class LinkAccountExportFragment : BaseBottomSheetFragment<LinkAccountExportPresenter>(), LinkAccountExportView {
    private var mBinding: FragLinkDeviceBinding? = null

    // Todo utils for other fragment too.
    fun isTokenValid(token: String): Boolean {
        // Contains `jami-auth://` scheme
        return token.startsWith("jami-auth://")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragLinkDeviceBinding.inflate(inflater, container, false).apply {
        // Todo: use logic from QrCodePinInputFragment & EditTextPinInputFragment
        barcodeScanner.decodeContinuous { result ->
            val qrCodeText = result.text
            Log.w("devdebug", "QR code text: $qrCodeText isTokenValid: ${isTokenValid(qrCodeText)}")
            if (isTokenValid(qrCodeText)) {
                barcodeScanner.pause()
                presenter.exportToPeer(qrCodeText!!)
//                presenter.setAccountId()
            }
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
    }

    override fun onResume() {
        super.onResume()
        mBinding?.barcodeScanner?.resume()
    }

    override fun onPause() {
        super.onPause()
        mBinding?.barcodeScanner?.pause()
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

    companion object {
        val TAG = LinkAccountExportFragment::class.simpleName!!
        fun newInstance(accountId: String) = LinkAccountExportFragment().apply {
            arguments = Bundle().apply {
                putString(AccountEditionFragment.ACCOUNT_ID_KEY, accountId)
            }
        }
    }

    override fun showLoadingToken() {
//        TODO("Not yet implemented")
    }

    override fun showTokenAvailable(token: String) {
//        TODO("Not yet implemented")
    }

    override fun showConnecting() {
//        TODO("Not yet implemented")
    }

    override fun showAuthenticating() {
//        TODO("Not yet implemented")
    }

    override fun showDone() {
//        TODO("Not yet implemented")
    }

    override fun showError(linkDeviceError: AccountService.DeviceAuthStateError) {
//        TODO("Not yet implemented")
    }
}