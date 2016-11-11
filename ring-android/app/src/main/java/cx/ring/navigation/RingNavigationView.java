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
package cx.ring.navigation;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.adapters.AccountSelectionAdapter;
import cx.ring.adapters.ContactDetailsTask;
import cx.ring.application.RingApplication;
import cx.ring.client.AccountWizard;
import cx.ring.client.HomeActivity;
import cx.ring.model.Account;
import cx.ring.service.LocalService;
import cx.ring.services.StateService;
import cx.ring.utils.CropImageUtils;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import ezvcard.parameter.ImageType;
import ezvcard.property.FormattedName;
import ezvcard.property.Photo;
import ezvcard.property.RawProperty;

public class RingNavigationView extends NavigationView implements NavigationAdapter.OnNavigationItemClicked {
    private static final String TAG = RingNavigationView.class.getSimpleName();

    private AccountSelectionAdapter mAccountAdapter;

    @Inject
    StateService mStateService;

    /****** Header views ******/

    @BindView(R.id.account_selection)
    Spinner mSpinnerAccounts;

    @BindView(R.id.addaccount_btn)
    Button mNewAccountBtn;

    @BindView(R.id.user_photo)
    ImageView mUserImage;

    @BindView(R.id.user_name)
    TextView mUserName;
    private Bitmap mSourcePhoto;
    private ImageView mProfilePhoto;
    private VCard mVCardProfile;

    /****** Menu views ******/

    @BindView(R.id.drawer_menu)
    RecyclerView mMenuView;

    private NavigationAdapter mMenuAdapter;
    private OnNavigationSectionSelected mSectionListener;

    public void selectSection(Section manage) {
        mMenuAdapter.setSelection(manage.position);
    }

    @Override
    public void onNavigationItemClicked(int position) {
        if (mSectionListener != null){
            mSectionListener.onNavigationSectionSelected(Section.valueOf(position));
        }
    }

    public interface OnNavigationSectionSelected {
        void onNavigationSectionSelected(Section position);
    }


    public interface MenuHeaderAccountSelectionListener {
        void accountSelected(Account account);
    }

    public enum Section {
        HOME(0),
        MANAGE(1),
        SETTINGS(2),
        SHARE(3),
        ABOUT(4);

        int position;
        Section(int pos) {
            position = pos;
        }

        public static Section valueOf(int sec) {
            for (Section s : Section.values()) {
                if (s.position == sec) return s;
            }
            return HOME;
        }
    }

    private List<WeakReference<MenuHeaderAccountSelectionListener>> mListeners;

