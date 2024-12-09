/*
 *  Copyright (C) 2004-2024 Savoir-faire Linux Inc.
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
package cx.ring.tv.account

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.leanback.widget.GuidanceStylist.Guidance
import androidx.leanback.widget.GuidedAction
import cx.ring.R
import cx.ring.account.ProfileCreationFragment
import cx.ring.tv.camera.CustomCameraActivity
import cx.ring.utils.AndroidFileUtils
import cx.ring.utils.BitmapUtils
import cx.ring.utils.ContentUri.getUri
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint
import net.jami.navigation.HomeNavigationPresenter
import net.jami.navigation.HomeNavigationView
import net.jami.navigation.HomeNavigationViewModel
import net.jami.utils.VCardUtils

@AndroidEntryPoint
class TVProfileEditingFragment : JamiGuidedStepFragment<HomeNavigationPresenter, HomeNavigationView>(), HomeNavigationView {
    private var iconSize = -1

    private val pickProfilePicture =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null)
                setProfilePicture(uri)
        }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            presenter.cameraPermissionChanged(granted)
            if (granted)
                presenter.cameraClicked()
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            ProfileCreationFragment.REQUEST_CODE_PHOTO -> if (resultCode == Activity.RESULT_OK && data != null) {
                val extras = data.extras
                if (extras == null) {
                    Log.e(TAG, "onActivityResult: Not able to get picture from extra")
                    return
                }
                extras.getUri(MediaStore.EXTRA_OUTPUT)?.let {
                    setProfilePicture(it)
                }
            }
            else -> {
            }
        }
    }

    private fun setProfilePicture(uri: Uri) {
        val displayName = getCurrentDisplayname() ?: ""
        AndroidFileUtils.getCacheFile(requireContext(), uri)
            .subscribe({ file ->
                val fileType = VCardUtils.pictureTypeFromMime(AndroidFileUtils.getMimeType(requireContext().contentResolver, uri))
                presenter.updateProfile(displayName, file, fileType)
            }) {
                presenter.updateProfile(displayName)
            }
    }

    private fun getCurrentDisplayname(): String? {
        val usernameAction = actions.find { it.id == USER_NAME }
        return usernameAction?.editTitle?.toString()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        iconSize = resources.getDimension(R.dimen.tv_avatar_size).toInt()
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?) = Guidance(
        getString(R.string.profile),
        getString(R.string.profile_message_warning),
        "",
        requireContext().getDrawable(R.drawable.ic_contact_picture_fallback)
    )

    override fun onProvideTheme(): Int = R.style.Theme_Ring_Leanback_GuidedStep_First

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        val context = requireContext()
        addEditTextAction(context, actions, USER_NAME, R.string.account_edit_profile, R.string.profile_name_hint)
        addAction(context, actions, CAMERA, R.string.take_a_photo)
        addAction(context, actions, GALLERY, R.string.open_the_gallery)
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        when (action.id) {
            USER_NAME -> presenter.updateProfile(action.editTitle.toString())
            CAMERA -> presenter.cameraClicked()
            GALLERY -> presenter.galleryClicked()
        }
        return super.onGuidedActionEditedAndProceed(action)
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        when (action.id) {
            CAMERA -> presenter.cameraClicked()
            GALLERY -> presenter.galleryClicked()
        }
    }

    override fun showViewModel(viewModel: HomeNavigationViewModel) {
        val action = actions.let { if (it.isEmpty()) null else it[0] }
        if (action != null && action.id == USER_NAME) {
            if (viewModel.profile.displayName.isNullOrEmpty()) {
                action.editTitle = ""
                action.title = getString(R.string.account_edit_profile)
            } else {
                action.editTitle = viewModel.profile.displayName
                action.title = viewModel.profile.displayName
            }
            notifyActionChanged(0)
        }
        if (viewModel.profile.displayName.isNullOrEmpty())
            guidanceStylist.titleView?.setText(R.string.profile)
        else guidanceStylist.titleView?.text = viewModel.profile.displayName
        guidanceStylist.iconView?.setImageDrawable(AvatarDrawable.build(requireContext(), viewModel.account, viewModel.profile, true)
            .apply { setInSize(iconSize) })
    }

    @Suppress("DEPRECATION")
    override fun gotToImageCapture() {
        startActivityForResult(Intent(activity, CustomCameraActivity::class.java), ProfileCreationFragment.REQUEST_CODE_PHOTO)
    }

    override fun askCameraPermission() {
        requestCameraPermission.launch(Manifest.permission.CAMERA)
    }

    override fun goToGallery() {
        pickProfilePicture.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    companion object {
        private const val USER_NAME = 1L
        private const val GALLERY = 2L
        private const val CAMERA = 3L
        private val TAG = TVProfileEditingFragment::class.simpleName!!
    }
}