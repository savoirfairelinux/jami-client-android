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
package cx.ring.settings

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnScrollChangedListener
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.account.AccountEditionFragment
import cx.ring.account.JamiAccountSummaryFragment
import cx.ring.client.HomeActivity
import cx.ring.databinding.FragAccountBinding
import cx.ring.utils.DeviceUtils
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.model.Account
import net.jami.services.AccountService
import javax.inject.Inject

@AndroidEntryPoint
class AccountFragment : Fragment(), OnScrollChangedListener {
    private var mBinding: FragAccountBinding? = null
    private val mDisposable = CompositeDisposable()

    @Inject
    lateinit var mAccountService: AccountService

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragAccountBinding.inflate(inflater, container, false).apply {
            scrollview.viewTreeObserver.addOnScrollChangedListener(this@AccountFragment)
            settingsChangePassword.setOnClickListener { (parentFragment as JamiAccountSummaryFragment).onPasswordChangeAsked() }
            settingsExport.setOnClickListener { (parentFragment as JamiAccountSummaryFragment).onClickExport() }
            mBinding = this
        }.root

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
        mDisposable.clear()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        val accountId = requireArguments().getString(AccountEditionFragment.ACCOUNT_ID_KEY)!!
        mDisposable.add(mAccountService.getAccountSingle(accountId)
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe({ account: Account ->
                mBinding?.let { binding ->
                    binding.settingsChangePassword.visibility = if (account.hasManager()) View.GONE else View.VISIBLE
                    binding.settingsExport.visibility = if (account.hasManager()) View.GONE else View.VISIBLE
                    binding.systemChangePasswordTitle.setText(if (account.hasPassword()) R.string.account_password_change else R.string.account_password_set)
                    binding.settingsDeleteAccount.setOnClickListener { createDeleteDialog(account.accountId).show() }
                    binding.settingsBlackList.setOnClickListener {
                        val summaryFragment = parentFragment as JamiAccountSummaryFragment?
                        summaryFragment?.goToBlackList(account.accountId)
                    }
                }
            }) {
                val summaryFragment = parentFragment as JamiAccountSummaryFragment?
                summaryFragment?.popBackStack()
            })
    }

    override fun onScrollChanged() {
        mBinding?.let { binding ->
            val activity: Activity? = activity
            if (activity is HomeActivity)
                activity.setToolbarElevation(binding.scrollview.canScrollVertically(SCROLL_DIRECTION_UP))
        }
    }

    private fun createDeleteDialog(accountId: String): AlertDialog {
        val alertDialog = MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.account_delete_dialog_message)
            .setTitle(R.string.account_delete_dialog_title)
            .setPositiveButton(R.string.menu_delete) { dialog: DialogInterface?, whichButton: Int ->
                mAccountService.removeAccount(accountId)
                (activity as HomeActivity?)?.onBackPressedDispatcher?.onBackPressed()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        activity?.let { activity -> alertDialog.setOwnerActivity(activity) }
        return alertDialog
    }

    companion object {
        val TAG = AccountFragment::class.simpleName!!
        private const val SCROLL_DIRECTION_UP = -1
        fun newInstance(accountId: String) = AccountFragment().apply {
            arguments = Bundle().apply {
                putString(AccountEditionFragment.ACCOUNT_ID_KEY, accountId)
            }
        }
    }
}