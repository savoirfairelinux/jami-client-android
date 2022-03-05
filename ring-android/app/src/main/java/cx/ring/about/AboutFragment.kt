/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.about

import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.view.View
import cx.ring.R
import android.view.Menu
import android.view.MenuInflater
import android.content.Intent
import android.net.Uri
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import java.lang.Exception
import com.google.android.material.snackbar.Snackbar
import cx.ring.BuildConfig
import cx.ring.client.HomeActivity
import cx.ring.databinding.FragAboutBinding

class AboutFragment : Fragment() {
    private var binding: FragAboutBinding? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragAboutBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        binding!!.apply {
            release.text = getString(R.string.app_release, BuildConfig.VERSION_NAME)
            logo.setOnClickListener { openWebsite(getString(R.string.app_website)) }
            sflLogo.setOnClickListener { openWebsite(getString(R.string.savoirfairelinux_website)) }
            contributeContainer.setOnClickListener { openWebsite(getString(R.string.ring_contribute_website)) }
            licenseContainer.setOnClickListener { openWebsite(getString(R.string.gnu_license_website)) }
            emailReportContainer.setOnClickListener { sendFeedbackEmail() }
            credits.setOnClickListener { creditsClicked() }
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as HomeActivity).setToolbarTitle(R.string.menu_item_about)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }

    private fun sendFeedbackEmail() {
        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + "jami@gnu.org"))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "[" + getText(R.string.app_name) + " Android - " + BuildConfig.VERSION_NAME + "]")
        launchSystemIntent(emailIntent, R.string.no_email_app_installed)
    }

    private fun creditsClicked() {
        val dialog: BottomSheetDialogFragment = AboutBottomSheetDialogFragment()
        dialog.show(childFragmentManager, dialog.tag)
    }

    private fun openWebsite(url: String) {
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
}