/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
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

package cx.ring.client;

import cx.ring.R;
import cx.ring.fragments.DetailsHistoryEntryFragment;
import cx.ring.fragments.HistoryFragment;
import cx.ring.model.Conference;
import cx.ring.model.SipCall;
import cx.ring.service.DRingService;
import cx.ring.service.IDRingService;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;

public class DetailHistoryActivity extends Activity implements DetailsHistoryEntryFragment.Callbacks {

    private boolean mBound = false;
    private IDRingService service;
    private String TAG = DetailHistoryActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_holder);

        Intent intent = new Intent(this, DRingService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        default:
            return true;
        }
    }

    @Override
    public IDRingService getService() {
        return service;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = IDRingService.Stub.asInterface(binder);

            FragmentTransaction ft = getFragmentManager().beginTransaction();

            Fragment fr = new DetailsHistoryEntryFragment();
            fr.setArguments(getIntent().getBundleExtra(HistoryFragment.ARGS));
            ft.replace(R.id.frag_container, fr);

            ft.commit();

            mBound = true;
            Log.d(TAG, "Service connected service=" + service);

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            mBound = false;
            Log.d(TAG, "Service disconnected service=" + service);
        }
    };

    @Override
    public void onCall(SipCall call) {
        Bundle bundle = new Bundle();
        Conference tmp = new Conference(Conference.DEFAULT_ID);

        tmp.getParticipants().add(call);

        bundle.putParcelable("conference", tmp);
        Intent intent = new Intent().setClass(this, CallActivity.class);
        intent.putExtra("resuming", false);
        intent.putExtras(bundle);
        startActivity(intent);
    }
}