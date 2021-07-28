/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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

package net.jami.navigation;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import net.jami.model.Account;
import net.jami.mvp.RootPresenter;
import net.jami.services.AccountService;
import net.jami.services.DeviceRuntimeService;
import net.jami.services.HardwareService;
import net.jami.utils.Log;
import net.jami.utils.StringUtils;
import net.jami.utils.Tuple;
import net.jami.utils.VCardUtils;
import ezvcard.property.Photo;
import ezvcard.property.RawProperty;
import ezvcard.property.Uid;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class HomeNavigationPresenter extends RootPresenter<HomeNavigationView> {

    private static final String TAG = HomeNavigationPresenter.class.getSimpleName();

    private final AccountService mAccountService;
    private final DeviceRuntimeService mDeviceRuntimeService;
    private final HardwareService mHardwareService;

    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    @Inject
    public HomeNavigationPresenter(AccountService accountService,
                                   HardwareService hardwareService,
                                   DeviceRuntimeService deviceRuntimeService) {
        mAccountService = accountService;
        mHardwareService = hardwareService;
        mDeviceRuntimeService = deviceRuntimeService;
    }

    @Override
    public void bindView(HomeNavigationView view) {
        super.bindView(view);
        mCompositeDisposable.add(mAccountService.getCurrentProfileAccountSubject()
                .switchMapSingle(account -> account.getLoadedProfile().map(alias -> new Tuple<>(account, alias)))
                .observeOn(mUiScheduler)
                .subscribe(alias -> {
                    HomeNavigationView v = getView();
                    if (v != null)
                        v.showViewModel(new HomeNavigationViewModel(alias.first, alias.second.first));
                }, e ->  Log.e(TAG, "Error loading account list !", e)));
        mCompositeDisposable.add(mAccountService.getObservableAccounts()
                .observeOn(mUiScheduler)
                .subscribe(account -> {
                    HomeNavigationView v = getView();
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
                VCardUtils.loadLocalProfileFromDiskWithDefault(filesDir, accountId)
                        .subscribeOn(Schedulers.io()),
                photo
                        .subscribeOn(Schedulers.io()),
                (vcard, pic) -> {
                    vcard.setUid(new Uid(ringId));
                    vcard.removeProperties(Photo.class);
                    vcard.addPhoto(pic);
                    vcard.removeProperties(RawProperty.class);
                    return vcard;
                })
                .flatMap(vcard -> VCardUtils.saveLocalProfileToDisk(vcard, accountId, filesDir))
                .subscribeOn(Schedulers.io())
                .subscribe(vcard -> {
                    account.resetProfile();
                    mAccountService.refreshAccounts();
                    getView().setPhoto(account);
                }, e -> Log.e(TAG, "Error saving vCard !", e)));
    }

    public void saveVCardFormattedName(String username) {
        Account account = mAccountService.getCurrentAccount();
        String accountId = account.getAccountID();
        File filesDir = mDeviceRuntimeService.provideFilesDir();

        mCompositeDisposable.add(VCardUtils
                .loadLocalProfileFromDiskWithDefault(filesDir, accountId)
                .doOnSuccess(vcard -> {
                    vcard.setFormattedName(username);
                    vcard.removeProperties(RawProperty.class);
                })
                .flatMap(vcard -> VCardUtils.saveLocalProfileToDisk(vcard, accountId, filesDir))
                .subscribeOn(Schedulers.io())
                .subscribe(vcard -> {
                    account.resetProfile();
                    mAccountService.refreshAccounts();
                    bindView(getView());
                }, e -> Log.e(TAG, "Error saving vCard !", e)));
    }

    public void saveVCard(Account account, String username, Single<Photo> photo) {
        String accountId = account.getAccountID();
        String ringId = account.getUsername();
        File filesDir = mDeviceRuntimeService.provideFilesDir();
        mCompositeDisposable.add(Single.zip(
                VCardUtils.loadLocalProfileFromDiskWithDefault(filesDir, accountId).subscribeOn(Schedulers.io()),
                photo, (vcard, pic) -> {
                    vcard.setUid(new Uid(ringId));
                    if (!StringUtils.isEmpty(username)) {
                        vcard.setFormattedName(username);
                    }
                    vcard.removeProperties(Photo.class);
                    vcard.addPhoto(pic);
                    vcard.removeProperties(RawProperty.class);
                    return vcard;
                })
                .flatMap(vcard -> VCardUtils.saveLocalProfileToDisk(vcard, accountId, filesDir))
                .subscribeOn(Schedulers.io())
                .subscribe(vcard -> {
                    account.resetProfile();
                    mAccountService.refreshAccounts();
                }, e -> Log.e(TAG, "Error saving vCard !", e)));
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

    public void cameraPermissionChanged(boolean isGranted) {
        if (isGranted && mHardwareService.isVideoAvailable()) {
            mHardwareService.initVideo()
                    .onErrorComplete()
                    .subscribe();
        }
    }
}
