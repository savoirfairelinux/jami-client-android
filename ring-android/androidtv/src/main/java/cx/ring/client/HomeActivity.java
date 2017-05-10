/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package cx.ring.client;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.model.ConfigKey;
import cx.ring.model.ServiceEvent;
import cx.ring.model.Settings;
import cx.ring.services.AccountService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.services.HardwareService;
import cx.ring.services.PreferencesService;
import cx.ring.utils.Log;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import ezvcard.parameter.ImageType;
import ezvcard.property.FormattedName;
import ezvcard.property.Photo;
import ezvcard.property.RawProperty;

/*
 * MainActivity class that loads MainFragment
 */
public class HomeActivity extends Activity implements Observer<ServiceEvent> {
    private static final String TAG = HomeActivity.class.getName();
    private boolean mNoAccountOpened = false;
    private boolean mCreationError = false;
    private boolean mCreatedAccount = false;
    private String mAccountType;
    private Account mAccount;
    private String mCreatedAccountId;
    private String mUsername;

    public static final int REQUEST_CODE_CREATE_ACCOUNT = 7;
    public static final int REQUEST_CODE_CALL = 3;
    public static final int REQUEST_CODE_CONVERSATION = 4;

    public static final int REQUEST_CODE_PHOTO = 5;
    public static final int REQUEST_CODE_GALLERY = 6;
    public static final int REQUEST_PERMISSION_CAMERA = 113;
    public static final int REQUEST_PERMISSION_READ_STORAGE = 114;

    @Inject
    AccountService mAccountService;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    @Inject
    PreferencesService mPreferencesService;

    @Inject
    HardwareService mHardwareService;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        setContentView(R.layout.activity_home);

        // dependency injection
        ((RingApplication) getApplication()).getRingInjectionComponent().inject(this);

        String[] toRequest = buildPermissionsToAsk();
        ArrayList<String> permissionsWeCanAsk = new ArrayList<>();

        for (String permission : toRequest) {
            if (((RingApplication) getApplication()).canAskForPermission(permission)) {
                permissionsWeCanAsk.add(permission);
            }
        }

