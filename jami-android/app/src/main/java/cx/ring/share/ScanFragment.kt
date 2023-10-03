/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.share

import android.Manifest
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import android.widget.TextView
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import cx.ring.R
import androidx.annotation.StringRes
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import cx.ring.fragments.QRCodeFragment
import com.google.zxing.ResultPoint
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import cx.ring.client.HomeActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScanFragment : Fragment() {
    private val viewModel: ScanViewModel by lazy {
        ViewModelProvider(this)[ScanViewModel::class.java]
    }
    private var barcodeView: DecoratedBarcodeView? = null
    private var mErrorMessageTextView: TextView? = null

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                viewModel.cameraPermissionChanged(true)
                hideErrorPanel()
                initializeBarcode()
            } else {
                displayNoPermissionsError()
            }
        }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.frag_scan, container, false)
        barcodeView = rootView.findViewById(R.id.barcode_scanner)
        mErrorMessageTextView = rootView.findViewById(R.id.error_msg_txt)
        if (hasCameraPermission()) {
            hideErrorPanel()
            initializeBarcode()
        }
        return rootView
    }

    override fun onResume() {
        super.onResume()
        if (checkPermission()) {
            barcodeView?.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        barcodeView?.pause()
    }

    private fun showErrorPanel(@StringRes textResId: Int) {
        mErrorMessageTextView?.apply {
            setText(textResId)
            visibility = View.VISIBLE
        }
        barcodeView?.visibility = View.GONE
    }

    private fun hideErrorPanel() {
        mErrorMessageTextView?.visibility = View.GONE
        barcodeView?.visibility = View.VISIBLE
    }

    private fun displayNoPermissionsError() {
        showErrorPanel(R.string.error_scan_no_camera_permissions)
    }

    private fun initializeBarcode() {
        barcodeView?.apply {
            barcodeView.decoderFactory =
                DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
            //barcodeView.initializeFromIntent(getActivity().getIntent());
            decodeContinuous(callback)
        }
    }

    private val callback: BarcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            if (result.text != null) {
                val contactUri = result.text
                if (contactUri != null) {
                    val parent = parentFragment as QRCodeFragment?
                    parent?.dismiss()
                    goToConversation(contactUri)
                }
            }
        }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
    }

    private fun goToConversation(conversationUri: String) {
        (requireActivity() as HomeActivity).startConversation(conversationUri)
    }

    private fun checkPermission(): Boolean {
        if (!hasCameraPermission()) {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
            return false
        }
        return true
    }

    companion object {
        val TAG = ScanFragment::class.simpleName!!
    }
}