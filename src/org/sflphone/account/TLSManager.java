/*
 *  Copyright (C) 2004-2013 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package org.sflphone.account;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.sflphone.R;
import org.sflphone.model.Account;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class TLSManager {
    PreferenceScreen mScreen;
    private Account mAccount;
    static Activity mContext;

    public void onCreate(Activity con, PreferenceScreen preferenceScreen, Account acc) {
        mContext = con;
        mScreen = preferenceScreen;
        mAccount = acc;

        setDetails();
    }

    private void setDetails() {
        for (int i = 0; i < mScreen.getPreferenceCount(); ++i) {

            if (mScreen.getPreference(i) instanceof EditTextPreference) {

            } else {

            }

            // ((CheckBoxPreference)
            // mScreen.getPreference(i)).setChecked(mAccount.getSrtpDetails().getDetailBoolean(mScreen.getPreference(i).getKey()));
            mScreen.getPreference(i).setOnPreferenceClickListener(new OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (preference.getKey().contentEquals(AccountDetailTls.CONFIG_TLS_CA_LIST_FILE)) {
                        Dialogo dialog = Dialogo.newInstance();
                        dialog.show(mContext.getFragmentManager(), "dialog");
                    } else if (preference.getKey().contentEquals(AccountDetailTls.CONFIG_TLS_CERTIFICATE_FILE)) {

                    } else if (preference.getKey().contentEquals(AccountDetailTls.CONFIG_TLS_PRIVATE_KEY_FILE)) {

                    }
                    return false;
                }
            });
        }
    }

    public void setTLSListener() {
        for (int i = 0; i < mScreen.getPreferenceCount(); ++i) {
            mScreen.getPreference(i).setOnPreferenceChangeListener(tlsListener);
        }
    }

    private OnPreferenceChangeListener tlsListener = new OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Log.i("TLS", "Setting " + preference.getKey() + " to" + (Boolean) newValue);
            mAccount.getTlsDetails().setDetailString(preference.getKey(), Boolean.toString((Boolean) newValue));
            mAccount.notifyObservers();
            return true;
        }
    };

    public static class Dialogo extends DialogFragment implements OnItemClickListener {

        /**
         * Create a new instance of CallActionsDFragment
         */
        public static Dialogo newInstance() {
            Dialogo f = new Dialogo();
            return f;
        }

        private List<String> item = null;
        private List<String> path = null;
        private String root;
        private TextView myPath;

        private String currentPath;
        Comparator<? super File> comparator;

        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            super.onCreateView(inflater, container, savedInstanceState);
            View rootView = inflater.inflate(R.layout.file_explorer_dfrag, container);
            myPath = (TextView) rootView.findViewById(R.id.path);

            comparator = filecomparatorByAlphabetically;
            root = Environment.getExternalStorageDirectory().getPath();
            getDir(root, rootView);

            Button btnAlphabetically = (Button) rootView.findViewById(R.id.button_alphabetically);
            btnAlphabetically.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    comparator = filecomparatorByAlphabetically;
                    getDir(currentPath, getView());

                }
            });

            Button btnLastDateModified = (Button) rootView.findViewById(R.id.button_lastDateModified);
            btnLastDateModified.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    comparator = filecomparatorByLastModified;
                    getDir(currentPath, getView());

                }
            });
            return rootView;
        }

        private void getDir(String dirPath, View parent) {
            currentPath = dirPath;

            myPath.setText("Location: " + dirPath);
            item = new ArrayList<String>();
            path = new ArrayList<String>();
            File f = new File(dirPath);
            File[] files = f.listFiles();

            if (!dirPath.equals(root)) {
                item.add(root);
                path.add(root);
                item.add("../");
                path.add(f.getParent());
            }

            Arrays.sort(files, comparator);

            for (int i = 0; i < files.length; i++) {
                File file = files[i];

                if (!file.isHidden() && file.canRead()) {
                    path.add(file.getPath());
                    if (file.isDirectory()) {
                        item.add(file.getName() + "/");
                    } else {
                        item.add(file.getName());
                    }
                }
            }

            ArrayAdapter<String> fileList = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, item);

            ((ListView) parent.findViewById(android.R.id.list)).setAdapter(fileList);
            ((ListView) parent.findViewById(android.R.id.list)).setOnItemClickListener(this);
        }

        Comparator<? super File> filecomparatorByLastModified = new Comparator<File>() {

            public int compare(File file1, File file2) {

                if (file1.isDirectory()) {
                    if (file2.isDirectory()) {
                        return Long.valueOf(file1.lastModified()).compareTo(file2.lastModified());
                    } else {
                        return -1;
                    }
                } else {
                    if (file2.isDirectory()) {
                        return 1;
                    } else {
                        return Long.valueOf(file1.lastModified()).compareTo(file2.lastModified());
                    }
                }

            }
        };

        Comparator<? super File> filecomparatorByAlphabetically = new Comparator<File>() {

            public int compare(File file1, File file2) {

                if (file1.isDirectory()) {
                    if (file2.isDirectory()) {
                        return String.valueOf(file1.getName().toLowerCase(Locale.getDefault())).compareTo(file2.getName().toLowerCase());
                    } else {
                        return -1;
                    }
                } else {
                    if (file2.isDirectory()) {
                        return 1;
                    } else {
                        return String.valueOf(file1.getName().toLowerCase(Locale.getDefault())).compareTo(file2.getName().toLowerCase());
                    }
                }

            }
        };

        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

            // TODO Auto-generated method stub
            File file = new File(path.get(position));

            if (file.isDirectory()) {
                if (file.canRead()) {
                    getDir(path.get(position), getView());
                } else {
                    new AlertDialog.Builder(mContext).setIcon(R.drawable.ic_launcher).setTitle("[" + file.getName() + "] folder can't be read!")
                            .setPositiveButton("OK", null).show();
                }
            } else {
                new AlertDialog.Builder(mContext).setIcon(R.drawable.ic_launcher).setTitle("[" + file.getName() + "]").setPositiveButton("OK", null)
                        .show();

            }
        }

    }
}