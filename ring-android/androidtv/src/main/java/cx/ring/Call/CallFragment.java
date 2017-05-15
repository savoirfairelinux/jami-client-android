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

package cx.ring.Call;

import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.call.CallPresenter;
import cx.ring.model.SipCall;
import cx.ring.model.Uri;
import cx.ring.mvp.BaseFragment;

public class CallFragment extends BaseFragment<CallPresenter> implements CallView {

    public static final String TAG = CallFragment.class.getSimpleName();

    public static final String ACTION_PLACE_CALL = "PLACE_CALL";
    public static final String ACTION_GET_CALL = "GET_CALL";

    public static final String KEY_ACTION = "action";
    public static final String KEY_ACCOUNT_ID = "accountId";
    public static final String KEY_CONF_ID = "confId";
    public static final String KEY_NUMBER = "number";
    public static final String KEY_HAS_VIDEO = "hasVideo";

    @Inject
    protected CallPresenter callPresenter;

    @BindView(R.id.video_preview_surface)
    protected SurfaceView mVideoSurface = null;

    @BindView(R.id.camera_preview_surface)
    protected SurfaceView mVideoPreview = null;

    @BindView(R.id.call_status_txt)
    protected TextView mCallStatusTxt;

    @Override
    protected CallPresenter createPresenter() {
        return callPresenter;
    }

    public static CallFragment newInstance(@NonNull String action, @Nullable String accountID, @Nullable Uri number, boolean hasVideo) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_ACTION, action);
        bundle.putString(KEY_ACCOUNT_ID, accountID);
        bundle.putSerializable(KEY_NUMBER, number);
        bundle.putBoolean(KEY_HAS_VIDEO, hasVideo);
        CallFragment countDownFragment = new CallFragment();
        countDownFragment.setArguments(bundle);
        return countDownFragment;
    }

    @Override
    protected void initPresenter(CallPresenter presenter) {
        super.initPresenter(presenter);

        String action = getArguments().getString(KEY_ACTION);
        if (action.equals(ACTION_PLACE_CALL)) {
            callPresenter.initOutGoing(getArguments().getString(KEY_ACCOUNT_ID),
                    (Uri) getArguments().getSerializable(KEY_NUMBER),
                    getArguments().getBoolean(KEY_HAS_VIDEO));
        } else if (action.equals(ACTION_GET_CALL)) {
            callPresenter.initIncoming(getArguments().getString(KEY_CONF_ID));
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_call, container, false);

        ButterKnife.bind(this, inflatedView);

        // dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        mVideoSurface.getHolder().setFormat(PixelFormat.RGBA_8888);
        mVideoSurface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                presenter.videoSurfaceCreated(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                presenter.videoSurfaceDestroyed();
            }
        });

        mVideoPreview.setZOrderMediaOverlay(true);

        return inflatedView;
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
    public void updateCallStatus(final int callState) {
        getActivity().runOnUiThread(new Runnable() {
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

    @Override
    public void displayVideoSurface(final boolean display) {
        getActivity().runOnUiThread(new Runnable() {
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
    public void finish() {

    }
}
