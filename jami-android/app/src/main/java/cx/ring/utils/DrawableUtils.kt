/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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
package cx.ring.utils

import android.content.Context
import android.graphics.drawable.Drawable

/**
 * Helper calls to manipulates Drawables
 */
object DrawableUtils {
    /**
     * Resize a drawable to the given width and height (not yet converted to dp)
     * @param drawable The drawable to resize
     * @param width The new width (not yet converted to dp)
     * @param height The new height (not yet converted to dp)
     */
    fun resizeDrawable(
        context: Context,
        drawable: Drawable,
        width: Int,
        height: Int,
    ): Drawable {
        return drawable.apply {
            setBounds(
                0,
                0,
                (context.resources.displayMetrics.density * width).toInt(),
                (context.resources.displayMetrics.density * height).toInt()
            )
        }
    }
}