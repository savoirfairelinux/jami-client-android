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
import cx.ring.R
import cx.ring.utils.BitmapUtils
import kotlin.math.min

class AvatarView @JvmOverloads constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0, defStyleRes: Int = 0) :
    View(context, attrs, defStyleAttr, defStyleRes) {

    private var uri: String? = null
    private var username: String? = null
    private var displayName: String? = null
    private var avatar: Drawable? = null
    private var avatarDrawable: AvatarDrawable
    private var cropCircle: Boolean = true

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AvatarView)
        uri = typedArray.getString(R.styleable.AvatarView_uri)
        username = typedArray.getString(R.styleable.AvatarView_username)
        displayName = typedArray.getString(R.styleable.AvatarView_displayName)
        avatar = typedArray.getDrawable(R.styleable.AvatarView_avatar)
        cropCircle = typedArray.getBoolean(R.styleable.AvatarView_cropCircle, true)
        typedArray.recycle()
        avatarDrawable = AvatarDrawable.Builder()
            .withId(uri)
            .withNameData(displayName, username)
            .withPhoto(avatar?.let { BitmapUtils.drawableToBitmap(it) })
            .withCircleCrop(cropCircle)
            .build(context)
        background = avatarDrawable
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