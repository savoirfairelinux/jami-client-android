/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AlertDialog;

import cx.ring.R;
import cx.ring.account.AccountWizardActivity;
import cx.ring.application.RingApplication;
import cx.ring.client.HomeActivity;
import cx.ring.mvp.BaseActivity;
import cx.ring.tv.account.TVAccountWizard;
import cx.ring.utils.DeviceUtils;

public class LaunchActivity extends BaseActivity<LaunchPresenter> implements LaunchView {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // dependency injection
        RingApplication.getInstance().getRingInjectionComponent().inject(this);
        super.onCreate(savedInstanceState);
        RingApplication.getInstance().startDaemon();

        setContentView(R.layout.activity_launch);

        presenter.init();
    }

    @Override
    public void askPermissions(String[] permissionsWeCanAsk) {
        ActivityCompat.requestPermissions(this, permissionsWeCanAsk, RingApplication.PERMISSIONS_REQUEST);
    }

    @Override
    public void goToHome() {
        startActivity(new Intent(LaunchActivity.this, DeviceUtils.isTv(this) ? cx.ring.tv.main.HomeActivity.class : HomeActivity.class));
        finish();
    }

    @Override
    public void goToAccountCreation() {
        startActivity(new Intent(LaunchActivity.this, DeviceUtils.isTv(this) ? TVAccountWizard.class : AccountWizardActivity.class));
        finish();
    }

    @Override
    public void displayAudioPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.start_error_title)
                .setMessage(R.string.start_error_mic_required)
                .setIcon(R.drawable.ic_mic_black)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> finish()).setOnCancelListener(dialog -> finish())
                .setOnDismissListener(dialog -> finish())
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case RingApplication.PERMISSIONS_REQUEST: {
                if (grantResults.length == 0) {
                    return;
                }
                for (int i = 0, n = permissions.length; i < n; i++) {
                    String permission = permissions[i];
                    RingApplication.getInstance().permissionHasBeenAsked(permission);
                    switch (permission) {
                        case Manifest.permission.READ_CONTACTS:
                            presenter.contactPermissionChanged(grantResults[i] == PackageManager.PERMISSION_GRANTED);
                            break;
                    }
                }
                break;
            }

        }
    }
}