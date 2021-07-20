package cx.ring.plugins.RecyclerPicker;

import android.graphics.drawable.Drawable;
import android.view.View;

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
        paddingLeft = RecyclerPickerUtils.getScreenWidth(mRecyclerView.getContext())/2 - RecyclerPickerUtils.dpToPx(mRecyclerView.getContext(), first/2);
        paddingRight = RecyclerPickerUtils.getScreenWidth(mRecyclerView.getContext())/2 - RecyclerPickerUtils.dpToPx(mRecyclerView.getContext(), last/2);
        updateRecyclerViewPadding();
    }

    private void setRecyclerViewPadding() {
        paddingLeft = RecyclerPickerUtils.getScreenWidth(mRecyclerView.getContext())/2;
        paddingRight = RecyclerPickerUtils.getScreenWidth(mRecyclerView.getContext())/2;
        updateRecyclerViewPadding();
    }

    private void updateRecyclerViewPadding(){
        mRecyclerView.setPadding(paddingLeft, 0, paddingRight, 0);
    }

    public void scrollToPosition(int position){
        mLayoutManager.scrollToPositionWithOffset(position, 0);
    }
}
