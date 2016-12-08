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
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.client.AccountWizard;
import cx.ring.services.AccountService;

public class AccountCreationFragment extends Fragment {
    static final String TAG = AccountCreationFragment.class.getSimpleName();

    private static final int FILE_SELECT_CODE = 2;
    private static final int REQUEST_READ_STORAGE = 113;

    @Inject
    AccountService mAccountService;

    // Values for email and password at the time of the login attempt.
    private String mAlias;
    private String mHostname;
    private String mUsername;
    private String mPassword;

    private String mDataPath;

    @BindView(R.id.alias)
    EditText mAliasView;

    @BindView(R.id.hostname)
    EditText mHostnameView;

    @BindView(R.id.username)
    EditText mUsernameView;

    @BindView(R.id.password)
    EditText mPasswordView;

    @BindView(R.id.create_sip_button)
    Button mCreateSIPAccountButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View inflatedView = inflater.inflate(R.layout.frag_acc_sip_create, parent, false);

        ButterKnife.bind(this, inflatedView);

        // dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        return inflatedView;
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

    /************************
     * SIP Account ADD
     ***********************/
    @OnClick(R.id.create_sip_button)
    public void createSIPAccount() {
        mAlias = mAliasView.getText().toString();
        mHostname = mHostnameView.getText().toString();
        mUsername = mUsernameView.getText().toString();
        mPassword = mPasswordView.getText().toString();
        attemptCreation();
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
                            this.mDataPath = getActivity().getCacheDir().getPath() + "/temp.gz";
                            readFromUri(data.getData(), this.mDataPath);
                            showRestoreDialog();
                        } catch (IOException e) {
                            Log.e(TAG, "Exception reading file", e);
                            Toast.makeText(getActivity(), getActivity().getString(R.string.account_cannot_read, data.getData()), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        showRestoreDialog();
                    }
                }
                break;
        }
    }

    private AlertDialog showRestoreDialog() {
        Activity ownerActivity = getActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(ownerActivity);
        LayoutInflater inflater = ownerActivity.getLayoutInflater();
        ViewGroup v = (ViewGroup) inflater.inflate(R.layout.dialog_account_restore, null);
        final TextView pwd = (TextView) v.findViewById(R.id.pwd_txt);
        builder.setMessage(R.string.account_restore_message)
                .setTitle(R.string.account_restore_account)
                .setPositiveButton(R.string.account_restore_account, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!TextUtils.isEmpty(mDataPath)) {
                            new RestoreAccountTask().execute(mDataPath, pwd.getText().toString());
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

    private class RestoreAccountTask extends AsyncTask<String, Void, Integer> {
        private ProgressDialog loadingDialog = null;

        @Override
        protected void onPreExecute() {
            loadingDialog = ProgressDialog.show(getActivity(),
                    getActivity().getString(R.string.restore_dialog_title),
                    getActivity().getString(R.string.restore_backup_wait), true);
        }

        protected Integer doInBackground(String... args) {
            return mAccountService.restoreAccounts(args[0], args[1]);
        }

        protected void onPostExecute(Integer ret) {
            if (loadingDialog != null) {
                loadingDialog.dismiss();
            }
            if (ret == 0) {
                getActivity().finish();
            } else {
                new AlertDialog.Builder(getActivity()).setTitle(R.string.restore_failed_dialog_title)
                        .setMessage(R.string.restore_failed_dialog_msg)
                        .setPositiveButton(android.R.string.ok, null).show();
            }
        }
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
            AccountWizard accountWizard = (AccountWizard) getActivity();
            if (accountWizard != null) {
                accountWizard.initSipAccountCreation(mAliasView.getText().toString(), mUsernameView.getText().toString(), mPasswordView.getText().toString(), mHostnameView.getText().toString());
            }
        }
    }

    private void showIP2IPDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_warn_ip2ip_account_title)
                .setMessage(R.string.dialog_warn_ip2ip_account_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        AccountWizard a = (AccountWizard) getActivity();
                        if (a != null)
                            a.initSipAccountCreation(mAliasView.getText().toString(), null, null, null);
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
}
