/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Authors: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *           Rayan Osseiran <rayan.osseiran@savoirfairelinux.com>
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
package cx.ring.share;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;


import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;


import cx.ring.R;
import cx.ring.application.RingApplication;
import cx.ring.client.ConversationActivity;
import cx.ring.client.QRCodeActivity;
import cx.ring.dependencyinjection.RingInjectionComponent;
import cx.ring.fragments.ConversationFragment;
import cx.ring.mvp.BaseSupportFragment;

import cx.ring.utils.DeviceUtils;

public class ScanFragment extends BaseSupportFragment {

    public static final String TAG = ScanFragment.class.getSimpleName();

    private DecoratedBarcodeView barcodeView;
    private ViewGroup mErrorMessagePane;
    private TextView mErrorMessageTextView;

    public static final String KEY_ACCOUNT_ID = "accountId";
    private static String ACCOUNT_ID;
    private Boolean isTabletMode = false;


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public int getLayout() {
        return R.layout.frag_scan;
    }

    @Override
    public void injectFragment(RingInjectionComponent component) {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.frag_scan, container, false);

        barcodeView = rootView.findViewById(R.id.barcode_scanner);
        mErrorMessagePane = rootView.findViewById(R.id.error_msg_pane);
        mErrorMessageTextView = rootView.findViewById(R.id.error_msg_txt);


        if (DeviceUtils.isTablet(getActivity())) {
            isTabletMode = true;
        }

        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            hideErrorPanel();
            initializeBarcode();
        }


        return rootView;
    }


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if (ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, RingApplication.PERMISSIONS_REQUEST);

                } else {
                    displayNoPermissionsError();
                }
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED && barcodeView != null) {
            barcodeView.resume();
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED && barcodeView != null) {
            barcodeView.pause();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
    }

    private void showErrorPanel(final int textResId) {
        if (mErrorMessagePane != null) {
            mErrorMessagePane.setVisibility(View.VISIBLE);
        }
        if (mErrorMessageTextView != null) {
            mErrorMessageTextView.setText(textResId);
        }
        if (barcodeView != null) {
            barcodeView.setVisibility(View.GONE);
        }
    }

    private void hideErrorPanel() {
        if (mErrorMessagePane != null) {
            mErrorMessagePane.setVisibility(View.GONE);
        }
        if (barcodeView != null) {
            barcodeView.setVisibility(View.VISIBLE);
        }
    }

    public void displayNoPermissionsError() {
        showErrorPanel(R.string.error_scan_no_camera_permissions);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0, n = permissions.length; i < n; i++) {
            switch (permissions[i]) {
                case Manifest.permission.CAMERA:
                    boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                    if (granted) {
                        hideErrorPanel();
                        initializeBarcode();
                    } else {
                        displayNoPermissionsError();
                    }
                    return;
                default:
                    break;
            }
        }
    }

    private void initializeBarcode() {
        if (barcodeView != null) {
            Collection<BarcodeFormat> formats = Arrays.asList(BarcodeFormat.QR_CODE);
            barcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(formats));
            barcodeView.initializeFromIntent(getActivity().getIntent());
            barcodeView.decodeContinuous(callback);
        }
    }

    private BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(@NonNull BarcodeResult result) {
            if (result.getText() == null) {
                return;
            } else {
                ACCOUNT_ID = getActivity().getIntent().getExtras().getString(KEY_ACCOUNT_ID);
                String contact_uri = result.getText();
                if (contact_uri != null && ACCOUNT_ID != null) {
                    goToConversation(ACCOUNT_ID, new cx.ring.model.Uri(contact_uri));
                }
            }
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
        }
    };


    public void goToConversation(String accountId, cx.ring.model.Uri contactId) {
        if (!isTabletMode) {
            Intent intent = new Intent()
                    .setClass(getActivity(), ConversationActivity.class)
                    .setAction(Intent.ACTION_VIEW)
                    .putExtra(ConversationFragment.KEY_ACCOUNT_ID, accountId)
                    .putExtra(ConversationFragment.KEY_CONTACT_RING_ID, contactId.toString());
            startActivity(intent);
        } else {
            Bundle bundle = new Bundle();
            bundle.putString(ConversationFragment.KEY_CONTACT_RING_ID, contactId.toString());
            bundle.putString(ConversationFragment.KEY_ACCOUNT_ID, accountId);
            ((QRCodeActivity) getActivity()).startConversationTablet(bundle);
        }
    }





}
