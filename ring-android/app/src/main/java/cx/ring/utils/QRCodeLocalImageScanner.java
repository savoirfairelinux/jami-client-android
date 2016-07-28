/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Romain Bertozzi <romain.bertozzi@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package cx.ring.utils;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.util.Hashtable;

public class QRCodeLocalImageScanner {
    public static final String TAG = QRCodeLocalImageScanner.class.getSimpleName();

    public static String readCodeFromBitmap(@NonNull Bitmap bitmapImage) {
        int[] intArray = new int[bitmapImage.getWidth() * bitmapImage.getHeight()];
        bitmapImage.getPixels(intArray, 0, bitmapImage.getWidth(), 0, 0, bitmapImage.getWidth(),
                bitmapImage.getHeight());
        LuminanceSource source = new RGBLuminanceSource(bitmapImage.getWidth(),
                bitmapImage.getHeight(), intArray);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Reader reader = new MultiFormatReader();
        try {
            Hashtable<DecodeHintType, Object> decodeHints = new Hashtable<>();
            decodeHints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            decodeHints.put(DecodeHintType.PURE_BARCODE, Boolean.FALSE);
            Result result = reader.decode(bitmap, decodeHints);
            return result.getText();
        } catch (NotFoundException|ChecksumException|FormatException|NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }
}
