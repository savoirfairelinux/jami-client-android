/*
 *  Copyright (C) 2015-2016 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *          Damien Riegel <damien.riegel@savoirfairelinux.com>
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
package cx.ring.service;

import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.HashMap;


public class VideoManagerCallback extends VideoCallback implements Camera.PreviewCallback
{
    private static final String TAG = VideoManagerCallback.class.getSimpleName();

    private final DRingService mService;
    //private Camera camera = null;

    /*private class Parameters {
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
    }*/

    private HashMap<String, DRingService.VideoParams> params = new HashMap<>();

    public VideoManagerCallback(DRingService s) {
        mService = s;
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
        mService.decodingStarted(id, shm_path, w, h, is_mixer);
    }

    @Override
    public void decodingStopped(String id, String shm_path, boolean is_mixer) {
        mService.decodingStopped(id);
    }

    public void onPreviewFrame(byte[] data, Camera camera) {
        int ptr = RingserviceJNI.obtainFrame(data.length);
        if (ptr != 0)
            RingserviceJNI.setVideoFrame(data, data.length, ptr);
        RingserviceJNI.releaseFrame(ptr);
    }

    public void setParameters(String camid, int format, int width, int height, int rate) {
        int id = Integer.valueOf(camid);
        DRingService.VideoParams p = new DRingService.VideoParams(id, format, width, height, rate);
        params.put(camid, p);
    }

    public void startCapture(String camid) {
        DRingService.VideoParams p = params.get(camid);
        if (p == null)
            return;

        mService.startCapture(p);

        /*

        stopCapture();


        int format = p.format;
        int width = p.width;
        int height = p.height;
        int rate = p.rate;


        try {
            camera = Camera.open(id);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return;
        }

        try {
            SurfaceHolder surface = DRingService.mCameraPreviewSurface.get();
            if (surface != null) {
                surface.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
                camera.setPreviewDisplay(surface);
            } else {
                Log.w(TAG, "Can't start capture: no surface registered.");
            }
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
        camera.startPreview();*/
    }

    public void stopCapture() {
        /*if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }*/
        mService.stopCapture();
    }

    @Override
    public void getCameraInfo(String camid, IntVect formats, UintVect sizes, UintVect rates) {
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

    private void getSizes(Camera.Parameters param, UintVect sizes) {
        for(Camera.Size s : param.getSupportedPreviewSizes()) {
            sizes.add(s.width);
            sizes.add(s.height);
        }
    }

    private void getRates(Camera.Parameters param, UintVect rates_) {
        for(int fps[] : param.getSupportedPreviewFpsRange()) {
            int rate = (fps[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] + fps[Camera.Parameters.PREVIEW_FPS_MAX_INDEX])/2;
            rates_.add(rate);
        }
    }
}
