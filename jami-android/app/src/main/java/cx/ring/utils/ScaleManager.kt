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

import android.graphics.Matrix
import android.util.Size
import kotlin.math.max
import kotlin.math.min

enum class PivotPoint {
    LEFT_TOP, LEFT_CENTER, LEFT_BOTTOM, CENTER_TOP, CENTER, CENTER_BOTTOM, RIGHT_TOP, RIGHT_CENTER, RIGHT_BOTTOM
}
enum class ScalableType {
    NONE, FIT_XY, FIT_START, FIT_CENTER, FIT_END, LEFT_TOP, LEFT_CENTER, LEFT_BOTTOM, CENTER_TOP, CENTER, CENTER_BOTTOM, RIGHT_TOP, RIGHT_CENTER, RIGHT_BOTTOM, LEFT_TOP_CROP, LEFT_CENTER_CROP, LEFT_BOTTOM_CROP, CENTER_TOP_CROP, CENTER_CROP, CENTER_BOTTOM_CROP, RIGHT_TOP_CROP, RIGHT_CENTER_CROP, RIGHT_BOTTOM_CROP, START_INSIDE, CENTER_INSIDE, END_INSIDE
}

class ScaleManager(private val viewSize: Size, private val videoSize: Size) {
    fun getScaleMatrix(scalableType: ScalableType): Matrix = when (scalableType) {
        ScalableType.NONE -> noScale
        ScalableType.FIT_XY -> fitXY()
        ScalableType.FIT_CENTER -> fitCenter()
        ScalableType.FIT_START -> fitStart()
        ScalableType.FIT_END -> fitEnd()
        ScalableType.LEFT_TOP -> getOriginalScale(PivotPoint.LEFT_TOP)
        ScalableType.LEFT_CENTER -> getOriginalScale(PivotPoint.LEFT_CENTER)
        ScalableType.LEFT_BOTTOM -> getOriginalScale(PivotPoint.LEFT_BOTTOM)
        ScalableType.CENTER_TOP -> getOriginalScale(PivotPoint.CENTER_TOP)
        ScalableType.CENTER -> getOriginalScale(PivotPoint.CENTER)
        ScalableType.CENTER_BOTTOM -> getOriginalScale(PivotPoint.CENTER_BOTTOM)
        ScalableType.RIGHT_TOP -> getOriginalScale(PivotPoint.RIGHT_TOP)
        ScalableType.RIGHT_CENTER -> getOriginalScale(PivotPoint.RIGHT_CENTER)
        ScalableType.RIGHT_BOTTOM -> getOriginalScale(PivotPoint.RIGHT_BOTTOM)
        ScalableType.LEFT_TOP_CROP -> getCropScale(PivotPoint.LEFT_TOP)
        ScalableType.LEFT_CENTER_CROP -> getCropScale(PivotPoint.LEFT_CENTER)
        ScalableType.LEFT_BOTTOM_CROP -> getCropScale(PivotPoint.LEFT_BOTTOM)
        ScalableType.CENTER_TOP_CROP -> getCropScale(PivotPoint.CENTER_TOP)
        ScalableType.CENTER_CROP -> getCropScale(PivotPoint.CENTER)
        ScalableType.CENTER_BOTTOM_CROP -> getCropScale(PivotPoint.CENTER_BOTTOM)
        ScalableType.RIGHT_TOP_CROP -> getCropScale(PivotPoint.RIGHT_TOP)
        ScalableType.RIGHT_CENTER_CROP -> getCropScale(PivotPoint.RIGHT_CENTER)
        ScalableType.RIGHT_BOTTOM_CROP -> getCropScale(PivotPoint.RIGHT_BOTTOM)
        ScalableType.START_INSIDE -> startInside()
        ScalableType.CENTER_INSIDE -> centerInside()
        ScalableType.END_INSIDE -> endInside()
    }

