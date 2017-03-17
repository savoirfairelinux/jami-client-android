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
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.adapters.ContactDetailsTask;
import cx.ring.application.RingApplication;
import cx.ring.client.AccountWizard;
import cx.ring.client.HomeActivity;
import cx.ring.model.Account;
import cx.ring.mvp.GenericView;
import cx.ring.services.DeviceRuntimeService;
import cx.ring.utils.BitmapUtils;
import ezvcard.VCard;
import ezvcard.parameter.ImageType;
import ezvcard.property.FormattedName;
import ezvcard.property.Photo;

public class RingNavigationFragment extends Fragment implements NavigationAdapter.OnNavigationItemClicked,
        AccountAdapter.OnAccountActionClicked, GenericView<RingNavigationViewModel> {
    private static final String TAG = RingNavigationFragment.class.getSimpleName();

    private AccountAdapter mAccountAdapter;
    private Account mSelectedAccount;

    @Inject
    RingNavigationPresenter mRingNavigationPresenter;

    @Inject
    DeviceRuntimeService mDeviceRuntimeService;

    /***************
     * Header views
     ***************/

    @BindView(R.id.account_selection)
    RelativeLayout mSelectedAccountLayout;

    @BindView(R.id.addaccount_btn)
    Button mNewAccountBtn;

    @BindView(R.id.user_photo)
    ImageView mUserImage;
    private Bitmap mSourcePhoto;
    private ImageView mProfilePhoto;

    @BindView(R.id.account_alias)
    TextView mSelectedAccountAlias;

    @BindView(R.id.account_host)
    TextView mSelectedAccountHost;

    @BindView(R.id.account_selected_error_indicator)
    AppCompatImageView mSelectedAccountError;

    @BindView(R.id.account_selected_arrow)
    AppCompatImageView mSelectedAccountArrow;

    /**************
     * Menu views
     **************/

    @BindView(R.id.drawer_menu)
    RecyclerView mMenuView;

    @BindView(R.id.drawer_accounts)
    RecyclerView mAccountsView;

    private NavigationAdapter mMenuAdapter;
    private OnNavigationSectionSelected mSectionListener;

    @Override
    public void onAccountSelected(Account selectedAccount) {

        toggleAccountList();

        mRingNavigationPresenter.setAccountOrder(selectedAccount);

        if (mSectionListener != null) {
            mSectionListener.onAccountSelected();
        }
    }

    @Override
    public void onAddRINGAccountSelected() {
        toggleAccountList();
        if (mSectionListener != null) {
            mSectionListener.onAddRingAccountSelected();
        }
    }

    @Override
    public void onAddSIPAccountSelected() {
        toggleAccountList();
        if (mSectionListener != null) {
            mSectionListener.onAddSipAccountSelected();
        }
    }

    public interface OnNavigationSectionSelected {
        void onNavigationSectionSelected(Section position);

        void onAccountSelected();

        void onAddRingAccountSelected();

        void onAddSipAccountSelected();
    }

    @Override
    public void onResume() {
        super.onResume();
        mRingNavigationPresenter.bindView(this);
        mRingNavigationPresenter.updateUser();
    }

    @Override
    public void onPause() {
        super.onPause();
        mRingNavigationPresenter.unbindView();
    }

    /**
     * Exposed enumeration listing app sections
     */
    public enum Section {
        HOME(0),
        MANAGE(1),
        SETTINGS(2),
        SHARE(3),
        ABOUT(4);

        final int position;

        Section(int pos) {
            position = pos;
        }

        public static Section valueOf(int sectionInt) {
            for (Section section : Section.values()) {
                if (section.position == sectionInt) {
                    return section;
                }
            }
            return HOME;
        }
    }

    /**
     * Internal class describing navigation sections
     */
    class NavigationItem {
        int mResTitleId;
        int mResImageId;

        NavigationItem(int resTitle, int resId) {
            mResTitleId = resTitle;
            mResImageId = resId;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View inflatedView = inflater.inflate(R.layout.frag_navigation, container, false);

        ButterKnife.bind(this, inflatedView);

        // dependency injection
        ((RingApplication) getActivity().getApplication()).getRingInjectionComponent().inject(this);

        mRingNavigationPresenter.updateUser();

        setupNavigationMenu();
        setupAccountList();
        if (savedInstanceState != null) {
            if (getFragmentManager().findFragmentByTag(HomeActivity.ACCOUNTS_TAG) != null &&
                    getFragmentManager().findFragmentByTag(HomeActivity.ACCOUNTS_TAG).isAdded()) {
                selectSection(RingNavigationFragment.Section.MANAGE);
            } else if (getFragmentManager().findFragmentByTag(HomeActivity.SETTINGS_TAG) != null &&
                    getFragmentManager().findFragmentByTag(HomeActivity.SETTINGS_TAG).isAdded()) {
                selectSection(RingNavigationFragment.Section.SETTINGS);
            } else if (getFragmentManager().findFragmentByTag(HomeActivity.SHARE_TAG) != null &&
                    getFragmentManager().findFragmentByTag(HomeActivity.SHARE_TAG).isAdded()) {
                selectSection(RingNavigationFragment.Section.SHARE);
            } else if (getFragmentManager().findFragmentByTag(HomeActivity.ABOUT_TAG) != null &&
                    getFragmentManager().findFragmentByTag(HomeActivity.ABOUT_TAG).isAdded()) {
                selectSection(RingNavigationFragment.Section.ABOUT);
            } else {
                selectSection(RingNavigationFragment.Section.HOME);
            }
        }

        return inflatedView;
    }

    private void setupAccountList() {
        mAccountAdapter = new AccountAdapter(new ArrayList<Account>(), getActivity());
        mAccountsView.setHasFixedSize(true);
        mAccountAdapter.setOnAccountActionClickedListener(this);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mAccountsView.setLayoutManager(mLayoutManager);
        mAccountsView.setAdapter(mAccountAdapter);
        mAccountsView.setVisibility(View.GONE);
    }

    /**
     * Can be called to reset the UI to the initial state, displaying the navigation items
     */
    public void displayNavigation() {
        mMenuView.setVisibility(View.VISIBLE);
        mAccountsView.setVisibility(View.GONE);
        mSelectedAccountArrow.setImageResource(R.drawable.ic_arrow_drop_down_black_24dp);
    }

    public void selectSection(Section manage) {
        mMenuAdapter.setSelection(manage.position);
    }

    @OnClick(R.id.account_selection)
    public void toggleAccountList() {
        boolean navigationIsDisplaying = mMenuView.getVisibility() == View.VISIBLE;
        if (navigationIsDisplaying) {
            mMenuView.setVisibility(View.GONE);
            mAccountsView.setVisibility(View.VISIBLE);
            mSelectedAccountArrow.setImageResource(R.drawable.ic_arrow_drop_up_black_24dp);
        } else {
            mMenuView.setVisibility(View.VISIBLE);
            mAccountsView.setVisibility(View.GONE);
            mSelectedAccountArrow.setImageResource(R.drawable.ic_arrow_drop_down_black_24dp);
        }
    }

    @Override
    public void onNavigationItemClicked(int position) {
        if (mSectionListener != null) {
            mSectionListener.onNavigationSectionSelected(Section.valueOf(position));
        }
    }

    public void updateUserView(VCard vcard) {
        Log.d(TAG, "updateUserView");

        if (getActivity() == null) {
            return;
        }

        if (!vcard.getPhotos().isEmpty()) {
            Photo tmp = vcard.getPhotos().get(0);
            if (mSourcePhoto == null) {
                mSourcePhoto = BitmapFactory.decodeByteArray(tmp.getData(), 0, tmp.getData().length);
            }
            mUserImage.setImageBitmap(BitmapUtils.cropImageToCircle(mSourcePhoto));
        } else {
            mUserImage.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_contact_picture, null));
        }
        FormattedName name = vcard.getFormattedName();
        if (name != null) {
            String name_value = name.getValue();
            if (!TextUtils.isEmpty(name_value)) {
                mSelectedAccountAlias.setText(name_value);
            }
        }
        Log.d(TAG, "User did change, updating user view.");
    }

    public void updatePhoto(Uri uriImage) {
        Bitmap imageProfile = ContactDetailsTask.loadProfilePhotoFromUri(getActivity(), uriImage);
        updatePhoto(imageProfile);
    }

    public void updatePhoto(Bitmap image) {
        mSourcePhoto = image;
        mProfilePhoto.setImageBitmap(BitmapUtils.cropImageToCircle(image));
    }

    @OnClick(R.id.addaccount_btn)
    public void addNewAccount(View sender) {
        getActivity().startActivity(new Intent(getActivity(), AccountWizard.class));
    }

    private void setupNavigationMenu() {
        mMenuView.setHasFixedSize(true);
        LinearLayoutManager mLayoutManager2 = new LinearLayoutManager(getActivity());
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.profile);

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.dialog_profile, null);

        final EditText editText = (EditText) view.findViewById(R.id.user_name);
        editText.setText(mRingNavigationPresenter.getAlias(mSelectedAccount));
        mProfilePhoto = (ImageView) view.findViewById(R.id.profile_photo);
        mProfilePhoto.setImageDrawable(mUserImage.getDrawable());

        ImageButton cameraView = (ImageButton) view.findViewById(R.id.camera);
        cameraView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean hasPermission = mDeviceRuntimeService.hasVideoPermission() &&
                        mDeviceRuntimeService.hasPhotoPermission();
                if (hasPermission) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    getActivity().startActivityForResult(intent, HomeActivity.REQUEST_CODE_PHOTO);
                } else {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            HomeActivity.REQUEST_PERMISSION_CAMERA);
                }
            }
        });

        ImageButton gallery = (ImageButton) view.findViewById(R.id.gallery);
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean hasPermission = mDeviceRuntimeService.hasGalleryPermission();
                if (hasPermission) {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    getActivity().startActivityForResult(intent, HomeActivity.REQUEST_CODE_GALLERY);
                } else {
                    ActivityCompat.requestPermissions(getActivity(),
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

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                if (mSourcePhoto != null && mProfilePhoto.getDrawable() != ResourcesCompat.getDrawable(getResources(), R.drawable.ic_contact_picture, null)) {
                    mSourcePhoto.compress(Bitmap.CompressFormat.PNG, 100, stream);

                } else {
                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_contact_picture);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                }
                Photo photo = new Photo(stream.toByteArray(), ImageType.PNG);

                mRingNavigationPresenter.saveVCard(editText.getText().toString().trim(), photo);
            }
        });

        builder.show();
    }

    private void updateSelectedAccountView(Account selectedAccount) {
        if (selectedAccount == null) {
            return;
        }
        mSelectedAccount = selectedAccount;
        String alias = mRingNavigationPresenter.getAlias(selectedAccount);
        if (TextUtils.isEmpty(alias)) {
            alias = selectedAccount.getAlias();
        }
        mSelectedAccountAlias.setText(alias);
        mSelectedAccountHost.setText(mRingNavigationPresenter.getHost(selectedAccount, getString(R.string.account_type_ip2ip)));
        mSelectedAccountError.setVisibility(selectedAccount.isRegistered() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void showViewModel(final RingNavigationViewModel viewModel) {
        RingApplication.uiHandler.post(new Runnable() {
            @Override
            public void run() {
                mAccountAdapter.replaceAll(viewModel.getAccounts());
                updateUserView(viewModel.getVcard(getActivity().getFilesDir()));
                updateSelectedAccountView(viewModel.getAccount());

                if (viewModel.getAccounts().isEmpty()) {
                    mNewAccountBtn.setVisibility(View.VISIBLE);
                    mSelectedAccountLayout.setVisibility(View.GONE);
                } else {
                    mNewAccountBtn.setVisibility(View.GONE);
                    mSelectedAccountLayout.setVisibility(View.VISIBLE);
                }
            }
        });
    }
}
