/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.tv.account

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.leanback.widget.GuidanceStylist.Guidance
import androidx.leanback.widget.GuidedAction
import cx.ring.R
import cx.ring.account.ProfileCreationFragment
import cx.ring.tv.camera.CustomCameraActivity
import cx.ring.utils.AndroidFileUtils.loadBitmap
import cx.ring.utils.BitmapUtils
import cx.ring.views.AvatarDrawable
import cx.ring.views.AvatarDrawable.Companion.load
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import net.jami.model.Account
import net.jami.navigation.HomeNavigationPresenter
import net.jami.navigation.HomeNavigationView
import net.jami.navigation.HomeNavigationViewModel

@AndroidEntryPoint
class TVProfileEditingFragment : JamiGuidedStepFragment<HomeNavigationPresenter?>(),
    HomeNavigationView {
    private var iconSize = -1
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            ProfileCreationFragment.REQUEST_CODE_PHOTO -> if (resultCode == Activity.RESULT_OK && data != null) {
                val extras = data.extras
                if (extras == null) {
                    Log.e(TAG, "onActivityResult: Not able to get picture from extra")
                    return
                }
                val uri = extras[MediaStore.EXTRA_OUTPUT] as Uri?
                if (uri != null) {
                    val cr = requireContext().contentResolver
                    presenter!!.saveVCardPhoto(Single.fromCallable {
                        cr.openInputStream(uri).use { BitmapFactory.decodeStream(it) }
                    }.map { obj: Bitmap -> BitmapUtils.bitmapToPhoto(obj) })
                }
            }
            ProfileCreationFragment.REQUEST_CODE_GALLERY -> if (resultCode == Activity.RESULT_OK && data != null) {
                presenter!!.saveVCardPhoto(loadBitmap(requireContext(), data.data!!)
                    .map { obj: Bitmap -> BitmapUtils.bitmapToPhoto(obj) })
            }
            else -> {
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            ProfileCreationFragment.REQUEST_PERMISSION_CAMERA -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                presenter!!.cameraPermissionChanged(true)
                presenter!!.cameraClicked()
            }
            ProfileCreationFragment.REQUEST_PERMISSION_READ_STORAGE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                presenter!!.galleryClicked()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        iconSize = resources.getDimension(R.dimen.tv_avatar_size).toInt()
    }

    override fun onCreateGuidance(savedInstanceState: Bundle): Guidance {
        val title = getString(R.string.profile)
        val breadcrumb = ""
        val description = getString(R.string.profile_message_warning)
        val icon = requireContext().getDrawable(R.drawable.ic_contact_picture_fallback)
        return Guidance(title, description, breadcrumb, icon)
    }

    override fun onProvideTheme(): Int {
        return R.style.Theme_Ring_Leanback_GuidedStep_First
    }

    override fun onCreateActions(actions: List<GuidedAction>, savedInstanceState: Bundle) {
        addEditTextAction(context, actions, USER_NAME.toLong(), R.string.account_edit_profile, R.string.profile_name_hint)
        addAction(context, actions, CAMERA.toLong(), R.string.take_a_photo)
        addAction(context, actions, GALLERY.toLong(), R.string.open_the_gallery)
        this.actions = actions
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        if (action.id == USER_NAME.toLong()) {
            val username = action.editTitle.toString()
            presenter!!.saveVCardFormattedName(username)
        } else if (action.id == CAMERA.toLong()) {
            presenter!!.cameraClicked()
        } else if (action.id == GALLERY.toLong()) {
            presenter!!.galleryClicked()
        }
        return super.onGuidedActionEditedAndProceed(action)
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == CAMERA.toLong()) {
            presenter!!.cameraClicked()
        } else if (action.id == GALLERY.toLong()) {
            presenter!!.galleryClicked()
        }
    }

    override fun showViewModel(viewModel: HomeNavigationViewModel) {
        val account = viewModel.account
        if (account == null) {
            Log.e(TAG, "Not able to get current account")
            return
        }
        val alias = viewModel.alias
        val action = actions?.let { if (it.isEmpty()) null else it[0] }
        if (action != null && action.id == USER_NAME.toLong()) {
            if (TextUtils.isEmpty(alias)) {
                action.editTitle = ""
                action.title = getString(R.string.account_edit_profile)
            } else {
                action.editTitle = alias
                action.title = alias
            }
            notifyActionChanged(0)
        }
        if (TextUtils.isEmpty(alias)) guidanceStylist.titleView.setText(R.string.profile) else guidanceStylist.titleView.text =
            alias
        setPhoto(account)
    }

    override fun setPhoto(account: Account) {
        load(requireContext(), account)
            .map { avatar -> avatar.apply { setInSize(iconSize) } }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { avatar: AvatarDrawable? -> guidanceStylist.iconView.setImageDrawable(avatar) }
    }

    override fun updateModel(account: Account) {}
    override fun gotToImageCapture() {
        val intent = Intent(activity, CustomCameraActivity::class.java)
        startActivityForResult(intent, ProfileCreationFragment.REQUEST_CODE_PHOTO)
    }

    override fun askCameraPermission() {
        requestPermissions(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
            ProfileCreationFragment.REQUEST_PERMISSION_CAMERA
        )
    }

    override fun askGalleryPermission() {
        requestPermissions(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            ProfileCreationFragment.REQUEST_PERMISSION_READ_STORAGE
        )
    }

    override fun goToGallery() {
        try {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, ProfileCreationFragment.REQUEST_CODE_GALLERY)
        } catch (e: ActivityNotFoundException) {
            AlertDialog.Builder(requireContext())
                .setPositiveButton(android.R.string.ok, null)
                .setTitle(R.string.gallery_error_title)
                .setMessage(R.string.gallery_error_message)
                .show()
        }
    }

    companion object {
        private const val USER_NAME = 1
        private const val GALLERY = 2
        private const val CAMERA = 3
    }
}