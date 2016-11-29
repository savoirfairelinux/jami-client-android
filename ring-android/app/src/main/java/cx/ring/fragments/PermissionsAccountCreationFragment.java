/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.fragments;

import android.Manifest;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.client.AccountWizard;

public class PermissionsAccountCreationFragment extends Fragment {

    public static final int REQUEST_PERMISSION_MICROPHONE = 1;
    public static final int REQUEST_PERMISSION_CAMERA = 2;
    public static final int REQUEST_PERMISSION_CONTACTS = 3;

    @BindView(R.id.switch_microphone)
    Switch mMicrophone;

    @BindView(R.id.microphone_help)
    TextView mMicrophoneHelp;

    @BindView(R.id.switch_camera)
    Switch mCamera;

    @BindView(R.id.camera_help)
    TextView mCameraHelp;

    @BindView(R.id.switch_contacts)
    Switch mContacts;

    @BindView(R.id.contacts_help)
    TextView mContactsHelp;

    @BindView(R.id.last_create_account)
    Button mLastButton;

    @BindView(R.id.create_account)
    Button mCreateAccountButton;

    private SharedPreferences mSharedPref;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.frag_acc_permissions_create, parent, false);
        ButterKnife.bind(this, view);

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        checkPermission();
        return view;
    }

    private void checkPermission() {
        Boolean hasPermissionAudio = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        Boolean hasPermissionCamera = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        Boolean hasPermissionContacts = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;

        changePermissionMicrophone(hasPermissionAudio);
        changePermissionCamera(hasPermissionCamera);
        changePermissionContacts(hasPermissionContacts);
    }

    public void changePermissionMicrophone(Boolean permission) {
        mMicrophone.setChecked(permission);
        mMicrophone.setEnabled(!permission);
        mMicrophoneHelp.setEnabled(!permission);
        mCreateAccountButton.setEnabled(permission);
    }

    public void changePermissionCamera(Boolean permission) {
        mCamera.setChecked(permission);
        mCamera.setEnabled(!permission);
        mCameraHelp.setEnabled(!permission);
        mSharedPref.edit().putBoolean(getString(R.string.pref_systemCamera_key), permission).apply();
    }

    public void changePermissionContacts(Boolean permission) {
        mContacts.setChecked(permission);
        mContacts.setEnabled(!permission);
        mContactsHelp.setEnabled(!permission);
        mSharedPref.edit().putBoolean(getString(R.string.pref_systemContacts_key), permission).apply();
    }

    @OnClick(R.id.switch_microphone)
    public void microphoneClicked() {
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_PERMISSION_MICROPHONE);
    }

    @OnClick(R.id.switch_camera)
    public void cameraClicked() {
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{Manifest.permission.CAMERA},
                REQUEST_PERMISSION_CAMERA);
    }

    @OnClick(R.id.switch_contacts)
    public void contactsClicked() {
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{Manifest.permission.READ_CONTACTS},
                REQUEST_PERMISSION_CONTACTS);
    }

    @OnClick(R.id.last_create_account)
    public void lastClicked() {
        ((AccountWizard) getActivity()).permissionsLast();
    }

    @OnClick(R.id.create_account)
    public void createAccountClicked() {
        ((AccountWizard) getActivity()).createAccount();
    }
}
