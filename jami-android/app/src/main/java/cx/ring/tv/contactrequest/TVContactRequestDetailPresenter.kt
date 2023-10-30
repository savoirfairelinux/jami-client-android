/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
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
package cx.ring.tv.contactrequest

import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import net.jami.smartlist.ConversationItemViewModel

class TVContactRequestDetailPresenter : AbstractDetailsDescriptionPresenter() {
    override fun onBindDescription(viewHolder: ViewHolder, item: Any) {
        val viewModel = item as ConversationItemViewModel?
        if (viewModel != null) {
            val id = viewModel.uriTitle
            val displayName = viewModel.title
            viewHolder.title.text = displayName
            if (displayName != id)
                viewHolder.subtitle.text = id
        }
    }
}