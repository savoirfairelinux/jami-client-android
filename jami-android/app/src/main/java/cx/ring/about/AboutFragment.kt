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
package cx.ring.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import cx.ring.BuildConfig
import cx.ring.R
import cx.ring.databinding.FragAboutBinding

class AboutFragment : Fragment() {
    private var binding: FragAboutBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
         FragAboutBinding.inflate(inflater, container, false).apply {
             release.text = getString(R.string.app_release, BuildConfig.VERSION_NAME)
             logo.setOnClickListener { visitWebpage(getString(R.string.app_website)) }
             sflLogo.setOnClickListener { visitWebpage(getString(R.string.savoirfairelinux_website)) }
             contributeContainer.setOnClickListener { visitWebpage(getString(R.string.ring_contribute_website)) }
             licenseContainer.setOnClickListener { visitWebpage(getString(R.string.gnu_license_website)) }
             submitIssueContainer
                 .setOnClickListener { visitWebpage(getString(R.string.submit_issue_link)) }
             credits.setOnClickListener { creditsClicked() }
             toolbar.setNavigationOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
             binding = this
        }.root

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