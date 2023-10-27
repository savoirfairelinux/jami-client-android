/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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
package cx.ring.service

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import cx.ring.R
import cx.ring.application.JamiApplication
import cx.ring.client.ConversationActivity
import cx.ring.fragments.ConversationFragment
import cx.ring.services.NotificationServiceImpl
import cx.ring.utils.ContentUriHandler
import cx.ring.utils.ConversationPath
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.Subject
import net.jami.daemon.JamiService
import net.jami.daemon.StringMap
import net.jami.services.AccountService
import net.jami.services.CallService
import net.jami.services.ContactService
import net.jami.services.ConversationFacade
import org.json.JSONException
import org.json.JSONObject
import java.util.Date
import java.util.Random
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.ceil

@AndroidEntryPoint
class LocationSharingService : Service(), LocationListener {
    @Inject
    lateinit var mConversationFacade: ConversationFacade
    @Inject
    lateinit var contactService: ContactService

    private val mRandom = Random()
    private val binder: IBinder = LocalBinder()
    private var started = false
    private var mLocationManager: LocationManager? = null
    private lateinit var mNotificationManager: NotificationManager
    private lateinit var mPreferences: SharedPreferences
    private lateinit var mHandler: Handler
    private val mMyLocationSubject: Subject<Location> = BehaviorSubject.create()
    private val contactLocationShare: MutableMap<ConversationPath, Date> = HashMap()
    private val mContactSharingSubject: Subject<Set<ConversationPath>> =
        BehaviorSubject.createDefault(contactLocationShare.keys)
    private val mDisposableBag = CompositeDisposable()

    val myLocation: Observable<Location>
        get() = mMyLocationSubject
    val contactSharing: Observable<Set<ConversationPath>>
        get() = mContactSharingSubject

