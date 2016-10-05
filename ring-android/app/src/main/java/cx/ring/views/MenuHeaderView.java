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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cx.ring.R;
import cx.ring.adapters.AccountSelectionAdapter;
import cx.ring.adapters.ContactDetailsTask;
import cx.ring.adapters.PhotoAdapter;
import cx.ring.client.AccountWizard;
import cx.ring.client.HomeActivity;
import cx.ring.model.CallContact;
import cx.ring.model.account.Account;
import cx.ring.service.LocalService;
import cx.ring.utils.CropImageUtils;

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
    private ImageView mPhotoCamera;

    //static private int REQUEST_IMAGE_CAPTURE = 1;

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
        Log.d(TAG, "updateUserView");
        if (null != inflater) {
            boolean shouldUpdate = true;
            CallContact user = CallContact.buildUserContact(inflater.getContext());
            if (null != this.mCurrentlyDisplayedUser && this.mCurrentlyDisplayedUser.equals(user)) {
                shouldUpdate = false;
                Log.d(TAG,"User did not change, not updating user view.");
            }
            if (shouldUpdate) {
                this.mCurrentlyDisplayedUser = user;
                new ContactDetailsTask(inflater.getContext(), mUserImage, user).run();
                mUserName.setText(user.getDisplayName());
                Log.d(TAG,"User did change, updating user view.");
            }
        }
    }

    public void updatePhoto(Bitmap image){
        Bitmap imageProfile = CropImageUtils.cropImageToCircle(rotation(image));
        mPhotoCamera.setImageBitmap(Bitmap.createScaledBitmap(imageProfile, 100, 100, false));
        mPhotoCamera.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }

    public Bitmap rotation(Bitmap image){
        /*ExifInterface exif = new ExifInterface(image);
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);*/

        return image;
    }

    private void initViews() {
        final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
        mUserImage.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                boolean hasReadExternalStoragePermission = ContextCompat.checkSelfPermission(getContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                if (hasReadExternalStoragePermission) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Photo");

                    LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
                    ViewGroup view = (ViewGroup) inflater.inflate(R.layout.dialog_profile_photo, null);

                    mPhotoCamera = (ImageView) view.findViewById(R.id.photoCamera);

                    final RelativeLayout layout = (RelativeLayout) view.findViewById(R.id.camera);
                    layout.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            ((Activity) getContext()).startActivityForResult(intent, HomeActivity.REQUEST_CODE_PHOTO);
                        }
                    });

                    TextView gallery = (TextView) view.findViewById(R.id.gallery);
                    gallery.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            ((Activity) getContext()).startActivityForResult(intent, HomeActivity.REQUEST_CODE_GALLERY);
                        }
                    });

                    builder.setView(view);

                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mUserImage = mPhotoCamera;
                            if(null != mUserImage)
                                CallContact.setUserContact(getContext(), mUserImage.getDrawable());
                        }
                    });

                    builder.show();
                }
                else {
                    Log.d(TAG, "permission denied");
                    try {
                        ActivityCompat.requestPermissions((Activity) getContext(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 60);
                    }
                    catch(Exception e){
                        Log.w(TAG, e);
                    }
                }
            }
        });

        mUserName = (TextView) inflatedView.findViewById(R.id.user_name);
        mUserName.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Name");
                builder.setMessage("This is shared with your contacts");

                final EditText edittext = new EditText(getContext());
                edittext.setText(mUserName.getText());
                builder.setView(edittext);

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                        Log.d(TAG, "name change : cancel");
                    }
                });

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int wichButton){
                        mUserName.setText(edittext.getText());
                        mCurrentlyDisplayedUser.setDisplayName(mUserName.getText().toString());
                        Log.d(TAG, "name change : " + mUserName.getText());

                        CallContact.setUserContact(getContext(), mUserName.getText().toString());
                    }
                });

                builder.show();
            }
        });

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
