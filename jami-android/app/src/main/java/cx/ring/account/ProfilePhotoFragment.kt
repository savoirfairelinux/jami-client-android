package cx.ring.account

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import cx.ring.R
import cx.ring.databinding.FragAccProfilePhotoBinding
import cx.ring.utils.AndroidFileUtils

class ProfilePhotoFragment : Fragment() {

    private var _binding: FragAccProfilePhotoBinding? = null
    private val binding get() = _binding!!
    private lateinit var cameraController: LifecycleCameraController

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), R.string.camera_permission_denied, Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragAccProfilePhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraController = LifecycleCameraController(requireContext())
        cameraController.setEnabledUseCases(LifecycleCameraController.IMAGE_CAPTURE)
        cameraController.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        if (checkCameraPermission()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.captureButton.setOnClickListener { takePhoto() }
        binding.closeButton.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.flipCameraButton.setOnClickListener {
            cameraController.cameraSelector = if (cameraController.cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        cameraController.bindToLifecycle(viewLifecycleOwner)
        binding.viewFinder.controller = cameraController
    }

    private fun takePhoto() {
        val photoFile = try {
            AndroidFileUtils.createImageFile(requireContext())
        } catch (e: Exception) {
            Log.e(TAG, "Photo file creation failed", e)
            return
        }

        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = cameraController.cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
            .setMetadata(metadata)
            .build()

        cameraController.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    setFragmentResult(
                        REQUEST_KEY_PHOTO,
                        bundleOf(RESULT_URI to savedUri.toString())
                    )
                    parentFragmentManager.popBackStack()
                }
            }
        )
    }

    companion object {
        const val TAG = "ProfilePhotoFragment"
        const val REQUEST_KEY_PHOTO = "profile_photo_result"
        const val RESULT_URI = "photo_uri"
    }
}
