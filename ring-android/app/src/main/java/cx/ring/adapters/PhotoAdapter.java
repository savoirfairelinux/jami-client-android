package cx.ring.adapters;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v13.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import cx.ring.client.HomeActivity;
import cx.ring.views.MenuHeaderView;

/**
 * Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

public class PhotoAdapter extends BaseAdapter{

    private Cursor mCursor;
    private final Context mContext;
    private boolean mIsItemChecked;
    private int mPositionItem;

    private static final String TAG = PhotoAdapter.class.getSimpleName();

    public PhotoAdapter(Context c){
        Log.d(TAG, "PhotoAdapter");
        Uri media = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media._ID};

        try {
            mCursor = c.getContentResolver().query(media, projection, null, null, null);
            Log.d(TAG, "size " + mCursor.getCount());
        }
        catch (Exception e) {
            Log.w(TAG, e);
        }

        mContext = c;
        mIsItemChecked = false;
        Log.d(TAG, "PhotoAdapter finish");
    }

    @Override
    public int getCount() {
        return mCursor.getCount();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.d(TAG, "getView " + position);
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(mContext);
            try {
                mCursor.moveToPosition(position);
                int imageID = mCursor.getInt(mCursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + imageID));
                imageView.setImageBitmap(ThumbnailUtils.extractThumbnail(bitmap, 70, 70));
                imageView.setLayoutParams(new GridView.LayoutParams(70, 70));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(8, 8, 8, 8);
            }
            catch(Exception e){
                Log.w(TAG, e);
            }
        }
        else {
            imageView = (ImageView) convertView;
        }

        if (mIsItemChecked && mPositionItem == position) {
            imageView.setBackgroundColor(Color.RED);
        }
        else {
            imageView.setBackgroundColor(Color.TRANSPARENT);
        }

        return imageView;

    }

    public void setSelectedItem(int position){
        if(!mIsItemChecked || mPositionItem != position){
            mIsItemChecked = true;
            mPositionItem = position;
        }
        else{
            mIsItemChecked = false;
        }
        notifyDataSetChanged();
    }
}
