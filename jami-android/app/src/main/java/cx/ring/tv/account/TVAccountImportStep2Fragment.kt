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

import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import cx.ring.R
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint
import net.jami.services.ContactService
import net.jami.utils.StringUtils
import javax.inject.Inject
import javax.inject.Singleton


@AndroidEntryPoint
class TVAccountImportStep2Fragment : GuidedStepSupportFragment() {

    @Inject
    @Singleton
    lateinit var contactService: ContactService

    private var mPassword: String = ""

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        val args = arguments ?: return super.onCreateGuidance(savedInstanceState)
        val id = args.getString("id") ?: ""
        val name = args.getString("registeredName", id)
        return GuidanceStylist.Guidance(
            getString(R.string.import_side_main_title),
            id,
            null,
            AvatarDrawable.Builder()
                .withId(id)
                .withName(name)
                .withCircleCrop(true)
                .build(requireContext())
                .apply { setInSize(resources.getDimensionPixelSize(R.dimen.tv_avatar_size)) }
        )
    }

    fun update(
        needPassword: Boolean,
        id: String,
        registeredName: String?,
        error: String?
    ) {
        // update avatar:
        guidanceStylist.iconView?.setImageDrawable(AvatarDrawable.Builder()
            .withId(id)
            .withName(registeredName)
            .withCircleCrop(true)
            .build(requireContext())
            .apply { setInSize(resources.getDimensionPixelSize(R.dimen.tv_avatar_size)) })

        // update guidance:
        guidanceStylist.titleView?.text = registeredName

        // update actions:
        val position = findActionPositionById(ACTION_ID_CHECK)
        if (position != -1) {
            val action = actions[position]
            action.title = error ?: ""
            if (error != null) {
                action.icon = requireContext().getDrawable(R.drawable.ic_error_red)
            }
            notifyActionChanged(position)
        }
    }

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        val args = arguments ?: return
        val needPassword = args.getBoolean("needPassword", false)
        //val error = args.getString("error") ?: ""
        if (needPassword) {
            actions.add(
                GuidedAction.Builder(requireContext())
                    .id(ACTION_ID_PASSWORD)
                    .title(R.string.wizard_password_info)
                    .description(R.string.enter_password)
                    .descriptionEditInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
                    .descriptionEditable(true)
                    .editDescription("")
                    .inputType(InputType.TYPE_TEXT_VARIATION_PASSWORD)
                    .build()
            )
            actions.add(GuidedAction.Builder(requireContext())
                .id(ACTION_ID_CHECK)
                .enabled(false)
                .focusable(false)
                .infoOnly(true)
                .build())
        }
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(GuidedAction.ACTION_ID_CONTINUE)
                .title(R.string.import_side_step2_password_import)
                .enabled(!needPassword)
                .build()
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .clickAction(GuidedAction.ACTION_ID_CANCEL)
                .build()
        )
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        if (action.id == ACTION_ID_PASSWORD) {
            updatePasswordAction(action)
        }
        return GuidedAction.ACTION_ID_NEXT
    }

    override fun onGuidedActionEditCanceled(action: GuidedAction) {
        if (action.id == ACTION_ID_PASSWORD) {
            updatePasswordAction(action)
        }
    }

    private fun updatePasswordAction(action: GuidedAction) {
        val password = (action.editDescription?.toString() ?: "").apply { mPassword = this }
        Log.w(TAG, "updatePasswordAction: $password")
        action.description = if (password.isNotEmpty()) StringUtils.toPassword(password) else getString(R.string.enter_password)
        notifyActionChanged(findActionPositionById(action.id))

        // update continue action:
        val continueAction = findActionPositionById(GuidedAction.ACTION_ID_CONTINUE)
        if (continueAction != -1) {
            actions[continueAction].isEnabled = password.isNotEmpty()
            notifyActionChanged(continueAction)
        }
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        val callback = (activity as? TVImportWizard) ?: return
        val needPassword = arguments?.getBoolean("needPassword", false) == true
        when (action.id) {
            ACTION_ID_PASSWORD -> {
                /*val password = action.editTitle.toString()
                callback.onAuthentication(password)*/
            }
            GuidedAction.ACTION_ID_CONTINUE -> {
                if (needPassword) {
                    callback.onAuthentication(mPassword)
                } else {
                    callback.onAuthentication("")
                }
            }
            GuidedAction.ACTION_ID_CANCEL -> {
                callback.onCancel()
            }
        }
    }

    override fun onProvideTheme(): Int = R.style.Theme_Ring_Leanback_GuidedStep_First

    companion object {
        const val TAG = "TVAccountImportStep2Fragment"
        const val ACTION_ID_PASSWORD = 1L
        const val ACTION_ID_CHECK = 2L

        fun build(
            needPassword: Boolean,
            id: String,
            registeredName: String?,
            error: String?
        ): TVAccountImportStep2Fragment =
            TVAccountImportStep2Fragment().apply {
                arguments = Bundle().apply {
                    putBoolean("needPassword", needPassword)
                    putString("id", id)
                    putString("registeredName", registeredName)
                    putString("error", error)
                }
            }
    }
}
