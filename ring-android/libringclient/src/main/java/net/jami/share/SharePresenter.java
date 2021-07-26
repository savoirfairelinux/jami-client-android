/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package net.jami.share;

import net.jami.services.AccountService;

import javax.inject.Inject;
import javax.inject.Named;

import net.jami.mvp.GenericView;
import net.jami.mvp.RootPresenter;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class SharePresenter extends RootPresenter<GenericView<ShareViewModel>> {
    private final net.jami.services.AccountService mAccountService;
    private final Scheduler mUiScheduler;

    @Inject
    public SharePresenter(AccountService accountService, @Named("UiScheduler") Scheduler uiScheduler) {
        mAccountService = accountService;
        mUiScheduler = uiScheduler;
    }

    @Override
    public void bindView(GenericView<ShareViewModel> view) {
        super.bindView(view);
        mCompositeDisposable.add(mAccountService
                .getCurrentAccountSubject()
                .map(ShareViewModel::new)
                .subscribeOn(Schedulers.computation())
                .observeOn(mUiScheduler)
                .subscribe(this::loadContactInformation));
    }

    private void loadContactInformation(ShareViewModel model) {
        GenericView<ShareViewModel> view = getView();
        if (view != null) {
            view.showViewModel(model);
        }
    }
}
