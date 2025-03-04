/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
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
package cx.ring.linkdevice.view

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import cx.ring.databinding.FragmentExportSideStep1Binding
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import cx.ring.R
import cx.ring.linkdevice.viewmodel.ExportSideInputError


class ExportSideStep1Fragment : Fragment() {
    private var _binding: FragmentExportSideStep1Binding? = null
    private val binding get() = _binding!!
    private var _callback: OnInputCallback? = null
    private val callback get() = _callback!!

    private enum class InputMode { QR, CODE }

    private var currentMode = InputMode.QR
    private var isLoading = false

    interface OnInputCallback {
        fun onAuthenticationUri(authenticationUri: String)
    }

    private val requestCameraPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                initializeBarcode()
                showInputQr()
            } else {
                Log.w(TAG, "Camera permission is denied.")
                showInputCode()
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        _callback = requireActivity() as OnInputCallback
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentExportSideStep1Binding.inflate(inflater, container, false)
        .apply { _binding = this }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if permission is granted, otherwise request it
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, so launch the permission request
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            // Permission already granted, proceed with camera-related tasks
            initializeBarcode()
            when (currentMode) {
                InputMode.QR -> showInputQr(loading = isLoading)
                InputMode.CODE -> showInputCode(loading = isLoading)
            }
        }

        binding.switchMode.setOnClickListener {
            if (currentMode == InputMode.QR) showInputCode()
            else showInputQr()
        }

        binding.code.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(currentText: Editable?) {
                val prefix = "jami-auth://"
                val currentTextStr = currentText.toString()
                // What we want here is to prevent the user from removing the prefix.
                if (!currentTextStr.startsWith(prefix)) {
                    // Interesting text is after the prefix.
                    // In the case the user write IN the prefix, for example: "jaAmi-auth://",
                    // we want to keep the part after "jaAmi-auth://" and restore the prefix.
                    val interestingText =
                        if (currentTextStr.length > prefix.length)
                            currentTextStr.substring(prefix.length + 1)
                        else ""
                    val finalText = prefix + interestingText
                    binding.code.setText(finalText)
                    // Always move the cursor to the end to hint the user that the prefix is fixed.
                    binding.code.setSelection(finalText.length)
                } else {
                    val interestingText = currentTextStr.substring(prefix.length)
                    if (interestingText.startsWith(prefix)) {
                        // If the prefix is detected twice, for example if user copy-paste the code.
                        // We want to remove the prefix from the interesting text.
                        val reallyInterestingText = interestingText.removePrefix(prefix)
                        val finalText = prefix + reallyInterestingText
                        binding.code.setText(finalText)
                        binding.code.setSelection(finalText.length)
                    }
                }
            }
        })

        binding.connect.setOnClickListener {
            Log.i(TAG, "Connect button clicked, authentication uri = ${binding.code.text}")
            showInputCode(loading = true)
            callback.onAuthenticationUri(binding.code.text.toString())
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "Resuming barcode scanner.")
        if (currentMode == InputMode.QR) binding.barcodeScanner.resume()
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "Pausing barcode scanner.")
        binding.barcodeScanner.pause()
    }

    fun showError(error: ExportSideInputError) {
        Log.w(TAG, "Input error: $error for current mode: $currentMode")
        if (currentMode == InputMode.QR) {
            showInputQr(error = error)
        } else {
            showInputCode(error = error)
        }
    }

    private val onBarcodeResult: BarcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            if (!result.text.startsWith("jami-auth://")) return
            binding.barcodeScanner.pause()
            binding.barcodeScanner.barcodeView.stopDecoding()
            showInputQr(loading = true)
            callback.onAuthenticationUri(result.text)
            Log.i(TAG, "Barcode scanned, authentication code = ${result.text}")
        }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
    }

    private fun initializeBarcode() {
        binding.barcodeScanner.apply {
            Log.i(TAG, "Initializing barcode scanner.")
            barcodeView.decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
            decodeContinuous(onBarcodeResult)
        }
    }

    private fun showInputQr(loading: Boolean = false, error: ExportSideInputError? = null) {
        binding.barcodeScanner.resume()

        isLoading = loading
        currentMode = InputMode.QR

        binding.inputCode.visibility = View.INVISIBLE
        binding.inputQr.visibility = View.VISIBLE
        binding.switchMode.text = getText(R.string.export_side_step1_switch_to_qr)

        if (loading) {
            binding.barcodeScanner.visibility = View.INVISIBLE
            binding.loadingQr.visibility = View.VISIBLE
            binding.errorQr.visibility = View.INVISIBLE
            binding.switchMode.isEnabled = false
        } else if (error != null) {
            initializeBarcode() // Restart the barcode scanner.
            binding.barcodeScanner.resume()
            binding.errorQr.text = when (error) {
                ExportSideInputError.INVALID_INPUT -> getText(R.string.export_side_step1_error_malformed)
            }
            binding.switchMode.isEnabled = true
            binding.barcodeScanner.visibility = View.VISIBLE
            binding.loadingQr.visibility = View.INVISIBLE
            binding.errorQr.visibility = View.VISIBLE
        } else {
            binding.switchMode.isEnabled = true
            binding.barcodeScanner.visibility = View.VISIBLE
            binding.loadingQr.visibility = View.INVISIBLE
            binding.errorQr.visibility = View.INVISIBLE
        }
    }

    private fun showInputCode(
        loading: Boolean = false,
        error: ExportSideInputError? = null
    ) {
        binding.barcodeScanner.pause()

        currentMode = InputMode.CODE
        isLoading = loading

        binding.inputCode.visibility = View.VISIBLE
        binding.inputQr.visibility = View.INVISIBLE
        binding.switchMode.text = getText(R.string.export_side_step1_switch_to_code)

        binding.codeLayout.error = when (error) {
            ExportSideInputError.INVALID_INPUT -> getText(R.string.export_side_step1_error_malformed)
            else -> null
        }

        if (loading) {
            binding.loadingCode.visibility = View.VISIBLE
            binding.connect.isEnabled = false
            binding.code.isEnabled = false
            binding.switchMode.isEnabled = false
        } else {
            binding.connect.isEnabled = true
            binding.code.isEnabled = true
            binding.switchMode.isEnabled = true
            binding.loadingCode.visibility = View.INVISIBLE
        }
    }

    companion object {
        private val TAG = ExportSideStep1Fragment::class.java.simpleName
    }

}