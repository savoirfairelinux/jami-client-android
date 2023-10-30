/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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
import android.content.res.TypedArray
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.Surface
import kotlin.jvm.JvmOverloads
import android.view.TextureView
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.utils.DeviceUtils
import cx.ring.utils.ScalableType
import cx.ring.utils.ScaleManager
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.daemon.JamiServiceJNI
import net.jami.services.HardwareService

/**
 * A [TextureView] that can be adjusted to a specified aspect ratio.
 */
class VideoSinkView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0, defStyleRes: Int = 0)
    : TextureView(context, attrs, defStyle, defStyleRes), TextureView.SurfaceTextureListener {
    private var ratioWidth = 0
    private var ratioHeight = 0
    private var sinkId: String? = null
    private var nativeWindow: Long = -1
    private var surface: Surface? = null
    private var fitToContent: Boolean = true

    var videoListener: (Boolean) -> Unit = {}

    private val disposableBag = CompositeDisposable()

    val hardwareService: HardwareService? = JamiApplication.instance?.hardwareService

    init {
        val a:TypedArray = context.obtainStyledAttributes(attrs, R.styleable.VideoSinkView, defStyle, defStyleRes)
        fitToContent = a.getBoolean(R.styleable.VideoSinkView_fitToContent, true)
        setAspectRatio(a.getDimensionPixelSize(R.styleable.VideoSinkView_ratioWidth, 0),
            a.getDimensionPixelSize(R.styleable.VideoSinkView_ratioHeight, 0))
        a.recycle()
    }

    override fun onAttachedToWindow() {
        if (!isInEditMode) {
            surfaceTextureListener = this
        }
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        stopSink()
        super.onDetachedFromWindow()
        surfaceTextureListener = null
        surface = null
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    private fun setAspectRatio(width: Int, height: Int) {
        require(!(width < 0 || height < 0)) { "Size cannot be negative." }
        val empty = width == 0 || height == 0
        if (!empty && ratioWidth != width || ratioHeight != height) {
            Log.w(TAG, "setAspectRatio: $sinkId $width $height")
            ratioWidth = width
            ratioHeight = height
            requestLayout()
            if (!fitToContent) {
                configureTransform(width, height)
            }
        }
        isVisible = !empty
        videoListener.invoke(!empty)
    }

    fun setFitToContent(fit: Boolean) {
        if (fitToContent != fit) {
            fitToContent = fit
            if(!isInLayout)
                requestLayout()
            else
                doOnPreDraw { requestLayout() }
            if (nativeWindow != -1L)
                configureTransform(ratioWidth, ratioHeight)
        }
    }

    fun setSinkId(id: String?) {
        if (sinkId != id) {
            stopSink()
            Log.w(TAG, "setSinkId: $id")
            sinkId = id
            if (id != null)
                startSink(id)
        }
    }

    private fun startSink(id: String) {
        Log.w(TAG, "startSink: $id")
        val nw = nativeWindow
        if (nw != -1L) {
            disposableBag.add(hardwareService!!.connectSink(id, nw)
                .observeOn(DeviceUtils.uiScheduler)
                .subscribe { size -> setAspectRatio(size.first, size.second) })
        } else {
            disposableBag.add(hardwareService!!.getSinkSize(id)
                .observeOn(DeviceUtils.uiScheduler)
                .subscribe { c -> setAspectRatio(c.first, c.second) })
        }
    }

    private fun stopSink() {
        Log.w(TAG, "stopSink: $sinkId")
        disposableBag.clear()
        videoListener.invoke(false)
    }

    private fun configureTransform(textureWidth: Int, textureHeight: Int) {
        if (!fitToContent) {
            setTransform(ScaleManager(Size(width, height), Size(textureWidth, textureHeight)).getScaleMatrix(ScalableType.CENTER_CROP))
        } else {
            setTransform(null)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if (0 == ratioWidth || 0 == ratioHeight) {
            setMeasuredDimension(0, 0)
        } else {
            if (fitToContent) {
                val ra = height * ratioWidth
                val rb = width * ratioHeight
                if (rb < ra) {
                    setMeasuredDimension(width, rb / ratioWidth)
                } else {
                    setMeasuredDimension(ra / ratioHeight, height)
                }
            } else {
                setMeasuredDimension(width, height)
            }
        }
    }

    override fun onSurfaceTextureAvailable(s: SurfaceTexture, width: Int, height: Int) {
        if (surface == null) {
            surface = Surface(s)
            nativeWindow = JamiServiceJNI.acquireNativeWindow(surface)
            JamiServiceJNI.setNativeWindowGeometry(nativeWindow, ratioWidth, ratioHeight)
            configureTransform(ratioWidth, ratioHeight)
        }
        sinkId?.let { startSink(it) }
    }

    override fun onSurfaceTextureSizeChanged(s: SurfaceTexture, width: Int, height: Int) {
        configureTransform(ratioWidth, ratioHeight)
        if (surface != null) {
            JamiServiceJNI.setNativeWindowGeometry(nativeWindow, ratioWidth, ratioHeight)
        }
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        stopSink()
        surface?.let { s ->
            JamiServiceJNI.releaseNativeWindow(nativeWindow)
            s.release()
            surface = null
            nativeWindow = -1
        }
        sinkId?.let { startSink(it) }
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
    }

    companion object {
        val TAG = VideoSinkView::class.simpleName!!
    }
}
