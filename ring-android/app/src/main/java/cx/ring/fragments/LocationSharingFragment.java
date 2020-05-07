/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 *  Authors: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
package cx.ring.fragments;

import android.Manifest;
import android.animation.LayoutTransition;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.icu.text.MeasureFormat;
import android.icu.util.Measure;
import android.icu.util.MeasureUnit;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import android.os.IBinder;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.contacts.AvatarFactory;
import cx.ring.facades.ConversationFacade;
import cx.ring.model.Account;
import cx.ring.model.CallContact;
import cx.ring.model.Uri;
import cx.ring.services.LocationSharingService;
import cx.ring.utils.ConversationPath;
import cx.ring.utils.TouchClickListener;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class LocationSharingFragment extends Fragment {
    private static final String TAG = LocationSharingFragment.class.getSimpleName();
    private static final int REQUEST_CODE_LOCATION = 47892;
    private static final String KEY_SHOW_CONTROLS = "showControls";

    private final CompositeDisposable mDisposableBag = new CompositeDisposable();
    private final CompositeDisposable mServiceDisposableBag = new CompositeDisposable();

    enum MapState {NONE, MINI, FULL}

    @Inject
    ConversationFacade mConversationFacade;

    private ConversationPath mPath;
    private CallContact mContact;

    private final Subject<Boolean> mShowControlsSubject = BehaviorSubject.create();
    private final Subject<Boolean> mIsSharingSubject = BehaviorSubject.create();
    private final Subject<Boolean> mIsContactSharingSubject = BehaviorSubject.create();
    private final Observable<MapState> mShowMapSubject = Observable.combineLatest(
            mShowControlsSubject,
            mIsSharingSubject,
            mIsContactSharingSubject,
            (showControls, isSharing, isContactSharing) -> showControls
                    ? MapState.FULL
                    : ((isSharing || isContactSharing) ? MapState.MINI : MapState.NONE))
            .distinctUntilChanged();

    private int bubbleSize;

    private Toolbar mToolbar;
    private ViewGroup mSnippetGroup;
    private TextView mSnippet;
    private ImageView mSnippetShadow;
    private ViewGroup mShareControls;
    private ViewGroup mShareControlsMini;
    private ExtendedFloatingActionButton mShareButton;
    private Chip mStopShareButton;
    private ChipGroup mShareTimeGroup;
    private Chip mTimeRemaining;
    private MapView mMap = null;
    private MyLocationNewOverlay overlay;
    private Marker marker;
    private BoundingBox lastBoundingBox = null;
    private boolean trackAll = true;
    private Integer mStartSharingPending = null;

    private LocationSharingService mService = null;
    private boolean mBound = false;

    public LocationSharingFragment() {
        super(R.layout.frag_location_sharing);
    }

    public static LocationSharingFragment newInstance(String accountId, String conversationId, boolean showControls) {
        LocationSharingFragment fragment = new LocationSharingFragment();
        Bundle args = ConversationPath.toBundle(accountId, conversationId);
        args.putBoolean(KEY_SHOW_CONTROLS, showControls);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((JamiApplication) requireActivity().getApplication()).getInjectionComponent().inject(this);
        setRetainInstance(true);

        Bundle args = getArguments();
        if (args != null) {
            mPath = ConversationPath.fromBundle(args);
            mShowControlsSubject.onNext(args.getBoolean(KEY_SHOW_CONTROLS, true));
        }

        Context ctx = requireContext();
        IConfigurationProvider configuration = Configuration.getInstance();
        configuration.load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        configuration.setOsmdroidBasePath(new File(ctx.getCacheDir(), "osm"));
        configuration.setMapViewHardwareAccelerated(true);

        bubbleSize = ctx.getResources().getDimensionPixelSize(R.dimen.location_sharing_avatar_size);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static CharSequence formatDuration(long millis, MeasureFormat.FormatWidth width) {
        final MeasureFormat formatter = MeasureFormat.getInstance(Locale.getDefault(), width);
        if (millis >= DateUtils.HOUR_IN_MILLIS) {
            final int hours = (int) ((millis + DateUtils.HOUR_IN_MILLIS/2) / DateUtils.HOUR_IN_MILLIS);
            return formatter.format(new Measure(hours, MeasureUnit.HOUR));
        } else if (millis >= DateUtils.MINUTE_IN_MILLIS) {
            final int minutes = (int) ((millis + DateUtils.MINUTE_IN_MILLIS/2) / DateUtils.MINUTE_IN_MILLIS);
            return formatter.format(new Measure(minutes, MeasureUnit.MINUTE));
        } else {
            final int seconds = (int) ((millis + DateUtils.SECOND_IN_MILLIS/2) / DateUtils.SECOND_IN_MILLIS);
            return formatter.format(new Measure(seconds, MeasureUnit.SECOND));
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mToolbar = view.findViewById(R.id.locshare_toolbar);
        mSnippetGroup = view.findViewById(R.id.locshare_snipet);
        mSnippet = view.findViewById(R.id.locshare_snipet_txt);
        mSnippetShadow = view.findViewById(R.id.locshare_snipet_txt_shadow);
        mMap = view.findViewById(R.id.map);
        mShareControls = view.findViewById(R.id.shareControls);
        mShareControlsMini = view.findViewById(R.id.shareControlsMini);
        mShareButton = view.findViewById(R.id.btn_share_location);
        mStopShareButton = view.findViewById(R.id.location_share_stop);
        mTimeRemaining = view.findViewById(R.id.location_share_time_remaining);
        mShareTimeGroup = view.findViewById(R.id.location_share_time_group);
        Chip chip_1h = view.findViewById(R.id.location_share_time_1h);
        Chip chip_10m = view.findViewById(R.id.location_share_time_10m);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            chip_1h.setText(formatDuration( DateUtils.HOUR_IN_MILLIS, MeasureFormat.FormatWidth.WIDE));
            chip_10m.setText(formatDuration( 10 * DateUtils.MINUTE_IN_MILLIS, MeasureFormat.FormatWidth.WIDE));
        }

        View locateView = view.findViewById(R.id.btn_center_position);
        locateView.setOnClickListener(v -> {
            if (overlay != null) {
                trackAll = true;
                if (lastBoundingBox != null)
                    mMap.zoomToBoundingBox(lastBoundingBox, true);
                else
                    overlay.enableFollowLocation();
            }
        });
        mShareTimeGroup.setOnCheckedChangeListener((group, id) -> {
            if (id == View.NO_ID)
                group.check(R.id.location_share_time_1h);
        });
        mToolbar.setNavigationOnClickListener(v -> mShowControlsSubject.onNext(false));
        mStopShareButton.setOnClickListener(v -> stopSharing());

        mMap.setTileSource(TileSourceFactory.MAPNIK);
        mMap.setHorizontalMapRepetitionEnabled(false);
        mMap.setTilesScaledToDpi(true);
        mMap.setMapOrientation(0, false);
        mMap.setMinZoomLevel(1d);
        mMap.setMaxZoomLevel(19.d);
        mMap.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mMap.getController().setZoom(14.0);
        ((ViewGroup)view).getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
    }

    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            mShowControlsSubject.onNext(false);
        }
    };

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        requireActivity().getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mShowControlsSubject.onComplete();
        mIsSharingSubject.onComplete();
        mIsContactSharingSubject.onComplete();
    }

    public void onResume() {
        super.onResume();
        mMap.onResume();
        if (overlay != null) {
            try {
                overlay.enableMyLocation();
            } catch (Exception e) {
                Log.w(TAG, e);
            }
        }
    }

    public void onPause(){
        super.onPause();
        mMap.onPause();
        if (overlay != null)
            overlay.disableMyLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_LOCATION) {
            boolean granted = false;
            for (int result : grantResults)
                granted |= (result == PackageManager.PERMISSION_GRANTED);
            if (granted) {
                startService();
            } else {
                mIsSharingSubject.onNext(false);
                mShowControlsSubject.onNext(false);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mDisposableBag.add(mServiceDisposableBag);
        mDisposableBag.add(mShowControlsSubject.subscribe(this::setShowControls));
        mDisposableBag.add(mIsSharingSubject.subscribe(this::setIsSharing));
        mDisposableBag.add(mShowMapSubject.subscribe(state -> {
            Fragment p = getParentFragment();
            if (p instanceof ConversationFragment) {
                ConversationFragment parent = (ConversationFragment) p;
                if (state == MapState.FULL)
                    parent.openLocationSharing();
                else
                    parent.closeLocationSharing(state == MapState.MINI);
            }
        }));
        mDisposableBag.add(mIsContactSharingSubject
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sharing -> {
                    if (sharing) {
                        String sharingString = getString(R.string.location_share_contact, mContact.getDisplayName());
                        mToolbar.setSubtitle(sharingString);
                        mSnippet.setVisibility(View.VISIBLE);
                        mSnippetShadow.setVisibility(View.VISIBLE);
                        mSnippet.setText(sharingString);
                    } else {
                        mToolbar.setSubtitle(null);
                        mSnippet.setVisibility(View.GONE);
                        mSnippetShadow.setVisibility(View.GONE);
                        mSnippet.setText(null);
                    }
                }));

        final Uri contactUri = new Uri(mPath.getContactId());

        mDisposableBag.add(mConversationFacade
                .getAccountSubject(mPath.getAccountId())
                .flatMapObservable(account -> account.getLocationsUpdates()
                        .map(locations -> {
                            List<Observable<LocationViewModel>> r = new ArrayList<>(locations.size());
                            boolean isContactSharing = false;
                            for (Map.Entry<CallContact, Observable<Account.ContactLocation>> l : locations.entrySet()) {
                                if (l.getKey() == account.getContactFromCache(contactUri)) {
                                    isContactSharing = true;
                                    mContact = l.getKey();
                                }
                                r.add(l.getValue().map(cl -> new LocationViewModel(l.getKey(), cl)));
                            }
                            mIsContactSharingSubject.onNext(isContactSharing);
                            return r;
                        }))
                .flatMap(locations -> Observable.combineLatest(locations, locsArray -> {
                    List<LocationViewModel> list = new ArrayList<>(locsArray.length);
                    for (Object vm : locsArray)
                        list.add((LocationViewModel)vm);
                    return list;
                }))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(locations -> {
                    Context context = getContext();
                    if (context != null) {
                        mMap.getOverlays().clear();
                        if (overlay != null)
                            mMap.getOverlays().add(overlay);
                        if (marker != null)
                            mMap.getOverlays().add(marker);

                        List<GeoPoint> geoLocations =new ArrayList<>(locations.size() + 1);
                        GeoPoint myLoc = overlay == null ? null : overlay.getMyLocation();
                        if (myLoc != null) {
                            geoLocations.add(myLoc);
                        }

                        for (LocationViewModel vm : locations) {
                            Marker m = new Marker(mMap);
                            GeoPoint position = new GeoPoint(vm.location.latitude, vm.location.longitude);
                            m.setInfoWindow(null);
                            m.setPosition(position);
                            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                            geoLocations.add(position);
                            mDisposableBag.add(AvatarFactory.getBitmapAvatar(context, vm.contact, bubbleSize, false).subscribe(avatar ->  {
                                BitmapDrawable bd = new BitmapDrawable(context.getResources(), avatar);
                                m.setIcon(bd);
                                m.setInfoWindow(null);
                                mMap.getOverlays().add(m);
                            }));
                        }

                        if (trackAll) {
                            if (geoLocations.size() == 1) {
                                lastBoundingBox = null;
                                mMap.getController().animateTo(geoLocations.get(0));
                            } else {
                                BoundingBox bb = BoundingBox.fromGeoPointsSafe(geoLocations);
                                bb = bb.increaseByScale(1.5f);
                                lastBoundingBox = bb;
                                mMap.zoomToBoundingBox(bb, true);
                            }
                        }
                    }
                }, e -> Log.w(TAG, "Error updating contact position", e))
        );

        Context ctx = requireContext();
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            mIsSharingSubject.onNext(false);
            mDisposableBag.add(mShowControlsSubject
                    .firstElement()
                    .subscribe(showControls -> {
                        if (showControls) {
                            requestPermissions(new String[]{ Manifest.permission.ACCESS_FINE_LOCATION }, REQUEST_CODE_LOCATION);
                        }
                    }));
        } else {
            startService();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mBound) {
            mServiceDisposableBag.clear();
            requireContext().unbindService(mConnection);
            mBound = false;
        }
        mDisposableBag.clear();
    }

    private void startService() {
        Context ctx = requireContext();
        ctx.bindService(new Intent(ctx, LocationSharingService.class), mConnection, Context.BIND_AUTO_CREATE);
    }


    void showControls() {
        mShowControlsSubject.onNext(true);
    }

    void hideControls() {
        mShowControlsSubject.onNext(false);
    }

    private void setShowControls(boolean show) {
        if (show) {
            onBackPressedCallback.setEnabled(true);
            mSnippetGroup.setVisibility(View.GONE);
            mShareControlsMini.setVisibility(View.GONE);
            mShareControlsMini.postDelayed(() -> {
                mShareControlsMini.setVisibility(View.GONE);
                mSnippetGroup.setVisibility(View.GONE);
            }, 300);
            mShareControls.setVisibility(View.VISIBLE);
            mToolbar.setVisibility(View.VISIBLE);
            mMap.setOnTouchListener(null);
            mMap.setMultiTouchControls(true);
        } else {
            onBackPressedCallback.setEnabled(false);
            mShareControls.setVisibility(View.GONE);
            mShareControlsMini.postDelayed(() -> {
                mShareControlsMini.setVisibility(View.VISIBLE);
                mSnippetGroup.setVisibility(View.VISIBLE);
            }, 300);
            mToolbar.setVisibility(View.GONE);
            mMap.setMultiTouchControls(false);
            mMap.setOnTouchListener(new TouchClickListener(mMap.getContext(), v -> mShowControlsSubject.onNext(true)));
        }
    }

    static class RxLocationListener implements IMyLocationProvider {
        private final CompositeDisposable mDisposableBag = new CompositeDisposable();
        private Observable<Location> mLocation;

        RxLocationListener(Observable<Location> location) {
            mLocation = location;
        }

        @Override
        public boolean startLocationProvider(IMyLocationConsumer myLocationConsumer) {
            mDisposableBag.add(mLocation.subscribe(loc -> myLocationConsumer.onLocationChanged(loc, this)));
            return false;
        }

        @Override
        public void stopLocationProvider() {
            mDisposableBag.clear();
        }

        @Override
        public Location getLastKnownLocation() {
            return mLocation.blockingFirst();
        }

        @Override
        public void destroy() {
            mDisposableBag.dispose();
            mLocation = null;
        }
    }

    static class LocationViewModel {
        CallContact contact;
        Account.ContactLocation location;
        LocationViewModel(CallContact c, Account.ContactLocation cl) {
            contact = c;
            location = cl;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.w(TAG, "onServiceConnected");
            LocationSharingService.LocalBinder binder = (LocationSharingService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            if (marker == null) {
                marker = new Marker(mMap);
                marker.setInfoWindow(null);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                mServiceDisposableBag.add(mConversationFacade
                        .getAccountSubject(mPath.getAccountId())
                        .flatMap(account -> AvatarFactory.getBitmapAvatar(requireContext(), account, bubbleSize))
                        .subscribe(avatar -> {
                            marker.setIcon(new BitmapDrawable(requireContext().getResources(), avatar));
                            mMap.getOverlays().add(marker);
                        }));
            }

            mServiceDisposableBag.add(mService.getContactSharing()
                    .subscribe(location -> mIsSharingSubject.onNext(location.contains(mPath))));
            mServiceDisposableBag.add(mService.getMyLocation()
                    .subscribe(location -> marker.setPosition(new GeoPoint(location))));
            mServiceDisposableBag.add(mService.getMyLocation()
                    .firstElement()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(location  -> {
                        // start map on first location
                        mMap.setExpectedCenter(new GeoPoint(location));
                        overlay = new MyLocationNewOverlay(new RxLocationListener(mService.getMyLocation()), mMap);
                        overlay.enableMyLocation();
                        mMap.getOverlays().add(overlay);
                    }));

            if (mStartSharingPending != null) {
                Integer pending = mStartSharingPending;
                mStartSharingPending = null;
                startSharing(pending);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
            mService = null;
            mServiceDisposableBag.clear();
        }
    };

    private int getSelectedDuration() {
        switch (mShareTimeGroup.getCheckedChipId()) {
            case R.id.location_share_time_10m:
                return 10 * 60;
            case R.id.location_share_time_1h:
            default:
                return 60 * 60;
        }
    }

    private void setIsSharing(boolean sharing) {
        if (sharing) {
            mShareButton.setBackgroundColor(ContextCompat.getColor(mShareButton.getContext(), R.color.design_default_color_error));
            mShareButton.setText(R.string.location_share_action_stop);
            mShareButton.setOnClickListener(v -> stopSharing());
            mShareTimeGroup.setVisibility(View.GONE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mTimeRemaining.setVisibility(View.VISIBLE);
                mDisposableBag.add(mService.getContactSharingExpiration(mPath)
                        .subscribe(l -> mTimeRemaining.setText(formatDuration(l, MeasureFormat.FormatWidth.SHORT))));
            }
            mStopShareButton.setVisibility(View.VISIBLE);
            requireView().post(this::hideControls);
        } else {
            mShareButton.setBackgroundColor(ContextCompat.getColor(mShareButton.getContext(), R.color.colorSecondary));
            mShareButton.setText(R.string.location_share_action_start);
            mShareButton.setOnClickListener(v -> startSharing(getSelectedDuration()));
            mTimeRemaining.setVisibility(View.GONE);
            mShareTimeGroup.setVisibility(View.VISIBLE);
            mStopShareButton.setVisibility(View.GONE);
        }
    }

    private void startSharing(int durationSec) {
        Context ctx = requireContext();
        try {
            if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                mStartSharingPending = durationSec;
                requestPermissions(new String[]{ Manifest.permission.ACCESS_FINE_LOCATION }, REQUEST_CODE_LOCATION);
            } else {
                Intent intent = new Intent(LocationSharingService.ACTION_START, mPath.toUri(), ctx, LocationSharingService.class)
                        .putExtra(LocationSharingService.EXTRA_SHARING_DURATION, durationSec);
                ContextCompat.startForegroundService(ctx, intent);
            }
        } catch (Exception e) {
            Toast.makeText(ctx, "Error starting location sharing: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopSharing() {
        Context ctx = requireContext();
        try {
            Intent intent = new Intent(LocationSharingService.ACTION_STOP, mPath.toUri(), ctx, LocationSharingService.class);
            ctx.startService(intent);
        } catch (Exception e) {
            Log.w(TAG, "Error stopping location sharing", e);
        }
    }
}
