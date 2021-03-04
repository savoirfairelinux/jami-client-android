/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
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
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import cx.ring.BuildConfig;
import cx.ring.R;
import cx.ring.client.HomeActivity;
import cx.ring.databinding.FragAboutBinding;
import cx.ring.mvp.BaseSupportFragment;
import net.jami.mvp.RootPresenter;

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
        binding.logo.setOnClickListener(v ->  openWebsite(getString(R.string.app_website)));
        binding.sflLogo.setOnClickListener(v -> openWebsite(getString(R.string.savoirfairelinux_website)));
        binding.contributeContainer.setOnClickListener(v -> openWebsite(getString(R.string.ring_contribute_website)));
        binding.licenseContainer.setOnClickListener(v -> openWebsite(getString(R.string.gnu_license_website)));
        binding.emailReportContainer.setOnClickListener(v -> sendFeedbackEmail());
        binding.credits.setOnClickListener(v -> creditsClicked());
    }

    @Override
    public void onResume() {
        super.onResume();
        ((HomeActivity) requireActivity()).setToolbarTitle(R.string.menu_item_about);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
    }

    private void sendFeedbackEmail() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + "jami@gnu.org"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "[" + getText(R.string.app_name) + " Android - " + BuildConfig.VERSION_NAME + "]");
        launchSystemIntent(emailIntent, R.string.no_email_app_installed);
    }

    private void creditsClicked() {
        BottomSheetDialogFragment dialog = new AboutBottomSheetDialogFragment();
        dialog.show(getChildFragmentManager(), dialog.getTag());
    }

    private void openWebsite(String url) {
        launchSystemIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(url)), R.string.no_browser_app_installed);
    }

    private void launchSystemIntent(Intent intentToLaunch, @StringRes int missingRes) {
        try  {
            startActivity(intentToLaunch);
        } catch (Exception e) {
            View rootView = getView();
            if (rootView != null) {
                Snackbar.make(rootView, getText(missingRes), Snackbar.LENGTH_SHORT).show();
            }
        }
    }
}
