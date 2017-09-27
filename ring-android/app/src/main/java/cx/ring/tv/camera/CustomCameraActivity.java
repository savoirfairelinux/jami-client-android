package cx.ring.tv.camera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import cx.ring.R;

public class CustomCameraActivity extends Activity {
    private Camera mCamera;
    private CameraPreview mCameraPreview;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camerapicker);

        mCamera = getCameraInstance();
        mCameraPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mCameraPreview);

        Button captureButton = (Button) findViewById(R.id.button_capture);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.takePicture(null, null, mPicture);
            }
        });
    }
    int cameraFront;
    int cameraBack;
    int currentCamera;
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
     *
     * @return
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

    Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] input, Camera camera) {
            Bundle conData = new Bundle();
           // Bitmap original = BitmapFactory.decodeByteArray(input , 0, input.length);
            conData.putByteArray("data", input);
            Intent intent = new Intent();
            intent.putExtras(conData);
            if(getParent() == null) {
                setResult(RESULT_OK, intent);
            } else {
                getParent().setResult(RESULT_OK, intent);
            }
            finish();
        }
    };

    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Ring");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("RIng", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }
}