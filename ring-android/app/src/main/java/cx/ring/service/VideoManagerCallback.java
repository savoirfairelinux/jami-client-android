package cx.ring.service;

import android.hardware.Camera;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;


public class VideoManagerCallback extends VideoCallback implements Camera.PreviewCallback {
    private static final String TAG = "VideoManagerCb";
    private Camera camera = null;

    private class Parameters {
        public Parameters(int format, int width, int height, int rate) {
            this.format = format;
            this.width = width;
            this.height = height;
            this.rate = rate;
        }

        public int format;
        public int width;
        public int height;
        public int rate;
    }

    private HashMap<String,Parameters> params;

    public VideoManagerCallback() {
        super();
        params = new HashMap<String, Parameters>();
    }

    public void init() {
        int number_cameras = getNumberOfCameras();
        for(int i = 0; i < number_cameras; i++) {
            RingserviceJNI.addVideoDevice(Integer.toString(i));
            Log.d(TAG, "Camera number " + i);
        }
    }

    public void onPreviewFrame(byte[] data, Camera camera) {
        int ptr = RingserviceJNI.obtainFrame(data.length);
        if (ptr != 0)
            RingserviceJNI.setVideoFrame(data, data.length, ptr);
        RingserviceJNI.releaseFrame(ptr);
    }

    public void setParameters(String camid, int format, int width, int height, int rate) {
        Parameters p = new Parameters(format, width, height, rate);
        params.put(camid, p);
    }

    public void startCapture(String camid) {
        stopCapture();

        Parameters p = params.get(camid);
        if (p == null)
            return;

        int format = p.format;
        int width = p.width;
        int height = p.height;
        int rate = p.rate;

        int id = Integer.valueOf(camid);

        try {
            camera = Camera.open(id);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return;
        }

        try {
            camera.setPreviewTexture(null);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return;
        }

        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewFormat(format);
        parameters.setPreviewSize(width, height);
        for(int[] fps : parameters.getSupportedPreviewFpsRange()) {
            if (rate >= fps[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] &&
                rate <= fps[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]) {
                parameters.setPreviewFpsRange(fps[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                                              fps[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
            }
        }

        try {
            camera.setParameters(parameters);
        } catch (RuntimeException e) {
            Log.e(TAG, e.getMessage());
        }

        camera.setPreviewCallback(this);
        camera.startPreview();
    }

    public void stopCapture() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
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
