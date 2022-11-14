/*
 *  Copyright (C) 2004-2022 Savoir-faire Linux Inc.
 *
 *  Author: Amirhossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import cx.ring.databinding.FragConversationMembersBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConversationMembersFragment : Fragment() {

    private var binding: FragConversationMembersBinding? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragConversationMembersBinding.inflate(inflater, container, false).apply {
            binding = this
        }.root

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        private val TAG = ConversationMembersFragment::class.simpleName!!
    }
}