    public RingNavigationView(Context context) {
        super(context);

        if (context instanceof Activity) {
            ((RingApplication)((Activity)context).getApplication()).getRingInjectionComponent().inject(this);
        }

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

                    // modify the state of the app
                    // State observers will be notified
                    mStateService.setCurrentAccount(getSelectedAccount());

                    for (WeakReference<MenuHeaderAccountSelectionListener> weakListener : mListeners) {
                        MenuHeaderAccountSelectionListener listener = weakListener.get();
                        if (listener != null) {
                            listener.accountSelected(getSelectedAccount());
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                    mAccountAdapter.setSelectedAccount(-1);
                }
            });
            updateAccounts(service.getAccounts());
        }
    }

    public void registerAccountSelectionListener(MenuHeaderAccountSelectionListener listener) {
        mListeners.add(new WeakReference<>(listener));
        listener.accountSelected(getSelectedAccount());
    }

    public void updateUserView() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Log.d(TAG, "updateUserView");
        if (null != inflater) {
            mVCardProfile = VCardUtils.loadLocalProfileFromDisk(getContext());
            if (!mVCardProfile.getPhotos().isEmpty()) {
                Photo tmp = mVCardProfile.getPhotos().get(0);
                mUserImage.setImageBitmap(CropImageUtils.cropImageToCircle(tmp.getData()));
            } else {
                mUserImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_contact_picture, null));
            }
            mUserName.setText(mVCardProfile.getFormattedName().getValue());
            Log.d(TAG, "User did change, updating user view.");
        }
    }

    public void updatePhoto(Uri uriImage) {
        Bitmap imageProfile = ContactDetailsTask.loadProfilePhotoFromUri(getContext(), uriImage);
        updatePhoto(imageProfile);
    }

    public void updatePhoto(Bitmap image) {
        mSourcePhoto = image;
        mProfilePhoto.setImageBitmap(CropImageUtils.cropImageToCircle(image));
    }

    @OnClick(R.id.addaccount_btn)
    public void addNewAccount(View sender) {
        getContext().startActivity(new Intent(sender.getContext(), AccountWizard.class));
    }

    private void initViews() {
        final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View inflatedView = inflater.inflate(R.layout.ring_navigation_view, this);

        ButterKnife.bind(this, inflatedView);

        mAccountAdapter = new AccountSelectionAdapter(inflater.getContext(), new ArrayList<Account>());
        mSpinnerAccounts.setAdapter(mAccountAdapter);
        mVCardProfile = VCardUtils.loadLocalProfileFromDisk(getContext());

        updateUserView();

        mListeners = new ArrayList<>();

        setupNavigationMenu();
    }

    private void setupNavigationMenu() {
        mMenuView.setHasFixedSize(true);
        LinearLayoutManager mLayoutManager2 = new LinearLayoutManager(getContext());
        mMenuView.setLayoutManager(mLayoutManager2);

        ArrayList<NavigationItem> menu = new ArrayList<>();
        menu.add(0, new NavigationItem(R.string.menu_item_home, R.drawable.ic_home_black));
        menu.add(1, new NavigationItem(R.string.menu_item_accounts, R.drawable.ic_group_black));
        menu.add(2, new NavigationItem(R.string.menu_item_settings, R.drawable.ic_settings_black));
        menu.add(3, new NavigationItem(R.string.menu_item_share, R.drawable.ic_share_black));
        menu.add(4, new NavigationItem(R.string.menu_item_about, R.drawable.ic_info_black));

        mMenuAdapter = new NavigationAdapter(menu);
        mMenuView.setAdapter(mMenuAdapter);
        mMenuAdapter.setOnNavigationItemClickedListener(this);
    }

    public void setNavigationSectionSelectedListener(OnNavigationSectionSelected listener) {
        mSectionListener = listener;
    }

    @OnClick(R.id.profile_container)
    public void profileContainerClicked() {
        Log.d(TAG, "Click on the edit profile");
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.profile);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.dialog_profile, null);

        final EditText editText = (EditText) view.findViewById(R.id.user_name);
        editText.setText(mUserName.getText());
        mProfilePhoto = (ImageView) view.findViewById(R.id.profile_photo);
        mProfilePhoto.setImageDrawable(mUserImage.getDrawable());

        ImageButton cameraView = (ImageButton) view.findViewById(R.id.camera);
        cameraView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean hasPermission = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                if (hasPermission) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    ((Activity) getContext()).startActivityForResult(intent, HomeActivity.REQUEST_CODE_PHOTO);
                } else {
                    ActivityCompat.requestPermissions((Activity) getContext(),
                            new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            HomeActivity.REQUEST_PERMISSION_CAMERA);
                }
            }
        });

        ImageButton gallery = (ImageButton) view.findViewById(R.id.gallery);
        gallery.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean hasPermission = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                if (hasPermission) {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    ((Activity) getContext()).startActivityForResult(intent, HomeActivity.REQUEST_CODE_GALLERY);
                } else {
                    ActivityCompat.requestPermissions((Activity) getContext(),
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            HomeActivity.REQUEST_PERMISSION_READ_STORAGE);
                }
            }
        });

        builder.setView(view);

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String username = editText.getText().toString().trim();
                if (username.isEmpty()) {
                    username = getContext().getString(R.string.unknown);
                }
                mVCardProfile.setFormattedName(new FormattedName(username));

                if (mSourcePhoto != null && mProfilePhoto.getDrawable() != ResourcesCompat.getDrawable(getResources(), R.drawable.ic_contact_picture, null)) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    mSourcePhoto.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    Photo photo = new Photo(stream.toByteArray(), ImageType.PNG);
                    mVCardProfile.removeProperties(Photo.class);
                    mVCardProfile.addPhoto(photo);
                }

                mVCardProfile.removeProperties(RawProperty.class);
                VCardUtils.saveLocalProfileToDisk(mVCardProfile, getContext());
                updateUserView();
            }
        });

        builder.show();
    }

    public Account getSelectedAccount() {
        return mAccountAdapter.getSelectedAccount();
    }

    public void updateAccounts(List<Account> accounts) {

        if (accounts.isEmpty()) {
            mNewAccountBtn.setVisibility(View.VISIBLE);
            mSpinnerAccounts.setVisibility(View.GONE);

            // modify the state of the app
            // State observers will be notified
            mStateService.setCurrentAccount(null);
        } else {
            mNewAccountBtn.setVisibility(View.GONE);
            mSpinnerAccounts.setVisibility(View.VISIBLE);
            mAccountAdapter.replaceAll(accounts);
            mSpinnerAccounts.setSelection(0);

            // modify the state of the app
            // State observers will be notified
            mStateService.setCurrentAccount(getSelectedAccount());
        }
    }

    public class NavigationItem {
        int mResTitleId;
        int mResImageId;

        NavigationItem(int resTitle, int resId) {
            mResTitleId = resTitle;
            mResImageId = resId;
        }
    }
}
