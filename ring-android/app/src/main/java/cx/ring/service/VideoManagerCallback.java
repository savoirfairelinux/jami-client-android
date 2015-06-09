package cx.ring.service;

import android.content.Context;
import android.hardware.Camera;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class VideoManagerCallback extends VideoCallback {

    private static final String TAG = "VideoManagerCb";

    public VideoManagerCallback() {
        super();

    }

    public void init() {
        int number_cameras = getNumberOfCameras();
        for(int i = 0; i < number_cameras; i++) {
            RingserviceJNI.addVideoDevice(Integer.toString(i));
            Log.d(TAG, "Camera number " + i);
        }
    }

    @Override
    public void getCameraInfo(String camid, IntVect formats, StringVect sizes, UintVect rates) {
        int id = Integer.valueOf(camid);

        if (id < 0 || id >= getNumberOfCameras())
            return;

        Camera cam;
        try {
            cam = Camera.open(id);
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
            return;
        }

        Camera.CameraInfo camInfo =     new Camera.CameraInfo();
        Camera.getCameraInfo(id, camInfo);

        //ringInfo.facing = camInfo.facing;

        Camera.Parameters param = cam.getParameters();
        cam.release();

        getFormats(param, formats);
        getSizes(param, sizes);
        getRates(param, rates);
    }

    private int getNumberOfCameras() {
        return Camera.getNumberOfCameras();
    }

    private void getFormats(Camera.Parameters param, IntVect formats_) {
        for(int fmt : param.getSupportedPreviewFormats()) {
            formats_.add(fmt);
        }
    }

    private void getSizes(Camera.Parameters param, StringVect sizes) {
        for(Camera.Size s : param.getSupportedPreviewSizes()) {
            sizes.add(s.width + "x" + s.height);
        }
    }

    private void getRates(Camera.Parameters param, UintVect rates_) {
        for(int fps[] : param.getSupportedPreviewFpsRange()) {
            /* documentation states that the value is framerate * 1000 but it is only the framerate */
            rates_.add((int) fps[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] * 1000);
            rates_.add((int) fps[Camera.Parameters.PREVIEW_FPS_MAX_INDEX] * 1000);
        }
    }
}
