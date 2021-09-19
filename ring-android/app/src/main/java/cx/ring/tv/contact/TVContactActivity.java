/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.tv.contact;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.BackgroundManager;

import cx.ring.R;
import cx.ring.tv.camera.CameraPreview;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@AndroidEntryPoint
public class TVContactActivity extends FragmentActivity {
    private static final String TAG = TVContactActivity.class.getSimpleName();

    private final CompositeDisposable mDisposable = new CompositeDisposable();
    public static final String SHARED_ELEMENT_NAME = "photo";
    public static final String CONTACT_REQUEST_URI = "uri";
    public static final String TYPE_CONTACT_REQUEST_INCOMING = "incoming";
    public static final String TYPE_CONTACT_REQUEST_OUTGOING = "outgoing";

    private int mDisplayWidth, mDisplayHeight;
    private BackgroundManager mBackgroundManager;
    private ImageView mBackgroundImage;
    private FrameLayout mPreviewView;
    private Camera mCamera;
    private CameraPreview mCameraPreview;
    private CameraManager mCameraManager;

    private Bitmap mBackgroundBitmap;
    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Allocation in, out;

    private final Camera.ErrorCallback mErrorCallback = new Camera.ErrorCallback() {
        @Override
        public void onError(int error, Camera camera) {
            mBackgroundImage.setVisibility(View.INVISIBLE);
            mBackgroundManager.setDrawable(ContextCompat.getDrawable(TVContactActivity.this, R.drawable.tv_background));
        }
    };

    private final Object mCameraAvailabilityCallback = new CameraManager.AvailabilityCallback() {
        @Override
        public void onCameraAvailable(String cameraId) {
            super.onCameraAvailable(cameraId);
            if (mBackgroundImage.getVisibility() == View.INVISIBLE) {
                setUpCamera();
            }
        }
    };

    private final Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (mBackgroundBitmap == null) {
                mBackgroundBitmap = Bitmap.createBitmap(mDisplayWidth, mDisplayHeight, Bitmap.Config.ARGB_8888);
                rs = RenderScript.create(TVContactActivity.this);
                yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
                Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(data.length);
                Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(mDisplayWidth).setY(mDisplayHeight);
                in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
                out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
            }
            in.copyFrom(data);
            yuvToRgbIntrinsic.setInput(in);
            yuvToRgbIntrinsic.forEach(out);
            out.copyTo(mBackgroundBitmap);

            mBackgroundImage.setImageBitmap(mBackgroundBitmap);
        }
    };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tv_frag_contact);

        mBackgroundManager = BackgroundManager.getInstance(this);
        mBackgroundManager.attach(getWindow());
        mPreviewView = findViewById(R.id.previewView);
        mBackgroundImage = findViewById(R.id.background);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mDisplayHeight = displayMetrics.heightPixels;
        mDisplayWidth = displayMetrics.widthPixels;
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
        mDisposable.dispose();
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
            yuvToRgbIntrinsic.destroy();
            mBackgroundBitmap.recycle();
            mBackgroundBitmap = null;
            rs = null;
            in = null;
            out = null;
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
        mDisposable.add(Single.fromCallable(this::getCameraInstance)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(camera -> {
                    mCamera.setErrorCallback(mErrorCallback);
                    mCamera.setPreviewCallback(mPreviewCallback);
                    mCameraPreview = new CameraPreview(this, mCamera);
                    mPreviewView.removeAllViews();
                    mPreviewView.addView(mCameraPreview, 0);
                    mBackgroundImage.setVisibility(View.VISIBLE);
                    if (mCameraManager == null) {
                        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                        mCameraManager.registerAvailabilityCallback((CameraManager.AvailabilityCallback) mCameraAvailabilityCallback, null);
                    }
                }, e -> {
                    mBackgroundManager.setDrawable(ContextCompat.getDrawable(TVContactActivity.this, R.drawable.tv_background));
                }));
    }

}
