/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
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
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import cx.ring.R;
import cx.ring.model.CallContact;
import cx.ring.utils.BitmapUtils;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import ezvcard.property.Photo;

public class ContactDetailsTask implements Runnable {
    static final String TAG = ContactDetailsTask.class.getSimpleName();

    private final WeakReference<ImageView> mImageViewWeakRef;
    private final WeakReference<TextView> mTextViewWeakRef;
    private final CallContact mContact;

    private Context mContext;

    private final ArrayList<DetailsLoadedCallback> mCallbacks = new ArrayList<>(1);

    private final int mViewWidth, mViewHeight;

    private final static String MIME_TYPE_JPG = "image/jpg";
    private final static String MIME_TYPE_JPEG = "image/jpeg";
    private final static String MIME_TYPE_PNG = "image/png";
    private final static int ORIENTATION_LEFT = 270;
    private final static int ORIENTATION_RIGHT = 90;
    private final static int MAX_IMAGE_DIMENSION = 200;

    public void addCallback(DetailsLoadedCallback cb) {
        synchronized (mCallbacks) {
            if (cb == null) {
                return;
            }
            mImageViewWeakRef.clear();
            mCallbacks.add(cb);
        }
    }

    public interface DetailsLoadedCallback {
        void onDetailsLoaded(Bitmap bmp, String formattedName, String username);
    }

    public ContactDetailsTask(Context context, ImageView element, CallContact item) {
        mContact = item;
        mContext = context;
        mImageViewWeakRef = new WeakReference<>(element);
        mTextViewWeakRef = new WeakReference<>(null);
        mViewWidth = element.getWidth();
        mViewHeight = element.getHeight();
    }

    public ContactDetailsTask(Context context, ImageView photoView, TextView nameView, CallContact item, DetailsLoadedCallback cb) {
        mContact = item;
        mContext = context;
        mViewWidth = photoView.getWidth();
        mViewHeight = photoView.getHeight();
        mImageViewWeakRef = new WeakReference<>(photoView);
        mTextViewWeakRef = new WeakReference<>(nameView);
        addCallback(cb);
    }

    public ContactDetailsTask(Context context, CallContact item, DetailsLoadedCallback cb) {
        mImageViewWeakRef = new WeakReference<>(null);
        mTextViewWeakRef = new WeakReference<>(null);
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

    @Override
    public void run() {
        if (mContact == null) {
            return;
        }
        Log.i(TAG, "ContactDetailsTask run " + mContact.getId() + " " + mContact.getDisplayName());

        final Bitmap externalBMP;
        String additionnalName = null;
        VCard vcard = null;

        if (!mContact.getPhones().isEmpty()) {
            String username = mContact.getPhones().get(0).getNumber().getHost();
            Log.d(TAG, "getPhones not empty. Username : " + username);
            vcard = VCardUtils.loadPeerProfileFromDisk(mContext.getFilesDir(), username + ".vcf");

            if (vcard != null && vcard.getFormattedName() != null) {
                if (!TextUtils.isEmpty(vcard.getFormattedName().getValue())) {
                    additionnalName = vcard.getFormattedName().getValue();
                }
            }
        }
        if (additionnalName == null) {
            additionnalName = mContact.getDisplayName();
        }

        if (vcard != null && !vcard.getPhotos().isEmpty()) {
            Photo tmp = vcard.getPhotos().get(0);
            Bitmap croppedBitmap;
            if (tmp != null && tmp.getData() != null) {
                croppedBitmap = BitmapUtils.cropImageToCircle(tmp.getData());
            } else {
                croppedBitmap = decodeSampledBitmapFromResource(mContext.getResources(), R.drawable.ic_contact_picture, mViewWidth, mViewHeight);
            }

            externalBMP = croppedBitmap != null ? croppedBitmap : decodeSampledBitmapFromResource(mContext.getResources(), R.drawable.ic_contact_picture, mViewWidth, mViewHeight);

        } else {
            Bitmap photoBmp;
            try {
                photoBmp = loadContactPhoto(mContext.getContentResolver(), mContact.getId());
            } catch (IllegalArgumentException e) {
                photoBmp = null;
            }

            if (photoBmp == null) {
                photoBmp = decodeSampledBitmapFromResource(mContext.getResources(), R.drawable.ic_contact_picture, mViewWidth, mViewHeight);
            }

            externalBMP = BitmapUtils.cropImageToCircle(photoBmp);
            photoBmp.recycle();
        }

        final String formattedName = additionnalName;
        mContact.setPhoto(BitmapUtils.bitmapToBytes(externalBMP));
        synchronized (mCallbacks) {
            final ImageView v = mImageViewWeakRef.get();
            final TextView textView = mTextViewWeakRef.get();

            mImageViewWeakRef.clear();
            if (v == null) {
                for (DetailsLoadedCallback cb : mCallbacks) {
                    cb.onDetailsLoaded(externalBMP, formattedName, mContact.getUsername());
                }
            } else {
                v.post(() -> {
                    v.setImageBitmap(externalBMP);
                    if (textView != null) {
                        textView.setText(formattedName);
                    }
                });
            }
            mCallbacks.clear();
        }
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

    public static Bitmap loadProfilePhotoFromUri(Context context, Uri uriImage) {
        try {
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

            String type = context.getContentResolver().getType(uriImage);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (type.equals(MIME_TYPE_PNG)) {
                srcBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            } else if (type.equals(MIME_TYPE_JPG) || type.equals(MIME_TYPE_JPEG)) {
                srcBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            }
            byte[] bMapArray = baos.toByteArray();
            baos.close();
            return BitmapFactory.decodeByteArray(bMapArray, 0, bMapArray.length);
        } catch (Exception e) {
            Log.e(TAG, "Error while loading photo from URI", e);
            return null;
        }
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
}
