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

import javax.inject.Inject;

import cx.ring.model.Account;
import cx.ring.model.ServiceEvent;
import cx.ring.mvp.GenericView;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;

public class SharePresenter extends RootPresenter<GenericView<ShareViewModel>> implements Observer<ServiceEvent> {

    @Inject
    AccountService mAccountService;

    @Override
    public void afterInjection() {
        // We observe the application state changes
        mAccountService.addObserver(this);
    }

    public void loadContactInformation() {
        if (getView() == null) {
            return;
        }

        // ask for the current account
        Account currentAccount = mAccountService.getCurrentAccount();

        // let the view display the ViewModel
        getView().showViewModel(new ShareViewModel(currentAccount));
    }

    @Override
    public void update(Observable observable, ServiceEvent o) {
        if (observable instanceof AccountService) {
            loadContactInformation();
        }
    }
}
