/*
 *  Copyright (C) 2015-2016 Savoir-faire Linux Inc.
 *
 *  Authors: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *           Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package cx.ring.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.OnClick;
import cx.ring.BuildConfig;
import cx.ring.R;
import cx.ring.client.HomeActivity;

public class AboutFragment extends Fragment {

    @Override
    public void onResume() {
        super.onResume();
        ((HomeActivity) getActivity()).setToolbarState(false, R.string.menu_item_about);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View inflatedView = inflater.inflate(R.layout.frag_about, parent, false);
        ButterKnife.inject(this, inflatedView);

        TextView release = (TextView) inflatedView.findViewById(R.id.app_release);
        release.setText(getString(R.string.app_release, BuildConfig.VERSION_NAME));

        TextView licence = (TextView) inflatedView.findViewById(R.id.licence);
        licence.setMovementMethod(LinkMovementMethod.getInstance());

        return inflatedView;
    }

    @OnClick(R.id.email_report_container)
    @SuppressWarnings("unused")
    public void sendFeedbackEmail(){
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + "mobile@lists.savoirfairelinux.net"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "[Ring Android - " + BuildConfig.VERSION_NAME + "]");

        // Check if an app can handle this intent
        boolean isResolvable = getActivity().getPackageManager().queryIntentActivities(emailIntent,
                PackageManager.MATCH_DEFAULT_ONLY).size() > 0;

        if (isResolvable)
            startActivity(Intent.createChooser(emailIntent, getString(R.string.email_chooser_title)));
        else
            Snackbar.make(getView(), R.string.no_email_app_installed, Snackbar.LENGTH_SHORT).show();    }

}
