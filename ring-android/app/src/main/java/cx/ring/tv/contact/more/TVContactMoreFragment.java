/*
 * Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 * Author: AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.tv.contact.more;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.leanback.preference.LeanbackSettingsFragmentCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import cx.ring.R;
import cx.ring.tv.account.JamiPreferenceFragment;
import cx.ring.utils.ConversationPath;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TVContactMoreFragment extends LeanbackSettingsFragmentCompat {

    public static final int CLEAR = 101;
    public static final int DELETE = 102;

    private static final int DIALOG_WIDTH = 900;
    private static final int DIALOG_HEIGHT = 400;

    @Override
    public void onPreferenceStartInitialScreen() {
        startPreferenceFragment(PrefsFragment.newInstance());
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat preferenceFragment, Preference preference) {
        return false;
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat caller, PreferenceScreen pref) {
        return false;
    }

    @AndroidEntryPoint
    public static class PrefsFragment extends JamiPreferenceFragment<TVContactMorePresenter> implements TVContactMoreView {

        public static PrefsFragment newInstance() {
            return new PrefsFragment();
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.tv_contact_more_pref, rootKey);
        }

        @Override
        public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            presenter.setContact(ConversationPath.fromIntent(requireActivity().getIntent()));
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            if (preference.getKey().equals("Contact.clear")) {
                createDialog(getString(R.string.conversation_action_history_clear_title), getString(R.string.clear_history),
                        (dialog, whichButton) -> presenter.clearHistory());
            } else if (preference.getKey().equals("Contact.delete")) {
                createDialog(getString(R.string.conversation_action_remove_this_title), getString(R.string.menu_delete),
                        (dialog, whichButton) -> presenter.removeContact());
            }
            return super.onPreferenceTreeClick(preference);
        }

        private void createDialog(String title, String buttonText, DialogInterface.OnClickListener onClickListener) {
            AlertDialog alertDialog = new MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MaterialComponents_Dialog)
                    .setTitle(title)
                    .setMessage("")
                    .setPositiveButton(buttonText, onClickListener)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            alertDialog.getWindow().setLayout(DIALOG_WIDTH, DIALOG_HEIGHT);
            alertDialog.setOwnerActivity(requireActivity());
            alertDialog.setOnShowListener(dialog -> {
                Button positive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positive.setFocusable(true);
                positive.setFocusableInTouchMode(true);
                positive.requestFocus();
            });

            alertDialog.show();
        }

        @Override
        public void finishView(boolean finishParent) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.setResult(finishParent? DELETE : CLEAR);
                activity.finish();
            }
        }

    }
}
