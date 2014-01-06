/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
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
package org.sflphone.fragments;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.sflphone.R;

import android.app.DialogFragment;
import android.os.Bundle;
import android.os.Environment;
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

public class FileExplorerDFragment extends DialogFragment implements OnItemClickListener {

    private String prefKey;

    public interface onFileSelectedListener {
        public void onFileSelected(String path, String key);
    }

    public static FileExplorerDFragment newInstance() {
        FileExplorerDFragment f = new FileExplorerDFragment();
        return f;
    }

    private List<String> item = null;
    private List<String> path = null;
    private String root;
    private TextView myPath;
    public onFileSelectedListener mDelegateListener;

    private String currentPath;
    Comparator<? super File> comparator;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.file_explorer_dfrag, container);
        myPath = (TextView) rootView.findViewById(R.id.path);

        getDialog().setTitle(getResources().getString(R.string.file_explorer_title));

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

        ArrayAdapter<String> fileList = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, item);

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

    public void setOnFileSelectedListener(onFileSelectedListener listener, String key) {
        mDelegateListener = listener;
        prefKey = key;
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
        File file = new File(path.get(pos));


//        if (file.isDirectory()) {
//                selectButton.setEnabled(false);
//                if (file.canRead()) {
//                        lastPositions.put(currentPath, position);
//                        getDir(path.get(position));
//                        if (canSelectDir) {
//                                selectedFile = file;
//                                v.setSelected(true);
//                                selectButton.setEnabled(true);
//                        }
//                } else {
//                        new AlertDialog.Builder(this).setIcon(R.drawable.icon)
//                                        .setTitle("[" + file.getName() + "] " + getText(R.string.cant_read_folder))
//                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//
//                                                @Override
//                                                public void onClick(DialogInterface dialog, int which) {
//
//                                                }
//                                        }).show();
//                }
//        } else {
//                selectedFile = file;
//                v.setSelected(true);
//                selectButton.setEnabled(true);
//        }
//        
        
        
        
        if (mDelegateListener != null) {
            mDelegateListener.onFileSelected((String) ((ListView) getView().findViewById(android.R.id.list)).getAdapter().getItem(pos), prefKey);
        }
    }
}
