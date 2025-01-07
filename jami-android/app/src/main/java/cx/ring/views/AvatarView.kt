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
package cx.ring.views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.use
import cx.ring.R
import cx.ring.utils.BitmapUtils
import net.jami.model.Uri
import kotlin.math.min

class AvatarView @JvmOverloads constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0, defStyleRes: Int = 0) :
    View(context, attrs, defStyleAttr, defStyleRes) {

    private var avatarDrawable: AvatarDrawable? = null

    init {
        context.obtainStyledAttributes(attrs, R.styleable.AvatarView).use { typedArray ->
            val uri = typedArray.getString(R.styleable.AvatarView_uri) ?: return@use
            if (isInEditMode) {
                val username = typedArray.getString(R.styleable.AvatarView_username)
                val displayName = typedArray.getString(R.styleable.AvatarView_displayName)
                val avatar = typedArray.getDrawable(R.styleable.AvatarView_avatar)
                val cropCircle = typedArray.getBoolean(R.styleable.AvatarView_cropCircle, true)
                setAvatar(
                    AvatarDrawable.Builder()
                        .withUri(Uri.fromString(uri))
                        .withNameData(displayName, username)
                        .withPhoto(avatar?.let { BitmapUtils.drawableToBitmap(it) })
                        .withCircleCrop(cropCircle)
                        .build(context)
                )
            }
        }
    }

    /** Returns true if the avatar was set if it was previously empty. */
    fun setAvatar(avatar: AvatarDrawable?): Boolean {
        val noPrevious = avatarDrawable == null
        avatarDrawable = avatar
        avatar?.setBounds(paddingLeft, paddingTop, width - paddingRight, height - paddingBottom)
        invalidate()
        return noPrevious
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val measuredHeight = MeasureSpec.getSize(heightMeasureSpec)
        val paddingW = paddingLeft + paddingRight
        val paddingH = paddingTop + paddingBottom
        val size = min(measuredWidth - paddingW, measuredHeight - paddingH)
        setMeasuredDimension(size + paddingW, size + paddingH)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        avatarDrawable?.setBounds(
            paddingLeft,
            paddingTop,
            w - paddingRight,
            h - paddingBottom
        )
    }

    override fun onDraw(canvas: Canvas) {
        avatarDrawable?.draw(canvas)
    }
}