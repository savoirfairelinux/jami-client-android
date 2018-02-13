/*
 *  Copyright (C) 2004-2017 Savoir-faire Linux Inc.
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
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.account.AccountWizardActivity;
import cx.ring.adapters.ContactDetailsTask;
import cx.ring.application.RingApplication;
import cx.ring.client.HomeActivity;
import cx.ring.dependencyinjection.RingInjectionComponent;
import cx.ring.model.Account;
import cx.ring.mvp.BaseFragment;
import cx.ring.utils.BitmapUtils;
import cx.ring.utils.VCardUtils;
import ezvcard.VCard;
import ezvcard.parameter.ImageType;
import ezvcard.property.FormattedName;
import ezvcard.property.Photo;

public class RingNavigationFragment extends BaseFragment<RingNavigationPresenter> implements NavigationAdapter.OnNavigationItemClicked,
        AccountAdapter.OnAccountActionClicked, RingNavigationView {

    public static final String TAG = RingNavigationFragment.class.getSimpleName();

    /***************
     * Header views
     ***************/

    @BindView(R.id.account_selection)
    protected RelativeLayout mSelectedAccountLayout;

    @BindView(R.id.addaccount_btn)
    protected Button mNewAccountBtn;

    @BindView(R.id.user_photo)
    protected ImageView mUserImage;

    @BindView(R.id.account_alias)
    protected TextView mSelectedAccountAlias;

    @BindView(R.id.account_disabled)
    protected TextView mSelectedAccountDisabled;

    @BindView(R.id.account_host)
    protected TextView mSelectedAccountHost;

    @BindView(R.id.loading_indicator)
    protected ProgressBar mSelectedAccountLoading;

    @BindView(R.id.error_indicator)
    protected ImageView mSelectedAccountError;

    @BindView(R.id.account_selected_arrow)
    protected ImageView mSelectedAccountArrow;

    /**************
     * Menu views
     **************/

    @BindView(R.id.drawer_menu)
    protected RecyclerView mMenuView;

    @BindView(R.id.drawer_accounts)
    protected RecyclerView mAccountsView;

    private NavigationAdapter mMenuAdapter;
    private OnNavigationSectionSelected mSectionListener;
    private AccountAdapter mAccountAdapter;
    private Account mSelectedAccount;
    private Bitmap mSourcePhoto;
    private ImageView mProfilePhoto;

    @Override
    public void onAccountSelected(Account selectedAccount) {

        toggleAccountList();

        presenter.setAccountOrder(selectedAccount);

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

    @Override
    public void onResume() {
        super.onResume();
        presenter.updateUser();
    }

    @Override
    public int getLayout() {
        return R.layout.frag_navigation;
    }

    @Override
    public void injectFragment(RingInjectionComponent component) {
        component.inject(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        presenter.updateUser();

        setupNavigationMenu();
        setupAccountList();
        if (savedInstanceState != null) {
            if (getFragmentManager().findFragmentByTag(HomeActivity.CONTACT_REQUESTS_TAG) != null &&
                    getFragmentManager().findFragmentByTag(HomeActivity.CONTACT_REQUESTS_TAG).isAdded()) {
                selectSection(Section.CONTACT_REQUESTS);
            } else if (getFragmentManager().findFragmentByTag(HomeActivity.ACCOUNTS_TAG) != null &&
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
    }

    private void setupAccountList() {
        mAccountAdapter = new AccountAdapter(presenter);
        mAccountAdapter.setOnAccountActionClickedListener(this);
        mAccountsView.setVisibility(View.GONE);
        mAccountsView.setHasFixedSize(true);
        mAccountsView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAccountsView.setAdapter(mAccountAdapter);
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
        if (getActivity() == null || vcard == null) {
            return;
        }

        if (!vcard.getPhotos().isEmpty()) {
            Photo tmp = vcard.getPhotos().get(0);
            Bitmap bitmap = BitmapFactory.decodeByteArray(tmp.getData(), 0, tmp.getData().length);
            mUserImage.setImageBitmap(BitmapUtils.cropImageToCircle(bitmap));
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
        Log.d(TAG, "updateUserView: User did change, updating user view.");
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
    public void addNewAccount() {
        getActivity().startActivity(new Intent(getActivity(), AccountWizardActivity.class));
    }

    private void setupNavigationMenu() {
        mMenuView.setHasFixedSize(true);
        LinearLayoutManager mLayoutManager2 = new LinearLayoutManager(getActivity());
        mMenuView.setLayoutManager(mLayoutManager2);

        ArrayList<NavigationItem> menu = new ArrayList<>();
        menu.add(0, new NavigationItem(R.string.menu_item_home, R.drawable.ic_home_black));
        menu.add(1, new NavigationItem(R.string.menu_item_contact_request, R.drawable.ic_drafts_black));
        menu.add(2, new NavigationItem(R.string.menu_item_accounts, R.drawable.ic_group_black));
        menu.add(3, new NavigationItem(R.string.menu_item_settings, R.drawable.ic_settings_black));
        menu.add(4, new NavigationItem(R.string.menu_item_share, R.drawable.ic_share_black));
        menu.add(5, new NavigationItem(R.string.menu_item_about, R.drawable.ic_info_black));

        mMenuAdapter = new NavigationAdapter(menu);
        mMenuView.setAdapter(mMenuAdapter);
        mMenuAdapter.setOnNavigationItemClickedListener(this);
    }

    public void setNavigationSectionSelectedListener(OnNavigationSectionSelected listener) {
        mSectionListener = listener;
    }

    @OnClick(R.id.profile_container)
    public void profileContainerClicked() {
        Log.d(TAG, "profileContainerClicked: Click on the edit profile");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.profile);

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.dialog_profile, null);

        final EditText editText = view.findViewById(R.id.user_name);
        editText.setText(presenter.getAlias(mSelectedAccount));
        mProfilePhoto = view.findViewById(R.id.profile_photo);
        mProfilePhoto.setImageDrawable(mUserImage.getDrawable());

        ImageButton cameraView = view.findViewById(R.id.camera);
        cameraView.setOnClickListener(v -> presenter.cameraClicked());

        ImageButton gallery = view.findViewById(R.id.gallery);
        gallery.setOnClickListener(v -> presenter.galleryClicked());

        builder.setView(view);

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            if (mSourcePhoto != null && mProfilePhoto.getDrawable() != ResourcesCompat.getDrawable(getResources(), R.drawable.ic_contact_picture, null)) {
                //FIXME: Reduce the bitmap but not use it.
                BitmapUtils.reduceBitmap(mSourcePhoto, VCardUtils.VCARD_PHOTO_SIZE);
                mSourcePhoto.compress(Bitmap.CompressFormat.PNG, 100, stream);
                Photo photo = new Photo(stream.toByteArray(), ImageType.PNG);

                presenter.saveVCardFormattedName(editText.getText().toString().trim());
                presenter.saveVCardPhoto(photo);
                mSourcePhoto = null;
            } else {
                presenter.saveVCardFormattedName(editText.getText().toString().trim());
            }
        });

        builder.show();
    }

    private void updateSelectedAccountView(Account selectedAccount) {
        if (selectedAccount == null) {
            return;
        }
        mSelectedAccount = selectedAccount;
        mSelectedAccountAlias.setText(presenter.getAccountAlias(selectedAccount));
        mSelectedAccountHost.setText(presenter.getUri(selectedAccount, getString(R.string.account_type_ip2ip)));
        mSelectedAccountDisabled.setVisibility(selectedAccount.isEnabled() ? View.GONE : View.VISIBLE);
        if (selectedAccount.isEnabled()) {
            if (!selectedAccount.isEnabled()) {
                mSelectedAccountError.setImageResource(R.drawable.ic_network_disconnect_black_24dp);
                mSelectedAccountError.setColorFilter(Color.BLACK);
                mSelectedAccountError.setVisibility(View.VISIBLE);
                mSelectedAccountLoading.setVisibility(View.GONE);
            } else if (selectedAccount.isTrying()) {
                mSelectedAccountError.setVisibility(View.GONE);
                mSelectedAccountLoading.setVisibility(View.VISIBLE);
            } else if (selectedAccount.needsMigration()) {
                mSelectedAccountHost.setText(R.string.account_update_needed);
                mSelectedAccountHost.setTextColor(Color.RED);
                mSelectedAccountError.setImageResource(R.drawable.ic_warning);
                mSelectedAccountError.setColorFilter(Color.RED);
                mSelectedAccountError.setVisibility(View.VISIBLE);
            } else if (selectedAccount.isInError() || !selectedAccount.isRegistered()) {
                mSelectedAccountError.setImageResource(R.drawable.ic_error_white);
                mSelectedAccountError.setColorFilter(Color.RED);
                mSelectedAccountError.setVisibility(View.VISIBLE);
                mSelectedAccountLoading.setVisibility(View.GONE);
            } else {
                mSelectedAccountError.setVisibility(View.GONE);
                mSelectedAccountLoading.setVisibility(View.GONE);
            }
        } else {
            mSelectedAccountError.setVisibility(View.GONE);
            mSelectedAccountLoading.setVisibility(View.GONE);
        }
    }

    @Override
    public void showViewModel(final RingNavigationViewModel viewModel) {
        RingApplication.uiHandler.post(() -> {
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
        });
    }

    @Override
    public void gotToImageCapture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        getActivity().startActivityForResult(intent, HomeActivity.REQUEST_CODE_PHOTO);
    }

    @Override
    public void askCameraPermission() {
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                HomeActivity.REQUEST_PERMISSION_CAMERA);
    }

    @Override
    public void goToGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        getActivity().startActivityForResult(intent, HomeActivity.REQUEST_CODE_GALLERY);
    }

    @Override
    public void askGalleryPermission() {
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                HomeActivity.REQUEST_PERMISSION_READ_STORAGE);
    }

    /**
     * Exposed enumeration listing app sections
     */
    public enum Section {
        HOME(0),
        CONTACT_REQUESTS(1),
        MANAGE(2),
        SETTINGS(3),
        SHARE(4),
        ABOUT(5);

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

    public interface OnNavigationSectionSelected {
        void onNavigationSectionSelected(Section position);

        void onAccountSelected();

        void onAddRingAccountSelected();

        void onAddSipAccountSelected();
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
}
