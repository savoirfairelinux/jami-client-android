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
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import cx.ring.R
import cx.ring.databinding.FragAccProfileCreateBinding
import cx.ring.mvp.BaseSupportFragment
import cx.ring.utils.AndroidFileUtils.loadBitmap
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint
import net.jami.account.ProfileCreationPresenter
import net.jami.account.ProfileCreationView
import net.jami.model.Uri

@AndroidEntryPoint
class ProfileCreationFragment : BaseSupportFragment<ProfileCreationPresenter, ProfileCreationView>(), ProfileCreationView {
    private val model: AccountCreationViewModel by activityViewModels()
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

        requireActivity().supportFragmentManager.setFragmentResultListener(ProfilePhotoFragment.REQUEST_KEY_PHOTO, viewLifecycleOwner) { _, bundle ->
            val uriString = bundle.getString(ProfilePhotoFragment.RESULT_URI)
            if (uriString != null) {
                val uri = uriString.toUri()
                presenter.photoUpdated(loadBitmap(requireContext(), uri).map { it })
            }
        }

        if (binding.profilePhoto.drawable == null) {
            binding.removePhoto.visibility = View.GONE // Hide `delete` option if no photo.
            binding.profilePhoto.setImageDrawable(AvatarDrawable.Builder()
                .withNameData(model.model.fullName, model.model.username)
                .withCircleCrop(true)
                .build(view.context))
        }
        presenter.initPresenter(model.model)
    }

    override fun displayProfileName(profileName: String) {
        binding!!.username.setText(profileName)
    }

    override fun goToGallery() {
        pickProfilePicture.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    override fun goToPhotoCapture() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.wizard_container, ProfilePhotoFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun askPhotoPermission() {
        // Permission is now handled in ProfilePhotoFragment
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