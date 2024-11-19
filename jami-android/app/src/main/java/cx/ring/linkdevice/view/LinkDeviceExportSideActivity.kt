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
import cx.ring.databinding.ActivityLinkDeviceExportSideBinding
import cx.ring.mvp.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import net.jami.linkdevice.presenter.AuthState
import net.jami.linkdevice.presenter.AuthError
import net.jami.linkdevice.presenter.ExportSidePresenter
import net.jami.linkdevice.view.ExportSideInputError
import net.jami.linkdevice.view.ExportSideView

@AndroidEntryPoint
class LinkDeviceExportSideActivity : BaseActivity<ExportSidePresenter>(), ExportSideView,
    ExportSideStep1Fragment.OnInputCallback,
    ExportSideStep2Fragment.OnReviewCallback,
    ExportSideStep3Fragment.OnResultCallback {

    private lateinit var binding: ActivityLinkDeviceExportSideBinding
    private var exitDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLinkDeviceExportSideBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupViewPager()
        setupToolbar()
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = ViewPagerAdapter(this)
        binding.viewPager.isUserInputEnabled = false // Disable swipe

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { _, _ -> }.attach()
        binding.tabLayout.touchables.forEach { it.isEnabled = false }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            Log.i(TAG, "Back button clicked.")
            launchExitAction()
        }
    }

    private inner class ViewPagerAdapter(activity: AppCompatActivity) :
        FragmentStateAdapter(activity) {
        val exportSideStep1 = ExportSideStep1Fragment()
        val exportSideStep2 = ExportSideStep2Fragment()
        val exportSideStep3 = ExportSideStep3Fragment()

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> exportSideStep1
                1 -> exportSideStep2
                2 -> exportSideStep3
                else -> throw IllegalStateException()
            }
        }
    }

    override fun showInput(error: ExportSideInputError?) {
        binding.viewPager.apply {
            currentItem = 0
            if (error != null) {
                post { // Post is used to let the fragment inflate its views.
                    (adapter as ViewPagerAdapter).exportSideStep1.showError(error)
                }
            }
        }
    }

    override fun showIP(ip: String) {
        binding.viewPager.apply {
            currentItem = 1
            post { // Post is used to let the fragment inflate its views.
                (adapter as ViewPagerAdapter).exportSideStep2.showIP(ip)
            }
        }
    }

    override fun showPasswordProtection() {
        binding.viewPager.apply {
            currentItem = 1
            post { // Post is used to let the fragment inflate its views.
                (adapter as ViewPagerAdapter).exportSideStep2.showPasswordProtection()
            }
        }
    }

    override fun showInProgress() {
        exitDialog?.dismiss() // Dismiss since the process might not be cancellable anymore.
        binding.viewPager.apply {
            currentItem = 2
            post { // Post is used to let the fragment inflate its views.
                (adapter as ViewPagerAdapter).exportSideStep3.showLoading()
            }
        }
    }

    override fun showResult(error: AuthError?) {
        exitDialog?.dismiss() // Dismiss since the process is done.
        binding.viewPager.apply {
            currentItem = 2
            post { // Post is used to let the fragment inflate its views.
                if (error != null) {
                    (adapter as ViewPagerAdapter).exportSideStep3.showError(error)
                } else {
                    (adapter as ViewPagerAdapter).exportSideStep3.showDone()
                }
            }
        }
    }

    override fun onAuthenticationUri(authenticationUri: String) =
        presenter.onAuthenticationUri(authenticationUri)

    override fun onIdentityConfirmation(confirm: Boolean) = presenter.onIdentityConfirmation()

    override fun onExit(returnCode: Int) {
        finish(returnCode)
    }

    private fun finish(returnCode: Int = 0) {
        setResult(if (returnCode == 0) Activity.RESULT_OK else Activity.RESULT_CANCELED)
        finish()
    }

    private fun launchExitAction() {
        val state = presenter.currentState

        if (state == AuthState.INIT) {
            finish(1)
            return
        } else if (state == AuthState.DONE) {
            finish(0)
            return
        }

        val message = when (state) {
            AuthState.CONNECTING, AuthState.AUTHENTICATING -> "Exiting now will cancel the account exportation process."
            AuthState.IN_PROGRESS -> "If not too late, exiting now will cancel the account exportation process."
            else -> throw UnsupportedOperationException()
        }

        exitDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Are you sure you want to exit?")
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ -> finish(1) }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .show()
    }

    companion object {
        private val TAG = LinkDeviceExportSideActivity::class.java.simpleName
    }
}
