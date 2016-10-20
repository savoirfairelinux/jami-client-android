/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
 *          Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *          Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v14.preference.PreferenceFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import cx.ring.R;
import cx.ring.interfaces.AccountCallbacks;
import cx.ring.interfaces.AccountChangedListener;
import cx.ring.model.Codec;
import cx.ring.model.account.Account;
import cx.ring.model.account.AccountDetail;
import cx.ring.model.account.AccountDetailAdvanced;
import cx.ring.model.account.AccountDetailBasic;
import cx.ring.service.LocalService;
import cx.ring.utils.FileUtils;

import static cx.ring.client.AccountEditionActivity.DUMMY_CALLBACKS;


public class MediaPreferenceFragment extends PreferenceFragment
        implements FragmentCompat.OnRequestPermissionsResultCallback, AccountChangedListener {
    static final String TAG = MediaPreferenceFragment.class.getSimpleName();

    private CodecPreference audioCodecsPref = null;
    private CodecPreference videoCodecsPref = null;

    protected AccountCallbacks mCallbacks = DUMMY_CALLBACKS;

    private int MAX_SIZE_RINGTONE = 800;

    private static final int SELECT_RINGTONE_PATH = 40;
    private Preference.OnPreferenceClickListener filePickerListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            performFileSearch(SELECT_RINGTONE_PATH);
            return true;
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof AccountCallbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (AccountCallbacks) activity;
        mCallbacks.addOnAccountChanged(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mCallbacks != null) {
            mCallbacks.removeOnAccountChanged(this);
        }
        mCallbacks = DUMMY_CALLBACKS;
    }

    @Override
    public void accountChanged(Account acc) {
        setPreferenceDetails(acc.getBasicDetails());
        setPreferenceDetails(acc.getAdvancedDetails());
        addPreferenceListener(acc.getAdvancedDetails(), changeAudioPreferenceListener);
        final ArrayList<Codec> audioCodec = new ArrayList<>();
        final ArrayList<Codec> videoCodec = new ArrayList<>();
        try {
            final ArrayList<Codec> codecList = ((ArrayList<Codec>) mCallbacks.getRemoteService().getCodecList(acc.getAccountID()));
            for (Codec codec : codecList) {
                if (codec.getType() == Codec.Type.AUDIO)
                    audioCodec.add(codec);
                else if (codec.getType() == Codec.Type.VIDEO)
                    videoCodec.add(codec);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in accountChanged", e);
        }

        audioCodecsPref.setCodecs(audioCodec);
        audioCodecsPref.setOnPreferenceChangeListener(changeCodecListener);

        videoCodecsPref.setCodecs(videoCodec);
        videoCodecsPref.setOnPreferenceChangeListener(changeCodecListener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_CANCELED) {
            return;
        }

        String path = FileUtils.getRealPathFromURI(getActivity(), data.getData());
        File myFile = new File(path);
        Log.i(TAG, "file selected: " + myFile.getAbsolutePath());
        if (requestCode == SELECT_RINGTONE_PATH) {
            String type = getActivity().getContentResolver().getType(data.getData());
            if ("audio/mpeg3".equals(type) || "audio/x-mpeg-3".equals(type) || "audio/mpeg".equals(type) || "audio/x-mpeg".equals(type)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.ringtone_error_title);
                builder.setMessage(R.string.ringtone_error_format_not_supported);
                builder.show();
                Log.d(TAG, "The extension file is not supported");
            } else if (myFile.length() / 1024 > MAX_SIZE_RINGTONE) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.ringtone_error_title);
                builder.setMessage(getString(R.string.ringtone_error_size_too_big, MAX_SIZE_RINGTONE));
                builder.show();
                Log.d(TAG, "The file is too big " + myFile.length() / 1024);
            } else {
                findPreference(AccountDetailAdvanced.CONFIG_RINGTONE_PATH).setSummary(myFile.getName());
                mCallbacks.getAccount().getAdvancedDetails().setDetailString(AccountDetailAdvanced.CONFIG_RINGTONE_PATH, myFile.getAbsolutePath());
                mCallbacks.getAccount().notifyObservers();
            }
        }
    }

    public void performFileSearch(int requestCodeToSet) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        startActivityForResult(intent, requestCodeToSet);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.account_media_prefs);
        audioCodecsPref = (CodecPreference) findPreference("Account.audioCodecs");
        videoCodecsPref = (CodecPreference) findPreference("Account.videoCodecs");
        boolean isChecked = Boolean.valueOf(mCallbacks.getAccount().getAdvancedDetails().getDetailString(AccountDetailAdvanced.CONFIG_RINGTONE_ENABLED));
        findPreference(AccountDetailAdvanced.CONFIG_RINGTONE_PATH).setEnabled(isChecked);

        addPreferenceListener(AccountDetailBasic.CONFIG_VIDEO_ENABLED, changeVideoPreferenceListener);
        final Account acc = mCallbacks.getAccount();
        if (acc != null) {
            accountChanged(acc);
        }
    }

    private final Preference.OnPreferenceChangeListener changeCodecListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            final Account acc = mCallbacks.getAccount();
            ArrayList<Long> audio = audioCodecsPref.getActiveCodecList();
            ArrayList<Long> video = videoCodecsPref.getActiveCodecList();
            ArrayList<Long> newOrder = new ArrayList<>(audio.size() + video.size());
            newOrder.addAll(audio);
            newOrder.addAll(video);
            try {
                mCallbacks.getRemoteService().setActiveCodecList(newOrder, acc.getAccountID());
            } catch (RemoteException e) {
                Log.e(TAG, "Error while setting active codecs", e);
            }
            acc.notifyObservers();
            return true;
        }
    };

    private final Preference.OnPreferenceChangeListener changeAudioPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final Account account = mCallbacks.getAccount();
            String key = preference.getKey();
            if (preference instanceof TwoStatePreference) {
                if (key.contentEquals(AccountDetailAdvanced.CONFIG_RINGTONE_ENABLED)) {
                    getPreferenceScreen().findPreference(AccountDetailAdvanced.CONFIG_RINGTONE_PATH).setEnabled((Boolean) newValue);
                }
                account.getAdvancedDetails().setDetailString(key, newValue.toString());
            } else if (key.contentEquals(AccountDetailAdvanced.CONFIG_ACCOUNT_DTMF_TYPE)) {
                preference.setSummary(((String) newValue).contentEquals("overrtp") ? "RTP" : "SIP");
            } else {
                preference.setSummary((CharSequence) newValue);
                Log.i(TAG, "Changing" + key + " value:" + newValue);
                account.getAdvancedDetails().setDetailString(key, newValue.toString());
            }
            account.notifyObservers();

            return true;
        }
    };

    private final Preference.OnPreferenceChangeListener changeVideoPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final Account account = mCallbacks.getAccount();
            if (null != account && newValue instanceof Boolean) {
                if (newValue.equals(true)) {
                    boolean hasCameraPermission = ContextCompat.checkSelfPermission(getActivity(),
                            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
                    if (hasCameraPermission) {
                        if (preference instanceof TwoStatePreference) {
                            account.getBasicDetails().setDetailString(preference.getKey(), newValue.toString());
                        }
                        account.notifyObservers();
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    LocalService.PERMISSIONS_REQUEST);
                        } else {
                            if (preference instanceof TwoStatePreference) {
                                account.getBasicDetails().setDetailString(preference.getKey(), newValue.toString());
                            }
                            account.notifyObservers();
                        }
                    }
                } else {
                    if (preference instanceof TwoStatePreference) {
                        account.getBasicDetails().setDetailString(preference.getKey(), newValue.toString());
                    }
                    account.notifyObservers();
                }
            }
            return true;
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0, n = permissions.length; i < n; i++) {
            switch (permissions[i]) {
                case Manifest.permission.CAMERA:
                    boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                    final Account account = mCallbacks.getAccount();
                    if (account != null) {
                        account.getBasicDetails().setDetailString(AccountDetailBasic.CONFIG_VIDEO_ENABLED, Boolean.toString(granted));
                        account.notifyObservers();
                    }
                    refresh();
                    if (!granted) {
                        this.presentCameraPermissionDeniedDialog();
                    }
                    break;
            }
        }
    }

    private void setPreferenceDetails(AccountDetail details) {
        for (AccountDetail.PreferenceEntry preferenceEntry : details.getDetailValues()) {
            Preference pref = findPreference(preferenceEntry.mKey);
            if (pref != null) {
                if (pref instanceof TwoStatePreference) {
                    ((TwoStatePreference) pref).setChecked(preferenceEntry.mValue.contentEquals(AccountDetail.TRUE_STR));
                } else if (preferenceEntry.mKey.contentEquals(AccountDetailAdvanced.CONFIG_ACCOUNT_DTMF_TYPE)) {
                    pref.setDefaultValue(preferenceEntry.mValue.contentEquals("overrtp") ? "RTP" : "SIP");
                    pref.setSummary(preferenceEntry.mValue.contentEquals("overrtp") ? "RTP" : "SIP");
                } else if (preferenceEntry.mKey.contentEquals(AccountDetailAdvanced.CONFIG_RINGTONE_PATH)) {
                    File tmp = new File(preferenceEntry.mValue);
                    pref.setSummary(tmp.getName());
                } else
                    pref.setSummary(preferenceEntry.mValue);
            }
        }
    }

    private void addPreferenceListener(AccountDetail details, Preference.OnPreferenceChangeListener listener) {
        for (AccountDetail.PreferenceEntry p : details.getDetailValues()) {
            addPreferenceListener(p.mKey, listener);
        }
    }

    private void addPreferenceListener(String key, Preference.OnPreferenceChangeListener listener) {
        Preference pref = findPreference(key);
        if (pref != null) {
            pref.setOnPreferenceChangeListener(listener);
            if (key.contentEquals(AccountDetailAdvanced.CONFIG_RINGTONE_PATH)) {
                pref.setOnPreferenceClickListener(filePickerListener);
            }
        }
    }

    public void refresh() {
        final Account account = mCallbacks.getAccount();
        if (account != null) {
            setPreferenceDetails(account.getBasicDetails());
            account.notifyObservers();
        }
        if (null != getListView() && null != getListView().getAdapter()) {
            getListView().getAdapter().notifyDataSetChanged();
        }
        if (null != videoCodecsPref) {
            videoCodecsPref.refresh();
        }
    }

    private void presentCameraPermissionDeniedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.permission_dialog_camera_title)
                .setMessage(R.string.permission_dialog_camera_message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.show();
    }
}
