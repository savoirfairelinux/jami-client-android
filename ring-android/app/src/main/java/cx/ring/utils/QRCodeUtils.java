package cx.ring.utils;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * Created by twittemberg on 16-10-19.
 */

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
