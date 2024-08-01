package cx.ring.utils.svg

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle

fun Bundle.getUri(key: String): Uri? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) getParcelable(key, Uri::class.java)
    else @Suppress("DEPRECATION") getParcelable(key) as? Uri?

fun Bundle.getBitmap(key: String): Bitmap? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        getParcelable(key, Bitmap::class.java)
    else @Suppress("DEPRECATION") getParcelable(key) as? Bitmap?