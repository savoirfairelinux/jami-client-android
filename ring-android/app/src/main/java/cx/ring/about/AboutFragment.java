/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import cx.ring.BuildConfig;
import cx.ring.R;
import cx.ring.client.HomeActivity;
import cx.ring.databinding.FragAboutBinding;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.mvp.RootPresenter;
import cx.ring.utils.DeviceUtils;

public class AboutFragment extends BaseSupportFragment<RootPresenter> {

    private FragAboutBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragAboutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        binding.release.setText(getString(R.string.app_release, BuildConfig.VERSION_NAME));
        binding.logo.setOnClickListener(v ->  startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_website)))));
        binding.sflLogo.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.savoirfairelinux_website)))));
        binding.contributeContainer.setOnClickListener(this::webSiteToView);
        binding.licenseContainer.setOnClickListener(this::webSiteToView);
        binding.emailReportContainer.setOnClickListener(v -> sendFeedbackEmail());
        binding.credits.setOnClickListener(v -> creditsClicked());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DeviceUtils.isTablet(getContext())) {
            Toolbar toolbar = getActivity().findViewById(R.id.main_toolbar);
            TextView title = toolbar.findViewById(R.id.contact_title);
            ImageView logo = toolbar.findViewById(R.id.contact_image);

            logo.setVisibility(View.GONE);
            title.setText(R.string.menu_item_about);
            title.setTextSize(19);
            title.setTypeface(null, Typeface.BOLD);

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) title.getLayoutParams();
            params.removeRule(RelativeLayout.ALIGN_TOP);
            params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
            title.setLayoutParams(params);
        } else {
            ((HomeActivity) getActivity()).setToolbarState(R.string.menu_item_about);
        }

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        Fragment existingFragment = fragmentManager.findFragmentByTag(TAG);
        if (existingFragment == null) {
            ((HomeActivity) getActivity()).goToAbout();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
    }

    private void webSiteToView(View view) {
        Uri uriToView;
        switch (view.getId()) {
            case R.id.contribute_container:
                uriToView = Uri.parse(getString(R.string.ring_contribute_website));
                break;
            case R.id.license_container:
                uriToView = Uri.parse(getString(R.string.gnu_license_website));
                break;
            default:
                return;
        }

        Intent webIntent = new Intent(Intent.ACTION_VIEW);
        webIntent.setData(uriToView);
        launchSystemIntent(webIntent, getString(R.string.website_chooser_title), getString(R.string.no_browser_app_installed));
    }

    private void sendFeedbackEmail() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + "jami@gnu.org"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "[" + getText(R.string.app_name) + " Android - " + BuildConfig.VERSION_NAME + "]");
        launchSystemIntent(emailIntent, getString(R.string.email_chooser_title), getString(R.string.no_email_app_installed));
    }

    private void creditsClicked() {
        BottomSheetDialogFragment dialog = new AboutBottomSheetDialogFragment();
        dialog.show(getActivity().getSupportFragmentManager(), dialog.getTag());
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
}
