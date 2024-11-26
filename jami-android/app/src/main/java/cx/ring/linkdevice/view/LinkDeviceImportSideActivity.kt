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
import android.content.Intent
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
import cx.ring.databinding.ActivityLinkDeviceImportSideBinding
import cx.ring.linkdevice.viewmodel.AddDeviceImportState
import cx.ring.linkdevice.viewmodel.ImportSideViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LinkDeviceImportSideActivity : AppCompatActivity(),
    ImportSideStep1Fragment.OnOutputCallback,
    ImportSideStep2Fragment.OnAuthenticationCallback,
    ImportSideStep3Fragment.OnResultCallback {

    private val importSideViewModel by lazy { ViewModelProvider(this)[ImportSideViewModel::class.java] }
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

        lifecycleScope.launch {
            importSideViewModel.uiState.collect {
                Log.d(TAG, "UI state: $it")
                when (it) {
                    is AddDeviceImportState.Init -> {}
                    is AddDeviceImportState.TokenAvailable -> {
                        binding.viewPager.apply {
                            currentItem = 0
                            post { // Post is used to let the fragment inflate its views.
                                (adapter as ViewPagerAdapter).importSideStep1.apply {
                                    if (it.token.isEmpty()) showLoading()
                                    else showOutput(it.token)
                                }
                            }
                        }
                    }

                    is AddDeviceImportState.Connecting -> {
                        binding.viewPager.apply {
                            currentItem = 1
                            post { // Post is used to let the fragment inflate its views.
                                (adapter as ViewPagerAdapter).importSideStep2.showActionRequired()
                            }
                        }
                    }

                    is AddDeviceImportState.Authenticating -> {
                        binding.viewPager.apply {
                            currentItem = 1
                            post { // Post is used to let the fragment inflate its views.
                                (adapter as ViewPagerAdapter).importSideStep2
                                    .showAuthentication(
                                        it.needPassword,
                                        it.jamiId,
                                        it.registeredName,
                                        it.error
                                    )
                            }
                        }
                    }

                    is AddDeviceImportState.InProgress -> {
                        exitDialog?.dismiss()
                        binding.viewPager.apply {
                            currentItem = 2
                            post { // Post is used to let the fragment inflate its views.
                                (adapter as ViewPagerAdapter).importSideStep3.showLoading()
                            }
                        }
                    }

                    is AddDeviceImportState.Done -> {
                        exitDialog?.dismiss()
                        binding.viewPager.apply {
                            currentItem = 2
                            post { // Post is used to let the fragment inflate its views.
                                if (it.error != null) {
                                    (adapter as ViewPagerAdapter).importSideStep3.showError(it.error)
                                } else {
                                    (adapter as ViewPagerAdapter).importSideStep3.showDone()
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

    private inner class ViewPagerAdapter(activity: AppCompatActivity) :
        FragmentStateAdapter(activity) {
        // Dynamic access since fragments can be recreated on configuration change (ex: rotation).
        val importSideStep1
            get() = supportFragmentManager.findFragmentByTag("f0") as ImportSideStep1Fragment
        val importSideStep2
            get() = supportFragmentManager.findFragmentByTag("f1") as ImportSideStep2Fragment
        val importSideStep3
            get() = supportFragmentManager.findFragmentByTag("f2") as ImportSideStep3Fragment

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ImportSideStep1Fragment()
                1 -> ImportSideStep2Fragment()
                2 -> ImportSideStep3Fragment()
                else -> throw IllegalStateException()
            }
        }
    }

    override fun onAuthentication(password: String) =
        importSideViewModel.onAuthentication(password)

    override fun onExit(returnCode: Int) {
        finish(returnCode)
    }

    /**
     * Finish the activity with a return code.
     * @param returnCode 0 if success, 1 or other if failure.
     */
    private fun finish(returnCode: Int = 0) {
        if (returnCode == 0) {
            Log.i(TAG, "Account importation successful.")
            val resultIntent = Intent()
                .apply {
                    putExtra(
                        EXTRA_ACCOUNT_ID_KEY,
                        importSideViewModel.tempAccount!!.accountId
                    )
                }
            setResult(Activity.RESULT_OK, resultIntent)
        } else {
            Log.w(TAG, "Account importation failed.")
            importSideViewModel.onCancel()
            setResult(Activity.RESULT_CANCELED)
        }
        this.finish()
    }

    private fun launchExitAction() {
        val state = importSideViewModel.uiState.value

        // If the state is NONE, TOKEN_AVAIL or DONE, exit (correspond to normal scenario).
        if (state is AddDeviceImportState.Init || state is AddDeviceImportState.TokenAvailable) {
            finish(1)
            return
        } else if (state is AddDeviceImportState.Done) {
            finish(0)
            return
        }

        val message = when (state) {
            is AddDeviceImportState.Connecting, is AddDeviceImportState.Authenticating ->
                resources.getString(R.string.link_device_dialog_exit_import_body_1)

            is AddDeviceImportState.InProgress ->
                resources.getString(R.string.link_device_dialog_exit_import_body_2)

            else -> throw UnsupportedOperationException()
        }

        exitDialog = MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.link_device_dialog_exit_title))
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ -> finish(1) }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }.show()
    }

    companion object {
        private val TAG = LinkDeviceImportSideActivity::class.java.simpleName
        const val EXTRA_ACCOUNT_ID_KEY = "account_id"
    }
}
