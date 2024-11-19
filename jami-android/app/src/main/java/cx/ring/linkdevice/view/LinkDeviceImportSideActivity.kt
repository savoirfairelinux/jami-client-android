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
import net.jami.linkdevice.presenter.ImportSidePresenter
import net.jami.linkdevice.presenter.LinkDeviceState
import net.jami.linkdevice.view.ImportSideResult
import net.jami.linkdevice.view.ImportSideView

class LinkDeviceImportSideActivity : AppCompatActivity(), ImportSideView,
    ImportSideStep1Fragment.OnOutputCallback,
    ImportSideStep2Fragment.OnAuthenticationCallback,
    ImportSideStep3Fragment.OnResultCallback {

    private lateinit var binding: ActivityLinkDeviceImportSideBinding
    private var _presenter: ImportSidePresenter? = null
    private val presenter get() = _presenter!!

    private var exitDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLinkDeviceImportSideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        _presenter = ImportSidePresenter(this)

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
        binding.viewPager.currentItem = 0
        (binding.viewPager.adapter as ViewPagerAdapter).importSideStep1.apply {
            if (authenticationUri == null) showLoading()
            else showOutput(authenticationUri)
        }
    }

    override fun showActionRequired() {
        binding.viewPager.currentItem = 1
        (binding.viewPager.adapter as ViewPagerAdapter).importSideStep2
            .showActionRequired()
    }

    override fun showAuthentication(
        needPassword: Boolean,
        jamiId: String,
        registeredName: String?,
    ) {
        binding.viewPager.currentItem = 1
        (binding.viewPager.adapter as ViewPagerAdapter).importSideStep2
            .showAuthentication(needPassword, jamiId, registeredName)
    }

    override fun showResult(result: ImportSideResult) {
        binding.viewPager.currentItem = 2
        exitDialog?.dismiss()
        (binding.viewPager.adapter as ViewPagerAdapter).importSideStep3.apply {
            when (result) {
                ImportSideResult.IN_PROGRESS -> showLoading()
                ImportSideResult.SUCCESS -> showDone()
                ImportSideResult.FAILURE -> showError()
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
        if (state == LinkDeviceState.NONE || state == LinkDeviceState.TOKEN_AVAIL) {
            finish(1)
            return
        } else if (state == LinkDeviceState.DONE) {
            finish(0)
            return
        }

        val message = when (state) {
            LinkDeviceState.CONNECTING, LinkDeviceState.AUTHENTICATION -> "Exiting now will cancel the account importation process."
            LinkDeviceState.IMPORTING -> "If not too late, exiting now will cancel the account importation process."
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
