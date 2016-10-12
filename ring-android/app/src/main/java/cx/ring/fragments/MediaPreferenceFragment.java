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
import android.os.RemoteException;
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
import cx.ring.model.account.AccountConfig;
import cx.ring.model.account.ConfigKey;
import cx.ring.service.LocalService;

import static cx.ring.client.AccountEditionActivity.DUMMY_CALLBACKS;


public class MediaPreferenceFragment extends PreferenceFragment
        implements FragmentCompat.OnRequestPermissionsResultCallback, AccountChangedListener {
    static final String TAG = MediaPreferenceFragment.class.getSimpleName();

    private CodecPreference audioCodecsPref = null;
    private CodecPreference videoCodecsPref = null;

    protected AccountCallbacks mCallbacks = DUMMY_CALLBACKS;

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
        setPreferenceDetails(acc.getConfig());
        addPreferenceListener(acc.getConfig(), changeAudioPreferenceListener);
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

        File myFile = new File(data.getData().getPath());
        Log.i(TAG, "file selected:" + data.getData());
        if (requestCode == SELECT_RINGTONE_PATH) {
            findPreference(ConfigKey.RINGTONE_PATH.key()).setSummary(myFile.getName());
            mCallbacks.getAccount().setDetail(ConfigKey.RINGTONE_PATH, myFile.getAbsolutePath());
            mCallbacks.saveAccount();
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

        findPreference(ConfigKey.RINGTONE_PATH.key()).setEnabled(
                ((TwoStatePreference) findPreference(ConfigKey.RINGTONE_ENABLED.key())).isChecked());
        addPreferenceListener(ConfigKey.VIDEO_ENABLED, changeVideoPreferenceListener);
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
            mCallbacks.saveAccount();
            return true;
        }
    };

    private final Preference.OnPreferenceChangeListener changeAudioPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final Account account = mCallbacks.getAccount();
            final ConfigKey key = ConfigKey.fromString(preference.getKey());
            if (preference instanceof TwoStatePreference) {
                if (key == ConfigKey.RINGTONE_ENABLED)
                    getPreferenceScreen().findPreference(ConfigKey.RINGTONE_PATH.key()).setEnabled((Boolean) newValue);
                account.setDetail(key, newValue.toString());
            } else  if (key == ConfigKey.ACCOUNT_DTMF_TYPE) {
                preference.setSummary(((String)newValue).contentEquals("overrtp") ? "RTP" : "SIP");
            } else {
                preference.setSummary((CharSequence) newValue);
                Log.i(TAG, "Changing" + key + " value:" + newValue);
                account.setDetail(key, newValue.toString());
            }
            mCallbacks.saveAccount();

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
                            mCallbacks.saveAccount();
                        }
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.CAMERA}, LocalService.PERMISSIONS_REQUEST);
                        } else if (preference instanceof TwoStatePreference) {
                            account.setDetail(key, newValue.toString());
                            mCallbacks.saveAccount();
                        }
                    }
                } else if (preference instanceof TwoStatePreference) {
                    account.setDetail(key, newValue.toString());
                    mCallbacks.saveAccount();
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
                        mCallbacks.saveAccount();
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
        for (ConfigKey k : details.getKeys()) {
            Preference pref = findPreference(k.key());
            if (pref != null) {
                if (pref instanceof TwoStatePreference) {
                    ((TwoStatePreference) pref).setChecked(details.getBool(k));
                } else if (k == ConfigKey.ACCOUNT_DTMF_TYPE) {
                    pref.setDefaultValue(details.get(k).contentEquals("overrtp") ? "RTP" : "SIP");
                    pref.setSummary(details.get(k).contentEquals("overrtp") ? "RTP" : "SIP");
                } else if (k == ConfigKey.RINGTONE_PATH) {
                    File tmp = new File(details.get(k));
                    pref.setSummary(tmp.getName());
                } else
                    pref.setSummary(details.get(k));
            }
        }
    }

    private void addPreferenceListener(AccountConfig details, Preference.OnPreferenceChangeListener listener) {
        for (ConfigKey p : details.getKeys())
            addPreferenceListener(p, listener);
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
