/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
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
package cx.ring.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import cx.ring.BuildConfig
import cx.ring.R
import cx.ring.about.ui.AboutScreen
import cx.ring.about.ui.AboutUiState
import cx.ring.ui.theme.JamiTheme

class AboutFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                JamiTheme {
                    AboutScreen(
                        uiState = AboutUiState(
                            releaseVersion = getString(R.string.app_release, BuildConfig.VERSION_NAME)
                        ),
                        onBackClick = { activity?.onBackPressedDispatcher?.onBackPressed() },
                        onLogoClick = { visitWebpage(getString(R.string.app_website)) },
                        onCreditsClick = { creditsClicked() },
                        onContributeClick = { visitWebpage(getString(R.string.app_website_contribute)) },
                        onLicenseClick = { visitWebpage(getString(R.string.gnu_license_website)) },
                        onSubmitIssueClick = { visitWebpage(getString(R.string.submit_issue_link)) },
                        onSflLogoClick = { visitWebpage(getString(R.string.savoirfairelinux_website)) }
                    )
                }
            }
        }

    private fun creditsClicked() {
        val dialog: BottomSheetDialogFragment = AboutBottomSheetDialogFragment()
        dialog.show(childFragmentManager, dialog.tag)
    }

    private fun visitWebpage(url: String) {
        launchSystemIntent(Intent(Intent.ACTION_VIEW, Uri.parse(url)), R.string.no_browser_app_installed)
    }

    private fun launchSystemIntent(intentToLaunch: Intent, @StringRes missingRes: Int) {
        try {
            startActivity(intentToLaunch)
        } catch (e: Exception) {
            val rootView = view
            if (rootView != null) {
                Snackbar.make(rootView, getText(missingRes), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        val TAG = AboutFragment::class.simpleName!!
    }
}
