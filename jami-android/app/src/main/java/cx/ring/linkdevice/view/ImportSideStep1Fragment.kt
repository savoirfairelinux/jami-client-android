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

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cx.ring.databinding.FragmentImportSideStep1Binding
import cx.ring.R
import cx.ring.utils.ActionHelper
import cx.ring.utils.QRCodeLoaderUtils

class ImportSideStep1Fragment : Fragment() {
    private var _binding: FragmentImportSideStep1Binding? = null
    private val binding get() = _binding!!
    private var _callback: OnOutputCallback? = null

    @Suppress("unused")
    private val callback get() = _callback!!

    private enum class OutputMode { QR, CODE }

    private var currentMode = OutputMode.QR
    private var currentAuthenticationToken = ""

    interface OnOutputCallback // Empty interface. Could be used for future implementations.

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (requireActivity() is OnOutputCallback) {
            _callback = requireActivity() as OnOutputCallback
        } else {
            throw RuntimeException("Parent fragment must implement ${OnOutputCallback::class.java.simpleName}")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentImportSideStep1Binding.inflate(inflater, container, false)
        .apply {
            _binding = this
            showLoading()
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.switchMode.setOnClickListener {
            if (currentMode == OutputMode.QR) {
                showOutputCode()
            } else {
                showOutputQr()
            }
        }

        // Detect click (on the code and on the button) to share the authentication token.
        binding.code.setOnClickListener {
            ActionHelper.shareAuthenticationToken(requireContext(), currentAuthenticationToken)
        }
        binding.share.setOnClickListener {
            ActionHelper.shareAuthenticationToken(requireContext(), currentAuthenticationToken)
        }

    }

    fun showOutput(token: String) {
        Log.i(TAG, "Show authentication token : $token currentMode: $currentMode")
        currentAuthenticationToken = token
        showOutputQr()
    }

    fun showLoading() {
        Log.i(TAG, "Show loading...")
        binding.codeContainer.visibility = View.INVISIBLE
        binding.QRContainer.visibility = View.INVISIBLE
        binding.loadingContainer.visibility = View.VISIBLE
        binding.connecting.text =
            getText(R.string.import_side_step1_preparing_device)
        binding.switchMode.isEnabled = false
    }

    // `showLoading()` during preparation of the authentication URI.
    // `showConnecting()` during peer-to-peer connection to the device.
    @Suppress("unused")
    fun showConnecting() { // Unused method. Could be used for future implementations.
        Log.i(TAG, "Show connecting...")
        binding.codeContainer.visibility = View.INVISIBLE
        binding.QRContainer.visibility = View.INVISIBLE
        binding.loadingContainer.visibility = View.VISIBLE
        binding.connecting.text = getText(R.string.import_side_step1_connecting)
        binding.advice.visibility = View.INVISIBLE
        binding.switchMode.visibility = View.INVISIBLE
    }

    private fun showOutputQr() {
        if (currentAuthenticationToken.isNotEmpty()) {
            QRCodeLoaderUtils.loadQRCodeData(
                currentAuthenticationToken,
                requireContext().getColor(android.R.color.black),
                requireContext().getColor(android.R.color.transparent)
            ) { qrCodeData ->
                binding.qrImage.setImageBitmap(
                    Bitmap.createBitmap(
                        qrCodeData.width, qrCodeData.height, Bitmap.Config.ARGB_8888
                    ).apply {
                        setPixels(
                            qrCodeData.data, 0, qrCodeData.width,
                            0, 0, qrCodeData.width, qrCodeData.height
                        )
                    }
                )
                currentMode = OutputMode.QR
                binding.codeContainer.visibility = View.INVISIBLE
                binding.QRContainer.visibility = View.VISIBLE
                binding.loadingContainer.visibility = View.INVISIBLE
                binding.switchMode.text =
                    getText(R.string.import_side_step1_switch_to_code)
                binding.advice.visibility = View.VISIBLE
                binding.switchMode.visibility = View.VISIBLE
                binding.switchMode.isEnabled = true
            }
        }
    }

    private fun showOutputCode() {
        if (currentMode != OutputMode.CODE) {
            currentMode = OutputMode.CODE
            binding.codeContainer.visibility = View.VISIBLE
            binding.QRContainer.visibility = View.INVISIBLE
            binding.loadingContainer.visibility = View.INVISIBLE
            binding.code.text = currentAuthenticationToken
            binding.switchMode.text =
                getText(R.string.import_side_step1_switch_to_qr)
            binding.advice.visibility = View.VISIBLE
            binding.switchMode.visibility = View.VISIBLE
        }
    }

    companion object {
        private val TAG = ImportSideStep1Fragment::class.java.simpleName
    }

}