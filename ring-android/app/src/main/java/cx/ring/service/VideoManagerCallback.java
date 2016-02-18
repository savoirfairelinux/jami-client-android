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