        if (!permissionsWeCanAsk.isEmpty()) {
//            mIsAskingForPermissions = true;
            ActivityCompat.requestPermissions(this, permissionsWeCanAsk.toArray(new String[permissionsWeCanAsk.size()]), RingApplication.PERMISSIONS_REQUEST);
        }
        else {
            checkAccount();
        }


    }

    public void checkAccount() {
        if (mAccountService.getAccounts().isEmpty()) {
            Log.d(TAG, "No account found");
            String mUsername = android.os.Build.MODEL;
            Log.d(TAG, "account name : " + mUsername);
            initRingAccountCreation("TEST-" + mUsername, "123456");
        }
            setContentView(R.layout.activity_home);
            Log.d(TAG, "Accounts => "+ mAccountService.getAccounts());
            Log.d(TAG, "Account => "+ mAccountService.getCurrentAccount());
    }

    public void initRingAccountCreation(String username, String password) {
        HashMap<String, String> accountDetails = initRingAccountDetails();
        if (accountDetails != null) {
            if (!TextUtils.isEmpty(username)) {
                accountDetails.put(ConfigKey.ACCOUNT_REGISTERED_NAME.key(), username);
            }
            if (!TextUtils.isEmpty(password)) {
                accountDetails.put(ConfigKey.ARCHIVE_PASSWORD.key(), password);
            }
            Log.d(TAG, "Account detail : " + accountDetails);
            createNewAccount(accountDetails);
        }
    }

    private HashMap<String, String> initRingAccountDetails() {
        HashMap<String, String> accountDetails = initAccountDetails();
        if (accountDetails != null) {
            accountDetails.put(ConfigKey.ACCOUNT_ALIAS.key(), mAccountService.getNewAccountName(getText(R.string.ring_account_default_name).toString()));
            accountDetails.put(ConfigKey.ACCOUNT_HOSTNAME.key(), "bootstrap.ring.cx");
            accountDetails.put(ConfigKey.ACCOUNT_UPNP_ENABLE.key(), AccountConfig.TRUE_STR);
        }
        return accountDetails;
    }

    private HashMap<String, String> initAccountDetails() {
        if (mAccountType == null) {
            mAccountType = AccountConfig.ACCOUNT_TYPE_RING;
        }
        try {
            Log.d(TAG, "ici " + mAccountType);
            HashMap<String, String> accountDetails = (HashMap<String, String>) mAccountService.getAccountTemplate(mAccountType);
            for (Map.Entry<String, String> e : accountDetails.entrySet()) {
                Log.d(TAG, "Default account detail: " + e.getKey() + " -> " + e.getValue());
            }

             boolean hasCameraPermission = mDeviceRuntimeService.hasVideoPermission();
            accountDetails.put(ConfigKey.VIDEO_ENABLED.key(), Boolean.toString(hasCameraPermission));

            //~ Sipinfo is forced for any sipaccount since overrtp is not supported yet.
            //~ This will have to be removed when it will be supported.
            accountDetails.put(ConfigKey.ACCOUNT_DTMF_TYPE.key(), getString(R.string.account_sip_dtmf_type_sipinfo));
            return accountDetails;
        } catch (Exception e) {
            Toast.makeText(this, "Error creating account", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error creating account", e);
            return null;
        }
    }

    private void createNewAccount(HashMap<String, String>... accs) {
        if (mAccountType.equals(AccountConfig.ACCOUNT_TYPE_RING) || mAccount == null) {
            mAccount = mAccountService.addAccount(accs[0]);
        }
    }

    private String[] buildPermissionsToAsk() {
        ArrayList<String> perms = new ArrayList<>();

        if (!mDeviceRuntimeService.hasAudioPermission()) {
            perms.add(Manifest.permission.RECORD_AUDIO);
        }

        Settings settings = mPreferencesService.loadSettings();

        if (settings.isAllowSystemContacts() && !mDeviceRuntimeService.hasContactPermission()) {
            perms.add(Manifest.permission.READ_CONTACTS);
        }

        if (!mDeviceRuntimeService.hasVideoPermission()) {
            perms.add(Manifest.permission.CAMERA);
        }

        if (settings.isAllowPlaceSystemCalls() && !mDeviceRuntimeService.hasCallLogPermission()) {
            perms.add(Manifest.permission.WRITE_CALL_LOG);
        }

        return perms.toArray(new String[perms.size()]);
    }

    @Override
    public boolean onSearchRequested() {
        startActivity(new Intent(this, SearchActivity.class));
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        android.util.Log.d(TAG, "onRequestPermissionsResult");

        switch (requestCode) {
            case RingApplication.PERMISSIONS_REQUEST: {
                if (grantResults.length == 0) {
                    return;
                }
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                for (int i = 0, n = permissions.length; i < n; i++) {
                    String permission = permissions[i];
                    ((RingApplication) getApplication()).permissionHasBeenAsked(permission);
                    switch (permission) {
                        case Manifest.permission.RECORD_AUDIO:
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                android.util.Log.e(TAG, "Missing required permission RECORD_AUDIO");
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
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                        @Override
                                        public void onDismiss(DialogInterface dialog) {
                                            finish();
                                        }
                                    });
                                }
                                builder.show();
                                return;
                            }
                            break;
                        case Manifest.permission.READ_CONTACTS:
                            sharedPref.edit().putBoolean(getString(R.string.pref_systemContacts_key), grantResults[i] == PackageManager.PERMISSION_GRANTED).apply();
                            break;
                        case Manifest.permission.CAMERA:
                            sharedPref.edit().putBoolean(getString(R.string.pref_systemCamera_key), grantResults[i] == PackageManager.PERMISSION_GRANTED).apply();
                            // permissions have changed, video params should be reset
                            final boolean isVideoAllowed = mDeviceRuntimeService.hasVideoPermission();
                            if (isVideoAllowed) {
                                mHardwareService.initVideo();
                            }
                    }
                }

                break;
            }
            case REQUEST_PERMISSION_READ_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, REQUEST_CODE_GALLERY);
                } else {
                    return;
                }
                break;
            case REQUEST_PERMISSION_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, REQUEST_CODE_PHOTO);
                } else {
                    return;
                }
                break;
        }

