/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import android.os.IBinder;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import net.jami.facades.ConversationFacade;
import net.jami.model.Account;
import net.jami.model.Contact;
import net.jami.model.Uri;

import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
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
import cx.ring.views.AvatarFactory;
import cx.ring.databinding.FragLocationSharingBinding;
import cx.ring.service.LocationSharingService;
import cx.ring.utils.ConversationPath;
import cx.ring.utils.TouchClickListener;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.Subject;

@AndroidEntryPoint
public class LocationSharingFragment extends Fragment {
    private static final String TAG = LocationSharingFragment.class.getSimpleName();
    private static final int REQUEST_CODE_LOCATION = 47892;
    private static final String KEY_SHOW_CONTROLS = "showControls";

    private final CompositeDisposable mDisposableBag = new CompositeDisposable();
    private final CompositeDisposable mServiceDisposableBag = new CompositeDisposable();
    private Disposable mCountdownDisposable = null;

    enum MapState {NONE, MINI, FULL}

    @Inject
    ConversationFacade mConversationFacade;

    private ConversationPath mPath;
    private Contact mContact;

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

    private MyLocationNewOverlay overlay;
    private Marker marker;
    private BoundingBox lastBoundingBox = null;
    private boolean trackAll = true;
    private Integer mStartSharingPending = null;

    private FragLocationSharingBinding binding = null;
    private LocationSharingService mService = null;
    private boolean mBound = false;

    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragLocationSharingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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
        //setRetainInstance(true);

        Bundle args = getArguments();
        if (args != null) {
            mPath = ConversationPath.fromBundle(args);
            mShowControlsSubject.onNext(args.getBoolean(KEY_SHOW_CONTROLS, true));
        }

