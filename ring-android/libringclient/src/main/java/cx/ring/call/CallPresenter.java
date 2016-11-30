/*
 *  Copyright (C) 2016 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
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
package cx.ring.call;

import java.util.Observable;
import java.util.Observer;

import javax.inject.Inject;

import cx.ring.model.DaemonEvent;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.CallService;
import cx.ring.utils.Log;

public class CallPresenter extends RootPresenter<CallView> implements Observer {

    static final String TAG = CallPresenter.class.getSimpleName();

    @Inject
    CallService mCallService;

    @Override
    public void afterInjection() {
        mCallService.addObserver(this);
    }

    public void acceptCall(String callId) {
        mCallService.accept(callId);
    }

    public void hangupCall(String callId) {
        mCallService.hangUp(callId);
    }

    @Override
    public void update(Observable observable, Object object) {
        if (!(object instanceof DaemonEvent)) {
            return;
        }

        DaemonEvent event = (DaemonEvent) object;
        switch (event.getEventType()) {
            case CALL_STATE_CHANGED:
                break;
            case INCOMING_CALL:
                break;
            case INCOMING_MESSAGE:

                break;
            case CONFERENCE_CREATED:
                break;
            case CONFERENCE_CHANGED:
                break;
            case CONFERENCE_REMOVED:
                break;
            case RECORD_PLAYBACK_FILEPATH:
                break;
            case RTCP_REPORT_RECEIVED:
                break;
            default:
                Log.i(TAG, "Unkown daemon event");
                break;
        }
    }

}
