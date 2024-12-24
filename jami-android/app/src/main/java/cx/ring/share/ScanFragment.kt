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
package cx.ring.share

import android.Manifest
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.util.Log
import cx.ring.R
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import cx.ring.fragments.QRCodeFragment
import com.google.zxing.ResultPoint
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import cx.ring.client.HomeActivity
import cx.ring.databinding.FragScanBinding
import cx.ring.mvp.BaseSupportFragment
import dagger.hilt.android.AndroidEntryPoint
import net.jami.model.Uri
import net.jami.scan.ScanPresenter
import net.jami.scan.ScanView

@AndroidEntryPoint
class ScanFragment : BaseSupportFragment<ScanPresenter, ScanView>(), ScanView {

    private var mBinding: FragScanBinding? = null

    private var cameraPermissionIsRefusedFlag = false // to not ask for permission again if refused

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                presenter.cameraPermissionChanged(true)
                hideErrorPanel()
                initializeBarcode()
            } else {
                cameraPermissionIsRefusedFlag = true
                showErrorPanel()
            }
        }

    private val callback: BarcodeCallback = object : BarcodeCallback {
        private var lastFailTime: Long = 0
        override fun barcodeResult(result: BarcodeResult) {
            val uri = Uri.fromString(result.text)
            if (uri.isEmpty || !uri.isJami) {
                val now = System.currentTimeMillis()
                if (now - lastFailTime > INVALID_SCAN_MIN_TIME) {
                    Toast.makeText(
                        context,
                        getString(R.string.qr_code_not_contact),
                        Toast.LENGTH_SHORT
                    ).show()
                    lastFailTime = now
                }
                return
            }
            mBinding?.barcodeScanner?.pause()
            mBinding?.barcodeScanner?.barcodeView?.stopDecoding()
            (parentFragment as? QRCodeFragment)?.dismiss()
            presenter.onBarcodeScanned(result.text)
        }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return FragScanBinding.inflate(inflater, container, false).apply {
            mBinding = this
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (hasCameraPermission()) {
            hideErrorPanel()
            initializeBarcode()
        }
    }

    override fun onPause() {
        super.onPause()
        mBinding?.barcodeScanner?.pause()
    }

    override fun onResume() {
        super.onResume()
        if (checkPermission()) {
            mBinding?.barcodeScanner?.resume()
        }
    }

    override fun moveToConversation(conversation: String) {
        try {
            (requireActivity() as HomeActivity).startConversation(conversation)
        } catch (e: Exception) {
            Log.w(TAG, "Error while starting conversation", e)
        }
    }

    private fun checkPermission(): Boolean {
        if (!hasCameraPermission()) {
            if (!cameraPermissionIsRefusedFlag) // if the permission is refused, don't ask again
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            return false
        }
        return true
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun hideErrorPanel() {
        mBinding?.errorMsgTxt?.visibility = View.GONE
        mBinding?.barcodeScanner?.visibility = View.VISIBLE
    }

    private fun initializeBarcode() {
        mBinding?.barcodeScanner?.apply {
            barcodeView.decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
            decodeContinuous(callback)
        }
    }

    private fun showErrorPanel() {
        mBinding?.errorMsgTxt?.apply {
            setText(R.string.error_scan_no_camera_permissions)
            visibility = View.VISIBLE
        }
        mBinding?.barcodeScanner?.visibility = View.GONE
    }

    companion object {
        val TAG = ScanFragment::class.simpleName!!
        const val INVALID_SCAN_MIN_TIME = 5000L
    }
}