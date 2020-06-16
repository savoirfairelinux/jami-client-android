package cx.ring.plugins;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import cx.ring.R;
import cx.ring.settings.pluginssettings.PluginDetails;

public class PluginPreferences extends Preference {
    Context mContext;
    RelativeLayout resetButton;
    RelativeLayout uninstallButton;
    PluginDetails mPluginDetails;


    private View.OnClickListener resetClickListener;
    private View.OnClickListener installClickListener;

    public void setResetClickListener(View.OnClickListener clickListener) {
        this.resetClickListener = clickListener;
    }

    public void setInstallClickListener(View.OnClickListener clickListener) {
        this.installClickListener = clickListener;
    }

    public PluginPreferences(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public PluginPreferences(Context context, PluginDetails pluginDetails) {
        super(context);
        mContext = context;
        mPluginDetails = pluginDetails;
        setLayoutResource(R.layout.frag_plugin_settings);
    }

    public PluginPreferences(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PluginPreferences(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        resetButton = (RelativeLayout) holder.findViewById(R.id.plugin_setting_reset);
        uninstallButton = (RelativeLayout) holder.findViewById(R.id.plugin_setting_install);
        ImageView pluginIcon = (ImageView) holder.findViewById(R.id.plugin_setting_icon);
        TextView pluginName = (TextView) holder.findViewById(R.id.plugin_setting_title);

        pluginIcon.setImageDrawable(mPluginDetails.getIcon());
        pluginName.setText(mPluginDetails.getName());

        if(resetClickListener != null) {
            resetButton.setOnClickListener(resetClickListener);
        }

        if(installClickListener != null) {
            uninstallButton.setOnClickListener(installClickListener);
        }
    }

}
