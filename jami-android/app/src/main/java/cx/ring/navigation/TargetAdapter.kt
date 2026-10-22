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
package cx.ring.navigation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import cx.ring.R
import cx.ring.databinding.ItemTargetEntryBinding
import cx.ring.views.AvatarDrawable
import net.jami.model.Target
import net.jami.model.TargetCapability

enum class TargetFilterType {
    ALL,
    CONTACTS,
    GROUPS,
    CIRCLES,
    BOARDS
}

class TargetAdapter(
    private val onTargetClick: (Target) -> Unit,
    private val onCallTargetClick: ((Target, Boolean) -> Unit)? = null
) : RecyclerView.Adapter<TargetAdapter.TargetViewHolder>() {

    private var allTargets: List<Target> = emptyList()
    private var displayedTargets: List<Target> = emptyList()
    private var currentFilter: TargetFilterType = TargetFilterType.ALL
    private var currentQuery: String = ""
    private var capability: TargetCapability = TargetCapability.CAN_MESSAGE

    fun setCapability(cap: TargetCapability) {
        capability = cap
        notifyDataSetChanged()
    }

    fun updateTargets(targets: List<Target>) {
        allTargets = targets
        applyFilter()
    }

    fun setFilterType(filterType: TargetFilterType) {
        currentFilter = filterType
        applyFilter()
    }

    fun setQuery(query: String) {
        currentQuery = query
        applyFilter()
    }

    private fun applyFilter() {
        displayedTargets = allTargets.filter { target ->
            val matchesFilter = when (currentFilter) {
                TargetFilterType.ALL -> true
                TargetFilterType.CONTACTS -> target is Target.ContactTarget
                TargetFilterType.GROUPS -> target is Target.GroupTarget
                TargetFilterType.CIRCLES -> target is Target.CircleTarget
                TargetFilterType.BOARDS -> target is Target.BoardTarget
            }
            val matchesQuery = if (currentQuery.isBlank()) true else {
                target.displayName.contains(currentQuery, ignoreCase = true) ||
                        (target.secondaryText?.contains(currentQuery, ignoreCase = true) == true)
            }
            matchesFilter && matchesQuery
        }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = displayedTargets.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TargetViewHolder {
        val binding = ItemTargetEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TargetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TargetViewHolder, position: Int) {
        holder.bind(displayedTargets[position])
    }

    inner class TargetViewHolder(
        private val binding: ItemTargetEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(target: Target) {
            val ctx = itemView.context

            // 1. Avatar of the target
            val vm = target.viewModel
            val avatarDrawable = if (vm != null && (target is Target.ContactTarget || target is Target.GroupTarget)) {
                AvatarDrawable.Builder()
                    .withViewModel(vm)
                    .withCircleCrop(true)
                    .build(ctx)
            } else {
                AvatarDrawable.Builder()
                    .withPhoto(vm?.conversationProfile?.avatar as? android.graphics.Bitmap)
                    .withNameData(target.displayName, target.id)
                    .withCircleCrop(true)
                    .build(ctx)
            }
            binding.targetAvatar.setImageDrawable(avatarDrawable)

            // 2. Icon of the target type (a contact's presence is its avatar badge)
            binding.targetIcon.setImageResource(when (target) {
                is Target.ContactTarget -> R.drawable.outline_person_24
                is Target.GroupTarget -> R.drawable.baseline_group_24
                is Target.CircleTarget -> R.drawable.ic_circles_24
                is Target.BoardTarget -> R.drawable.baseline_campaign_24
            })

            // 3. Name of the target
            binding.targetName.text = target.displayName

            // 4. Action icons depending on the mode (Call vs Chat)
            val isCallMode = capability == TargetCapability.CAN_CALL
            binding.targetChatIcon.isVisible = !isCallMode
            binding.targetVideoCallIcon.isVisible = isCallMode
            binding.targetAudioCallIcon.isVisible = isCallMode

            if (isCallMode) {
                binding.targetVideoCallIcon.setOnClickListener {
                    onCallTargetClick?.invoke(target, true) ?: onTargetClick(target)
                }
                binding.targetAudioCallIcon.setOnClickListener {
                    onCallTargetClick?.invoke(target, false) ?: onTargetClick(target)
                }
                binding.root.setOnClickListener {
                    onCallTargetClick?.invoke(target, false) ?: onTargetClick(target)
                }
            } else {
                // The function the row leads to, drawn as on the functions panel
                val (actionIcon, actionName) = when (capability) {
                    TargetCapability.CAN_SHARE_DOC -> R.drawable.baseline_article_24 to R.string.collab_new_document
                    TargetCapability.CAN_SHARE_LOCATION -> R.drawable.baseline_my_location_24 to R.string.function_location
                    else -> R.drawable.baseline_chat_24 to R.string.function_messages
                }
                binding.targetChatIcon.setImageResource(actionIcon)
                binding.targetChatIcon.contentDescription = ctx.getString(actionName)
                binding.targetChatIcon.setOnClickListener {
                    onTargetClick(target)
                }
                binding.root.setOnClickListener {
                    onTargetClick(target)
                }
            }
        }
    }
}
