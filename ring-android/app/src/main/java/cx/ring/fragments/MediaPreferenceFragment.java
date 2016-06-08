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
import cx.ring.model.Codec;
import cx.ring.model.account.Account;
import cx.ring.model.account.AccountDetail;
import cx.ring.model.account.AccountDetailAdvanced;
import cx.ring.model.account.AccountDetailBasic;
import cx.ring.service.IDRingService;
import cx.ring.service.LocalService;

public class MediaPreferenceFragment extends PreferenceFragment implements FragmentCompat.OnRequestPermissionsResultCallback{
    static final String TAG = MediaPreferenceFragment.class.getSimpleName();

    private CodecPreference audioCodecsPref = null;
    private CodecPreference videoCodecsPref = null;

    public interface Callbacks extends LocalService.Callbacks {
        Account getAccount();
    }

    private static final Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public IDRingService getRemoteService() {
            return null;
        }

        @Override
        public LocalService getService() {
            return null;
        }

        @Override
        public Account getAccount() {
            return null;
        }
    };
    protected Callbacks mCallbacks = sDummyCallbacks;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    private static final int SELECT_RINGTONE_PATH = 40;
    private Preference.OnPreferenceClickListener filePickerListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            performFileSearch(SELECT_RINGTONE_PATH);
            return true;
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_CANCELED)
            return;

        File myFile = new File(data.getData().getPath());
        Log.i(TAG, "file selected:" + data.getData());
        if (requestCode == SELECT_RINGTONE_PATH) {
            findPreference(AccountDetailAdvanced.CONFIG_RINGTONE_PATH).setSummary(myFile.getName());
            mCallbacks.getAccount().getAdvancedDetails().setDetailString(AccountDetailAdvanced.CONFIG_RINGTONE_PATH, myFile.getAbsolutePath());
            mCallbacks.getAccount().notifyObservers();
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
        final Account acc = mCallbacks.getAccount();
        if (acc == null)
            return;
        addPreferencesFromResource(R.xml.account_media_prefs);
        setPreferenceDetails(acc.getBasicDetails());
        setPreferenceDetails(acc.getAdvancedDetails());
        findPreference(AccountDetailAdvanced.CONFIG_RINGTONE_PATH).setEnabled(
                ((TwoStatePreference) findPreference(AccountDetailAdvanced.CONFIG_RINGTONE_ENABLED)).isChecked());
        addPreferenceListener(acc.getAdvancedDetails(), changeAudioPreferenceListener);
        addPreferenceListener(AccountDetailBasic.CONFIG_VIDEO_ENABLED, changeVideoPreferenceListener);

        final ArrayList<Codec> audioCodec = new ArrayList<>();
        final ArrayList<Codec> videoCodec = new ArrayList<>();
        try {
            final ArrayList<Codec> codec = ((ArrayList<Codec>) mCallbacks.getRemoteService().getCodecList(acc.getAccountID()));
            for (Codec c : codec) {
                if (c.getType() == Codec.Type.AUDIO)
                    audioCodec.add(c);
                else if (c.getType() == Codec.Type.VIDEO)
                    videoCodec.add(c);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        audioCodecsPref = (CodecPreference) findPreference("Account.audioCodecs");
        audioCodecsPref.setCodecs(audioCodec);
        audioCodecsPref.setOnPreferenceChangeListener(changeCodecListener);

        videoCodecsPref = (CodecPreference) findPreference("Account.videoCodecs");
        videoCodecsPref.setCodecs(videoCodec);
        videoCodecsPref.setOnPreferenceChangeListener(changeCodecListener);
    }

    private final Preference.OnPreferenceChangeListener changeCodecListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            final Account acc = mCallbacks.getAccount();
            ArrayList<Long> audio = audioCodecsPref.getActiveCodecList();
            ArrayList<Long> video = videoCodecsPref.getActiveCodecList();
            ArrayList<Long> new_order = new ArrayList<>(audio.size() + video.size());
            new_order.addAll(audio);
            new_order.addAll(video);
            try {
                mCallbacks.getRemoteService().setActiveCodecList(new_order, acc.getAccountID());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            acc.notifyObservers();
            return true;
        }
    };

    private final Preference.OnPreferenceChangeListener changeAudioPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final Account acc = mCallbacks.getAccount();
            String key = preference.getKey();
            if (preference instanceof TwoStatePreference) {
                if (key.contentEquals(AccountDetailAdvanced.CONFIG_RINGTONE_ENABLED))
                    getPreferenceScreen().findPreference(AccountDetailAdvanced.CONFIG_RINGTONE_PATH).setEnabled((Boolean) newValue);
                acc.getAdvancedDetails().setDetailString(key, newValue.toString());
            } else  if (key.contentEquals(AccountDetailAdvanced.CONFIG_ACCOUNT_DTMF_TYPE)) {
                preference.setSummary(((String)newValue).contentEquals("overrtp") ? "RTP" : "SIP");
            } else {
                preference.setSummary((CharSequence) newValue);
                Log.i(TAG, "Changing" + key + " value:" + newValue);
                acc.getAdvancedDetails().setDetailString(key, newValue.toString());
            }
            acc.notifyObservers();

            return true;
        }
    };

    private final Preference.OnPreferenceChangeListener changeVideoPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final Account acc = mCallbacks.getAccount();
            if (null != acc && newValue instanceof Boolean) {
                if (newValue.equals(true)) {
                    boolean hasCameraPermission = ContextCompat.checkSelfPermission(getActivity(),
                            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
                    if (hasCameraPermission) {
                        if (preference instanceof TwoStatePreference) {
                            acc.getBasicDetails().setDetailString(preference.getKey(), newValue.toString());
                        }
                        acc.notifyObservers();
                    }
                    else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    LocalService.PERMISSIONS_REQUEST);
                        }
                        else {
                            if (preference instanceof TwoStatePreference) {
                                acc.getBasicDetails().setDetailString(preference.getKey(), newValue.toString());
                            }
                            acc.notifyObservers();
                        }
                    }
                }
                else {
                    if (preference instanceof TwoStatePreference) {
                        acc.getBasicDetails().setDetailString(preference.getKey(), newValue.toString());
                    }
                    acc.notifyObservers();
                }
            }
            return true;
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i=0, n=permissions.length; i<n; i++) {
            switch (permissions[i]) {
                case Manifest.permission.CAMERA:
                    boolean granted = (grantResults[i] == PackageManager.PERMISSION_GRANTED);
                    final Account acc = mCallbacks.getAccount();
                    if (null != acc) {
                        acc.getBasicDetails().setDetailString(AccountDetailBasic.CONFIG_VIDEO_ENABLED, Boolean.toString(granted));
                        acc.notifyObservers();
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
        for (AccountDetail.PreferenceEntry p : details.getDetailValues()) {
            Preference pref = findPreference(p.mKey);
            if (pref != null) {
                if (pref instanceof TwoStatePreference) {
                    ((TwoStatePreference) pref).setChecked(p.mValue.contentEquals(AccountDetail.TRUE_STR));
                } else if (p.mKey.contentEquals(AccountDetailAdvanced.CONFIG_ACCOUNT_DTMF_TYPE)) {
                    pref.setDefaultValue(p.mValue.contentEquals("overrtp") ? "RTP" : "SIP");
                    pref.setSummary(p.mValue.contentEquals("overrtp") ? "RTP" : "SIP");
                } else if (p.mKey.contentEquals(AccountDetailAdvanced.CONFIG_RINGTONE_PATH)) {
                    File tmp = new File(p.mValue);
                    pref.setSummary(tmp.getName());
                } else
                    pref.setSummary(p.mValue);
            }
        }
    }

    private void addPreferenceListener(AccountDetail details, Preference.OnPreferenceChangeListener listener) {
        for (AccountDetail.PreferenceEntry p : details.getDetailValues())
            addPreferenceListener(p.mKey, listener);
    }

    private void addPreferenceListener(String key, Preference.OnPreferenceChangeListener listener) {
        Preference pref = findPreference(key);
        if (pref != null) {
            pref.setOnPreferenceChangeListener(listener);
            if (key.contentEquals(AccountDetailAdvanced.CONFIG_RINGTONE_PATH))
                pref.setOnPreferenceClickListener(filePickerListener);
        }
    }

    public void refresh() {
        final Account acc = mCallbacks.getAccount();
        if (acc != null) {
            setPreferenceDetails(acc.getBasicDetails());
            acc.notifyObservers();
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
