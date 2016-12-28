/*
 *  Copyright (C) 2016 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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

package cx.ring.navigation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import cx.ring.model.Account;
import cx.ring.model.DaemonEvent;
import cx.ring.mvp.GenericView;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import ezvcard.property.Photo;
import ezvcard.property.RawProperty;

public class RingNavigationPresenter extends RootPresenter<GenericView<RingNavigationViewModel>> implements Observer<DaemonEvent> {

    @Inject
    AccountService mAccountService;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    private String mDefaultName;

    @Override
    public void afterInjection() {
        // We observe the application state changes
        mAccountService.addObserver(this);
    }

    @Override
    public void bindView(GenericView<RingNavigationViewModel> view) {
        mAccountService.addObserver(this);
        super.bindView(view);
    }

    @Override
    public void unbindView() {
        mAccountService.removeObserver(this);
        super.unbindView();
    }

    public void initializePresenter(String defaultName) {
        mDefaultName = defaultName;
        updateUser();
    }

    public void updateUser() {
        if (getView() == null) {
            return;
        }

        Account currentAccount = mAccountService.getCurrentAccount();
        getView().showViewModel(new RingNavigationViewModel(currentAccount, mAccountService.getAccounts()));
    }

    public void setAccountOrder(Account selectedAccount) {
        if (getView() == null) {
            return;
        }
        mAccountService.setCurrentAccount(selectedAccount);
    }

    public void saveVCard(String username, Photo photo) {
        String accountId = mAccountService.getCurrentAccount().getAccountID();

        if (username.isEmpty()) {
            username = mDefaultName;
        }
        File filesDir = mDeviceRuntimeService.provideFilesDir();

        VCard vcard = VCardUtils.loadLocalProfileFromDisk(filesDir, accountId, mDefaultName);
        vcard.setFormattedName(username);
        vcard.removeProperties(Photo.class);
        vcard.addPhoto(photo);
        vcard.removeProperties(RawProperty.class);
        VCardUtils.saveLocalProfileToDisk(vcard, accountId, filesDir);

        updateUser();
    }

    public String getAlias(Account account) {
        VCard vcard = VCardUtils.loadLocalProfileFromDisk(mDeviceRuntimeService.provideFilesDir(), account.getAccountID(), mDefaultName);
        return vcard.getFormattedName().getValue();
    }

    public String getHost(Account account, String defaultNameSip) {
        String username;
        if (account.isRing()) {
            username = account.getRegisteredName();
            if (account.registeringUsername || username == null || username.isEmpty()) {
                username = account.getUsername();
            }
        } else if (account.isSip() && !account.isIP2IP()) {
            username = account.getUsername() + "@" + account.getHost();
        } else {
            username = defaultNameSip;
        }
        return username;
    }

    @Override
    public void update(Observable observable, DaemonEvent event) {
        if (event == null) {
            return;
        }

        if (event.getEventType() == DaemonEvent.EventType.ACCOUNTS_CHANGED) {
            updateUser();
        }
    }
}
