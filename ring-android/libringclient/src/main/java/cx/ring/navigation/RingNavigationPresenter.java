/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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

package cx.ring.navigation;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.model.Account;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.utils.Log;
import cx.ring.utils.StringUtils;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import ezvcard.property.FormattedName;
import ezvcard.property.Photo;
import ezvcard.property.RawProperty;
import ezvcard.property.Uid;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class RingNavigationPresenter extends RootPresenter<RingNavigationView> {

    private static final String TAG = RingNavigationPresenter.class.getSimpleName();

    private AccountService mAccountService;
    private DeviceRuntimeService mDeviceRuntimeService;

    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    @Inject
    public RingNavigationPresenter(AccountService accountService,
                                   DeviceRuntimeService deviceRuntimeService) {
        mAccountService = accountService;
        mDeviceRuntimeService = deviceRuntimeService;
    }

    @Override
    public void bindView(RingNavigationView view) {
        super.bindView(view);
        mCompositeDisposable.add(mAccountService.getProfileAccountList()
                .observeOn(mUiScheduler)
                .subscribe(accounts -> {
                    RingNavigationView v = getView();
                    if (v != null)
                        v.showViewModel(new RingNavigationViewModel(accounts.isEmpty() ? null : accounts.get(0), accounts));
                }, e ->  Log.e(TAG, "Error loading account list !", e)));
        mCompositeDisposable.add(mAccountService.getObservableAccounts()
                .observeOn(mUiScheduler)
                .subscribe(account -> {
                    RingNavigationView v = getView();
                    if (v != null)
                        v.updateModel(account);
                }, e ->  Log.e(TAG, "Error loading account list !", e)));
    }

    public void setAccountOrder(Account selectedAccount) {
        mAccountService.setCurrentAccount(selectedAccount);
    }

    public void saveVCardPhoto(Single<Photo> photo) {
        Account account = mAccountService.getCurrentAccount();
        String accountId = account.getAccountID();
        String ringId = account.getUsername();
        File filesDir = mDeviceRuntimeService.provideFilesDir();

        mCompositeDisposable.add(Single.zip(
                VCardUtils.loadLocalProfileFromDisk(filesDir, accountId).subscribeOn(Schedulers.io()),
                photo, (vcard, pic) -> {
                    vcard.setUid(new Uid(ringId));
                    vcard.removeProperties(Photo.class);
                    vcard.addPhoto(pic);
                    vcard.removeProperties(RawProperty.class);
                    return vcard;
                })
                .flatMap(vcard -> VCardUtils.saveLocalProfileToDisk(vcard, accountId, filesDir))
                .subscribeOn(Schedulers.io())
                .subscribe(vcard -> {
                    account.setProfile(vcard);
                    mAccountService.refreshAccounts();
                }, e -> Log.e(TAG, "Error saving vCard !", e)));
    }

    public void saveVCardFormattedName(String username) {
        Account account = mAccountService.getCurrentAccount();
        String accountId = account.getAccountID();
        File filesDir = mDeviceRuntimeService.provideFilesDir();

        mCompositeDisposable.add(VCardUtils
                .loadLocalProfileFromDisk(filesDir, accountId)
                .doOnSuccess(vcard -> {
                    vcard.setFormattedName(username);
                    vcard.removeProperties(RawProperty.class);
                })
                .flatMap(vcard -> VCardUtils.saveLocalProfileToDisk(vcard, accountId, filesDir))
                .subscribeOn(Schedulers.io())
                .subscribe(vcard -> {
                    account.setProfile(vcard);
                    mAccountService.refreshAccounts();
                }, e -> Log.e(TAG, "Error saving vCard !", e)));
    }

    public void saveVCard(Account account, String username, Single<Photo> photo) {
        String accountId = account.getAccountID();
        String ringId = account.getUsername();
        File filesDir = mDeviceRuntimeService.provideFilesDir();
        mCompositeDisposable.add(Single.zip(
                VCardUtils.loadLocalProfileFromDisk(filesDir, accountId).subscribeOn(Schedulers.io()),
                photo, (vcard, pic) -> {
                    vcard.setUid(new Uid(ringId));
                    if (!StringUtils.isEmpty(username)) {
                        vcard.setFormattedName(username);
                    }
                    if (photo != null) {
                        vcard.removeProperties(Photo.class);
                        vcard.addPhoto(pic);
                    }
                    vcard.removeProperties(RawProperty.class);
                    return vcard;
                })
                .flatMap(vcard -> VCardUtils.saveLocalProfileToDisk(vcard, accountId, filesDir))
                .subscribeOn(Schedulers.io())
                .subscribe(vcard -> {
                    account.setProfile(vcard);
                    mAccountService.refreshAccounts();
                }, e -> Log.e(TAG, "Error saving vCard !", e)));
    }

    public String getAlias(Account account) {
        if (account == null) {
            Log.e(TAG, "Not able to get alias");
            return null;
        }
        VCard vcard = account.getProfile();
        if (vcard != null) {
            FormattedName name = vcard.getFormattedName();
            if (name != null) {
                String name_value = name.getValue();
                if (name_value != null && !name_value.isEmpty()) {
                    return name_value;
                }
            }
        }
        return null;
    }

    public String getAccountAlias(Account account) {
        if (account == null) {
            Log.e(TAG, "Not able to get account alias");
            return null;
        }
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
                mDeviceRuntimeService.hasWriteExternalStoragePermission();
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
}
