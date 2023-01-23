@file:Suppress("NOTHING_TO_INLINE")
package cx.ring.utils

import android.util.Size

public inline fun Size.surface() = width * height

public inline fun Size.contains(w: Int, h: Int) = w < width && h < height
public inline fun Size.contains(s: Size) = contains(s.width, s.height)

public inline fun Size.flip(f: Boolean? = null) = if (f == null || f == true) Size(height, width) else this

public inline fun Size.round(n: Int = 16) = Size(width / n * n, height / n * n)

public inline fun Size.fitOrScale(w: Int, h: Int): Size {
    val ra = height * w
    val rb = width * h
    return when {
        (rb > ra) -> Size(ra / h, height)
        else -> Size(width, rb / w)
    }
}

public inline fun Size.fit(w: Int, h: Int) = if (contains(w, h)) Size(w, h) else fitOrScale(w, h)

public inline fun Size.fit(s: Size) = if (contains(s)) s else fitOrScale(s.width, s.height)
