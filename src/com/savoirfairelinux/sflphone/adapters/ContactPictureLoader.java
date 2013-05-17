package com.savoirfairelinux.sflphone.adapters;

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

import com.savoirfairelinux.sflphone.R;

public class ContactPictureLoader implements Runnable
{
	private ImageView view;
	private long cid;
	private ContentResolver cr;
	private final String TAG = ContactPictureLoader.class.getSimpleName();

	public ContactPictureLoader(Context context, ImageView element, long contact_id)
	{
		cid = contact_id;
		cr = context.getContentResolver();
		view = element;
	}

	public static Bitmap loadContactPhoto(ContentResolver cr, long id)
	{
		Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
		InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(cr, uri);
		if (input == null) {
			return null;
		}
		return BitmapFactory.decodeStream(input);
	}

	@Override
	public void run()
	{
		Bitmap photo_bmp = loadContactPhoto(cr, cid);

		if (photo_bmp == null) {
			photo_bmp = BitmapFactory.decodeResource(view.getResources(), R.drawable.ic_contact_picture);
		}

		int w = photo_bmp.getWidth(), h = photo_bmp.getHeight();

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
			public void run()
			{
				view.setImageBitmap(externalBMP);
				view.invalidate();
			}
		});
	}
}
