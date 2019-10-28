package cx.ring.settings;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cx.ring.R;

import static cx.ring.Plugins.PluginUtils.getPluginIcon;


public class PluginsListAdapter extends RecyclerView.Adapter<PluginsListAdapter.PluginViewHolder> {
    private List<PluginDetails> mList;
    private PluginListItemListener listener;
    public static final String TAG = PluginsListAdapter.class.getSimpleName();

    PluginsListAdapter(List<PluginDetails> pluginsList, PluginListItemListener listener) {
        this.mList = pluginsList;
        this.listener = listener;
    }


    @NonNull
    @Override
    public PluginViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.frag_plugins_list_item, parent, false);

        return new PluginViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull PluginViewHolder holder, int position) {
        holder.setDetails(mList.get(position));
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    class PluginViewHolder extends RecyclerView.ViewHolder{
        private ImageView pluginIcon;
        private TextView pluginNameTextView;
        private CheckBox pluginItemEnableCheckbox;
        private PluginDetails details = null;

        PluginViewHolder(@NonNull View itemView, PluginListItemListener listener) {
            super(itemView);
            // Views that should be updated by the update method
            pluginIcon = itemView.findViewById(R.id.plugin_item_icon);
            pluginNameTextView = itemView.findViewById(R.id.plugin_item_name);
            pluginItemEnableCheckbox = itemView.findViewById(R.id.plugin_item_enable_checkbox);

            // Set listeners, we set the listeners on creation so details can be null
            itemView.setOnClickListener(v -> {
                if (details != null) {
                    listener.onPluginItemClicked(details);
                }
            });

            pluginItemEnableCheckbox.setOnClickListener(
                    v -> {
                        if (details != null) {
                            this.details.setEnabled(!this.details.isEnabled());
                            listener.onPluginEnabled(details);
                        }
                    });
        }

        public void setDetails(PluginDetails details) {
            this.details = details;
            update(this.details);
        }

        // update the viewHolder view
        public void update(PluginDetails details) {
            pluginNameTextView.setText(details.getName());
            pluginItemEnableCheckbox.setChecked(details.isEnabled());
            // Set the plugin icon
            Drawable icon = getPluginIcon(details);
            if(icon != null) {
                pluginIcon.setImageDrawable(icon);
            }
        }
    }

    public interface PluginListItemListener{
        void onPluginItemClicked(PluginDetails pluginDetails);
        void onPluginEnabled(PluginDetails pluginDetails);
    }
}
