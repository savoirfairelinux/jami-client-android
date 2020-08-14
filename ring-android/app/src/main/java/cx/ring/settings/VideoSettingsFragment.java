package cx.ring.settings;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.client.HomeActivity;
import cx.ring.fragments.GeneralAccountPresenter;
import cx.ring.fragments.GeneralAccountView;
import cx.ring.model.Account;
import cx.ring.mvp.BasePreferenceFragment;
import cx.ring.services.SharedPreferencesServiceImpl;
import cx.ring.utils.DeviceUtils;
import cx.ring.utils.Tuple;

public class VideoSettingsFragment extends BasePreferenceFragment<GeneralAccountPresenter> implements GeneralAccountView {
    private static final String TAG = VideoSettingsFragment.class.getName();
    private Preference resolutionPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        ((JamiApplication) requireActivity().getApplication()).getInjectionComponent().inject(this);
        super.onCreatePreferences(savedInstanceState, rootKey);
        presenter.init();

        PreferenceManager pm = getPreferenceManager();
        pm.setSharedPreferencesMode(Context.MODE_PRIVATE);
        pm.setSharedPreferencesName(SharedPreferencesServiceImpl.PREFS_VIDEO);
        setPreferencesFromResource(R.xml.video_prefs, rootKey);
        handleResolutionIcon(pm.getSharedPreferences().getString(SharedPreferencesServiceImpl.PREF_RESOLUTION, "720"));

        resolutionPref = findPreference(SharedPreferencesServiceImpl.PREF_RESOLUTION);
        if (resolutionPref != null) {
            resolutionPref.setOnPreferenceChangeListener((preference, newValue) -> {
                Log.w(TAG, "onPreferenceChange " + preference.getKey() + " " + newValue);
                handleResolutionIcon((String) newValue);
                return true;
            });
        }
    }

    private void handleResolutionIcon(String resolution) {
        if (resolution.equals("480"))
            resolutionPref.setIcon(R.drawable.baseline_videocam_24);
        else
            resolutionPref.setIcon(R.drawable.baseline_hd_24);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (DeviceUtils.isTablet(getContext())) {
            Toolbar toolbar = requireActivity().findViewById(R.id.main_toolbar);
            TextView title = toolbar.findViewById(R.id.contact_title);
            ImageView logo = toolbar.findViewById(R.id.contact_image);

            logo.setVisibility(View.GONE);
            title.setText(R.string.menu_item_settings);
            title.setTextSize(19);
            title.setTypeface(null, Typeface.BOLD);

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) title.getLayoutParams();
            params.removeRule(RelativeLayout.ALIGN_TOP);
            params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
            title.setLayoutParams(params);
        } else {
            ((HomeActivity) requireActivity()).setToolbarState(R.string.menu_item_settings);
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void addJamiPreferences(String accountId) {

    }

    @Override
    public void addSipPreferences() {

    }

    @Override
    public void accountChanged(@NonNull Account account) {

    }

    @Override
    public void finish() {

    }

    @Override
    public void updateResolutions(Tuple<Integer, Integer> maxResolution, int currentResolution) {

    }
}
