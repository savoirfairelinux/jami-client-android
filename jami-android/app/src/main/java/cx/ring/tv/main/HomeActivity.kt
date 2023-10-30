/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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
package cx.ring.tv.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.hardware.Camera
import android.hardware.Camera.ErrorCallback
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraManager.AvailabilityCallback
import android.os.Build
import android.os.Bundle
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.renderscript.ScriptIntrinsicYuvToRGB
import android.renderscript.Type
import android.util.Log
import android.util.Size
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
import java.util.Collections
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
    private var cameraPermissionIsRefusedFlag = false // to not ask for permission again if refused

    private val mErrorCallback = ErrorCallback { error, camera ->
        try {
            mBlurImage.visibility = View.INVISIBLE
            mBackgroundManager.drawable =
                ContextCompat.getDrawable(this@HomeActivity, R.drawable.tv_background)
                    ?: ColorDrawable(getColor(R.color.colorPrimary))
        } catch (e: Exception) {
            Log.e(TAG, "ErrorCallback", e)
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            onBackInvokedDispatcher.registerOnBackInvokedCallback(1) { onBackPressed() }
        JamiApplication.instance?.startDaemon(this)
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
        mDisposableBag.add(
            mAccountService.observableAccountList
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
        mCameraPreview?.let { preview ->
            mCamera?.setPreviewCallback(null)
            preview.stop()
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
        mBlurOutputBitmap?.let { blurOutputBitmap ->
            input?.destroy()
            input = null
            out?.destroy()
            out = null
            blurIntrinsic?.destroy()
            blurIntrinsic = null
            mBlurOut?.destroy()
            mBlurOut = null
            yuvToRgbIntrinsic?.destroy()
            yuvToRgbIntrinsic = null
            blurOutputBitmap.recycle()
            mBlurOutputBitmap = null
            rs?.destroy()
            rs = null
        }
    }

    internal class CompareSizesByArea : Comparator<Camera.Size> {
        override fun compare(lhs: Camera.Size, rhs: Camera.Size): Int {
            // We cast here to ensure the multiplications won't overflow
            return java.lang.Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
        }
    }

    private fun chooseOptimalSize(
        choices: List<Camera.Size>,
        minWidth: Int,
        minHeight: Int,
        maxWidth: Int,
        maxHeight: Int,
        target: Size
    ): Camera.Size {
        if (choices.isEmpty())
            throw IllegalArgumentException()
        // Collect the supported resolutions that are at least as big as the preview Surface
        val bigEnough: MutableList<Camera.Size> = ArrayList()
        // Collect the supported resolutions that are smaller than the preview Surface
        val notBigEnough: MutableList<Camera.Size> = ArrayList()
        val w = target.width
        val h = target.height
        for (option in choices) {
            Log.w(TAG, "supportedSize: $option")
            if (option.width <= maxWidth && option.height <= maxHeight && option.height == option.width * h / w) {
                if (option.width >= minWidth && option.height >= minHeight) {
                    bigEnough.add(option)
                } else {
                    notBigEnough.add(option)
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        return when {
            bigEnough.size > 0 -> Collections.min(bigEnough, CompareSizesByArea())
            notBigEnough.size > 0 -> Collections.max(notBigEnough, CompareSizesByArea())
            else -> {
                Log.e(TAG, "Couldn't find any suitable preview size")
                choices[0]
            }
        }
    }

    private fun setUpCamera() {
        mDisposableBag.add(Single.fromCallable {
            val currentCamera = 0
            Camera.open(currentCamera)!!.apply { mCamera = this }
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ camera: Camera ->
                try {
                    Log.w(TAG, "setUpCamera()")
                    val params = camera.parameters
                    val selectSize = chooseOptimalSize(params.supportedPictureSizes, 1280, 720, 1920, 1080, Size(1280, 720))
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
                } catch (e: Exception) {
                    Log.e(TAG, "setUpCamera() error", e)
                    mBackgroundManager.drawable = ContextCompat.getDrawable(this@HomeActivity, R.drawable.tv_background)
                }
            }) {
                mBackgroundManager.drawable = ContextCompat.getDrawable(this@HomeActivity, R.drawable.tv_background)
            })
    }

    private fun checkCameraAvailability() {
        if (mDeviceRuntimeService.hasVideoPermission()) {
            setUpCamera()
        } else {
            if (!cameraPermissionIsRefusedFlag) askCameraPermission()
        }
    }

    private fun askCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA),
            ProfileCreationFragment.REQUEST_PERMISSION_CAMERA)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ProfileCreationFragment.REQUEST_PERMISSION_CAMERA ->
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraPermissionChanged(true)
                    setUpCamera()
                }
                else cameraPermissionIsRefusedFlag = true
        }
    }

    private fun cameraPermissionChanged(isGranted: Boolean) {
        if (isGranted && mHardwareService.isVideoAvailable) {
            mHardwareService.initVideo()
                    .onErrorComplete()
                    .subscribe()
        }
    }

    companion object {
        private val TAG = HomeActivity::class.simpleName!!
        private const val BLUR_RADIUS = 7.5f
    }
}