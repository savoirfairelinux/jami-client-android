/*
 * Copyright (C) 2004-2018 Savoir-faire Linux Inc.
 *
 * Author: Pierre Duchemin <pierre.duchemin@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.tv.views;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.util.Log;

import com.bumptech.glide.request.target.BaseTarget;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.transition.Transition;

public class DetailsOverviewRowTarget extends BaseTarget<Drawable> {

    private static final String TAG = DetailsOverviewRowTarget.class.getSimpleName();
    private DetailsOverviewRow detailsOverviewRow;
    private Drawable drawable;

    public DetailsOverviewRowTarget(DetailsOverviewRow detailsOverviewRow, Drawable drawable) {
        if (detailsOverviewRow == null || drawable == null) {
            Log.d(TAG, "DetailsOverviewRowTarget: invalid parameter");
            return;
        }
        this.detailsOverviewRow = detailsOverviewRow;
        this.drawable = drawable;
    }

    @Override
    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
        this.detailsOverviewRow.setImageDrawable(resource);
    }

    @Override
    public void getSize(@NonNull SizeReadyCallback cb) {
        cb.onSizeReady(drawable.getMinimumWidth(), drawable.getMinimumHeight());
    }

    @Override
    public void removeCallback(@NonNull SizeReadyCallback cb) {
        // Do nothing, we never retain a reference to the callback
    }
}
