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
import net.jami.account.LinkDevicePresenter
import net.jami.account.LinkDeviceView

// Todo: Legacy
@AndroidEntryPoint
class LinkDeviceFragment : BaseBottomSheetFragment<LinkDevicePresenter>(), LinkDeviceView {
    private var mBinding: FragLinkDeviceBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragLinkDeviceBinding.inflate(inflater, container, false).apply {
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
        val TAG = LinkDeviceFragment::class.simpleName!!
        fun newInstance(accountId: String) = LinkDeviceFragment().apply {
            arguments = Bundle().apply {
                putString(AccountEditionFragment.ACCOUNT_ID_KEY, accountId)
            }
        }
    }
}