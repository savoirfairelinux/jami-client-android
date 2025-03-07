/*
 *  Copyright (C) 2004-2025 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.account

import android.Manifest
import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import net.jami.model.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import cx.ring.R
import cx.ring.account.AccountPasswordDialog.UnlockAccountListener
import cx.ring.account.DeviceAdapter.DeviceRevocationListener
import cx.ring.account.RegisterNameDialog.RegisterNameDialogListener
import cx.ring.account.RenameDeviceDialog.RenameDeviceListener
import cx.ring.client.HomeActivity
import cx.ring.fragments.BlockListFragment
import cx.ring.databinding.DialogProfileBinding
import cx.ring.databinding.FragAccSummaryBinding
import cx.ring.databinding.ItemProgressDialogBinding
import cx.ring.fragments.*
import cx.ring.interfaces.AppBarStateListener
import cx.ring.mvp.BaseSupportFragment
import cx.ring.settings.AccountFragment
import cx.ring.utils.AndroidFileUtils
import cx.ring.utils.BiometricHelper
import cx.ring.utils.BitmapUtils
import cx.ring.utils.ContentUri
import cx.ring.utils.ContentUri.getBitmap
import cx.ring.utils.DeviceUtils
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.account.JamiAccountSummaryPresenter
import net.jami.account.JamiAccountSummaryView
import net.jami.model.Account
import net.jami.model.Contact
import net.jami.model.Profile
import net.jami.services.AccountService
import java.io.File
import javax.inject.Inject
import cx.ring.linkdevice.view.LinkDeviceExportSideActivity

@AndroidEntryPoint
class JamiAccountSummaryFragment :
    BaseSupportFragment<JamiAccountSummaryPresenter, JamiAccountSummaryView>(),
    RegisterNameDialogListener, JamiAccountSummaryView, AppBarStateListener,
    UnlockAccountListener, RenameDeviceListener, DeviceRevocationListener {

    private val mOnBackPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            val binding = mBinding ?: return

            if (childFragmentManager.backStackEntryCount == 1) {
                onAppBarScrollTargetViewChanged(binding.scrollview)
                binding.fragment.visibility = View.GONE
                this.isEnabled = false
            }

            childFragmentManager.popBackStack()
        }
    }
    private var mWaitDialog: AlertDialog? = null
    private var mAccountHasPassword = true
    private var mBestName = ""
    private var mAccount: Account? = null
    private var mCacheArchive: File? = null
    private var mProfilePhoto: ImageView? = null
    private var mDialogRemovePhoto: FloatingActionButton? = null
    private var mSourcePhoto: Bitmap? = null
    private var tmpProfilePhotoUri: android.net.Uri? = null
    private var mDeviceAdapter: DeviceAdapter? = null
    private val mDisposableBag = CompositeDisposable()
    private var mBinding: FragAccSummaryBinding? = null
    private var biometricEnroll: BiometricHelper.BiometricEnroll? = null
    private val enrollBiometricLauncher = registerForActivityResult(StartActivityForResult()) {
        biometricEnroll?.onActivityResult(it.resultCode, it.data)
    }

    @Inject
    lateinit var mAccountService: AccountService

    private val cameraLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            tmpProfilePhotoUri.let { photoUri ->
                if (photoUri == null) {
                    result.data?.extras?.getBitmap("data")?.let { bitmap ->
                        updatePhoto(Single.just(bitmap))
                    }
                } else {
                    updatePhoto(photoUri)
                }
            }
        }
    private val exportBackupLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            result.data?.data?.let { uri: android.net.Uri ->
                mCacheArchive?.let { cacheArchive ->
                    AndroidFileUtils.moveToUri(requireContext().contentResolver, cacheArchive, uri)
                        .observeOn(DeviceUtils.uiScheduler)
                        .subscribe({}) { e: Throwable ->
                            val view = view
                            if (view != null) {
                                Log.e(TAG, "Error exporting archive", e)
                                Snackbar.make(
                                    view,
                                    getString(R.string.export_archive_error),
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                        }.let { mDisposableBag.add(it) }
                }
            }
        }

    private val linkDeviceActivityLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            Log.w(TAG, "linkDeviceActivityLauncher: ${result.resultCode}")
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragAccSummaryBinding.inflate(inflater, container, false).apply {
            onAppBarScrollTargetViewChanged(scrollview)
            toolbar.setNavigationOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
            linkNewDevice.setOnClickListener { showLinkNewDevice() }
            linkedDevices.setRightDrawableOnClickListener { onDeviceRename() }
            registerName.setOnClickListener { showUsernameRegistrationPopup() }
            chipMore.setOnClickListener {
                if (devicesList.visibility == View.GONE) {
                    expand(devicesList)
                } else collapse(devicesList)
            }

            settingsAccount.setOnClickListener { presenter.goToAccount() }
            settingsMedia.setOnClickListener { presenter.goToMedia() }
            settingsMessages.setOnClickListener { presenter.goToSystem() }
            settingsAdvanced.setOnClickListener { presenter.goToAdvanced() }

            mBinding = this
        }.root

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, mOnBackPressedCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mDisposableBag.clear()
        mBinding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        mDisposableBag.dispose()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.setAccountId(requireArguments().getString(AccountEditionFragment.ACCOUNT_ID_KEY)!!)
    }

    override fun onResume() {
        super.onResume()
        (activity as HomeActivity?)?.let { activity ->
            mBinding!!.accountSwitch.setOnCheckedChangeListener { _, isChecked: Boolean ->
                presenter.enableAccount(isChecked)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mBinding!!.accountSwitch.setOnCheckedChangeListener(null)
    }

    //================= AppBar management =====================
    override fun onAppBarScrollTargetViewChanged(v: View?) {
        mBinding?.appBar?.setLiftOnScrollTargetView(v)
    }

    override fun onToolbarTitleChanged(title: CharSequence) {
        mBinding?.toolbar?.title = title
    }
    //=============== AppBar management end ===================

    override fun accountChanged(account: Account, profile: Profile) {
        mAccount = account
        val bestName = account.registeredName.ifEmpty { account.displayUsername ?: account.username!! }
        mBestName = "$bestName.jac"
        mBinding?.let { binding ->
            binding.userPhoto.setImageDrawable(AvatarDrawable.build(binding.root.context, account, profile, true))
            binding.username.setText(profile.displayName)
            binding.userPhoto.setOnClickListener { profileContainerClicked(account) }
            binding.linkedDevices.text = account.deviceName
            setLinkedDevicesAdapter(account)
            binding.linkNewDevice.visibility = if (account.hasManager()) View.GONE else View.VISIBLE
            mAccountHasPassword = account.hasPassword()
            mBinding!!.accountSwitch.setCheckedSilent(account.isEnabled)
            binding.accountAliasTxt.text = getString(R.string.profile)
            binding.identity.setText(account.username)
            val username = account.registeredName
            val currentRegisteredName = account.registeringUsername
            val hasRegisteredName = !currentRegisteredName && username.isNotEmpty()
            binding.groupRegisteringName.visibility = if (currentRegisteredName) View.VISIBLE else View.GONE
            binding.btnShare.setOnClickListener { shareAccount(if (hasRegisteredName) username else account.username) }
            binding.registerName.visibility = if (hasRegisteredName) View.GONE else View.VISIBLE
            binding.registeredName.setText(if (hasRegisteredName) username else resources.getString(R.string.no_registered_name_for_account))
            binding.btnQr.setOnClickListener {
                QRCodeFragment.newInstance(
                    QRCodeFragment.MODE_SCAN or QRCodeFragment.MODE_SHARE,
                    QRCodeFragment.MODE_SHARE,
                    Uri.fromString(account.uri!!)
                ).show(parentFragmentManager, QRCodeFragment.TAG)
            }
            binding.username.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val inputMethodManager = requireContext()
                        .getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(
                        requireActivity().currentFocus!!.windowToken, 0
                    )
                    binding.username.clearFocus()
                }
                false
            }
            binding.username.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus: Boolean ->
                val name = binding.username.text
                if (!hasFocus) {
                    presenter.updateProfile(name.toString())
                }
            }
        }

        setSwitchStatus(account)
    }

    private fun showWizard(accountId: String) {
        LinkDeviceFragment.newInstance(accountId)
            .show(parentFragmentManager, LinkDeviceFragment.TAG)
    }

    override fun showNetworkError() {
        dismissWaitDialog()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.account_export_end_network_title)
            .setMessage(R.string.account_export_end_network_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun showPasswordError() {
        dismissWaitDialog()
    }

    override fun showGenericError() {
        dismissWaitDialog()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.account_export_end_error_title)
            .setMessage(R.string.account_export_end_error_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun showPIN(pin: String) {}
    private fun profileContainerClicked(account: Account) {
        val inflater = LayoutInflater.from(activity)
        val view = DialogProfileBinding.inflate(inflater).apply {
            camera.setOnClickListener { presenter.cameraClicked() }
            gallery.setOnClickListener { presenter.galleryClicked() }
            removePhoto.setOnClickListener {
                removePhoto(account.loadedProfile!!.blockingGet().displayName)
            }
        }
        mProfilePhoto = view.profilePhoto

        // Show `delete` option if the account has a profile photo.
        mDialogRemovePhoto = view.removePhoto
        val dialogDisposableBag = CompositeDisposable().apply {
            add( // Show the delete button if the account has a profile photo.
                mAccountService.getObservableAccountProfile(account.accountId)
                    .observeOn(DeviceUtils.uiScheduler)
                    .subscribe {
                        view.removePhoto.visibility =
                            if (it.second.avatar != null) View.VISIBLE else View.GONE
                    }
            )
            add( // Load the profile photo.
                AvatarDrawable.load(inflater.context, account)
                    .observeOn(DeviceUtils.uiScheduler)
                    .subscribe { a -> view.profilePhoto.setImageDrawable(a) }
            )
            mDisposableBag.add(this)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.profile)
            .setView(view.root)
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
            .setPositiveButton(android.R.string.ok) { dialog, which ->
                mSourcePhoto?.let { source ->
                    Single.just(source).map { BitmapUtils.bitmapToBase64(it)!! }
                        .observeOn(Schedulers.computation())
                        .subscribe({ presenter.updateProfile(mBinding!!.username.text.toString(), it, "PNG") })
                        { e -> Log.e(TAG, "Error updating profile", e) }
                } ?: presenter.updateProfile(mBinding!!.username.text.toString(), "", "")
            }
            .setOnDismissListener {
                dialogDisposableBag.dispose()
                mProfilePhoto = null
                mDialogRemovePhoto = null
                mSourcePhoto = null
            }
            .show()
    }

    fun onClickExport() {
        onBackupAccount()
    }

    private fun showUsernameRegistrationPopup() {
        RegisterNameDialog().apply {
            arguments = Bundle().apply {
                putString(AccountEditionFragment.ACCOUNT_ID_KEY, mAccount?.accountId)
                putBoolean(AccountEditionFragment.ACCOUNT_HAS_PASSWORD_KEY, mAccountHasPassword)
            }
            setListener(this@JamiAccountSummaryFragment)
        }.show(parentFragmentManager, TAG)
    }

    override fun onRegisterName(name: String) {
        val account = mAccount ?: return
        BiometricHelper.startAccountAuthentication(this, account, getString(R.string.register_username)) { scheme: String, password: String ->
            presenter.registerName(name, scheme, password)
        }
    }

    override fun showExportingProgressDialog() {
        mWaitDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(ItemProgressDialogBinding.inflate(layoutInflater).root)
            .setTitle(R.string.export_account_wait_title)
            .setMessage(R.string.export_account_wait_message)
            .show()
    }

    override fun showPasswordProgressDialog() {
        mWaitDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(ItemProgressDialogBinding.inflate(layoutInflater).root)
            .setTitle(R.string.export_account_wait_title)
            .setMessage(R.string.account_password_change_wait_message)
            .show()
    }

    private fun dismissWaitDialog() {
        mWaitDialog?.apply {
            dismiss()
            mWaitDialog = null
        }
    }

    override fun passwordChangeEnded(accountId: String, ok: Boolean, newPassword: String) {
        dismissWaitDialog()
        if (!ok) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.account_device_revocation_wrong_password)
                .setMessage(R.string.account_export_end_decryption_message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        } else if (newPassword.isNotEmpty()) {
            biometricEnroll = BiometricHelper.BiometricEnroll(
                accountId,
                this,
                newPassword,
                mAccountService,
                { bi ->
                    Log.w(TAG, "Biometric enrollment: $bi")
                    if (bi == null)
                        BiometricHelper.deleteAccountKey(requireContext(), accountId)
                },
                launcher = enrollBiometricLauncher
            ).apply { start() }
        }
    }

    private fun selectBackupPath(mimeType: String?, fileName: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = mimeType
        intent.putExtra(Intent.EXTRA_TITLE, fileName)
        exportBackupLauncher.launch(intent)
    }

    override fun displayCompleteArchive(dest: File) {
        val type = AndroidFileUtils.getMimeType(dest.absolutePath)
        mCacheArchive = dest
        dismissWaitDialog()
        selectBackupPath(type, mBestName)
    }

    private fun onBackupAccount() {
        val account = mAccount ?: return
        BiometricHelper.startAccountAuthentication(this, account, getString(R.string.account_export_file)) { scheme: String, password: String ->
            onUnlockAccount(scheme, password)
        }
    }

    fun onBiometricChangeAsked() {
        val accountId = mAccount?.accountId ?: return
        val hasBiometric = BiometricHelper.loadAccountKey(requireContext(), accountId) != null
        Log.w(TAG, "hasBiometric: $hasBiometric")
        if (hasBiometric) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.account_biometric_disable)
                .setIcon(R.drawable.fingerprint_24)
                .setPositiveButton(R.string.menu_delete) { _, _ ->
                    BiometricHelper.deleteAccountKey(requireContext(), accountId)
                    mAccountService.refreshAccount(accountId)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } else {
            ConfirmBiometricDialog().apply {
                setListener { password: String ->
                    Log.w(TAG, "ConfirmBiometricDialog: $password")
                    biometricEnroll = BiometricHelper.BiometricEnroll(accountId,
                        this@JamiAccountSummaryFragment,
                        password,
                        mAccountService,
                        { bi -> Log.w(TAG, "Biometric enrollment: $bi") },
                        launcher = enrollBiometricLauncher
                    ).apply { start() }
                }
            }.show(parentFragmentManager, FRAGMENT_DIALOG_PASSWORD)
        }
    }

    fun onPasswordChangeAsked() {
        ChangePasswordDialog().apply {
            arguments = Bundle().apply {
                putString(AccountEditionFragment.ACCOUNT_ID_KEY, mAccount?.accountId)
                putBoolean(AccountEditionFragment.ACCOUNT_HAS_PASSWORD_KEY, mAccountHasPassword)
            }
            setListener { oldPassword: String, newPassword: String ->
                presenter.changePassword(oldPassword, newPassword)
            }
        }.show(parentFragmentManager, FRAGMENT_DIALOG_PASSWORD)
    }

    override fun onUnlockAccount(scheme: String, password: String) {
        val cacheDir = File(AndroidFileUtils.getTempShareDir(requireContext()), "archives")
        cacheDir.mkdirs()
        if (!cacheDir.canWrite()) Log.w(TAG, "Can't write to: $cacheDir")
        val dest = File(cacheDir, mBestName)
        if (dest.exists()) dest.delete()
        presenter.downloadAccountsArchive(dest, scheme, password)
    }

    override fun gotToImageCapture() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            val context = requireContext()
            val file = AndroidFileUtils.createImageFile(context)
            val uri = FileProvider.getUriForFile(context, ContentUri.AUTHORITY_FILES, file)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                .putExtra("android.intent.extras.CAMERA_FACING", 1)
                .putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                .putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
            tmpProfilePhotoUri = uri
            cameraLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                getString(R.string.starting_camera_error),
                Toast.LENGTH_SHORT
            ).show()
            Log.e(TAG, "Can't create temp file", e)
        }
    }

    override fun askCameraPermission() {
        requestPermissions(arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ), HomeActivity.REQUEST_PERMISSION_CAMERA)
    }

    private val pickProfilePicture =
        registerForActivityResult(PickVisualMedia()) { uri ->
            if (uri != null)
                updatePhoto(uri)
        }

    override fun goToGallery() {
        pickProfilePicture.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
    }

    private fun updatePhoto(uriImage: android.net.Uri) {
        updatePhoto(AndroidFileUtils.loadBitmap(requireContext(), uriImage))
    }

    /**
     * Remove the photo from the profile picture dialog.
     * Replace it with the default avatar.
     */
    private fun removePhoto(profileName: String?) {
        val account = presenter.account ?: return
        val username = account.username ?: return
        mDialogRemovePhoto?.visibility = View.GONE
        mProfilePhoto?.setImageDrawable(
            AvatarDrawable.Builder()
                .withNameData(profileName, account.registeredName)
                .withUri(Uri(Uri.JAMI_URI_SCHEME, username))
                .withCircleCrop(true)
                .withOnlineState(Contact.PresenceStatus.OFFLINE)
                .build(requireContext())
        )
        mSourcePhoto = null
    }

    private fun updatePhoto(image: Single<Bitmap>) {
        val account = presenter.account ?: return
        val username = account.username ?: return
        mDisposableBag.add(image.subscribeOn(Schedulers.io())
            .map { img ->
                mSourcePhoto = img
                AvatarDrawable.Builder()
                    .withPhoto(img)
                    .withNameData(null, account.registeredName)
                    .withUri(Uri(Uri.JAMI_URI_SCHEME, username))
                    .withCircleCrop(true)
                    .build(requireContext())
            }
            .observeOn(DeviceUtils.uiScheduler)
            .subscribe({ avatar: AvatarDrawable ->
                mDialogRemovePhoto?.visibility = View.VISIBLE
                mProfilePhoto?.setImageDrawable(avatar) }) { e: Throwable ->
                Log.e(TAG, "Error loading image", e)
            })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            HomeActivity.REQUEST_PERMISSION_CAMERA -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                presenter.cameraPermissionChanged(true)
                presenter.cameraClicked()
            }
            HomeActivity.REQUEST_PERMISSION_READ_STORAGE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                presenter.galleryClicked()
            }
        }
    }

    private fun setSwitchStatus(account: Account) {
        val switchButton = mBinding!!.accountSwitch
        var color = R.color.red_400
        val status: String
        if (account.isEnabled) {
            if (account.isTrying) {
                color = R.color.orange_400
                switchButton.showImage(true)
                switchButton.startImageAnimation()
            } else if (account.needsMigration()) {
                status = getString(R.string.account_update_needed)
                switchButton.showImage(false)
                switchButton.status = status
            } else if (account.isInError) {
                status = getString(R.string.account_status_connection_error)
                switchButton.showImage(false)
                switchButton.status = status
            } else if (account.isRegistered) {
                status = getString(R.string.account_status_online)
                color = R.color.green_400
                switchButton.showImage(false)
                switchButton.status = status
            } else if (!account.isRegistered) {
                color = R.color.grey_400
                status = getString(R.string.account_status_offline)
                switchButton.showImage(false)
                switchButton.status = status
            } else {
                status = getString(R.string.account_status_error)
                switchButton.showImage(false)
                switchButton.status = status
            }
        } else {
            color = R.color.grey_400
            status = getString(R.string.account_status_offline)
            switchButton.showImage(false)
            switchButton.status = status
        }
        switchButton.backColor = ContextCompat.getColor(requireContext(), color)
    }

    override fun showRevokingProgressDialog() {
        mWaitDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(ItemProgressDialogBinding.inflate(layoutInflater).root)
            .setTitle(R.string.revoke_device_wait_title)
            .setMessage(R.string.revoke_device_wait_message)
            .show()
    }

    override fun deviceRevocationEnded(device: String, status: Int) {
        dismissWaitDialog()
        val message: Int
        var title = R.string.account_device_revocation_error_title
        when (status) {
            0 -> {
                title = R.string.account_device_revocation_success_title
                message = R.string.account_device_revocation_success
            }
            1 -> message = R.string.account_device_revocation_wrong_password
            2 -> message = R.string.account_device_revocation_unknown_device
            else -> message = R.string.account_device_revocation_error_unknown
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { dialog: DialogInterface, which: Int ->
                dialog.dismiss()
                if (status == 1) {
                    onDeviceRevocationAsked(device)
                }
            }
            .show()
    }

    override fun updateDeviceList(devices: Map<String, String>, currentDeviceId: String) {
        mDeviceAdapter?.setData(devices, currentDeviceId)
        collapse(mBinding!!.devicesList)
    }

    private fun shareAccount(username: String?) {
        if (!username.isNullOrEmpty()) {
            val sharingIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getText(R.string.account_contact_me))
                putExtra(Intent.EXTRA_TEXT, getString(R.string.account_share_body, username, getText(R.string.app_website)))
            }
            startActivity(Intent.createChooser(sharingIntent, getText(R.string.share_via)))
        }
    }

    private fun fragmentWithBundle(result: Fragment, accountId: String): Fragment {
        return result.apply {
            arguments = Bundle().apply { putString(AccountEditionFragment.ACCOUNT_ID_KEY, accountId) }
        }
    }

    private fun changeFragment(fragment: Fragment, tag: String?) {
        childFragmentManager
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(R.id.fragment, fragment, tag)
            .addToBackStack(tag)
            .commit()
        mBinding!!.fragment.visibility = View.VISIBLE
        mOnBackPressedCallback.isEnabled = true
    }

    override fun goToAccount(accountId: String) {
        changeFragment(AccountFragment.newInstance(accountId), AccountFragment.TAG)
    }

    override fun goToMedia(accountId: String) {
        changeFragment(MediaPreferenceFragment.newInstance(accountId), MediaPreferenceFragment.TAG)
    }

    override fun goToSystem(accountId: String) {
        changeFragment(GeneralAccountFragment.newInstance(accountId), GeneralAccountFragment.TAG)
    }

    override fun goToAdvanced(accountId: String) {
        changeFragment(fragmentWithBundle(AdvancedAccountFragment(), accountId), AdvancedAccountFragment.TAG)
    }

    fun goToBlackList(accountId: String?) {
        val fragment = BlockListFragment().apply {
            arguments = Bundle().apply { putString(AccountEditionFragment.ACCOUNT_ID_KEY, accountId) }
        }
        changeFragment(fragment, BlockListFragment.TAG)
    }

    override fun onDeviceRevocationAsked(deviceId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(getString(R.string.revoke_device_message, deviceId))
            .setTitle(getText(R.string.revoke_device_title))
            .setPositiveButton(R.string.revoke_device_title) { _, _ ->
                mAccount?.let {  account ->
                    BiometricHelper.startAccountAuthentication(this, account, getString(R.string.revoke_device_title)) { scheme: String, password: String ->
                        presenter.revokeDevice(deviceId, scheme, password)
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel) { di, _ -> di.dismiss() }
            .create()
            .show()
    }

    override fun onDeviceRename() {
        RenameDeviceDialog().apply {
            arguments = Bundle().apply { putString(RenameDeviceDialog.DEVICENAME_KEY, presenter.deviceName) }
            setListener(this@JamiAccountSummaryFragment)
        }.show(parentFragmentManager, FRAGMENT_DIALOG_RENAME)
    }

    override fun onDeviceRename(newName: String) {
        presenter.renameDevice(newName)
    }

    private fun showLinkNewDevice() {
        linkDeviceActivityLauncher
            .launch(Intent(requireContext(), LinkDeviceExportSideActivity::class.java))
    }

    private fun expand(summary: View) {
        summary.visibility = View.VISIBLE
        summary.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.AT_MOST)
        )
        val targetHeight = summary.measuredHeight
        slideAnimator(0, targetHeight, summary).start()
        mBinding!!.chipMore.setText(R.string.account_link_hide_button)
    }

    private fun collapse(summary: View) {
        val startHeight = summary.height
        if (startHeight != 0) {
            slideAnimator(startHeight, 0, summary).apply {
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationEnd(animator: Animator) {
                        // Height=0, but it set visibility to GONE
                        summary.visibility = View.GONE
                    }
                    override fun onAnimationStart(animator: Animator) {}
                    override fun onAnimationCancel(animator: Animator) {}
                    override fun onAnimationRepeat(animator: Animator) {}
                })
                start()
            }
        }
        val binding = mBinding ?: return
        val adapter = mDeviceAdapter ?: return
        if (binding.chipMore.isVisible)
            binding.chipMore.text = getString(R.string.account_link_show_button, adapter.count)
    }

    private fun setLinkedDevicesAdapter(account: Account) {
        val binding = mBinding!!
        if (account.devices.size == 1) {
            binding.chipMore.visibility = View.GONE
            collapse(binding.devicesList)
        } else {
            if (mDeviceAdapter == null) {
                mDeviceAdapter = DeviceAdapter(requireContext(), account.devices, account.deviceId, this@JamiAccountSummaryFragment)
                binding.devicesList.adapter = mDeviceAdapter
            } else {
                mDeviceAdapter!!.setData(account.devices, account.deviceId)
            }
            binding.chipMore.visibility = View.VISIBLE
            binding.chipMore.text = getString(R.string.account_link_show_button, mDeviceAdapter!!.count)
        }
    }

    companion object {
        val TAG = JamiAccountSummaryFragment::class.simpleName!!
        private val FRAGMENT_DIALOG_RENAME = "$TAG.dialog.deviceRename"
        private val FRAGMENT_DIALOG_PASSWORD = "$TAG.dialog.changePassword"

        private fun slideAnimator(start: Int, end: Int, summary: View) = ValueAnimator.ofInt(start, end).apply {
             addUpdateListener { valueAnimator: ValueAnimator ->
                 // Update Height
                 val value = valueAnimator.animatedValue as Int
                 val layoutParams = summary.layoutParams
                 layoutParams.height = value
                 summary.layoutParams = layoutParams
             }
         }
    }
}