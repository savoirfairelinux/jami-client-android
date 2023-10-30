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

import android.animation.Animator
import android.app.Activity
import android.media.MediaRecorder
import android.hardware.Camera.PictureCallback
import cx.ring.utils.ContentUriHandler
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import android.content.Intent
import android.hardware.Camera
import android.provider.MediaStore
import android.widget.Toast
import cx.ring.R
import android.os.Bundle
import android.view.ViewAnimationUtils
import android.hardware.Camera.CameraInfo
import android.media.CamcorderProfile
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import cx.ring.databinding.CamerapickerBinding
import cx.ring.utils.AndroidFileUtils
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception
import kotlin.math.max

class CustomCameraActivity : Activity() {
    private var binding: CamerapickerBinding? = null
    private val mDisposableBag = CompositeDisposable()
    private var cameraFront = -1
    private var cameraBack = -1
    private var currentCamera = 0
    private var recorder: MediaRecorder? = null
    private var mRecording = false
    private var mActionVideo = false
    private var mVideoFile: File? = null
    private var mCamera: Camera? = null
    private var mCameraPreview: CameraPreview? = null

    private val mPicture = PictureCallback { input: ByteArray, camera ->
        mDisposableBag.add(
            Single.fromCallable {
                if (mCameraPreview != null) mCameraPreview!!.stop()
                val file = AndroidFileUtils.createImageFile(this)
                FileOutputStream(file).use { out ->
                    out.write(input)
                    out.flush()
                }
                ContentUriHandler.getUriForFile(this, ContentUriHandler.AUTHORITY_FILES, file)
            }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ uri: Uri ->
                    setResult(RESULT_OK, Intent()
                            .putExtra(MediaStore.EXTRA_OUTPUT, uri)
                            .setType(TYPE_IMAGE))
                    finish()
                }) { e: Throwable ->
                    Log.e(TAG, "Error saving picture", e)
                    setResult(RESULT_CANCELED)
                    finish()
                })
    }

    private fun takePicture() {
        if (mRecording) releaseMediaRecorder()
        if (mCamera != null) {
            binding!!.buttonPicture.isEnabled = false
            binding!!.buttonVideo.visibility = View.GONE
            try {
                mCamera!!.takePicture(null, null, mPicture)
            } catch (e: Exception) {
                Toast.makeText(this, "Error taking picture", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun takeVideo() {
        if (mRecording) {
            releaseMediaRecorder()
            mCameraPreview!!.stop()
            val intent = Intent()
                .putExtra(MediaStore.EXTRA_OUTPUT, ContentUriHandler.getUriForFile(this, ContentUriHandler.AUTHORITY_FILES, mVideoFile!!))
                .setType(TYPE_VIDEO)
            setResult(RESULT_OK, intent)
            binding!!.buttonVideo.setImageResource(R.drawable.baseline_videocam_24)
            finish()
        } else {
            if (mCamera != null) {
                initRecorder()
                binding!!.buttonVideo.setImageResource(androidx.leanback.R.drawable.lb_ic_stop)
                binding!!.buttonPicture.visibility = View.GONE
            }
        }
        mRecording = !mRecording
    }

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CamerapickerBinding.inflate(layoutInflater)
        if (intent.action != null) {
            mActionVideo = intent.action == MediaStore.ACTION_VIDEO_CAPTURE
        }
        binding!!.buttonVideo.isEnabled = false
        binding!!.buttonPicture.isEnabled = false
        if (mActionVideo) {
            binding!!.buttonVideo.visibility = View.VISIBLE
        }
        setContentView(binding!!.root)
    }

    override fun onStart() {
        super.onStart()
        mDisposableBag.add(Single.fromCallable { cameraInstance }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ camera ->
                if (binding == null) {
                    camera.release()
                } else {
                    mCamera = camera
                    mCameraPreview = CameraPreview(this, camera)
                    binding!!.cameraPreview.addView(mCameraPreview, 0)
                    binding!!.buttonVideo.isEnabled = true
                    binding!!.buttonPicture.isEnabled = true
                    binding!!.buttonPicture.setOnClickListener { takePicture() }
                    binding!!.buttonVideo.setOnClickListener { takeVideo() }
                    val endRadius = max(binding!!.root.width, binding!!.root.height)
                    val x = binding!!.root.width / 2
                    val y = binding!!.root.height / 2
                    if (binding!!.loadClip.visibility == View.VISIBLE) {
                        val anim = ViewAnimationUtils.createCircularReveal(
                            binding!!.loadClip, x, y, endRadius.toFloat(), 0f
                        )
                        anim.addListener(object : Animator.AnimatorListener {
                            override fun onAnimationStart(animator: Animator) {}
                            override fun onAnimationEnd(animator: Animator) {
                                binding!!.loadClip.visibility = View.GONE
                            }

                            override fun onAnimationCancel(animator: Animator) {}
                            override fun onAnimationRepeat(animator: Animator) {}
                        })
                        anim.duration = 600
                        anim.startDelay = 50
                        anim.start()
                    }
                }
            }) { e: Throwable ->
                Toast.makeText(this, "Can't open camera", Toast.LENGTH_LONG).show()
                finish()
            })
    }

    override fun onStop() {
        super.onStop()
        if (mCameraPreview != null) {
            mCameraPreview!!.stop()
            mCameraPreview = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mDisposableBag.dispose()
        binding = null
        mCamera?.apply {
            release()
            mCamera = null
        }
    }

    private fun initVideo() {
        val numberCameras = Camera.getNumberOfCameras()
        if (numberCameras == 0) return
        val camInfo = CameraInfo()
        for (i in 0 until numberCameras) {
            Camera.getCameraInfo(i, camInfo)
            if (camInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                cameraFront = i
            } else {
                cameraBack = i
            }
        }
        currentCamera = if (cameraFront == -1) cameraBack else cameraFront
    }

    /**
     * Helper method to access the camera returns null if it cannot get the
     * camera or does not exist
     */
    private val cameraInstance: Camera
        get() {
            initVideo()
            return Camera.open(currentCamera)
        }

    private fun initRecorder() {
        val videoWidth = mCamera!!.parameters.previewSize.width
        val videoHeight = mCamera!!.parameters.previewSize.height
        mCamera?.unlock()
        recorder = MediaRecorder().apply {
            setCamera(mCamera)
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.DEFAULT)
            setProfile(CamcorderProfile.get(currentCamera, CamcorderProfile.QUALITY_HIGH))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    mVideoFile = AndroidFileUtils.createVideoFile(this@CustomCameraActivity)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                setOutputFile(mVideoFile)
            }
            setVideoSize(videoWidth, videoHeight)
        }
        prepareRecorder()
    }

    private fun prepareRecorder() {
        recorder!!.setPreviewDisplay(mCameraPreview!!.holder.surface)
        try {
            recorder!!.prepare()
            recorder!!.start()
        } catch (e: Exception) {
            Toast.makeText(this, "Error starting the recorder: " + e.localizedMessage, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun releaseMediaRecorder() {
        recorder?.apply {
            reset()
            release()
            recorder = null
        }
    }

    companion object {
        private const val TAG = "CustomCameraActivity"
        const val TYPE_IMAGE = "image/jpeg"
        const val TYPE_VIDEO = "video"
    }
}