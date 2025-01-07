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
package cx.ring.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfRenderer.Page
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import androidx.annotation.AttrRes
import ezvcard.parameter.ImageType
import ezvcard.property.Photo
import net.jami.utils.QRCodeUtils
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

    fun bitmapToBase64(bitmap: Bitmap?): String? {
        if (bitmap == null) return null
        return try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting Bitmap to Base64", e)
            null
        }
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

    /** Fits a page in a provided maximum space using the standard fit strategy to preserve ratio*/
    private fun pageRenderSize(page: Page, maxWidth: Int, maxHeight: Int): Pair<Int, Int> =
        if (maxHeight > 0) {
            if (maxWidth > 0) {
                val a = page.width * maxHeight
                val b = page.height * maxWidth
                if (a > b)
                    (a / page.height) to maxHeight
                else
                    maxWidth to (b / page.width)
            } else
                (maxHeight * page.width / page.height) to maxHeight
        } else if (maxWidth > 0) {
            maxWidth to (maxWidth * page.height / page.width)
        } else
            page.width to page.height

    private fun pageToBitmap(page: Page, maxWidth: Int, maxHeight: Int): Bitmap =
        pageRenderSize(page, maxWidth, maxHeight)
            .let { (w, h) -> Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888) }
            .apply { page.render(this, null, null, Page.RENDER_MODE_FOR_DISPLAY) }

    fun documentToBitmap(context: Context, uri: Uri, maxWidth: Int = -1, maxHeight: Int = -1): Bitmap? =
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                PdfRenderer(fd).use { doc ->
                    if (doc.pageCount == 0) null else
                        doc.openPage(0).use { pageToBitmap(it, maxWidth, maxHeight) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "documentToBitmap: ", e)
            null
        }

    /**
     * Generate an Android Adaptive Bitmap from the given drawable and size in pixels
     * Uses about 20% padding for the adaptive icon as per
     * https://developer.android.com/develop/ui/views/launch/icon_design_adaptive
     */
    fun drawableToAdaptiveBitmap(drawable: Drawable, size: Int): Bitmap =
        drawableToBitmap(drawable, size, size / 5)

    fun qrToBitmap(qrCodeData: QRCodeUtils.QRCodeData) =
        Bitmap.createBitmap(qrCodeData.width, qrCodeData.height, Bitmap.Config.ARGB_8888).apply {
            setPixels(qrCodeData.data, 0, qrCodeData.width, 0, 0, qrCodeData.width, qrCodeData.height)
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

    fun getColorFromAttribute(context: Context, @AttrRes attrColor: Int): Int {
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(attrColor, typedValue, true)
        return typedValue.data
    }

}