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
package cx.ring.tv.account

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.activityViewModels
import androidx.leanback.widget.GuidanceStylist.Guidance
import androidx.leanback.widget.GuidedAction
import cx.ring.R
import cx.ring.account.AccountCreationViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.snackbar.Snackbar
import cx.ring.account.HomeAccountCreationFragment
import cx.ring.account.JamiImportBackupFragment
import cx.ring.utils.AndroidFileUtils.getCacheFile
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.account.HomeAccountCreationPresenter
import net.jami.account.HomeAccountCreationView
import net.jami.model.AccountCreationModel
import java.io.File

@AndroidEntryPoint
class TVHomeAccountCreationFragment : JamiGuidedStepFragment<HomeAccountCreationPresenter, HomeAccountCreationView>(),
    HomeAccountCreationView {
    private val model: AccountCreationViewModel by activityViewModels()
    private val mCompositeDisposable = CompositeDisposable()

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                finish()
            }
        }

    private val selectFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        Log.w(TAG, "Selected file: $uri")
        if (uri == null) {
            return@registerForActivityResult
        }
        getCacheFile(requireContext(), uri)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ file: File ->
                model.model.archive = file
                Log.w(TAG, "Loaded file: $file")
                presenter.clickOnBackupAccountLink()
            }) { e: Throwable ->
                Log.e(HomeAccountCreationFragment.Companion.TAG, "Error importing archive", e)
                view?.let { view ->
                    Snackbar.make(
                        view,
                        getString(R.string.import_archive_error),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }.let { mCompositeDisposable.add(it) }
    }

    private fun finish(){
        activity?.finish()
    }

    override fun goToAccountCreation() {
        add(parentFragmentManager, TVJamiAccountCreationFragment())
    }

    override fun goToAccountLink() {
        startForResult.launch(Intent(requireContext(), TVImportWizard::class.java))
    }

    override fun goToAccountConnect() {
        add(parentFragmentManager, TVJamiAccountConnectFragment())
    }

    override fun goToBackupAccountLink() {
        Log.w(TAG, "goToBackupAccountLink")
        add(parentFragmentManager, TVJamiLinkAccountFragment())
    }

    override fun goToSIPAccountCreation() {
        //TODO
    }

    override fun onProvideTheme(): Int = R.style.Theme_Ring_Leanback_GuidedStep_First

    override fun onCreateGuidance(savedInstanceState: Bundle?): Guidance {
        val title = getString(R.string.account_creation_home)
        val breadcrumb = ""
        val description = getString(R.string.help_ring)
        val icon = requireContext().getDrawable(R.drawable.ic_jami)
        return Guidance(title, description, breadcrumb, icon)
    }

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        val context = requireContext()
        addAction(context, actions, LINK_ACCOUNT, getString(R.string.account_link_device), "", true)
        addAction(context, actions, LINK_BACKUP_ACCOUNT, getString(R.string.account_link_archive_button), "", true)

        addAction(context, actions, CREATE_ACCOUNT, getString(R.string.account_create_title), "", true)
        addAction(
            context, actions, CREATE_JAMS_ACCOUNT,
            getString(R.string.account_connect_server_button), "", true
        )
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        when (action.id) {
            LINK_ACCOUNT -> presenter.clickOnLinkAccount()
            LINK_BACKUP_ACCOUNT -> selectFile.launch("*/*")
            CREATE_ACCOUNT -> presenter.clickOnCreateAccount()
            CREATE_JAMS_ACCOUNT -> presenter.clickOnConnectAccount()
            else -> requireActivity().finish()
        }
    }

    companion object {
        private const val TAG = "TVHomeAccountCreationFragment"

        private const val LINK_ACCOUNT = 0L
        private const val LINK_BACKUP_ACCOUNT = 1L
        private const val CREATE_ACCOUNT = 2L
        private const val CREATE_JAMS_ACCOUNT = 3L

        private const val REQUEST_CODE_IMPORT = 56
    }
}