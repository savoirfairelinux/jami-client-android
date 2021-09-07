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
package cx.ring.account

import android.Manifest
import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnScrollChangedListener
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import cx.ring.R
import cx.ring.account.BackupAccountDialog.UnlockAccountListener
import cx.ring.account.ChangePasswordDialog.PasswordChangedListener
import cx.ring.account.ConfirmRevocationDialog.ConfirmRevocationListener
import cx.ring.account.DeviceAdapter.DeviceRevocationListener
import cx.ring.account.RegisterNameDialog.RegisterNameDialogListener
import cx.ring.account.RenameDeviceDialog.RenameDeviceListener
import cx.ring.client.HomeActivity
import cx.ring.contactrequests.BlockListFragment
import cx.ring.databinding.FragAccSummaryBinding
import cx.ring.fragments.*
import cx.ring.mvp.BaseSupportFragment
import cx.ring.services.VCardServiceImpl.Companion.loadProfile
import cx.ring.settings.AccountFragment
import cx.ring.utils.AndroidFileUtils
import cx.ring.utils.BitmapUtils
import cx.ring.utils.ContentUriHandler
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.account.JamiAccountSummaryPresenter
import net.jami.account.JamiAccountSummaryView
import net.jami.model.Account
import net.jami.utils.StringUtils
import java.io.File
import java.util.*

