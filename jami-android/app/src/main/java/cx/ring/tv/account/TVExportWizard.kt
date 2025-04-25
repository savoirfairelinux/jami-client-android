package cx.ring.tv.account

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import cx.ring.application.JamiApplication
import cx.ring.linkdevice.view.ExportSideStep3Fragment
import cx.ring.linkdevice.viewmodel.AddDeviceExportState
import cx.ring.linkdevice.viewmodel.ExportSideInputError
import cx.ring.linkdevice.viewmodel.ExportSideViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.jami.services.AccountService

@AndroidEntryPoint
class TVExportWizard : AppCompatActivity(),
    TVAccountExportStep1Fragment.OnInputCallback,
    TVAccountExportStep2Fragment.OnReviewCallback,
    ExportSideStep3Fragment.OnResultCallback {

    private val exportSideViewModel by lazy { ViewModelProvider(this)[ExportSideViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        JamiApplication.instance?.startDaemon(this)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, TVAccountExportStep1Fragment())
                .commitNow()
        }

        observeUiState()
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            exportSideViewModel.uiState.collect { state ->
                try {
                    when (state) {
                        is AddDeviceExportState.Init -> showInit(state.error)
                        is AddDeviceExportState.TokenAvailable -> throw UnsupportedOperationException()
                        is AddDeviceExportState.Connecting -> Log.d(TAG, "State: Connecting")
                        is AddDeviceExportState.Authenticating -> showAuthenticating(state.peerAddress)
                        is AddDeviceExportState.InProgress -> showInProgress()
                        is AddDeviceExportState.Done -> showDone(state.error)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error while processing UI state.", e)
                }
            }
        }
    }

    private fun showInit(error: ExportSideInputError?) {
        (supportFragmentManager.findFragmentById(android.R.id.content) as? TVAccountExportStep1Fragment)
            ?.showError(error)
    }

    private fun showAuthenticating(peerAddress: String?) {
        val accountId = intent.getStringExtra(EXTRA_ACCOUNT_ID) ?: ""
        val registeredName = intent.getStringExtra(EXTRA_REGISTERED_NAME) ?: accountId
        val fragment = supportFragmentManager.findFragmentById(android.R.id.content) as? TVAccountExportStep2Fragment

        if (fragment == null) {
            GuidedStepSupportFragment.addAsRoot(
                this,
                TVAccountExportStep2Fragment.build(peerAddress, accountId, registeredName),
                android.R.id.content
            )
        } else {
            fragment.update(peerAddress)
        }
    }

    private fun showInProgress() {
        val fragment = ExportSideStep3Fragment()
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .commitNow()

        fragment.showLoading()
    }

    private fun showDone(error: AccountService.AuthError?) {
        val fragment = ExportSideStep3Fragment()
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .commitNow()

        if (error != null) {
            fragment.showError(error)
        } else {
            fragment.showDone()
        }
    }

    override fun onAuthenticationUri(authenticationUri: String) {
        exportSideViewModel.onAuthenticationUri(authenticationUri)
    }

    override fun onIdentityConfirmation(confirm: Boolean) {
        if (!confirm) {
            finish(1)
        } else {
            exportSideViewModel.onIdentityConfirmation()
        }
    }

    override fun onExit(returnCode: Int) {
        finish(returnCode)
    }

    private fun finish(returnCode: Int = 0) {
        if (returnCode != 0) {
            Log.w(TAG, "Account exportation failed.")
            exportSideViewModel.onCancel()
        }
        setResult(if (returnCode == 0) Activity.RESULT_OK else Activity.RESULT_CANCELED)
        finish()
    }


    companion object {
        private const val TAG = "TVExportWizard"
        const val EXTRA_ACCOUNT_ID = "account_id"
        const val EXTRA_REGISTERED_NAME = "registered_name"
    }
}
