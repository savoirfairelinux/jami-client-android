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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v14.preference.PreferenceFragment;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.account.AccountEditionActivity;
import cx.ring.application.RingApplication;
import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.model.Codec;
import cx.ring.model.ConfigKey;
import cx.ring.model.ServiceEvent;
import cx.ring.services.AccountService;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.utils.FileUtils;
import cx.ring.utils.Observable;
import cx.ring.utils.Observer;


public class MediaPreferenceFragment extends PreferenceFragment
        implements FragmentCompat.OnRequestPermissionsResultCallback, Observer<ServiceEvent> {

    static final String TAG = MediaPreferenceFragment.class.getSimpleName();

    @Inject
    AccountService mAccountService;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    private CodecPreference audioCodecsPref = null;
    private CodecPreference videoCodecsPref = null;
    private SwitchPreference mRingtoneCustom = null;

    private int MAX_SIZE_RINGTONE = 800;

    private static final int SELECT_RINGTONE_PATH = 40;
    private Preference.OnPreferenceClickListener filePickerListener = preference -> {
        performFileSearch(SELECT_RINGTONE_PATH);
        return true;
    };
    private String mAccountID;

    @Override
    public void onResume() {
        super.onResume();
        if (getArguments() == null || getArguments().getString(AccountEditionActivity.ACCOUNT_ID_KEY) == null) {
            return;
        }
        mAccountID = getArguments().getString(AccountEditionActivity.ACCOUNT_ID_KEY);
        boolean isRingtoneEnabled = Boolean.valueOf(mAccountService.getAccount(mAccountID).getDetail(ConfigKey.RINGTONE_ENABLED));
        mRingtoneCustom.setEnabled(isRingtoneEnabled);
        boolean isCustomRingtoneEnabled = isRingtoneEnabled && mRingtoneCustom.isChecked();
        findPreference(ConfigKey.RINGTONE_PATH.key()).setEnabled(isCustomRingtoneEnabled);

        addPreferenceListener(ConfigKey.VIDEO_ENABLED, changeVideoPreferenceListener);
        mRingtoneCustom.setOnPreferenceChangeListener(changeAudioPreferenceListener);
        final Account acc = mAccountService.getAccount(mAccountID);
        if (acc != null) {
            accountChanged(acc);
        }
        mAccountService.addObserver(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mAccountService.removeObserver(this);
    }

    private void accountChanged(Account account) {
        if (account == null) {
            return;
        }
        setPreferenceDetails(account.getConfig());
        final ArrayList<Codec> audioCodec = new ArrayList<>();
        final ArrayList<Codec> videoCodec = new ArrayList<>();
        try {
            final List<Codec> codecList = mAccountService.getCodecList(account.getAccountID());
            for (Codec codec : codecList) {
                if (codec.getType() == Codec.Type.AUDIO) {
                    audioCodec.add(codec);
                } else if (codec.getType() == Codec.Type.VIDEO) {
                    videoCodec.add(codec);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in accountChanged", e);
        }

        audioCodecsPref.setCodecs(audioCodec);
        videoCodecsPref.setCodecs(videoCodec);

        addPreferenceListener(account.getConfig(), changeAudioPreferenceListener);
        audioCodecsPref.setOnPreferenceChangeListener(changeCodecListener);
        videoCodecsPref.setOnPreferenceChangeListener(changeCodecListener);
        mRingtoneCustom.setOnPreferenceChangeListener(changeAudioPreferenceListener);
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
                builder.setPositiveButton(android.R.string.ok, null);
                builder.show();
                Log.d(TAG, "The extension file is not supported");
            } else if (myFile.length() / 1024 > MAX_SIZE_RINGTONE) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.ringtone_error_title);
                builder.setMessage(getString(R.string.ringtone_error_size_too_big, MAX_SIZE_RINGTONE));
                builder.setPositiveButton(android.R.string.ok, null);
                builder.show();
                Log.d(TAG, "The file is too big " + myFile.length() / 1024);
            } else {
                setRingtonepath(myFile);
            }
        }
    }

    private void setRingtonepath(File file) {
        findPreference(ConfigKey.RINGTONE_PATH.key()).setSummary(file.getName());
        mAccountService.getAccount(mAccountID).setDetail(ConfigKey.RINGTONE_PATH, file.getAbsolutePath());
        mAccountService.setCredentials(mAccountID, mAccountService.getAccount(mAccountID).getCredentialsHashMapList());
        mAccountService.setAccountDetails(mAccountID, mAccountService.getAccount(mAccountID).getDetails());
    }

    public void performFileSearch(int requestCodeToSet) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        startActivityForResult(intent, requestCodeToSet);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        // dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        Log.d(TAG, "onCreatePreferences");
        addPreferencesFromResource(R.xml.account_media_prefs);
        audioCodecsPref = (CodecPreference) findPreference("Account.audioCodecs");
        videoCodecsPref = (CodecPreference) findPreference("Account.videoCodecs");
        mRingtoneCustom = (SwitchPreference) findPreference("Account.ringtoneCustom");
    }

    private final Preference.OnPreferenceChangeListener changeCodecListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            final Account account = mAccountService.getAccount(mAccountID);
            ArrayList<Long> audio = audioCodecsPref.getActiveCodecList();
            ArrayList<Long> video = videoCodecsPref.getActiveCodecList();
            ArrayList<Long> newOrder = new ArrayList<>(audio.size() + video.size());
            newOrder.addAll(audio);
            newOrder.addAll(video);
            mAccountService.setActiveCodecList(newOrder, account.getAccountID());
            mAccountService.setCredentials(mAccountID, mAccountService.getAccount(mAccountID).getCredentialsHashMapList());
            mAccountService.setAccountDetails(mAccountID, mAccountService.getAccount(mAccountID).getDetails());
            accountChanged(account);
            return true;
        }
    };

    private final Preference.OnPreferenceChangeListener changeAudioPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final Account account = mAccountService.getAccount(mAccountID);
            final ConfigKey key = ConfigKey.fromString(preference.getKey());
            if (preference instanceof TwoStatePreference) {
                if (key == ConfigKey.RINGTONE_ENABLED) {
                    mRingtoneCustom.setEnabled((Boolean) newValue);
                    Boolean isEnabled = (Boolean) newValue && mRingtoneCustom.isChecked();
                    getPreferenceScreen().findPreference(ConfigKey.RINGTONE_PATH.key()).setEnabled(isEnabled);
                } else if (preference == mRingtoneCustom) {
                    getPreferenceScreen().findPreference(ConfigKey.RINGTONE_PATH.key()).setEnabled((Boolean) newValue);
                    if (newValue.toString().contentEquals("false")) {
                        setRingtonepath(new File(FileUtils.ringtonesPath(getActivity()) + File.separator + "default.wav"));
                    }
                }
                if (key != null) {
                    account.setDetail(key, newValue.toString());
                }
            } else if (key == ConfigKey.ACCOUNT_DTMF_TYPE) {
                preference.setSummary(((String) newValue).contentEquals("overrtp") ? "RTP" : "SIP");
            } else {
                preference.setSummary((CharSequence) newValue);
                Log.i(TAG, "Changing" + key + " value:" + newValue);
                account.setDetail(key, newValue.toString());
            }

            mAccountService.setCredentials(mAccountID, mAccountService.getAccount(mAccountID).getCredentialsHashMapList());
            mAccountService.setAccountDetails(mAccountID, mAccountService.getAccount(mAccountID).getDetails());
            return true;
        }
    };

    private final Preference.OnPreferenceChangeListener changeVideoPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final Account account = mAccountService.getAccount(mAccountID);
            final ConfigKey key = ConfigKey.fromString(preference.getKey());
            if (null != account && newValue instanceof Boolean) {
                if (newValue.equals(true)) {
                    boolean hasCameraPermission = mDeviceRuntimeService.hasVideoPermission();
                    if (hasCameraPermission) {
                        if (preference instanceof TwoStatePreference) {
                            account.setDetail(key, newValue.toString());

                            mAccountService.setCredentials(mAccountID, mAccountService.getAccount(mAccountID).getCredentialsHashMapList());
                            mAccountService.setAccountDetails(mAccountID, mAccountService.getAccount(mAccountID).getDetails());
                        }
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.CAMERA}, RingApplication.PERMISSIONS_REQUEST);
                        } else if (preference instanceof TwoStatePreference) {
                            account.setDetail(key, newValue.toString());

                            mAccountService.setCredentials(mAccountID, mAccountService.getAccount(mAccountID).getCredentialsHashMapList());
                            mAccountService.setAccountDetails(mAccountID, mAccountService.getAccount(mAccountID).getDetails());
                        }
                    }
                } else if (preference instanceof TwoStatePreference) {
                    account.setDetail(key, newValue.toString());

                    mAccountService.setCredentials(mAccountID, mAccountService.getAccount(mAccountID).getCredentialsHashMapList());
                    mAccountService.setAccountDetails(mAccountID, mAccountService.getAccount(mAccountID).getDetails());
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
                    final Account account = mAccountService.getAccount(mAccountID);
                    if (account != null) {
                        account.setDetail(ConfigKey.VIDEO_ENABLED, granted);

                        mAccountService.setCredentials(mAccountID, mAccountService.getAccount(mAccountID).getCredentialsHashMapList());
                        mAccountService.setAccountDetails(mAccountID, mAccountService.getAccount(mAccountID).getDetails());
                    }
                    refresh();
                    if (!granted) {
                        this.presentCameraPermissionDeniedDialog();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void setPreferenceDetails(AccountConfig details) {
        for (ConfigKey confKey : details.getKeys()) {
            Preference pref = findPreference(confKey.key());

            if (pref != null) {
                if (pref instanceof TwoStatePreference) {
                    ((TwoStatePreference) pref).setChecked(details.getBool(confKey));
                } else if (confKey == ConfigKey.ACCOUNT_DTMF_TYPE) {
                    pref.setDefaultValue(details.get(confKey).contentEquals("overrtp") ? "RTP" : "SIP");
                    pref.setSummary(details.get(confKey).contentEquals("overrtp") ? "RTP" : "SIP");
                } else if (confKey == ConfigKey.RINGTONE_PATH) {
                    File tmp = new File(details.get(confKey));
                    pref.setSummary(tmp.getName());
                } else {
                    pref.setSummary(details.get(confKey));
                }
            }
        }
    }

    private void addPreferenceListener(AccountConfig details, Preference.OnPreferenceChangeListener listener) {
        for (ConfigKey confKey : details.getKeys())
            addPreferenceListener(confKey, listener);
    }

    private void addPreferenceListener(ConfigKey key, Preference.OnPreferenceChangeListener listener) {
        Preference pref = findPreference(key.key());
        if (pref != null) {
            pref.setOnPreferenceChangeListener(listener);
            if (key == ConfigKey.RINGTONE_PATH)
                pref.setOnPreferenceClickListener(filePickerListener);
        }
    }

    public void refresh() {
        final Account account = mAccountService.getAccount(mAccountID);
        if (account != null) {
            setPreferenceDetails(account.getConfig());
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
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Override
    public void update(Observable observable, ServiceEvent event) {
        if (event == null || getView() == null) {
            return;
        }

        switch (event.getEventType()) {
            case ACCOUNTS_CHANGED:
            case REGISTRATION_STATE_CHANGED:
                accountChanged(mAccountService.getAccount(mAccountID));
                break;
            default:
                break;
        }
    }
}
