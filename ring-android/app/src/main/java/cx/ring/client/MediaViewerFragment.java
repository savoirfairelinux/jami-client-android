/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
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
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.client;

import android.app.Activity;
import android.app.Fragment;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.load.resource.bitmap.CenterInside;

import cx.ring.R;
import cx.ring.utils.GlideApp;
import cx.ring.utils.GlideOptions;

/**
 * A placeholder fragment containing a simple view.
 */
public class MediaViewerFragment extends Fragment {
    private final static String TAG = MediaViewerFragment.class.getSimpleName();

    private Uri mUri = null;

    protected ImageView mImage;

    private final GlideOptions PICTURE_OPTIONS = new GlideOptions().transform(new CenterInside());

    public MediaViewerFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.fragment_media_viewer, container, false);
        mImage = view.findViewById(R.id.image);
        showImage();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Activity activity = getActivity();
        if (activity == null)
            return;
        mUri = activity.getIntent().getData();
        showImage();
    }

    private void showImage() {
        if (mUri == null) {
            Log.w(TAG, "showImage(): null URI");
            return;
        }
        Activity a = getActivity();
        if (a == null) {
            Log.w(TAG, "showImage(): null Activity");
            return;
        }
        if (mImage == null) {
            Log.w(TAG, "showImage(): null image view");
            return;
        }
        GlideApp.with(a)
                .load(mUri)
                .apply(PICTURE_OPTIONS)
                .into(mImage);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }
}
