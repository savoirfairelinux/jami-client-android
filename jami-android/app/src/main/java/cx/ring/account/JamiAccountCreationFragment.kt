/*
 *  Copyright (C) 2004-2026 Savoir-faire Linux Inc.
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

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.activityViewModels
import com.google.android.material.textfield.TextInputLayout
import cx.ring.R
import cx.ring.databinding.FragAccJamiCreateBinding
import cx.ring.mvp.BaseSupportFragment
import cx.ring.utils.AndroidFileUtils.loadBitmap
import cx.ring.utils.RegisteredNameFilter
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import net.jami.account.JamiAccountCreationPresenter
import net.jami.account.JamiAccountCreationView
import net.jami.account.JamiAccountCreationView.UsernameAvailabilityStatus

@AndroidEntryPoint
class JamiAccountCreationFragment :
    BaseSupportFragment<JamiAccountCreationPresenter, JamiAccountCreationView>(),
    JamiAccountCreationView {

    private val model: AccountCreationViewModel by activityViewModels()
    private var binding: FragAccJamiCreateBinding? = null
    private var restoreSystemBars: (() -> Unit)? = null
    private var imm: InputMethodManager? = null
    private val disposables = CompositeDisposable()

    private val pickProfilePicture =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                presenter.photoUpdated(loadBitmap(requireContext(), uri).map { it })
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragAccJamiCreateBinding.inflate(inflater, container, false).apply {
        backBtn.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }

        halo(identityTitle, 8f)
        halo(identitySubtitle, 6f)
        ViewCompat.setAccessibilityHeading(identityTitle, true)

        profilePhoto.setOnClickListener { openGallery() }
        gallery.setOnClickListener { openGallery() }
        camera.setOnClickListener { openCamera() }
        removePhoto.setOnClickListener { presenter.photoRemoved() }

        displayName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                presenter.fullNameUpdated(s.toString())
                updateAvatar()
            }
        })

        inputUsername.filters = arrayOf<InputFilter>(RegisteredNameFilter())
        inputUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                presenter.userNameChanged(s.toString())
                updateAvatar()
            }
        })
        inputUsername.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && createIdentityBtn.isEnabled) {
                imm?.hideSoftInputFromWindow(v.windowToken, 0)
                presenter.createAccount()
                true
            } else {
                false
            }
        }

        createIdentityBtn.setOnClickListener {
            imm?.hideSoftInputFromWindow(root.windowToken, 0)
            presenter.createAccount()
        }

        binding = this
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        restoreSystemBars = paintSystemBars()
        imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?

        requireActivity().supportFragmentManager.setFragmentResultListener(
            ProfilePhotoFragment.REQUEST_KEY_PHOTO,
            viewLifecycleOwner
        ) { _, bundle ->
            val uriString = bundle.getString(ProfilePhotoFragment.RESULT_URI)
            if (uriString != null) {
                presenter.photoUpdated(loadBitmap(requireContext(), uriString.toUri()).map { it })
            }
        }

        presenter.init(model.model)
        presenter.setPush(true)

        disposables.add(
            model.model.profileUpdates
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { updateAvatar() }
        )

        // Restore any existing values from the model (e.g. after returning from camera capture)
        val b = binding ?: return
        if (b.displayName.text.isNullOrEmpty() && model.model.fullName.isNotEmpty()) {
            b.displayName.setText(model.model.fullName)
        }
        if (b.inputUsername.text.isNullOrEmpty() && model.model.username.isNotEmpty()) {
            b.inputUsername.setText(model.model.username)
        }
        updateAvatar()
    }

    override fun onDestroyView() {
        restoreSystemBars?.invoke()
        restoreSystemBars = null
        disposables.clear()
        super.onDestroyView()
        binding = null
        imm = null
    }

    private fun openGallery() {
        pickProfilePicture.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun openCamera() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.wizard_container, ProfilePhotoFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun updateAvatar() {
        val b = binding ?: return
        val ctx = context ?: return
        val m = model.model
        val seed = m.fullName.ifBlank { m.username }
        val builder = AvatarDrawable.Builder()
            .withPhoto(m.photo as? Bitmap)
            .withNameData(m.fullName, m.username)
            .withCircleCrop(true)
            .withPresence(false)
        if (seed.isNotBlank()) {
            builder.withId(seed)
        }
        b.profilePhoto.setImageDrawable(builder.build(ctx))
        b.removePhoto.visibility = if (m.photo != null) View.VISIBLE else View.GONE
    }

    private fun halo(view: TextView, radiusDp: Float) {
        view.setShadowLayer(
            radiusDp * resources.displayMetrics.density,
            0f,
            0f,
            ContextCompat.getColor(view.context, R.color.welcome_background)
        )
    }

    @Suppress("DEPRECATION")
    private fun paintSystemBars(): () -> Unit {
        val activity = requireActivity()
        val window = activity.window
        val color = ContextCompat.getColor(activity, R.color.welcome_background)
        val luminous = resources.getBoolean(R.bool.welcome_dark)
        val bars = WindowCompat.getInsetsController(window, window.decorView)
        val lightStatus = bars.isAppearanceLightStatusBars
        val lightNavigation = bars.isAppearanceLightNavigationBars
        val container = activity.findViewById<ViewGroup>(R.id.wizard_container)
        val containerBackground = container?.background
        val clipChildren = container?.clipChildren ?: true
        val clipToPadding = container?.clipToPadding ?: true
        val statusColor = window.statusBarColor
        val navigationColor = window.navigationBarColor
        val veiled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && window.isNavigationBarContrastEnforced
        bars.isAppearanceLightStatusBars = !luminous
        bars.isAppearanceLightNavigationBars = !luminous
        container?.setBackgroundColor(color)
        container?.clipChildren = false
        container?.clipToPadding = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.statusBarColor = color
            window.navigationBarColor = color
        }
        return {
            bars.isAppearanceLightStatusBars = lightStatus
            bars.isAppearanceLightNavigationBars = lightNavigation
            container?.background = containerBackground
            container?.clipChildren = clipChildren
            container?.clipToPadding = clipToPadding
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = veiled
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                window.statusBarColor = statusColor
                window.navigationBarColor = navigationColor
            }
        }
    }

    override fun updateUsernameAvailability(status: UsernameAvailabilityStatus) {
        val b = binding ?: return
        b.ringUsernameAvailabilitySpinner.visibility = View.GONE
        when (status) {
            UsernameAvailabilityStatus.ERROR -> {
                b.inputUsernameTxtBox.isErrorEnabled = true
                b.inputUsernameTxtBox.error = getString(R.string.unknown_error)
                b.inputUsernameTxtBox.endIconMode = TextInputLayout.END_ICON_NONE
            }
            UsernameAvailabilityStatus.ERROR_USERNAME_INVALID -> {
                b.inputUsernameTxtBox.isErrorEnabled = true
                b.inputUsernameTxtBox.error = getString(R.string.invalid_username)
                b.inputUsernameTxtBox.endIconMode = TextInputLayout.END_ICON_NONE
            }
            UsernameAvailabilityStatus.ERROR_USERNAME_TAKEN -> {
                b.inputUsernameTxtBox.isErrorEnabled = true
                b.inputUsernameTxtBox.error = getString(R.string.username_already_taken)
                b.inputUsernameTxtBox.endIconMode = TextInputLayout.END_ICON_NONE
            }
            UsernameAvailabilityStatus.LOADING -> {
                b.inputUsernameTxtBox.isErrorEnabled = false
                b.inputUsernameTxtBox.endIconMode = TextInputLayout.END_ICON_NONE
                b.ringUsernameAvailabilitySpinner.visibility = View.VISIBLE
            }
            UsernameAvailabilityStatus.AVAILABLE -> {
                b.inputUsernameTxtBox.isErrorEnabled = false
                b.inputUsernameTxtBox.endIconMode = TextInputLayout.END_ICON_CUSTOM
                b.inputUsernameTxtBox.setEndIconDrawable(R.drawable.ic_good_green)
                b.inputUsernameTxtBox.setEndIconTintList(null)
            }
            UsernameAvailabilityStatus.RESET -> {
                b.inputUsernameTxtBox.isErrorEnabled = false
                b.inputUsernameTxtBox.endIconMode = TextInputLayout.END_ICON_NONE
            }
        }
    }

    override fun showInvalidPasswordError(display: Boolean) {}

    override fun showNonMatchingPasswordError(display: Boolean) {}

    override fun enableNextButton(enabled: Boolean) {
        binding?.createIdentityBtn?.apply {
            isEnabled = enabled
            alpha = if (enabled) 1.0f else 0.45f
        }
    }

    override fun goToAccountCreation() {
        val wizardActivity = activity as? AccountWizardActivity ?: return
        wizardActivity.createAccount()
    }

    override fun cancel() {
        activity?.onBackPressedDispatcher?.onBackPressed()
    }

    fun scrollPagerFragment() {
        // Kept for compatibility with legacy fragments if referenced
    }

    companion object {
        val TAG = JamiAccountCreationFragment::class.simpleName!!
    }
}