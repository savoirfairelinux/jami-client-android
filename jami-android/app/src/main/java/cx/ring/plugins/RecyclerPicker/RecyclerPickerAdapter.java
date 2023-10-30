/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.plugins.RecyclerPicker;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cx.ring.R;

public class RecyclerPickerAdapter extends RecyclerView.Adapter<RecyclerPickerAdapter.ItemViewHolder> {
    private List<Drawable> mList;
    private final ItemClickListener mItemClickListener;
    private final int mItemLayoutResource;
    private final LayoutInflater mInflater;

    public RecyclerPickerAdapter(Context ctx, @LayoutRes int recyclerItemLayout, ItemClickListener itemClickListener) {
        mItemLayoutResource = recyclerItemLayout;
        mItemClickListener = itemClickListener;
        mInflater = LayoutInflater.from(ctx);
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(mItemLayoutResource, parent, false);
        view.setOnClickListener(mItemClickListener::onItemClicked);
        return new ItemViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.update(mList.get(position));
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mList != null ? mList.size() : 0;
    }

    public void updateData(List<Drawable> newlist) {
        this.mList = newlist;
        notifyDataSetChanged();
    }


    public static class ItemViewHolder extends RecyclerView.ViewHolder{
        private final ImageView itemImageView;
        private final View view;
        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            this.view = itemView;
            itemImageView = itemView.findViewById(R.id.item_image_view);
        }

        void update(Drawable drawable) {
            itemImageView.setImageDrawable(drawable);
        }

        public View getView() {
            return view;
        }
    }

    public interface ItemClickListener {
        void onItemClicked(View view);
    }

}
