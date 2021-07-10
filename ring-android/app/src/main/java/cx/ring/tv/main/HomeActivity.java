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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.camera.view.PreviewView;
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

    private final CompositeDisposable mDisposable = new CompositeDisposable();
    private final CompositeDisposable mDisposableBag = new CompositeDisposable();

    @Inject
    AccountService mAccountService;

    private int mBlurWidth, mBlurHeight;
    private int mDisplayWidth, mDisplayHeight;
    private BackgroundManager mBackgroundManager;
    private ImageView mBlurImage;
    private PreviewView mPreviewView;
    private View mFadeView;
    private Camera mCamera;
    private CameraPreview mCameraPreview;
    private CameraManager mCameraManager;

    private Bitmap mBackgroundBitmap, mBlurInputBitmap, mBlurOutputBitmap;
    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private ScriptIntrinsicBlur blurIntrinsic;
    private Allocation in, out, mBlurIn, mBlurOut;

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
            super.onCameraAvailable(cameraId);
            if (mBlurImage.getVisibility() == View.INVISIBLE) {
                setUpCamera();
            }
        }
    };

    private final Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (mBackgroundBitmap == null) {
                mBackgroundBitmap = Bitmap.createBitmap(mDisplayWidth, mDisplayHeight, Bitmap.Config.ARGB_8888);
                rs = RenderScript.create(HomeActivity.this);
                yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
                blurIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
                blurIntrinsic.setRadius(BLUR_RADIUS);
                Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(data.length);
                Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(mDisplayWidth).setY(mDisplayHeight);
                in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
                out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
            }
            in.copyFrom(data);
            yuvToRgbIntrinsic.setInput(in);
            yuvToRgbIntrinsic.forEach(out);
            out.copyTo(mBackgroundBitmap);

            if (getSupportFragmentManager().getFragments().get(getSupportFragmentManager().getBackStackEntryCount()) instanceof TVContactFragment) {
                mBlurImage.setImageBitmap(mBackgroundBitmap);
                if (mFadeView != null) {
                    mFadeView.setVisibility(View.INVISIBLE);
                }
                return;
            }

            ///// blur image
            mBlurInputBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap, mBlurWidth, mBlurHeight, false);
            mBlurOutputBitmap = Bitmap.createBitmap(mBlurInputBitmap);
            if (mBlurIn == null) {
                mBlurIn = Allocation.createFromBitmap(rs, mBlurInputBitmap);
                mBlurOut = Allocation.createFromBitmap(rs, mBlurOutputBitmap);
            }
            mBlurIn.copyFrom(mBlurInputBitmap);
            blurIntrinsic.setInput(mBlurIn);
            blurIntrinsic.forEach(mBlurOut);
            mBlurOut.copyTo(mBlurOutputBitmap);
            mBlurImage.setImageBitmap(mBlurOutputBitmap);
            if (mFadeView != null) {
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

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mDisplayHeight = displayMetrics.heightPixels;
        mDisplayWidth = displayMetrics.widthPixels;
        mBlurWidth= Math.round(mDisplayWidth * BITMAP_SCALE);
        mBlurHeight = Math.round(mDisplayHeight * BITMAP_SCALE);
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
        mDisposable.clear();
        mDisposable.add(mAccountService.getObservableAccountList()
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
        if (mBackgroundBitmap != null){
            rs.destroy();
            in.destroy();
            out.destroy();
            blurIntrinsic.destroy();
            yuvToRgbIntrinsic.destroy();
            mBackgroundBitmap.recycle();
            mBlurInputBitmap.recycle();
            mBlurOutputBitmap.recycle();
            mBackgroundBitmap = null;
            mBlurInputBitmap = null;
            mBlurOutputBitmap = null;
            rs = null;
            in = null;
            out = null;
            mBlurIn = null;
            mBlurOut = null;
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
                    mCamera.setErrorCallback(mErrorCallback);
                    mCamera.setPreviewCallback(mPreviewCallback);
                    mCameraPreview = new CameraPreview(this, mCamera);
                    mPreviewView.removeAllViews();
                    mPreviewView.addView(mCameraPreview, 0);
                    mBlurImage.setVisibility(View.VISIBLE);
                    if (mCameraManager == null) {
                        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                        mCameraManager.registerAvailabilityCallback((CameraManager.AvailabilityCallback) mCameraAvailabilityCallback, null);
                    }
                }, e -> {
                    mBackgroundManager.setDrawable(ContextCompat.getDrawable(HomeActivity.this, R.drawable.tv_background));
                }));
    }

}