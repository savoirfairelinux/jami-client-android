/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
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
package cx.ring.fragments

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.account.AccountEditionFragment
import cx.ring.client.RingtoneActivity
import cx.ring.mvp.BasePreferenceFragment
import dagger.hilt.android.AndroidEntryPoint
import net.jami.model.Account
import net.jami.model.AccountConfig
import net.jami.model.Codec
import net.jami.model.ConfigKey
import net.jami.settings.MediaPreferencePresenter
import net.jami.settings.MediaPreferenceView

@AndroidEntryPoint
class MediaPreferenceFragment : BasePreferenceFragment<MediaPreferencePresenter>(), MediaPreferenceView {
    private val changeVideoPreferenceListener = Preference.OnPreferenceChangeListener { preference: Preference, newValue: Any ->
        val key = ConfigKey.fromString(preference.key)!!
        presenter.videoPreferenceChanged(key, newValue)
        true
    }
    private var audioCodecsPref: CodecPreference? = null
    private var videoCodecsPref: CodecPreference? = null

    private val changeCodecListener = Preference.OnPreferenceChangeListener { _, _ ->
        val audio = audioCodecsPref!!.activeCodecList
        val video = videoCodecsPref!!.activeCodecList
        val newOrder = ArrayList<Long>(audio.size + video.size)
        newOrder.addAll(audio)
        newOrder.addAll(video)
        presenter.codecChanged(newOrder)
        true
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        val accountId = requireArguments().getString(AccountEditionFragment.ACCOUNT_ID_KEY)!!
        addPreferencesFromResource(R.xml.account_media_prefs)
        audioCodecsPref = findPreference("Account.audioCodecs")
        videoCodecsPref = findPreference("Account.videoCodecs")
        findPreference<Preference>("ringtone")?.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val i = Intent(requireActivity(), RingtoneActivity::class.java)
                i.putExtra(AccountEditionFragment.ACCOUNT_ID_KEY, accountId)
                requireActivity().startActivity(i)
                true
            }
        }
        presenter.init(accountId)
    }

    override fun accountChanged(account: Account, audioCodec: ArrayList<Codec>, videoCodec: ArrayList<Codec>) {
        setPreferenceDetails(account.config)
        audioCodecsPref!!.setCodecs(audioCodec)
        videoCodecsPref!!.setCodecs(videoCodec)
        addPreferenceListener(ConfigKey.VIDEO_ENABLED, changeVideoPreferenceListener)
        audioCodecsPref!!.onPreferenceChangeListener = changeCodecListener
        videoCodecsPref!!.onPreferenceChangeListener = changeCodecListener
    }

    override fun displayWrongFileFormatDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.ringtone_error_title)
            .setMessage(R.string.ringtone_error_format_not_supported)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun displayPermissionCameraDenied() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.permission_dialog_camera_title)
            .setMessage(R.string.permission_dialog_camera_message)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
            .show()
    }

    override fun displayFileSearchDialog() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "audio/*"
        startActivityForResult(intent, SELECT_RINGTONE_PATH)
    }

    override fun refresh(account: Account) {
        setPreferenceDetails(account.config)
        if (null != listView && null != listView.adapter) {
            listView.adapter!!.notifyDataSetChanged()
        }
        if (null != videoCodecsPref) {
            videoCodecsPref!!.refresh()
        }
        if (null != audioCodecsPref) {
            audioCodecsPref!!.refresh()
        }
    }

    private fun setPreferenceDetails(details: AccountConfig) {
        for (confKey in details.keys) {
            val pref = findPreference<Preference>(confKey.key)
            if (pref != null) {
                if (pref is TwoStatePreference) {
                    pref.isChecked = details.getBool(confKey)
                } else if (confKey === ConfigKey.ACCOUNT_DTMF_TYPE) {
                    pref.setDefaultValue(if (details[confKey].contentEquals("overrtp")) "RTP" else "SIP")
                    pref.summary = if (details[confKey].contentEquals("overrtp")) "RTP" else "SIP"
                } else {
                    pref.summary = details[confKey]
                }
            }
        }
    }

    private fun addPreferenceListener(details: AccountConfig, listener: Preference.OnPreferenceChangeListener) {
        for (confKey in details.keys) addPreferenceListener(confKey, listener)
    }

    private fun addPreferenceListener(key: ConfigKey, listener: Preference.OnPreferenceChangeListener) {
        findPreference<Preference>(key.key)?.onPreferenceChangeListener = listener
    }

    companion object {
        val TAG = MediaPreferenceFragment::class.simpleName!!
        private const val SELECT_RINGTONE_PATH = 40
        fun newInstance(accountId: String): MediaPreferenceFragment {
            val mediaPreferenceFragment = MediaPreferenceFragment()
            mediaPreferenceFragment.arguments = Bundle().apply { putString(AccountEditionFragment.ACCOUNT_ID_KEY, accountId) }
            return mediaPreferenceFragment
        }
    }
}