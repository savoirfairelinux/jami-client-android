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
import android.util.Log
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.leanback.widget.GuidedActionsStylist
import cx.ring.R
import cx.ring.views.AvatarDrawable
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TVAccountExportStep2Fragment : GuidedStepSupportFragment() {

    private var peerAddress: String? = null

    interface OnReviewCallback {
        fun onIdentityConfirmation(confirm: Boolean)
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        val id = arguments?.getString(ARG_ACCOUNT_ID) ?: ""
        val name = arguments?.getString(ARG_REGISTERED_NAME) ?: id
        Log.d(TAG, "onCreateGuidance: id=$id, name=$name")

        return GuidanceStylist.Guidance(
            getString(R.string.export_side_main_title),
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

    override fun onCreateActionsStylist(): GuidedActionsStylist {
        return TVGuidedActionsStylist()
    }

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        peerAddress = arguments?.getString(ARG_PEER_ADDRESS)
        val isPasswordMode = peerAddress.isNullOrEmpty()
        Log.d(TAG, "onCreateActions: peerAddress=$peerAddress, isPasswordMode=$isPasswordMode")

        if (isPasswordMode) {
            actions.add(
                GuidedAction.Builder(requireContext())
                    .id(ACTION_ID_PASSWORD_NOTICE)
                    .title(getString(R.string.export_side_step2_password))
                    .infoOnly(true)
                    .enabled(false)
                    .focusable(false)
                    .build()
            )
        } else {
            actions.add(
                GuidedAction.Builder(requireContext())
                    .id(ACTION_ID_PEER_IP)
                    .title(getString(R.string.export_side_step2_advice_ip_only))
                    .description(peerAddress ?: "")
                    .infoOnly(true)
                    .enabled(false)
                    .focusable(false)
                    .build()
            )
        }

        actions.add(
            GuidedAction.Builder(requireContext())
                .id(GuidedAction.ACTION_ID_CONTINUE)
                .title(R.string.export_side_step2_confirm)
                .build()
        )

        actions.add(
            GuidedAction.Builder(requireContext())
                .id(GuidedAction.ACTION_ID_CANCEL)
                .title(android.R.string.cancel)
                .build()
        )
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        val callback = activity as? TVExportWizard ?: return
        when (action.id) {
            GuidedAction.ACTION_ID_CONTINUE -> {
                callback.onIdentityConfirmation(true)
            }
            GuidedAction.ACTION_ID_CANCEL -> {
                callback.onIdentityConfirmation(false)
            }
        }
    }

    fun update(peerAddress: String?) {
        val isPasswordMode = peerAddress.isNullOrEmpty()
        val isCurrentlyPasswordMode = findActionPositionById(ACTION_ID_PASSWORD_NOTICE) != -1
        val isCurrentlyIPMode = findActionPositionById(ACTION_ID_PEER_IP) != -1

        if (isPasswordMode && !isCurrentlyPasswordMode) {
            actions.clear()
            setupPasswordModeActions()
        } else if (!isPasswordMode && !isCurrentlyIPMode) {
            actions.clear()
            setupIPModeActions(peerAddress)
        } else if (!isPasswordMode && isCurrentlyIPMode) {
            val ipIndex = findActionPositionById(ACTION_ID_PEER_IP)
            if (ipIndex != -1) {
                actions[ipIndex].description = peerAddress
                notifyActionChanged(ipIndex)
            }
        }
    }

    private fun setupPasswordModeActions() {
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ACTION_ID_PASSWORD_NOTICE)
                .title(getString(R.string.export_side_step2_password).replace("\\n", "\n"))
                .infoOnly(true)
                .enabled(false)
                .focusable(false)
                .build()
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(GuidedAction.ACTION_ID_CONTINUE)
                .title(R.string.export_side_step2_confirm)
                .enabled(true)
                .build()
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(GuidedAction.ACTION_ID_CANCEL)
                .title(android.R.string.cancel)
                .build()
        )
        setActions(actions)
    }

    private fun setupIPModeActions(peerAddress: String?) {
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ACTION_ID_PEER_IP)
                .title(getString(R.string.export_side_step2_advice_ip_only))
                .description(peerAddress ?: "")
                .infoOnly(true)
                .enabled(false)
                .focusable(false)
                .build()
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(GuidedAction.ACTION_ID_CONTINUE)
                .title(R.string.export_side_step2_confirm)
                .enabled(true)
                .build()
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(GuidedAction.ACTION_ID_CANCEL)
                .title(android.R.string.cancel)
                .build()
        )
        setActions(actions)
    }

    override fun onProvideTheme(): Int = R.style.Theme_Ring_Leanback_GuidedStep_First

    companion object {
        private const val TAG = "TVAccountExportStep2Fragment"
        private const val ACTION_ID_PASSWORD_NOTICE = 1L
        private const val ACTION_ID_PEER_IP = 2L
        private const val ARG_PEER_ADDRESS = "peerAddress"
        private const val ARG_ACCOUNT_ID = "accountId"
        private const val ARG_REGISTERED_NAME = "registeredName"

        fun build(peerAddress: String?, accountId: String, registeredName: String): TVAccountExportStep2Fragment {
            Log.d(TAG, "Building TVAccountExportStep2Fragment with peerAddress=$peerAddress, accountId=$accountId, registeredName=$registeredName")
            return TVAccountExportStep2Fragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PEER_ADDRESS, peerAddress)
                    putString(ARG_ACCOUNT_ID, accountId)
                    putString(ARG_REGISTERED_NAME, registeredName)
                }
            }
        }
    }
}

class TVGuidedActionsStylist : GuidedActionsStylist() {
    override fun onBindViewHolder(viewHolder: ViewHolder, action: GuidedAction) {
        super.onBindViewHolder(viewHolder, action)

        viewHolder.titleView?.apply {
            maxLines = 5
            isSingleLine = false
            ellipsize = null
        }

        viewHolder.descriptionView?.apply {
            maxLines = 5
            isSingleLine = false
            ellipsize = null
        }
    }
}




