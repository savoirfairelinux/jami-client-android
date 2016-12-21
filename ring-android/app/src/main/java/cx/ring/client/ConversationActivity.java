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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.fragments.ConversationFragment;
import cx.ring.service.LocalService;

public class ConversationActivity extends AppCompatActivity {

    @BindView(R.id.main_toolbar)
    Toolbar mToolbar;

    private static final String TAG = ConversationActivity.class.getSimpleName();
    static final long REFRESH_INTERVAL_MS = 30 * 1000;

    private boolean mBound = false;
    private LocalService mService = null;
    private final Handler mRefreshTaskHandler = new Handler();
    private ConversationFragment mConversationFragment;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mConversationFragment.refreshView(0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (mConversationFragment == null) {
            mConversationFragment = new ConversationFragment();
            getFragmentManager().beginTransaction()
                    .replace(R.id.main_frame, mConversationFragment, null)
                    .commit();
        }

        if (!mBound) {
            Log.d(TAG, "onCreate: Binding service...");
            Intent intent = new Intent(this, LocalService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            mService = null;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.d(TAG, "ConversationActivity onServiceConnected " + className.getClassName());
            mService = ((LocalService.LocalBinder) binder).getService();

            IntentFilter filter = new IntentFilter(LocalService.ACTION_CONF_UPDATE);
            registerReceiver(receiver, filter);

            if (mConversationFragment != null) {
                mConversationFragment.setCallback(mService);
                mConversationFragment.refreshView(0);
            }

            mBound = true;

            mRefreshTaskHandler.postDelayed(refreshTask, REFRESH_INTERVAL_MS);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d(TAG, "ConversationActivity onServiceDisconnected " + arg0.getClassName());
            mBound = false;
            mRefreshTaskHandler.removeCallbacks(refreshTask);
        }
    };

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

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive " + intent.getAction() + " " + intent.getDataString());
            mConversationFragment.refreshView(intent.getLongExtra(LocalService.ACTION_CONF_UPDATE_EXTRA_MSG, 0));
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ConversationFragment.REQ_ADD_CONTACT:
                if (mService != null) {
                    mService.refreshConversations();
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unregisterReceiver(receiver);
            unbindService(mConnection);
            mBound = false;
        }
    }
}
