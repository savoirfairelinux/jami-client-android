/*
 * Copyright (C) 2026 Savoir-faire Linux Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.views

import android.view.View
import android.view.LayoutInflater
import androidx.core.view.children
import androidx.core.view.isVisible
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import cx.ring.R
import net.jami.smartlist.ConversationFilter

internal class ConversationFilterChips(
    private val group: ChipGroup,
    private val onSelected: (ConversationFilter) -> Unit,
    onCreateLabel: () -> Unit
) {
    private val addLabel = group.findViewById<Chip>(R.id.add_contact_label)
    private var showLabels = true
    private var canCreateLabel = true
    private val selectionListener = ChipGroup.OnCheckedStateChangeListener { _, ids ->
        val filter = ids.singleOrNull()?.let { group.findViewById<Chip>(it)?.tag as? ConversationFilter }
        if (filter != null) onSelected(filter)
    }

    init {
        mapOf(
            R.id.filter_all to ConversationFilter.All,
            R.id.filter_contacts to ConversationFilter.Contacts,
            R.id.filter_groups to ConversationFilter.Groups,
            R.id.filter_feeds to ConversationFilter.Feeds
        ).forEach { (id, filter) -> group.findViewById<Chip>(id).tag = filter }
        addLabel.setOnClickListener { onCreateLabel() }
        group.setOnCheckedStateChangeListener(selectionListener)
    }

    fun render(labels: List<String>, selected: ConversationFilter, canCreate: Boolean = true) {
        canCreateLabel = canCreate
        group.setOnCheckedStateChangeListener(null)
        group.children.filter { it.tag is ConversationFilter.Label }.toList().forEach(group::removeView)
        for (label in labels) {
            group.addView((LayoutInflater.from(group.context).inflate(R.layout.item_contact_label, group, false) as Chip).apply {
                id = View.generateViewId()
                text = label
                isCheckable = true
                tag = ConversationFilter.Label(label)
                isVisible = showLabels
            }, group.indexOfChild(addLabel))
        }
        val effective = if (selected is ConversationFilter.Label && selected.label !in labels)
            ConversationFilter.All else selected
        select(effective)
        addLabel.isVisible = showLabels && canCreateLabel
        group.setOnCheckedStateChangeListener(selectionListener)
        if (effective != selected) onSelected(effective)
    }

    fun select(filter: ConversationFilter) {
        group.children.firstOrNull { it.tag == filter }?.let { group.check(it.id) }
    }

    fun showLabelActions(show: Boolean) {
        showLabels = show
        group.children.filter { it.tag is ConversationFilter.Label }.forEach { it.isVisible = show }
        addLabel.isVisible = show && canCreateLabel
    }
}
