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

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import cx.ring.R;
import cx.ring.model.CallContact;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.ImageView;

public class ContactPictureTask implements Runnable {
    static final String TAG = ContactPictureTask.class.getSimpleName();

    private final WeakReference<ImageView> view;
    private final CallContact contact;

    private ContentResolver cr;
    private final Resources res;
    private final ArrayList<PictureLoadedCallback> callbacks = new ArrayList<>(1);

    private final int vw, vh;

    public void addCallback(PictureLoadedCallback cb) {
        synchronized (callbacks) {
            view.clear();
            callbacks.add(cb);
        }
    }

    public interface PictureLoadedCallback {
        void onPictureLoaded(Bitmap bmp);
    };

    public ContactPictureTask(Context context, ImageView element, CallContact item) {
        contact = item;
        cr = context.getContentResolver();
        res = context.getResources();
        view = new WeakReference<>(element);
        vw = element.getWidth();
        vh = element.getHeight();
    }
    public ContactPictureTask(Context context, ImageView element, CallContact item, PictureLoadedCallback cb) {
        contact = item;
        cr = context.getContentResolver();
        res = context.getResources();
        vw = element.getWidth();
        vh = element.getHeight();
        view = new WeakReference<>(element);
        addCallback(cb);
    }

    public static Bitmap loadContactPhoto(ContentResolver cr, long id) {
        if(id == -1)
            return null;
        Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
        InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(cr, uri, true);
        if (input == null) {
            return null;
        }
        return BitmapFactory.decodeStream(input);
    }

    @Override
    public void run() {
        Log.i(TAG, "ContactPictureTask run " + contact.getId() + " " + vw + " " + vh);

        Bitmap photo_bmp;
        try {
            photo_bmp = loadContactPhoto(cr, contact.getId());
        } catch (IllegalArgumentException e) {
            photo_bmp = null;
        }
        cr = null;

        if (photo_bmp == null)
            photo_bmp = decodeSampledBitmapFromResource(res, R.drawable.ic_contact_picture, vw, vh);

        int w = photo_bmp.getWidth(), h = photo_bmp.getHeight();
        if (w > h) {
            w = h;
        } else if (h > w) {
            h = w;
        }

        final Bitmap externalBMP = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        BitmapShader shader;
        shader = new BitmapShader(photo_bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(shader);
        Canvas internalCanvas = new Canvas(externalBMP);

        Paint paintLine = new Paint();
        paintLine.setAntiAlias(true);
        paintLine.setDither(true);
        paintLine.setStyle(Style.STROKE);
        paintLine.setColor(Color.WHITE);
        internalCanvas.drawOval(new RectF(0, 0, externalBMP.getWidth(), externalBMP.getHeight()), paint);

        photo_bmp.recycle();

        contact.setPhoto(externalBMP);
        synchronized (callbacks) {
            final ImageView v = view.get();
            view.clear();
            if (v == null) {
                for (PictureLoadedCallback cb : callbacks) {
                    cb.onPictureLoaded(externalBMP);
                }
            } else {
                v.post(new Runnable() {
                    @Override
                    public void run() {
                        v.setImageBitmap(externalBMP);
                    }
                });
            }
            callbacks.clear();
        }
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        // options.inJustDecodeBounds = true;
        // BitmapFactory.decodeResource(res, resId, options);

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
}
