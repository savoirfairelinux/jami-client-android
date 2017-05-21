/*
 *  Copyright (C) 2016 Savoir-faire Linux Inc.
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

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.view.*
import cx.ring.BuildConfig
import cx.ring.R
import cx.ring.application.RingApplication
import cx.ring.client.HomeActivity
import cx.ring.mvp.BaseFragment
import kotlinx.android.synthetic.main.frag_about.*

class AboutFragment : BaseFragment<AboutPresenter>(), AboutView {

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        val inflatedView = inflater.inflate(R.layout.frag_about, parent, false)
        // dependency injection
        (activity.application as RingApplication).ringInjectionComponent.inject(this)
        return inflatedView
    }

    override fun onResume() {
        super.onResume()
        (activity as HomeActivity).setToolbarState(false, R.string.menu_item_about)

        // fonctional stuff
        presenter.loadAbout()

    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        contribute_container.setOnClickListener { webSiteToView(it) }
        license_container.setOnClickListener { webSiteToView(it) }
        email_report_container.setOnClickListener { sendFeedbackEmail() }
        credits.setOnClickListener { creditsClicked() }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }

    fun webSiteToView(view: View) {
        var uriToView: Uri? = null

        when (view.id) {
            R.id.contribute_container -> uriToView = Uri.parse(getString(R.string.ring_contribute_website))
            R.id.license_container -> uriToView = Uri.parse(getString(R.string.gnu_license_website))
        }

        if (uriToView != null) {
            val webIntent = Intent(Intent.ACTION_VIEW)
            webIntent.data = uriToView
            launchSystemIntent(webIntent, getString(R.string.website_chooser_title), getString(R.string.no_browser_app_installed))
        }
    }

    fun sendFeedbackEmail() {
        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + "mobile@lists.savoirfairelinux.net"))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "[Ring Android - " + BuildConfig.VERSION_NAME + "]")
        launchSystemIntent(emailIntent, getString(R.string.email_chooser_title), getString(R.string.no_email_app_installed))
    }

    fun creditsClicked() {
        val dialog = AboutBottomSheetDialogFragment()
        dialog.show((activity as AppCompatActivity).supportFragmentManager, dialog.tag)
    }

    private fun launchSystemIntent(intentToLaunch: Intent,
                                   intentChooserTitle: String,
                                   intentMissingTitle: String) {
        // Check if an app can handle this intent
        val isResolvable = activity.packageManager.queryIntentActivities(intentToLaunch,
                PackageManager.MATCH_DEFAULT_ONLY).size > 0

        if (isResolvable) {
            startActivity(Intent.createChooser(intentToLaunch, intentChooserTitle))
        } else {
            val rootView = view
            if (rootView != null) {
                Snackbar.make(rootView, intentMissingTitle, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    //region View Methods Implementation
    override fun showRingLogo(image: ByteArray?) {
        logo_ring_beta2.setImageResource(R.drawable.logo_ring_beta2)
    }

    override fun showSavoirFaireLinuxLogo(image: ByteArray?) {
        logo.setImageResource(R.drawable.logo_sfl_coul_rgb)
    }

    override fun showRelease(releaseString: String) {
        release.text = getString(R.string.app_release, BuildConfig.VERSION_NAME)
    }

    override fun showContribute(contribute: String) {
        web_site.text = Html.fromHtml(getString(R.string.app_website_contribute))
    }

    override fun showCopyright(copyrightString: String) {
        copyright.text = getString(R.string.copyright)
    }

    override fun showLicense(licenseString: String) {
        license.text = Html.fromHtml(getString(R.string.license))
    }

    override fun showFeedback(feedback: String) {
        email_report.text = Html.fromHtml(getString(R.string.report))
    }

    override fun showSupport(support: String) {
        developped_by?.text = getString(R.string.sponsor_section)
    }
    //endregion
}
