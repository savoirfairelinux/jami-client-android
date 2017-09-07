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

import javax.inject.Inject;

import cx.ring.facades.ConversationFacade;
import cx.ring.model.Account;
import cx.ring.model.ServiceEvent;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import ezvcard.property.FormattedName;
import ezvcard.property.Photo;
import ezvcard.property.RawProperty;
import ezvcard.property.Uid;

public class RingNavigationPresenter extends RootPresenter<RingNavigationView> implements Observer<ServiceEvent> {

    private AccountService mAccountService;
    private DeviceRuntimeService mDeviceRuntimeService;
    private ConversationFacade mConversationFacade;

    @Inject
    public RingNavigationPresenter(AccountService accountService,
                                   DeviceRuntimeService deviceRuntimeService,
                                   ConversationFacade conversationFacade) {
        this.mAccountService = accountService;
        this.mDeviceRuntimeService = deviceRuntimeService;
        this.mConversationFacade = conversationFacade;
    }

    @Override
    public void bindView(RingNavigationView view) {
        mAccountService.addObserver(this);
        super.bindView(view);
    }

    @Override
    public void unbindView() {
        mAccountService.removeObserver(this);
        super.unbindView();
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
        mConversationFacade.clearConversations();
        mAccountService.setCurrentAccount(selectedAccount);
    }

    public void saveVCard(String username, Photo photo) {
        String accountId = mAccountService.getCurrentAccount().getAccountID();
        String ringId = mAccountService.getCurrentAccount().getUsername();
        File filesDir = mDeviceRuntimeService.provideFilesDir();

        VCard vcard = VCardUtils.loadLocalProfileFromDisk(filesDir, accountId);
        vcard.setFormattedName(username);
        vcard.setUid(new Uid(ringId));
        vcard.removeProperties(Photo.class);
        vcard.addPhoto(photo);
        vcard.removeProperties(RawProperty.class);
        VCardUtils.saveLocalProfileToDisk(vcard, accountId, filesDir);

        updateUser();
    }

    public void saveVCard(String username) {
        String accountId = mAccountService.getCurrentAccount().getAccountID();
        File filesDir = mDeviceRuntimeService.provideFilesDir();

        VCard vcard = VCardUtils.loadLocalProfileFromDisk(filesDir, accountId);
        vcard.setFormattedName(username);
        vcard.removeProperties(RawProperty.class);
        VCardUtils.saveLocalProfileToDisk(vcard, accountId, filesDir);

        updateUser();
    }

    public String getAlias(Account account) {
        VCard vcard = VCardUtils.loadLocalProfileFromDisk(mDeviceRuntimeService.provideFilesDir(), account.getAccountID());
        FormattedName name = vcard.getFormattedName();
        if (name != null) {
            String name_value = name.getValue();
            if (name_value != null && !name_value.isEmpty()) {
                return name_value;
            }
        }
        return null;
    }

    public String getAccountAlias(Account account) {
        String alias = getAlias(account);
        return (alias == null) ? account.getAlias() : alias;
    }

    public String getUri(Account account, CharSequence defaultNameSip) {
        if (account.isIP2IP()) {
            return defaultNameSip.toString();
        }
        return account.getDisplayUri();
    }

    public void cameraClicked() {
        boolean hasPermission = mDeviceRuntimeService.hasVideoPermission() &&
                mDeviceRuntimeService.hasPhotoPermission();
        if (hasPermission) {
            getView().gotToImageCapture();
        } else {
            getView().askCameraPermission();
        }
    }

    public void galleryClicked() {
        boolean hasPermission = mDeviceRuntimeService.hasGalleryPermission();
        if (hasPermission) {
            getView().goToGallery();
        } else {
            getView().askGalleryPermission();
        }
    }


    @Override
    public void update(Observable observable, ServiceEvent event) {
        if (event == null) {
            return;
        }

        switch (event.getEventType()) {
            case ACCOUNTS_CHANGED:
            case REGISTRATION_STATE_CHANGED:
                updateUser();
                break;
        }
    }
}
