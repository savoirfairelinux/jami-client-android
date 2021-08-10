/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Aline Gondim Santos <aline.gondimsantos@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
import cx.ring.utils.AndroidFileUtils;

public class PathListAdapter extends RecyclerView.Adapter<PathListAdapter.PathViewHolder> {
    private List<String> mList;
    private PathListItemListener mListener;
    public static final String TAG = PathListAdapter.class.getSimpleName();

    PathListAdapter(List<String> pathList, PathListItemListener listener) {
        mList = pathList;
        mListener = listener;
    }

    @NonNull
    @Override
    public PathViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.frag_path_list_item, parent, false);
        return new PathViewHolder(view, mListener);
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
        private final ImageView pathIcon;
        private final TextView pathTextView;
        private String path;

        PathViewHolder(@NonNull View itemView, PathListItemListener listener) {
            super(itemView);
            // Views that should be updated by the update method
            pathIcon = itemView.findViewById(R.id.path_item_icon);
            pathTextView = itemView.findViewById(R.id.path_item_name);

            // Set listeners, we set the listeners on creation so details can be null
            itemView.setOnClickListener(v -> listener.onPathItemClicked(path));
        }

        // update the viewHolder view
        public void update(String s) {
            // Set the plugin icon
            File file = new File(s);
            if (file.exists()) {
                if (AndroidFileUtils.isImage(s)) {
                    pathTextView.setVisibility(View.GONE);
                    Drawable icon = Drawable.createFromPath(s);
                    if (icon != null) {
                        pathIcon.setImageDrawable(icon);
                    }
                } else {
                    pathTextView.setVisibility(View.VISIBLE);
                    pathTextView.setText(AndroidFileUtils.getFileName(s));
                }
            }
        }

        public void setDetails(String path) {
            this.path = path;
            update(this.path);
        }
    }

    public interface PathListItemListener {
        void onPathItemClicked(String path);
    }
}
