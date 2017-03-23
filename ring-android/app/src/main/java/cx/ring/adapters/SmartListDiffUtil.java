package cx.ring.adapters;

import android.support.v7.util.DiffUtil;

import java.util.List;

import cx.ring.smartlist.SmartListViewModel;

/**
 * Created by hdsousa on 17-03-24.
 */

public class SmartListDiffUtil extends DiffUtil.Callback {

    private List<SmartListViewModel> oldList;
    private List<SmartListViewModel> newList;

    public SmartListDiffUtil(List<SmartListViewModel> oldList, List<SmartListViewModel> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).getUuid() == newList.get(newItemPosition).getUuid();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }

}
