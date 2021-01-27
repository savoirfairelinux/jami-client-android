/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package cx.ring.adapters;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

import net.jami.smartlist.SmartListViewModel;

public class SmartListDiffUtil extends DiffUtil.Callback {

    private final List<SmartListViewModel> mOldList;
    private final List<SmartListViewModel> mNewList;

    public SmartListDiffUtil(List<SmartListViewModel> oldList, List<SmartListViewModel> newList) {
        mOldList = oldList;
        mNewList = newList;
    }

    @Override
    public int getOldListSize() {
        return mOldList.size();
    }

    @Override
    public int getNewListSize() {
        return mNewList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        SmartListViewModel oldItem = mOldList.get(oldItemPosition);
        SmartListViewModel newItem = mNewList.get(newItemPosition);
        if (newItem.getHeaderTitle() != oldItem.getHeaderTitle())
            return false;
        if (newItem.getContact() != oldItem.getContact()) {
            if (newItem.getContact().size() != oldItem.getContact().size())
                return false;
            for (int i = 0; i < newItem.getContact().size(); i++) {
                if (newItem.getContact().get(i) != oldItem.getContact().get(i))
                    return false;
            }
        }
        return true;
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return mNewList.get(newItemPosition).equals(mOldList.get(oldItemPosition));
    }
}
