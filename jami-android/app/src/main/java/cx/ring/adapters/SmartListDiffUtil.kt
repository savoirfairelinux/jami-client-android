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
package cx.ring.adapters

import androidx.recyclerview.widget.DiffUtil
import net.jami.services.ConversationFacade

class SmartListDiffUtil(
    private val mOldList: ConversationFacade.ConversationList,
    private val mNewList: ConversationFacade.ConversationList
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = mOldList.getCombinedSize()
    override fun getNewListSize(): Int = mNewList.getCombinedSize()

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = mOldList[oldItemPosition]
        val newItem = mNewList[newItemPosition]
        if (oldItem == null || newItem == null)
            return oldItem === newItem
        return newItem.uri == oldItem.uri
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        mNewList[newItemPosition] === mOldList[oldItemPosition]
}