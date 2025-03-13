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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.createBitmap
import androidx.fragment.app.Fragment
import cx.ring.R
import cx.ring.databinding.FragmentImportSideStep1Binding
import cx.ring.utils.QRCodeLoaderUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TVAccountImportStep1Fragment : Fragment() {

    private var _binding: FragmentImportSideStep1Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        FragmentImportSideStep1Binding.inflate(inflater, container, false)
            .apply {
                _binding = this
                showLoading()
            }.root

    fun showLoading() {
        binding.QRContainer.visibility = View.INVISIBLE
        binding.loadingContainer.visibility = View.VISIBLE
        binding.connecting.text = getText(R.string.import_side_step1_preparing_device)
    }

    fun showToken(token: String) {
        Log.w(TAG, "showToken: $token")

        QRCodeLoaderUtils.loadQRCodeData(
            token,
            requireContext().getColor(android.R.color.black),
            requireContext().getColor(android.R.color.transparent)
        ) { qrCodeData ->
            binding.qrImage.setImageBitmap(
                createBitmap(qrCodeData.width, qrCodeData.height).apply {
                    setPixels(
                        qrCodeData.data, 0, qrCodeData.width,
                        0, 0, qrCodeData.width, qrCodeData.height
                    )
                }
            )
            binding.code.text = token
            binding.QRContainer.visibility = View.VISIBLE
            binding.loadingContainer.visibility = View.INVISIBLE
            binding.advice.visibility = View.VISIBLE
        }
    }

    fun showConnecting() {
        binding.QRContainer.visibility = View.INVISIBLE
        binding.loadingContainer.visibility = View.VISIBLE
        binding.connecting.text = getText(R.string.import_side_step1_connecting)
    }

    companion object {
        private const val TAG = "TVAccountImportStep1Fragment"
    }
}