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
package cx.ring.tv.contact.more;

import net.jami.facades.ConversationFacade;
import net.jami.model.Uri;
import net.jami.mvp.RootPresenter;

import javax.inject.Inject;

import cx.ring.utils.ConversationPath;

public class TVContactMorePresenter extends RootPresenter<TVContactMoreView> {

    private static final String TAG = TVContactMorePresenter.class.getSimpleName();

    private final ConversationFacade mConversationService;

    private String mAccountId;
    private Uri mUri;

    @Inject
    TVContactMorePresenter(ConversationFacade conversationService) {
        mConversationService = conversationService;
    }

    public void setContact(ConversationPath path) {
        mAccountId = path.getAccountId();
        mUri = path.getConversationUri();
    }

    public void clearHistory() {
        mConversationService.clearHistory(mAccountId, mUri).subscribe();
        getView().finishView(false);
    }

    public void removeContact() {
        mConversationService.removeConversation(mAccountId, mUri).subscribe();
        getView().finishView(true);
    }

}
