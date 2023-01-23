/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Authors: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *           Rayan Osseiran <rayan.osseiran@savoirfairelinux.com>
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
import android.os.Build
import android.view.View
import androidx.fragment.app.Fragment
import cx.ring.application.JamiApplication
import cx.ring.client.HomeActivity

class ScanFragment : Fragment() {
    private var barcodeView: DecoratedBarcodeView? = null
    private var mErrorMessageTextView: TextView? = null
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        if (checkPermission() && barcodeView != null) {
            barcodeView!!.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (hasCameraPermission() && barcodeView != null) {
            barcodeView!!.pause()
        }
    }

    private fun showErrorPanel(@StringRes textResId: Int) {
        if (mErrorMessageTextView != null) {
            mErrorMessageTextView!!.setText(textResId)
            mErrorMessageTextView!!.visibility = View.VISIBLE
        }
        if (barcodeView != null) {
            barcodeView!!.visibility = View.GONE
        }
    }

    private fun hideErrorPanel() {
        if (mErrorMessageTextView != null) {
            mErrorMessageTextView!!.visibility = View.GONE
        }
        if (barcodeView != null) {
            barcodeView!!.visibility = View.VISIBLE
        }
    }

    private fun displayNoPermissionsError() {
        showErrorPanel(R.string.error_scan_no_camera_permissions)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var i = 0
        val n = permissions.size
        while (i < n) {
            when (permissions[i]) {
                Manifest.permission.CAMERA -> {
                    val granted = grantResults[i] == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        hideErrorPanel()
                        initializeBarcode()
                    } else {
                        displayNoPermissionsError()
                    }
                    return
                }
                else -> {
                }
            }
            i++
        }
    }

    private fun initializeBarcode() {
        if (barcodeView != null) {
            barcodeView!!.barcodeView.decoderFactory =
                DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
            //barcodeView.initializeFromIntent(getActivity().getIntent());
            barcodeView!!.decodeContinuous(callback)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(
                    arrayOf(Manifest.permission.CAMERA),
                    JamiApplication.PERMISSIONS_REQUEST
                )
            } else {
                displayNoPermissionsError()
            }
            return false
        }
        return true
    }

    companion object {
        val TAG = ScanFragment::class.simpleName!!
    }
}