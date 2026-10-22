/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
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

import android.animation.Animator
import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator

/** Keeps a thin ring softly rippling out of its place, to tell that something is live. */
object Pulse {
    private const val CYCLE_MS = 2500L
    // The ring grows and fades over this part of each cycle, then rests.
    private const val RIPPLE = 0.72f
    private const val SCALE = 1.45f
    private const val ALPHA = 0.55f

    fun start(ring: View, alpha: Float = ALPHA): Animator {
        val ease = DecelerateInterpolator(1.5f)
        fun frames(from: Float, to: Float) = arrayOf(
            Keyframe.ofFloat(0f, from),
            Keyframe.ofFloat(RIPPLE, to).apply { interpolator = ease },
            Keyframe.ofFloat(1f, to)
        )
        return ObjectAnimator.ofPropertyValuesHolder(ring,
            PropertyValuesHolder.ofKeyframe(View.SCALE_X, *frames(1f, SCALE)),
            PropertyValuesHolder.ofKeyframe(View.SCALE_Y, *frames(1f, SCALE)),
            PropertyValuesHolder.ofKeyframe(View.ALPHA, *frames(alpha, 0f))
        ).apply {
            duration = CYCLE_MS
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }
}
