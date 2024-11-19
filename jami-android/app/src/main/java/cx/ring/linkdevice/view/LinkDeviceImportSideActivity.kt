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
package cx.ring.linkdevice.view

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import cx.ring.databinding.ActivityLinkDeviceImportSideBinding
import cx.ring.mvp.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import net.jami.linkdevice.presenter.AuthState
import net.jami.linkdevice.presenter.AuthError
import net.jami.linkdevice.presenter.ImportSidePresenter
import net.jami.linkdevice.presenter.ImportSidePresenter.InputError
import net.jami.linkdevice.view.ImportSideView

@AndroidEntryPoint
class LinkDeviceImportSideActivity : BaseActivity<ImportSidePresenter>(), ImportSideView,
    ImportSideStep1Fragment.OnOutputCallback,
    ImportSideStep2Fragment.OnAuthenticationCallback,
    ImportSideStep3Fragment.OnResultCallback {

    private lateinit var binding: ActivityLinkDeviceImportSideBinding

    private var exitDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLinkDeviceImportSideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = ViewPagerAdapter(this)
        binding.viewPager.isUserInputEnabled = false // Disable viewPager swipe.

        // TabLayout is used to show the current step. Disable touch events.
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { _, _ -> }.attach()
        binding.tabLayout.touchables.forEach { it.isEnabled = false }

        binding.toolbar.setNavigationOnClickListener {
            Log.i(TAG, "Back button clicked.")
            launchExitAction()
        }
    }

    private inner class ViewPagerAdapter(activity: AppCompatActivity) :
        FragmentStateAdapter(activity) {
        val importSideStep1 = ImportSideStep1Fragment()
        val importSideStep2 = ImportSideStep2Fragment()
        val importSideStep3 = ImportSideStep3Fragment()

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> importSideStep1
                1 -> importSideStep2
                2 -> importSideStep3
                else -> throw IllegalStateException()
            }
        }
    }

    override fun showAuthenticationUri(authenticationUri: String?) {
        binding.viewPager.apply {
            currentItem = 0
            post { // Post is used to let the fragment inflate its views.
                (adapter as ViewPagerAdapter).importSideStep1.apply {
                    if (authenticationUri == null) showLoading()
                    else showOutput(authenticationUri)
                }
            }
        }
    }

    override fun showActionRequired() {
        binding.viewPager.apply {
            currentItem = 1
            post { // Post is used to let the fragment inflate its views.
                (adapter as ViewPagerAdapter).importSideStep2.showActionRequired()
            }
        }
    }

    override fun showAuthentication(
        needPassword: Boolean,
        jamiId: String,
        registeredName: String?,
        error: InputError?
    ) {
        binding.viewPager.apply {
            currentItem = 1
            post { // Post is used to let the fragment inflate its views.
                (adapter as ViewPagerAdapter).importSideStep2
                    .showAuthentication(needPassword, jamiId, registeredName, error)
            }
        }
    }

    override fun showInProgress() {
        exitDialog?.dismiss()
        binding.viewPager.apply {
            currentItem = 2
            post { // Post is used to let the fragment inflate its views.
                (adapter as ViewPagerAdapter).importSideStep3.showLoading()
            }
        }
    }

    override fun showResult(error: AuthError?) {
        exitDialog?.dismiss()
        binding.viewPager.apply {
            currentItem = 2
            post { // Post is used to let the fragment inflate its views.
                if (error != null) {
                    (adapter as ViewPagerAdapter).importSideStep3.showError(error)
                } else {
                    (adapter as ViewPagerAdapter).importSideStep3.showDone()
                }
            }
        }
    }

    override fun onAuthentication(password: String) =
        presenter.onAuthentication(password)

    override fun onExit(returnCode: Int) {
        finish(returnCode)
    }

    /**
     * Finish the activity with a return code.
     * @param returnCode 0 if success, 1 or other if failure.
     */
    private fun finish(returnCode: Int = 0) {
        setResult(if (returnCode == 0) Activity.RESULT_OK else Activity.RESULT_CANCELED)
        this.finish()
    }

    private fun launchExitAction() {
        val state = presenter.currentState

        // If the state is NONE, TOKEN_AVAIL or DONE, exit (correspond to normal scenario).
        if (state == AuthState.INIT || state == AuthState.TOKEN_AVAILABLE) {
            finish(1)
            return
        } else if (state == AuthState.DONE) {
            finish(0)
            return
        }

        val message = when (state) {
            AuthState.CONNECTING, AuthState.AUTHENTICATING -> "Exiting now will cancel the account importation process."
            AuthState.IN_PROGRESS -> "If not too late, exiting now will cancel the account importation process."
            else -> throw UnsupportedOperationException()
        }

        exitDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Are you sure you want to exit ?")
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ -> finish(1) }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }.show()
    }

    companion object {
        private val TAG = LinkDeviceImportSideActivity::class.java.simpleName
    }
}
