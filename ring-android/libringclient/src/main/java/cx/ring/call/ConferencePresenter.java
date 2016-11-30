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

import cx.ring.model.Account;
import cx.ring.model.DaemonEvent;
import cx.ring.model.SipCall;
import cx.ring.model.Uri;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.ConferenceService;
import cx.ring.utils.Log;

public class ConferencePresenter extends RootPresenter<ConferenceView> implements Observer {

    static final String TAG = ConferencePresenter.class.getSimpleName();

    @Inject
    ConferenceService mConferenceService;

    @Inject
    AccountService mAccountService;

    @Override
    public void afterInjection() {
        mConferenceService.addObserver(this);
    }

    public void placeCall (Uri number, boolean withVideo) {

        Account currentAccount = mAccountService.getCurrentAccount();
        SipCall call = new SipCall(null, currentAccount.getAccountID(), number, SipCall.Direction.OUTGOING);
        call.muteVideo(!withVideo);


    }

    @Override
    public void update(Observable observable, Object object) {
        if (!(object instanceof DaemonEvent)) {
            return;
        }

        DaemonEvent event = (DaemonEvent) object;
        switch (event.getEventType()) {

            default:
                Log.i(TAG, "Unknown daemon event");
                break;
        }
    }

}
