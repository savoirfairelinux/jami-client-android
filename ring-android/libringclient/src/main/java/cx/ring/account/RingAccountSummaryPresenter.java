/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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

package cx.ring.account;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;

import javax.inject.Inject;
import javax.inject.Named;

import cx.ring.model.Account;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
import cx.ring.utils.FileUtils;
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

public class RingAccountSummaryPresenter extends RootPresenter<RingAccountSummaryView> {

    private static final String TAG = RingAccountSummaryPresenter.class.getSimpleName();

    private final DeviceRuntimeService mDeviceRuntimeService;
    private final HardwareService mHardwareService;
    private AccountService mAccountService;
    private String mAccountID;

    @Inject
    @Named("UiScheduler")
    protected Scheduler mUiScheduler;

    @Inject
    public RingAccountSummaryPresenter(AccountService accountService,
                                        HardwareService hardwareService,
                                        DeviceRuntimeService deviceRuntimeService) {
        mAccountService = accountService;
        mHardwareService = hardwareService;
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
        RingAccountSummaryView v = getView();
        if (v != null)
            v.accountChanged(mAccountService.getAccount(mAccountID));
        mCompositeDisposable.add(mAccountService.getObservableAccountUpdates(mAccountID)
                .observeOn(mUiScheduler)
                .subscribe(account -> {
                    RingAccountSummaryView view = getView();
                    if (view != null) {
                        view.accountChanged(account);
                    }
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

    public void revokeDevice(final String deviceId, String password) {
        if (getView() != null) {
            getView().showRevokingProgressDialog();
        }
        mCompositeDisposable.add(mAccountService
                .revokeDevice(mAccountID, password, deviceId)
                .observeOn(mUiScheduler)
                .subscribe(result -> getView().deviceRevocationEnded(deviceId, result)));
    }

    public void renameDevice(String newName) {
        mAccountService.renameDevice(mAccountID, newName);
    }

    public void changePassword(String oldPassword, String newPassword) {
        RingAccountSummaryView view = getView();
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

}
