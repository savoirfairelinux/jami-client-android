/**
 * Copyright (C) 2016 by Savoir-faire Linux
 * Author : Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 * <p>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.navigation;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;

class NavigationAdapter extends RecyclerView.Adapter<NavigationAdapter.NavigationItemView> {

    private List<RingNavigationView.NavigationItem> mDataset;
    private OnNavigationItemClicked mListener;
    private int mItemSelected;

    public interface OnNavigationItemClicked {
        void onNavigationItemClicked(int position);
    }

    NavigationAdapter(ArrayList<RingNavigationView.NavigationItem> menu) {
        mDataset = menu;
    }

    class NavigationItemView extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.navigation_item_icon)
        AppCompatImageView icon;

        @BindView(R.id.navigation_item_title)
        TextView title;

        @BindColor(R.color.color_primary_light)
        int tintColor;

        public NavigationItemView(View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                mItemSelected = getAdapterPosition();
                setSelection(mItemSelected);
                mListener.onNavigationItemClicked(mItemSelected);
            }
        }
    }

    public void setSelection(int position) {
        mItemSelected = position;
        notifyDataSetChanged();
    }

    public void setOnNavigationItemClickedListener(OnNavigationItemClicked listener) {
        mListener = listener;
    }

    @Override
    public NavigationItemView onCreateViewHolder(ViewGroup parent,
                                                 int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu, parent, false);
        return new NavigationItemView(v);
    }

    @Override
    public void onBindViewHolder(NavigationItemView holder, int position) {
        holder.title.setText(mDataset.get(position).mResTitleId);
        holder.icon.setImageResource(mDataset.get(position).mResImageId);

        if (position == mItemSelected) {
            Drawable wrapDrawable = DrawableCompat.wrap(holder.icon.getDrawable());
            DrawableCompat.setTint(wrapDrawable, holder.tintColor);
            holder.itemView.setBackgroundColor(Color.parseColor("#d5d5d5"));
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
            Drawable wrapDrawable = DrawableCompat.wrap(holder.icon.getDrawable());
            DrawableCompat.setTint(wrapDrawable, Color.BLACK);
        }
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}