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

    private Map<String, Camera> devices;

    public VideoManagerCallback() {
        super();

        devices = new HashMap<String,Camera>(Camera.getNumberOfCameras());
    }

    public void init() {
        int number_cameras = Camera.getNumberOfCameras();
        for(int i = 0; i < number_cameras; i++) {
            RingserviceJNI.addVideoDevice(Integer.toString(i));
            Log.d(TAG, "Camera number " + i);
        }
    }

    @Override
    public void acquireCamera(String cameraId) {
        //name = "";
        if (!devices.containsKey(cameraId)) {
            Camera camera = Camera.open(Integer.valueOf(cameraId));
            if (camera == null)
                return;

            devices.put(cameraId, camera);
        }

        /*
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(Integer.valueOf(cameraId), info);
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
            name = "FRONT." + cameraId;
        else
            name = "BACK." + cameraId;
        */
    }

    @Override
    public void releaseCamera(String cameraId) {
        Camera camera = devices.get(cameraId);
        if (camera == null)
            return;

        camera.release();
    }

    public void getCameraFormats(String cameraId, IntVect formats_) {
        Camera camera = devices.get(cameraId);
        if (camera == null)
            return;

        Camera.Parameters parameters = camera.getParameters();

        for(int fmt : parameters.getSupportedPreviewFormats()) {
            formats_.add(fmt);
        }
    }

    public void getCameraSizes(String cameraId, int format, StringVect sizes) {
        Camera camera = devices.get(cameraId);
        if (camera == null)
            return;

        Camera.Parameters parameters = camera.getParameters();
        for(Camera.Size s : parameters.getSupportedPreviewSizes()) {
            sizes.add(s.width + "x" + s.height);
        }

    }

    public void getCameraRates(String cameraId, int format, String size, FloatVect rates_) {
        Camera camera = devices.get(cameraId);
        if (camera == null)
            return;

        Camera.Parameters parameters = camera.getParameters();
        for(int fps[] : parameters.getSupportedPreviewFpsRange()) {
            int fps_max = fps[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
            /* documentation states that the value is framerate * 1000 but it is only the framerate */
            rates_.add((float) fps_max /*/ 1000*/);
        }
    }
}
