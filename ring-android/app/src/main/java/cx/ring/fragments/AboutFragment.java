/*
 *  Copyright (C) 2015-2016 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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

import android.os.Bundle;
import android.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import cx.ring.BuildConfig;
import cx.ring.R;
import cx.ring.client.HomeActivity;

public class AboutFragment extends Fragment {

    @Override
    public void onResume() {
        super.onResume();
        ((HomeActivity)getActivity()).setToolbarState(false, R.string.menu_item_about);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_about, parent, false);

        TextView title = (TextView) inflatedView.findViewById(R.id.app_name);
        title.setText(getString(R.string.app_name) + " for Android ");

        TextView release = (TextView) inflatedView.findViewById(R.id.app_release);
        release.setText(getString(R.string.app_release, BuildConfig.VERSION_NAME));

        TextView licence = (TextView) inflatedView.findViewById(R.id.licence);
        licence.setMovementMethod(LinkMovementMethod.getInstance());

        return inflatedView;
    }

}
