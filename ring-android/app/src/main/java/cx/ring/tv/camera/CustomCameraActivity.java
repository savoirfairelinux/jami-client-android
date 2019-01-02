/*
 * Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Loïc Siret <loic.siret@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.tv.camera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.widget.FrameLayout;

import butterknife.ButterKnife;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.utils.BitmapUtils;

public class CustomCameraActivity extends Activity {
    int cameraFront;
    int cameraBack;
    int currentCamera;
    private Camera mCamera;
    private CameraPreview mCameraPreview;
    private final Camera.PictureCallback mPicture = (input, camera) -> {
        Bitmap photo = BitmapFactory.decodeByteArray(input, 0, input.length);
        Bitmap scaled = BitmapUtils.createScaledBitmap(photo, 256);
        Intent intent = new Intent();
        intent.putExtra("data", scaled);
        if (getParent() == null) {
            setResult(RESULT_OK, intent);
        } else {
            getParent().setResult(RESULT_OK, intent);
        }
        finish();
    };

    @OnClick(R.id.button_capture)
    public void takePicture(){
        mCamera.takePicture(null, null, mPicture);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camerapicker);
        ButterKnife.bind(this);
        mCamera = getCameraInstance();
        mCameraPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = findViewById(R.id.camera_preview);
        preview.addView(mCameraPreview);
    }

    public void initVideo() {
        int numberCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo camInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberCameras; i++) {
            Camera.getCameraInfo(i, camInfo);
            if (camInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraFront = i;

            } else {
                cameraBack = i;
            }
        }
        currentCamera = cameraFront;
    }

    /**
     * Helper method to access the camera returns null if it cannot get the
     * camera or does not exist
     */
    private Camera getCameraInstance() {
        initVideo();
        Camera camera = null;
        try {
            camera = Camera.open(currentCamera);
        } catch (Exception e) {
            // cannot get camera or does not exist
        }
        return camera;
    }
}