/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Authors: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *  Author: Raphaël Brulé <raphael.brule@savoirfairelinux.com>
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

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.client.ConversationActivity;
import net.jami.daemon.Blob;
import net.jami.daemon.JamiService;
import net.jami.daemon.StringMap;
import net.jami.facades.ConversationFacade;
import cx.ring.fragments.ConversationFragment;

import net.jami.services.AccountService;
import net.jami.services.CallService;

import cx.ring.services.NotificationServiceImpl;
import cx.ring.utils.ConversationPath;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.Subject;

@AndroidEntryPoint
public class LocationSharingService extends Service implements LocationListener {
    private static final String TAG = "LocationSharingService";

    public static final int NOTIF_SYNC_SERVICE_ID = 931801;

    public static final String ACTION_START = "startSharing";
    public static final String ACTION_STOP = "stopSharing";
    public static final String EXTRA_SHARING_DURATION = "locationShareDuration";

    public static final String PREFERENCES_LOCATION = "location";
    public static final String PREFERENCES_KEY_POS_LONG = "lastPosLongitude";
    public static final String PREFERENCES_KEY_POS_LAT = "lastPosLatitude";
    public static final int SHARE_DURATION_SEC = 60 * 5;

    @Inject
    ConversationFacade mConversationFacade;

    private final Random mRandom = new Random();
    private final IBinder binder = new LocalBinder();
    private boolean started = false;

    private LocationManager mLocationManager;
    private NotificationManager mNotificationManager;
    private SharedPreferences mPreferences;
    private Handler mHandler;

    private final Subject<Location> mMyLocationSubject = BehaviorSubject.create();
    private final Map<ConversationPath, Date> contactLocationShare = new HashMap<>();
    private final Subject<Set<ConversationPath>> mContactSharingSubject = BehaviorSubject.createDefault(contactLocationShare.keySet());

    private final CompositeDisposable mDisposableBag = new CompositeDisposable();

    public LocationSharingService() {
    }

    public Observable<Location> getMyLocation() {
        return mMyLocationSubject;
    }

    public Observable<Set<ConversationPath>> getContactSharing() {
        return mContactSharingSubject;
    }

