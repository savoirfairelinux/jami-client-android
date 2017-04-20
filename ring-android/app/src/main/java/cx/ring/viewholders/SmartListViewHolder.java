/*
 *  Copyright (C) 2017 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.viewholders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.smartlist.SmartListViewModel;

public class SmartListViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.conv_participant)
    public TextView convParticipants;
    @BindView(R.id.conv_last_item)
    public TextView convStatus;
    @BindView(R.id.conv_last_time)
    public TextView convTime;
    @BindView(R.id.photo)
    public ImageView photo;
    @BindView(R.id.online)
    public ImageView online;

    public SmartListViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public void bind(final SmartListListeners clickListener, final SmartListViewModel smartListViewModel) {
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListener.onItemClick(smartListViewModel);

            }
        });
        itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                clickListener.onItemLongClick(smartListViewModel);
                return true;
            }
        });
        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListener.onPhotoClick(smartListViewModel);
            }
        });
    }

    public interface SmartListListeners {
        void onItemClick(SmartListViewModel smartListViewModel);

        void onItemLongClick(SmartListViewModel smartListViewModel);

        void onPhotoClick(SmartListViewModel smartListViewModel);
    }

}
