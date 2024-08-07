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
import cx.ring.fragments.SIPAccountCreationFragment
import cx.ring.mvp.BaseSupportFragment
import cx.ring.utils.AndroidFileUtils.getCacheFile
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import net.jami.account.HomeAccountCreationPresenter
import net.jami.account.HomeAccountCreationView
import net.jami.utils.Log
import java.io.File

@AndroidEntryPoint
class HomeAccountCreationFragment :
    BaseSupportFragment<HomeAccountCreationPresenter, HomeAccountCreationView>(),
    HomeAccountCreationView {
    private var binding: FragAccHomeCreateBinding? = null
    private val model: AccountCreationViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragAccHomeCreateBinding.inflate(inflater, container, false).apply {
            ringAddAccount.setOnClickListener { goToAccountLink() }
            ringCreateBtn.setOnClickListener { goToAccountCreation() }
            accountConnectServer.setOnClickListener { goToAccountConnect() }
            ringImportAccount.setOnClickListener { performFileSearch() }
            sipAddAccount.setOnClickListener { goToSIPAccountCreation() }
            binding = this
        }.root

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun goToAccountCreation() {
        model.model = AccountCreationModelImpl()
        replaceFragmentWithSlide(JamiAccountCreationFragment(), JamiAccountCreationFragment.TAG, R.id.wizard_container)
    }

    override fun goToAccountLink() {
        model.model = AccountCreationModelImpl().apply {
            isLink = true
        }
        replaceFragmentWithSlide(JamiLinkAccountFragment(), JamiLinkAccountFragment.TAG, R.id.wizard_container)
    }

    override fun goToAccountConnect() {
        model.model = AccountCreationModelImpl().apply {
            isLink = true
        }
        replaceFragmentWithSlide(JamiAccountConnectFragment(), JamiAccountConnectFragment.TAG, R.id.wizard_container)
    }

    override fun goToSIPAccountCreation() {
        replaceFragmentWithSlide(SIPAccountCreationFragment(), SIPAccountCreationFragment.TAG, R.id.wizard_container)
    }

    private fun performFileSearch() {
        try {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*"), ARCHIVE_REQUEST_CODE)
        } catch (e: Exception) {
            view?.let { v ->
                Snackbar.make(v, getString(R.string.browser_error), Snackbar.LENGTH_SHORT).show() }
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
                        replaceFragmentWithSlide(
                                JamiLinkAccountFragment(), JamiLinkAccountFragment.TAG, R.id.wizard_container)
                    }) { e: Throwable ->
                        Log.e(TAG, "Error importing archive", e)
                        view?.let { v ->
                            Snackbar.make(v, getString(R.string.import_archive_error), Snackbar.LENGTH_LONG).show() }
                    }
            }
        }
//        else if (requestCode == ADD_SIP_ACCOUNT && resultCode == Activity.RESULT_OK) {
//            (activity as AccountWizardActivity?)?.displaySuccessDialog()
//        }
    }

    companion object {
        private const val ARCHIVE_REQUEST_CODE = 42
        val TAG = HomeAccountCreationFragment::class.simpleName!!
    }
}