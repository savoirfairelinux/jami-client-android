/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
package cx.ring.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import cx.ring.R;
import cx.ring.adapters.AccountSelectionAdapter;
import cx.ring.adapters.ContactPictureTask;
import cx.ring.client.AccountWizard;
import cx.ring.model.account.Account;
import cx.ring.model.CallContact;
import cx.ring.service.LocalService;

import java.util.ArrayList;
import java.util.List;

public class MenuFragment extends Fragment {

    @SuppressWarnings("unused")
    private static final String TAG = MenuFragment.class.getSimpleName();

    private AccountSelectionAdapter mAccountAdapter;
    private Spinner spinnerAccounts;
    private ImageButton shareBtn;
    private Button newAccountBtn;
    private ImageView qrImage;

    private LocalService.Callbacks mCallbacks = LocalService.DUMMY_CALLBACKS;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof LocalService.Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }
        mCallbacks = (LocalService.Callbacks) activity;
        updateAllAccounts();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = LocalService.DUMMY_CALLBACKS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().contentEquals(LocalService.ACTION_ACCOUNT_UPDATE)) {
                updateAllAccounts();
            }
        }
    };

    @Override
    public void onResume() {
        Log.i(TAG, "Resuming");
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocalService.ACTION_ACCOUNT_UPDATE);
        getActivity().registerReceiver(mReceiver, intentFilter);
        updateAllAccounts();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.frag_menu_header, parent, false);

        newAccountBtn = (Button) inflatedView.findViewById(R.id.addaccount_btn);
        newAccountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent().setClass(getActivity(), AccountWizard.class);
                startActivityForResult(intent, AccountsManagementFragment.ACCOUNT_CREATE_REQUEST);
            }
        });

        shareBtn = (ImageButton) inflatedView.findViewById(R.id.share_btn);
        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Account acc = mAccountAdapter.getSelectedAccount();
                String share_uri = acc.getShareURI();
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                String shareBody = "Contact me using " + share_uri + " on the Ring distributed communication platform: http://ring.cx";
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Contact me on Ring !");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, getText(R.string.share_via)));
            }
        });
        qrImage = (ImageView) inflatedView.findViewById(R.id.qr_image);
        spinnerAccounts = (Spinner) inflatedView.findViewById(R.id.account_selection);
        mAccountAdapter = new AccountSelectionAdapter(getActivity(), new ArrayList<Account>());
        spinnerAccounts.setAdapter(mAccountAdapter);
        spinnerAccounts.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View view, int pos, long arg3) {
                Log.w(TAG, "onItemSelected -> setSelectedAccount" + pos);
                mAccountAdapter.setSelectedAccount(pos);

                String share_uri = getSelectedAccount().getShareURI();
                Bitmap qrBitmap = encodeStringAsQrBitmap(share_uri, qrImage.getWidth());
                qrImage.setImageBitmap(qrBitmap);

                mCallbacks.getService().setAccountOrder(mAccountAdapter.getAccountOrder());
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                Log.w(TAG, "onNothingSelected -1");
                mAccountAdapter.setSelectedAccount(-1);
            }
        });

        CallContact user = CallContact.buildUserContact(getActivity());
        new ContactPictureTask(getActivity(), (ImageView) inflatedView.findViewById(R.id.user_photo), user).run();

        ((TextView) inflatedView.findViewById(R.id.user_name)).setText(user.getDisplayName());

        updateAllAccounts();
        return inflatedView;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public Account getSelectedAccount() {
        Log.w(TAG, "getSelectedAccount " + mAccountAdapter.getSelectedAccount().getAccountID());
        return mAccountAdapter.getSelectedAccount();
    }

    public void updateAllAccounts() {
        if (mAccountAdapter != null && mCallbacks.getService() != null) {
            List<Account> accs = mCallbacks.getService().getAccounts();
            if (accs.isEmpty()) {
                newAccountBtn.setVisibility(View.VISIBLE);
                shareBtn.setVisibility(View.GONE);
                spinnerAccounts.setVisibility(View.GONE);
            } else {
                newAccountBtn.setVisibility(View.GONE);
                shareBtn.setVisibility(View.VISIBLE);
                spinnerAccounts.setVisibility(View.VISIBLE);
                mAccountAdapter.replaceAll(accs);
            }
        }
    }

    private Bitmap encodeStringAsQrBitmap(String input, int qrWindowPixels){
        QRCodeWriter qr_writer = new QRCodeWriter();
        BitMatrix qr_image_matrix = null;
        try {
            qr_image_matrix = qr_writer.encode(input, BarcodeFormat.QR_CODE, qrWindowPixels , qrWindowPixels);
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }

        int qrImageWidth = qr_image_matrix.getWidth();
        int qrImageHeight = qr_image_matrix.getHeight();
        int[] pixels = new int[qrImageWidth * qrImageHeight];

        final int BLACK = 0x00FFFFFF;
        final int WHITE = 0xFFFFFFFF;

        for (int row = 0; row < qrImageHeight; row++) {
            int offset = row * qrImageWidth;
            for (int column = 0; column < qrImageWidth; column++) {
                pixels[offset + column] = qr_image_matrix.get(column, row) ? BLACK : WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(qrImageWidth, qrImageHeight, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, qrImageWidth, 0, 0, qrImageWidth, qrImageHeight);

        return bitmap;
    }

}
