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
package cx.ring.tv.account

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import cx.ring.R
import cx.ring.databinding.FragmentExportSideStep1Binding
import cx.ring.linkdevice.viewmodel.ExportSideInputError
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TVAccountExportStep1Fragment : Fragment() {

    private var _binding: FragmentExportSideStep1Binding? = null
    private val binding get() = _binding!!

    private enum class InputMode { QR, CODE }

    private var currentMode = InputMode.QR
    private var isLoading = false

    interface OnInputCallback {
        fun onAuthenticationUri(authenticationUri: String)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentExportSideStep1Binding.inflate(inflater, container, false)
        .also { _binding = it }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeBarcode()
        showInputQr()

        binding.switchMode.setOnClickListener {
            if (currentMode == InputMode.QR) showInputCode()
            else showInputQr()
        }

        binding.connect.setOnClickListener {
            Log.i(TAG, "Connect clicked, authentication uri = ${binding.code.text}")
            showInputCode(loading = true)
            (requireActivity() as OnInputCallback)
                .onAuthenticationUri(binding.code.text.toString())
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentMode == InputMode.QR) {
            binding.barcodeScanner.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (currentMode == InputMode.QR) {
            binding.barcodeScanner.pause()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun showError(error: ExportSideInputError?) {
        if (currentMode == InputMode.QR) showInputQr(error = error)
        else showInputCode(error = error)
    }

    private fun initializeBarcode() {
        binding.barcodeScanner.apply {
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
        binding.switchMode.text = getText(R.string.export_side_step1_switch_to_code)

        if (loading) {
            binding.barcodeScanner.visibility = View.INVISIBLE
            binding.loadingQr.visibility = View.VISIBLE
            binding.errorQr.visibility = View.INVISIBLE
            binding.switchMode.isEnabled = false
        } else if (error != null) {
            initializeBarcode()
            binding.barcodeScanner.resume()
            binding.errorQr.text = when (error) {
                ExportSideInputError.INVALID_INPUT ->
                    getString(R.string.export_side_step1_error_malformed)
            }
            binding.switchMode.isEnabled = true
            binding.barcodeScanner.visibility = View.VISIBLE
            binding.loadingQr.visibility = View.INVISIBLE
            binding.errorQr.visibility = View.VISIBLE
            binding.errorQr.visibility = View.VISIBLE
        } else {
            binding.barcodeScanner.resume()
            binding.barcodeScanner.visibility = View.VISIBLE
            binding.loadingQr.visibility = View.INVISIBLE
            binding.errorQr.visibility = View.INVISIBLE
            binding.switchMode.isEnabled = true
        }
    }

    private fun showInputCode(loading: Boolean = false, error: ExportSideInputError? = null) {
        binding.barcodeScanner.pause()

        isLoading = loading
        currentMode = InputMode.CODE

        binding.inputCode.visibility = View.VISIBLE
        binding.inputQr.visibility = View.INVISIBLE
        binding.switchMode.text = getText(R.string.export_side_step1_switch_to_qr)

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

    private val onBarcodeResult = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            if (!result.text.startsWith(SCHEME)) return
            Log.i(TAG, "QR scanned: ${result.text}")
            binding.barcodeScanner.pause()
            binding.barcodeScanner.barcodeView.stopDecoding()
            showInputQr(loading = true)
            (requireActivity() as OnInputCallback).onAuthenticationUri(result.text)
        }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
    }

    companion object {
        private const val TAG = "TVAccountExportStep1Fragment"
        const val SCHEME = "jami-auth://"
    }
}
