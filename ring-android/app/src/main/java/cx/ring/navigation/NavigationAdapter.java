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

class NavigationAdapter extends RecyclerView.Adapter<NavigationAdapter.NavigationView> {

    private List<NavigationItem> mDataset;
    private RingNavigationView.OnNavigationItemClicked mListener;
    int itemSelected;

    class NavigationView extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.navigation_item_icon)
        AppCompatImageView icon;

        @BindView(R.id.navigation_item_title)
        TextView title;

        @BindColor(R.color.color_primary_light)
        int tintColor;

        public NavigationView(View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener(this);
        }


        @Override
        public void onClick(View v) {
            if (mListener != null) {
                itemSelected = getAdapterPosition();
                mListener.onNavigationItemSelected(itemSelected);
                notifyDataSetChanged();
            }
        }
    }

    public void setOnNavigationItemClickedListener(RingNavigationView.OnNavigationItemClicked listener) {
        mListener = listener;
    }

    private class NavigationItem {
        int mResTitleId;
        int mResImageId;

        NavigationItem(int resTitle, int resId) {
            mResTitleId = resTitle;
            mResImageId = resId;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public NavigationAdapter() {
        mDataset = new ArrayList<>();
        mDataset.add(0, new NavigationItem(R.string.menu_item_home, R.drawable.ic_home_black));
        mDataset.add(1, new NavigationItem(R.string.menu_item_accounts, R.drawable.ic_group_black));
        mDataset.add(2, new NavigationItem(R.string.menu_item_settings, R.drawable.ic_settings_black));
        mDataset.add(3, new NavigationItem(R.string.menu_item_share, R.drawable.ic_share_black));
        mDataset.add(4, new NavigationItem(R.string.menu_item_about, R.drawable.ic_info_black));
    }

    // Create new views (invoked by the layout manager)
    @Override
    public NavigationAdapter.NavigationView onCreateViewHolder(ViewGroup parent,
                                                               int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_menu, parent, false);
        // set the view's size, margins, paddings and layout parameters

        NavigationView vh = new NavigationView(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(NavigationView holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.title.setText(mDataset.get(position).mResTitleId);
        holder.icon.setImageResource(mDataset.get(position).mResImageId);

        if (position == itemSelected) {
            Drawable wrapDrawable = DrawableCompat.wrap(holder.icon.getDrawable());
            DrawableCompat.setTint(wrapDrawable, holder.tintColor);
            holder.itemView.setBackgroundColor(Color.parseColor("#d5d5d5"));
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
            Drawable wrapDrawable = DrawableCompat.wrap(holder.icon.getDrawable());
            DrawableCompat.setTint(wrapDrawable, Color.BLACK);
        }

    }



    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}