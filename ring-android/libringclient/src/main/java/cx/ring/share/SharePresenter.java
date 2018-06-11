/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
import cx.ring.mvp.GenericView;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;

public class SharePresenter extends RootPresenter<GenericView<ShareViewModel>> {
    private AccountService mAccountService;

    @Inject
    public SharePresenter(AccountService accountService) {
        mAccountService = accountService;
    }

    @Override
    public void bindView(GenericView<ShareViewModel> view) {
        super.bindView(view);
        mCompositeDisposable.add(mAccountService.getCurrentAccountSubject().subscribe(this::loadContactInformation));
    }

    @Override
    public void unbindView() {
        super.unbindView();
    }

    public void loadContactInformation() {
        loadContactInformation(mAccountService.getCurrentAccount());
    }
    public void loadContactInformation(Account a) {
        if (getView() == null) {
            return;
        }
        // let the view display the ViewModel
        getView().showViewModel(new ShareViewModel(a));
    }
}
