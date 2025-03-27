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
package cx.ring.tv.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.util.Log
import android.view.TextureView
import java.lang.Exception

class CameraPreview(context: Context, private var mCamera: Camera?) : TextureView(context), TextureView.SurfaceTextureListener {
    private var mSurfaceTexture: SurfaceTexture? = null

    init {
        surfaceTextureListener = this
    }

    fun stop() {
        mCamera?.let { camera ->
            try {
                camera.stopPreview()
            } catch (e: Exception) {
                // intentionally left blank
            }
            camera.release()
            mCamera = null
            mSurfaceTexture?.release()
        }
        surfaceTextureListener = null
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        Log.w(TAG, "Surface texture available: $width x $height")
        startPreview(surface)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        Log.w(TAG, "Surface texture changed: $width x $height")
        startPreview(surface)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        Log.w(TAG, "Surface texture destroyed")
        stop()
        mSurfaceTexture = null
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // intentionally left blank
    }

    private fun startPreview(surfaceTexture: SurfaceTexture) {
        mCamera?.let { camera ->
            try {
                if (mSurfaceTexture != surfaceTexture) {
                    camera.setPreviewTexture(surfaceTexture)
                    mSurfaceTexture = surfaceTexture
                }
                camera.startPreview()
            } catch (e: Exception) {
                Log.w(TAG, "Error starting camera preview: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "CameraPreview"
    }
}