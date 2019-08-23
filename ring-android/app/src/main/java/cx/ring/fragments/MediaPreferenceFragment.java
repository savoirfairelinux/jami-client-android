/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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


import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import java.util.ArrayList;

import cx.ring.R;
import cx.ring.account.AccountEditionActivity;
import cx.ring.application.RingApplication;
import cx.ring.client.RingtoneActivity;
import cx.ring.model.Account;
import cx.ring.model.AccountConfig;
import cx.ring.model.Codec;
import cx.ring.model.ConfigKey;
import cx.ring.mvp.BasePreferenceFragment;

public class MediaPreferenceFragment extends BasePreferenceFragment<MediaPreferencePresenter> implements MediaPreferenceView {

    public static final String TAG = MediaPreferenceFragment.class.getSimpleName();
    private static final int SELECT_RINGTONE_PATH = 40;
    private final Preference.OnPreferenceChangeListener changeVideoPreferenceListener = (preference, newValue) -> {
        final ConfigKey key = ConfigKey.fromString(preference.getKey());
        presenter.videoPreferenceChanged(key, newValue);
        return true;
    };
    private CodecPreference audioCodecsPref = null;
    private CodecPreference videoCodecsPref = null;
    private final Preference.OnPreferenceChangeListener changeCodecListener = (preference, o) -> {
        ArrayList<Long> audio = audioCodecsPref.getActiveCodecList();
        ArrayList<Long> video = videoCodecsPref.getActiveCodecList();
        ArrayList<Long> newOrder = new ArrayList<>(audio.size() + video.size());
        newOrder.addAll(audio);
        newOrder.addAll(video);
        presenter.codecChanged(newOrder);
        return true;
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

        String accountId = getArguments().getString(AccountEditionActivity.ACCOUNT_ID_KEY);

        addPreferencesFromResource(R.xml.account_media_prefs);
        audioCodecsPref = (CodecPreference) findPreference("Account.audioCodecs");
        videoCodecsPref = (CodecPreference) findPreference("Account.videoCodecs");
        Preference ringtonePref = findPreference("ringtone");
        ringtonePref.setOnPreferenceClickListener(preference -> {
            Intent i = new Intent(requireActivity(), RingtoneActivity.class);
            i.putExtra(AccountEditionActivity.ACCOUNT_ID_KEY, accountId);
            requireActivity().startActivity(i);
            return true;
        });

        presenter.init(accountId);
    }

    @Override
    public void accountChanged(Account account, ArrayList<Codec> audioCodec, ArrayList<Codec> videoCodec) {
        if (account == null) {
            return;
        }
        setPreferenceDetails(account.getConfig());
        audioCodecsPref.setCodecs(audioCodec);
        videoCodecsPref.setCodecs(videoCodec);

        addPreferenceListener(ConfigKey.VIDEO_ENABLED, changeVideoPreferenceListener);
        audioCodecsPref.setOnPreferenceChangeListener(changeCodecListener);
        videoCodecsPref.setOnPreferenceChangeListener(changeCodecListener);
    }

    @Override
    public void displayWrongFileFormatDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.ringtone_error_title)
                .setMessage(R.string.ringtone_error_format_not_supported)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @Override
    public void displayPermissionCameraDenied() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.permission_dialog_camera_title)
                .setMessage(R.string.permission_dialog_camera_message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void displayFileSearchDialog() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        startActivityForResult(intent, SELECT_RINGTONE_PATH);
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
        if (null != audioCodecsPref) {
            audioCodecsPref.refresh();
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
        }
    }


}
