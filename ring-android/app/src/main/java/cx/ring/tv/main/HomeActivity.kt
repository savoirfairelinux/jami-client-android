/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Michel Schmit <michel.schmit@savoirfairelinux.com>
 *  Author: Aline Bonnet <aline.bonnet@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *  Author: AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.tv.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.Camera
import android.hardware.Camera.ErrorCallback
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraManager.AvailabilityCallback
import android.os.Bundle
import android.renderscript.*
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.GuidedStepSupportFragment
import cx.ring.R
import cx.ring.account.ProfileCreationFragment
import cx.ring.application.JamiApplication
import cx.ring.tv.account.TVAccountWizard
import cx.ring.tv.camera.CameraPreview
import cx.ring.tv.contact.TVContactFragment
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.model.Account
import net.jami.services.AccountService
import net.jami.services.DeviceRuntimeService
import net.jami.services.HardwareService
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : FragmentActivity() {
    private val mDisposableBag = CompositeDisposable()

    @Inject
    lateinit var mAccountService: AccountService
    @Inject
    lateinit var mDeviceRuntimeService: DeviceRuntimeService
    @Inject
    lateinit var mHardwareService: HardwareService
    private lateinit var mBackgroundManager: BackgroundManager
    private lateinit var mBlurImage: ImageView
    private lateinit var mPreviewView: FrameLayout
    private lateinit var mFadeView: View
    private var mCamera: Camera? = null
    private var mCameraPreview: CameraPreview? = null
    private var mCameraManager: CameraManager? = null
    private var mBlurOutputBitmap: Bitmap? = null
    private var rs: RenderScript? = null
    private var yuvToRgbIntrinsic: ScriptIntrinsicYuvToRGB? = null
    private var blurIntrinsic: ScriptIntrinsicBlur? = null
    private var input: Allocation? = null
    private var out: Allocation? = null
    private var mBlurOut: Allocation? = null

    private val mErrorCallback = ErrorCallback { error, camera ->
        mBlurImage.visibility = View.INVISIBLE
        mBackgroundManager.drawable = ContextCompat.getDrawable(this@HomeActivity, R.drawable.tv_background)
    }
    private val mCameraAvailabilityCallback: AvailabilityCallback = object : AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
            if (mBlurImage.visibility == View.INVISIBLE) {
                checkCameraAvailability()
            }
        }
    }
    private val mPreviewCallback = Camera.PreviewCallback { data, camera ->
        if (supportFragmentManager.findFragmentByTag(TVContactFragment.TAG) != null) {
            mBlurImage.visibility = View.GONE
            mFadeView.visibility = View.GONE
            mPreviewView.visibility = View.VISIBLE
            return@PreviewCallback
        }
        if (mBlurOutputBitmap == null) {
            val size = camera.parameters.previewSize
            rs = RenderScript.create(this@HomeActivity)
            val yuvType = Type.Builder(rs, Element.U8(rs)).setX(data.size).create()
            input = Allocation.createTyped(rs, yuvType, Allocation.USAGE_SCRIPT)
            yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs)).apply { setInput(input) }
            val rgbaType = Type.Builder(rs, Element.RGBA_8888(rs)).setX(size.width).setY(size.height).create()
            out = Allocation.createTyped(rs, rgbaType, Allocation.USAGE_SCRIPT)
            blurIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs)).apply {
                setRadius(BLUR_RADIUS * size.width / 1080)
                setInput(out)
            }
            mBlurOutputBitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
            mBlurOut = Allocation.createFromBitmap(rs, mBlurOutputBitmap)
        }
        input!!.copyFrom(data)
        yuvToRgbIntrinsic!!.forEach(out)
        blurIntrinsic!!.forEach(mBlurOut)
        mBlurOut!!.copyTo(mBlurOutputBitmap)
        mBlurImage.setImageBitmap(mBlurOutputBitmap)
        if (mBlurImage.visibility == View.GONE) {
            mPreviewView.visibility = View.INVISIBLE
            mBlurImage.visibility = View.VISIBLE
            mFadeView.visibility = View.VISIBLE
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        JamiApplication.instance?.startDaemon()
        setContentView(R.layout.tv_activity_home)
        mBackgroundManager = BackgroundManager.getInstance(this).apply { attach(window) }
        mPreviewView = findViewById(R.id.previewView)
        mBlurImage = findViewById(R.id.blur)
        mFadeView = findViewById(R.id.fade)
    }

    override fun onBackPressed() {
        if (GuidedStepSupportFragment.getCurrentGuidedStepSupportFragment(supportFragmentManager) != null) {
//            mIsContactFragmentVisible = false;
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        //mDisposable.clear();
        mDisposableBag.add(
            mAccountService!!.observableAccountList
                .observeOn(AndroidSchedulers.mainThread())
                .firstElement()
                .subscribe { accounts: List<Account?> ->
                    if (accounts.isEmpty()) {
                        startActivity(Intent(this, TVAccountWizard::class.java))
                    }
                })
    }

    override fun onPostResume() {
        super.onPostResume()
        checkCameraAvailability()
    }

    override fun onPause() {
        super.onPause()
        if (mCameraPreview != null) {
            mCamera!!.setPreviewCallback(null)
            mCameraPreview!!.stop()
            mCameraPreview = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mDisposableBag.dispose()
        mCameraManager?.unregisterAvailabilityCallback(mCameraAvailabilityCallback)
        mCamera?.let { camera ->
            camera.release();
            mCamera = null
        }
        if (mBlurOutputBitmap != null) {
            input!!.destroy()
            input = null
            out!!.destroy()
            out = null
            blurIntrinsic!!.destroy()
            blurIntrinsic = null
            mBlurOut!!.destroy()
            mBlurOut = null
            yuvToRgbIntrinsic!!.destroy()
            yuvToRgbIntrinsic = null
            mBlurOutputBitmap!!.recycle()
            mBlurOutputBitmap = null
            rs!!.destroy()
            rs = null
        }
    }

    private val cameraInstance: Camera
        private get() {
            try {
                val currentCamera = 0
                mCamera = Camera.open(currentCamera)
            } catch (e: RuntimeException) {
                Log.e(TAG, "failed to open camera")
            }
            return mCamera!!
        }

    private fun setUpCamera() {
        mDisposableBag.add(Single.fromCallable { cameraInstance }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ camera: Camera ->
                Log.w(TAG, "setUpCamera()")
                val params = camera.parameters
                var selectSize: Camera.Size? = null
                for (size in params.supportedPictureSizes) {
                    if (size.width == 1280 && size.height == 720) {
                        selectSize = size
                        break
                    }
                }
                checkNotNull(selectSize) { "No supported size" }
                Log.w(TAG, "setUpCamera() selectSize " + selectSize.width + "x" + selectSize.height)
                params.setPictureSize(selectSize.width, selectSize.height)
                params.setPreviewSize(selectSize.width, selectSize.height)
                camera.parameters = params
                mBlurImage.visibility = View.VISIBLE
                if (mCameraManager == null) {
                    mCameraManager = (getSystemService(CAMERA_SERVICE) as CameraManager).apply {
                        registerAvailabilityCallback((mCameraAvailabilityCallback), null)
                    }
                }
                mCameraPreview = CameraPreview(this, camera)
                mPreviewView.removeAllViews()
                mPreviewView.addView(mCameraPreview, 0)
                camera.setErrorCallback(mErrorCallback)
                camera.setPreviewCallback(mPreviewCallback)
            }) {
                mBackgroundManager.drawable = ContextCompat.getDrawable(this@HomeActivity, R.drawable.tv_background)
            })
    }

    private fun checkCameraAvailability() {
        if (mDeviceRuntimeService.hasVideoPermission()) {
            setUpCamera()
        } else {
            askCameraPermission()
        }
    }

    private fun askCameraPermission() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                ProfileCreationFragment.REQUEST_PERMISSION_CAMERA
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ProfileCreationFragment.REQUEST_PERMISSION_CAMERA -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                cameraPermissionChanged(true)
                setUpCamera()
            }
        }
    }

    private fun cameraPermissionChanged(isGranted: Boolean) {
        if (isGranted && mHardwareService.isVideoAvailable) {
            mHardwareService.initVideo()
                    .onErrorComplete()
                    .subscribe()
        }
    }

    fun getFrameLayout(): FrameLayout {
        return mPreviewView
    }

    companion object {
        private val TAG = HomeActivity::class.simpleName!!
        private const val BLUR_RADIUS = 7.5f
    }
}