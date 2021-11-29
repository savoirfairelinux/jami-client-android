/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
package cx.ring.tv.contact;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import cx.ring.R;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TVContactActivity extends FragmentActivity {
    private static final String TAG = TVContactActivity.class.getSimpleName();

    public static final String SHARED_ELEMENT_NAME = "photo";
    public static final String TYPE_CONTACT_REQUEST_INCOMING = "incoming";
    public static final String TYPE_CONTACT_REQUEST_OUTGOING = "outgoing";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tv_frag_contact);
    }

}
