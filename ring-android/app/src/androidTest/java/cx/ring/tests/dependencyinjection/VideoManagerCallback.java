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
package cx.ring.tests.dependencyinjection;

import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.Camera;
import android.util.Log;
import android.util.LongSparseArray;

import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import javax.inject.Inject;

import cx.ring.daemon.StringMap;
import cx.ring.model.DaemonEvent;
import cx.ring.service.DRingService;
import cx.ring.services.HardwareService;

public class VideoManagerCallback implements Observer {
    private static final String TAG = VideoManagerCallback.class.getSimpleName();

    @Inject
    HardwareService mHardwareService;

    private final LongSparseArray<DeviceParams> mNativeParams = new LongSparseArray<>();
    private final HashMap<String, DRingService.VideoParams> mParams = new HashMap<>();

    @Override
    public void update(Observable o, Object arg) {
        if (!(arg instanceof DaemonEvent)) {
            return;
        }

        DaemonEvent event = (DaemonEvent) arg;
        switch (event.getEventType()) {
            case DECODING_STARTED:
                break;
            case DECODING_STOPPED:
                break;
            case GET_CAMERA_INFO:
                break;
            case SET_PARAMETERS:
                break;
            case START_CAPTURE:
                break;
            case STOP_CAPTURE:
                break;
            default:
                Log.i(TAG, "Unknown daemon event");
                break;
        }
    }

    class DeviceParams {
        Point size;
        long rate;
        Camera.CameraInfo infos;

        public StringMap toMap(int orientation) {
            StringMap map = new StringMap();
            boolean rotated = (size.x > size.y) == (orientation == Configuration.ORIENTATION_PORTRAIT);
            map.set("size", Integer.toString(rotated ? size.y : size.x) + "x" + Integer.toString(rotated ? size.x : size.y));
            map.set("rate", Long.toString(rate));
            return map;
        }
    }

    public int cameraFront = 0;
    public int cameraBack = 0;

    public void init() {
        int number_cameras = getNumberOfCameras();
        Camera.CameraInfo camInfo = new Camera.CameraInfo();
        for (int i = 0; i < number_cameras; i++) {
            mHardwareService.addVideoDevice(Integer.toString(i));
            Camera.getCameraInfo(i, camInfo);
            if (camInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraFront = i;
            } else {
                cameraBack = i;
            }
            Log.d(TAG, "Camera number " + i);
        }
        mHardwareService.setDefaultVideoDevice(Integer.toString(cameraFront));
    }

    private int getNumberOfCameras() {
        return Camera.getNumberOfCameras();
    }

}
