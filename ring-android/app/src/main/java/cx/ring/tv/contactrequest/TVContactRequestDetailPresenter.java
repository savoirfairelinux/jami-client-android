/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
package cx.ring.tv.contactrequest;

import androidx.leanback.widget.AbstractDetailsDescriptionPresenter;

import cx.ring.tv.model.TVListViewModel;

public class TVContactRequestDetailPresenter extends AbstractDetailsDescriptionPresenter {

    @Override
    protected void onBindDescription(ViewHolder viewHolder, Object item) {
        TVListViewModel viewModel = (TVListViewModel) item;
        if (viewModel != null) {
            String id = viewModel.getContact().getRingUsername();
            String displayName = viewModel.getContact().getDisplayName();
            viewHolder.getTitle().setText(displayName);
            if (!displayName.equals(id))
                viewHolder.getSubtitle().setText(id);
        }
    }
}