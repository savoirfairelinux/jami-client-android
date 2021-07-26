/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;

import java.util.Collections;
import java.util.List;


import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.client.HomeActivity;
import cx.ring.fragments.QRCodeFragment;

public class ScanFragment extends Fragment {
    public static final String TAG = ScanFragment.class.getSimpleName();

    private DecoratedBarcodeView barcodeView;
    private TextView mErrorMessageTextView;

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.frag_scan, container, false);
        barcodeView = rootView.findViewById(R.id.barcode_scanner);
        mErrorMessageTextView = rootView.findViewById(R.id.error_msg_txt);

        if (hasCameraPermission()) {
            hideErrorPanel();
            initializeBarcode();
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkPermission() && barcodeView != null) {
            barcodeView.resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (hasCameraPermission() && barcodeView != null) {
            barcodeView.pause();
        }
    }

    private void showErrorPanel(@StringRes final int textResId) {
        if (mErrorMessageTextView != null) {
            mErrorMessageTextView.setText(textResId);
            mErrorMessageTextView.setVisibility(View.VISIBLE);
        }
        if (barcodeView != null) {
            barcodeView.setVisibility(View.GONE);
        }
    }

    private void hideErrorPanel() {
        if (mErrorMessageTextView != null) {
            mErrorMessageTextView.setVisibility(View.GONE);
        }
        if (barcodeView != null) {
            barcodeView.setVisibility(View.VISIBLE);
        }
    }

    private void displayNoPermissionsError() {
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
            barcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(Collections.singletonList(BarcodeFormat.QR_CODE)));
            //barcodeView.initializeFromIntent(getActivity().getIntent());
            barcodeView.decodeContinuous(callback);
        }
    }

    private final BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(@NonNull BarcodeResult result) {
            if (result.getText() != null) {
                String contactUri = result.getText();
                if (contactUri != null) {
                    QRCodeFragment parent = (QRCodeFragment) getParentFragment();
                    if (parent != null) {
                        parent.dismiss();
                    }
                    goToConversation(contactUri);
                }
            }
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
        }
    };

    private void goToConversation(String conversationUri) {
        ((HomeActivity) requireActivity()).startConversation(conversationUri);
    }

    private boolean checkPermission() {
        if (!hasCameraPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, JamiApplication.PERMISSIONS_REQUEST);
            } else {
                displayNoPermissionsError();
            }
            return false;
        }
        return true;
    }

}
