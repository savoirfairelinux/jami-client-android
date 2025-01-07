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
package cx.ring.tv.contact

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.leanback.widget.Presenter
import cx.ring.R
import cx.ring.tv.conversation.TvConversationFragment
import cx.ring.utils.ConversationPath
import net.jami.model.Conversation
import net.jami.smartlist.ConversationItemViewModel

class TVContactDetailPresenter : Presenter() {
    override fun onCreateViewHolder(viewGroup: ViewGroup): ViewHolder = CustomViewHolder(
        LayoutInflater.from(viewGroup.context).inflate(R.layout.tv, viewGroup, false))

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        (viewHolder as CustomViewHolder).bind(item as ConversationItemViewModel)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {}

    private class CustomViewHolder(view: View) : ViewHolder(view) {
        fun bind(item: ConversationItemViewModel) {
            val fragment = TvConversationFragment.newInstance(ConversationPath.toBundle(item.accountId, item.uri))
            val fragmentManager = (view.context as FragmentActivity).supportFragmentManager
            fragmentManager.beginTransaction()
                .replace(R.id.content, fragment, FRAGMENT_TAG)
                .commit()
        }
    }

    companion object {
        const val FRAGMENT_TAG = "conversation"
    }
}