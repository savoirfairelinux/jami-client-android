/*
 * Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Thibault Wittemberg <thibault.wittemberg@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.HashMap;

public class QRCodeUtils {

    private final static String TAG = QRCodeUtils.class.getName();

    private final static int QRCODE_IMAGE_SIZE = 300;
    private final static int QRCODE_IMAGE_PADDING = 0;

    /**
     * @param input uri to be displayed
     * @return the resulting data
     */
    public static QRCodeData encodeStringAsQRCodeData(String input, final int black, final int white) {

        if (input == null || input.isEmpty()) {
            return null;
        }

        QRCodeWriter qrWriter = new QRCodeWriter();
        BitMatrix qrImageMatrix;
        try {

            HashMap<EncodeHintType, Integer> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, QRCODE_IMAGE_PADDING);

            qrImageMatrix = qrWriter.encode(input, BarcodeFormat.QR_CODE, QRCODE_IMAGE_SIZE, QRCODE_IMAGE_SIZE, hints);
        } catch (WriterException e) {
            Log.e(TAG, "Error while encoding QR", e);
            return null;
        }

        int qrImageWidth = qrImageMatrix.getWidth();
        int qrImageHeight = qrImageMatrix.getHeight();
        int[] pixels = new int[qrImageWidth * qrImageHeight];

        for (int row = 0; row < qrImageHeight; row++) {
            int offset = row * qrImageWidth;
            for (int column = 0; column < qrImageWidth; column++) {
                pixels[offset + column] = qrImageMatrix.get(column, row) ? black : white;
            }
        }

        return new QRCodeData(pixels, qrImageWidth, qrImageHeight);
    }

    public static class QRCodeData {
        private int[] mData;
        private int mWidth;
        private int mHeight;

        public QRCodeData(int[] data, int width, int height) {
            mData = data;
            mWidth = width;
            mHeight = height;
        }

        public int[] getData() {
            return mData;
        }

        public int getWidth() {
            return mWidth;
        }

        public int getHeight() {
            return mHeight;
        }
    }

}