@AndroidEntryPoint
class JamiAccountSummaryFragment :
    BaseSupportFragment<JamiAccountSummaryPresenter, JamiAccountSummaryView>(),
    RegisterNameDialogListener, JamiAccountSummaryView, PasswordChangedListener,
    UnlockAccountListener, OnScrollChangedListener, RenameDeviceListener, DeviceRevocationListener,
    ConfirmRevocationListener {
    private val mOnBackPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (mBinding!!.fragment.visibility == View.VISIBLE) {
                mBinding!!.fragment.visibility = View.GONE
                this.isEnabled = false
                childFragmentManager.popBackStack()
            }
        }
    }
    private var mWaitDialog: ProgressDialog? = null
    private var mAccountHasPassword = true
    private var mBestName = ""
    private var mAccountId: String? = ""
    private var mCacheArchive: File? = null
    private var mProfilePhoto: ImageView? = null
    private var mSourcePhoto: Bitmap? = null
    private var tmpProfilePhotoUri: Uri? = null
    private var mDeviceAdapter: DeviceAdapter? = null
    private val mDisposableBag = CompositeDisposable()
    private val mProfileDisposable = CompositeDisposable()
    private var mBinding: FragAccSummaryBinding? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mDisposableBag.add(mProfileDisposable)
        return FragAccSummaryBinding.inflate(inflater, container, false).apply {
            mBinding = this
        }.root
    }

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
        requireArguments().let { arguments ->
            presenter.setAccountId(arguments.getString(AccountEditionFragment.ACCOUNT_ID_KEY)!!)
        }
        mBinding!!.scrollview.viewTreeObserver.addOnScrollChangedListener(this)
        mBinding!!.linkNewDevice.setOnClickListener { v: View? -> showWizard(mAccountId) }
        mBinding!!.linkedDevices.setRightDrawableOnClickListener { v: View? -> onDeviceRename() }
        mBinding!!.registerName.setOnClickListener { v: View? -> showUsernameRegistrationPopup() }
        val items: MutableList<SettingItem> = ArrayList(4)
        items.add(SettingItem(R.string.account, R.drawable.baseline_account_card_details) { presenter.goToAccount() })
        items.add(SettingItem(R.string.account_preferences_media_tab, R.drawable.outline_file_copy_24) { presenter.goToMedia() })
        items.add(SettingItem(R.string.notif_channel_messages, R.drawable.baseline_chat_24) { presenter.goToSystem() })
        items.add(SettingItem(R.string.account_preferences_advanced_tab, R.drawable.round_check_circle_24) { presenter.goToAdvanced() })
        val adapter = SettingsAdapter(view.context, R.layout.item_setting, items)
        mBinding!!.settingsList.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView: AdapterView<*>?, v: View?, i: Int, l: Long ->
                adapter.getItem(i)!!
                    .onClick()
            }
        mBinding!!.settingsList.adapter = adapter
        var totalHeight = 0
        for (i in 0 until adapter.count) {
            val listItem = adapter.getView(i, null, mBinding!!.settingsList)
            listItem.measure(0, 0)
            totalHeight += listItem.measuredHeight
        }
        val par = mBinding!!.settingsList.layoutParams
        par.height = totalHeight + mBinding!!.settingsList.dividerHeight * (adapter.count - 1)
        mBinding!!.settingsList.layoutParams = par
        mBinding!!.settingsList.requestLayout()
        mBinding!!.chipMore.setOnClickListener { v: View? ->
            if (mBinding!!.devicesList.visibility == View.GONE) {
                expand(mBinding!!.devicesList)
            } else {
                collapse(mBinding!!.devicesList)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as HomeActivity).let { activity ->
            activity.showAccountStatus(true)
            activity.switchButton.setOnCheckedChangeListener { _, isChecked: Boolean ->
                presenter.enableAccount(isChecked)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        (requireActivity() as HomeActivity).let { activity ->
            activity.showAccountStatus(false)
            activity.switchButton.setOnCheckedChangeListener(null)
        }
    }

    fun setAccount(accountId: String) {
        presenter.setAccountId(accountId)
    }

    override fun updateUserView(account: Account) {
        val context = context ?: return
        mProfileDisposable.clear()
        mProfileDisposable.add(loadProfile(context, account)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ profile ->
                mBinding?.let { binding ->
                    binding.userPhoto.setImageDrawable(AvatarDrawable.build(context, account, profile, true))
                    binding.username.setText(profile.first)
                }
            }, { e: Throwable -> Log.e(TAG, "Error loading avatar", e) })
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        when (requestCode) {
            WRITE_REQUEST_CODE -> if (resultCode == Activity.RESULT_OK) {
                if (resultData != null) {
                    val uri = resultData.data
                    if (uri != null) {
                        if (mCacheArchive != null) {
                            AndroidFileUtils.moveToUri(requireContext().contentResolver, mCacheArchive!!, uri)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({}) { e: Throwable ->
                                    val v = view
                                    if (v != null)
                                        Snackbar.make(v, "Can't export archive: " + e.message, Snackbar.LENGTH_LONG).show()
                                }
                        }
                    }
                }
            }
            HomeActivity.REQUEST_CODE_PHOTO -> {
                tmpProfilePhotoUri.let { photoUri ->
                    if (resultCode == Activity.RESULT_OK) {
                        if (photoUri == null) {
                            if (resultData != null)
                                updatePhoto(Single.just(resultData.extras!!["data"] as Bitmap?))
                        } else {
                            updatePhoto(photoUri)
                        }
                    }
                    tmpProfilePhotoUri = null
                }
            }
            HomeActivity.REQUEST_CODE_GALLERY -> if (resultCode == Activity.RESULT_OK && resultData != null) {
                updatePhoto(resultData.data!!)
            }
        }
    }

    override fun accountChanged(account: Account) {
        updateUserView(account)
        mBinding?.let { binding ->
            binding.userPhoto.setOnClickListener { profileContainerClicked(account) }
            binding.linkedDevices.setText(account.deviceName)
            setLinkedDevicesAdapter(account)
            mAccountHasPassword = account.hasPassword()
            (requireActivity() as HomeActivity).switchButton.setCheckedSilent(account.isEnabled)
            binding.accountAliasTxt.text = getString(R.string.profile)
            binding.identity.setText(account.username)
            mAccountId = account.accountID
            mBestName = account.registeredName ?: account.displayUsername ?: account.username!!
            mBestName = "$mBestName.gz"
            val username = account.registeredName
            val currentRegisteredName = account.registeringUsername
            val hasRegisteredName = !currentRegisteredName && username != null && !username.isEmpty()
            binding.groupRegisteringName.visibility = if (currentRegisteredName) View.VISIBLE else View.GONE
            binding.btnShare.setOnClickListener { shareAccount(if (hasRegisteredName) username else account.username) }
            binding.registerName.visibility = if (hasRegisteredName) View.GONE else View.VISIBLE
            binding.registeredName.setText(if (hasRegisteredName) username else resources.getString(R.string.no_registered_name_for_account))
            binding.btnQr.setOnClickListener {
                QRCodeFragment.newInstance(QRCodeFragment.INDEX_CODE)
                    .show(parentFragmentManager, QRCodeFragment.TAG)
            }
            binding.username.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus: Boolean ->
                    val name = binding.username.text
                    if (!hasFocus && !TextUtils.isEmpty(name)) {
                        presenter.saveVCardFormattedName(name.toString())
                    }
                }
        }

        setSwitchStatus(account)
    }

    fun onBackPressed(): Boolean {
        return false
    }

    private fun showWizard(accountId: String?) {
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
        val view = inflater.inflate(R.layout.dialog_profile, null) as ViewGroup
        val profilePhoto = view.findViewById<ImageView>(R.id.profile_photo).apply { mProfilePhoto = this}
        mDisposableBag.add(AvatarDrawable.load(inflater.context, account)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { a -> profilePhoto.setImageDrawable(a) })
        val cameraView = view.findViewById<ImageButton>(R.id.camera)
        cameraView.setOnClickListener { presenter.cameraClicked() }
        val gallery = view.findViewById<ImageButton>(R.id.gallery)
        gallery.setOnClickListener { presenter.galleryClicked() }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.profile)
            .setView(view)
            .setNegativeButton(android.R.string.cancel) { dialog, which -> dialog.cancel() }
            .setPositiveButton(android.R.string.ok) { dialog, which ->
                mSourcePhoto?.let { source ->
                    presenter.saveVCard(mBinding!!.username.text.toString(),
                        Single.just(source).map { obj -> BitmapUtils.bitmapToPhoto(obj) })
                    mSourcePhoto = null
                }
            }
            .show()
    }

    fun onClickExport() {
        if (mAccountHasPassword) {
            onBackupAccount()
        } else {
            onUnlockAccount(mAccountId!!, "")
        }
    }

    private fun showUsernameRegistrationPopup() {
        RegisterNameDialog().apply {
            arguments = Bundle().apply {
                putString(AccountEditionFragment.ACCOUNT_ID_KEY, mAccountId)
                putBoolean(AccountEditionFragment.ACCOUNT_HAS_PASSWORD_KEY, mAccountHasPassword)
            }
            setListener(this@JamiAccountSummaryFragment)
        }.show(parentFragmentManager, TAG)
    }

    override fun onRegisterName(name: String?, password: String?) {
        presenter.registerName(name, password)
    }

    override fun showExportingProgressDialog() {
        mWaitDialog = ProgressDialog.show(activity, getString(R.string.export_account_wait_title), getString(R.string.export_account_wait_message))
    }

    override fun showPasswordProgressDialog() {
        mWaitDialog = ProgressDialog.show(activity, getString(R.string.export_account_wait_title), getString(R.string.account_password_change_wait_message))
    }

    private fun dismissWaitDialog() {
        mWaitDialog?.apply {
            dismiss()
            mWaitDialog = null
        }
    }

    override fun passwordChangeEnded(ok: Boolean) {
        dismissWaitDialog()
        if (!ok) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.account_device_revocation_wrong_password)
                .setMessage(R.string.account_export_end_decryption_message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private fun createFile(mimeType: String?, fileName: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = mimeType
        intent.putExtra(Intent.EXTRA_TITLE, fileName)
        startActivityForResult(intent, WRITE_REQUEST_CODE)
    }

    override fun displayCompleteArchive(dest: File) {
        val type = AndroidFileUtils.getMimeType(dest.absolutePath)
        mCacheArchive = dest
        dismissWaitDialog()
        createFile(type, mBestName)
    }

    private fun onBackupAccount() {
        BackupAccountDialog().apply {
            arguments = Bundle().apply {
                putString(AccountEditionFragment.ACCOUNT_ID_KEY, mAccountId)
            }
            setListener(this@JamiAccountSummaryFragment)
        }.show(parentFragmentManager, FRAGMENT_DIALOG_BACKUP)
    }

    fun onPasswordChangeAsked() {
        ChangePasswordDialog().apply {
            arguments = Bundle().apply {
                putString(AccountEditionFragment.ACCOUNT_ID_KEY, mAccountId)
                putBoolean(AccountEditionFragment.ACCOUNT_HAS_PASSWORD_KEY, mAccountHasPassword)
            }
            setListener(this@JamiAccountSummaryFragment)
        }.show(parentFragmentManager, FRAGMENT_DIALOG_PASSWORD)
    }

    override fun onPasswordChanged(oldPassword: String, newPassword: String) {
        presenter.changePassword(oldPassword, newPassword)
    }

    override fun onUnlockAccount(accountId: String, password: String) {
        val context = requireContext()
        val cacheDir = File(AndroidFileUtils.getTempShareDir(context), "archives")
        cacheDir.mkdirs()
        if (!cacheDir.canWrite()) Log.w(TAG, "Can't write to: $cacheDir")
        val dest = File(cacheDir, mBestName)
        if (dest.exists()) dest.delete()
        presenter.downloadAccountsArchive(dest, password)
    }

    override fun gotToImageCapture() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            val context = requireContext()
            val file = AndroidFileUtils.createImageFile(context)
            val uri = FileProvider.getUriForFile(context, ContentUriHandler.AUTHORITY_FILES, file)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                .putExtra("android.intent.extras.CAMERA_FACING", 1)
                .putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                .putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
            tmpProfilePhotoUri = uri
            startActivityForResult(intent, HomeActivity.REQUEST_CODE_PHOTO)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error starting camera: " + e.localizedMessage, Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Can't create temp file", e)
        }
    }

    override fun askCameraPermission() {
        requestPermissions(arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), HomeActivity.REQUEST_PERMISSION_CAMERA
        )
    }

    override fun goToGallery() {
        try {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, HomeActivity.REQUEST_CODE_GALLERY)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.gallery_error_message, Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun askGalleryPermission() {
        requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), HomeActivity.REQUEST_PERMISSION_READ_STORAGE)
    }

    private fun updatePhoto(uriImage: Uri) {
        updatePhoto(AndroidFileUtils.loadBitmap(requireContext(), uriImage))
    }

    private fun updatePhoto(image: Single<Bitmap>) {
        val account = presenter.account ?: return
        mDisposableBag.add(image.subscribeOn(Schedulers.io())
            .map { img ->
                mSourcePhoto = img
                AvatarDrawable.Builder()
                    .withPhoto(img)
                    .withNameData(null, account.registeredName)
                    .withId(account.uri)
                    .withCircleCrop(true)
                    .build(requireContext())
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ avatar: AvatarDrawable -> mProfilePhoto!!.setImageDrawable(avatar) }) { e: Throwable ->
                Log.e(TAG, "Error loading image", e)
            })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            HomeActivity.REQUEST_PERMISSION_CAMERA -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                presenter.cameraClicked()
            }
            HomeActivity.REQUEST_PERMISSION_READ_STORAGE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                presenter.galleryClicked()
            }
        }
    }

    override fun onScrollChanged() {
        if (mBinding != null) {
            val activity = activity
            if (activity is HomeActivity) activity.setToolbarElevation(
                mBinding!!.scrollview.canScrollVertically(SCROLL_DIRECTION_UP)
            )
        }
    }

    override fun setSwitchStatus(account: Account) {
        val switchButton = (requireActivity() as HomeActivity).switchButton
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
        mWaitDialog = ProgressDialog.show(activity,
            getString(R.string.revoke_device_wait_title),
            getString(R.string.revoke_device_wait_message)
        )
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
        if (mDeviceAdapter == null) {
            return
        }
        mDeviceAdapter!!.setData(devices, currentDeviceId)
        collapse(mBinding!!.devicesList)
    }

    private fun shareAccount(username: String?) {
        if (!StringUtils.isEmpty(username)) {
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getText(R.string.account_contact_me))
            sharingIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.account_share_body, username, getText(R.string.app_website)))
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
            .addToBackStack(tag).commit()
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
        val blockListFragment = BlockListFragment().apply {
            arguments = Bundle().apply { putString(AccountEditionFragment.ACCOUNT_ID_KEY, accountId) }
        }
        changeFragment(blockListFragment, BlockListFragment.TAG)
    }

    fun popBackStack() {
        childFragmentManager.popBackStackImmediate()
        val fragmentTag = childFragmentManager.getBackStackEntryAt(childFragmentManager.backStackEntryCount - 1).name
        val fragment = childFragmentManager.findFragmentByTag(fragmentTag)
        if (fragment != null)
            changeFragment(fragment, fragmentTag)
    }

    override fun onConfirmRevocation(deviceId: String, password: String) {
        presenter.revokeDevice(deviceId, password)
    }

    override fun onDeviceRevocationAsked(deviceId: String?) {
        ConfirmRevocationDialog().apply {
            arguments = Bundle().apply {
                putString(ConfirmRevocationDialog.DEVICEID_KEY, deviceId)
                putBoolean(ConfirmRevocationDialog.HAS_PASSWORD_KEY, mAccountHasPassword)
            }
            setListener(this@JamiAccountSummaryFragment)
        }.show(parentFragmentManager, FRAGMENT_DIALOG_REVOCATION)
    }

    override fun onDeviceRename() {
        RenameDeviceDialog().apply {
            arguments = Bundle().apply { putString(RenameDeviceDialog.DEVICENAME_KEY, presenter.deviceName) }
            setListener(this@JamiAccountSummaryFragment)
        }.show(parentFragmentManager, FRAGMENT_DIALOG_RENAME)
    }

    override fun onDeviceRename(newName: String) {
        Log.d(TAG, "onDeviceRename: " + presenter.deviceName + " -> " + newName)
        presenter.renameDevice(newName)
    }

    private fun expand(summary: View) {
        summary.visibility = View.VISIBLE
        val widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        summary.measure(
            View.MeasureSpec.makeMeasureSpec(widthSpec, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.AT_MOST)
        )
        val targetHeight = summary.measuredHeight
        val animator = slideAnimator(0, targetHeight, summary)
        animator.start()
        mBinding!!.chipMore.setText(R.string.account_link_hide_button)
    }

    private fun collapse(summary: View) {
        val finalHeight = summary.height
        val mAnimator = slideAnimator(finalHeight, 0, summary)
        mAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animator: Animator) {
                // Height=0, but it set visibility to GONE
                summary.visibility = View.GONE
            }

            override fun onAnimationStart(animator: Animator) {}
            override fun onAnimationCancel(animator: Animator) {}
            override fun onAnimationRepeat(animator: Animator) {}
        })
        mAnimator.start()
        mBinding!!.chipMore.text = getString(R.string.account_link_show_button, mDeviceAdapter!!.count)
    }

    private fun setLinkedDevicesAdapter(account: Account) {
        if (account.devices.size == 1) {
            mBinding!!.chipMore.visibility = View.GONE
        } else {
            mBinding!!.chipMore.visibility = View.VISIBLE
            if (mDeviceAdapter == null) {
                mDeviceAdapter = DeviceAdapter(requireContext(), account.devices, account.deviceId, this@JamiAccountSummaryFragment)
                mBinding!!.chipMore.text = getString(R.string.account_link_show_button, mDeviceAdapter!!.count)
                mBinding!!.devicesList.adapter = mDeviceAdapter
            } else {
                mDeviceAdapter!!.setData(account.devices, account.deviceId)
            }
        }
    }

    companion object {
        val TAG = JamiAccountSummaryFragment::class.simpleName!!
        private val FRAGMENT_DIALOG_REVOCATION = "$TAG.dialog.deviceRevocation"
        private val FRAGMENT_DIALOG_RENAME = "$TAG.dialog.deviceRename"
        private val FRAGMENT_DIALOG_PASSWORD = "$TAG.dialog.changePassword"
        private val FRAGMENT_DIALOG_BACKUP = "$TAG.dialog.backup"
        private const val WRITE_REQUEST_CODE = 43
        private const val SCROLL_DIRECTION_UP = -1
        private fun slideAnimator(start: Int, end: Int, summary: View): ValueAnimator {
            val animator = ValueAnimator.ofInt(start, end)
            animator.addUpdateListener { valueAnimator: ValueAnimator ->
                // Update Height
                val value = valueAnimator.animatedValue as Int
                val layoutParams = summary.layoutParams
                layoutParams.height = value
                summary.layoutParams = layoutParams
            }
            return animator
        }
    }
}