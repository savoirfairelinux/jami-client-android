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
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import cx.ring.R
import cx.ring.databinding.ActivityLinkDeviceExportSideBinding
import cx.ring.linkdevice.viewmodel.AddDeviceExportState
import dagger.hilt.android.AndroidEntryPoint
import cx.ring.linkdevice.viewmodel.ExportSideViewModel
import kotlinx.coroutines.launch


@AndroidEntryPoint
class LinkDeviceExportSideActivity : AppCompatActivity(),
    ExportSideStep1Fragment.OnInputCallback,
    ExportSideStep2Fragment.OnReviewCallback,
    ExportSideStep3Fragment.OnResultCallback {

    private val exportSideViewModel by lazy { ViewModelProvider(this)[ExportSideViewModel::class.java] }
    private lateinit var binding: ActivityLinkDeviceExportSideBinding
    private var exitDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLinkDeviceExportSideBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupViewPager()
        setupToolbar()

        lifecycleScope.launch {
            exportSideViewModel.uiState.collect {
                Log.d(TAG, "UI state: $it")
                when (it) {
                    is AddDeviceExportState.Init -> {
                        binding.viewPager.apply {
                            currentItem = 0
                            if (it.error != null) {
                                post { // Post is used to let the fragment inflate its views.
                                    (adapter as ViewPagerAdapter).exportSideStep1.showError(it.error)
                                }
                            }
                        }
                    }
                    is AddDeviceExportState.TokenAvailable -> throw UnsupportedOperationException()
                    is AddDeviceExportState.Connecting -> {}
                    is AddDeviceExportState.Authenticating -> {
                        binding.viewPager.apply {
                            currentItem = 1
                            post { // Post is used to let the fragment inflate its views.
                                if (!it.peerAddress.isNullOrEmpty())
                                    (adapter as ViewPagerAdapter).exportSideStep2.showIP(it.peerAddress)
                                else
                                    (adapter as ViewPagerAdapter).exportSideStep2.showPasswordProtection()
                            }
                        }
                    }

                    is AddDeviceExportState.InProgress -> {
                        binding.viewPager.apply {
                            currentItem = 2
                            post { // Post is used to let the fragment inflate its views.
                                (adapter as ViewPagerAdapter).exportSideStep3.showLoading()
                            }
                        }
                    }

                    is AddDeviceExportState.Done -> {
                        binding.viewPager.apply {
                            currentItem = 2
                            post { // Post is used to let the fragment inflate its views.
                                if (it.error != null) {
                                    (adapter as ViewPagerAdapter).exportSideStep3.showError(it.error)
                                } else {
                                    (adapter as ViewPagerAdapter).exportSideStep3.showDone()
                                }
                            }
                        }
                    }
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.i(TAG, "Back button pressed.")
                launchExitAction()
            }
        })
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
        // Dynamic access since fragments can be recreated on configuration change (ex: rotation).
        val exportSideStep1
            get() = supportFragmentManager.findFragmentByTag("f0") as ExportSideStep1Fragment
        val exportSideStep2
            get() = supportFragmentManager.findFragmentByTag("f1") as ExportSideStep2Fragment
        val exportSideStep3
            get() = supportFragmentManager.findFragmentByTag("f2") as ExportSideStep3Fragment

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ExportSideStep1Fragment()
                1 -> ExportSideStep2Fragment()
                2 -> ExportSideStep3Fragment()
                else -> throw IllegalStateException()
            }
        }
    }

    override fun onAuthenticationUri(authenticationUri: String) =
        exportSideViewModel.onAuthenticationUri(authenticationUri)

    override fun onIdentityConfirmation(confirm: Boolean) {
        if (!confirm) finish(1)
        else exportSideViewModel.onIdentityConfirmation()
    }

    override fun onExit(returnCode: Int) {
        finish(returnCode)
    }

    private fun finish(returnCode: Int = 0) {
        if(returnCode == 0) {
            Log.i(TAG, "Account exportation successful.")
        } else {
            Log.w(TAG, "Account exportation failed.")
            exportSideViewModel.onCancel()
        }
        setResult(if (returnCode == 0) Activity.RESULT_OK else Activity.RESULT_CANCELED)
        finish()
    }

    private fun launchExitAction() {
        val state = exportSideViewModel.uiState.value

        if (state is AddDeviceExportState.Init) {
            finish(1)
            return
        } else if (state is AddDeviceExportState.Done) {
            finish(0)
            return
        }

        val message = when (state) {
            is AddDeviceExportState.Connecting, is AddDeviceExportState.Authenticating ->
                resources.getString(R.string.link_device_dialog_exit_export_body_1)

            AddDeviceExportState.InProgress ->
                resources.getString(R.string.link_device_dialog_exit_export_body_2)

            else -> throw UnsupportedOperationException()
        }

        exitDialog = MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.link_device_dialog_exit_title))
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ -> finish(1) }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .show()
    }

    companion object {
        private val TAG = LinkDeviceExportSideActivity::class.java.simpleName
    }
}
