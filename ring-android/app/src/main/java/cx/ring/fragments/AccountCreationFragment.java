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
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.util.HashMap;

import cx.ring.R;
import cx.ring.client.HomeActivity;
import cx.ring.model.account.AccountDetailBasic;
import cx.ring.service.LocalService;

public class AccountCreationFragment extends Fragment {
    static final String TAG = AccountCreationFragment.class.getSimpleName();

    static private final int FILE_SELECT_CODE = 2;
    private static final int REQUEST_READ_STORAGE = 113;

    // Values for email and password at the time of the login attempt.
    private String mAlias;
    private String mHostname;
    private String mUsername;
    private String mPassword;
    private String mAccountType;

    // UI references.
    private EditText mAliasView;
    private EditText mHostnameView;
    private EditText mUsernameView;
    private EditText mPasswordView;

    private String mDataPath;

    private LocalService.Callbacks mCallbacks = LocalService.DUMMY_CALLBACKS;
    private boolean creatingAccount = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_account_creation, parent, false);

        mAliasView = (EditText) inflatedView.findViewById(R.id.alias);
        mHostnameView = (EditText) inflatedView.findViewById(R.id.hostname);
        mUsernameView = (EditText) inflatedView.findViewById(R.id.username);
        mPasswordView = (EditText) inflatedView.findViewById(R.id.password);

        mPasswordView.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                mAccountType = "SIP";
                mAlias = mAliasView.getText().toString();
                mHostname = mHostnameView.getText().toString();
                mUsername = mUsernameView.getText().toString();
                mPassword = mPasswordView.getText().toString();
                attemptCreation();
                return true;
            }
        });
        inflatedView.findViewById(R.id.create_ring_account).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAccountType = "RING";
                initCreation();
            }
        });
        inflatedView.findViewById(R.id.create_sip_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAccountType = "SIP";
                mAlias = mAliasView.getText().toString();
                mHostname = mHostnameView.getText().toString();
                mUsername = mUsernameView.getText().toString();
                mPassword = mPasswordView.getText().toString();
                attemptCreation();
            }
        });
        inflatedView.findViewById(R.id.select_file_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startImport();
            }
        });

        return inflatedView;
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
    public static String getPath(final Context context, final Uri uri) {

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
    public static String getDataColumn(Context context, Uri uri, String selection,
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
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } catch (Exception e) {
            Log.w(TAG, "Can't find data column", e);
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private void presentFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, FILE_SELECT_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    Log.w(TAG, "onActivityResult " + data.getDataString());
                    this.mDataPath = getPath(getActivity(), data.getData());
                    if (TextUtils.isEmpty(this.mDataPath))
                        Toast.makeText(getActivity(), "Can't read " + data.getData(), Toast.LENGTH_LONG).show();
                    else
                        showImportDialog();
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

    private void startImport() {
        Activity activity = getActivity();
        if (null != activity) {
            boolean hasPermission = (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (hasPermission) {
                presentFilePicker();
            } else {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_STORAGE);
            }
        }
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
        private ProgressDialog loading_dialog = null;

        @Override
        protected void onPreExecute() {
            loading_dialog = ProgressDialog.show(getActivity(),
                    getActivity().getString(R.string.import_dialog_title),
                    getActivity().getString(R.string.import_export_wait), true);
        }

        protected Integer doInBackground(String... args) {
            int ret = 1;
            try {
                ret = mCallbacks.getRemoteService().importAccounts(args[0], args[1]);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return ret;
        }

        protected void onPostExecute(Integer ret) {
            if (loading_dialog != null)
                loading_dialog.dismiss();
            if (ret == 0)
                getActivity().finish();
            else
                new AlertDialog.Builder(getActivity()).setTitle(R.string.import_failed_dialog_title)
                        .setMessage(R.string.import_failed_dialog_msg)
                        .setPositiveButton(android.R.string.ok, null).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
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
    public void attemptCreation() {

        // Reset errors.
        mAliasView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if (TextUtils.isEmpty(mPassword)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        if (TextUtils.isEmpty(mUsername)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        }

        if (TextUtils.isEmpty(mHostname)) {
            mHostnameView.setError(getString(R.string.error_field_required));
            focusView = mHostnameView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(mAlias)) {
            mAliasView.setError(getString(R.string.error_field_required));
            focusView = mAliasView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            initCreation();

        }
    }

    @SuppressWarnings("unchecked")
    private void initCreation() {
        try {
            HashMap<String, String> accountDetails = (HashMap<String, String>) mCallbacks.getRemoteService().getAccountTemplate(mAccountType);
            accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_TYPE, mAccountType);
            accountDetails.put(AccountDetailBasic.CONFIG_VIDEO_ENABLED, "true");
            if (mAccountType.equals("RING")) {
                accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS, "Ring");
                accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_HOSTNAME, "bootstrap.ring.cx");
            } else {
                accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_ALIAS, mAlias);
                accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_HOSTNAME, mHostname);
                accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_USERNAME, mUsername);
                accountDetails.put(AccountDetailBasic.CONFIG_ACCOUNT_PASSWORD, mPassword);
            }

            createNewAccount(accountDetails);

        } catch (RemoteException e) {
            Toast.makeText(getActivity(), "Error creating account", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

    }

    class CreateAccountTask extends AsyncTask<HashMap<String, String>, Void, String> {
        private ProgressDialog progress = null;

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(getActivity());
            progress.setTitle(R.string.dialog_wait_create);
            progress.setMessage(getString(R.string.dialog_wait_create_details));
            progress.setCancelable(false);
            progress.setCanceledOnTouchOutside(false);
            progress.show();
        }

        @SafeVarargs
        @Override
        protected final String doInBackground(HashMap<String, String>... accs) {
            try {
                return mCallbacks.getRemoteService().addAccount(accs[0]);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            if (progress != null) {
                progress.dismiss();
                progress = null;
            }
            Intent resultIntent = new Intent(getActivity(), HomeActivity.class);
            getActivity().setResult(s.isEmpty() ? Activity.RESULT_CANCELED : Activity.RESULT_OK, resultIntent);
            getActivity().finish();
        }
    }

    private void createNewAccount(HashMap<String, String> accountDetails) {
        if (creatingAccount)
            return;
        creatingAccount = true;

        //noinspection unchecked
        new CreateAccountTask().execute(accountDetails);
    }

}
