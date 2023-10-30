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
package cx.ring.plugins.RecyclerPicker;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecyclerPicker implements RecyclerPickerAdapter.ItemClickListener {
    private final RecyclerView mRecyclerView;
    private final RecyclerPickerAdapter mAdapter;
    private final RecyclerPickerLayoutManager mLayoutManager;
    private final RecyclerPickerLayoutManager.ItemSelectedListener mItemSelectedListener;
    private int paddingLeft;
    private int paddingRight;

    public RecyclerPicker(@NonNull RecyclerView recyclerView,
                          @LayoutRes int recyclerItemLayout, int orientation,
                          RecyclerPickerLayoutManager.ItemSelectedListener listener) {
        mRecyclerView = recyclerView;
        mItemSelectedListener = listener;
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);
        // use a linear layout manager
        mLayoutManager = new RecyclerPickerLayoutManager(mRecyclerView.getContext(), orientation,false,
                mItemSelectedListener);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new RecyclerPickerAdapter(mRecyclerView.getContext(), recyclerItemLayout, this);
        mRecyclerView.setAdapter(mAdapter);
        setRecyclerViewPadding();
    }

    public void updateData(List<Drawable> newList){
        mAdapter.updateData(newList);
    }

    @Override
    public void onItemClicked(View view) {
        int position = mRecyclerView.getChildLayoutPosition(view);
        int currentPos = mLayoutManager.findFirstVisibleItemPosition();
        if (position != currentPos) {
            mRecyclerView.smoothScrollToPosition(position);
            mItemSelectedListener.onItemSelected(position);
        } else {
            mItemSelectedListener.onItemClicked(position);
        }
    }

    public void setFirstLastElementsWidths(int first, int last){
        paddingLeft = getScreenWidth(mRecyclerView.getContext())/2 - dpToPx(mRecyclerView.getContext(), first/2);
        paddingRight = getScreenWidth(mRecyclerView.getContext())/2 - dpToPx(mRecyclerView.getContext(), last/2);
        updateRecyclerViewPadding();
    }

    private void setRecyclerViewPadding() {
        paddingLeft = getScreenWidth(mRecyclerView.getContext())/2;
        paddingRight = getScreenWidth(mRecyclerView.getContext())/2;
        updateRecyclerViewPadding();
    }

    private void updateRecyclerViewPadding(){
        mRecyclerView.setPadding(paddingLeft, 0, paddingRight, 0);
    }

    public void scrollToPosition(int position){
        mLayoutManager.scrollToPositionWithOffset(position, 0);
    }

    private static int getScreenWidth(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        if (windowManager != null) {
            windowManager.getDefaultDisplay().getMetrics(dm);
        }
        return dm.widthPixels;
    }

    private static int dpToPx(Context context, int value){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) value,
                context.getResources().getDisplayMetrics());
    }
}
