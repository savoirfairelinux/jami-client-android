/*
 *  Copyright (C) 2016 Savoir-faire Linux Inc.
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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

import java.util.List;

import cx.ring.smartlist.SmartListViewModel;

public class SmartListDiffUtil extends DiffUtil.Callback {

    public static final String KEY_LAST_INTERACTION_TIME = "LAST_INTERACTION_TIME";

    private List<SmartListViewModel> mOldList;
    private List<SmartListViewModel> mNewList;

    public SmartListDiffUtil(List<SmartListViewModel> oldList, List<SmartListViewModel> newList) {
        this.mOldList = oldList;
        this.mNewList = newList;
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
        return mNewList.get(newItemPosition).getUuid().equals(mOldList.get(oldItemPosition).getUuid());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        //Contents are always different as the lastInteractionTime change constantly
        return false;
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        SmartListViewModel newItem = mNewList.get(newItemPosition);
        SmartListViewModel oldItem = mOldList.get(oldItemPosition);
        Bundle diffBundle = new Bundle();
        //We want to update all the item only if there is another change than the date
        if (newItem.equals(oldItem)) {
            if (newItem.getLastInteractionTime() != oldItem.getLastInteractionTime()) {
                diffBundle.putLong(KEY_LAST_INTERACTION_TIME, newItem.getLastInteractionTime());
            }
        } else {
            diffBundle = null;
        }
        return diffBundle;
    }
}