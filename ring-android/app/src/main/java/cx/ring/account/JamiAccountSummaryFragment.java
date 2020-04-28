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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.provider.MediaStore;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Map;

import javax.inject.Inject;

import androidx.core.content.FileProvider;
import androidx.core.util.Pair;

import cx.ring.R;
import cx.ring.application.JamiApplication;
import cx.ring.client.HomeActivity;
import cx.ring.databinding.FragAccSummaryBinding;
import cx.ring.model.Account;
import cx.ring.mvp.BaseSupportFragment;
import cx.ring.services.AccountService;
import cx.ring.services.VCardServiceImpl;
import cx.ring.utils.AndroidFileUtils;
import cx.ring.utils.BitmapUtils;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.KeyboardVisibilityManager;
import cx.ring.views.AvatarDrawable;
import ezvcard.VCard;
import ezvcard.property.FormattedName;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class JamiAccountSummaryFragment extends BaseSupportFragment<JamiAccountSummaryPresenter> implements
        RegisterNameDialog.RegisterNameDialogListener,
        RenameDeviceDialog.RenameDeviceListener,
        DeviceAdapter.DeviceRevocationListener,
        ConfirmRevocationDialog.ConfirmRevocationListener,
        JamiAccountSummaryView, ChangePasswordDialog.PasswordChangedListener,
        BackupAccountDialog.UnlockAccountListener,
        ViewTreeObserver.OnScrollChangedListener {

    public static final String TAG = JamiAccountSummaryFragment.class.getSimpleName();
    private static final String FRAGMENT_DIALOG_REVOCATION = TAG + ".dialog.deviceRevocation";
    private static final String FRAGMENT_DIALOG_RENAME = TAG + ".dialog.deviceRename";
    private static final String FRAGMENT_DIALOG_PASSWORD = TAG + ".dialog.changePassword";
    private static final String FRAGMENT_DIALOG_BACKUP = TAG + ".dialog.backup";
    private static final int WRITE_REQUEST_CODE = 43;
    private static final int SCROLL_DIRECTION_UP = -1;

    private boolean mIsVisible = true;
    private DeviceAdapter mDeviceAdapter;
    private ProgressDialog mWaitDialog;
    private boolean mAccountHasPassword = true;
    private boolean mAccountHasManager = true;
    private String mBestName = "";
    private String mAccountId = "";
    private File mCacheArchive = null;
    private BottomSheetBehavior mSheetBehavior;

    private ImageView mProfilePhoto;
    private Bitmap mSourcePhoto;
    private Uri tmpProfilePhotoUri;

    private final CompositeDisposable mDisposableBag = new CompositeDisposable();
    private final CompositeDisposable mProfileDisposable = new CompositeDisposable();

    @Inject
    AccountService mAccountService;

    private FragAccSummaryBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragAccSummaryBinding.inflate(inflater, container, false);
        ((JamiApplication) getActivity().getApplication()).getInjectionComponent().inject(this);
        mDisposableBag.add(mProfileDisposable);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mDisposableBag.clear();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDisposableBag.dispose();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSheetBehavior = BottomSheetBehavior.from(binding.layoutAddDevice);

        hidePopWizard();
        if (getArguments() != null) {
            String accountId = getArguments().getString(AccountEditionFragment.ACCOUNT_ID_KEY);
            if (accountId != null) {
                presenter.setAccountId(accountId);
            }
        }

        mSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int i) {
                if (binding != null && mSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                    binding.passwordLayout.setVisibility(mAccountHasPassword ? View.VISIBLE : View.GONE);
                    binding.btnEndExport.setVisibility(View.GONE);
                    binding.btnStartExport.setVisibility(View.VISIBLE);
                    binding.accountLinkInfo.setText(R.string.account_link_export_info);
                }
            }

            @Override
            public void onSlide(@NonNull View view, float v) {}
        });

        updateUserView(mAccountService.getCurrentAccount());
        binding.scrollview.getViewTreeObserver().addOnScrollChangedListener(this);
        binding.btnAddDevice.setOnClickListener(v -> flipForm());
        binding.btnStartExport.setOnClickListener(v -> onClickStart());
        binding.btnEndExport.setOnClickListener(v -> hidePopWizard());
        binding.exportAccountBtn.setOnClickListener(v -> onClickExport());
        binding.profileContainer.setOnClickListener(v -> profileContainerClicked());
        binding.userProfileEdit.setOnClickListener(v -> profileContainerClicked());
        binding.accountSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> presenter.enableAccount(isChecked));
        binding.changePasswordBtn.setOnClickListener(v -> onPasswordChangeAsked());
        binding.registerNameBtn.setOnClickListener(v -> showUsernameRegistrationPopup());
        binding.ringPassword.setOnEditorActionListener(this::onPasswordEditorAction);
        binding.registeredNameCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("Jami username", binding.registeredNameTxt.getText()));
                Snackbar.make(binding.getRoot(), getString(R.string.conversation_action_copied_peer_number_clipboard, binding.registeredNameTxt.getText()), Snackbar.LENGTH_SHORT)
                        .setAnchorView(binding.layoutAddDevice)
                        .show();
            }
        });
        binding.registeredNameShare.setOnClickListener(v ->  {
            Intent sendIntent = new Intent(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_TEXT, binding.registeredNameTxt.getText())
                    .setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, null));
        });
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
                .map(avatar -> new Pair<>(account, avatar))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(d -> {
                    binding.username.setText(getAccountAlias(d.first));
                    binding.subtitle.setText(getUri(d.first,  getString(R.string.account_type_ip2ip)));
                    binding.userPhoto.setImageDrawable(d.second);
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
        if (mDeviceAdapter == null) {
            mDeviceAdapter = new DeviceAdapter(requireContext(), account.getDevices(), account.getDeviceId(),
                    JamiAccountSummaryFragment.this);
            binding.deviceList.setAdapter(mDeviceAdapter);
        } else {
            mDeviceAdapter.setData(account.getDevices(), account.getDeviceId());
        }

        int totalHeight = 0;
        for (int i = 0; i < mDeviceAdapter.getCount(); i++) {
            View listItem = mDeviceAdapter.getView(i, null, binding.deviceList);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams par = binding.deviceList.getLayoutParams();
        par.height = totalHeight + (binding.deviceList.getDividerHeight() * (mDeviceAdapter.getCount() - 1));
        binding.deviceList.setLayoutParams(par);
        binding.deviceList.requestLayout();
        mAccountHasPassword = account.hasPassword();
        mAccountHasManager = account.hasManager();

        binding.changePasswordBtn.setText(mAccountHasPassword ? R.string.account_password_change : R.string.account_password_set);

        binding.accountSwitch.setChecked(account.isEnabled());
        binding.accountAliasTxt.setText(getString(R.string.profile));
        binding.accountIdTxt.setText(account.getUsername());
        mAccountId = account.getAccountID();
        mBestName = account.getRegisteredName();
        if (mBestName.isEmpty()) {
            mBestName = account.getAlias();
            if (mBestName.isEmpty()) {
                mBestName = account.getUsername();
            }
        }
        mBestName = mBestName + ".gz";
        String username = account.getRegisteredName();
        boolean currentRegisteredName = account.registeringUsername;
        boolean hasRegisteredName = !currentRegisteredName && username != null && !username.isEmpty();
        binding.groupRegisteringName.setVisibility(currentRegisteredName ? View.VISIBLE : View.GONE);
        binding.groupRegisterName.setVisibility((!hasRegisteredName && !currentRegisteredName) ? View.VISIBLE : View.GONE);
        binding.groupRegisteredName.setVisibility(hasRegisteredName ? View.VISIBLE : View.GONE);
        if (hasRegisteredName) {
            binding.registeredNameTxt.setText(username);
        }

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

        binding.accountStatus.setText(status);
        binding.accountStatus.setChipBackgroundColorResource(color);

        binding.passwordLayout.setVisibility(mAccountHasPassword ? View.VISIBLE : View.GONE);
        binding.layoutAddDevice.setVisibility(mAccountHasManager ? View.GONE : View.VISIBLE);
        binding.layoutAccountOptions.setVisibility(mAccountHasManager ? View.GONE : View.VISIBLE);
    }

    public boolean onBackPressed() {
        if (isDisplayingWizard()) {
            hideWizard();
            return true;
        }

        return false;
    }

    /*
    Add a new device UI management
     */
    private void flipForm() {
        if (!isDisplayingWizard()) {
            showWizard();
        } else {
            hideWizard();
        }
    }

    private void showWizard() {
        mSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void hidePopWizard() {
        mSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void hideWizard() {
        mSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        KeyboardVisibilityManager.hideKeyboard(getActivity(), 0);
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
        binding.passwordLayout.setError(getString(R.string.account_export_end_decryption_message));
        binding.ringPassword.setText("");
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

    private boolean isDisplayingWizard() {
        return mSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED;
    }

    private boolean onPasswordEditorAction(TextView pwd, int actionId, KeyEvent event) {
        Log.i(TAG, "onEditorAction " + actionId + " " + (event == null ? null : event.toString()));
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            if (pwd.getText().length() == 0) {
                pwd.setError(getString(R.string.account_enter_password));
            } else {
                onClickStart();
                return true;
            }
        }
        return false;
    }

    private void profileContainerClicked() {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.dialog_profile, null);

        final EditText editText = view.findViewById(R.id.user_name);
        editText.setText(presenter.getAlias(mAccountService.getCurrentAccount()));
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
                        presenter.saveVCard(editText.getText().toString().trim(), Single.just(mSourcePhoto).map(BitmapUtils::bitmapToPhoto));
                        mSourcePhoto = null;
                    } else {
                        presenter.saveVCardFormattedName(editText.getText().toString().trim());
                    }
                })
                .show();
    }

    private void onClickStart() {
        binding.passwordLayout.setError(null);
        String password = binding.ringPassword.getText().toString();
        presenter.startAccountExport(password);
    }

    private void onClickExport() {
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
    public void showRevokingProgressDialog() {
        mWaitDialog = ProgressDialog.show(getActivity(),
                getString(R.string.revoke_device_wait_title),
                getString(R.string.revoke_device_wait_message));
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
    public void showPIN(final String pin) {
        binding.ringPassword.setText("");
        mSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        binding.passwordLayout.setVisibility(View.GONE);
        binding.btnEndExport.setVisibility(View.VISIBLE);
        binding.btnStartExport.setVisibility(View.GONE);
        dismissWaitDialog();
        String pined = getString(R.string.account_end_export_infos).replace("%%", pin);
        final SpannableString styledResultText = new SpannableString(pined);
        int pos = pined.lastIndexOf(pin);
        styledResultText.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), pos, (pos + pin.length()), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        styledResultText.setSpan(new StyleSpan(Typeface.BOLD), pos, (pos + pin.length()), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        styledResultText.setSpan(new RelativeSizeSpan(2.8f), pos, (pos + pin.length()), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        binding.accountLinkInfo.setText(styledResultText);
        binding.accountLinkInfo.requestFocus();

        KeyboardVisibilityManager.hideKeyboard(getActivity(), 0);
    }

    @Override
    public void updateDeviceList(final Map<String, String> devices, final String currentDeviceId) {
        if (mDeviceAdapter == null) {
            return;
        }
        mDeviceAdapter.setData(devices, currentDeviceId);
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

    @Override
    public void onConfirmRevocation(String deviceId, String password) {
        presenter.revokeDevice(deviceId, password);
    }

    private void onBackupAccount() {
        BackupAccountDialog dialog = new BackupAccountDialog();
        Bundle args = new Bundle();
        args.putString(AccountEditionFragment.ACCOUNT_ID_KEY, getArguments().getString(AccountEditionFragment.ACCOUNT_ID_KEY));
        dialog.setArguments(args);
        dialog.setListener(this);
        dialog.show(requireFragmentManager(), FRAGMENT_DIALOG_BACKUP);
    }

    @Override
    public void onDeviceRevocationAsked(String deviceId) {
        ConfirmRevocationDialog dialog = new ConfirmRevocationDialog();
        Bundle args = new Bundle();
        args.putString(ConfirmRevocationDialog.DEVICEID_KEY, deviceId);
        args.putBoolean(ConfirmRevocationDialog.HAS_PASSWORD_KEY, mAccountHasPassword);
        dialog.setArguments(args);
        dialog.setListener(this);
        dialog.show(requireFragmentManager(), FRAGMENT_DIALOG_REVOCATION);
    }

    @Override
    public void onDeviceRename() {
        final String dev_name = presenter.getDeviceName();
        RenameDeviceDialog dialog = new RenameDeviceDialog();
        Bundle args = new Bundle();
        args.putString(RenameDeviceDialog.DEVICENAME_KEY, dev_name);
        dialog.setArguments(args);
        dialog.setListener(this);
        dialog.show(requireFragmentManager(), FRAGMENT_DIALOG_RENAME);
    }

    private void onPasswordChangeAsked() {
        ChangePasswordDialog dialog = new ChangePasswordDialog();
        Bundle args = new Bundle();
        args.putString(AccountEditionFragment.ACCOUNT_ID_KEY, getArguments().getString(AccountEditionFragment.ACCOUNT_ID_KEY));
        args.putBoolean(AccountEditionFragment.ACCOUNT_HAS_PASSWORD_KEY, mAccountHasPassword);
        dialog.setArguments(args);
        dialog.setListener(this);
        dialog.show(requireFragmentManager(), FRAGMENT_DIALOG_PASSWORD);
    }

    @Override
    public void onDeviceRename(String newName) {
        Log.d(TAG, "onDeviceRename: " + presenter.getDeviceName() + " -> " + newName);
        presenter.renameDevice(newName);
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
        } catch (Exception e) {
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

    private String getAccountAlias(Account account) {
        if (account == null) {
            cx.ring.utils.Log.e(TAG, "Not able to get account alias");
            return null;
        }
        String alias = getAlias(account);
        return (alias == null) ? account.getAlias() : alias;
    }

    private String getAlias(Account account) {
        if (account == null) {
            cx.ring.utils.Log.e(TAG, "Not able to get alias");
            return null;
        }
        VCard vcard = account.getProfile();
        if (vcard != null) {
            FormattedName name = vcard.getFormattedName();
            if (name != null) {
                String name_value = name.getValue();
                if (name_value != null && !name_value.isEmpty()) {
                    return name_value;
                }
            }
        }
        return null;
    }

    private String getUri(Account account, CharSequence defaultNameSip) {
        if (account.isIP2IP()) {
            return defaultNameSip.toString();
        }
        return account.getDisplayUri();
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
        if (mIsVisible && binding != null) {
            Activity activity = getActivity();
            if (activity instanceof HomeActivity)
                ((HomeActivity) activity).setToolbarElevation(binding.scrollview.canScrollVertically(SCROLL_DIRECTION_UP));
        }
    }

    public void setFragmentVisibility(boolean isVisible) {
        mIsVisible = isVisible;
    }
}
