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
package cx.ring.share;

import java.util.Observable;
import java.util.Observer;

import javax.inject.Inject;

import cx.ring.model.Account;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.StateService;
import cx.ring.utils.QRCodeUtils;

public class SharePresenter extends RootPresenter<ShareView> implements Observer {

    @Inject
    StateService mStateService;

    @Override
    public void afterInjection() {
        // We observe the application state changes
        mStateService.addObserver(this);
    }

    public void loadContactInformation() {
        if (getView() == null) {
            return;
        }

        // ask for the current app state
        Account currentAccount = mStateService.getCurrentAccount();

        if (currentAccount==null || !currentAccount.isEnabled()) {
            getView().showQRCode(null);
            getView().showShareMessage("", "");
        } else {
            QRCodeUtils.QRCodeData qrCodeData = QRCodeUtils.encodeStringAsQRCodeData(currentAccount.getShareURI());
            getView().showQRCode(qrCodeData);
            getView().showShareMessage(currentAccount.getShareURI(), currentAccount.getRegisteredName());
        }
    }

    @Override
    public void update(Observable observable, Object o) {
        loadContactInformation();
    }
}
