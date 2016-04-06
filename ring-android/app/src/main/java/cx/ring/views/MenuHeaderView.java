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
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.views;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

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

public class MenuHeaderView extends FrameLayout {
    private static final String TAG = MenuHeaderView.class.getSimpleName();

    private AccountSelectionAdapter mAccountAdapter;
    private Spinner spinnerAccounts;
    private ImageButton shareBtn;
    private Button newAccountBtn;
    private ImageView qrImage;

    public MenuHeaderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initViews();
    }

    public MenuHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViews();
    }

    public MenuHeaderView(Context context) {
        super(context);
        initViews();
    }

    public void setCallbacks(final LocalService service) {
        if (service != null) {
            spinnerAccounts.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View view, int pos, long arg3) {
                    Log.w(TAG, "onItemSelected -> setSelectedAccount " + pos);
                    if (mAccountAdapter.getAccount(pos) != mAccountAdapter.getSelectedAccount()) {
                        mAccountAdapter.setSelectedAccount(pos);
                        service.setAccountOrder(mAccountAdapter.getAccountOrder());
                    }
                    String share_uri = getSelectedAccount().getShareURI();
                    Bitmap qrBitmap = encodeStringAsQrBitmap(share_uri, qrImage.getWidth());
                    qrImage.setImageBitmap(qrBitmap);
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                    Log.w(TAG, "onNothingSelected -1");
                    mAccountAdapter.setSelectedAccount(-1);
                }
            });
            updateAccounts(service.getAccounts());
        }
    }

    private void initViews() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View inflatedView = inflater.inflate(R.layout.frag_menu_header, this);

        newAccountBtn = (Button) inflatedView.findViewById(R.id.addaccount_btn);
        newAccountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getContext().startActivity(new Intent(v.getContext(), AccountWizard.class));
            }
        });

        mAccountAdapter = new AccountSelectionAdapter(inflater.getContext(), new ArrayList<Account>());

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
                getContext().startActivity(Intent.createChooser(sharingIntent, getContext().getText(R.string.share_via)));
            }
        });
        qrImage = (ImageView) inflatedView.findViewById(R.id.qr_image);
        spinnerAccounts = (Spinner) inflatedView.findViewById(R.id.account_selection);
        spinnerAccounts.setAdapter(mAccountAdapter);

        CallContact user = CallContact.buildUserContact(inflater.getContext());
        new ContactPictureTask(inflater.getContext(), (ImageView) inflatedView.findViewById(R.id.user_photo), user).run();

        ((TextView) inflatedView.findViewById(R.id.user_name)).setText(user.getDisplayName());
    }

    public Account getSelectedAccount() {
        return mAccountAdapter.getSelectedAccount();
    }

    public void updateAccounts(List<Account> accs) {
        if (accs.isEmpty()) {
            newAccountBtn.setVisibility(View.VISIBLE);
            shareBtn.setVisibility(View.GONE);
            spinnerAccounts.setVisibility(View.GONE);
            qrImage.setVisibility(View.GONE);
        } else {
            newAccountBtn.setVisibility(View.GONE);
            shareBtn.setVisibility(View.VISIBLE);
            spinnerAccounts.setVisibility(View.VISIBLE);
            qrImage.setVisibility(View.VISIBLE);
            mAccountAdapter.replaceAll(accs);
            spinnerAccounts.setSelection(0);
        }
    }

    private Bitmap encodeStringAsQrBitmap(String input, int qrWindowPixels) {
        QRCodeWriter qr_writer = new QRCodeWriter();
        BitMatrix qr_image_matrix = null;
        try {
            qr_image_matrix = qr_writer.encode(input, BarcodeFormat.QR_CODE, qrWindowPixels, qrWindowPixels);
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
