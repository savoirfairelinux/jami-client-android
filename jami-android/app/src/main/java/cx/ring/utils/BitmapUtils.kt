/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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
package cx.ring.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.Base64
import android.util.Log
import ezvcard.parameter.ImageType
import ezvcard.property.Photo
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Helper calls to manipulates Bitmaps
 */
object BitmapUtils {
    private val TAG = BitmapUtils::class.simpleName!!

    fun bitmapToPhoto(image: Bitmap) = Photo(bitmapToPng(image), ImageType.PNG)

    fun bitmapToPng(image: Bitmap): ByteArray = ByteArrayOutputStream()
        .apply { image.compress(Bitmap.CompressFormat.PNG, 100, this) }
        .toByteArray()

    fun bitmapToBytes(bmp: Bitmap): ByteArray = ByteBuffer.allocate(bmp.byteCount)
        .apply { bmp.copyPixelsToBuffer(this) }
        .array()

    fun base64ToBitmap(base64: String?): Bitmap? = if (base64 == null) null else try {
        bytesToBitmap(Base64.decode(base64, Base64.DEFAULT))
    } catch (e: IllegalArgumentException) {
        null
    }

    fun bytesToBitmap(imageData: ByteArray?): Bitmap? = if (imageData != null && imageData.isNotEmpty()) {
        BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
    } else null

    fun bytesToBitmap(data: ByteArray, maxSize: Int): Bitmap {
        // First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(data, 0, data.size, options)
        var width = options.outWidth
        var height = options.outHeight
        var scale = 1
        while (3 * width * height > maxSize) {
            scale *= 2
            width /= 2
            height /= 2
        }
        options.inSampleSize = scale
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeByteArray(data, 0, data.size, options)
    }

    fun reduceBitmap(bmp: Bitmap, size: Int): Bitmap {
        if (bmp.byteCount <= size) return bmp
        Log.d(TAG, "reduceBitmap: bitmap size before reduce " + bmp.byteCount)
        var height = bmp.height
        var width = bmp.width
        val minRatio = bmp.byteCount / size
        var ratio = 2
        while (ratio * ratio < minRatio) ratio *= 2
        height /= ratio
        width /= ratio
        val ret = Bitmap.createScaledBitmap(bmp, width, height, true)
        Log.d(TAG, "reduceBitmap: bitmap size after x" + ratio + " reduce " + ret.byteCount)
        return ret
    }

    fun createScaledBitmap(bitmap: Bitmap?, maxSize: Int): Bitmap {
        require(!(bitmap == null || maxSize < 0))
        var width = bitmap.height
        var height = bitmap.width
        if (width != height) {
            if (width < height) {
                // portrait
                height = maxSize
                width = maxSize * bitmap.width / bitmap.height
            } else {
                // landscape
                height = maxSize * bitmap.height / bitmap.width
                width = maxSize
            }
        } else {
            width = maxSize
            height = maxSize
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    fun drawableToBitmap(drawable: Drawable, size: Int = -1, padding: Int = 0): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: size
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: size
        val bitmap =
            Bitmap.createBitmap(width + 2 * padding, height + 2 * padding, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(padding, padding, canvas.width - padding, canvas.height - padding)
        drawable.draw(canvas)
        return bitmap
    }

    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight
                && halfWidth / inSampleSize >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun withPadding(drawable: Drawable, padding: Int): Drawable =
        LayerDrawable(arrayOf(drawable)).apply {
            setLayerInset(0, padding, padding, padding, padding)
        }
}