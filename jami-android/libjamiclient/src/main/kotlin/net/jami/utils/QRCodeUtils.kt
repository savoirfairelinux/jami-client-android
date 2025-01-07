/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.jami.utils

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import net.jami.utils.Log.e
import java.util.*

object QRCodeUtils {
    private val TAG = QRCodeUtils::class.simpleName!!
    private const val QRCODE_IMAGE_SIZE = 256
    private const val QRCODE_IMAGE_PADDING = 1

    /**
     * @param input uri to be displayed
     * @return the resulting data
     */
    fun encodeStringAsQRCodeData(input: String?, foregroundColor: Int, backgroundColor: Int): QRCodeData? {
        if (input == null || input.isEmpty()) {
            return null
        }
        val qrWriter = QRCodeWriter()
        val qrImageMatrix: BitMatrix
        try {
            val hints = HashMap<EncodeHintType, Int?>()
            hints[EncodeHintType.MARGIN] = QRCODE_IMAGE_PADDING
            qrImageMatrix = qrWriter.encode(input, BarcodeFormat.QR_CODE, QRCODE_IMAGE_SIZE, QRCODE_IMAGE_SIZE, hints)
        } catch (e: WriterException) {
            e(TAG, "Error while encoding QR", e)
            return null
        }
        val qrImageWidth = qrImageMatrix.getWidth()
        val qrImageHeight = qrImageMatrix.getHeight()
        val pixels = IntArray(qrImageWidth * qrImageHeight)
        for (row in 0 until qrImageHeight) {
            val offset = row * qrImageWidth
            for (column in 0 until qrImageWidth) {
                pixels[offset + column] = if (qrImageMatrix[column, row]) foregroundColor else backgroundColor
            }
        }
        return QRCodeData(pixels, qrImageWidth, qrImageHeight)
    }

    class QRCodeData(val data: IntArray, val width: Int, val height: Int)
}