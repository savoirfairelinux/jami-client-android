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
package cx.ring.tv.account

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.leanback.widget.GuidanceStylist.Guidance
import androidx.leanback.widget.GuidedAction
import cx.ring.R
import cx.ring.account.AccountCreationViewModel
import cx.ring.account.ProfileCreationFragment
import cx.ring.tv.camera.CustomCameraActivity
import cx.ring.utils.AndroidFileUtils.loadBitmap
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.core.Single
import net.jami.account.ProfileCreationPresenter
import net.jami.account.ProfileCreationView

@AndroidEntryPoint
class TVProfileCreationFragment : JamiGuidedStepFragment<ProfileCreationPresenter, ProfileCreationView>(), ProfileCreationView {
    private val model: AccountCreationViewModel by activityViewModels()
    private val pickProfilePicture =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null)
                presenter.photoUpdated(loadBitmap(requireContext(), uri).map { it })
        }

    private var iconSize = -1
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        when (requestCode) {
            ProfileCreationFragment.REQUEST_CODE_PHOTO -> if (resultCode == Activity.RESULT_OK && intent != null && intent.extras != null) {
                val uri = intent.extras!![MediaStore.EXTRA_OUTPUT] as Uri?
                try {
                    requireContext().contentResolver.openInputStream(uri!!).use { iStream ->
                        val image = BitmapFactory.decodeStream(iStream)
                        presenter.photoUpdated(Single.just(intent).map { image })
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, intent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            ProfileCreationFragment.REQUEST_PERMISSION_CAMERA -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                presenter.cameraPermissionChanged(true)
                presenter.cameraClick()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val m = model.model
        iconSize = resources.getDimension(R.dimen.tv_avatar_size).toInt()
        presenter.initPresenter(m)
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?): Guidance {
        val title = getString(R.string.account_create_title, R.string.app_name)
        val breadcrumb = ""
        val description = getString(R.string.profile_message_warning)
        val m = model.model
        return Guidance(title, description, breadcrumb, AvatarDrawable.Builder()
            .withNameData(m.fullName, m.username)
            .withCircleCrop(true)
            .build(requireContext()))
    }

    override fun onProvideTheme(): Int {
        return R.style.Theme_Ring_Leanback_GuidedStep_First
    }

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        val context = requireContext()
        addEditTextAction(context, actions, USER_NAME, R.string.profile_name_hint, R.string.profile_name_hint)
        addAction(context, actions, CAMERA, getString(R.string.take_a_photo))
        addAction(context, actions, GALLERY, getString(R.string.open_the_gallery))
        addAction(context, actions, NEXT, getString(R.string.wizard_next), "", true)
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        when (action.id) {
            CAMERA -> presenter.cameraClick()
            GALLERY -> presenter.galleryClick()
            NEXT -> presenter.nextClick()
        }
    }

    override fun displayProfileName(profileName: String) {
        findActionById(USER_NAME)?.editDescription = profileName
        notifyActionChanged(findActionPositionById(USER_NAME))
    }

    override fun goToGallery() {
        pickProfilePicture.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    override fun goToPhotoCapture() {
        try {
            startActivityForResult(Intent(activity, CustomCameraActivity::class.java),
                ProfileCreationFragment.REQUEST_CODE_PHOTO)
        } catch (e: Exception) {
            AlertDialog.Builder(requireActivity())
                .setPositiveButton(android.R.string.ok, null)
                .setTitle(R.string.gallery_error_title)
                .setMessage(R.string.gallery_error_message)
                .show()
        }
    }

    override fun askPhotoPermission() {
        requestPermissions(arrayOf(Manifest.permission.CAMERA), ProfileCreationFragment.REQUEST_PERMISSION_CAMERA)
    }

    override fun goToNext(saveProfile: Boolean) {
        (activity as TVAccountWizard?)?.profileCreated(saveProfile)
    }

    override fun setProfile() {
        val m = model.model
        val username =  m.newAccount?.username ?:return
        val avatar = AvatarDrawable.Builder()
            .withPhoto(m.photo as Bitmap?)
            .withNameData(m.fullName, m.username)
            .withUri(net.jami.model.Uri(net.jami.model.Uri.JAMI_URI_SCHEME, username))
            .withCircleCrop(true)
            .build(requireContext())
        avatar.setInSize(iconSize)
        guidanceStylist.iconView?.setImageDrawable(avatar)
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        when (action.id) {
            USER_NAME -> {
                val username = action.editTitle.toString()
                presenter.fullNameUpdated(username)
                if (username.isEmpty()) action.title =
                    getString(R.string.profile_name_hint) else action.title = username
            }
            CAMERA -> presenter.cameraClick()
            GALLERY -> presenter.galleryClick()
        }
        return super.onGuidedActionEditedAndProceed(action)
    }

    override fun onGuidedActionEditCanceled(action: GuidedAction) {
        if (action.id == USER_NAME) {
            val username = action.editTitle.toString()
            presenter.fullNameUpdated(username)
            if (username.isEmpty()) action.title =
                getString(R.string.profile_name_hint) else action.title = username
        }
        super.onGuidedActionEditCanceled(action)
    }

    companion object {
        private const val USER_NAME = 1L
        private const val GALLERY = 2L
        private const val CAMERA = 3L
        private const val NEXT = 4L
    }
}