/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Michel Schmit <michel.schmit@savoirfairelinux.com>
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

package cx.ring.client;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.Call.CallPresenter;
import cx.ring.Call.CallView;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.model.SipCall;
import cx.ring.model.Uri;
import cx.ring.services.AccountService;
import cx.ring.utils.Log;

public class CallActivity extends Activity implements CallView {


    static final String TAG = CallActivity.class.getSimpleName();

    @Inject
    protected CallPresenter mCallPresenter;

    @BindView(R.id.video_preview_surface)
    protected SurfaceView mVideoSurface = null;

    @BindView(R.id.camera_preview_surface)
    protected SurfaceView mVideoPreview = null;

    @BindView(R.id.call_status_txt)
    protected TextView mCallStatusTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        ButterKnife.bind(this);

        // dependency injection
        ((RingApplication) getApplication()).getRingInjectionComponent().inject(this);

        mCallPresenter.bindView(this);

        mCallPresenter.startDirtyCall();


    }

     /*
    View Method
 */

    @Override
    public void finish() {
//        getActivity().finish();
    }

    @Override
    public void initNormalStateDisplay(boolean hasVideo) {

    }

    @Override
    public void initContactDisplay(SipCall call) {

    }

    @Override
    public void initIncomingCallDisplay() {

    }

    @Override
    public void initOutGoingCallDisplay() {

    }

    @Override
    public void updateCallStatus(final int callState) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (callState) {
                    case SipCall.State.NONE:
                        mCallStatusTxt.setText("");
                        break;
                    default:
                        mCallStatusTxt.setText(callStateToHumanState(callState));
                        break;
                }

            }
        });
    }

    public static int callStateToHumanState(final int state) {
        switch (state) {
            case SipCall.State.INCOMING:
                return R.string.call_human_state_incoming;
            case SipCall.State.CONNECTING:
                return R.string.call_human_state_connecting;
            case SipCall.State.RINGING:
                return R.string.call_human_state_ringing;
            case SipCall.State.CURRENT:
                return R.string.call_human_state_current;
            case SipCall.State.HUNGUP:
                return R.string.call_human_state_hungup;
            case SipCall.State.BUSY:
                return R.string.call_human_state_busy;
            case SipCall.State.FAILURE:
                return R.string.call_human_state_failure;
            case SipCall.State.HOLD:
                return R.string.call_human_state_hold;
            case SipCall.State.UNHOLD:
                return R.string.call_human_state_unhold;
            case SipCall.State.OVER:
                return R.string.call_human_state_over;
            case SipCall.State.NONE:
            default:
                return R.string.call_human_state_none;
        }
    }

    @Override
    public void displayVideoSurface(final boolean display) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoSurface.setVisibility(display ? View.VISIBLE : View.GONE);
                mVideoPreview.setVisibility(display ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void updateTime(long duration) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        mCallPresenter.bindView(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCallPresenter.bindView(this);
    }
}
