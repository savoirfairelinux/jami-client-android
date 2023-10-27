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
package cx.ring.fragments

import android.Manifest
import android.animation.LayoutTransition
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.icu.text.MeasureFormat
import android.icu.text.MeasureFormat.FormatWidth
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cx.ring.R
import cx.ring.databinding.FragLocationSharingBinding
import cx.ring.service.LocationSharingService
import cx.ring.service.LocationSharingService.LocalBinder
import cx.ring.utils.ConversationPath
import cx.ring.utils.DeviceUtils
import cx.ring.utils.TouchClickListener
import cx.ring.views.AvatarFactory
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.Subject
import net.jami.services.ConversationFacade
import net.jami.model.Account
import net.jami.model.Account.ContactLocation
import net.jami.model.ContactViewModel
import net.jami.services.ContactService
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class LocationSharingFragment : Fragment() {
    private val mDisposableBag = CompositeDisposable()
    private val mServiceDisposableBag = CompositeDisposable()
    private var mCountdownDisposable: Disposable? = null

    internal enum class MapState { NONE, MINI, FULL }

    @Inject
    lateinit var mConversationFacade: ConversationFacade
    @Inject
    lateinit var contactService: ContactService

    private lateinit var mPath: ConversationPath
    private var mContact: ContactViewModel? = null
    private val mShowControlsSubject: Subject<Boolean> = BehaviorSubject.create()
    private val mIsSharingSubject: Subject<Boolean> = BehaviorSubject.create()
    private val mIsContactSharingSubject: Subject<Boolean> = BehaviorSubject.create()
    private val mShowMapSubject = Observable.combineLatest(
        mShowControlsSubject,
        mIsSharingSubject,
        mIsContactSharingSubject
    ) { showControls, isSharing, isContactSharing ->
        if (showControls)
            MapState.FULL
        else if (isSharing || isContactSharing)
            MapState.MINI
        else
            MapState.NONE
    }.distinctUntilChanged()

    private var bubbleSize = 0
    private var overlay: MyLocationNewOverlay? = null
    private var marker: Marker? = null
    private var lastBoundingBox: BoundingBox? = null
    private var trackAll = true
    private var mStartSharingPending: Int? = null
    private var binding: FragLocationSharingBinding? = null
    private var mService: LocationSharingService? = null
    private var mBound = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragLocationSharingBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireArguments().let { args ->
            mPath = ConversationPath.fromBundle(args)!!
            mShowControlsSubject.onNext(args.getBoolean(KEY_SHOW_CONTROLS, true))
        }
        val ctx = requireContext()
        val osmPath = File(ctx.cacheDir, "osm")
        Configuration.getInstance().apply {
            osmdroidBasePath = osmPath
            osmdroidTileCache = File(osmPath, "tiles")
            userAgentValue = "net.jami.android"
            isMapViewHardwareAccelerated = true
            isMapViewRecyclerFriendly = false
        }
        bubbleSize = ctx.resources.getDimensionPixelSize(R.dimen.location_sharing_avatar_size)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding?.let { binding ->
            binding.locationShareTime1h.text = formatDuration(DateUtils.HOUR_IN_MILLIS, FormatWidth.WIDE)
            binding.locationShareTime10m.text = formatDuration(10 * DateUtils.MINUTE_IN_MILLIS, FormatWidth.WIDE)
            binding.infoBtn.setOnClickListener { v: View ->
                val padding = v.resources.getDimensionPixelSize(R.dimen.padding_large)
                val textView = TextView(v.context)
                textView.setText(R.string.location_share_about_message)
                textView.setOnClickListener { tv -> tv.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.location_share_about_osm_copy_url)))) }
                textView.setPadding(padding, padding, padding, padding)
                MaterialAlertDialogBuilder(view.context)
                    .setTitle(R.string.location_share_about_title)
                    .setView(textView)
                    .create().show()
            }
            binding.btnCenterPosition.setOnClickListener {
                overlay?.let { overlay ->
                    trackAll = true
                    if (lastBoundingBox != null) binding.map.zoomToBoundingBox(lastBoundingBox, true)
                    else overlay.enableFollowLocation()
                }
            }
            binding.locationShareTimeGroup.setOnCheckedChangeListener { group: ChipGroup, id: Int ->
                if (id == View.NO_ID) group.check(R.id.location_share_time_1h)
            }
            binding.locshareToolbar.setNavigationOnClickListener { mShowControlsSubject.onNext(false) }
            binding.locationShareStop.setOnClickListener { stopSharing() }
            binding.map.setTileSource(TileSourceFactory.MAPNIK)
            binding.map.isHorizontalMapRepetitionEnabled = false
            binding.map.isTilesScaledToDpi = true
            binding.map.setMapOrientation(0f, false)
            binding.map.minZoomLevel = 1.0
            binding.map.maxZoomLevel = 19.0
            binding.map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            binding.map.controller.setZoom(14.0)
        }
        (view as ViewGroup).layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
    }

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                mShowControlsSubject.onNext(false)
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        mShowControlsSubject.onComplete()
        mIsSharingSubject.onComplete()
        mIsContactSharingSubject.onComplete()
    }

    override fun onResume() {
        super.onResume()
        binding?.map?.onResume()
        try {
            overlay?.enableMyLocation()
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
    }

    override fun onPause() {
        super.onPause()
        binding?.map?.onPause()
        overlay?.disableMyLocation()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_LOCATION) {
            var granted = false
            for (result in grantResults) granted =
                granted or (result == PackageManager.PERMISSION_GRANTED)
            if (granted) {
                startService()
            } else {
                mIsSharingSubject.onNext(false)
                mShowControlsSubject.onNext(false)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mDisposableBag.add(mShowControlsSubject.subscribe { show: Boolean -> setShowControls(show) })
        mDisposableBag.add(mIsSharingSubject.subscribe { sharing: Boolean -> setIsSharing(sharing) })
        mDisposableBag.add(mShowMapSubject
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { state: MapState ->
                val p = parentFragment
                if (p is ConversationFragment) {
                    if (state == MapState.FULL)
                        p.openLocationSharing()
                    else
                        p.closeLocationSharing(state == MapState.MINI)
                }
            })
        mDisposableBag.add(mIsContactSharingSubject
            .distinctUntilChanged()
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe { sharing ->
                binding?.let { binding  ->
                    if (sharing) {
                        val sharingString =
                            getString(R.string.location_share_contact, mContact!!.displayName)
                        binding.locshareToolbar.subtitle = sharingString
                        binding.locshareSnipetTxt.visibility = View.VISIBLE
                        binding.locshareSnipetTxtShadow.visibility = View.VISIBLE
                        binding.locshareSnipetTxt.text = sharingString
                    } else {
                        binding.locshareToolbar.subtitle = null
                        binding.locshareSnipetTxt.visibility = View.GONE
                        binding.locshareSnipetTxtShadow.visibility = View.GONE
                        binding.locshareSnipetTxt.text = null
                    }
                }
            })
        val contactUri = mPath.conversationUri
        mDisposableBag.add(mConversationFacade
            .getAccountSubject(mPath.accountId)
            .flatMapObservable { account: Account ->
                val conversation = account.getByUri(contactUri)!!
                account.locationsUpdates
                    .switchMapSingle { locations -> contactService.getLoadedContact(mPath.accountId, locations.keys).map { contacts ->
                        val r: MutableList<Observable<LocationViewModel>> = ArrayList(locations.size)
                        var isContactSharing = false
                        for (c in contacts) {
                            if (c.contact === conversation.findContact(c.contact.uri)) {
                                isContactSharing = true
                                mContact = c
                            }
                            locations[c.contact]?.let { location -> r.add(location.map { l -> LocationViewModel(c, l) }) }
                        }
                        mIsContactSharingSubject.onNext(isContactSharing)
                        r
                    } }
            }
            .flatMap { locations: List<Observable<LocationViewModel>> ->
                Observable.combineLatest<LocationViewModel, List<LocationViewModel>>(locations) { locsArray: Array<Any> ->
                    val list: MutableList<LocationViewModel> = ArrayList(locsArray.size)
                    for (vm in locsArray) list.add(vm as LocationViewModel)
                    list
                }
            }
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe({ locations: List<LocationViewModel> ->
                val context = context
                if (context != null) {
                    binding!!.map.overlays.clear()
                    if (overlay != null) binding!!.map.overlays.add(overlay)
                    if (marker != null) binding!!.map.overlays.add(marker)
                    val geoLocations: MutableList<GeoPoint> = ArrayList(locations.size + 1)
                    overlay?.myLocation?.let { myLoc -> geoLocations.add(myLoc) }
                    for (vm in locations) {
                        val m = Marker(binding!!.map)
                        val position = GeoPoint(vm.location.latitude, vm.location.longitude)
                        m.setInfoWindow(null)
                        m.position = position
                        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        geoLocations.add(position)
                        mDisposableBag.add(
                            AvatarFactory.getBitmapAvatar(context, vm.contact, bubbleSize, false)
                                .subscribe { avatar: Bitmap ->
                                    val bd = BitmapDrawable(context.resources, avatar)
                                    m.icon = bd
                                    m.setInfoWindow(null)
                                    binding!!.map.overlays.add(m)
                                })
                    }
                    if (trackAll) {
                        if (geoLocations.size == 1) {
                            lastBoundingBox = null
                            binding!!.map.controller.animateTo(geoLocations[0])
                        } else {
                            var bb = BoundingBox.fromGeoPointsSafe(geoLocations)
                            bb = bb.increaseByScale(1.5f)
                            lastBoundingBox = bb
                            binding!!.map.zoomToBoundingBox(bb, true)
                        }
                    }
                }
            }) { e: Throwable -> Log.w(TAG, "Error updating contact position", e) }
        )
        val ctx = requireContext()
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            mIsSharingSubject.onNext(false)
            mDisposableBag.add(mShowControlsSubject
                .firstElement()
                .subscribe { showControls ->
                    if (showControls) {
                        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_LOCATION)
                    }
                })
        } else {
            startService()
        }
    }

    override fun onStop() {
        super.onStop()
        if (mBound) {
            requireContext().unbindService(mConnection)
            mConnection.onServiceDisconnected(null)
            mBound = false
        }
        mDisposableBag.clear()
    }

    private fun startService() {
        val ctx = requireContext()
        ctx.bindService(Intent(ctx, LocationSharingService::class.java), mConnection, Context.BIND_AUTO_CREATE)
    }

    fun showControls() {
        mShowControlsSubject.onNext(true)
    }

    fun hideControls() {
        mShowControlsSubject.onNext(false)
    }

    private fun setShowControls(show: Boolean) {
        binding?.let { b ->
            if (show) {
                onBackPressedCallback.isEnabled = true
                b.locshareSnipet.visibility = View.GONE
                b.shareControlsMini.visibility = View.GONE
                b.shareControlsMini.postDelayed({
                    binding?.let { b ->
                        b.shareControlsMini.visibility = View.GONE
                        b.locshareSnipet.visibility = View.GONE
                    }
                }, 300)
                b.shareControls.visibility = View.VISIBLE
                b.locshareToolbar.visibility = View.VISIBLE
                b.map.setOnTouchListener(null)
                b.map.setMultiTouchControls(true)
            } else {
                onBackPressedCallback.isEnabled = false
                b.shareControls.visibility = View.GONE
                b.shareControlsMini.postDelayed({
                        binding?.let { b ->
                        b.shareControlsMini.visibility = View.VISIBLE
                        b.locshareSnipet.visibility = View.VISIBLE
                    }
                }, 300)
                b.locshareToolbar.visibility = View.GONE
                b.map.setMultiTouchControls(false)
                b.map.setOnTouchListener(TouchClickListener(binding!!.map.context) { mShowControlsSubject.onNext(true) })
            }
        }
    }

    internal class RxLocationListener(private val mLocation: Observable<Location>) : IMyLocationProvider {
        private val mDisposableBag = CompositeDisposable()

        override fun startLocationProvider(myLocationConsumer: IMyLocationConsumer): Boolean {
            mDisposableBag.add(mLocation.subscribe { loc -> myLocationConsumer.onLocationChanged(loc, this) })
            return false
        }

        override fun stopLocationProvider() {
            mDisposableBag.clear()
        }

        override fun getLastKnownLocation(): Location {
            return mLocation.blockingFirst()
        }

        override fun destroy() {
            mDisposableBag.dispose()
        }
    }

    internal class LocationViewModel(var contact: ContactViewModel, var location: ContactLocation)

    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.w(TAG, "onServiceConnected")
            val binder = service as LocalBinder
            mService = binder.service
            mBound = true
            if (marker == null) {
                marker = Marker(binding!!.map).apply {
                    setInfoWindow(null)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                }
                mServiceDisposableBag.add(mConversationFacade
                    .getAccountSubject(mPath.accountId)
                    .flatMap { account -> AvatarFactory.getBitmapAvatar(requireContext(), account, bubbleSize) }
                    .subscribe { avatar ->
                        marker!!.icon = BitmapDrawable(requireContext().resources, avatar)
                        binding!!.map.overlays.add(marker)
                    })
            }
            mServiceDisposableBag.add(binder.service.contactSharing
                    .subscribe { location -> mIsSharingSubject.onNext(location.contains(mPath)) })
            mServiceDisposableBag.add(binder.service.myLocation
                    .subscribe { location -> marker!!.position = GeoPoint(location) })
            mServiceDisposableBag.add(binder.service.myLocation
                    .firstElement()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { location ->
                        // start map on first location
                        binding?.let { binding ->
                            binding.map.setExpectedCenter(GeoPoint(location))
                            overlay = MyLocationNewOverlay(RxLocationListener(binder.service.myLocation), binding.map)
                                .apply { enableMyLocation() }
                            binding.map.overlays.add(overlay)
                        }
                    })
            mStartSharingPending?.let { pending ->
                mStartSharingPending = null
                startSharing(pending)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "onServiceDisconnected")
            mBound = false
            mServiceDisposableBag.clear()
            mService = null
        }
    }
    private val selectedDuration: Int
        get() = when (binding!!.locationShareTimeGroup.checkedChipId) {
            R.id.location_share_time_10m -> 10 * 60
            R.id.location_share_time_1h -> 60 * 60
            else -> 60 * 60
        }

    private fun setIsSharing(sharing: Boolean) {
        binding?.let { binding ->
            if (sharing) {
                binding.btnShareLocation.setBackgroundColor(ContextCompat.getColor(binding.btnShareLocation.context, com.google.android.material.R.color.design_default_color_error))
                binding.btnShareLocation.setText(R.string.location_share_action_stop)
                binding.btnShareLocation.setOnClickListener { v: View? -> stopSharing() }
                binding.locationShareTimeGroup.visibility = View.GONE
                mService?.let { service ->
                    binding.locationShareTimeRemaining.visibility = View.VISIBLE
                    if (mCountdownDisposable == null || mCountdownDisposable!!.isDisposed) {
                        mServiceDisposableBag.add(service.getContactSharingExpiration(mPath)
                            .subscribe { l -> binding.locationShareTimeRemaining.text = formatDuration(l, FormatWidth.SHORT) }
                            .apply { mCountdownDisposable = this })
                    }
                }
                binding.locationShareStop.visibility = View.VISIBLE
                requireView().post { hideControls() }
            } else {
                mCountdownDisposable?.let { disposable ->
                    disposable.dispose()
                    mCountdownDisposable = null
                }
                binding.btnShareLocation.setBackgroundColor(ContextCompat.getColor(binding.btnShareLocation.context, R.color.colorSecondary))
                binding.btnShareLocation.setText(R.string.location_share_action_start)
                binding.btnShareLocation.setOnClickListener { startSharing(selectedDuration) }
                binding.locationShareTimeRemaining.visibility = View.GONE
                binding.locationShareTimeGroup.visibility = View.VISIBLE
                binding.locationShareStop.visibility = View.GONE
            }
        }
    }

    private fun startSharing(durationSec: Int) {
        val ctx = requireContext()
        try {
            if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                mStartSharingPending = durationSec
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_LOCATION)
            } else {
                val intent = Intent(LocationSharingService.ACTION_START, mPath.toUri(), ctx, LocationSharingService::class.java)
                    .putExtra(LocationSharingService.EXTRA_SHARING_DURATION, durationSec)
                ContextCompat.startForegroundService(ctx, intent)
            }
        } catch (e: Exception) {
            Toast.makeText(ctx, "Error starting location sharing: " + e.localizedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopSharing() {
        try {
            val ctx = requireContext()
            ctx.startService(Intent(LocationSharingService.ACTION_STOP, mPath.toUri(), ctx, LocationSharingService::class.java))
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping location sharing", e)
        }
    }

    companion object {
        private val TAG = LocationSharingFragment::class.simpleName!!
        private const val REQUEST_CODE_LOCATION = 47892
        private const val KEY_SHOW_CONTROLS = "showControls"

        fun newInstance(accountId: String, conversationId: String, showControls: Boolean): LocationSharingFragment {
            val fragment = LocationSharingFragment()
            val args = ConversationPath.toBundle(accountId, conversationId)
            args.putBoolean(KEY_SHOW_CONTROLS, showControls)
            fragment.arguments = args
            return fragment
        }

        private fun formatDuration(millis: Long, width: FormatWidth): CharSequence {
            val formatter = MeasureFormat.getInstance(Locale.getDefault(), width)
            return when {
                millis >= DateUtils.HOUR_IN_MILLIS -> {
                    val hours = ((millis + DateUtils.HOUR_IN_MILLIS / 2) / DateUtils.HOUR_IN_MILLIS).toInt()
                    formatter.format(Measure(hours, MeasureUnit.HOUR))
                }
                millis >= DateUtils.MINUTE_IN_MILLIS -> {
                    val minutes = ((millis + DateUtils.MINUTE_IN_MILLIS / 2) / DateUtils.MINUTE_IN_MILLIS).toInt()
                    formatter.format(Measure(minutes, MeasureUnit.MINUTE))
                }
                else -> {
                    val seconds = ((millis + DateUtils.SECOND_IN_MILLIS / 2) / DateUtils.SECOND_IN_MILLIS).toInt()
                    formatter.format(Measure(seconds, MeasureUnit.SECOND))
                }
            }
        }
    }
}