package cx.ring.account.pinInput

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import cx.ring.databinding.QrCodePinInputBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QrCodePinInputFragment : Fragment() {
    private val viewModel: QrCodePinInputViewModel by viewModels({ requireParentFragment() })
    private lateinit var binding: QrCodePinInputBinding

    private var cameraPermissionIsRefusedFlag = false // to not ask for permission again if refused
    // check the permission to use the camera
    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                showErrorPanel(isError = false)
                initializeBarcode()
            } else {
                cameraPermissionIsRefusedFlag = true
                showErrorPanel(isError = true)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        QrCodePinInputBinding.inflate(inflater, container, false).apply {
            binding = this
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.barcodeScanner.setStatusText("")
    }

    override fun onResume() {
        super.onResume()
        manageCameraPermission()
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeScanner.pause()
    }

    // the views below are the states of the QR code scanner : valid or error
    private fun showValid() {
        binding.apply {
            barcodeScanner.visibility = View.GONE
            background.visibility = View.VISIBLE
            qrCodeLayout.visibility = View.VISIBLE
            checkboxValid.visibility = View.VISIBLE
        }
    }

    private fun showError() {
        binding.apply {
            barcodeScanner.visibility = View.VISIBLE
            textInvalid.visibility = View.VISIBLE
        }
    }

    private fun showErrorPanel(isError: Boolean) {
        if (isError) {
            binding.barcodeScanner.visibility = View.GONE
            binding.errorMsgTxt.visibility = View.VISIBLE
        } else {
            binding.barcodeScanner.visibility = View.VISIBLE
            binding.errorMsgTxt.visibility = View.VISIBLE
        }
    }

    // this is the function that is called when the QR code is scanned
    private fun initializeBarcode() {
        binding.barcodeScanner.apply {
            binding.barcodeScanner.decoderFactory =
                DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
            decodeContinuous { it ->
                // check if the pin is a valid one (method in viewModel)
                viewModel.checkPin(it.text).let {
                    if (it == PinValidity.VALID) {
                        showValid()
                    } else {
                        showError()
                    }
                }
            }
        }
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun manageCameraPermission() {
        if (!hasCameraPermission()) {
            if (!cameraPermissionIsRefusedFlag) // if the permission is refused, don't ask again
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            else showErrorPanel(isError = true)
        }
        else{
            showErrorPanel(isError = false)
            Log.w("QrCodePinInputFragment", "p1")
            initializeBarcode()
            binding.barcodeScanner.resume()
        }
    }
}