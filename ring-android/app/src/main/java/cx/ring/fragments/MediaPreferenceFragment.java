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
import android.support.v14.preference.SwitchPreference;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;

import java.io.File;
import java.util.ArrayList;

import cx.ring.R;
import cx.ring.account.AccountEditionActivity;
import cx.ring.application.RingApplication;
import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.model.Codec;
import cx.ring.model.ConfigKey;
import cx.ring.mvp.BasePreferenceFragment;
import cx.ring.utils.FileUtils;

public class MediaPreferenceFragment extends BasePreferenceFragment<MediaPreferencePresenter>
        implements FragmentCompat.OnRequestPermissionsResultCallback, MediaPreferenceView {

    public static final String TAG = MediaPreferenceFragment.class.getSimpleName();

    private CodecPreference audioCodecsPref = null;
    private CodecPreference videoCodecsPref = null;
    private SwitchPreference mRingtoneCustom = null;

    private static final int SELECT_RINGTONE_PATH = 40;
    private Preference.OnPreferenceClickListener filePickerListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            presenter.fileSearchClicked();
            return true;
        }
    };

    public static MediaPreferenceFragment newInstance(@NonNull String accountId) {
        Bundle bundle = new Bundle();
        bundle.putString(AccountEditionActivity.ACCOUNT_ID_KEY, accountId);
        MediaPreferenceFragment mediaPreferenceFragment = new MediaPreferenceFragment();
        mediaPreferenceFragment.setArguments(bundle);
        return mediaPreferenceFragment;
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);
        super.onCreatePreferences(bundle, rootKey);

        addPreferencesFromResource(R.xml.account_media_prefs);
        audioCodecsPref = (CodecPreference) findPreference("Account.audioCodecs");
        videoCodecsPref = (CodecPreference) findPreference("Account.videoCodecs");
        mRingtoneCustom = (SwitchPreference) findPreference("Account.ringtoneCustom");

        presenter.init(getArguments().getString(AccountEditionActivity.ACCOUNT_ID_KEY));

        addPreferenceListener(ConfigKey.VIDEO_ENABLED, changeVideoPreferenceListener);
        mRingtoneCustom.setOnPreferenceChangeListener(changeAudioPreferenceListener);

    }

    @Override
    public void accountChanged(Account account, ArrayList<Codec> audioCodec, ArrayList<Codec> videoCodec) {
        if (account == null) {
            return;
        }
        setPreferenceDetails(account.getConfig());
        audioCodecsPref.setCodecs(audioCodec);
        videoCodecsPref.setCodecs(videoCodec);

        addPreferenceListener(account.getConfig(), changeAudioPreferenceListener);
        audioCodecsPref.setOnPreferenceChangeListener(changeCodecListener);
        videoCodecsPref.setOnPreferenceChangeListener(changeCodecListener);
        mRingtoneCustom.setOnPreferenceChangeListener(changeAudioPreferenceListener);
    }

    @Override
    public void displayWrongFileFormatDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.ringtone_error_title);
        builder.setMessage(R.string.ringtone_error_format_not_supported);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
    }

    @Override
    public void displayFileTooBigDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.ringtone_error_title);
        builder.setMessage(getString(R.string.ringtone_error_size_too_big, MediaPreferencePresenter.MAX_SIZE_RINGTONE));
        builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
    }

    @Override
    public void displayPermissionCameraDenied() {
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

    @Override
    public void displayFileSearchDialog() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        startActivityForResult(intent, SELECT_RINGTONE_PATH);
    }

    @Override
    public void requestVideoPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, RingApplication.PERMISSIONS_REQUEST);
        }
    }

    @Override
    public void refresh(Account account) {
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_CANCELED) {
            return;
        }

        if (requestCode == SELECT_RINGTONE_PATH) {
            String path = FileUtils.getRealPathFromURI(getActivity(), data.getData());
            String type = getActivity().getContentResolver().getType(data.getData());
            presenter.onFileFound(type, path);
        }
    }

    private final Preference.OnPreferenceChangeListener changeCodecListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            ArrayList<Long> audio = audioCodecsPref.getActiveCodecList();
            ArrayList<Long> video = videoCodecsPref.getActiveCodecList();
            ArrayList<Long> newOrder = new ArrayList<>(audio.size() + video.size());
            newOrder.addAll(audio);
            newOrder.addAll(video);
            presenter.codecChanged(newOrder);
            return true;
        }
    };

    private final Preference.OnPreferenceChangeListener changeAudioPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final ConfigKey key = ConfigKey.fromString(preference.getKey());
            if (preference instanceof TwoStatePreference) {
                if (key == ConfigKey.RINGTONE_ENABLED) {
                    mRingtoneCustom.setEnabled((Boolean) newValue);
                    Boolean isEnabled = (Boolean) newValue && mRingtoneCustom.isChecked();
                    getPreferenceScreen().findPreference(ConfigKey.RINGTONE_PATH.key()).setEnabled(isEnabled);
                } else if (key == ConfigKey.RINGTONE_CUSTOM) {
                    getPreferenceScreen().findPreference(ConfigKey.RINGTONE_PATH.key()).setEnabled((Boolean) newValue);
                    if ((Boolean) newValue) {
                        findPreference(ConfigKey.RINGTONE_PATH.key()).setSummary(
                                new File(FileUtils.ringtonesPath(getActivity()) + File.separator + "default.wav").getName());
                    }
                }
            } else if (key == ConfigKey.ACCOUNT_DTMF_TYPE) {
                preference.setSummary(((String) newValue).contentEquals("overrtp") ? "RTP" : "SIP");
            } else {
                preference.setSummary((CharSequence) newValue);
            }

            presenter.audioPreferenceChanged(key, newValue);
            return true;
        }
    };

    private final Preference.OnPreferenceChangeListener changeVideoPreferenceListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final ConfigKey key = ConfigKey.fromString(preference.getKey());
            boolean versionMOrSuperior = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
            presenter.videoPreferenceChanged(key, newValue, versionMOrSuperior);
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
                    presenter.permissionsUpdated(granted);
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

    @Override
    public void initPreferences(boolean isRingtoneEnabled, boolean isCustomRingtoneEnabled) {
        mRingtoneCustom.setEnabled(isRingtoneEnabled);
        findPreference(ConfigKey.RINGTONE_PATH.key()).setEnabled(isCustomRingtoneEnabled);
    }
}