    fun getContactSharingExpiration(path: ConversationPath): Observable<Long> {
        return Observable.timer(1, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .startWithItem(0L)
            .repeat()
            .map { contactLocationShare[path]!!.time - SystemClock.elapsedRealtime() }
            .onErrorComplete()
    }

    override fun onCreate() {
        super.onCreate()
        mLocationManager = getSystemService(LOCATION_SERVICE) as LocationManager?
        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mHandler = Handler(mainLooper)
        mPreferences = getSharedPreferences(PREFERENCES_LOCATION, MODE_PRIVATE)
        val posLongitude = mPreferences.getString(PREFERENCES_KEY_POS_LONG, null)
        val posLatitude = mPreferences.getString(PREFERENCES_KEY_POS_LAT, null)
        if (posLatitude != null && posLongitude != null) {
            try {
                val location = Location("cache")
                location.latitude = posLatitude.toDouble()
                location.longitude = posLongitude.toDouble()
                mMyLocationSubject.onNext(location)
            } catch (e: Exception) {
                Log.w(TAG, "Can't load last location", e)
            }
        }
        mLocationManager?.let { locationManager ->
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    val c = Criteria()
                    c.accuracy = Criteria.ACCURACY_FINE
                    locationManager.requestLocationUpdates(2500, 0.5f, c, this, null)
                } catch (e: Exception) {
                    Log.e(TAG, "Can't start location tracking", e)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.w(TAG, "onStartCommand $intent")
        val action = intent.action
        val path = ConversationPath.fromIntent(intent)
        val now = SystemClock.elapsedRealtime()
        val uptime = SystemClock.uptimeMillis()
        if (ACTION_START == action) {
            val duration = intent.getIntExtra(EXTRA_SHARING_DURATION, SHARE_DURATION_SEC)
            val expiration = now + duration * 1000L
            if (contactLocationShare.put(path!!, Date(expiration)) == null) {
                mContactSharingSubject.onNext(contactLocationShare.keys)
            }
            mHandler.postAtTime({ refreshSharing() }, uptime + duration * 1000L)
            if (!started) {
                started = true
                mDisposableBag.add(getNotification(now)
                    .subscribe { notification ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                            startForeground(NOTIF_SYNC_SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                        else
                            startForeground(NOTIF_SYNC_SERVICE_ID, notification)
                        mHandler.postAtTime({ refreshNotificationTimer() }, uptime + 30 * 1000)
                        JamiApplication.instance?.startDaemon(this)
                    })
                mDisposableBag.add(mMyLocationSubject
                    .throttleLatest(10, TimeUnit.SECONDS)
                    .map { location ->
                        val out = JSONObject()
                        out.put("type", AccountService.Location.Type.Position.toString())
                        out.put("lat", location.latitude)
                        out.put("long", location.longitude)
                        out.put("alt", location.altitude)
                        out.put("time", location.elapsedRealtimeNanos / 1000000L)
                        val bearing = location.bearing
                        if (bearing != 0f) out.put("bearing", bearing.toDouble())
                        val speed = location.speed
                        if (speed != 0f) out.put("speed", speed.toDouble())
                        out
                    }
                    .subscribe { location: JSONObject ->
                        Log.w(TAG, "location send " + location + " to " + contactLocationShare.size)
                        val msgs = StringMap()
                        msgs.setUnicode(CallService.MIME_GEOLOCATION, location.toString())
                        for (p in contactLocationShare.keys)
                            JamiService.sendAccountTextMessage(p.accountId, p.conversationId, msgs, 1)
                    })
            } else {
                mDisposableBag.add(getNotification(now)
                    .subscribe { notification -> mNotificationManager.notify(NOTIF_SYNC_SERVICE_ID, notification) })
            }
        } else if (ACTION_STOP == action) {
            if (path == null)
                contactLocationShare.clear()
            else {
                val removed = contactLocationShare.remove(path)
                if (removed != null) {
                    val jsonObject = JSONObject()
                    try {
                        jsonObject.put("type", AccountService.Location.Type.Stop.toString())
                        jsonObject.put("time", Long.MAX_VALUE)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                    Log.w(TAG, "location send $jsonObject to ${contactLocationShare.size}")
                    JamiService.sendAccountTextMessage(path.accountId, path.conversationId, StringMap().apply {
                        setUnicode(CallService.MIME_GEOLOCATION, jsonObject.toString())
                    }, 1)
                }
            }
            mContactSharingSubject.onNext(contactLocationShare.keys)
            if (contactLocationShare.isEmpty()) {
                Log.w(TAG, "stopping sharing $intent")
                mDisposableBag.clear()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                started = false
            } else {
                mDisposableBag.add(getNotification(now)
                    .subscribe { notification -> mNotificationManager.notify(NOTIF_SYNC_SERVICE_ID, notification) })
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.w(TAG, "onDestroy")
        mLocationManager?.removeUpdates(this)
        mMyLocationSubject.onComplete()
        mContactSharingSubject.onComplete()
        mDisposableBag.dispose()
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onUnbind(intent: Intent): Boolean = true

    override fun onLocationChanged(location: Location) {
        // Log.w(TAG, "onLocationChanged " + location.toString());
        mMyLocationSubject.onNext(location)
        mPreferences.edit()
            .putString(PREFERENCES_KEY_POS_LAT, location.latitude.toString())
            .putString(PREFERENCES_KEY_POS_LONG, location.longitude.toString())
            .apply()
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    private fun getNotification(now: Long): Single<Notification> {
        val contactCount = contactLocationShare.size
        val firsPath = contactLocationShare.keys.iterator().next()
        var largest: Date? = null
        for (d in contactLocationShare.values)
            if (largest == null || d.after(largest))
            largest = d
        val largestDate = largest?.time ?: now
        // Log.w(TAG, "getNotification " + firsPath.getContactId());

        return mConversationFacade.observeConversation(firsPath.accountId, firsPath.conversationUri, false)
            .firstOrError()
            .map { conversation ->
                val title: String
                val stopIntent = Intent(ACTION_STOP).setClass(applicationContext, LocationSharingService::class.java)
                val contentIntent = Intent(
                    Intent.ACTION_VIEW,
                    firsPath.toUri(),
                    applicationContext,
                    ConversationActivity::class.java
                )
                    .putExtra(ConversationFragment.EXTRA_SHOW_MAP, true)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (contactCount == 1) {
                    stopIntent.data = firsPath.toUri()
                    title = getString(R.string.notif_location_title, conversation.title)
                } else {
                    title = getString(R.string.notif_location_multi_title, contactCount)
                }
                val subtitle = getString(R.string.notif_location_remaining,
                    ceil((largestDate - now) / (1000 * 60).toDouble()).toInt())
                NotificationCompat.Builder(this, NotificationServiceImpl.NOTIF_CHANNEL_SYNC)
                    .setContentTitle(title)
                    .setContentText(subtitle)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setAutoCancel(false)
                    .setOngoing(false)
                    .setVibrate(null)
                    .setColorized(true)
                    .setColor(resources.getColor(R.color.color_primary_dark))
                    .setSmallIcon(R.drawable.ic_ring_logo_white)
                    .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                    .setOnlyAlertOnce(true)
                    .setDeleteIntent(PendingIntent.getService(
                        applicationContext,
                        mRandom.nextInt(),
                        stopIntent,
                        ContentUriHandler.immutable()))
                    .setContentIntent(PendingIntent.getActivity(
                        applicationContext,
                        mRandom.nextInt(),
                        contentIntent,
                        ContentUriHandler.immutable()))
                    .addAction(
                        R.drawable.baseline_location_disabled_24,
                        getText(R.string.notif_location_action_stop),
                        PendingIntent.getService(
                            applicationContext,
                            0,
                            stopIntent,
                            ContentUriHandler.immutable(PendingIntent.FLAG_ONE_SHOT)))
                    .build()
            }
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
    }

    private fun refreshSharing() {
        if (!started) return
        var changed = false
        val now = Date(SystemClock.elapsedRealtime())
        val it: MutableIterator<Map.Entry<ConversationPath, Date>> =
            contactLocationShare.entries.iterator()
        while (it.hasNext()) {
            val e = it.next()
            if (e.value.before(now)) {
                changed = true
                it.remove()
            }
        }
        if (changed) mContactSharingSubject.onNext(contactLocationShare.keys)
        if (contactLocationShare.isEmpty()) {
            mDisposableBag.clear()
            stopForeground(true)
            stopSelf()
            started = false
        } else if (changed) {
            mDisposableBag.add(getNotification(now.time)
                .subscribe { notification -> mNotificationManager.notify(NOTIF_SYNC_SERVICE_ID, notification) })
        }
    }

    private fun refreshNotificationTimer() {
        if (!started) return
        val now = SystemClock.uptimeMillis()
        mDisposableBag.add(getNotification(SystemClock.elapsedRealtime())
            .subscribe { notification -> mNotificationManager.notify(NOTIF_SYNC_SERVICE_ID, notification) })
        mHandler.postAtTime({ refreshNotificationTimer() }, now + 30 * 1000)
    }

    fun isSharing(path: ConversationPath?): Boolean = contactLocationShare[path] != null

    inner class LocalBinder : Binder() {
        val service: LocationSharingService
            get() = this@LocationSharingService
    }

    companion object {
        private const val TAG = "LocationSharingService"
        const val NOTIF_SYNC_SERVICE_ID = 931801
        const val ACTION_START = "startSharing"
        const val ACTION_STOP = "stopSharing"
        const val EXTRA_SHARING_DURATION = "locationShareDuration"
        const val PREFERENCES_LOCATION = "location"
        const val PREFERENCES_KEY_POS_LONG = "lastPosLongitude"
        const val PREFERENCES_KEY_POS_LAT = "lastPosLatitude"
        const val SHARE_DURATION_SEC = 60 * 5
    }
}