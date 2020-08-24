package cx.ring.settings.pluginssettings;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

import cx.ring.R;

import static cx.ring.utils.AndroidFileUtils.getFileName;
import static cx.ring.utils.AndroidFileUtils.isImage;


public class PathListAdapter extends RecyclerView.Adapter<PathListAdapter.PathViewHolder> {
    private List<String> mList;
    private PathListItemListener listener;
    private Drawable icon;
    public static final String TAG = PathListAdapter.class.getSimpleName();

    PathListAdapter(List<String> pathList, PathListItemListener listener) {
        this.mList = pathList;
        this.listener = listener;
    }


    @NonNull
    @Override
    public PathViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.frag_path_list_item, parent, false);

        return new PathViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull PathViewHolder holder, int position) {
        holder.setDetails(mList.get(position));
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public void updatePluginsList(List<String> listPaths) {
        mList = listPaths;
        notifyDataSetChanged();
    }

    class PathViewHolder extends RecyclerView.ViewHolder{
        private ImageView pathIcon;
        private TextView pathTextView;
        private String path;

        PathViewHolder(@NonNull View itemView, PathListItemListener listener) {
            super(itemView);
            // Views that should be updated by the update method
            pathIcon = itemView.findViewById(R.id.path_item_icon);
            pathTextView = itemView.findViewById(R.id.path_item_name);

            // Set listeners, we set the listeners on creation so details can be null
            itemView.setOnClickListener(v -> {
                    listener.onPathItemClicked(this.path);
            });
        }

        // update the viewHolder view
        public void update(String s) {
            // Set the plugin icon
            File file = new File(s);
            if (file.exists()) {
                if (isImage(s)) {
                    pathTextView.setVisibility(View.INVISIBLE);
                    icon = Drawable.createFromPath(s);
                    if (icon != null) {
                        pathIcon.setImageDrawable(icon);
                    }
                }else {
                    pathTextView.setVisibility(View.VISIBLE);
                    pathTextView.setText(getFileName(s));
                }
            }
        }

        public void setDetails(String path) {
            this.path = path;
            update(this.path);
        }
    }

    public interface PathListItemListener{
        void onPathItemClicked(String path);
    }
}
