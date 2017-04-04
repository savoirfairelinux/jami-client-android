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

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.facades.ConversationFacade;
import cx.ring.fragments.ConversationFragment;

public class ConversationActivity extends AppCompatActivity {

    private static final String TAG = ConversationActivity.class.getSimpleName();

    @BindView(R.id.main_toolbar)
    Toolbar mToolbar;

    @Inject
    ConversationFacade mConversationFacade;

    static final long REFRESH_INTERVAL_MS = 30 * 1000;

    private final Handler mRefreshTaskHandler = new Handler();

    private ConversationFragment mConversationFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private final Runnable refreshTask = new Runnable() {
        private long lastRefresh = 0;

        public void run() {
            if (lastRefresh == 0) {
                lastRefresh = SystemClock.uptimeMillis();
            } else {
                lastRefresh += REFRESH_INTERVAL_MS;
            }

            mRefreshTaskHandler.postAtTime(this, lastRefresh + REFRESH_INTERVAL_MS);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ConversationFragment.REQ_ADD_CONTACT:
                mConversationFacade.refreshConversations();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getIntent() == null || getIntent().getData() == null) {
            return;
        }

        if (mConversationFragment == null) {
            Bundle bundle = new Bundle();
            bundle.putString("conversationID", getIntent().getData().getLastPathSegment());
            bundle.putString("number", getIntent().getStringExtra("number"));

            mConversationFragment = new ConversationFragment();
            mConversationFragment.setArguments(bundle);
            getFragmentManager().beginTransaction()
                    .replace(R.id.main_frame, mConversationFragment, null)
                    .commit();
        }

        mRefreshTaskHandler.postDelayed(refreshTask, REFRESH_INTERVAL_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRefreshTaskHandler.removeCallbacks(refreshTask);
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

}
