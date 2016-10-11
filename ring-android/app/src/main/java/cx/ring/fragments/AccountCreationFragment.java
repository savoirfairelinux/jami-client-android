/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnFocusChange;
import cx.ring.R;
import cx.ring.model.account.Account;
import cx.ring.model.account.AccountDetail;
import cx.ring.model.account.AccountDetailAdvanced;
import cx.ring.model.account.AccountDetailBasic;
import cx.ring.model.account.AccountDetailVolatile;
import cx.ring.service.LocalService;

public class AccountCreationFragment extends Fragment {
    static final String TAG = AccountCreationFragment.class.getSimpleName();

    private static final int FILE_SELECT_CODE = 2;
    private static final int REQUEST_READ_STORAGE = 113;

    // Values for email and password at the time of the login attempt.
    private String mAlias;
    private String mHostname;
    private String mUsername;
    private String mPassword;
    private String mAccountType;

    private String mDataPath;
    private LocalService.Callbacks mCallbacks = LocalService.DUMMY_CALLBACKS;
    private CreateAccountTask mAccountTask;

    // UI references.
    @BindView(R.id.addAccountLayout)
    LinearLayout mAddAccountLayout;

    @BindView(R.id.newAccountLayout)
    LinearLayout mNewAccountLayout;

    @BindView(R.id.sipFormLinearLayout)
    LinearLayout mSipFormLinearLayout;

    @BindView(R.id.alias)
    EditText mAliasView;

    @BindView(R.id.hostname)
    EditText mHostnameView;

    @BindView(R.id.username)
    EditText mUsernameView;

    @BindView(R.id.password)
    EditText mPasswordView;

    @BindView(R.id.login_form)
    ScrollView mSIPLoginForm;

    @BindView(R.id.create_sip_button)
    Button mCreateSIPAccountButton;

    @BindView(R.id.ring_password)
    EditText mRingPassword;

    @BindView(R.id.ring_password_repeat)
    EditText mRingPasswordRepeat;

    @BindView(R.id.ring_add_pin)
    EditText mRingPin;

    @BindView(R.id.ring_add_password)
    EditText mRingAddPassword;

    private void flipForm(boolean addAccount, boolean newAccount) {
        mAddAccountLayout.setVisibility(addAccount ? View.VISIBLE : View.GONE);
        mNewAccountLayout.setVisibility(newAccount ? View.VISIBLE : View.GONE);
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (newAccount) {
            mRingPassword.requestFocus();
            imm.showSoftInput(mRingPassword, InputMethodManager.SHOW_IMPLICIT);
        } else if (addAccount) {
            mRingPin.requestFocus();
            imm.showSoftInput(mRingPin, InputMethodManager.SHOW_IMPLICIT);
        }
        if (addAccount || newAccount) {
            mSipFormLinearLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View inflatedView = inflater.inflate(R.layout.frag_account_creation, parent, false);

        ButterKnife.bind(this, inflatedView);
        mSipFormLinearLayout.setVisibility(View.GONE);

        return inflatedView;
    }

    private boolean checkPassword(@NonNull TextView pwd, TextView confirm) {
        boolean error = false;
        if (pwd.getText().length() == 0) {
            error = true;
        } else {
            if (pwd.getText().length() < 6) {
                pwd.setError(getString(R.string.error_password_char_count));
                error = true;
            } else {
                pwd.setError(null);
            }
        }
        if (confirm != null) {
            if (!pwd.getText().toString().equals(confirm.getText().toString())) {
                confirm.setError(getString(R.string.error_passwords_not_equals));
                error = true;
            } else {
                confirm.setError(null);
            }
        }
        return error;
    }

    @OnEditorAction(R.id.password)
    @SuppressWarnings("unused")
    public boolean keyPressedOnPasswordField(TextView v, int actionId, KeyEvent event) {
        if (actionId == getResources().getInteger(R.integer.register_sip_account_actionid)
                || event == null
                || (event.getAction() == KeyEvent.ACTION_UP)) {
            mCreateSIPAccountButton.callOnClick();
        }
        return true;
    }
    /***********************
     * Ring Account Creation
     ***********************/
    @OnEditorAction(R.id.ring_password)
    @SuppressWarnings("unused")
    public boolean keyPressedOnRingPasswordField(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_NEXT) {
            return checkPassword(v, null);
        }
        return false;
    }

    @OnFocusChange(R.id.ring_password)
    @SuppressWarnings("unused")
    public void onFocusChangeOnPasswordField(TextView v, boolean hasFocus) {
        if (!hasFocus) {
            checkPassword(v, null);
        }
    }

