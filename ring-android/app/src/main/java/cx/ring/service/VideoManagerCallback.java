package cx.ring.service;

import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.HashMap;


public class VideoManagerCallback extends VideoCallback implements Camera.PreviewCallback {
    private static final String TAG = "VideoManagerCb";
    private Camera camera = null;
    private long mWindow = 0;

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

    @Override
    public void decodingStarted(String id, String shm_path, int w, int h, boolean is_mixer) {
        Surface surface = DRingService.mVideoPreviewSurface.getHolder().getSurface();
        mWindow = RingserviceJNI.acquireNativeWindow(surface);
        if (mWindow == 0)
            return;
        RingserviceJNI.setNativeWindowGeometry(mWindow, w, h);
        RingserviceJNI.registerVideoCallback(id, mWindow);
    }

    @Override
    public void decodingStopped(String id, String shm_path, boolean is_mixer) {
        if (mWindow != 0) {
            RingserviceJNI.releaseNativeWindow(mWindow);
            mWindow = 0;
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
            SurfaceView surfaceView = DRingService.mCameraPreviewSurface;
            surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            camera.setPreviewDisplay(surfaceView.getHolder());
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
            Log.w(TAG, "Adding format: " + fmt);
            formats_.add(fmt);
        }
    }

    private void getSizes(Camera.Parameters param, StringVect sizes) {
        for(Camera.Size s : param.getSupportedPreviewSizes()) {
            Log.w(TAG, "Adding size: " + s.width + "x" + s.height);
            sizes.add(s.width + "x" + s.height);
        }
    }

    private void getRates(Camera.Parameters param, UintVect rates_) {
        for(int fps[] : param.getSupportedPreviewFpsRange()) {
            int rate = fps[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] + fps[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
            Log.w(TAG, "Adding rate: " + fps[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] + " - " + fps[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
            rate /= 2; /* mean */
            rate /= 1000;
            rates_.add(rate);
        }
    }
}
