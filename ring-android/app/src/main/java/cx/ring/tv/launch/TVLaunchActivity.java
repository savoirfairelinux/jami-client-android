package cx.ring.tv.launch;/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
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

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.ArrayList;

import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.launch.LaunchPresenter;
import cx.ring.launch.LaunchView;
import cx.ring.mvp.BaseActivity;
import cx.ring.tv.account.TVAccountWizard;
import cx.ring.tv.main.HomeActivity;

public class TVLaunchActivity extends BaseActivity<LaunchPresenter> implements LaunchView {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // dependency injection
        ((RingApplication) getApplication()).getRingInjectionComponent().inject(this);
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        setContentView(R.layout.activity_launch);

        presenter.init();
    }


    @Override
    public void goToHome() {
        startActivity(new Intent(TVLaunchActivity.this, HomeActivity.class));
        finish();
    }

    @Override
    public void goToAccountCreation() {
        startActivity(new Intent(TVLaunchActivity.this, TVAccountWizard.class));
        finish();
    }


    @Override
    public void askPermissions(ArrayList<String> permissionsWeCanAsk) {
        ActivityCompat.requestPermissions(this, permissionsWeCanAsk.toArray(new String[permissionsWeCanAsk.size()]), RingApplication.PERMISSIONS_REQUEST);
    }


    @Override
    public void displayAudioPermissionDialog() {
        Log.e(TAG, "Missing required permission RECORD_AUDIO");
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.start_error_title)
                .setMessage(R.string.start_error_mic_required)
                .setIcon(R.drawable.ic_mic_black)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });

        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");

        switch (requestCode) {
            case RingApplication.PERMISSIONS_REQUEST: {
                if (grantResults.length == 0) {
                    return;
                }
                boolean isAudioGranted = false;
                for (int i = 0, n = permissions.length; i < n; i++) {
                    String permission = permissions[i];
                    ((RingApplication) getApplication()).permissionHasBeenAsked(permission);
                    switch (permission) {
                        case Manifest.permission.RECORD_AUDIO:
                            isAudioGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                            presenter.audioPermissionChanged(isAudioGranted);
                            break;
                        case Manifest.permission.READ_CONTACTS:
                            presenter.contactPermissionChanged(grantResults[i] == PackageManager.PERMISSION_GRANTED);
                            break;
                        case Manifest.permission.CAMERA:
                            presenter.cameraPermissionChanged(grantResults[i] == PackageManager.PERMISSION_GRANTED);
                    }
                }
                if (isAudioGranted) {
                    presenter.checkAccounts();
                }
                break;
            }

        }
    }

}