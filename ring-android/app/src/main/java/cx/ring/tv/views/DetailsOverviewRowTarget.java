/*
 * Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.widget.DetailsOverviewRow;
import android.util.Log;

import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

public class DetailsOverviewRowTarget extends SimpleTarget<Drawable> {

    private static final String TAG = DetailsOverviewRowTarget.class.getSimpleName();
    private DetailsOverviewRow detailsOverviewRow;

    public DetailsOverviewRowTarget(DetailsOverviewRow detailsOverviewRow) {
        if (detailsOverviewRow == null) {
            Log.d(TAG, "DetailsOverviewRowTarget: invalid parameter");
            return;
        }
        this.detailsOverviewRow = detailsOverviewRow;
    }

    @Override
    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
        this.detailsOverviewRow.setImageDrawable(resource);
    }
}
