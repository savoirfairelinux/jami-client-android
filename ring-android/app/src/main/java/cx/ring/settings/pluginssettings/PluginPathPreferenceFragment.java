package cx.ring.settings.pluginssettings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cx.ring.R;
import cx.ring.client.HomeActivity;
import cx.ring.utils.AndroidFileUtils;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

import static cx.ring.utils.AndroidFileUtils.getFileName;
import static cx.ring.utils.AndroidFileUtils.isImage;

public class PluginPathPreferenceFragment extends Fragment implements PathListAdapter.PathListItemListener {

    public static final String TAG = PluginPathPreferenceFragment.class.getSimpleName();
    private static final int PATH_REQUEST_CODE = 1;
    private List<String> pathList = new ArrayList<String>();
    private String mCurrentValue;
    private Context mContext;
    private List<Map<String, String>> mPreferencesAttributes;
    private CoordinatorLayout mCoordinatorLayout;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private FloatingActionButton mButton;
    private ImageView mCurentPathIcon;
    private TextView  mCurrentPathTextView;
    private TextView preferenceSubtitle;
    private String subtitle;
    private String SUPPORTED_MIME_TYPES = "*/*";

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

            mPreferencesAttributes = pluginDetails.getPluginPreferences();
            if (mPreferencesAttributes != null){
                String key = arguments.getString("preferenceKey");
                for (Map<String, String> preferenceAttributes : mPreferencesAttributes) {
                    setHasOptionsMenu(true);
                    mCurrentValue = pluginDetails.getPluginPreferencesValues().get(key);
                    if (preferenceAttributes.get("key").equals(key)) {
                        if (preferenceAttributes.containsKey("mimeType") && !preferenceAttributes.get("mimeType").isEmpty())
                            SUPPORTED_MIME_TYPES = preferenceAttributes.get("mimeType");
                        subtitle = pluginDetails.getName() + " - " + preferenceAttributes.get("title");
                        if (preferenceAttributes.containsKey("defaultValue") && !preferenceAttributes.get("defaultValue").isEmpty()) {
                            String defaultPath = preferenceAttributes.get("defaultValue");
                            defaultPath = defaultPath.substring(0, defaultPath.lastIndexOf("/"));
                            File directory = new File(defaultPath);
                            File[] files = directory.listFiles();
                            if (files.length > 0) {
                                for (int i = 0; i < files.length; i++) {
                                    pathList.add(files[i].toString());
                                }
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
        View pathPreferencesList = inflater.inflate(R.layout.frag_plugins_path_preference,
                container, false);
        mCoordinatorLayout = pathPreferencesList.
                findViewById(R.id.path_preference_coordinator_layout);
        mRecyclerView = pathPreferencesList.findViewById(R.id.path_preferences);
        mRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        if (!pathList.isEmpty()) {
            mAdapter = new PathListAdapter(pathList, this);
            mRecyclerView.setAdapter(mAdapter);
        }

        mButton = pathPreferencesList.findViewById(R.id.plugins_path_preference_fab);

        mCurentPathIcon = pathPreferencesList.findViewById(R.id.current_path_item_icon);
        mCurrentPathTextView = pathPreferencesList.findViewById(R.id.current_path_item_name);
        preferenceSubtitle = pathPreferencesList.findViewById(R.id.plugin_setting_subtitle);

        ((HomeActivity) requireActivity()).
                setToolbarState(R.string.menu_item_plugin_list);

        return pathPreferencesList;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        preferenceSubtitle.setText(subtitle);
        mButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType(SUPPORTED_MIME_TYPES);
                startActivityForResult(intent, PATH_REQUEST_CODE);
            }
        });

        if (!mCurrentValue.isEmpty()) {
            mCurentPathIcon.setVisibility(View.VISIBLE);
            File file = new File(mCurrentValue);
            if (file.exists()) {
                if (isImage(mCurrentValue)) {
                    mCurrentPathTextView.setVisibility(View.INVISIBLE);
                    Drawable icon = Drawable.createFromPath(mCurrentValue);
                    if (icon != null) {
                        mCurentPathIcon.setImageDrawable(icon);
                    }
                }else {
                    mCurrentPathTextView.setVisibility(View.VISIBLE);
                    mCurrentPathTextView.setText(getFileName(mCurrentValue));
                }
            }
        }
        else{
            mCurentPathIcon.setVisibility(View.INVISIBLE);
            mCurrentPathTextView.setVisibility(View.INVISIBLE);
            mButton.performClick();
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
                            .map(file -> {return file.getAbsolutePath();})
                            .subscribe(new SingleObserver<String>() {
                                @Override
                                public void onSubscribe(Disposable d) {
                                }

                                @Override
                                public void onSuccess(String filename) {
                                    String[] ext = filename.split("\\.");
                                    Toast.makeText(mContext, "File: " + filename +
                                            " successfully read", Toast.LENGTH_LONG).show();
                                    setPreferencePath(filename);
                                }

                                @Override
                                public void onError(Throwable e) {
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
                    mCurentPathIcon.setVisibility(View.VISIBLE);
                    File file = new File(mCurrentValue);
                    Drawable icon = null;
                    if (file.exists() && isImage(mCurrentValue)) {
                        icon = Drawable.createFromPath(mCurrentValue);
                    }
                    if (icon != null) {
                        mCurentPathIcon.setImageDrawable(icon);
                    }
                    if(isImage(mCurrentValue))
                        mCurrentPathTextView.setVisibility(View.INVISIBLE);
                    else {
                        mCurrentPathTextView.setText(getFileName(mCurrentValue));
                        mCurrentPathTextView.setVisibility(View.VISIBLE);
                    }
                }
                else{
                    mCurentPathIcon.setVisibility(View.INVISIBLE);
                    mCurrentPathTextView.setVisibility(View.INVISIBLE);
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
