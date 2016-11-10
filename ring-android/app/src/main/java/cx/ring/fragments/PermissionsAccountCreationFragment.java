package cx.ring.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.client.AccountWizard;
import cx.ring.client.HomeActivity;

/**
 * Created by abonnet on 16-11-11.
 */

public class PermissionsAccountCreationFragment extends Fragment {

    public static final int REQUEST_PERMISSION_MICROPHONE = 1;
    public static final int REQUEST_PERMISSION_CAMERA = 2;
    public static final int REQUEST_PERMISSION_CONTACTS = 3;

    @BindView(R.id.switch_microphone)
    Switch mMicrophone;

    @BindView(R.id.switch_camera)
    Switch mCamera;

    @BindView(R.id.switch_contacts)
    Switch mContacts;

    @BindView(R.id.last_create_account)
    Button mLastButton;

    @BindView(R.id.create_account)
    Button mCreateAccountButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.frag_acc_permissions_create, parent, false);
        ButterKnife.bind(this, view);

        return view;
    }

    public void changePermissionMicrophone(Boolean permission) {
        mMicrophone.setChecked(permission);
    }

    public void changePermissionCamera(Boolean permission) {
        mCamera.setChecked(permission);
    }

    public void changePermissionContacts(Boolean permission) {
        mContacts.setChecked(permission);
    }

    @OnCheckedChanged(R.id.switch_microphone)
    public void microphoneCheckedChanged() {
        Log.d("Permissions", "permission demandée");
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{Manifest.permission.WRITE_CALL_LOG},
                REQUEST_PERMISSION_MICROPHONE);
    }

    @OnCheckedChanged(R.id.switch_camera)
    public void cameraCheckedChanged() {
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{Manifest.permission.CAMERA},
                REQUEST_PERMISSION_CAMERA);
    }

    @OnCheckedChanged(R.id.switch_contacts)
    public void contactsCheckedChanged() {
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{Manifest.permission.READ_CONTACTS},
                REQUEST_PERMISSION_CONTACTS);
    }

    @OnClick(R.id.last_create_account)
    public void lastClicked() {
        AccountWizard accountWizard = (AccountWizard) getActivity();
        accountWizard.permissionsLast();
    }

    @OnClick(R.id.create_account)
    public void createAccountClicked() {
        AccountWizard accountWizard = (AccountWizard) getActivity();
        accountWizard.createAccount();
    }
}
