package cx.ring.adapters;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import cx.ring.utils.CropImageUtils;

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
        String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.ORIENTATION};

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
    @Nullable
    public ImageView getItem(int position) {
        ImageView image;

        try {
            image = new ImageView(mContext);
            int imageID = mCursor.getInt(mCursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
            int imageOrientation = mCursor.getInt(mCursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION));
            Uri uriFile = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + imageID);

            Bitmap bitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), uriFile);

            image.setImageBitmap(bitmap);
            image.setRotation(imageOrientation);
        }
        catch(Exception e){
            Log.w(TAG, e);
            image = null;
        }

        return image;
    }

    @Nullable
    public ImageView getItemSelected(){
        ImageView image;
        if(mIsItemChecked){
            image = getItem(mPositionItem);
        }
        else{
            image = null;
        }
        return image;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
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
                int imageOrientation = mCursor.getInt(mCursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION));
                Log.d(TAG, "orientation " + imageOrientation);
                Uri uriFile = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + imageID);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                AssetFileDescriptor fileDescriptor = mContext.getContentResolver().openAssetFileDescriptor(uriFile, "r");
                BitmapFactory.decodeFileDescriptor(fileDescriptor.getFileDescriptor(), null, options);
                options.inSampleSize = calculateInSampleSize(options, 100, 100);
                options.inJustDecodeBounds = false;
                Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor.getFileDescriptor(), null, options);
                bitmap = CropImageUtils.cropImageToCircle(bitmap);

                imageView.setImageBitmap(bitmap);
                imageView.setLayoutParams(new GridView.LayoutParams(100, 100));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(8, 8, 8, 8);
                imageView.setRotation(imageOrientation);
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