    private fun getMatrix(sx: Float, sy: Float, px: Float, py: Float) = Matrix().apply {
        setScale(sx, sy, px, py)
    }

    private fun getMatrix(sx: Float, sy: Float, pivotPoint: PivotPoint): Matrix = when (pivotPoint) {
        PivotPoint.LEFT_TOP -> getMatrix(sx, sy, 0f, 0f)
        PivotPoint.LEFT_CENTER -> getMatrix(sx, sy, 0f, viewSize.height / 2f)
        PivotPoint.LEFT_BOTTOM -> getMatrix(sx, sy, 0f, viewSize.height.toFloat())
        PivotPoint.CENTER_TOP -> getMatrix(sx, sy, viewSize.width / 2f, 0f)
        PivotPoint.CENTER -> getMatrix(sx, sy, viewSize.width / 2f, viewSize.height / 2f)
        PivotPoint.CENTER_BOTTOM -> getMatrix(sx, sy, viewSize.width / 2f, viewSize.height.toFloat())
        PivotPoint.RIGHT_TOP -> getMatrix(sx, sy, viewSize.width.toFloat(), 0f)
        PivotPoint.RIGHT_CENTER -> getMatrix(sx, sy, viewSize.width.toFloat(), viewSize.height / 2f)
        PivotPoint.RIGHT_BOTTOM -> getMatrix(sx, sy, viewSize.width.toFloat(), viewSize.height.toFloat())
    }

    private val noScale: Matrix
        get() {
            val sx = videoSize.width / viewSize.width.toFloat()
            val sy = videoSize.height / viewSize.height.toFloat()
            return getMatrix(sx, sy, PivotPoint.LEFT_TOP)
        }

    private fun getFitScale(pivotPoint: PivotPoint): Matrix {
        var sx = viewSize.width.toFloat() / videoSize.width
        var sy = viewSize.height.toFloat() / videoSize.height
        val minScale = min(sx, sy)
        sx = minScale / sx
        sy = minScale / sy
        return getMatrix(sx, sy, pivotPoint)
    }

    private fun fitXY(): Matrix = getMatrix(1f, 1f, PivotPoint.LEFT_TOP)

    private fun fitStart(): Matrix = getFitScale(PivotPoint.LEFT_TOP)

    private fun fitCenter(): Matrix = getFitScale(PivotPoint.CENTER)

    private fun fitEnd(): Matrix = getFitScale(PivotPoint.RIGHT_BOTTOM)

    private fun getOriginalScale(pivotPoint: PivotPoint): Matrix {
        val sx = videoSize.width / viewSize.width.toFloat()
        val sy = videoSize.height / viewSize.height.toFloat()
        return getMatrix(sx, sy, pivotPoint)
    }

    private fun getCropScale(pivotPoint: PivotPoint): Matrix {
        val sx = viewSize.width.toFloat() / videoSize.width
        val sy = viewSize.height.toFloat() / videoSize.height
        val maxScale = max(sx, sy)
        return getMatrix(maxScale / sx, maxScale / sy, pivotPoint)
    }

    private fun startInside(): Matrix = if (videoSize.height <= viewSize.width && videoSize.height <= viewSize.height) {
        // video is smaller than view size
        getOriginalScale(PivotPoint.LEFT_TOP)
    } else {
        // either of width or height of the video is larger than view size
        fitStart()
    }

    private fun centerInside(): Matrix = if (videoSize.height <= viewSize.width && videoSize.height <= viewSize.height) {
        // video is smaller than view size
        getOriginalScale(PivotPoint.CENTER)
    } else {
        // either of width or height of the video is larger than view size
        fitCenter()
    }

    private fun endInside(): Matrix = if (videoSize.height <= viewSize.width && videoSize.height <= viewSize.height) {
        // video is smaller than view size
        getOriginalScale(PivotPoint.RIGHT_BOTTOM)
    } else {
        // either of width or height of the video is larger than view size
        fitEnd()
    }
}