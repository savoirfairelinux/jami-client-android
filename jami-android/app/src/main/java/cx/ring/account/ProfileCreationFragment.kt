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
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import cx.ring.databinding.FragAccProfileCreateBinding
import cx.ring.mvp.BaseSupportFragment
import cx.ring.utils.AndroidFileUtils.createImageFile
import cx.ring.utils.AndroidFileUtils.loadBitmap
import cx.ring.utils.ContentUri.getUriForFile
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.core.Single
import net.jami.account.ProfileCreationPresenter
import net.jami.account.ProfileCreationView
import net.jami.model.Uri
import java.io.IOException

@AndroidEntryPoint
class ProfileCreationFragment : BaseSupportFragment<ProfileCreationPresenter, ProfileCreationView>(), ProfileCreationView {
    private val model: AccountCreationViewModel by activityViewModels()
    private var tmpProfilePhotoUri: android.net.Uri? = null
    private var binding: FragAccProfileCreateBinding? = null

    private val pickProfilePicture =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null)
                presenter.photoUpdated(loadBitmap(requireContext(), uri).map { it })
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragAccProfileCreateBinding.inflate(inflater, container, false).apply {
            gallery.setOnClickListener { presenter.galleryClick() }
            camera.setOnClickListener { presenter.cameraClick() }
            removePhoto.setOnClickListener { presenter.photoRemoved() }
            nextCreateAccount.setOnClickListener { presenter.nextClick() }
            skipCreateAccount.setOnClickListener { presenter.skipClick() }
            username.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    presenter.fullNameUpdated(s.toString())
                }
            })
            binding = this
        }.root

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = binding ?: return
        super.onViewCreated(view, savedInstanceState)
        if (binding.profilePhoto.drawable == null) {
            binding.removePhoto.visibility = View.GONE // Hide `delete` option if no photo.
            binding.profilePhoto.setImageDrawable(AvatarDrawable.Builder()
                .withNameData(model.model.fullName, model.model.username)
                .withCircleCrop(true)
                .build(view.context))
        }
        presenter.initPresenter(model.model)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        when (requestCode) {
            REQUEST_CODE_PHOTO -> if (resultCode == Activity.RESULT_OK) {
                if (tmpProfilePhotoUri == null) {
                    if (intent != null) {
                        val bundle = intent.extras
                        val b = if (bundle == null) null else bundle["data"] as Bitmap?
                        if (b != null) {
                            presenter.photoUpdated(Single.just(b))
                        }
                    }
                } else {
                    presenter.photoUpdated(loadBitmap(requireContext(), tmpProfilePhotoUri!!).map { b: Bitmap -> b })
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, intent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_PERMISSION_CAMERA -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                presenter.cameraPermissionChanged(true)
                presenter.cameraClick()
            }
        }
    }

    override fun displayProfileName(profileName: String) {
        binding!!.username.setText(profileName)
    }

    override fun goToGallery() {
        pickProfilePicture.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    override fun goToPhotoCapture() {
        try {
            val context = requireContext()
            val file = createImageFile(context)
            val uri = getUriForFile(context, file)
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                .putExtra(MediaStore.EXTRA_OUTPUT, uri)
                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                .putExtra("android.intent.extras.CAMERA_FACING", 1)
                .putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                .putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
            tmpProfilePhotoUri = uri
            startActivityForResult(intent, REQUEST_CODE_PHOTO)
        } catch (e: IOException) {
            Log.e(TAG, "Can't create temp file", e)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Could not start activity")
        }
    }

    override fun askPhotoPermission() {
        requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_PERMISSION_CAMERA)
    }

    override fun goToNext(saveProfile: Boolean) {
        (activity as AccountWizardActivity?)?.profileCreated(saveProfile)
    }

    override fun setProfile() {
        val binding = binding ?: return
        val m = model.model
        val username = m.newAccount?.username ?: return
        binding.profilePhoto.setImageDrawable(
            AvatarDrawable.Builder()
                .withPhoto(m.photo as Bitmap?)
                .withNameData(m.fullName, m.username)
                .withUri(Uri(Uri.JAMI_URI_SCHEME, username))
                .withCircleCrop(true)
                .build(requireContext())
        )
        binding.removePhoto.visibility = if (m.photo != null) View.VISIBLE else View.GONE
    }

    companion object {
        val TAG = ProfileCreationFragment::class.simpleName!!
        const val REQUEST_CODE_PHOTO = 1
        const val REQUEST_PERMISSION_CAMERA = 3
    }
}