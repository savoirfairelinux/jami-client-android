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
package cx.ring.about;

import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import cx.ring.BuildConfig;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.client.HomeActivity;

public class AboutFragment extends Fragment implements AboutView {

    @Inject
    public AboutPresenter mAboutPresenter;

    @BindView(R.id.logo_ring_beta2)
    ImageView mImageViewLogo;

    @BindView(R.id.release)
    TextView mTextViewRelease;

    @BindView(R.id.web_site)
    TextView mTextViewWebSite;

    @BindView(R.id.copyright)
    TextView mTextViewCopyright;

    @BindView(R.id.license)
    TextView mTextViewLicense;

    @BindView(R.id.email_report)
    TextView mTextViewEmailReport;

    @BindView(R.id.developped_by)
    TextView mTextViewDeveloppedBy;

    @BindView(R.id.logo)
    ImageView mImageViewSFLLogo;

    private Unbinder mUnbinder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        final View inflatedView = inflater.inflate(R.layout.frag_about, parent, false);

        // views injection
        mUnbinder = ButterKnife.bind(this, inflatedView);

        // dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        return inflatedView;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((HomeActivity) getActivity()).setToolbarState(false, R.string.menu_item_about);

        // view binding
        mAboutPresenter.bindView(this);

        // fonctional stuff
        mAboutPresenter.loadAbout();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Butterknife unbinding
        mUnbinder.unbind();

        // view unbinding
        mAboutPresenter.unbindView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
    }

    @OnClick({R.id.contribute_container, R.id.license_container})
    public void webSiteToView(View view) {

        Uri uriToView = null;

        switch (view.getId()) {
            case R.id.contribute_container:
                uriToView = Uri.parse(getString(R.string.ring_contribute_website));
                break;
            case R.id.license_container:
                uriToView = Uri.parse(getString(R.string.gnu_license_website));
                break;
        }

        if (uriToView == null) {
            return;
        }

        Intent webIntent = new Intent(Intent.ACTION_VIEW);
        webIntent.setData(uriToView);
        launchSystemIntent(webIntent, getString(R.string.website_chooser_title), getString(R.string.no_browser_app_installed));
    }

    @OnClick(R.id.email_report_container)
    public void sendFeedbackEmail() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + "mobile@lists.savoirfairelinux.net"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "[Ring Android - " + BuildConfig.VERSION_NAME + "]");
        launchSystemIntent(emailIntent, getString(R.string.email_chooser_title), getString(R.string.no_email_app_installed));
    }

    private void launchSystemIntent(Intent intentToLaunch,
                                    String intentChooserTitle,
                                    String intentMissingTitle) {
        // Check if an app can handle this intent
        boolean isResolvable = getActivity().getPackageManager().queryIntentActivities(intentToLaunch,
                PackageManager.MATCH_DEFAULT_ONLY).size() > 0;

        if (isResolvable) {
            startActivity(Intent.createChooser(intentToLaunch, intentChooserTitle));
        } else {
            View rootView = getView();
            if (rootView != null) {
                Snackbar.make(rootView, intentMissingTitle, Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    //region View Methods Implementation
    @Override
    public void showRingLogo(byte[] image) {
        mImageViewLogo.setImageResource(R.drawable.logo_ring_beta2);
    }

    @Override
    public void showSavoirFaireLinuxLogo(byte[] image) {
        mImageViewSFLLogo.setImageResource(R.drawable.logo_sfl_coul_rgb);
    }

    @Override
    public void showRelease(String release) {
        mTextViewRelease.setText(getString(R.string.app_release, BuildConfig.VERSION_NAME));
    }

    @Override
    public void showContribute(String contribute) {
        mTextViewWebSite.setText(Html.fromHtml(getString(R.string.app_website_contribute)));
    }

    @Override
    public void showCopyright(String copyright) {
        mTextViewCopyright.setText(getString(R.string.copyright));
    }

    @Override
    public void showLicense(String license) {
        mTextViewLicense.setText(Html.fromHtml(getString(R.string.license)));
    }

    @Override
    public void showFeedback(String feedback) {
        mTextViewEmailReport.setText(Html.fromHtml(getString(R.string.report)));
    }

    @Override
    public void showSupport(String support) {
        mTextViewDeveloppedBy.setText(getString(R.string.sponsor_section));
    }
    //endregion
}