//        mIsAskingForPermissions = false;
        checkAccount();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAccountService.addObserver(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAccountService.removeObserver(this);
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        if (event == null || (AccountConfig.ACCOUNT_TYPE_SIP.equals(mAccountType))) {
            return;
        }

        switch (event.getEventType()) {
            case ACCOUNT_ADDED:
                mCreatedAccountId = event.getEventInput(ServiceEvent.EventInput.ACCOUNT_ID, String.class);
                cx.ring.utils.Log.d(TAG, "Event " + event.getEventType() + "id : " + mCreatedAccountId);
                handleCreationState(event);
                break;
            case REGISTRATION_STATE_CHANGED:
                cx.ring.utils.Log.d(TAG, "Event " + event.getEventType());
                handleCreationState(event);
                break;
            default:
                cx.ring.utils.Log.d(TAG, "Event " + event.getEventType() + " is not handled here");
                break;
        }
    }

    private void handleCreationState(final ServiceEvent event) {
        RingApplication.uiHandler.post(new Runnable() {
            @Override
            public void run() {

                String stateAccountId = event.getEventInput(ServiceEvent.EventInput.ACCOUNT_ID, String.class);
                Log.d(TAG, "stateAccountId : "  + stateAccountId);
                if (!TextUtils.isEmpty(stateAccountId) && stateAccountId.equals(mCreatedAccountId)) {
                    String newState = event.getEventInput(ServiceEvent.EventInput.STATE, String.class);
                    Log.d(TAG, "account status : " + newState);
                    mAccount = mAccountService.getAccount(mCreatedAccountId);

                    if (mAccount.isRing() && (newState.isEmpty() || newState.contentEquals(AccountConfig.STATE_INITIALIZING))) {
                        return;
                    }

/*                    if (!mCreationError) {
                        returnAccountStatus(newState);
                       if (mCreatedAccount) {
//                            saveProfile(mAccount.getAccountID());

                            if (!TextUtils.isEmpty(mUsername)) {
                                Log.i(TAG, "Account created, registering " + mUsername);
                                mAccountService.registerName(mAccount, "", mUsername);
                            }
                        }
                    }*/
                }
            }
        });
    }

    private void returnAccountStatus(String state) {
        switch (state) {
            case AccountConfig.STATE_ERROR_GENERIC:
                mCreationError = true;
                break;
            case AccountConfig.STATE_UNREGISTERED:
                mCreatedAccount = true;
                mCreationError = false;
                break;
            case AccountConfig.STATE_ERROR_NETWORK:
                mCreationError = true;
                break;
            default:
                mCreatedAccount = true;
                mCreationError = false;
                break;
        }
    }

   /* public void saveProfile(String accountID) {
        VCard vcard = new VCard();
        //TODO replace mUsername by mFullname to customise it
        vcard.setFormattedName(new FormattedName(mUsername));
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
*//*        if (mPhotoProfile != null) {
            mPhotoProfile.compress(Bitmap.CompressFormat.PNG, 100, stream);
            Photo photoVCard = new Photo(stream.toByteArray(), ImageType.PNG);
            vcard.removeProperties(Photo.class);
            vcard.addPhoto(photoVCard);
        }*//*
        vcard.removeProperties(RawProperty.class);
        VCardUtils.saveLocalProfileToDisk(vcard, accountID, getFilesDir());
    }*/

}