package cx.ring.services;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;

import cx.ring.utils.BitmapUtils;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import ezvcard.parameter.ImageType;
import ezvcard.property.Photo;
import ezvcard.property.RawProperty;

/**
 * Created by hdesousa on 02/01/18.
 */

public class VCardServiceImpl extends VCardService {

    private Context mContext;

    public VCardServiceImpl(Context context) {
        this.mContext = context;
    }

    @Override
    public VCard loadSmallVCard(String accountId) {
        VCard vcard = VCardUtils.loadLocalProfileFromDisk(mContext.getFilesDir(), accountId);
        if (vcard != null && !vcard.getPhotos().isEmpty()) {
            // Reduce photo size to fit in one DHT packet
            Bitmap photo = BitmapUtils.bytesToBitmap(vcard.getPhotos().get(0).getData());
            photo = BitmapUtils.reduceBitmap(photo, 30000);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.PNG, 100, stream);
            vcard.removeProperties(Photo.class);
            vcard.addPhoto(new Photo(stream.toByteArray(), ImageType.PNG));
            vcard.removeProperties(RawProperty.class);
        }
        return vcard;
    }
}
