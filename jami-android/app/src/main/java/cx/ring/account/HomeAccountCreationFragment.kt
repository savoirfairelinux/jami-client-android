/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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
package cx.ring.account

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import cx.ring.R
import cx.ring.databinding.FragAccHomeCreateBinding
import cx.ring.mvp.BaseSupportFragment
import cx.ring.utils.AndroidFileUtils.getCacheFile
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import net.jami.account.HomeAccountCreationPresenter
import net.jami.account.HomeAccountCreationView
import net.jami.model.AccountConfig
import java.io.File

@AndroidEntryPoint
class HomeAccountCreationFragment :
    BaseSupportFragment<HomeAccountCreationPresenter, HomeAccountCreationView>(),
    HomeAccountCreationView {
    private var binding: FragAccHomeCreateBinding? = null
    private val model: AccountCreationViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragAccHomeCreateBinding.inflate(inflater, container, false).apply {
            ringAddAccount.setOnClickListener {presenter.clickOnLinkAccount() }
            ringCreateBtn.setOnClickListener { presenter.clickOnCreateAccount() }
            accountConnectServer.setOnClickListener { presenter.clickOnConnectAccount() }
            ringImportAccount.setOnClickListener { performFileSearch() }
            sipAddAccount.setOnClickListener { presenter.clickOnCreateSIPAccount() }
            binding = this
        }.root

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun goToAccountCreation() {
        model.model = AccountCreationModelImpl()
        replaceFragmentWithSlide(JamiAccountCreationFragment(), R.id.wizard_container)
    }

    override fun goToAccountLink() {
        model.model = AccountCreationModelImpl().apply {
            isLink = true
        }
        //isBackup = false set the title of the page to "Connect From another device"
        replaceFragmentWithSlide(JamiLinkAccountFragment(false), R.id.wizard_container)
    }

    override fun goToAccountConnect() {
        model.model = AccountCreationModelImpl().apply {
            isLink = true
        }
        replaceFragmentWithSlide(JamiAccountConnectFragment(), R.id.wizard_container)
    }

    override fun goToSIPAccountCreation() {
        val intent = Intent(activity, AccountWizardActivity::class.java)
        intent.action = AccountConfig.ACCOUNT_TYPE_SIP
        startActivityForResult(intent, ADD_SIP_ACCOUNT)
    }

    private fun performFileSearch() {
        try {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*"), ARCHIVE_REQUEST_CODE)
        } catch (e: Exception) {
            view?.let { v ->
                Snackbar.make(v, "No file browser available on this device", Snackbar.LENGTH_SHORT).show() }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == ARCHIVE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.let { uri ->
                getCacheFile(requireContext(), uri)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ file: File ->
                        model.model = AccountCreationModelImpl().apply {
                            isLink = true
                            archive = file
                        }
                        //isBackup = true set the title of the page to " Connect from backup"
                        replaceFragmentWithSlide(JamiLinkAccountFragment(true), R.id.wizard_container)
                    }) { e: Throwable ->
                        view?.let { v ->
                            Snackbar.make(v, "Can't import archive: " + e.message, Snackbar.LENGTH_LONG).show() }
                    }
            }
        } else if (requestCode == ADD_SIP_ACCOUNT && resultCode == Activity.RESULT_OK) {
            (activity as AccountWizardActivity?)?.displaySuccessDialog()
        }
    }

    companion object {
        private const val ARCHIVE_REQUEST_CODE = 42
        private const val ADD_SIP_ACCOUNT = 101
        val TAG = HomeAccountCreationFragment::class.simpleName!!
    }
}