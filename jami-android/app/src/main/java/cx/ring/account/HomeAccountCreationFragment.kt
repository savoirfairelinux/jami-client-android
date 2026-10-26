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
package cx.ring.account

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.children
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import cx.ring.R
import cx.ring.client.HomeActivity
import cx.ring.databinding.FragAccHomeCreateBinding
import cx.ring.databinding.WelcomeChoiceBinding
import cx.ring.databinding.WelcomeChoicesBinding
import cx.ring.fragments.SIPAccountCreationFragment
import cx.ring.mvp.BaseSupportFragment
import cx.ring.utils.AndroidFileUtils.getCacheFile
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.account.HomeAccountCreationPresenter
import net.jami.account.HomeAccountCreationView
import net.jami.model.AccountCreationModel
import net.jami.utils.Log
import java.io.File
import cx.ring.linkdevice.view.LinkDeviceImportSideActivity

@AndroidEntryPoint
class HomeAccountCreationFragment :
    BaseSupportFragment<HomeAccountCreationPresenter, HomeAccountCreationView>(),
    HomeAccountCreationView {
    private var binding: FragAccHomeCreateBinding? = null
    private var restoreSystemBars: (() -> Unit)? = null
    private val model: AccountCreationViewModel by activityViewModels()
    private val mCompositeDisposable = CompositeDisposable()
    private val importBackupLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(StartActivityForResult()) { result ->
            result?.data?.data?.let { uri ->
                getCacheFile(requireContext(), uri)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ file: File ->
                        model.model = AccountCreationModel().apply {
                            archive = file
                        }
                        replaceFragmentWithSlide(
                            fragment = JamiImportBackupFragment(),
                            tag = JamiImportBackupFragment.TAG,
                            containerID = R.id.wizard_container
                        )
                    }) { e: Throwable ->
                        Log.e(TAG, "Error importing archive", e)
                        view?.let { view ->
                            Snackbar.make(
                                view,
                                getString(R.string.import_archive_error),
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }.let { mCompositeDisposable.add(it) }
            }
        }

    private val linkDeviceActivityLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            Log.i(JamiAccountSummaryFragment.TAG, "linkDeviceActivityLauncher: ${result.resultCode}")
            if (result.resultCode == Activity.RESULT_OK) {
                val accountId = result.data // Todo: How to use it ? Need to change currentAccount.
                    ?.getStringExtra(LinkDeviceImportSideActivity.EXTRA_ACCOUNT_ID_KEY)
                startActivity(Intent(requireContext(), HomeActivity::class.java))
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragAccHomeCreateBinding.inflate(inflater, container, false).apply {
            ringCreateBtn.setOnClickListener { presenter.clickOnCreateAccount() }
            existingAccountBtn.setOnClickListener {
                showChoices(R.string.account_already_have,
                    R.string.account_link_from_device to { presenter.clickOnLinkAccount() },
                    R.string.account_link_from_backup to { presenter.clickOnBackupAccountLink() },
                    R.string.account_link_from_organization to { presenter.clickOnConnectAccount() })
            }
            advancedBtn.setOnClickListener {
                showChoices(R.string.account_advanced_show,
                    R.string.account_add_phone_line to { presenter.clickOnCreateSIPAccount() })
            }
            val claims = welcomeClaims.children.filterIsInstance<TextView>().toList()
            claims.forEach { bracket(it) }
            // A halo the colour of the page keeps the words legible over the painting.
            halo(welcomeTitle, 10f)
            (claims + existingAccountBtn + advancedBtn).forEach { halo(it, 6f) }
            ViewCompat.setAccessibilityHeading(welcomeTitle, true)
            welcomePainting.landOn(welcomeTitle)
            binding = this
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        restoreSystemBars = paintSystemBars()
    }

    private fun halo(view: TextView, radiusDp: Float) {
        view.setShadowLayer(radiusDp * resources.displayMetrics.density, 0f, 0f,
            ContextCompat.getColor(view.context, R.color.welcome_background))
    }

    /** Keeps the claim text plain inside its bordered Cubist pill. */
    private fun bracket(claim: TextView) {
        claim.contentDescription = claim.text
    }

    /** Offers the ways in behind a secondary entry, in a bottom sheet. */
    private fun showChoices(@StringRes title: Int, vararg choices: Pair<Int, () -> Unit>) {
        val dialog = BottomSheetDialog(requireContext(), R.style.Welcome_Sheet)
        val inflater = LayoutInflater.from(dialog.context)
        val sheet = WelcomeChoicesBinding.inflate(inflater)
        sheet.welcomeChoicesTitle.setText(title)
        ViewCompat.setAccessibilityHeading(sheet.welcomeChoicesTitle, true)
        for ((label, action) in choices) {
            WelcomeChoiceBinding.inflate(inflater, sheet.welcomeChoicesList, true).root.apply {
                setText(label)
                setOnClickListener {
                    dialog.dismiss()
                    action()
                }
            }
        }
        dialog.setContentView(sheet.root)
        dialog.show()
    }

    /**
     * Lays the welcome colours under the system bars, and gives the previous
     * ones back on leaving. Under the navigation bar the wizard pads its
     * container: the painting goes on there, so the container must not clip it.
     * With three-button navigation, the system would veil that strip in the
     * theme's background colour: the veil is lifted while the welcome shows.
     */
    @Suppress("DEPRECATION")
    private fun paintSystemBars(): () -> Unit {
        val activity = requireActivity()
        val window = activity.window
        val color = ContextCompat.getColor(activity, R.color.welcome_background)
        val luminous = resources.getBoolean(R.bool.welcome_dark)
        val bars = WindowCompat.getInsetsController(window, window.decorView)
        val lightStatus = bars.isAppearanceLightStatusBars
        val lightNavigation = bars.isAppearanceLightNavigationBars
        val container = activity.findViewById<ViewGroup>(R.id.wizard_container)
        val containerBackground = container?.background
        val clipChildren = container?.clipChildren ?: true
        val clipToPadding = container?.clipToPadding ?: true
        val statusColor = window.statusBarColor
        val navigationColor = window.navigationBarColor
        val veiled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && window.isNavigationBarContrastEnforced
        bars.isAppearanceLightStatusBars = !luminous
        bars.isAppearanceLightNavigationBars = !luminous
        container?.setBackgroundColor(color)
        container?.clipChildren = false
        container?.clipToPadding = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.statusBarColor = color
            window.navigationBarColor = color
        }
        return {
            bars.isAppearanceLightStatusBars = lightStatus
            bars.isAppearanceLightNavigationBars = lightNavigation
            container?.background = containerBackground
            container?.clipChildren = clipChildren
            container?.clipToPadding = clipToPadding
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = veiled
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                window.statusBarColor = statusColor
                window.navigationBarColor = navigationColor
            }
        }
    }

    override fun onDestroyView() {
        restoreSystemBars?.invoke()
        restoreSystemBars = null
        super.onDestroyView()
        mCompositeDisposable.clear()
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        mCompositeDisposable.dispose()
    }

    override fun goToAccountCreation() {
        model.model = AccountCreationModel()
        replaceFragmentWithSlide(JamiAccountCreationFragment(), JamiAccountCreationFragment.TAG, R.id.wizard_container)
    }

    override fun goToAccountLink() {
        linkDeviceActivityLauncher
            .launch(Intent(requireContext(), LinkDeviceImportSideActivity::class.java))
    }

    override fun goToAccountConnect() {
        replaceFragmentWithSlide(JamiAccountConnectFragment(), JamiAccountConnectFragment.TAG, R.id.wizard_container)
    }

    override fun goToSIPAccountCreation() {
        replaceFragmentWithSlide(SIPAccountCreationFragment(), SIPAccountCreationFragment.TAG, R.id.wizard_container)
    }

    override fun goToBackupAccountLink() {
        try {
            importBackupLauncher.launch(
                Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("*/*")
            )
        } catch (e: Exception) {
            view?.let { v ->
                Snackbar.make(v, getString(R.string.browser_error), Snackbar.LENGTH_SHORT).show() }
        }
    }

    companion object {
        val TAG = HomeAccountCreationFragment::class.simpleName!!
    }
}