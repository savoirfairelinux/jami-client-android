/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import butterknife.BindView;
import butterknife.OnClick;
import cx.ring.R;
import cx.ring.account.AccountWizardActivity;
import cx.ring.client.CallActivity;
import cx.ring.client.HomeActivity;
import cx.ring.dependencyinjection.RingInjectionComponent;
import cx.ring.model.Account;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.services.VCardServiceImpl;
import cx.ring.utils.AndroidFileUtils;
import cx.ring.utils.BitmapUtils;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.Tuple;
import cx.ring.views.AvatarDrawable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class RingNavigationFragment extends BaseSupportFragment<RingNavigationPresenter> implements NavigationAdapter.OnNavigationItemClicked,
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
    private Uri tmpProfilePhotoUri;

    private final CompositeDisposable mDisposableBag = new CompositeDisposable();

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
    public int getLayout() {
        return R.layout.frag_navigation;
    }

    @Override
    public void injectFragment(RingInjectionComponent component) {
        component.inject(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupNavigationMenu();
        setupAccountList();
        if (savedInstanceState != null) {
            FragmentManager fm = getFragmentManager();
            if (fm.findFragmentByTag(HomeActivity.CONTACT_REQUESTS_TAG) != null &&
                    fm.findFragmentByTag(HomeActivity.CONTACT_REQUESTS_TAG).isAdded()) {
                selectSection(Section.CONTACT_REQUESTS);
            } else if (fm.findFragmentByTag(HomeActivity.ACCOUNTS_TAG) != null &&
                    fm.findFragmentByTag(HomeActivity.ACCOUNTS_TAG).isAdded()) {
                selectSection(RingNavigationFragment.Section.MANAGE);
            } else if (fm.findFragmentByTag(HomeActivity.SETTINGS_TAG) != null &&
                    fm.findFragmentByTag(HomeActivity.SETTINGS_TAG).isAdded()) {
                selectSection(RingNavigationFragment.Section.SETTINGS);
            } else if (fm.findFragmentByTag(HomeActivity.ABOUT_TAG) != null &&
                    fm.findFragmentByTag(HomeActivity.ABOUT_TAG).isAdded()) {
                selectSection(RingNavigationFragment.Section.ABOUT);
            } else {
                selectSection(RingNavigationFragment.Section.HOME);
            }
        }
    }

    @Override
    public void onDestroyView() {
        mDisposableBag.clear();
        super.onDestroyView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case HomeActivity.REQUEST_CODE_CALL:
                if (resultCode == CallActivity.RESULT_FAILURE) {
                    Log.w(TAG, "Call Failed");
                }
                break;
            case HomeActivity.REQUEST_CODE_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    if (tmpProfilePhotoUri == null) {
                        if (intent != null)
                            updatePhoto((Bitmap) intent.getExtras().get("data"));
                    } else {
                        updatePhoto(tmpProfilePhotoUri);
                    }
                }
                tmpProfilePhotoUri = null;
                break;
            case HomeActivity.REQUEST_CODE_GALLERY:
                if (resultCode == Activity.RESULT_OK && intent != null) {
                    updatePhoto(intent.getData());
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case HomeActivity.REQUEST_PERMISSION_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    presenter.cameraPermissionChanged(true);
                    presenter.cameraClicked();
                }
                break;
            case HomeActivity.REQUEST_PERMISSION_READ_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    presenter.galleryClicked();
                }
                break;
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
        if (mMenuAdapter == null) {
            Log.e(TAG, "null menu adapter");
            return;
        }
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

    private void updateUserView(final Account account) {
        if (getActivity() == null || account == null) {
            Log.e(TAG, "Not able to update navigation view");
            return;
        }

        mDisposableBag.add(AvatarDrawable.load(getActivity(), account)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(avatar -> mUserImage.setImageDrawable(avatar), e -> Log.e(TAG, "Error loading avatar", e)));
    }

    public void updatePhoto(Uri uriImage) {
        mDisposableBag.add(AndroidFileUtils.loadBitmap(getActivity(), uriImage)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updatePhoto, e -> Log.e(TAG, "Error loading image " + uriImage, e)));
    }

    public void updatePhoto(Bitmap image) {
        if (mSelectedAccount == null)
            return;
        mSourcePhoto = image;
        mDisposableBag.add(VCardServiceImpl.loadProfile(mSelectedAccount)
                .map(profile -> new AvatarDrawable(getContext(), image, profile.first, mSelectedAccount.getRegisteredName(), mSelectedAccount.getUri(), true))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(avatar -> mProfilePhoto.setImageDrawable(avatar), e-> Log.e(TAG, "Error loading image", e)));
    }

    @OnClick(R.id.addaccount_btn)
    public void addNewAccount() {
        startActivity(new Intent(getActivity(), AccountWizardActivity.class));
    }

    private void setupNavigationMenu() {
        mMenuView.setHasFixedSize(true);
        mMenuView.setLayoutManager(new LinearLayoutManager(getActivity()));

        ArrayList<NavigationItem> menu = new ArrayList<>();
        menu.add(0, new NavigationItem(R.string.menu_item_home, R.drawable.baseline_home_24));
        menu.add(1, new NavigationItem(R.string.menu_item_contact_request, R.drawable.baseline_drafts_24));
        menu.add(2, new NavigationItem(R.string.menu_item_accounts, R.drawable.baseline_group_24));
        menu.add(3, new NavigationItem(R.string.menu_item_settings, R.drawable.ic_settings_black));
        menu.add(4, new NavigationItem(R.string.menu_item_about, R.drawable.baseline_info_24));

        mMenuAdapter = new NavigationAdapter(menu);
        mMenuView.setAdapter(mMenuAdapter);
        mMenuAdapter.setOnNavigationItemClickedListener(this);
    }

    public void setNavigationSectionSelectedListener(OnNavigationSectionSelected listener) {
        mSectionListener = listener;
    }

    @OnClick({R.id.profile_container, R.id.user_profile_edit})
    public void profileContainerClicked() {
        if (mSelectedAccount == null)
            return;

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.dialog_profile, null);

        final EditText editText = view.findViewById(R.id.user_name);
        editText.setText(presenter.getAlias(mSelectedAccount));
        mProfilePhoto = view.findViewById(R.id.profile_photo);
        mDisposableBag.add(AvatarDrawable.load(inflater.getContext(), mSelectedAccount)
                .subscribe(a -> mProfilePhoto.setImageDrawable(a)));

        ImageButton cameraView = view.findViewById(R.id.camera);
        cameraView.setOnClickListener(v -> presenter.cameraClicked());

        ImageButton gallery = view.findViewById(R.id.gallery);
        gallery.setOnClickListener(v -> presenter.galleryClicked());

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.profile)
            .setView(view)
            .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                if (mSourcePhoto != null) {
                    presenter.saveVCard(mSelectedAccount, editText.getText().toString().trim(), Single.just(mSourcePhoto).map(BitmapUtils::bitmapToPhoto));
                    mSourcePhoto = null;
                } else {
                    presenter.saveVCardFormattedName(editText.getText().toString().trim());
                }
            })
            .show();
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
                mSelectedAccountError.setImageResource(R.drawable.baseline_sync_disabled_24px);
                mSelectedAccountError.setColorFilter(Color.BLACK);
                mSelectedAccountError.setVisibility(View.VISIBLE);
                mSelectedAccountLoading.setVisibility(View.GONE);
            } else if (selectedAccount.isTrying()) {
                mSelectedAccountError.setVisibility(View.GONE);
                mSelectedAccountLoading.setVisibility(View.VISIBLE);
            } else if (selectedAccount.needsMigration()) {
                mSelectedAccountHost.setText(R.string.account_update_needed);
                mSelectedAccountHost.setTextColor(Color.RED);
                mSelectedAccountError.setImageResource(R.drawable.baseline_warning_24);
                mSelectedAccountError.setColorFilter(Color.RED);
                mSelectedAccountError.setVisibility(View.VISIBLE);
            } else if (selectedAccount.isInError() || !selectedAccount.isRegistered()) {
                mSelectedAccountError.setImageResource(R.drawable.baseline_error_24);
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
        mAccountAdapter.replaceAll(viewModel.getAccounts());
        updateUserView(viewModel.getAccount());
        updateSelectedAccountView(viewModel.getAccount());
        if (viewModel.getAccounts().isEmpty()) {
            mNewAccountBtn.setVisibility(View.VISIBLE);
            mSelectedAccountLayout.setVisibility(View.GONE);
        } else {
            mNewAccountBtn.setVisibility(View.GONE);
            mSelectedAccountLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void updateModel(Account account) {
        mAccountAdapter.replace(account);
        if (mSelectedAccount == account)
            updateSelectedAccountView(account);
    }

    @Override
    public void gotToImageCapture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            File file = AndroidFileUtils.createImageFile(getContext());
            Uri uri = FileProvider.getUriForFile(getContext(), ContentUriHandler.AUTHORITY_FILES, file);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            tmpProfilePhotoUri = uri;
        } catch (IOException e) {
            Log.e(TAG, "Can't create temp file", e);
        }
        startActivityForResult(intent, HomeActivity.REQUEST_CODE_PHOTO);
    }

    @Override
    public void askCameraPermission() {
        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, HomeActivity.REQUEST_PERMISSION_CAMERA);
    }

    @Override
    public void goToGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, HomeActivity.REQUEST_CODE_GALLERY);
    }

    @Override
    public void askGalleryPermission() {
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, HomeActivity.REQUEST_PERMISSION_READ_STORAGE);
    }

    /**
     * Exposed enumeration listing app sections
     */
    public enum Section {
        HOME(0),
        CONTACT_REQUESTS(1),
        MANAGE(2),
        SETTINGS(3),
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