        Context ctx = requireContext();
        File osmPath = new File(ctx.getCacheDir(), "osm");
        IConfigurationProvider configuration = Configuration.getInstance();
        configuration.setOsmdroidBasePath(osmPath);
        configuration.setOsmdroidTileCache(new File(osmPath, "tiles"));
        configuration.setUserAgentValue("net.jami.android");
        configuration.setMapViewHardwareAccelerated(true);
        configuration.setMapViewRecyclerFriendly(false);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding.locationShareTime1h.setText(formatDuration( DateUtils.HOUR_IN_MILLIS, MeasureFormat.FormatWidth.WIDE));
            binding.locationShareTime10m.setText(formatDuration( 10 * DateUtils.MINUTE_IN_MILLIS, MeasureFormat.FormatWidth.WIDE));
        }
        binding.infoBtn.setOnClickListener(v -> {
            int padding = v.getResources().getDimensionPixelSize(R.dimen.padding_large);
            TextView textView = new TextView(v.getContext());
            textView.setText(R.string.location_share_about_message);
            textView.setOnClickListener(tv -> tv.getContext().startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(getString(R.string.location_share_about_osm_copy_url)))));
            textView.setPadding(padding, padding, padding, padding);
            new MaterialAlertDialogBuilder(view.getContext())
                    .setTitle(R.string.location_share_about_title)
                    .setView(textView)
                    .create().show();
        });

        View locateView = view.findViewById(R.id.btn_center_position);
        locateView.setOnClickListener(v -> {
            if (overlay != null) {
                trackAll = true;
                if (lastBoundingBox != null)
                    binding.map.zoomToBoundingBox(lastBoundingBox, true);
                else
                    overlay.enableFollowLocation();
            }
        });
        binding.locationShareTimeGroup.setOnCheckedChangeListener((group, id) -> {
            if (id == View.NO_ID)
                group.check(R.id.location_share_time_1h);
        });
        binding.locshareToolbar.setNavigationOnClickListener(v -> mShowControlsSubject.onNext(false));
        binding.locationShareStop.setOnClickListener(v -> stopSharing());

        binding.map.setTileSource(TileSourceFactory.MAPNIK);
        binding.map.setHorizontalMapRepetitionEnabled(false);
        binding.map.setTilesScaledToDpi(true);
        binding.map.setMapOrientation(0, false);
        binding.map.setMinZoomLevel(1d);
        binding.map.setMaxZoomLevel(19.d);
        binding.map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        binding.map.getController().setZoom(14.0);
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
        binding.map.onResume();
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
        binding.map.onPause();
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
        mDisposableBag.add(mShowMapSubject
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> {
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
                        binding.locshareToolbar.setSubtitle(sharingString);
                        binding.locshareSnipetTxt.setVisibility(View.VISIBLE);
                        binding.locshareSnipetTxtShadow.setVisibility(View.VISIBLE);
                        binding.locshareSnipetTxt.setText(sharingString);
                    } else {
                        binding.locshareToolbar.setSubtitle(null);
                        binding.locshareSnipetTxt.setVisibility(View.GONE);
                        binding.locshareSnipetTxtShadow.setVisibility(View.GONE);
                        binding.locshareSnipetTxt.setText(null);
                    }
                }));

        final Uri contactUri = mPath.getConversationUri();

        mDisposableBag.add(mConversationFacade
                .getAccountSubject(mPath.getAccountId())
                .flatMapObservable(account -> account.getLocationsUpdates()
                        .map(locations -> {
                            List<Observable<LocationViewModel>> r = new ArrayList<>(locations.size());
                            boolean isContactSharing = false;
                            for (Map.Entry<Contact, Observable<Account.ContactLocation>> l : locations.entrySet()) {
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
                        binding.map.getOverlays().clear();
                        if (overlay != null)
                            binding.map.getOverlays().add(overlay);
                        if (marker != null)
                            binding.map.getOverlays().add(marker);

                        List<GeoPoint> geoLocations = new ArrayList<>(locations.size() + 1);
                        GeoPoint myLoc = overlay == null ? null : overlay.getMyLocation();
                        if (myLoc != null) {
                            geoLocations.add(myLoc);
                        }

                        for (LocationViewModel vm : locations) {
                            Marker m = new Marker(binding.map);
                            GeoPoint position = new GeoPoint(vm.location.latitude, vm.location.longitude);
                            m.setInfoWindow(null);
                            m.setPosition(position);
                            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                            geoLocations.add(position);
                            mDisposableBag.add(AvatarFactory.getBitmapAvatar(context, vm.contact, bubbleSize, false).subscribe(avatar ->  {
                                BitmapDrawable bd = new BitmapDrawable(context.getResources(), avatar);
                                m.setIcon(bd);
                                m.setInfoWindow(null);
                                binding.map.getOverlays().add(m);
                            }));
                        }

                        if (trackAll) {
                            if (geoLocations.size() == 1) {
                                lastBoundingBox = null;
                                binding.map.getController().animateTo(geoLocations.get(0));
                            } else {
                                BoundingBox bb = BoundingBox.fromGeoPointsSafe(geoLocations);
                                bb = bb.increaseByScale(1.5f);
                                lastBoundingBox = bb;
                                binding.map.zoomToBoundingBox(bb, true);
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
            requireContext().unbindService(mConnection);
            mConnection.onServiceDisconnected(null);
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
            binding.locshareSnipet.setVisibility(View.GONE);
            binding.shareControlsMini.setVisibility(View.GONE);
            binding.shareControlsMini.postDelayed(() -> {
                if (binding != null) {
                    binding.shareControlsMini.setVisibility(View.GONE);
                    binding.locshareSnipet.setVisibility(View.GONE);
                }
            }, 300);
            binding.shareControls.setVisibility(View.VISIBLE);
            binding.locshareToolbar.setVisibility(View.VISIBLE);
            binding.map.setOnTouchListener(null);
            binding.map.setMultiTouchControls(true);
        } else {
            onBackPressedCallback.setEnabled(false);
            binding.shareControls.setVisibility(View.GONE);
            binding.shareControlsMini.postDelayed(() -> {
                if (binding != null) {
                    binding.shareControlsMini.setVisibility(View.VISIBLE);
                    binding.locshareSnipet.setVisibility(View.VISIBLE);
                }
            }, 300);
            binding.locshareToolbar.setVisibility(View.GONE);
            binding.map.setMultiTouchControls(false);
            binding.map.setOnTouchListener(new TouchClickListener(binding.map.getContext(), v -> mShowControlsSubject.onNext(true)));
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
        Contact contact;
        Account.ContactLocation location;
        LocationViewModel(Contact c, Account.ContactLocation cl) {
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
                marker = new Marker(binding.map);
                marker.setInfoWindow(null);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                mServiceDisposableBag.add(mConversationFacade
                        .getAccountSubject(mPath.getAccountId())
                        .flatMap(account -> AvatarFactory.getBitmapAvatar(requireContext(), account, bubbleSize))
                        .subscribe(avatar -> {
                            marker.setIcon(new BitmapDrawable(requireContext().getResources(), avatar));
                            binding.map.getOverlays().add(marker);
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
                        binding.map.setExpectedCenter(new GeoPoint(location));
                        overlay = new MyLocationNewOverlay(new RxLocationListener(mService.getMyLocation()), binding.map);
                        overlay.enableMyLocation();
                        binding.map.getOverlays().add(overlay);
                    }));

            if (mStartSharingPending != null) {
                Integer pending = mStartSharingPending;
                mStartSharingPending = null;
                startSharing(pending);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w(TAG, "onServiceDisconnected");
            mBound = false;
            mServiceDisposableBag.clear();
            mService = null;
        }
    };

    private int getSelectedDuration() {
        switch (binding.locationShareTimeGroup.getCheckedChipId()) {
            case R.id.location_share_time_10m:
                return 10 * 60;
            case R.id.location_share_time_1h:
            default:
                return 60 * 60;
        }
    }

    private void setIsSharing(boolean sharing) {
        if (sharing) {
            binding.btnShareLocation.setBackgroundColor(ContextCompat.getColor(binding.btnShareLocation.getContext(), R.color.design_default_color_error));
            binding.btnShareLocation.setText(R.string.location_share_action_stop);
            binding.btnShareLocation.setOnClickListener(v -> stopSharing());
            binding.locationShareTimeGroup.setVisibility(View.GONE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (mService != null) {
                    binding.locationShareTimeRemaining.setVisibility(View.VISIBLE);
                    if (mCountdownDisposable == null || mCountdownDisposable.isDisposed())  {
                        mCountdownDisposable = mService.getContactSharingExpiration(mPath)
                                .subscribe(l -> binding.locationShareTimeRemaining.setText(formatDuration(l, MeasureFormat.FormatWidth.SHORT)));
                        mServiceDisposableBag.add(mCountdownDisposable);
                    }
                }
            }
            binding.locationShareStop.setVisibility(View.VISIBLE);
            requireView().post(this::hideControls);
        } else {
            if (mCountdownDisposable != null) {
                mCountdownDisposable.dispose();
                mCountdownDisposable = null;
            }
            binding.btnShareLocation.setBackgroundColor(ContextCompat.getColor(binding.btnShareLocation.getContext(), R.color.colorSecondary));
            binding.btnShareLocation.setText(R.string.location_share_action_start);
            binding.btnShareLocation.setOnClickListener(v -> startSharing(getSelectedDuration()));
            binding.locationShareTimeRemaining.setVisibility(View.GONE);
            binding.locationShareTimeGroup.setVisibility(View.VISIBLE);
            binding.locationShareStop.setVisibility(View.GONE);
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
