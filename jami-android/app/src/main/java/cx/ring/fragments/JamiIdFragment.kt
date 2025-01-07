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
package cx.ring.fragments

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.text.method.KeyListener
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cx.ring.R
import cx.ring.databinding.JamiIdLayoutBinding
import cx.ring.utils.ActionHelper.shareAccount
import cx.ring.utils.BiometricHelper
import cx.ring.utils.KeyboardVisibilityManager.showKeyboard
import cx.ring.utils.RegisteredNameFilter
import cx.ring.utils.TextUtils.copyAndShow
import cx.ring.viewmodel.JamiIdStatus
import cx.ring.viewmodel.JamiIdUiState
import cx.ring.viewmodel.JamiIdViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.jami.utils.Log

@AndroidEntryPoint
class JamiIdFragment : Fragment() {

    private lateinit var binding: JamiIdLayoutBinding
    private val jamiIdViewModel: JamiIdViewModel by lazy {
        ViewModelProvider(requireActivity())[JamiIdViewModel::class.java]
    }

    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}

        override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) =
            jamiIdViewModel.textChanged(text?.toString()!!)
    }

    // This var is used in a trick to get ellipsize to work. Allows to EditText.setEnabled.
    private lateinit var keyListener: KeyListener

    companion object {
        private val TAG = JamiIdFragment::class.simpleName!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        JamiIdLayoutBinding.inflate(inflater, container, false).apply {
            binding = this
            jamiIdEditText.filters = arrayOf<InputFilter>(RegisteredNameFilter())
            keyListener = jamiIdEditText.keyListener!!
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe the uiState and update the UI
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                var prevState: JamiIdUiState? = null

                jamiIdViewModel.uiState.collect { uiState ->

                    // Hide component before the account is loaded.
                    if (uiState.jamiIdStatus == null) binding.root.visibility = View.INVISIBLE
                    else binding.root.visibility = View.VISIBLE

                    if (uiState.jamiIdStatus == prevState?.jamiIdStatus) return@collect
                    prevState = uiState

                    when (uiState.jamiIdStatus) {
                        JamiIdStatus.USERNAME_NOT_DEFINED ->
                            setUsernameNotDefinedUiState(
                                username = uiState.username
                            )

                        JamiIdStatus.EDITING_USERNAME_INITIAL -> {
                            binding.jamiIdEditText.addTextChangedListener(textWatcher)
                            setEditingUsernameInitialUiState(
                                typingUsername = uiState.editedUsername
                            )
                        }

                        JamiIdStatus.EDITING_USERNAME_LOADING ->
                            setEditingUsernameLoadingUiState(
                                typingUsername = uiState.editedUsername
                            )

                        JamiIdStatus.EDITING_USERNAME_NOT_AVAILABLE ->
                            setEditingUsernameNotAvailableUiState(
                                typingUsername = uiState.editedUsername
                            )

                        JamiIdStatus.EDITING_USERNAME_AVAILABLE ->
                            setEditingUsernameAvailableUiState(
                                typingUsername = uiState.editedUsername
                            )

                        JamiIdStatus.USERNAME_DEFINED -> {
                            binding.jamiIdEditText.removeTextChangedListener(textWatcher)
                            setUsernameDefinedUiState(
                                username = uiState.username
                            )
                        }

                        else -> {
                            Log.e(TAG, "Unknown JamiIdStatus: ${uiState.jamiIdStatus}")
                            setUsernameDefinedUiState(
                                username = uiState.username
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Set the jamiIdEditText to the given username.
     * Adapt the size to the length of the jamiId.
     */
    private fun setEditTextUsername(username: String) {
        // Adapt the size to the length of the jamiId
        // If the jamiId is too long (more than 16 characters), use a smaller font size
        val jamiIdEditTextSize =
            if (username.length > 16)
                requireContext().resources.getDimension(R.dimen.jami_id_small_font_size)
            else
                requireContext().resources.getDimension(R.dimen.jami_id_regular_font_size)
        binding.jamiIdEditText.setTextSize(TypedValue.COMPLEX_UNIT_PX, jamiIdEditTextSize)

        // Set the jamiId
        if (binding.jamiIdEditText.text.toString() != username) {
            binding.jamiIdEditText.setText(username)
            binding.jamiIdEditText.setSelection(username.length)
        }
    }

    /**
     * Set the right drawable of the jamiIdEditText to the given drawable.
     * Resize it to 24dp x 24dp.
     */
    private fun setEditTextRightDrawable(@DrawableRes id: Int? = null) {
        binding.jamiIdEditText.setCompoundDrawables(
            null, null,
            id?.let {
                val dimensionPixelSize = requireContext().resources
                    .getDimensionPixelSize(R.dimen.jami_id_edit_text_drawable_size)
                AppCompatResources.getDrawable(requireContext(), id)!!.apply {
                    setBounds(0, 0, dimensionPixelSize, dimensionPixelSize)
                }
            },
            null
        )
    }

    private fun setValidateEnabled(enabled: Boolean) {
        binding.jamiIdValidateButton.isEnabled = enabled
        binding.jamiIdValidateButtonWrapper.backgroundTintList =
            requireContext().getColorStateList(
                if (enabled) R.color.jami_id_validate_background_enabled_color
                else R.color.jami_id_validate_background_disabled_color
            )
        binding.jamiIdValidateButton.imageTintList = requireContext().getColorStateList(
            if (enabled) R.color.jami_id_validate_icon_enabled_color
            else R.color.jami_id_validate_icon_disabled_color
        )
    }

    // Functions to define EditText
    // Will be red if the username is not available
    // Will be green if the username is available

    private fun setEditTextUsernameDefinedOrUsernameNotDefinedUiState() {
        binding.jamiIdEditText
            .setTextColor(requireContext().getColorStateList(R.color.jami_id_surface_color))
        binding.jamiIdEditText.backgroundTintList =
            requireContext().getColorStateList(R.color.transparent)
        setEditTextRightDrawable()
    }

    private fun setEditTextEditingUsernameLoadingUiState() {
        binding.jamiIdEditText
            .setTextColor(requireContext().getColorStateList(R.color.jami_id_surface_color))
        binding.jamiIdEditText.backgroundTintList =
            requireContext().getColorStateList(R.color.jami_id_edit_text_underline_color)
        setEditTextRightDrawable()
    }

    private fun setEditTextEditingUsernameNotAvailableUiState() {
        val redColorStateList = requireContext().getColorStateList(R.color.red_500)

        binding.jamiIdEditText.setTextColor(redColorStateList)
        binding.jamiIdEditText.backgroundTintList = redColorStateList
        setEditTextRightDrawable(R.drawable.ic_error_red)
    }

    private fun setEditTextEditingUsernameAvailableUiState() {
        val greenColorStateList = requireContext().getColorStateList(R.color.green_500)

        binding.jamiIdEditText.setTextColor(greenColorStateList)
        binding.jamiIdEditText.backgroundTintList = greenColorStateList
        setEditTextRightDrawable(R.drawable.ic_good_green)
    }

    private fun setEditTextEditingUsernameInitialUiState() {
        binding.jamiIdEditText
            .setTextColor(requireContext().getColorStateList(R.color.jami_id_surface_color))
        binding.jamiIdEditText.backgroundTintList =
            requireContext().getColorStateList(R.color.jami_id_edit_text_underline_color)
        setEditTextRightDrawable()
    }

    // Functions to define EditText Status
    // Helps the user to know what to do

    private fun setEditTextStatusEditingUsernameLoadingUiState() {
        binding.jamiIdEditTextStatus
            .setTextColor(requireContext().getColorStateList(R.color.jami_id_surface_color))
        binding.jamiIdEditTextStatus.text =
            requireContext().getString(R.string.jami_id_looking_for_availability)
    }

    private fun setEditTextStatusEditingUsernameNotAvailableUiState() {
        binding.jamiIdEditTextStatus
            .setTextColor(requireContext().getColorStateList(R.color.red_500))
        binding.jamiIdEditTextStatus.text =
            requireContext().getString(R.string.jami_id_not_available)
    }

    private fun setEditTextStatusEditingUsernameAvailableUiState() {
        binding.jamiIdEditTextStatus
            .setTextColor(requireContext().getColorStateList(R.color.green_500))
        binding.jamiIdEditTextStatus.text = requireContext().getString(R.string.jami_id_available)
    }

    private fun setEditTextStatusEditingUsernameInitialUiState() {
        binding.jamiIdEditTextStatus
            .setTextColor(requireContext().getColorStateList(R.color.jami_id_surface_color))
        binding.jamiIdEditTextStatus.text =
            requireContext().getString(R.string.jami_id_choose_username)
    }

    // Functions to define UI
    // Five states :
    // - Username not defined
    // - Editing username initial
    // - Editing username loading
    // - Editing username not available
    // - Editing username available
    // - Username defined

    private fun setUsernameNotDefinedUiState(username: String) {

        setEditTextUsername(username = username)
        setEditTextUsernameDefinedOrUsernameNotDefinedUiState()

        // Connect the copy, share, choose username and validate buttons
        binding.jamiIdCopyButton.setOnClickListener {
            copyAndShow(requireContext(), getString(R.string.clip_contact_uri), username)
        }
        binding.jamiIdShareButton.setOnClickListener {
            shareAccount(requireContext(), username)
        }
        binding.jamiIdChooseUsernameButton.setOnClickListener {
            jamiIdViewModel.onChooseUsernameClicked()
        }
        binding.jamiIdValidateButton.setOnClickListener {
            BiometricHelper.startAccountAuthentication(
                fragment = this,
                account = jamiIdViewModel.account,
                reason = getString(R.string.register_username)
            ) { scheme: String, password: String ->
                jamiIdViewModel.onValidateClicked(scheme, password)
            }
        }

        // Equivalent to setEnabled(false) but allows to trick a bug and get ellipsize to work.
        binding.jamiIdEditText.keyListener = null

        binding.jamiIdChooseUsernameButton.visibility = View.VISIBLE
        binding.jamiIdShareButtonWrapper.visibility = View.VISIBLE
        binding.jamiIdCopyButtonWrapper.visibility = View.VISIBLE
        binding.jamiIdEditTextStatus.visibility = View.INVISIBLE
        binding.jamiIdEditTextInfo.visibility = View.INVISIBLE
        binding.jamiIdValidateButtonWrapper.visibility = View.GONE
        binding.jamiIdProgressBar.visibility = View.GONE
    }

    private fun setEditingUsernameInitialUiState(typingUsername: String) {

        setEditTextUsername(username = typingUsername)
        setEditTextEditingUsernameInitialUiState()
        setEditTextStatusEditingUsernameInitialUiState()
        setValidateEnabled(false)

        binding.jamiIdEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                jamiIdViewModel.onLooseFocus()
            }
        }

        // Equivalent to setEnabled(true) but allows to trick a bug and get ellipsize to work.
        binding.jamiIdEditText.keyListener = keyListener

        binding.jamiIdShareButtonWrapper.visibility = View.GONE
        binding.jamiIdCopyButtonWrapper.visibility = View.GONE
        binding.jamiIdProgressBar.visibility = View.GONE
        binding.jamiIdChooseUsernameButton.visibility = View.INVISIBLE
        binding.jamiIdEditTextInfo.visibility = View.INVISIBLE
        binding.jamiIdEditTextStatus.visibility = View.VISIBLE
        binding.jamiIdValidateButtonWrapper.visibility = View.VISIBLE

        showKeyboard(binding.jamiIdEditText)
    }

    private fun setEditingUsernameLoadingUiState(typingUsername: String) {

        setEditTextUsername(username = typingUsername)
        setEditTextEditingUsernameLoadingUiState()
        setEditTextStatusEditingUsernameLoadingUiState()
        setValidateEnabled(false)

        binding.jamiIdEditTextInfo.visibility = View.INVISIBLE
        binding.jamiIdProgressBar.visibility = View.VISIBLE
    }

    private fun setEditingUsernameNotAvailableUiState(typingUsername: String) {

        setEditTextUsername(username = typingUsername)
        setEditTextEditingUsernameNotAvailableUiState()
        setEditTextStatusEditingUsernameNotAvailableUiState()
        setValidateEnabled(false)

        binding.jamiIdEditTextInfo.visibility = View.VISIBLE
        binding.jamiIdProgressBar.visibility = View.GONE
    }

    private fun setEditingUsernameAvailableUiState(typingUsername: String) {

        setEditTextUsername(username = typingUsername)
        setEditTextEditingUsernameAvailableUiState()
        setEditTextStatusEditingUsernameAvailableUiState()
        setValidateEnabled(true)

        binding.jamiIdEditTextInfo.visibility = View.INVISIBLE
        binding.jamiIdProgressBar.visibility = View.GONE
    }

    private fun setUsernameDefinedUiState(username: String) {

        setEditTextUsername(username = username)
        setEditTextUsernameDefinedOrUsernameNotDefinedUiState()

        binding.jamiIdWrapper.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT

        // Update listeners on copy and share buttons
        binding.jamiIdCopyButton.setOnClickListener {
            copyAndShow(requireContext(), getString(R.string.clip_contact_uri), username)
        }
        binding.jamiIdShareButton.setOnClickListener {
            shareAccount(requireContext(), username)
        }

        // Equivalent to setEnabled(false) but allows to trick a bug and get ellipsize to work .
        binding.jamiIdEditText.keyListener = null

        binding.jamiIdEditTextInfo.visibility = View.GONE
        binding.jamiIdEditTextStatus.visibility = View.GONE
        binding.jamiIdProgressBar.visibility = View.GONE
        binding.jamiIdChooseUsernameButton.visibility = View.GONE
        binding.jamiIdValidateButtonWrapper.visibility = View.GONE
        binding.jamiIdShareButtonWrapper.visibility = View.VISIBLE
        binding.jamiIdCopyButtonWrapper.visibility = View.VISIBLE
    }
}