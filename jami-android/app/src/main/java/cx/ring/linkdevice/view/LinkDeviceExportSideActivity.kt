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
import net.jami.linkdevice.presenter.AuthState
import net.jami.linkdevice.presenter.AuthError
import net.jami.linkdevice.presenter.ExportSidePresenter
import net.jami.linkdevice.view.ExportSideInputError
import net.jami.linkdevice.view.ExportSideResult
import net.jami.linkdevice.view.ExportSideView
import javax.inject.Inject

class LinkDeviceExportSideActivity : AppCompatActivity(), ExportSideView,
    ExportSideStep1Fragment.OnInputCallback,
    ExportSideStep2Fragment.OnReviewCallback,
    ExportSideStep3Fragment.OnResultCallback {

    @Inject
    lateinit var presenter: ExportSidePresenter

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
        binding.viewPager.currentItem = 0
        if (error != null) {
            (binding.viewPager.adapter as ViewPagerAdapter).exportSideStep1.showError(error)
        }
    }

    override fun showIP(ip: String) {
        binding.viewPager.currentItem = 1
        (binding.viewPager.adapter as ViewPagerAdapter).exportSideStep2.showIP(ip)
    }

    override fun showPasswordProtection() {
        binding.viewPager.currentItem = 1
        (binding.viewPager.adapter as ViewPagerAdapter).exportSideStep2.showPasswordProtection()
    }

    override fun showResult(result: ExportSideResult, error: AuthError?) {
        binding.viewPager.currentItem = 2
        exitDialog?.dismiss()
        (binding.viewPager.adapter as ViewPagerAdapter).exportSideStep3.apply {
            when (result) {
                ExportSideResult.IN_PROGRESS -> showLoading()
                ExportSideResult.SUCCESS -> showDone()
                ExportSideResult.FAILURE -> {
                    if (error == null)
                        throw NullPointerException("Result is FAILURE. Error should not be null.")
                    showError(error)
                }
            }
        }
    }

    override fun onAuthenticationUri(authenticationUri: String) =
        presenter.onAuthenticationUri(authenticationUri)

    override fun onIdentityConfirmation(confirm: Boolean) = presenter.onIdentityConfirmation()

    override fun onExit() {
        finish()
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
