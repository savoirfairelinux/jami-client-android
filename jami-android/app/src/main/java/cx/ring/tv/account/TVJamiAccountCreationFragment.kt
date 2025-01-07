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

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.activityViewModels
import androidx.leanback.widget.GuidanceStylist.Guidance
import androidx.leanback.widget.GuidedAction
import cx.ring.R
import cx.ring.account.AccountCreationViewModel
import cx.ring.utils.RegisteredNameFilter
import dagger.hilt.android.AndroidEntryPoint
import net.jami.account.JamiAccountCreationPresenter
import net.jami.account.JamiAccountCreationView
import net.jami.account.JamiAccountCreationView.UsernameAvailabilityStatus
import net.jami.utils.StringUtils

@AndroidEntryPoint
class TVJamiAccountCreationFragment : JamiGuidedStepFragment<JamiAccountCreationPresenter, JamiAccountCreationView>(), JamiAccountCreationView {
    private val model: AccountCreationViewModel by activityViewModels()
    private var mIsPasswordCorrect = true
    private var mPassword: String? = null
    private var mPasswordConfirm: String? = null
    private val mUsernameWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            val newName = s.toString()
            if (newName != resources.getString(R.string.register_username)) {
                val empty = newName.isEmpty()
                /* If the username is empty make sure to set isRegisterUsernameChecked
                 *  to False, this allows to create an account with an empty username */
                presenter.registerUsernameChanged(!empty)
                /* Send the newName even when empty (in order to reset the views) */
                presenter.userNameChanged(newName)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.init(model.model)
        presenter.registerUsernameChanged(false)
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?): Guidance {
        val title = getString(R.string.account_create_title)
        val breadcrumb = ""
        val description = getString(R.string.help_ring)
        val icon = resources.getDrawable(R.drawable.ic_contact_picture_fallback)
        return Guidance(title, description, breadcrumb, icon)
    }

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        val context = requireContext()
        addEditTextAction(context, actions, USERNAME, R.string.register_username, R.string.prompt_new_username)
        addDisabledNonFocusableAction(context, actions, CHECK)
        addPasswordAction(context,actions, PASSWORD, getString(R.string.prompt_new_password_optional), getString(R.string.enter_password))
        addPasswordAction(context, actions,PASSWORD_CONFIRMATION, getString(R.string.prompt_new_password_repeat),getString(R.string.enter_password))
        addAction(context, actions, CONTINUE, getString(R.string.action_create), "", true)
    }

    override fun onGuidedActionFocused(action: GuidedAction) {
        val view = getActionItemView(findActionPositionById(USERNAME)) as ViewGroup?
        if (view != null) {
            val text = view.findViewById<EditText>(androidx.leanback.R.id.guidedactions_item_title)
            text.removeTextChangedListener(mUsernameWatcher)
            if (action.id == USERNAME) {
                text.filters = arrayOf<InputFilter>(RegisteredNameFilter())
                text.addTextChangedListener(mUsernameWatcher)
            }
        }
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        onGuidedActionChange(action)
        return GuidedAction.ACTION_ID_NEXT
    }

    override fun onGuidedActionEditCanceled(action: GuidedAction) {
        onGuidedActionChange(action)
    }

    private fun onGuidedActionChange(action: GuidedAction) {
        when (action.id) {
            USERNAME -> usernameChanged(action)
            PASSWORD -> passwordChanged(action)
            PASSWORD_CONFIRMATION -> confirmPasswordChanged(action)
        }
    }

    private fun usernameChanged(action: GuidedAction) {
        val username = action.editTitle.toString()
        val view = getActionItemView(findActionPositionById(USERNAME)) as ViewGroup?
        if (view != null) {
            val text = view.findViewById<EditText>(androidx.leanback.R.id.guidedactions_item_title)
            text.removeTextChangedListener(mUsernameWatcher)
        }
        action.title = username.ifEmpty { getString(R.string.register_username) }
        notifyActionChanged(findActionPositionById(PASSWORD))
    }

    private fun passwordChanged(action: GuidedAction) {
        val password = action.editDescription.toString().apply { mPassword = this }
        action.description = if (password.isNotEmpty()) StringUtils.toPassword(password) else getString(R.string.account_enter_password)
        notifyActionChanged(findActionPositionById(PASSWORD))
        presenter.passwordChanged(password)
    }

    private fun confirmPasswordChanged(action: GuidedAction) {
        val passwordConfirm = action.editDescription.toString().apply { mPasswordConfirm = this }
        action.description = if (passwordConfirm.isNotEmpty()) StringUtils.toPassword(passwordConfirm) else getText(R.string.account_enter_password)
        notifyActionChanged(findActionPositionById(PASSWORD_CONFIRMATION))
        presenter.passwordConfirmChanged(passwordConfirm)
    }

    override fun onProvideTheme(): Int {
        return R.style.Theme_Ring_Leanback_GuidedStep_First
    }

    override fun updateUsernameAvailability(status: UsernameAvailabilityStatus) {
        val actionCheck = findActionById(CHECK) ?: return
        when (status) {
            UsernameAvailabilityStatus.ERROR -> {
                actionCheck.title = resources.getString(R.string.generic_error)
                displayErrorIconTitle(actionCheck, getString(R.string.unknown_error))
                enableNextButton(false)
            }
            UsernameAvailabilityStatus.ERROR_USERNAME_INVALID -> {
                displayErrorIconTitle(actionCheck, getString(R.string.invalid_username))
                enableNextButton(false)
            }
            UsernameAvailabilityStatus.ERROR_USERNAME_TAKEN -> {
                displayErrorIconTitle(
                    actionCheck,
                    getString(R.string.username_already_taken)
                )
                enableNextButton(false)
            }
            UsernameAvailabilityStatus.LOADING -> {
                actionCheck.icon = null
                actionCheck.title = resources.getString(R.string.looking_for_username_availability)
                enableNextButton(false)
            }
            UsernameAvailabilityStatus.AVAILABLE -> {
                actionCheck.title = getString(R.string.username_available)
                actionCheck.icon = requireContext().getDrawable(R.drawable.ic_good_green)
                enableNextButton(true)
            }
            UsernameAvailabilityStatus.RESET -> {
                actionCheck.icon = null
                actionCheck.title = ""
                enableNextButton(true)
                actionCheck.icon = null
            }
            else -> actionCheck.icon = null
        }
        notifyActionChanged(findActionPositionById(CHECK))
    }

    override fun showInvalidPasswordError(display: Boolean) {
        val action = findActionById(CONTINUE) ?: return
        if (display) {
            displayErrorIconDescription(action, getString(R.string.error_password_char_count))
            mIsPasswordCorrect = false
            action.isEnabled = false
        } else {
            action.description = ""
            mIsPasswordCorrect = true
            action.isEnabled = true
        }
        notifyActionChanged(findActionPositionById(CONTINUE))
    }

    override fun showNonMatchingPasswordError(display: Boolean) {
        val action = findActionById(CONTINUE) ?: return
        if (display) {
            displayErrorIconDescription(action, getString(R.string.error_passwords_not_equals))
            mIsPasswordCorrect = false
            action.isEnabled = false
        } else {
            action.description = ""
            mIsPasswordCorrect = true
            action.isEnabled = true
        }
        notifyActionChanged(findActionPositionById(CONTINUE))
    }

    override fun enableNextButton(enabled: Boolean) {
        if (mPassword.isNullOrEmpty() && mPasswordConfirm.isNullOrEmpty()) {
            mIsPasswordCorrect = true
        }
        val enabled = mIsPasswordCorrect && enabled
        Log.d(TAG, "enableNextButton: $enabled")
        val actionContinue = findActionById(CONTINUE) ?: return
        if (enabled) actionContinue.icon = null
        actionContinue.isEnabled = enabled
        notifyActionChanged(findActionPositionById(CONTINUE))
    }

    override fun goToAccountCreation() {
        (activity as TVAccountWizard?)?.createAccount()
    }

    override fun cancel() {
        activity?.onBackPressedDispatcher?.onBackPressed()
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == CONTINUE) {
            presenter.createAccount()
        }
    }

    private fun displayErrorIconTitle(action: GuidedAction, title: String) {
        action.icon = requireContext().getDrawable(R.drawable.ic_error_red)
        action.title = title
    }

    private fun displayErrorIconDescription(action: GuidedAction, description: String) {
        action.icon = requireContext().getDrawable(R.drawable.ic_error_red)
        action.description = description
    }

    companion object {
        private val TAG = TVJamiAccountCreationFragment::class.simpleName!!
        private const val USERNAME = 0L
        private const val PASSWORD = 1L
        private const val PASSWORD_CONFIRMATION = 2L
        private const val CHECK = 3L
        private const val CONTINUE = 4L
    }
}