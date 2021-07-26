/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import net.jami.account.JamiAccountSummaryPresenter;
import net.jami.account.JamiAccountSummaryView;
import net.jami.model.Account;
import net.jami.utils.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.client.HomeActivity;
import cx.ring.contactrequests.BlockListFragment;
import cx.ring.databinding.FragAccSummaryBinding;
import cx.ring.fragments.AdvancedAccountFragment;
import cx.ring.fragments.GeneralAccountFragment;
import cx.ring.fragments.LinkDeviceFragment;
import cx.ring.fragments.MediaPreferenceFragment;
import cx.ring.fragments.QRCodeFragment;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.settings.AccountFragment;
import cx.ring.settings.SettingsFragment;
import cx.ring.utils.AndroidFileUtils;
import cx.ring.utils.BitmapUtils;
import cx.ring.utils.ContentUriHandler;
import cx.ring.views.AvatarDrawable;
import cx.ring.views.SwitchButton;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@AndroidEntryPoint
public class JamiAccountSummaryFragment extends BaseSupportFragment<JamiAccountSummaryPresenter, JamiAccountSummaryView> implements
        RegisterNameDialog.RegisterNameDialogListener,
        JamiAccountSummaryView, ChangePasswordDialog.PasswordChangedListener,
        BackupAccountDialog.UnlockAccountListener,
        ViewTreeObserver.OnScrollChangedListener,
        RenameDeviceDialog.RenameDeviceListener,
        DeviceAdapter.DeviceRevocationListener,
        ConfirmRevocationDialog.ConfirmRevocationListener {

    public static final String TAG = JamiAccountSummaryFragment.class.getSimpleName();
    private static final String FRAGMENT_DIALOG_REVOCATION = TAG + ".dialog.deviceRevocation";
    private static final String FRAGMENT_DIALOG_RENAME = TAG + ".dialog.deviceRename";

    private static final String FRAGMENT_DIALOG_PASSWORD = TAG + ".dialog.changePassword";
    private static final String FRAGMENT_DIALOG_BACKUP = TAG + ".dialog.backup";
    private static final int WRITE_REQUEST_CODE = 43;
    private static final int SCROLL_DIRECTION_UP = -1;

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

    private ProgressDialog mWaitDialog;
    private boolean mAccountHasPassword = true;
    private String mBestName = "";
    private String mAccountId = "";
    private File mCacheArchive = null;
    private ImageView mProfilePhoto;
    private Bitmap mSourcePhoto;
    private Uri tmpProfilePhotoUri;
    private DeviceAdapter mDeviceAdapter;

    private final CompositeDisposable mDisposableBag = new CompositeDisposable();
    private final CompositeDisposable mProfileDisposable = new CompositeDisposable();
    private FragAccSummaryBinding mBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragAccSummaryBinding.inflate(inflater, container, false);
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

        mBinding.scrollview.getViewTreeObserver().addOnScrollChangedListener(this);
        mBinding.linkNewDevice.setOnClickListener(v -> showWizard(mAccountId));
        mBinding.linkedDevices.setRightDrawableOnClickListener(v -> onDeviceRename());
        mBinding.registerName.setOnClickListener(v -> showUsernameRegistrationPopup());

        List<SettingItem> items = new ArrayList<>(4);
        items.add(new SettingItem(R.string.account, R.drawable.baseline_account_card_details, () -> presenter.goToAccount()));
        items.add(new SettingItem(R.string.account_preferences_media_tab, R.drawable.outline_file_copy_24, () -> presenter.goToMedia()));
        items.add(new SettingItem(R.string.notif_channel_messages, R.drawable.baseline_chat_24, () -> presenter.goToSystem()));
        items.add(new SettingItem(R.string.account_preferences_advanced_tab, R.drawable.round_check_circle_24, () -> presenter.goToAdvanced()));

        SettingsAdapter adapter = new SettingsAdapter(view.getContext(), R.layout.item_setting, items);
        mBinding.settingsList.setOnItemClickListener((adapterView, v, i, l) -> adapter.getItem(i).onClick());
        mBinding.settingsList.setAdapter(adapter);

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

        mBinding.chipMore.setOnClickListener(v -> {
            if (mBinding.devicesList.getVisibility() == View.GONE) {
                expand(mBinding.devicesList);
            } else {
                collapse(mBinding.devicesList);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        ((HomeActivity) requireActivity()).showAccountStatus(true);
        ((HomeActivity) requireActivity()).getSwitchButton().setOnCheckedChangeListener((buttonView, isChecked) -> presenter.enableAccount(isChecked));
    }

    @Override
    public void onPause() {
        super.onPause();
        ((HomeActivity) requireActivity()).showAccountStatus(false);
        ((HomeActivity) requireActivity()).getSwitchButton().setOnCheckedChangeListener(null);
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
                    if (mBinding != null) {
                        mBinding.userPhoto.setImageDrawable(avatar);
                        mBinding.username.setText(account.getLoadedProfile().blockingGet().first);
                    }
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
                                        .subscribe(() -> {}, e -> {
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
                            updatePhoto(Single.just((Bitmap) resultData.getExtras().get("data")));
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
        mBinding.userPhoto.setOnClickListener(v -> profileContainerClicked(account));
        mBinding.linkedDevices.setText(account.getDeviceName());
        setLinkedDevicesAdapter(account);
        mAccountHasPassword = account.hasPassword();

        ((HomeActivity) requireActivity()).getSwitchButton().setCheckedSilent(account.isEnabled());
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
        mBinding.btnShare.setOnClickListener(v -> shareAccount(hasRegisteredName? username : account.getUsername()));
        mBinding.registerName.setVisibility(hasRegisteredName ? View.GONE : View.VISIBLE);
        mBinding.registeredName.setText(hasRegisteredName ? username : getResources().getString(R.string.no_registered_name_for_account));
        mBinding.btnQr.setOnClickListener(v -> QRCodeFragment.newInstance(QRCodeFragment.INDEX_CODE).show(getParentFragmentManager(), QRCodeFragment.TAG));
        mBinding.username.setOnFocusChangeListener((v, hasFocus) -> {
            Editable name = mBinding.username.getText();
            if (!hasFocus && !TextUtils.isEmpty(name)) {
                presenter.saveVCardFormattedName(name.toString());
            }
        });

        setSwitchStatus(account);
    }

    public boolean onBackPressed() {
        return false;
    }

    private void showWizard(String accountId) {
        LinkDeviceFragment.newInstance(accountId).show(getParentFragmentManager(), LinkDeviceFragment.TAG);
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

    private void profileContainerClicked(Account account) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());

        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.dialog_profile, null);
        mProfilePhoto = view.findViewById(R.id.profile_photo);
        mDisposableBag.add(AvatarDrawable.load(inflater.getContext(), account)
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
                        presenter.saveVCard(mBinding.username.getText().toString(), Single.just(mSourcePhoto).map(BitmapUtils::bitmapToPhoto));
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
        args.putString(AccountEditionFragment.ACCOUNT_ID_KEY, mAccountId);
        args.putBoolean(AccountEditionFragment.ACCOUNT_HAS_PASSWORD_KEY, mAccountHasPassword);
        RegisterNameDialog registrationDialog = new RegisterNameDialog();
        registrationDialog.setArguments(args);
        registrationDialog.setListener(this);
        registrationDialog.show(getParentFragmentManager(), TAG);
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
        args.putString(AccountEditionFragment.ACCOUNT_ID_KEY, mAccountId);
        dialog.setArguments(args);
        dialog.setListener(this);
        dialog.show(getParentFragmentManager(), FRAGMENT_DIALOG_BACKUP);
    }

    public void onPasswordChangeAsked() {
        ChangePasswordDialog dialog = new ChangePasswordDialog();
        Bundle args = new Bundle();
        args.putString(AccountEditionFragment.ACCOUNT_ID_KEY, mAccountId);
        args.putBoolean(AccountEditionFragment.ACCOUNT_HAS_PASSWORD_KEY, mAccountHasPassword);
        dialog.setArguments(args);
        dialog.setListener(this);
        dialog.show(getParentFragmentManager(), FRAGMENT_DIALOG_PASSWORD);
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
        updatePhoto(AndroidFileUtils.loadBitmap(requireContext(), uriImage));
    }

    private void updatePhoto(Single<Bitmap> image) {
        Account account = presenter.getAccount();
        if (account == null)
            return;
        mDisposableBag.add(image.subscribeOn(Schedulers.io())
                .map(img -> {
                    mSourcePhoto = img;
                    return new AvatarDrawable.Builder()
                            .withPhoto(img)
                            .withNameData(null, account.getRegisteredName())
                            .withId(account.getUri())
                            .withCircleCrop(true)
                            .build(getContext());
                })
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
        if (mBinding != null) {
            Activity activity = getActivity();
            if (activity instanceof HomeActivity)
                ((HomeActivity) activity).setToolbarElevation(mBinding.scrollview.canScrollVertically(SCROLL_DIRECTION_UP));
        }
    }

    @Override
    public void setSwitchStatus(Account account) {
        SwitchButton switchButton = ((HomeActivity) requireActivity()).getSwitchButton();
        int color = R.color.red_400;
        String status;

        if (account.isEnabled()) {
            if (account.isTrying()) {
                color = R.color.orange_400;
                switchButton.showImage(true);
                switchButton.startImageAnimation();
            } else if (account.needsMigration()) {
                status = getString(R.string.account_update_needed);
                switchButton.showImage(false);
                switchButton.setStatus(status);
            } else if (account.isInError()) {
                status = getString(R.string.account_status_connection_error);
                switchButton.showImage(false);
                switchButton.setStatus(status);
            } else if (account.isRegistered()) {
                status = getString(R.string.account_status_online);
                color = R.color.green_400;
                switchButton.showImage(false);
                switchButton.setStatus(status);
            } else if (!account.isRegistered()){
                color = R.color.grey_400;
                status = getString(R.string.account_status_offline);
                switchButton.showImage(false);
                switchButton.setStatus(status);
            } else {
                status = getString(R.string.account_status_error);
                switchButton.showImage(false);
                switchButton.setStatus(status);
            }
        } else {
            color = R.color.grey_400;
            status = getString(R.string.account_status_offline);
            switchButton.showImage(false);
            switchButton.setStatus(status);
        }

        switchButton.setBackColor(ContextCompat.getColor(requireContext(), color));
    }

    @Override
    public void showRevokingProgressDialog() {
        mWaitDialog = ProgressDialog.show(getActivity(),
                getString(R.string.revoke_device_wait_title),
                getString(R.string.revoke_device_wait_message));
    }

    @Override
    public void deviceRevocationEnded(final String device, final int status) {
        dismissWaitDialog();
        int message, title = R.string.account_device_revocation_error_title;
        switch (status) {
            case 0:
                title = R.string.account_device_revocation_success_title;
                message = R.string.account_device_revocation_success;
                break;
            case 1:
                message = R.string.account_device_revocation_wrong_password;
                break;
            case 2:
                message = R.string.account_device_revocation_unknown_device;
                break;
            default:
                message = R.string.account_device_revocation_error_unknown;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                    if (status == 1) {
                        onDeviceRevocationAsked(device);
                    }
                })
                .show();
    }

    @Override
    public void updateDeviceList(final Map<String, String> devices, final String currentDeviceId) {
        if (mDeviceAdapter == null) {
            return;
        }
        mDeviceAdapter.setData(devices, currentDeviceId);
        collapse(mBinding.devicesList);
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
        args.putString(AccountEditionFragment.ACCOUNT_ID_KEY, accountId);
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
        changeFragment(AccountFragment.newInstance(accountId), AccountFragment.TAG);
    }

    public void goToMedia(String accountId) {
        changeFragment(MediaPreferenceFragment.newInstance(accountId), MediaPreferenceFragment.TAG);
    }

    public void goToSystem(String accountId) {
        changeFragment(GeneralAccountFragment.newInstance(accountId), GeneralAccountFragment.TAG);
    }

    public void goToAdvanced(String accountId) {
        changeFragment(fragmentWithBundle(new AdvancedAccountFragment(), accountId), AdvancedAccountFragment.TAG);
    }

    public void goToBlackList(String accountId) {
        BlockListFragment blockListFragment = new BlockListFragment();
        Bundle args = new Bundle();
        args.putString(AccountEditionFragment.ACCOUNT_ID_KEY, accountId);
        blockListFragment.setArguments(args);
        changeFragment(blockListFragment, BlockListFragment.Companion.getTAG());
    }

    public void popBackStack() {
        getChildFragmentManager().popBackStackImmediate();
        String fragmentTag = getChildFragmentManager().getBackStackEntryAt(getChildFragmentManager().getBackStackEntryCount() - 1).getName();
        Fragment fragment = getChildFragmentManager().findFragmentByTag(fragmentTag);
        changeFragment(fragment, fragmentTag);
    }

    @Override
    public void onConfirmRevocation(String deviceId, String password) {
        presenter.revokeDevice(deviceId, password);
    }

    @Override
    public void onDeviceRevocationAsked(String deviceId) {
        ConfirmRevocationDialog dialog = new ConfirmRevocationDialog();
        Bundle args = new Bundle();
        args.putString(ConfirmRevocationDialog.DEVICEID_KEY, deviceId);
        args.putBoolean(ConfirmRevocationDialog.HAS_PASSWORD_KEY, mAccountHasPassword);
        dialog.setArguments(args);
        dialog.setListener(this);
        dialog.show(getParentFragmentManager(), FRAGMENT_DIALOG_REVOCATION);
    }

    @Override
    public void onDeviceRename() {
        final String dev_name = presenter.getDeviceName();
        RenameDeviceDialog dialog = new RenameDeviceDialog();
        Bundle args = new Bundle();
        args.putString(RenameDeviceDialog.DEVICENAME_KEY, dev_name);
        dialog.setArguments(args);
        dialog.setListener(this);
        dialog.show(getParentFragmentManager(), FRAGMENT_DIALOG_RENAME);
    }

    @Override
    public void onDeviceRename(String newName) {
        Log.d(TAG, "onDeviceRename: " + presenter.getDeviceName() + " -> " + newName);
        presenter.renameDevice(newName);
    }

    private void expand(View summary) {
        summary.setVisibility(View.VISIBLE);

        final int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        summary.measure(View.MeasureSpec.makeMeasureSpec(widthSpec, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.AT_MOST));
        final int targetHeight = summary.getMeasuredHeight();

        ValueAnimator animator = slideAnimator(0, targetHeight, summary);
        animator.start();

        mBinding.chipMore.setText(R.string.account_link_hide_button);
    }

    private void collapse(final View summary) {
        int finalHeight = summary.getHeight();
        ValueAnimator mAnimator = slideAnimator(finalHeight, 0, summary);
        mAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animator) {
                // Height=0, but it set visibility to GONE
                summary.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });

        mAnimator.start();
        mBinding.chipMore.setText(getString(R.string.account_link_show_button, mDeviceAdapter.getCount()));
    }

    private static ValueAnimator slideAnimator(int start, int end, final View summary) {
        ValueAnimator animator = ValueAnimator.ofInt(start, end);
        animator.addUpdateListener(valueAnimator -> {
            // Update Height
            int value = (Integer) valueAnimator.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = summary.getLayoutParams();
            layoutParams.height = value;
            summary.setLayoutParams(layoutParams);
        });
        return animator;
    }

    private void setLinkedDevicesAdapter(Account account) {
        if (account.getDevices().size() == 1) {
            mBinding.chipMore.setVisibility(View.GONE);
        } else {
            mBinding.chipMore.setVisibility(View.VISIBLE);
            if (mDeviceAdapter == null) {
                mDeviceAdapter = new DeviceAdapter(requireContext(), account.getDevices(), account.getDeviceId(), JamiAccountSummaryFragment.this);
                mBinding.chipMore.setText(getString(R.string.account_link_show_button, mDeviceAdapter.getCount()));
                mBinding.devicesList.setAdapter(mDeviceAdapter);
            } else {
                mDeviceAdapter.setData(account.getDevices(), account.getDeviceId());
            }
        }
    }

}
