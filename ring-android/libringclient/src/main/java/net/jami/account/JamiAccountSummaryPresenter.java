/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package net.jami.account;

import net.jami.model.Account;
import net.jami.services.AccountService;
import net.jami.services.DeviceRuntimeService;

import java.io.File;
import java.net.SocketException;

import javax.inject.Inject;
import javax.inject.Named;

import net.jami.mvp.RootPresenter;
import net.jami.utils.Log;
import net.jami.utils.StringUtils;
import net.jami.utils.VCardUtils;

import ezvcard.property.Photo;
import ezvcard.property.RawProperty;
import ezvcard.property.Uid;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class JamiAccountSummaryPresenter extends RootPresenter<JamiAccountSummaryView> {

    private static final String TAG = JamiAccountSummaryPresenter.class.getSimpleName();

    private final net.jami.services.DeviceRuntimeService mDeviceRuntimeService;
    private final net.jami.services.AccountService mAccountService;
    private String mAccountID;

    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    @Inject
    public JamiAccountSummaryPresenter(AccountService accountService,
                                       DeviceRuntimeService deviceRuntimeService) {
        mAccountService = accountService;
        mDeviceRuntimeService = deviceRuntimeService;
    }

    public void registerName(String name, String password) {
        final Account account = mAccountService.getAccount(mAccountID);
        if (account == null || getView() == null) {
            return;
        }
        mAccountService.registerName(account, password, name);
        getView().accountChanged(account);
    }

    public void startAccountExport(String password) {
        if (getView() == null) {
            return;
        }
        getView().showExportingProgressDialog();
        mCompositeDisposable.add(mAccountService
                .exportOnRing(mAccountID, password)
                .observeOn(mUiScheduler)
                .subscribe(pin -> getView().showPIN(pin),
                           error -> {
                    if (error instanceof IllegalArgumentException) {
                        getView().showPasswordError();
                    } else if (error instanceof SocketException) {
                        getView().showNetworkError();
                    } else {
                        getView().showGenericError();
                    }
                }));
    }

    public void setAccountId(String accountID) {
        mCompositeDisposable.clear();
        mAccountID = accountID;
        net.jami.account.JamiAccountSummaryView v = getView();
        Account account = mAccountService.getAccount(mAccountID);
        if (v != null && account != null)
            v.accountChanged(account);
        mCompositeDisposable.add(mAccountService.getObservableAccountUpdates(mAccountID)
                .observeOn(mUiScheduler)
                .subscribe(a -> {
                    net.jami.account.JamiAccountSummaryView view = getView();
                    if (view != null)
                        view.accountChanged(a);
                }));
    }

    public void enableAccount(boolean newValue) {
        Account account = mAccountService.getAccount(mAccountID);
        if (account == null) {
            Log.w(TAG, "account not found!");
            return;
        }

        account.setEnabled(newValue);
        mAccountService.setAccountEnabled(account.getAccountID(), newValue);
    }

    public void changePassword(String oldPassword, String newPassword) {
        net.jami.account.JamiAccountSummaryView view = getView();
        if (view != null)
            view.showPasswordProgressDialog();
        mCompositeDisposable.add(mAccountService.setAccountPassword(mAccountID, oldPassword, newPassword)
                .observeOn(mUiScheduler)
                .subscribe(
                        () -> getView().passwordChangeEnded(true),
                        e -> getView().passwordChangeEnded(false)));
    }

    public String getDeviceName() {
        Account account = mAccountService.getAccount(mAccountID);
        if (account == null) {
            Log.w(TAG, "account not found!");
            return null;
        }
        return account.getDeviceName();
    }

    public void downloadAccountsArchive(File dest, String password) {
        getView().showExportingProgressDialog();
        mCompositeDisposable.add(
            mAccountService.exportToFile(mAccountID, dest.getAbsolutePath(), password)
            .observeOn(mUiScheduler)
            .subscribe(() -> getView().displayCompleteArchive(dest),
                    error -> getView().passwordChangeEnded(false)));
    }

    public void saveVCardFormattedName(String username) {
        Account account = mAccountService.getAccount(mAccountID);
        File filesDir = mDeviceRuntimeService.provideFilesDir();

        mCompositeDisposable.add(VCardUtils.loadLocalProfileFromDiskWithDefault(filesDir, mAccountID)
                .doOnSuccess(vcard -> {
                    vcard.setFormattedName(username);
                    vcard.removeProperties(RawProperty.class);
                })
                .flatMap(vcard -> VCardUtils.saveLocalProfileToDisk(vcard, mAccountID, filesDir))
                .subscribeOn(Schedulers.io())
                .subscribe(vcard -> {
                    account.resetProfile();
                    mAccountService.refreshAccounts();
                    getView().updateUserView(account);
                }, e -> Log.e(TAG, "Error saving vCard !", e)));
    }

    public void saveVCard(String username, Single<Photo> photo) {
        Account account = mAccountService.getAccount(mAccountID);
        String ringId = account.getUsername();
        File filesDir = mDeviceRuntimeService.provideFilesDir();
        mCompositeDisposable.add(Single.zip(
                VCardUtils.loadLocalProfileFromDiskWithDefault(filesDir, mAccountID).subscribeOn(Schedulers.io()),
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
                .flatMap(vcard -> VCardUtils.saveLocalProfileToDisk(vcard, mAccountID, filesDir))
                .subscribeOn(Schedulers.io())
                .subscribe(vcard -> {
                    account.resetProfile();
                    mAccountService.refreshAccounts();
                    getView().updateUserView(account);
                }, e -> Log.e(TAG, "Error saving vCard !", e)));
    }

    public void cameraClicked() {
        boolean hasPermission = mDeviceRuntimeService.hasVideoPermission() &&
                mDeviceRuntimeService.hasWriteExternalStoragePermission();
        net.jami.account.JamiAccountSummaryView view = getView();
        if (view != null) {
            if (hasPermission) {
                view.gotToImageCapture();
            } else {
                view.askCameraPermission();
            }
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

    public void goToAccount() {
        getView().goToAccount(mAccountID);
    }

    public void goToMedia() {
        getView().goToMedia(mAccountID);
    }

    public void goToSystem() {
        getView().goToSystem(mAccountID);
    }

    public void goToAdvanced() {
        getView().goToAdvanced(mAccountID);
    }

    public void revokeDevice(final String deviceId, String password) {
        if (getView() != null) {
            getView().showRevokingProgressDialog();
        }
        mCompositeDisposable.add(mAccountService
                .revokeDevice(mAccountID, password, deviceId)
                .observeOn(mUiScheduler)
                .subscribe(result -> {
                    getView().deviceRevocationEnded(deviceId, result);
                    getView().updateDeviceList(mAccountService.getAccount(mAccountID).getDevices(),
                            mAccountService.getAccount(mAccountID).getDeviceId());
                }));
    }

    public void renameDevice(String newName) {
        mAccountService.renameDevice(mAccountID, newName);
    }

    public Account getAccount() {
        return mAccountService.getAccount(mAccountID);
    }
}
