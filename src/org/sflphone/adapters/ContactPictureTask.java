/*
 *  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
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

package org.sflphone.adapters;

import java.io.InputStream;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.net.Uri;
import android.provider.ContactsContract;
import android.widget.ImageView;

import org.sflphone.R;

public class ContactPictureTask implements Runnable {
    private ImageView view;
    private long cid;
    private ContentResolver cr;
//    private final String TAG = ContactPictureTask.class.getSimpleName();

    public ContactPictureTask(Context context, ImageView element, long contact_id) {
        cid = contact_id;
        cr = context.getContentResolver();
        view = element;
    }

    public static Bitmap loadContactPhoto(ContentResolver cr, long id) {
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
        try{
        photo_bmp = loadContactPhoto(cr, cid);
        }catch(IllegalArgumentException e){
            photo_bmp = null;
        }
        if (photo_bmp == null) {
            photo_bmp = BitmapFactory.decodeResource(view.getResources(), R.drawable.ic_contact_picture);
        }

        int w = photo_bmp.getWidth(), h = photo_bmp.getHeight();
        if (w > h) {
            w = h;
        } else if (h > w) {
            h = w;
        }

        final Bitmap externalBMP = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        int radius = externalBMP.getWidth() / 2;
        Path path = new Path();

        path.addCircle(radius, radius, radius, Path.Direction.CW);
        Paint mPaintPath = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintPath.setStyle(Paint.Style.FILL);
        mPaintPath.setAntiAlias(true);
        Bitmap circle = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas circle_drawer = new Canvas(circle);
        circle_drawer.drawOval(new RectF(0, 0, w, h), mPaintPath);
        mPaintPath.setFilterBitmap(false);

        Canvas internalCanvas = new Canvas(externalBMP);
        internalCanvas.drawBitmap(photo_bmp, 0, 0, mPaintPath);
        mPaintPath.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
        internalCanvas.drawBitmap(circle, 0, 0, mPaintPath);

        view.post(new Runnable() {
            @Override
            public void run() {
                view.setImageBitmap(externalBMP);
                view.invalidate();
            }
        });
    }
}
