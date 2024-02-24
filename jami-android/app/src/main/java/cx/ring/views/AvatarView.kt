/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
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
package cx.ring.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.use
import cx.ring.R
import cx.ring.utils.BitmapUtils
import kotlin.math.min

class AvatarView @JvmOverloads constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0, defStyleRes: Int = 0) :
    View(context, attrs, defStyleAttr, defStyleRes) {
    init {
        context.obtainStyledAttributes(attrs, R.styleable.AvatarView).use { typedArray ->
            val uri = typedArray.getString(R.styleable.AvatarView_uri)
            if (uri != null || isInEditMode) {
                val username = typedArray.getString(R.styleable.AvatarView_username)
                val displayName = typedArray.getString(R.styleable.AvatarView_displayName)
                val avatar = typedArray.getDrawable(R.styleable.AvatarView_avatar)
                val cropCircle = typedArray.getBoolean(R.styleable.AvatarView_cropCircle, true)
                setAvatar(AvatarDrawable.Builder()
                    .withId(uri)
                    .withNameData(displayName, username)
                    .withPhoto(avatar?.let { BitmapUtils.drawableToBitmap(it) })
                    .withCircleCrop(cropCircle)
                    .build(context))
            }
        }
    }

    fun setAvatar(avatar: AvatarDrawable?) {
        background = avatar
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val measuredHeight = MeasureSpec.getSize(heightMeasureSpec)
        val paddingW = paddingLeft + paddingRight
        val paddingH = paddingTop + paddingBottom
        val size = min(measuredWidth - paddingW, measuredHeight - paddingH)
        setMeasuredDimension(size + paddingW, size + paddingH)
    }
}