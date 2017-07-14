/*
 *  Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Regis Montoya <r3gis.3R@gmail.com>
 *  Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 * This class is used to listen to the accelerometer to monitor the orientation of the phone. The client of this class is notified when the
 * orientation changes between horizontal and vertical.
 */
public final class AccelerometerListener {
    private SensorManager mSensorManager;
    private Sensor mSensor;

    // mOrientation is the orientation value most recently reported to the client.
    private int mOrientation;

    // mPendingOrientation is the latest orientation computed based on the sensor value.
    // This is sent to the client after a rebounce delay, at which point it is copied to
    // mOrientation.
    private int mPendingOrientation;

    private OrientationListener mListener;

    // Device orientation
    public static final int ORIENTATION_UNKNOWN = 0;
    public static final int ORIENTATION_VERTICAL = 1;
    public static final int ORIENTATION_HORIZONTAL = 2;

    private static final int ORIENTATION_CHANGED = 1234;

    private static final int VERTICAL_DEBOUNCE = 100;
    private static final int HORIZONTAL_DEBOUNCE = 500;
    private static final double VERTICAL_ANGLE = 50.0;

    public interface OrientationListener {
        public void orientationChanged(int orientation);
    }

    public AccelerometerListener(Context context, OrientationListener listener) {
        mListener = listener;
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void enable(boolean enable) {
//        if (DEBUG)
//            Log.d(TAG, "enable(" + enable + ")");
        synchronized (this) {
            if (enable) {
                mOrientation = ORIENTATION_UNKNOWN;
                mPendingOrientation = ORIENTATION_UNKNOWN;
                mSensorManager.registerListener(mSensorListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                mSensorManager.unregisterListener(mSensorListener);
                mHandler.removeMessages(ORIENTATION_CHANGED);
            }
        }
    }

    private void setOrientation(int orientation) {
        synchronized (this) {
            if (mPendingOrientation == orientation) {
                // Pending orientation has not changed, so do nothing.
                return;
            }

            // Cancel any pending messages.
            // We will either start a new timer or cancel alltogether
            // if the orientation has not changed.
            mHandler.removeMessages(ORIENTATION_CHANGED);

            if (mOrientation != orientation) {
                // Set timer to send an event if the orientation has changed since its
                // previously reported value.
                mPendingOrientation = orientation;
                Message m = mHandler.obtainMessage(ORIENTATION_CHANGED);
                // set delay to our debounce timeout
                int delay = (orientation == ORIENTATION_VERTICAL ? VERTICAL_DEBOUNCE : HORIZONTAL_DEBOUNCE);
                mHandler.sendMessageDelayed(m, delay);
            } else {
                // no message is pending
                mPendingOrientation = ORIENTATION_UNKNOWN;
            }
        }
    }

    private void onSensorEvent(double x, double y, double z) {
//        if (VDEBUG)
//            Log.d(TAG, "onSensorEvent(" + x + ", " + y + ", " + z + ")");

        // If some values are exactly zero, then likely the sensor is not powered up yet.
        // ignore these events to avoid false horizontal positives.
        if (x == 0.0 || y == 0.0 || z == 0.0)
            return;

        // magnitude of the acceleration vector projected onto XY plane
        double xy = Math.sqrt(x * x + y * y);
        // compute the vertical angle
        double angle = Math.atan2(xy, z);
        // convert to degrees
        angle = angle * 180.0 / Math.PI;
        int orientation = (angle > VERTICAL_ANGLE ? ORIENTATION_VERTICAL : ORIENTATION_HORIZONTAL);
//        if (VDEBUG)
//            Log.d(TAG, "angle: " + angle + " orientation: " + orientation);
        setOrientation(orientation);
    }

    SensorEventListener mSensorListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            onSensorEvent(event.values[0], event.values[1], event.values[2]);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // ignore
        }
    };

    Handler mHandler = new AccelerometerHandler(this);

    private static class AccelerometerHandler extends Handler {
        WeakReference<AccelerometerListener> l;

        AccelerometerHandler(AccelerometerListener listener) {
            l = new WeakReference<AccelerometerListener>(listener);
        }

        public void handleMessage(Message msg) {
            AccelerometerListener listener = l.get();
            if (listener == null) {
                return;
            }
            switch (msg.what) {
            case ORIENTATION_CHANGED:
                synchronized (listener) {
                    listener.mOrientation = listener.mPendingOrientation;
//                    if (DEBUG) {
//                        Log.d(TAG, "orientation: "
//                                + (listener.mOrientation == ORIENTATION_HORIZONTAL ? "horizontal"
//                                        : (listener.mOrientation == ORIENTATION_VERTICAL ? "vertical" : "unknown")));
//                    }
                    listener.mListener.orientationChanged(listener.mOrientation);
                }
                break;
            }
        }
    };
}