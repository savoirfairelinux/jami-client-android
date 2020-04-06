/*
 * Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Loïc Siret <loic.siret@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *  Author: AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
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
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.utils.AndroidFileUtils;
import cx.ring.utils.ContentUriHandler;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class CustomCameraActivity extends Activity {
    private static final String TAG = "CustomCameraActivity";
    public static final String TYPE_IMAGE = "image/jpeg";
    public static final String TYPE_VIDEO = "video";

    private int cameraFront = -1;
    private int cameraBack = -1;
    private int currentCamera = 0;

    private MediaRecorder recorder;
    private boolean mRecording = false;
    private boolean mActionVideo = false;

    private File mVideoFile;

    @BindView(R.id.button_video)
    FloatingActionButton mButtonVideo;
    @BindView(R.id.button_picture)
    FloatingActionButton mButtonPicture;

    private Camera mCamera;
    private CameraPreview mCameraPreview;
    private final Camera.PictureCallback mPicture = (input, camera) -> Single.fromCallable(() ->  {
            mCameraPreview.stop();
            File file = AndroidFileUtils.createImageFile(this);
            try (OutputStream out = new FileOutputStream(file)) {
                out.write(input);
                out.flush();
            }
            return ContentUriHandler.getUriForFile(this, ContentUriHandler.AUTHORITY_FILES, file);
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(uri -> {
                setResult(RESULT_OK, new Intent()
                        .putExtra(MediaStore.EXTRA_OUTPUT, uri)
                        .setType(TYPE_IMAGE));
                finish();
            }, e -> {
                Log.e(TAG, "Error saving picture", e);
                setResult(RESULT_CANCELED);
                finish();
            });

    @OnClick(R.id.button_picture)
    public void takePicture() {
        if (mRecording)
            releaseMediaRecorder();
        if (mCamera != null) {
            mButtonPicture.setEnabled(false);
            mButtonVideo.setVisibility(View.GONE);
            try {
                mCamera.takePicture(null, null, mPicture);
            } catch (Exception e) {
                Log.w(TAG, "Error taking picture", e);
            }
        }
    }

    @OnClick(R.id.button_video)
    public void takeVideo() {
        if (mRecording) {
            releaseMediaRecorder();
            mCameraPreview.stop();
            Intent intent = new Intent()
                    .putExtra(MediaStore.EXTRA_OUTPUT, ContentUriHandler.getUriForFile(this, ContentUriHandler.AUTHORITY_FILES, mVideoFile))
                    .setType(TYPE_VIDEO);
            setResult(RESULT_OK, intent);
            finish();
            mButtonVideo.setImageResource(R.drawable.baseline_videocam_24);
            return;
        }
        if (mCamera != null) {
            initRecorder();
            mButtonVideo.setImageResource(R.drawable.lb_ic_stop);
            mButtonPicture.setVisibility(View.GONE);
        }
        mRecording = !mRecording;
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camerapicker);
        ButterKnife.bind(this);

        if (getIntent().getAction() != null) {
            mActionVideo = getIntent().getAction().equals(MediaStore.ACTION_VIDEO_CAPTURE);
        }

        mCamera = getCameraInstance();
        if (mCamera == null) {
            Toast.makeText(this, "Can't open camera", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        mCameraPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = findViewById(R.id.camera_preview);
        preview.addView(mCameraPreview, 0);

        if (mActionVideo) {
            mButtonVideo.setVisibility(View.VISIBLE);
        }
    }

    public void initVideo() {
        int numberCameras = Camera.getNumberOfCameras();
        if (numberCameras == 0)
            return;
        Camera.CameraInfo camInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberCameras; i++) {
            Camera.getCameraInfo(i, camInfo);
            if (camInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraFront = i;
            } else {
                cameraBack = i;
            }
        }
        currentCamera = cameraFront == -1 ? cameraBack : cameraFront;
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

    private void initRecorder() {
        int videoWidth = mCamera.getParameters().getPreviewSize().width;
        int videoHeight = mCamera.getParameters().getPreviewSize().height;
        mCamera.unlock();
        recorder = new MediaRecorder();
        recorder.setCamera(mCamera);
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

        try {
            mVideoFile = AndroidFileUtils.createVideoFile(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        CamcorderProfile cpHigh = CamcorderProfile.get(Integer.valueOf(getFrontFacingCameraId(manager)), CamcorderProfile.QUALITY_HIGH);
        recorder.setProfile(cpHigh);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            recorder.setOutputFile(mVideoFile);
        }
        recorder.setVideoSize(videoWidth, videoHeight);

        prepareRecorder();
    }

    private void prepareRecorder() {
        recorder.setPreviewDisplay(mCameraPreview.getHolder().getSurface());

        try {
            recorder.prepare();
            recorder.start();
        } catch (IllegalStateException | IOException e) {
            e.printStackTrace();
            finish();
        }
    }

    private String getFrontFacingCameraId(CameraManager cManager){
        try {
            return cManager.getCameraIdList()[0];
        } catch (CameraAccessException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private void releaseMediaRecorder() {
        if (recorder != null) {
            recorder.reset();
            recorder.release();
            recorder = null;
        }
    }
}