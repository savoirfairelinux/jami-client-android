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
package cx.ring.tv.camera

import android.content.Context
import android.hardware.Camera
import android.view.SurfaceView
import android.view.SurfaceHolder
import java.lang.Exception

class CameraPreview(context: Context, private var mCamera: Camera?) : SurfaceView(context), SurfaceHolder.Callback {
    fun stop() {
        mCamera?.let { camera ->
            try {
                camera.stopPreview()
            } catch (e: Exception) {
                // intentionally left blank
            }
            camera.release()
            mCamera = null
        }
    }

    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        mCamera?.let { camera ->
            try {
                camera.setPreviewDisplay(surfaceHolder)
                camera.startPreview()
            } catch (e: Exception) {
                // left blank for now
            }
        }
    }

    override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
        stop()
    }

    override fun surfaceChanged(surfaceHolder: SurfaceHolder, format: Int, width: Int, height: Int) {
        mCamera?.let { camera ->
            try {
                camera.setPreviewDisplay(surfaceHolder)
                camera.startPreview()
            } catch (e: Exception) {
                // intentionally left blank for a test
            }
        }
    }

    // Constructor that obtains context and camera
    init {
        holder.addCallback(this)
    }
}