    public Observable<Long> getContactSharingExpiration(ConversationPath path) {
        return Observable.timer(1, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .startWithItem(0L)
                .repeat()
                .map(i -> contactLocationShare.get(path).getTime() - SystemClock.elapsedRealtime())
                .onErrorComplete();
    }

    @Override
    public void onCreate() {
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mPreferences = getSharedPreferences(PREFERENCES_LOCATION, Context.MODE_PRIVATE);
        mHandler = new Handler(getMainLooper());
        String posLongitude = mPreferences.getString(PREFERENCES_KEY_POS_LONG, null);
        String posLatitude = mPreferences.getString(PREFERENCES_KEY_POS_LAT, null);
        if (posLatitude != null && posLongitude != null) {
            try {
                Location location = new Location("cache");
                location.setLatitude(Double.parseDouble(posLatitude));
                location.setLongitude(Double.parseDouble(posLongitude));
                mMyLocationSubject.onNext(location);
            } catch (Exception e) {
                Log.w(TAG, "Can't load last location", e);
            }
        }
        if (mLocationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                try {
                    Criteria c = new Criteria();
                    c.setAccuracy(Criteria.ACCURACY_FINE);
                    mLocationManager.requestLocationUpdates(0, 0.f, c, this, null);
                } catch (Exception e) {
                    Log.e(TAG, "Can't start location tracking", e);
                }
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.w(TAG, "onStartCommand " + intent);
        String action = intent.getAction();
        ConversationPath path = ConversationPath.fromIntent(intent);
        long now = SystemClock.elapsedRealtime();

        if (ACTION_START.equals(action)) {
            int duration = intent.getIntExtra(EXTRA_SHARING_DURATION, SHARE_DURATION_SEC);
            long expiration = now + (duration * 1000L);
            if (contactLocationShare.put(path, new Date(expiration)) == null) {
                mContactSharingSubject.onNext(contactLocationShare.keySet());
            }
            mHandler.postAtTime(this::refreshSharing, expiration);

            if (!started) {
                started = true;
                mDisposableBag.add(getNotification(now)
                        .subscribe(notification -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                                startForeground(NOTIF_SYNC_SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
                            else
                                startForeground(NOTIF_SYNC_SERVICE_ID, notification);

                            mHandler.postAtTime(this::refreshNotificationTimer, now + 30 * 1000);
                            JamiApplication.getInstance().startDaemon();
                        }));
                mDisposableBag.add(mMyLocationSubject
                        .throttleLatest(10, TimeUnit.SECONDS)
                        .map(location -> {
                            JSONObject out = new JSONObject();
                            out.put("type", AccountService.Location.Type.position.toString());
                            out.put("lat", location.getLatitude());
                            out.put("long", location.getLongitude());
                            out.put("alt", location.getAltitude());
                            out.put("time",location.getElapsedRealtimeNanos()/1000000L);
                            float bearing = location.getBearing();
                            if (bearing != 0.f)
                                out.put("bearing", bearing);
                            float speed = location.getSpeed();
                            if (speed != 0.f)
                                out.put("speed", speed);
                            return out;
                        })
                        .subscribe(location -> {
                            Log.w(TAG, "location send " + location + " to " + contactLocationShare.size());
                            StringMap msgs = new StringMap();
                            msgs.setRaw(net.jami.services.CallService.MIME_GEOLOCATION, Blob.fromString(location.toString()));
                            for (ConversationPath p : contactLocationShare.keySet())  {
                                JamiService.sendAccountTextMessage(p.getAccountId(), p.getConversationId(), msgs);
                            }
                        }));
            } else {
                mDisposableBag.add(getNotification(now)
                        .subscribe(notification -> mNotificationManager.notify(NOTIF_SYNC_SERVICE_ID, notification)));
            }
        }
        else if (ACTION_STOP.equals(action)) {
            if (path == null)
                contactLocationShare.clear();
            else {
                contactLocationShare.remove(path);

                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("type", net.jami.services.AccountService.Location.Type.stop.toString());
                    jsonObject.put("time", Long.MAX_VALUE);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Log.w(TAG, "location send " + jsonObject + " to " + contactLocationShare.size());
                StringMap msgs = new StringMap();
                msgs.setRaw(CallService.MIME_GEOLOCATION, Blob.fromString(jsonObject.toString()));
                JamiService.sendAccountTextMessage(path.getAccountId(), path.getConversationId(), msgs);
            }

            mContactSharingSubject.onNext(contactLocationShare.keySet());

            if (contactLocationShare.isEmpty()) {
                Log.w(TAG, "stopping sharing " + intent);
                mDisposableBag.clear();
                stopForeground(true);
                stopSelf();
                started = false;
            } else {
                mDisposableBag.add(getNotification(now)
                        .subscribe(notification -> mNotificationManager.notify(NOTIF_SYNC_SERVICE_ID, notification)));
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.w(TAG, "onDestroy");
        if (mLocationManager !=  null) {
            mLocationManager.removeUpdates(this);
        }
        mMyLocationSubject.onComplete();
        mContactSharingSubject.onComplete();
        mDisposableBag.dispose();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    @Override
    public void onLocationChanged(Location location) {
        // Log.w(TAG, "onLocationChanged " + location.toString());
        mMyLocationSubject.onNext(location);
        mPreferences.edit()
                .putString(PREFERENCES_KEY_POS_LAT, Double.toString(location.getLatitude()))
                .putString(PREFERENCES_KEY_POS_LONG, Double.toString(location.getLongitude()))
                .apply();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @NonNull
    private Single<Notification> getNotification(long now) {
        int contactCount = contactLocationShare.size();
        ConversationPath firsPath = contactLocationShare.keySet().iterator().next();
        Date largest = null;
        for (Date d : contactLocationShare.values())
            if (largest == null || d.after(largest))
                largest = d;
        final long largestDate = largest == null ? now : largest.getTime();
        // Log.w(TAG, "getNotification " + firsPath.getContactId());

        return mConversationFacade.getAccountSubject(firsPath.getAccountId())
                .map(account -> account.getContactFromCache(firsPath.getConversationUri()))
                .map(contact -> {
                    String title;
                    final Intent stopIntent = new Intent(ACTION_STOP).setClass(getApplicationContext(), LocationSharingService.class);
                    final Intent contentIntent = new Intent(Intent.ACTION_VIEW, firsPath.toUri(), getApplicationContext(), ConversationActivity.class)
                            .putExtra(ConversationFragment.EXTRA_SHOW_MAP, true)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    if (contactCount == 1) {
                        stopIntent.setData(firsPath.toUri());
                        title = getString(R.string.notif_location_title, contact.getDisplayName());
                    } else {
                        title = getString(R.string.notif_location_multi_title, contactCount);
                    }
                    String subtitle = getString(R.string.notif_location_remaining, (int)Math.ceil((largestDate - now)/(double)(1000 * 60)));

                    return new NotificationCompat.Builder(this, NotificationServiceImpl.NOTIF_CHANNEL_SYNC)
                            .setContentTitle(title)
                            .setContentText(subtitle)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                            .setAutoCancel(false)
                            .setOngoing(false)
                            .setVibrate(null)
                            .setColorized(true)
                            .setColor(getResources().getColor(R.color.color_primary_dark))
                            .setSmallIcon(R.drawable.ic_ring_logo_white)
                            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                            .setOnlyAlertOnce(true)
                            .setDeleteIntent(PendingIntent.getService(getApplicationContext(), mRandom.nextInt(), stopIntent, 0))
                            .setContentIntent(PendingIntent.getActivity(getApplicationContext(), mRandom.nextInt(), contentIntent, 0))
                            .addAction(R.drawable.baseline_location_disabled_24,
                                    getText(R.string.notif_location_action_stop),
                                    PendingIntent.getService(
                                            getApplicationContext(),
                                            0,
                                            stopIntent,
                                            PendingIntent.FLAG_ONE_SHOT))
                            .build();
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void refreshSharing() {
        if (!started)
            return;

        boolean changed = false;
        final Date now = new Date(SystemClock.uptimeMillis());
        Iterator<Map.Entry<ConversationPath, Date>> it = contactLocationShare.entrySet().iterator();
        while (it.hasNext())  {
            Map.Entry<ConversationPath, Date> e = it.next();
            if (e.getValue().before(now)) {
                changed = true;
                it.remove();
            }
        }

        if (changed)
            mContactSharingSubject.onNext(contactLocationShare.keySet());

        if (contactLocationShare.isEmpty()) {
            mDisposableBag.clear();
            stopForeground(true);
            stopSelf();
            started = false;
        } else if (changed) {
            mDisposableBag.add(getNotification(now.getTime())
                    .subscribe(notification -> mNotificationManager.notify(NOTIF_SYNC_SERVICE_ID, notification)));
        }
    }

    private void refreshNotificationTimer() {
        if (!started)
            return;
        long now = SystemClock.uptimeMillis();
        mDisposableBag.add(getNotification(now)
                .subscribe(notification -> mNotificationManager.notify(NOTIF_SYNC_SERVICE_ID, notification)));
        mHandler.postAtTime(this::refreshNotificationTimer, now + (30 * 1000));
    }

    public boolean isSharing(ConversationPath path) {
        return contactLocationShare.get(path) != null;
    }

    public class LocalBinder extends Binder {
        public LocationSharingService getService() {
            return LocationSharingService.this;
        }
    }
}
