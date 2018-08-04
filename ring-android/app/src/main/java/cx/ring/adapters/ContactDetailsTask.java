/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.adapters;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import cx.ring.R;
import cx.ring.model.CallContact;
import cx.ring.utils.BitmapUtils;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class ContactDetailsTask implements Runnable {
    static final String TAG = ContactDetailsTask.class.getSimpleName();
    private final static String MIME_TYPE_JPG = "image/jpg";
    private final static String MIME_TYPE_JPEG = "image/jpeg";
    private final static String MIME_TYPE_PNG = "image/png";
    private final static int ORIENTATION_LEFT = 270;
    private final static int ORIENTATION_RIGHT = 90;
    private final static int MAX_IMAGE_DIMENSION = 1024;
    private final CallContact mContact;
    private final ArrayList<DetailsLoadedCallback> mCallbacks = new ArrayList<>(1);
    private final int mViewWidth, mViewHeight;
    private Context mContext;

    public ContactDetailsTask(Context context, CallContact item, DetailsLoadedCallback cb) {
        mViewWidth = 0;
        mViewHeight = 0;

        mContact = item;
        mContext = context;
        addCallback(cb);
    }

    public static Bitmap loadContactPhoto(ContentResolver cr, long id) {
        if (id == -1) {
            return null;
        }
        Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
        InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(cr, uri, true);
        if (input == null) {
            return null;
        }
        return BitmapFactory.decodeStream(input);
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {

        final BitmapFactory.Options options = new BitmapFactory.Options();

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }

    public static Single<Bitmap> loadProfilePhotoFromUri(Context context, Uri uriImage) {
        return Single.fromCallable(() -> {
            InputStream is = context.getContentResolver().openInputStream(uriImage);
            BitmapFactory.Options dbo = new BitmapFactory.Options();
            dbo.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, dbo);
            is.close();

            int rotatedWidth, rotatedHeight;
            int orientation = getOrientation(context, uriImage);

            if (orientation == ORIENTATION_LEFT || orientation == ORIENTATION_RIGHT) {
                rotatedWidth = dbo.outHeight;
                rotatedHeight = dbo.outWidth;
            } else {
                rotatedWidth = dbo.outWidth;
                rotatedHeight = dbo.outHeight;
            }

            Bitmap srcBitmap;
            is = context.getContentResolver().openInputStream(uriImage);
            if (rotatedWidth > MAX_IMAGE_DIMENSION || rotatedHeight > MAX_IMAGE_DIMENSION) {
                float widthRatio = ((float) rotatedWidth) / ((float) MAX_IMAGE_DIMENSION);
                float heightRatio = ((float) rotatedHeight) / ((float) MAX_IMAGE_DIMENSION);
                float maxRatio = Math.max(widthRatio, heightRatio);

                // Create the bitmap from file
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = (int) maxRatio;
                srcBitmap = BitmapFactory.decodeStream(is, null, options);
            } else {
                srcBitmap = BitmapFactory.decodeStream(is);
            }
            is.close();

            if (orientation > 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(orientation);

                srcBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(),
                        srcBitmap.getHeight(), matrix, true);
            }
            return srcBitmap;
        }).subscribeOn(Schedulers.io());
    }

    public static int getOrientation(Context context, Uri photoUri) {
        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[]{MediaStore.Images.ImageColumns.ORIENTATION}, null, null, null);

        if (cursor.getCount() != 1) {
            return -1;
        }

        cursor.moveToFirst();
        return cursor.getInt(0);
    }

    public void addCallback(DetailsLoadedCallback cb) {
        synchronized (mCallbacks) {
            if (cb == null) {
                return;
            }
            mCallbacks.add(cb);
        }
    }

    @Override
    public void run() {
        if (mContact == null) {
            return;
        }
        Log.i(TAG, "ContactDetailsTask run " + mContact.getId() + " " + mContact.getDisplayName());

        final Bitmap externalBMP;

        if (!mContact.detailsLoaded && !mContact.getPhones().isEmpty()) {
            String username = mContact.getPhones().get(0).getNumber().getRawRingId();
            Log.d(TAG, "getPhones not empty. Username : " + username);
            VCard vcard = VCardUtils.loadPeerProfileFromDisk(mContext.getFilesDir(), username + ".vcf");
            mContact.setVCardProfile(vcard);
        }

        byte[] photo = mContact.getPhoto();
        if (photo != null) {
            externalBMP = BitmapUtils.cropImageToCircle(photo);

        } else if (mContact.getId() > 0) {
            Bitmap photoBmp;
            try {
                photoBmp = loadContactPhoto(mContext.getContentResolver(), mContact.getId());
            } catch (IllegalArgumentException e) {
                photoBmp = null;
            }

            if (photoBmp == null) {
                photoBmp = decodeSampledBitmapFromResource(mContext.getResources(), R.drawable.ic_contact_picture_fallback, mViewWidth, mViewHeight);
            }

            mContact.setPhoto(BitmapUtils.bitmapToBytes(photoBmp));
            externalBMP = BitmapUtils.cropImageToCircle(photoBmp);
            photoBmp.recycle();
        } else {
            externalBMP = decodeSampledBitmapFromResource(mContext.getResources(), R.drawable.ic_contact_picture_fallback, mViewWidth, mViewHeight);
        }

        synchronized (mCallbacks) {
            for (DetailsLoadedCallback cb : mCallbacks) {
                cb.onDetailsLoaded(externalBMP, mContact.getDisplayName(), mContact.getRingUsername());
            }
            mCallbacks.clear();
        }
    }

    public interface DetailsLoadedCallback {
        void onDetailsLoaded(Bitmap bmp, String formattedName, String username);
    }
}
