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
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.interfaces.AccountCallbacks;
import cx.ring.interfaces.AccountChangedListener;
import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.model.Codec;
import cx.ring.model.ConfigKey;
import cx.ring.service.LocalService;
import cx.ring.services.AccountService;
import cx.ring.utils.FileUtils;

import static cx.ring.client.AccountEditionActivity.DUMMY_CALLBACKS;


public class MediaPreferenceFragment extends PreferenceFragment
        implements FragmentCompat.OnRequestPermissionsResultCallback, AccountChangedListener {
    static final String TAG = MediaPreferenceFragment.class.getSimpleName();

    @Inject
    AccountService mAccountService;

    private CodecPreference audioCodecsPref = null;
    private CodecPreference videoCodecsPref = null;
    private SwitchPreference mRingtoneCustom = null;
    //private boolean mIsRefresh = false;

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
    public void accountChanged(Account account) {
        Log.d(TAG, "accountChanged");
        //mIsRefresh = true;
        setPreferenceDetails(account.getConfig());
        addPreferenceListener(account.getConfig(), changeAudioPreferenceListener);
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
        audioCodecsPref.setOnPreferenceChangeListener(changeCodecListener);

        videoCodecsPref.setCodecs(videoCodec);
        videoCodecsPref.setOnPreferenceChangeListener(changeCodecListener);

        mRingtoneCustom.setOnPreferenceChangeListener(changeAudioPreferenceListener);
        //mIsRefresh = false;
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
        mCallbacks.getAccount().setDetail(ConfigKey.RINGTONE_PATH, file.getAbsolutePath());
        //if (!mIsRefresh) {
        mCallbacks.saveAccount();
        //}
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

        boolean isRingtoneEnabled = Boolean.valueOf(mCallbacks.getAccount().getDetail(ConfigKey.RINGTONE_ENABLED));
        mRingtoneCustom.setEnabled(isRingtoneEnabled);
        boolean isCustomRingtoneEnabled = isRingtoneEnabled && mRingtoneCustom.isChecked();
        findPreference(ConfigKey.RINGTONE_PATH.key()).setEnabled(isCustomRingtoneEnabled);

        addPreferenceListener(ConfigKey.VIDEO_ENABLED, changeVideoPreferenceListener);
        mRingtoneCustom.setOnPreferenceChangeListener(changeAudioPreferenceListener);
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
            mAccountService.setActiveCodecList(newOrder, acc.getAccountID());

            //if (!mIsRefresh) {
            mCallbacks.saveAccount();
            //}
            return true;
        }
    };

    private final Preference.OnPreferenceChangeListener changeAudioPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final Account account = mCallbacks.getAccount();
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
            //if (!mIsRefresh) {
            mCallbacks.saveAccount();
            //}
            return true;
        }
    };

    private final Preference.OnPreferenceChangeListener changeVideoPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final Account account = mCallbacks.getAccount();
            final ConfigKey key = ConfigKey.fromString(preference.getKey());
            if (null != account && newValue instanceof Boolean) {
                if (newValue.equals(true)) {
                    boolean hasCameraPermission = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
                    if (hasCameraPermission) {
                        if (preference instanceof TwoStatePreference) {
                            account.setDetail(key, newValue.toString());
                            //if (!mIsRefresh) {
                            mCallbacks.saveAccount();
                            //}
                        }
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.CAMERA}, LocalService.PERMISSIONS_REQUEST);
                        } else if (preference instanceof TwoStatePreference) {
                            account.setDetail(key, newValue.toString());
                            //if (!mIsRefresh) {
                            mCallbacks.saveAccount();
                            //}
                        }
                    }
                } else if (preference instanceof TwoStatePreference) {
                    account.setDetail(key, newValue.toString());
                    //if (!mIsRefresh) {
                        mCallbacks.saveAccount();
                    //}
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
                        account.setDetail(ConfigKey.VIDEO_ENABLED, granted);
                        //if (!mIsRefresh) {
                            mCallbacks.saveAccount();
                        //}
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
        final Account account = mCallbacks.getAccount();
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
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.show();
    }
}
