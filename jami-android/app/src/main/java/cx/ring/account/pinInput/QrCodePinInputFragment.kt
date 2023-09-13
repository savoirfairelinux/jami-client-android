package cx.ring.account.pinInput

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import cx.ring.R
import cx.ring.databinding.QrCodePinInputBinding
import dagger.hilt.android.AndroidEntryPoint
import net.jami.utils.Log

@AndroidEntryPoint
class QrCodePinInputFragment : Fragment() {
    private val viewModel: QrCodePinInputViewModel by viewModels({ requireParentFragment() })
    private lateinit var binding: QrCodePinInputBinding


    companion object {
        private val TAG = QrCodePinInputFragment::class.simpleName!!
    }

    // inflate the layout and link with viewModel
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        QrCodePinInputBinding.inflate(inflater, container, false).apply {
            binding = this
        }.root


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.barcodeScanner.setStatusText("")
        super.onViewCreated(view, savedInstanceState)
        checkPermission()
    }

    override fun onResume() {
        super.onResume()
        if (checkPermission()) {
            binding.barcodeScanner.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeScanner.pause()
    }

    // the views below are the states of the QR code scanner
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

    private fun showErrorPanel(@StringRes textResId: Int) {
        binding.barcodeScanner.visibility = View.GONE
    }

    private fun displayNoPermissionsError() {
        showErrorPanel(R.string.error_scan_no_camera_permissions)
    }

    // this is the function that is called when the QR code is scanned
    private fun initializeBarcode() {
        Log.w(TAG, "initializeBarcode: ")
        binding.barcodeScanner.apply {
            binding.barcodeScanner.decoderFactory =
                DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
            //barcodeView.initializeFromIntent(getActivity().getIntent());
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

    private fun hideErrorPanel() {
        binding.barcodeScanner.visibility = View.VISIBLE
    }

    // check the permission to use the camera
    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                hideErrorPanel()
                initializeBarcode()
            } else {
                displayNoPermissionsError()
            }
        }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun checkPermission(): Boolean {
        if (!hasCameraPermission()) {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
            return false
        }
        initializeBarcode()
        return true
    }
}