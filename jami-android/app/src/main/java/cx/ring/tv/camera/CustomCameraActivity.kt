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

import android.Manifest
import android.animation.Animator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
import androidx.core.content.ContextCompat
import cx.ring.R
import cx.ring.databinding.CamerapickerBinding
import cx.ring.utils.AndroidFileUtils
import cx.ring.utils.ContentUri
import java.io.File
import kotlin.math.max
import androidx.core.view.isVisible

class CustomCameraActivity : AppCompatActivity() {
    private var binding: CamerapickerBinding? = null
    private lateinit var cameraController: LifecycleCameraController
    private var activeRecording: Recording? = null

    private var mActionVideo = false
    private var mVideoFile: File? = null

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
            if (!cameraGranted) {
                Toast.makeText(this, getString(R.string.open_camera_error), Toast.LENGTH_LONG).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CamerapickerBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        if (intent.action != null) {
            mActionVideo = intent.action == MediaStore.ACTION_VIDEO_CAPTURE
        }

        setupCamera()
        setupUI()
    }

    private fun setupCamera() {
        cameraController = LifecycleCameraController(this)
        cameraController.setEnabledUseCases(LifecycleCameraController.IMAGE_CAPTURE or LifecycleCameraController.VIDEO_CAPTURE)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    cameraController.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    Toast.makeText(this, getString(R.string.open_camera_error), Toast.LENGTH_LONG).show()
                    finish()
                }
                cameraController.bindToLifecycle(this)
            } catch (e: Exception) {
                Log.e(TAG, "Use camera error", e)
            }
        }, ContextCompat.getMainExecutor(this))

        val previewView = PreviewView(this)
        previewView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        previewView.controller = cameraController
        binding!!.cameraPreview.addView(previewView, 0)
    }

    private fun setupUI() {
        val binding = binding ?: return
        binding.buttonVideo.isEnabled = true
        binding.buttonPicture.isEnabled = true

        if (mActionVideo) {
            binding.buttonVideo.visibility = View.VISIBLE
            binding.buttonVideo.setOnClickListener { toggleVideoRecording() }
            binding.buttonPicture.visibility = View.GONE
        } else {
            binding.buttonPicture.setOnClickListener { takePicture() }
        }

        binding.root.post {
            val endRadius = max(binding.root.width, binding.root.height)
            val x = binding.root.width / 2
            val y = binding.root.height / 2
            if (binding.loadClip.isVisible) {
                val anim = ViewAnimationUtils.createCircularReveal(
                    binding.loadClip, x, y, endRadius.toFloat(), 0f
                )
                anim.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animator: Animator) {}
                    override fun onAnimationEnd(animator: Animator) {
                        try {
                            binding.loadClip.visibility = View.GONE
                        } catch (e: Exception) {
                            Log.e(TAG, "Error hiding load clip", e)
                        }
                    }

                    override fun onAnimationCancel(animator: Animator) {}
                    override fun onAnimationRepeat(animator: Animator) {}
                })
                anim.duration = 600
                anim.startDelay = 50
                anim.start()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        if (mActionVideo) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissionsLauncher.launch(missing.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    private fun takePicture() {
        binding!!.buttonPicture.isEnabled = false

        val file = try {
            AndroidFileUtils.createImageFile(this)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.taking_picture_error), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        cameraController.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = ContentUri.getUriForFile(this@CustomCameraActivity, file)
                    setResult(RESULT_OK, Intent()
                        .putExtra(MediaStore.EXTRA_OUTPUT, uri)
                        .setType(TYPE_IMAGE))
                    finish()
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Error saving picture", exc)
                    Toast.makeText(this@CustomCameraActivity, getString(R.string.taking_picture_error), Toast.LENGTH_LONG).show()
                    setResult(RESULT_CANCELED)
                    finish()
                }
            }
        )
    }

    private fun toggleVideoRecording() {
        if (activeRecording != null) {
            stopVideoRecording()
        } else {
            startVideoRecording()
        }
    }

    private fun startVideoRecording() {
        mVideoFile = try {
            AndroidFileUtils.createVideoFile(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating video file", e)
            return
        }

        val outputOptions = FileOutputOptions.Builder(mVideoFile!!).build()

        val audioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        val pending = cameraController.startRecording(
            outputOptions,
            AudioConfig.create(audioPermission),
            ContextCompat.getMainExecutor(this)
        ) { event ->
            if (event is VideoRecordEvent.Finalize) {
                handleVideoFinalize(event)
            }
        }

        activeRecording = pending

        binding!!.buttonVideo.setImageResource(androidx.leanback.R.drawable.lb_ic_stop)
    }

    private fun stopVideoRecording() {
        activeRecording?.stop()
        activeRecording = null
        binding!!.buttonVideo.setImageResource(R.drawable.baseline_videocam_24)
    }

    private fun handleVideoFinalize(event: VideoRecordEvent.Finalize) {
        if (!event.hasError()) {
            val uri = ContentUri.getUriForFile(this, mVideoFile!!)
            setResult(RESULT_OK, Intent()
                .putExtra(MediaStore.EXTRA_OUTPUT, uri)
                .setType(TYPE_VIDEO))
            finish()
        } else {
            Log.e(TAG, "Video capture failed: ${event.error}")
            Toast.makeText(this, getString(R.string.starting_recorder_error), Toast.LENGTH_LONG).show()
            mVideoFile?.delete()
            finish()
        }
    }

    companion object {
        private const val TAG = "CustomCameraActivity"
        const val TYPE_IMAGE = "image/jpeg"
        const val TYPE_VIDEO = "video"
    }
}