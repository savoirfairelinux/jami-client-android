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
package cx.ring.tv.main;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.GuidedStepSupportFragment;

import net.jami.services.AccountService;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.tv.account.TVAccountWizard;
import dagger.hilt.android.AndroidEntryPoint;
import cx.ring.tv.camera.CameraPreview;
import cx.ring.tv.contact.TVContactFragment;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@AndroidEntryPoint
public class HomeActivity extends FragmentActivity {

    private static final String TAG = HomeActivity.class.getSimpleName();

    private static final float BITMAP_SCALE = 0.4f;
    private static final float BLUR_RADIUS = 7.5f;

    private final CompositeDisposable mDisposableBag = new CompositeDisposable();

    @Inject
    AccountService mAccountService;

    private BackgroundManager mBackgroundManager;
    private ImageView mBlurImage;
    private FrameLayout mPreviewView;
    private View mFadeView;
    private Camera mCamera;
    private CameraPreview mCameraPreview;
    private CameraManager mCameraManager;

    private Bitmap mBlurOutputBitmap;
    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private ScriptIntrinsicBlur blurIntrinsic;
    private Allocation in, out, mBlurOut;

    private final Camera.ErrorCallback mErrorCallback = new Camera.ErrorCallback() {
        @Override
        public void onError(int error, Camera camera) {
            mBlurImage.setVisibility(View.INVISIBLE);
            mBackgroundManager.setDrawable(ContextCompat.getDrawable(HomeActivity.this, R.drawable.tv_background));
        }
    };

    private final Object mCameraAvailabilityCallback = new CameraManager.AvailabilityCallback() {
        @Override
        public void onCameraAvailable(String cameraId) {
            if (mBlurImage.getVisibility() == View.INVISIBLE) {
                setUpCamera();
            }
        }
    };

    private final Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (getSupportFragmentManager().findFragmentByTag(TVContactFragment.TAG) != null) {
                mBlurImage.setVisibility(View.GONE);
                mFadeView.setVisibility(View.GONE);
                mPreviewView.setVisibility(View.VISIBLE);
                return;
            }
            if (mBlurOutputBitmap == null) {
                Camera.Size size = camera.getParameters().getPreviewSize();
                rs = RenderScript.create(HomeActivity.this);
                Type yuvType = new Type.Builder(rs, Element.U8(rs)).setX(data.length).create();
                in = Allocation.createTyped(rs, yuvType, Allocation.USAGE_SCRIPT);
                yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
                yuvToRgbIntrinsic.setInput(in);
                Type rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(size.width).setY(size.height).create();
                out = Allocation.createTyped(rs, rgbaType, Allocation.USAGE_SCRIPT);
                blurIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
                blurIntrinsic.setRadius(BLUR_RADIUS * size.width / 1080);
                blurIntrinsic.setInput(out);
                mBlurOutputBitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888);
                mBlurOut = Allocation.createFromBitmap(rs, mBlurOutputBitmap);
            }
            in.copyFrom(data);
            yuvToRgbIntrinsic.forEach(out);
            blurIntrinsic.forEach(mBlurOut);
            mBlurOut.copyTo(mBlurOutputBitmap);
            mBlurImage.setImageBitmap(mBlurOutputBitmap);
            if (mBlurImage.getVisibility() == View.GONE) {
                mPreviewView.setVisibility(View.INVISIBLE);
                mBlurImage.setVisibility(View.VISIBLE);
                mFadeView.setVisibility(View.VISIBLE);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JamiApplication.getInstance().startDaemon();
        setContentView(R.layout.tv_activity_home);
        mBackgroundManager = BackgroundManager.getInstance(this);
        mBackgroundManager.attach(getWindow());
        mPreviewView = findViewById(R.id.previewView);
        mBlurImage = findViewById(R.id.blur);
        mFadeView = findViewById(R.id.fade);
    }

    @Override
    public void onBackPressed() {
        if (GuidedStepSupportFragment.getCurrentGuidedStepSupportFragment(getSupportFragmentManager()) != null) {
//            mIsContactFragmentVisible = false;
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //mDisposable.clear();
        mDisposableBag.add(mAccountService.getObservableAccountList()
                .observeOn(AndroidSchedulers.mainThread())
                .firstElement()
                .subscribe(accounts -> {
                    if (accounts.isEmpty()) {
                        startActivity(new Intent(this, TVAccountWizard.class));
                    }
                }));
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        setUpCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraPreview != null) {
            mCamera.setPreviewCallback(null);
            mCameraPreview.stop();
            mCameraPreview = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDisposableBag.dispose();
        if (mCameraManager != null) {
            mCameraManager.unregisterAvailabilityCallback((CameraManager.AvailabilityCallback) mCameraAvailabilityCallback);
        }
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        if (mBlurOutputBitmap != null){
            in.destroy();
            in = null;
            out.destroy();
            out = null;
            blurIntrinsic.destroy();
            blurIntrinsic = null;
            mBlurOut.destroy();
            mBlurOut = null;
            yuvToRgbIntrinsic.destroy();
            yuvToRgbIntrinsic = null;
            mBlurOutputBitmap.recycle();
            mBlurOutputBitmap = null;
            rs.destroy();
            rs = null;
        }
    }

    private Camera getCameraInstance() {
        try {
            int currentCamera = 0;
            mCamera = Camera.open(currentCamera);
        }
        catch (RuntimeException e) {
            Log.e(TAG, "failed to open camera");
        }
        return mCamera;
    }

    private void setUpCamera() {
        mDisposableBag.add(Single.fromCallable(this::getCameraInstance)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(camera -> {
                    Log.w(TAG, "setUpCamera()");
                    Camera.Parameters params = camera.getParameters();
                    Camera.Size selectSize = null;
                    for (Camera.Size size :  params.getSupportedPictureSizes()) {
                        if (size.width == 1280 && size.height == 720) {
                            selectSize = size;
                            break;
                        }
                    }
                    if (selectSize == null)
                        throw new IllegalStateException("No supported size");
                    Log.w(TAG, "setUpCamera() selectSize " + selectSize.width + "x" + selectSize.height);
                    params.setPictureSize(selectSize.width, selectSize.height);
                    params.setPreviewSize(selectSize.width, selectSize.height);
                    camera.setParameters(params);
                    mBlurImage.setVisibility(View.VISIBLE);
                    if (mCameraManager == null) {
                        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                        mCameraManager.registerAvailabilityCallback((CameraManager.AvailabilityCallback) mCameraAvailabilityCallback, null);
                    }
                    mCameraPreview = new CameraPreview(this, camera);
                    mPreviewView.removeAllViews();
                    mPreviewView.addView(mCameraPreview, 0);
                    camera.setErrorCallback(mErrorCallback);
                    camera.setPreviewCallback(mPreviewCallback);
                }, e -> mBackgroundManager.setDrawable(ContextCompat.getDrawable(HomeActivity.this, R.drawable.tv_background))));
    }

}