package cx.ring.utils

import android.graphics.drawable.PictureDrawable
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import cx.ring.utils.svg.SvgSoftwareLayerSetter

enum class ImageFormat {
    SVG,
    PNG,
    JPG,
    GIF,
    UNKNOWN
}

object ImageViewUtils {
    val TAG = ImageViewUtils::class.simpleName!!

    fun setImage(imageView: ImageView, imageUrl: String) {
        when (getImageFormat(imageUrl)) {
            ImageFormat.SVG -> setSvgImage(imageView, imageUrl)
            ImageFormat.PNG, ImageFormat.JPG, ImageFormat.GIF -> setNormalImage(imageView, imageUrl)
            ImageFormat.UNKNOWN -> Log.w(TAG, "Unknown image format to load inside ImageView")
        }
    }

    /**
     * Get image format from image url.
     */
    private fun getImageFormat(imageUrl: String): ImageFormat {
        return when (imageUrl.substringAfterLast(".")) {
            "svg" -> ImageFormat.SVG
            "png" -> ImageFormat.PNG
            "jpg" -> ImageFormat.JPG
            "gif" -> ImageFormat.GIF
            else -> ImageFormat.UNKNOWN
        }
    }

    /**
     * Set SVG image to ImageView.
     */
    private fun setSvgImage(imageView: ImageView, svgUrl: String) {
        Glide.with(imageView.context)
            .`as`(PictureDrawable::class.java)
            .listener(SvgSoftwareLayerSetter())
            .load(svgUrl)
            .into(imageView)
        // Don't know why, but without this line, the image is not centered in the ImageView
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
    }

    /**
     * Set image to ImageView.
     * Accepted formats: png, jpg and gif.
     * Maybe other formats are supported by Glide, but not tested.
     */
    private fun setNormalImage(imageView: ImageView, imageUrl: String) {
        Glide.with(imageView.context)
            .load(imageUrl)
            .into(imageView)
    }
}