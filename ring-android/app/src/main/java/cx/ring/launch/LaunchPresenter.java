/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>s
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
package cx.ring.launch;

import android.Manifest;
import android.util.Log;

import java.util.ArrayList;

import javax.inject.Inject;

import cx.ring.model.Settings;
import cx.ring.mvp.RootPresenter;
import cx.ring.services.AccountService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
import cx.ring.services.PreferencesService;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class LaunchPresenter extends RootPresenter<LaunchView> {
    protected static final String TAG = LaunchPresenter.class.getSimpleName();

    protected AccountService mAccountService;
    protected DeviceRuntimeService mDeviceRuntimeService;
    protected PreferencesService mPreferencesService;
    protected HardwareService mHardwareService;

    @Inject
    public LaunchPresenter(AccountService accountService, DeviceRuntimeService deviceRuntimeService,
                           PreferencesService preferencesService, HardwareService hardwareService) {
        this.mAccountService = accountService;
        this.mDeviceRuntimeService = deviceRuntimeService;
        this.mPreferencesService = preferencesService;
        this.mHardwareService = hardwareService;
    }

    public void init() {
        String[] toRequest = buildPermissionsToAsk();
        if (toRequest.length == 0) {
            checkAccounts();
        } else {
            getView().askPermissions(toRequest);
        }
    }


    public void contactPermissionChanged(boolean isGranted) {
    }


    public void checkAccounts() {
        mCompositeDisposable.add(mAccountService.getObservableAccountList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(accounts -> {
                    if (accounts.isEmpty()) {
                        getView().goToAccountCreation();
                    } else {
                        getView().goToHome();
                    }
                    mCompositeDisposable.clear();
                }));
    }

    private String[] buildPermissionsToAsk() {
        ArrayList<String> perms = new ArrayList<>();


        Settings settings = mPreferencesService.getSettings();

        if (settings.isAllowSystemContacts() && !mDeviceRuntimeService.hasContactPermission()) {
            perms.add(Manifest.permission.READ_CONTACTS);
        }


        if (settings.isAllowPlaceSystemCalls() && !mDeviceRuntimeService.hasCallLogPermission()) {
            perms.add(Manifest.permission.WRITE_CALL_LOG);
        }

        return perms.toArray(new String[perms.size()]);
    }
}