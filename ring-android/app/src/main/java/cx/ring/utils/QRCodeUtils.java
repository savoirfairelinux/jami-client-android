/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.utils;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class QRCodeUtils {

    private final static String TAG = QRCodeUtils.class.getName();

    /**
     * @param input          uri to be displayed
     * @param qrWindowPixels the ImageView size that will contain the QRcode
     * @return the resulting image
     */
    public static Bitmap encodeStringAsQrBitmap(String input, int qrWindowPixels) {
        QRCodeWriter qrWriter = new QRCodeWriter();
        BitMatrix qrImageMatrix;
        try {
            qrImageMatrix = qrWriter.encode(input, BarcodeFormat.QR_CODE, qrWindowPixels, qrWindowPixels);
        } catch (WriterException e) {
            Log.e(TAG, "Error while encoding QR", e);
            return null;
        }

        int qrImageWidth = qrImageMatrix.getWidth();
        int qrImageHeight = qrImageMatrix.getHeight();
        int[] pixels = new int[qrImageWidth * qrImageHeight];

        final int BLACK = 0x00FFFFFF;
        final int WHITE = 0xFFFFFFFF;

        for (int row = 0; row < qrImageHeight; row++) {
            int offset = row * qrImageWidth;
            for (int column = 0; column < qrImageWidth; column++) {
                pixels[offset + column] = qrImageMatrix.get(column, row) ? BLACK : WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(qrImageWidth, qrImageHeight, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, qrImageWidth, 0, 0, qrImageWidth, qrImageHeight);
        return bitmap;
    }

}
