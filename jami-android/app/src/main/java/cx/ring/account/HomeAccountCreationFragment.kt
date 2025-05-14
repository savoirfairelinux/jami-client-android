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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import cx.ring.R
import cx.ring.client.HomeActivity
import cx.ring.databinding.FragAccHomeCreateBinding
import cx.ring.fragments.SIPAccountCreationFragment
import cx.ring.mvp.BaseSupportFragment
import cx.ring.utils.AndroidFileUtils.getCacheFile
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.account.HomeAccountCreationPresenter
import net.jami.account.HomeAccountCreationView
import net.jami.model.AccountCreationModel
import net.jami.utils.Log
import java.io.File
import cx.ring.linkdevice.view.LinkDeviceImportSideActivity

@AndroidEntryPoint
class HomeAccountCreationFragment :
    BaseSupportFragment<HomeAccountCreationPresenter, HomeAccountCreationView>(),
    HomeAccountCreationView {
    private var binding: FragAccHomeCreateBinding? = null
    private val model: AccountCreationViewModel by activityViewModels()
    private val mCompositeDisposable = CompositeDisposable()
    private val importBackupLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(StartActivityForResult()) { result ->
            result?.data?.data?.let { uri ->
                getCacheFile(requireContext(), uri)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ file: File ->
                        model.model = AccountCreationModel().apply {
                            archive = file
                        }
                        replaceFragmentWithSlide(
                            fragment = JamiImportBackupFragment(),
                            tag = JamiImportBackupFragment.TAG,
                            containerID = R.id.wizard_container
                        )
                    }) { e: Throwable ->
                        Log.e(TAG, "Error importing archive", e)
                        view?.let { view ->
                            Snackbar.make(
                                view,
                                getString(R.string.import_archive_error),
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }.let { mCompositeDisposable.add(it) }
            }
        }

    private val linkDeviceActivityLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            Log.i(JamiAccountSummaryFragment.TAG, "linkDeviceActivityLauncher: ${result.resultCode}")
            if (result.resultCode == Activity.RESULT_OK) {
                val accountId = result.data // Todo: How to use it ? Need to change currentAccount.
                    ?.getStringExtra(LinkDeviceImportSideActivity.EXTRA_ACCOUNT_ID_KEY)
                startActivity(Intent(requireContext(), HomeActivity::class.java))
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragAccHomeCreateBinding.inflate(inflater, container, false).apply {
            linkDevice.setOnClickListener { presenter.clickOnLinkAccount() }
            ringCreateBtn.setOnClickListener { presenter.clickOnCreateAccount() }
            accountConnectServer.setOnClickListener { presenter.clickOnConnectAccount() }
            ringImportAccount.setOnClickListener { presenter.clickOnBackupAccountLink() }
            sipAddAccount.setOnClickListener { presenter.clickOnCreateSIPAccount() }
            binding = this
        }.root

    override fun onDestroyView() {
        super.onDestroyView()
        mCompositeDisposable.clear()
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        mCompositeDisposable.dispose()
    }

    override fun goToAccountCreation() {
        model.model = AccountCreationModel()
        replaceFragmentWithSlide(JamiAccountCreationFragment(), JamiAccountCreationFragment.TAG, R.id.wizard_container)
    }

    override fun goToAccountLink() {
        linkDeviceActivityLauncher
            .launch(Intent(requireContext(), LinkDeviceImportSideActivity::class.java))
    }

    override fun goToAccountConnect() {
        replaceFragmentWithSlide(JamiAccountConnectFragment(), JamiAccountConnectFragment.TAG, R.id.wizard_container)
    }

    override fun goToSIPAccountCreation() {
        replaceFragmentWithSlide(SIPAccountCreationFragment(), SIPAccountCreationFragment.TAG, R.id.wizard_container)
    }

    override fun goToBackupAccountLink() {
        try {
            importBackupLauncher.launch(
                Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("*/*")
            )
        } catch (e: Exception) {
            view?.let { v ->
                Snackbar.make(v, getString(R.string.browser_error), Snackbar.LENGTH_SHORT).show() }
        }
    }

    companion object {
        val TAG = HomeAccountCreationFragment::class.simpleName!!
    }
}