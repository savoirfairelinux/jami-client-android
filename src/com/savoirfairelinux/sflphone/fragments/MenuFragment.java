/*
 *  Copyright (C) 2004-2013 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */
package com.savoirfairelinux.sflphone.fragments;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Profile;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.savoirfairelinux.sflphone.R;
import com.savoirfairelinux.sflphone.adapters.MenuAdapter;
import com.savoirfairelinux.sflphone.client.SFLPhoneHomeActivity;
import com.savoirfairelinux.sflphone.client.SFLPhonePreferenceActivity;

public class MenuFragment extends Fragment {

    private static final String TAG = MenuFragment.class.getSimpleName();
    public static final String ARG_SECTION_NUMBER = "section_number";

    MenuAdapter mAdapter;
    String[] mProjection = new String[] { Profile._ID, Profile.DISPLAY_NAME_PRIMARY, Profile.LOOKUP_KEY, Profile.PHOTO_THUMBNAIL_URI };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new MenuAdapter(getActivity());

        String[] categories = getResources().getStringArray(R.array.menu_categories);
        ArrayAdapter<String> paramAdapter = new ArrayAdapter<String>(getActivity(), R.layout.item_menu, getResources().getStringArray(
                R.array.menu_items_param));
        ArrayAdapter<String> helpAdapter = new ArrayAdapter<String>(getActivity(), R.layout.item_menu, getResources().getStringArray(
                R.array.menu_items_help));

        // Add Sections
        mAdapter.addSection(categories[0], paramAdapter);
        mAdapter.addSection(categories[1], helpAdapter);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_menu, parent, false);

        ((ListView) inflatedView.findViewById(R.id.listView)).setAdapter(mAdapter);
        ((ListView) inflatedView.findViewById(R.id.listView)).setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
                if (pos == 1 || pos == 2 || pos == 3) {
                    Intent launchPreferencesIntent = new Intent().setClass(getActivity(), SFLPhonePreferenceActivity.class);
                    getActivity().startActivityForResult(launchPreferencesIntent, SFLPhoneHomeActivity.REQUEST_CODE_PREFERENCES);
                }
            }
        });

        Cursor mProfileCursor = getActivity().getContentResolver().query(Profile.CONTENT_URI, mProjection, null, null, null);

        if (mProfileCursor.getCount() > 0) {
            mProfileCursor.moveToFirst();
            String photo_uri = mProfileCursor.getString(mProfileCursor.getColumnIndex(Profile.PHOTO_THUMBNAIL_URI));
            if (photo_uri != null) {
                ((ImageView) inflatedView.findViewById(R.id.user_photo)).setImageURI(Uri.parse(photo_uri));
            }
            ((TextView) inflatedView.findViewById(R.id.user_name)).setText(mProfileCursor.getString(mProfileCursor
                    .getColumnIndex(Profile.DISPLAY_NAME_PRIMARY)));
            mProfileCursor.close();
        }
        return inflatedView;
    }

    @Override
    public void onStart() {
        super.onStart();

    }

}
