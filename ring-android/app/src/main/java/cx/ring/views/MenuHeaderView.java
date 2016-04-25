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

import java.util.ArrayList;
import java.util.List;

import cx.ring.R;
import cx.ring.adapters.AccountSelectionAdapter;
import cx.ring.adapters.ContactPictureTask;
import cx.ring.client.AccountWizard;
import cx.ring.client.HomeActivity;
import cx.ring.model.CallContact;
import cx.ring.model.account.Account;
import cx.ring.service.LocalService;

public class MenuHeaderView extends FrameLayout {
    private static final String TAG = MenuHeaderView.class.getSimpleName();

    private AccountSelectionAdapter mAccountAdapter;
    private Spinner mSpinnerAccounts;
    private ImageButton mShareBtn;
    private Button mNewAccountBtn;
    private ImageButton mQrImage;
    private ImageView mUserImage;
    private TextView mUserName;
    private CallContact mCurrentlyDisplayedUser;

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
            mSpinnerAccounts.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View view, int pos, long arg3) {
                    if (mAccountAdapter.getAccount(pos) != mAccountAdapter.getSelectedAccount()) {
                        mAccountAdapter.setSelectedAccount(pos);
                        service.setAccountOrder(mAccountAdapter.getAccountOrder());
                    }
                    String share_uri = getSelectedAccount().getShareURI();
                    Bitmap qrBitmap = HomeActivity.QRCodeFragment.encodeStringAsQrBitmap(share_uri, mQrImage.getWidth());
                    mQrImage.setImageBitmap(qrBitmap);
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                    mAccountAdapter.setSelectedAccount(-1);
                }
            });
            updateAccounts(service.getAccounts());
        }
    }

    public void updateUserView() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (null != inflater) {
            boolean shouldUpdate = true;
            CallContact user = CallContact.buildUserContact(inflater.getContext());
            if (null != this.mCurrentlyDisplayedUser && this.mCurrentlyDisplayedUser.equals(user)) {
                shouldUpdate = false;
                Log.d(TAG,"User did not change, not updating user view.");
            }
            if (shouldUpdate) {
                this.mCurrentlyDisplayedUser = user;
                new ContactPictureTask(inflater.getContext(), mUserImage, user).run();
                mUserName.setText(user.getDisplayName());
                Log.d(TAG,"User did change, updating user view.");
            }
        }
    }

    private void initViews() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View inflatedView = inflater.inflate(R.layout.frag_menu_header, this);

        mNewAccountBtn = (Button) inflatedView.findViewById(R.id.addaccount_btn);
        mNewAccountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getContext().startActivity(new Intent(v.getContext(), AccountWizard.class));
            }
        });

        mAccountAdapter = new AccountSelectionAdapter(inflater.getContext(), new ArrayList<Account>());

        mShareBtn = (ImageButton) inflatedView.findViewById(R.id.share_btn);
        mShareBtn.setOnClickListener(new View.OnClickListener() {
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
        mQrImage = (ImageButton) inflatedView.findViewById(R.id.qr_image);
        mSpinnerAccounts = (Spinner) inflatedView.findViewById(R.id.account_selection);
        mSpinnerAccounts.setAdapter(mAccountAdapter);

        mUserImage = (ImageView) inflatedView.findViewById(R.id.user_photo);
        mUserName = (TextView) inflatedView.findViewById(R.id.user_name);
        this.updateUserView();
    }

    public Account getSelectedAccount() {
        return mAccountAdapter.getSelectedAccount();
    }

    public void updateAccounts(List<Account> accs) {
        if (accs.isEmpty()) {
            mNewAccountBtn.setVisibility(View.VISIBLE);
            mShareBtn.setVisibility(View.GONE);
            mSpinnerAccounts.setVisibility(View.GONE);
            mQrImage.setVisibility(View.GONE);
        } else {
            mNewAccountBtn.setVisibility(View.GONE);
            mShareBtn.setVisibility(View.VISIBLE);
            mSpinnerAccounts.setVisibility(View.VISIBLE);
            mQrImage.setVisibility(View.VISIBLE);
            mAccountAdapter.replaceAll(accs);
            mSpinnerAccounts.setSelection(0);
        }
    }

    public void setQRCodeListener(OnClickListener l) {
        mQrImage.setOnClickListener(l);
    }

}
