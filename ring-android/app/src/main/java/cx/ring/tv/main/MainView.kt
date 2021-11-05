/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Alexandre Lision <alexandre.lision@savoirfairelinux.com>
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
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.tv.main

import net.jami.navigation.HomeNavigationViewModel
import net.jami.smartlist.ConversationItemViewModel

interface MainView {
    fun showLoading(show: Boolean)
    fun refreshContact(index: Int, contact: ConversationItemViewModel)
    fun showContacts(contacts: List<ConversationItemViewModel>)
    fun showContactRequests(contactRequests: List<ConversationItemViewModel>)
    fun callContact(accountID: String, ringID: String)
    fun displayAccountInfo(viewModel: HomeNavigationViewModel)
    fun showExportDialog(pAccountID: String, hasPassword: Boolean)
    fun showProfileEditing()
    fun showAccountShare()
    fun showSettings()
}