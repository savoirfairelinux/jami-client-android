/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Aline Gondim Santos <aline.gondimsantos@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.settings.pluginssettings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cx.ring.R;
import cx.ring.client.HomeActivity;
import cx.ring.databinding.FragPluginsPathPreferenceBinding;
import cx.ring.utils.AndroidFileUtils;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class PluginPathPreferenceFragment extends Fragment implements PathListAdapter.PathListItemListener {

    public static final String TAG = PluginPathPreferenceFragment.class.getSimpleName();
    private static final int PATH_REQUEST_CODE = 1;
    private final List<String> pathList = new ArrayList<>();
    private String mCurrentValue;
    private Context mContext;
    private String subtitle;
    private String supportedMimeTypes = "*/*";

    private FragPluginsPathPreferenceBinding binding;

    public static PluginPathPreferenceFragment newInstance(PluginDetails pluginDetails, String preferenceKey) {
        Bundle args = new Bundle();
        args.putString("name", pluginDetails.getName());
        args.putString("rootPath", pluginDetails.getRootPath());
        args.putBoolean("enabled", pluginDetails.isEnabled());
        args.putString("preferenceKey", preferenceKey);
        PluginPathPreferenceFragment ppf = new PluginPathPreferenceFragment();
        ppf.setArguments(args);
        return ppf;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = requireActivity();

        Bundle arguments = getArguments();

        if (arguments != null && arguments.getString("name") != null
                && arguments.getString("rootPath") != null) {
            PluginDetails pluginDetails = new PluginDetails(arguments.getString("name"),
                    arguments.getString("rootPath"), arguments.getBoolean("enabled"));

            List<Map<String, String>> mPreferencesAttributes = pluginDetails.getPluginPreferences();
            if (mPreferencesAttributes != null && !mPreferencesAttributes.isEmpty()) {
                String key = arguments.getString("preferenceKey");
                Map<String, String> prefValues = pluginDetails.getPluginPreferencesValues();
                mCurrentValue = prefValues.get(key);
                setHasOptionsMenu(true);
                for (Map<String, String> preferenceAttributes : mPreferencesAttributes) {
                    if (preferenceAttributes.get("key").equals(key)) {
                        String mimeType = preferenceAttributes.get("mimeType");
                        if (!TextUtils.isEmpty(mimeType))
                            supportedMimeTypes = mimeType;
                        subtitle = pluginDetails.getName() + " - " + preferenceAttributes.get("title");

                        String defaultPath = preferenceAttributes.get("defaultValue");
                        if (!TextUtils.isEmpty(defaultPath)) {
                            defaultPath = defaultPath.substring(0, defaultPath.lastIndexOf("/"));
                            for (File file : new File(defaultPath).listFiles()) {
                                pathList.add(file.toString());
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragPluginsPathPreferenceBinding.inflate(inflater, container, false);

        if (!pathList.isEmpty()) {
            binding.pathPreferences.setAdapter(new PathListAdapter(pathList, this));
        }

        ((HomeActivity) requireActivity()).setToolbarTitle(R.string.menu_item_plugin_list);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        binding.pluginSettingSubtitle.setText(subtitle);
        binding.pluginsPathPreferenceFab.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType(supportedMimeTypes);
            startActivityForResult(intent, PATH_REQUEST_CODE);
        });

        if (!mCurrentValue.isEmpty()) {
            binding.currentPathItemIcon.setVisibility(View.VISIBLE);
            File file = new File(mCurrentValue);
            if (file.exists()) {
                if (AndroidFileUtils.isImage(mCurrentValue)) {
                    binding.currentPathItemName.setVisibility(View.INVISIBLE);
                    Drawable icon = Drawable.createFromPath(mCurrentValue);
                    if (icon != null) {
                        binding.currentPathItemIcon.setImageDrawable(icon);
                    }
                } else {
                    binding.currentPathItemName.setVisibility(View.VISIBLE);
                    binding.currentPathItemName.setText(AndroidFileUtils.getFileName(mCurrentValue));
                }
            }
        }
        else{
            binding.currentPathItemIcon.setVisibility(View.INVISIBLE);
            binding.currentPathItemName.setVisibility(View.INVISIBLE);
            binding.pluginsPathPreferenceFab.performClick();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == PATH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if(data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    AndroidFileUtils.getCacheFile(requireContext(), uri)
                            .observeOn(AndroidSchedulers.mainThread())
                            .map(File::getAbsolutePath)
                            .subscribe(new SingleObserver<String>() {
                                @Override
                                public void onSubscribe(Disposable d) {
                                }

                                @Override
                                public void onSuccess(@NonNull String filename) {
                                    Toast.makeText(mContext, "File: " + filename + " successfully read", Toast.LENGTH_LONG).show();
                                    setPreferencePath(filename);
                                }

                                @Override
                                public void onError(@NonNull Throwable e) {
                                    Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                }
            }
        }
    }

    @Override
    public void onResume() {
        ((HomeActivity) requireActivity()).
                setToolbarState(R.string.menu_item_plugin_list);
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private boolean setPreferencePath(String path){
        Bundle arguments = getArguments();

        if (arguments != null && arguments.getString("name") != null
                && arguments.getString("rootPath") != null) {
            PluginDetails pluginDetails = new PluginDetails(arguments.getString("name"),
                    arguments.getString("rootPath"), arguments.getBoolean("enabled"));
            if (pluginDetails.setPluginPreference(arguments.getString("preferenceKey"), path)){
                mCurrentValue = path;
                if (!mCurrentValue.isEmpty()) {
                    binding.currentPathItemIcon.setVisibility(View.VISIBLE);
                    File file = new File(mCurrentValue);
                    Drawable icon = null;
                    if (file.exists() && AndroidFileUtils.isImage(mCurrentValue)) {
                        icon = Drawable.createFromPath(mCurrentValue);
                    }
                    if (icon != null) {
                        binding.currentPathItemIcon.setImageDrawable(icon);
                    }
                    if (AndroidFileUtils.isImage(mCurrentValue))
                        binding.currentPathItemName.setVisibility(View.INVISIBLE);
                    else {
                        binding.currentPathItemName.setText(AndroidFileUtils.getFileName(mCurrentValue));
                        binding.currentPathItemName.setVisibility(View.VISIBLE);
                    }
                }
                else{
                    binding.currentPathItemIcon.setVisibility(View.INVISIBLE);
                    binding.currentPathItemName.setVisibility(View.INVISIBLE);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void onPathItemClicked(String path) {
        setPreferencePath(path);
    }
}
