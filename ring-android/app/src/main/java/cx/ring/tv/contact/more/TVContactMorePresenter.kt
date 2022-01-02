/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
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
package cx.ring.tv.contact.more

import cx.ring.utils.ConversationPath
import net.jami.model.Uri
import net.jami.mvp.RootPresenter
import net.jami.services.ConversationFacade
import javax.inject.Inject

class TVContactMorePresenter @Inject internal constructor(private val conversationService: ConversationFacade) :
    RootPresenter<TVContactMoreView>() {
    private var mAccountId: String? = null
    private var mUri: Uri? = null
    fun setContact(path: ConversationPath) {
        mAccountId = path.accountId
        mUri = path.conversationUri
    }

    fun clearHistory() {
        conversationService.clearHistory(mAccountId!!, mUri!!).subscribe()
        view?.finishView(false)
    }

    fun removeContact() {
        conversationService.removeConversation(mAccountId!!, mUri!!).subscribe()
        view?.finishView(true)
    }
}