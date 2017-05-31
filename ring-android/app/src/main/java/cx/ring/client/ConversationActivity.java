/*
 *  Copyright (C) 2015-2016 Savoir-faire Linux Inc.
 *
 *  Authors:    Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *              Romain Bertozzi <romain.bertozzi@savoirfairelinux.com>
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

package cx.ring.client;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.fragments.ConversationFragment;

public class ConversationActivity extends AppCompatActivity {

    private static final String TAG = ConversationActivity.class.getSimpleName();

    @BindView(R.id.main_toolbar)
    protected Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ConversationFragment fragment = ConversationFragment.newInstance(getIntent().getStringExtra(ConversationFragment.KEY_ACCOUNT_ID),
                getIntent().getStringExtra(ConversationFragment.KEY_CONTACT_ID),
                getIntent().getLongExtra(ConversationFragment.KEY_CONVERSATION_ID, 0L));
        getFragmentManager().beginTransaction()
                .replace(R.id.main_frame, fragment, null)
                .commit();
    }
}