package cx.ring.settings;
import cx.ring.R;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;



public class PluginsListAdapter extends RecyclerView.Adapter<PluginsListAdapter.PluginViewHolder> {
    private List<String> mList;

    public PluginsListAdapter(List<String> pluginsList) {
        this.mList = pluginsList;
    }

    @NonNull
    @Override
    public PluginViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.frag_plugins_list_item, parent, false);

        PluginViewHolder pluginViewHolder = new PluginViewHolder(view);

        return pluginViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull PluginViewHolder holder, int position) {
        holder.pluginNameTextView.setText(mList.get(position));
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    class PluginViewHolder extends RecyclerView.ViewHolder{
        // each data item is just a string in this case
        public TextView pluginNameTextView;
        public PluginViewHolder(@NonNull View itemView) {
            super(itemView);
            pluginNameTextView = itemView.findViewById(R.id.plugin_item_name);
        }
    }
}
