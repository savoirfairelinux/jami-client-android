/*
 *  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  If you modify this program, or any covered work, by linking or
 *  combining it with the OpenSSL project's OpenSSL library (or a
 *  modified version of that library), containing parts covered by the
 *  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
 *  grants you additional permission to convey the resulting work.
 *  Corresponding Source for a non-source form of such a combination
 *  shall include the source code for the parts of OpenSSL used as well
 *  as that of the covered work.
 */

package cx.ring.adapters;

import java.io.InputStream;

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
import android.widget.ImageView;

public class ContactPictureTask implements Runnable {
    private ImageView view;
    private CallContact contact;
    private ContentResolver cr;
    private static int PADDING = 5;

    // private final String TAG = ContactPictureTask.class.getSimpleName();

    public ContactPictureTask(Context context, ImageView element, CallContact item) {
        contact = item;
        cr = context.getContentResolver();
        view = element;
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
        Bitmap photo_bmp;
        try {
            photo_bmp = loadContactPhoto(cr, contact.getId());
        } catch (IllegalArgumentException e) {
            photo_bmp = null;
        }

        int dpiPadding = (int) (PADDING * view.getResources().getDisplayMetrics().density);

        if (photo_bmp == null) {
            photo_bmp = decodeSampledBitmapFromResource(view.getResources(), R.drawable.ic_contact_picture, view.getWidth(), view.getHeight());
        }

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
        // internalCanvas.drawCircle(externalBMP.getWidth() / 2, externalBMP.getHeight() / 2, externalBMP.getWidth() / 2 - dpiPadding / 2, paintLine);
        // internalCanvas.drawOval(new RectF(PADDING, PADDING, externalBMP.getWidth() - dpiPadding, externalBMP.getHeight() - dpiPadding), paint);
        internalCanvas.drawOval(new RectF(0, 0, externalBMP.getWidth(), externalBMP.getHeight()), paint);

        view.post(new Runnable() {
            @Override
            public void run() {
                view.setImageBitmap(externalBMP);
                contact.setPhoto(externalBMP);
                view.invalidate();
            }
        });
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
