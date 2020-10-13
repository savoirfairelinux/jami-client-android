/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *          Loïc Siret <loic.siret@savoirfairelinux.com>
 *          Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
package cx.ring.account;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.core.content.FileProvider;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import cx.ring.R;
import cx.ring.about.AboutFragment;
import cx.ring.application.JamiApplication;
import cx.ring.client.HomeActivity;
import cx.ring.contactrequests.BlackListFragment;
import cx.ring.databinding.FragAccSummaryBinding;
import cx.ring.fragments.AdvancedAccountFragment;
import cx.ring.fragments.SettingFragment;
import cx.ring.fragments.LinkNewDeviceFragment;
import cx.ring.fragments.LinkedDevicesFragment;
import cx.ring.fragments.MediaPreferenceFragment;
import cx.ring.fragments.QRCodeFragment;
import cx.ring.model.Account;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.services.AccountService;
import cx.ring.services.VCardServiceImpl;
import cx.ring.settings.AccountFragment;
import cx.ring.settings.GeneralFragment;
import cx.ring.settings.VideoSettingsFragment;
import cx.ring.settings.pluginssettings.PluginsListSettingsFragment;
import cx.ring.utils.AndroidFileUtils;
import cx.ring.utils.BitmapUtils;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.StringUtils;
import cx.ring.views.AvatarDrawable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class JamiAccountSummaryFragment extends BaseSupportFragment<JamiAccountSummaryPresenter> implements
        RegisterNameDialog.RegisterNameDialogListener,
        JamiAccountSummaryView, ChangePasswordDialog.PasswordChangedListener,
        BackupAccountDialog.UnlockAccountListener,
        ViewTreeObserver.OnScrollChangedListener {

    public static final String TAG = JamiAccountSummaryFragment.class.getSimpleName();

    private static final String FRAGMENT_DIALOG_PASSWORD = TAG + ".dialog.changePassword";
    private static final String FRAGMENT_DIALOG_BACKUP = TAG + ".dialog.backup";
    private static final int WRITE_REQUEST_CODE = 43;
    private static final int SCROLL_DIRECTION_UP = -1;
    public static final String ACCOUNT_ID_KEY = AccountEditionFragment.class.getCanonicalName() + "accountid";

    private static final int SETTINGS_ACCOUNT = 0;
    private static final int SETTINGS_MEDIA = 1;
    private static final int SETTINGS_SYSTEM = 2;
    private static final int SETTINGS_ADVANCED = 3;

    private final OnBackPressedCallback mOnBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            if (mBinding.fragment.getVisibility() == View.VISIBLE) {
                mBinding.fragment.setVisibility(View.GONE);
                mOnBackPressedCallback.setEnabled(false);
                getChildFragmentManager().popBackStack();
            }
        }
    };

    private boolean mIsVisible = true;
    private ProgressDialog mWaitDialog;
    private boolean mAccountHasPassword = true;
    private String mBestName = "";
    private String mAccountId = "";
    private File mCacheArchive = null;
    private ImageView mProfilePhoto;
    private Bitmap mSourcePhoto;
    private Uri tmpProfilePhotoUri;

    private final CompositeDisposable mDisposableBag = new CompositeDisposable();
    private final CompositeDisposable mProfileDisposable = new CompositeDisposable();

    @Inject
    AccountService mAccountService;

    private FragAccSummaryBinding mBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragAccSummaryBinding.inflate(inflater, container, false);
        ((JamiApplication) getActivity().getApplication()).getInjectionComponent().inject(this);
        mDisposableBag.add(mProfileDisposable);
        return mBinding.getRoot();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        requireActivity().getOnBackPressedDispatcher().addCallback(this, mOnBackPressedCallback);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mDisposableBag.clear();
        mBinding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDisposableBag.dispose();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            mAccountId = getArguments().getString(AccountEditionFragment.ACCOUNT_ID_KEY);
            if (mAccountId != null) {
                presenter.setAccountId(mAccountId);
            }
        }

        updateUserView(mAccountService.getCurrentAccount());
        mBinding.scrollview.getViewTreeObserver().addOnScrollChangedListener(this);
        mBinding.linkNewDevice.setOnClickListener(v -> showWizard(mAccountId));
        mBinding.linkedDevices.setRightDrawableOnClickListener(v ->
                changeFragment(LinkedDevicesFragment.newInstance(mAccountId), LinkedDevicesFragment.TAG));
        mBinding.username.setLeftDrawableOnClickListener(v -> profileContainerClicked());
        mBinding.userPhoto.setOnClickListener(v -> profileContainerClicked());
        mBinding.accountSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> presenter.enableAccount(isChecked));
        mBinding.registerName.setOnClickListener(v -> showUsernameRegistrationPopup());

        List<SettingItem> items = new ArrayList<>();

        SettingItem account = new SettingItem(R.string.account, R.drawable.baseline_account_card_details);
        SettingItem media = new SettingItem(R.string.account_preferences_media_tab, R.drawable.outline_file_copy_24);
        SettingItem system = new SettingItem(R.string.notif_channel_messages, R.drawable.baseline_chat_24);
        SettingItem advanced = new SettingItem(R.string.account_preferences_advanced_tab, R.drawable.round_check_circle_24);

        items.add(account);
        items.add(media);
        items.add(system);
        items.add(advanced);

        SettingsAdapter adapter = new SettingsAdapter(getContext(), R.layout.item_setting, items);
        mBinding.settingsList.setAdapter(adapter);
        mBinding.settingsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i) {
                    case SETTINGS_ACCOUNT:
                        presenter.goToAccount();
                        break;
                    case SETTINGS_MEDIA:
                        presenter.goToMedia();
                        break;
                    case SETTINGS_SYSTEM:
                        presenter.goToSystem();
                        break;
                    case SETTINGS_ADVANCED:
                        presenter.goToAdvanced();
                        break;
                }
            }
        });

        int totalHeight = 0;
        for (int i = 0; i < adapter.getCount(); i++) {
            View listItem = adapter.getView(i, null, mBinding.settingsList);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams par = mBinding.settingsList.getLayoutParams();
        par.height = totalHeight + (mBinding.settingsList.getDividerHeight() * (adapter.getCount() - 1));
        mBinding.settingsList.setLayoutParams(par);
        mBinding.settingsList.requestLayout();
    }

    public void setAccount(String accountId) {
        if (presenter != null)
            presenter.setAccountId(accountId);
    }

    @Override
    public void updateUserView(Account account) {
        Context context = getContext();
        if (context == null || account == null)
            return;

        mProfileDisposable.clear();
        mProfileDisposable.add(AvatarDrawable.load(context, account)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(avatar -> {
                    mBinding.userPhoto.setImageDrawable(avatar);
                    mBinding.username.setText(account.getLoadedProfile().blockingGet().first);
                }, e -> Log.e(TAG, "Error loading avatar", e)));
    }

    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        switch (requestCode) {
            case WRITE_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    if (resultData != null) {
                        Uri uri = resultData.getData();
                        if (uri != null) {
                            if (mCacheArchive != null) {
                                AndroidFileUtils.moveToUri(requireContext().getContentResolver(), mCacheArchive, uri)
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(() -> {
                                        }, e -> {
                                            View v = getView();
                                            if (v != null)
                                                Snackbar.make(v, "Can't export archive: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                                        });
                            }
                        }
                    }
                }
            break;
            case HomeActivity.REQUEST_CODE_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    if (tmpProfilePhotoUri == null) {
                        if (resultData != null)
                            updatePhoto((Bitmap) resultData.getExtras().get("data"));
                    } else {
                        updatePhoto(tmpProfilePhotoUri);
                    }
                }
                tmpProfilePhotoUri = null;
                break;
            case HomeActivity.REQUEST_CODE_GALLERY:
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    updatePhoto(resultData.getData());
                }
                break;
        }
    }

    @Override
    public void accountChanged(@NonNull final Account account) {
        updateUserView(account);
        mBinding.linkedDevices.setText(getString(R.string.total_devices) + account.getDevices().size());

        mAccountHasPassword = account.hasPassword();

        mBinding.accountSwitch.setChecked(account.isEnabled());
        mBinding.accountAliasTxt.setText(getString(R.string.profile));
        mBinding.identity.setText(account.getUsername());
        mAccountId = account.getAccountID();
        mBestName = account.getRegisteredName();
        if (mBestName.isEmpty()) {
            mBestName = account.getDisplayUsername();
            if (mBestName.isEmpty()) {
                mBestName = account.getUsername();
            }
        }
        mBestName = mBestName + ".gz";
        String username = account.getRegisteredName();
        boolean currentRegisteredName = account.registeringUsername;
        boolean hasRegisteredName = !currentRegisteredName && username != null && !username.isEmpty();
        mBinding.groupRegisteringName.setVisibility(currentRegisteredName ? View.VISIBLE : View.GONE);
        mBinding.username.setRightDrawableOnClickListener(v -> shareAccount(hasRegisteredName? username : account.getUsername()));
        mBinding.registerName.setVisibility(hasRegisteredName? View.GONE : View.VISIBLE);
        mBinding.registeredName.setText(hasRegisteredName? username : getResources().getString(R.string.no_registered_name_for_account));
        mBinding.identity.setRightDrawableOnClickListener(v -> QRCodeFragment.newInstance(QRCodeFragment.INDEX_CODE).show(getParentFragmentManager(), QRCodeFragment.TAG));
        mBinding.username.getEditText().setOnFocusChangeListener((v, hasFocus) -> {
            String name = mBinding.username.getText();
            if (!hasFocus && !TextUtils.isEmpty(name)) {
                presenter.saveVCardFormattedName(name.trim());
            }
        });

        int color = R.color.red_400;
        String status;

        if (account.isEnabled()) {
            if (account.isTrying()) {
                color = R.color.orange_400;
                status = getString(R.string.account_status_connecting);
            } else if (account.needsMigration()) {
                status = getString(R.string.account_update_needed);
            } else if (account.isInError()) {
                status = getString(R.string.account_status_connection_error);
            } else if (account.isRegistered()) {
                status = getString(R.string.account_status_online);
                color = R.color.green_400;
            } else if (!account.isRegistered()){
                color = R.color.grey_400;
                status = getString(R.string.account_status_offline);
            } else {
                status = getString(R.string.account_status_error);
            }
        } else {
            color = R.color.grey_400;
            status = getString(R.string.account_status_offline);
        }

        mBinding.accountStatus.setText(status);
        mBinding.accountStatus.setChipBackgroundColorResource(color);
    }

    public boolean onBackPressed() {
        return false;
    }

    private void showWizard(String accountId) {
        LinkNewDeviceFragment.newInstance(accountId).show(getParentFragmentManager(), LinkNewDeviceFragment.TAG);
    }

    @Override
    public void showNetworkError() {
        dismissWaitDialog();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.account_export_end_network_title)
                .setMessage(R.string.account_export_end_network_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @Override
    public void showPasswordError() {
        dismissWaitDialog();
    }

    @Override
    public void showGenericError() {
        dismissWaitDialog();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.account_export_end_error_title)
                .setMessage(R.string.account_export_end_error_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @Override
    public void showPIN(String pin) {

    }

    private void profileContainerClicked() {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.dialog_profile, null);

        mProfilePhoto = view.findViewById(R.id.profile_photo);
        mDisposableBag.add(AvatarDrawable.load(inflater.getContext(), mAccountService.getCurrentAccount())
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
                        presenter.saveVCard(mBinding.username.getText().trim(), Single.just(mSourcePhoto).map(BitmapUtils::bitmapToPhoto));
                        mSourcePhoto = null;
                    }
                })
                .show();
    }

    public void onClickExport() {
        if (mAccountHasPassword) {
            onBackupAccount();
        } else {
            onUnlockAccount(mAccountId, "");
        }
    }

    private void showUsernameRegistrationPopup() {
        Bundle args = new Bundle();
        args.putString(AccountEditionFragment.ACCOUNT_ID_KEY, getArguments().getString(AccountEditionFragment.ACCOUNT_ID_KEY));
        args.putBoolean(AccountEditionFragment.ACCOUNT_HAS_PASSWORD_KEY, mAccountHasPassword);
        RegisterNameDialog registrationDialog = new RegisterNameDialog();
        registrationDialog.setArguments(args);
        registrationDialog.setListener(this);
        registrationDialog.show(requireFragmentManager(), TAG);
    }

    @Override
    public void onRegisterName(String name, String password) {
        presenter.registerName(name, password);
    }

    @Override
    public void showExportingProgressDialog() {
        mWaitDialog = ProgressDialog.show(getActivity(),
                getString(R.string.export_account_wait_title),
                getString(R.string.export_account_wait_message));
    }

    @Override
    public void showPasswordProgressDialog() {
        mWaitDialog = ProgressDialog.show(getActivity(),
                getString(R.string.export_account_wait_title),
                getString(R.string.account_password_change_wait_message));
    }

    private void dismissWaitDialog() {
        if (mWaitDialog != null) {
            mWaitDialog.dismiss();
            mWaitDialog = null;
        }
    }

    @Override
    public void passwordChangeEnded(boolean ok) {
        dismissWaitDialog();
        if (!ok) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.account_device_revocation_wrong_password)
                    .setMessage(R.string.account_export_end_decryption_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    private void createFile(String mimeType, String fileName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, WRITE_REQUEST_CODE);
    }

    @Override
    public void displayCompleteArchive(File dest)  {
        String type = AndroidFileUtils.getMimeType(dest.getAbsolutePath());
        mCacheArchive = dest;
        dismissWaitDialog();
        createFile(type, mBestName);
    }

    private void onBackupAccount() {
        BackupAccountDialog dialog = new BackupAccountDialog();
        Bundle args = new Bundle();
        args.putString(AccountEditionFragment.ACCOUNT_ID_KEY, getArguments().getString(AccountEditionFragment.ACCOUNT_ID_KEY));
        dialog.setArguments(args);
        dialog.setListener(this);
        dialog.show(requireFragmentManager(), FRAGMENT_DIALOG_BACKUP);
    }

    public void onPasswordChangeAsked() {
        ChangePasswordDialog dialog = new ChangePasswordDialog();
        Bundle args = new Bundle();
        args.putString(AccountEditionFragment.ACCOUNT_ID_KEY, getArguments().getString(AccountEditionFragment.ACCOUNT_ID_KEY));
        args.putBoolean(AccountEditionFragment.ACCOUNT_HAS_PASSWORD_KEY, mAccountHasPassword);
        dialog.setArguments(args);
        dialog.setListener(this);
        dialog.show(requireFragmentManager(), FRAGMENT_DIALOG_PASSWORD);
    }

    @Override
    public void onPasswordChanged(String oldPassword, String newPassword) {
        presenter.changePassword(oldPassword, newPassword);
    }

    @Override
    public void onUnlockAccount(String accountId, String password) {
        Context context = requireContext();
        File cacheDir = new File(AndroidFileUtils.getTempShareDir(context), "archives");
        cacheDir.mkdirs();
        if (!cacheDir.canWrite())
            Log.w(TAG, "Can't write to: " + cacheDir);
        File dest = new File(cacheDir, mBestName);
        if (dest.exists())
            dest.delete();
        presenter.downloadAccountsArchive(dest, password);
    }

    @Override
    public void gotToImageCapture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            Context context = requireContext();
            File file = AndroidFileUtils.createImageFile(context);
            Uri uri = FileProvider.getUriForFile(context, ContentUriHandler.AUTHORITY_FILES, file);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    .putExtra("android.intent.extras.CAMERA_FACING", 1)
                    .putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                    .putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
            tmpProfilePhotoUri = uri;
            startActivityForResult(intent, HomeActivity.REQUEST_CODE_PHOTO);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error starting camera: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Can't create temp file", e);
        }
    }

    @Override
    public void askCameraPermission() {
        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, HomeActivity.REQUEST_PERMISSION_CAMERA);
    }

    @Override
    public void goToGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, HomeActivity.REQUEST_CODE_GALLERY);
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.gallery_error_message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void askGalleryPermission() {
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, HomeActivity.REQUEST_PERMISSION_READ_STORAGE);
    }

    private void updatePhoto(Uri uriImage) {
        mDisposableBag.add(AndroidFileUtils.loadBitmap(getActivity(), uriImage)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updatePhoto, e -> Log.e(TAG, "Error loading image " + uriImage, e)));
    }

    private void updatePhoto(Bitmap image) {
        mSourcePhoto = image;
        AvatarDrawable avatarDrawable = new AvatarDrawable.Builder()
                        .withPhoto(image)
                        .withNameData(null, mAccountService.getCurrentAccount().getRegisteredName())
                        .withId(mAccountService.getCurrentAccount().getUri())
                        .withCircleCrop(true)
                        .build(getContext());
        mDisposableBag.add(VCardServiceImpl.loadProfile(mAccountService.getCurrentAccount())
                .map(profile -> avatarDrawable)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(avatar -> mProfilePhoto.setImageDrawable(avatar), e-> Log.e(TAG, "Error loading image", e)));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case HomeActivity.REQUEST_PERMISSION_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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

    @Override
    public void onScrollChanged() {
        if (mIsVisible && mBinding != null) {
            Activity activity = getActivity();
            if (activity instanceof HomeActivity)
                ((HomeActivity) activity).setToolbarElevation(mBinding.scrollview.canScrollVertically(SCROLL_DIRECTION_UP));
        }
    }

    public void setFragmentVisibility(boolean isVisible) {
        mIsVisible = isVisible;
    }

    private void shareAccount(String username) {
        if (!StringUtils.isEmpty(username)) {
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getText(R.string.account_contact_me));
            sharingIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.account_share_body, username, getText(R.string.app_website)));
            startActivity(Intent.createChooser(sharingIntent, getText(R.string.share_via)));
        }
    }

    private Fragment fragmentWithBundle(Fragment result, String accountId) {
        Bundle args = new Bundle();
        args.putString(ACCOUNT_ID_KEY, accountId);
        result.setArguments(args);
        return result;
    }

    private void changeFragment(Fragment fragment, String tag) {
        getChildFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.fragment, fragment, tag)
                .addToBackStack(tag).commit();
        mBinding.fragment.setVisibility(View.VISIBLE);
        mOnBackPressedCallback.setEnabled(true);
    }

    public void goToAccount(String accountId) {
        changeFragment(AccountFragment.newInstance(accountId), MediaPreferenceFragment.TAG);
    }

    public void goToMedia(String accountId) {
        changeFragment(MediaPreferenceFragment.newInstance(accountId), MediaPreferenceFragment.TAG);
    }

    public void goToSystem(String accountId) {
        changeFragment(SettingFragment.newInstance(accountId), SettingFragment.TAG);
    }

    public void goToAdvanced(String accountId) {
        changeFragment(fragmentWithBundle(new AdvancedAccountFragment(), accountId), GeneralFragment.TAG);
    }

    public void goToBlackList(String accountId) {
        BlackListFragment blackListFragment = new BlackListFragment();
        Bundle args = new Bundle();
        args.putString(ACCOUNT_ID_KEY, accountId);
        blackListFragment.setArguments(args);
        changeFragment(blackListFragment, BlackListFragment.TAG);
    }

    public void popBackStack() {
        getChildFragmentManager().popBackStackImmediate();
        String fragmentTag = getChildFragmentManager().getBackStackEntryAt(getChildFragmentManager().getBackStackEntryCount() - 1).getName();
        Fragment fragment = getChildFragmentManager().findFragmentByTag(fragmentTag);
        changeFragment(fragment, fragmentTag);
    }

}