    @OnEditorAction(R.id.ring_password_repeat)
    @SuppressWarnings("unused")
    public boolean keyPressedOnPasswordRepeatField(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            if (mRingPassword.getText().length() != 0 && !checkPassword(mRingPassword, v)) {
                createRingAccount();
                return true;
            }
        }
        return false;
    }

    @OnClick(R.id.ring_create_btn)
    public void createRingAccount() {
        if (mNewAccountLayout.getVisibility() == View.GONE) {
            flipForm(false, true);
        } else {
            if (!checkPassword(mRingPassword, mRingPasswordRepeat)) {
                mAccountType = AccountDetailBasic.ACCOUNT_TYPE_RING;
                mUsername = mAlias;
                initAccountCreation(null, null, mRingPassword.getText().toString());
            }
        }
    }
    /************************
     * Ring Account ADD
     ***********************/
    @OnEditorAction(R.id.ring_add_password)
    @SuppressWarnings("unused")
     public boolean keyPressedOnPasswordAddField(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            addRingAccount();
            return true;
        }
        return false;
    }

    @OnClick(R.id.ring_add_account)
    @SuppressWarnings("unused")
    public void addRingAccount() {
        if (mAddAccountLayout.getVisibility() == View.GONE) {
            flipForm(true, false);
        } else if (mRingPin.getText().length() != 0 && mRingAddPassword.getText().length() != 0) {
            mAccountType = AccountDetailBasic.ACCOUNT_TYPE_RING;
            mUsername = mAlias;
            initAccountCreation(null, mRingPin.getText().toString(), mRingAddPassword.getText().toString());
        }
    }
    /************************
     * SIP Account ADD
     ***********************/
    @OnClick(R.id.create_sip_button)
    public void createSIPAccount() {
        mAccountType = AccountDetailBasic.ACCOUNT_TYPE_SIP;
        mAlias = mAliasView.getText().toString();
        mHostname = mHostnameView.getText().toString();
        mUsername = mUsernameView.getText().toString();
        mPassword = mPasswordView.getText().toString();
        attemptCreation();
    }

    @OnClick(R.id.sipHeaderLinearLayout)
    void presentSIPForm() {
        if (mSipFormLinearLayout != null && mSipFormLinearLayout.getVisibility() != View.VISIBLE) {
            mSipFormLinearLayout.setVisibility(View.VISIBLE);
            //~ Let the time to perform setVisibility before scrolling.
            mSIPLoginForm.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mSIPLoginForm.fullScroll(ScrollView.FOCUS_DOWN);
                    mAliasView.requestFocus();
                }
            }, 100);
        }
    }

    @OnClick(R.id.sipHeaderLinearLayout)
    @SuppressWarnings("unused")
    public void onClickHeader(View v) {
        if (null != mSipFormLinearLayout) {
            if (mSipFormLinearLayout.getVisibility() != View.VISIBLE) {
                flipForm(false, false);
                mSipFormLinearLayout.setVisibility(View.VISIBLE);
                //~ Let the time to perform setVisibility before scrolling.
                mSIPLoginForm.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mSIPLoginForm.fullScroll(ScrollView.FOCUS_DOWN);
                        mAliasView.requestFocus();
                    }
                }, 100);
            }
        }
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     */
    @SuppressLint("NewApi")
    private static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private static String getDataColumn(Context context, Uri uri, String selection,
                                        String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int columnIndex = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            Log.e(TAG, "Can't find data column", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private void presentFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, FILE_SELECT_CODE);
    }

    public static long copy(Reader input, Writer output) throws IOException {
        char[] buffer = new char[8192];
        long count = 0;
        int n;
        while ((n = input.read(buffer)) != -1) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    private int getDocumentSize(Uri uri) {
        // The query, since it only applies to a single document, will only return
        // one row. There's no need to filter, sort, or select fields, since we want
        // all fields for one document.
        Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null, null);

        try {
            // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
            // "if there's anything to look at, look at it" conditionals.
            if (cursor != null && cursor.moveToFirst()) {

                // Note it's called "Display Name".  This is
                // provider-specific, and might not necessarily be the file name.
                //String displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                //Log.i(TAG, "Display Name: " + displayName);

                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                // If the size is unknown, the value stored is null.  But since an
                // int can't be null in Java, the behavior is implementation-specific,
                // which is just a fancy term for "unpredictable".  So as
                // a rule, check if it's null before assigning to an int.  This will
                // happen often:  The storage API allows for remote files, whose
                // size might not be locally known.
                String size = null;
                if (!cursor.isNull(sizeIndex)) {
                    // Technically the column stores an int, but cursor.getString()
                    // will do the conversion automatically.
                    size = cursor.getString(sizeIndex);
                    Log.d(TAG, "Size: " + size);
                    return cursor.getInt(sizeIndex);
                } else {
                    size = "Unknown";
                }
                Log.d(TAG, "Size: " + size);

            }
        } finally {
            cursor.close();
        }
        return 0;
    }

    private void readFromUri(Uri uri, String outPath) throws IOException {
        if (getDocumentSize(uri) > 16 * 1024 * 1024) {
            Toast.makeText(getActivity(), R.string.account_creation_file_too_big, Toast.LENGTH_LONG).show();
            throw new IOException("File is too big");
        }
        copy(new InputStreamReader(getActivity().getContentResolver().openInputStream(uri)), new FileWriter(outPath));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "onActivityResult " + data.getDataString());
                    this.mDataPath = getPath(getActivity(), data.getData());
                    if (TextUtils.isEmpty(this.mDataPath)) {
                        try {
                            this.mDataPath = getContext().getCacheDir().getPath() + "/temp.gz";
                            readFromUri(data.getData(), this.mDataPath);
                            showImportDialog();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(getActivity(), getContext().getString(R.string.account_cannot_read, data.getData()), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        showImportDialog();
                    }
                }
                break;
        }
    }

    private AlertDialog showImportDialog() {
        Activity ownerActivity = getActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(ownerActivity);
        LayoutInflater inflater = ownerActivity.getLayoutInflater();
        ViewGroup v = (ViewGroup) inflater.inflate(R.layout.dialog_account_import, null);
        final TextView pwd = (TextView) v.findViewById(R.id.pwd_txt);
        builder.setMessage(R.string.account_import_message)
                .setTitle(R.string.account_import_account)
                .setPositiveButton(R.string.account_import_account, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!TextUtils.isEmpty(mDataPath)) {
                            new ImportAccountTask().execute(mDataPath, pwd.getText().toString());
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                /* Terminate with no action */
                    }
                }).setView(v);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        return alertDialog;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_READ_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    presentFilePicker();
                } else {
                    Activity activity = getActivity();
                    if (null != activity) {
                        Toast.makeText(activity, R.string.permission_read_denied, Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    private class ImportAccountTask extends AsyncTask<String, Void, Integer> {
        private ProgressDialog loadingDialog = null;

        @Override
        protected void onPreExecute() {
            loadingDialog = ProgressDialog.show(getActivity(),
                    getActivity().getString(R.string.import_dialog_title),
                    getActivity().getString(R.string.import_export_wait), true);
        }

        protected Integer doInBackground(String... args) {
            int ret = 1;
            try {
                ret = mCallbacks.getRemoteService().importAccounts(args[0], args[1]);
            } catch (RemoteException e) {
                Log.e(TAG, "Error while importing account", e);
            }
            return ret;
        }

        protected void onPostExecute(Integer ret) {
            if (loadingDialog != null) {
                loadingDialog.dismiss();
            }
            if (ret == 0){
                getActivity().finish();
            } else {
                new AlertDialog.Builder(getActivity()).setTitle(R.string.import_failed_dialog_title)
                        .setMessage(R.string.import_failed_dialog_msg)
                        .setPositiveButton(android.R.string.ok, null).show();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.ab_account_creation);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof LocalService.Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (LocalService.Callbacks) activity;
    }

    /**
     * Attempts to sign in or register the account specified by the login form. If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptCreation() {
        // Reset errors.
        mAliasView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        boolean cancel = false;
        boolean warningIPAccount = false;
        View focusView = null;

        // Alias is mandatory
        if (TextUtils.isEmpty(mAlias)) {
            mAliasView.setError(getString(R.string.error_field_required));
            focusView = mAliasView;
            cancel = true;
        }

        if (TextUtils.isEmpty(mHostname)) {
            warningIPAccount = true;
        }

        if (!warningIPAccount && TextUtils.isEmpty(mPassword)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        if (!warningIPAccount && TextUtils.isEmpty(mUsername)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else if (warningIPAccount) {
            showIP2IPDialog();
        } else {
            initAccountCreation(null, null, null);
        }
    }

    private void showIP2IPDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_warn_ip2ip_account_title)
                .setMessage(R.string.dialog_warn_ip2ip_account_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        initAccountCreation(null, null, null);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        /* Terminate with no action */
                    }
                })
                .create().show();
    }

    @SuppressWarnings("unchecked")
    private void initAccountCreation(String newUsername, String pin, String password) {
        try {
            HashMap<String, String> accountDetails = (HashMap<String, String>) mCallbacks.getRemoteService().getAccountTemplate(mAccountType);
            accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_TYPE, mAccountType);
            //~ Checking the state of the Camera permission to enable Video or not.
            boolean hasCameraPermission = ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
            accountDetails.put(AccountDetailBasic.CONFIG_VIDEO_ENABLED, Boolean.toString(hasCameraPermission));

            //~ Sipinfo is forced for any sipaccount since overrtp is not supported yet.
            //~ This will have to be removed when it will be supported.
            accountDetails.put(AccountDetailAdvanced.CONFIG_ACCOUNT_DTMF_TYPE, getString(R.string.account_sip_dtmf_type_sipinfo));

            if (mAccountType.equals(AccountDetailBasic.ACCOUNT_TYPE_RING)) {
                accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS, "Ring");
                accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_HOSTNAME, "bootstrap.ring.cx");
                if (password != null && !password.isEmpty()) {
                    accountDetails.put(AccountDetailBasic.CONFIG_ARCHIVE_PASSWORD, password);
                }
                if (pin != null && !pin.isEmpty()) {
                    accountDetails.put(AccountDetailBasic.CONFIG_ARCHIVE_PIN, pin);
                }
                // Enable UPNP by default for Ring accounts
                accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_UPNP_ENABLE, AccountDetail.TRUE_STR);
                createNewAccount(accountDetails, mUsername);
            } else {
                accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS, mAlias);
                accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_HOSTNAME, mHostname);
                accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_USERNAME, mUsername);
                accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_PASSWORD, mPassword);
                createNewAccount(accountDetails, null);
            }

        } catch (RemoteException e) {
            Toast.makeText(getActivity(), R.string.account_creation_error, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Error while creating account", e);
        }

    }

    private class CreateAccountTask extends AsyncTask<HashMap<String, String>, Void, String> {
        private ProgressDialog progress = null;
        private final String username;

        CreateAccountTask(String regUsername) {
            Log.d(TAG, "CreateAccountTask ");
            username = regUsername;
        }

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(getActivity());
            progress.setTitle(R.string.dialog_wait_create);
            progress.setMessage(getString(R.string.dialog_wait_create_details));
            progress.setCancelable(false);
            progress.setCanceledOnTouchOutside(false);
            progress.show();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            mAccountTask = null;
        }

        @SafeVarargs
        @Override
        protected final String doInBackground(HashMap<String, String>... accounts) {
            final Account account = mCallbacks.getService().createAccount(accounts[0]);
            account.stateListener = new Account.OnStateChangedListener() {
                @Override
                public void stateChanged(String state, int code) {
                    if (!AccountDetailVolatile.STATE_INITIALIZING.contentEquals(state)) {
                        account.stateListener = null;
                        if (progress != null) {
                            progress.dismiss();
                            progress = null;
                        }
                        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
                        dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //do things
                            }
                        });
                        boolean success = false;
                        switch (state) {
                            case AccountDetailVolatile.STATE_ERROR_GENERIC:
                                dialog.setTitle(R.string.account_cannot_be_found_title)
                                        .setMessage(R.string.account_cannot_be_found_message);
                                break;
                            case AccountDetailVolatile.STATE_ERROR_NETWORK:
                                dialog.setTitle(R.string.account_no_network_title)
                                        .setMessage(R.string.account_no_network_message);
                                break;
                            default:
                                dialog.setTitle(R.string.account_device_added_title)
                                        .setMessage(R.string.account_device_added_message);
                                success = true;
                                break;
                        }
                        AlertDialog d = dialog.show();
                        if (success) {
                            d.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialogInterface) {
                                    getActivity().setResult(Activity.RESULT_OK, new Intent());
                                    getActivity().finish();
                                }
                            });
                        }
                    }
                }
            };
            Log.d(TAG, "Account created, registering " + username);
            return account.getAccountID();
        }
    }

    private void createNewAccount(HashMap<String, String> accountDetails, String registerName) {
        if (mAccountTask != null) {
            return;
        }

        //noinspection unchecked
        mAccountTask = new CreateAccountTask(registerName);
        mAccountTask.execute(accountDetails);
    }
}
