package cx.ring.Plugins.RecyclerPicker;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecyclerPicker implements RecyclerPickerAdapter.ItemClickListener{
    private Context mContext;
    private RecyclerView mRecyclerView;
    private int mItemLayoutResource;
    private RecyclerPickerAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private int mOrientation;
    private RecyclerPickerLayoutManager.ItemSelectedListener mItemSelectedListener;
    private int paddingLeft;
    private int paddingRight;

    public RecyclerPicker(Context context, RecyclerView recyclerView,
                          @LayoutRes int recyclerItemLayout, int orientation,
                          RecyclerPickerLayoutManager.ItemSelectedListener listener) {
        this.mContext = context;
        this.mRecyclerView = recyclerView;
        this.mItemLayoutResource = recyclerItemLayout;
        this.mOrientation = orientation;
        this.mItemSelectedListener = listener;
        init();
    }

    private void init() {
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);
        // use a linear layout manager
        mLayoutManager = new RecyclerPickerLayoutManager(mContext, mOrientation,false,
                mItemSelectedListener);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new RecyclerPickerAdapter(mItemLayoutResource, this);
        mRecyclerView.setAdapter(mAdapter);
        setRecyclerViewPadding();
    }

    public void updateData(List<Drawable> newList){
        mAdapter.updateData(newList);
    }

    @Override
    public void onItemClicked(View view) {
        int position = mRecyclerView.getChildLayoutPosition(view);
        mRecyclerView.smoothScrollToPosition(position);
    }

    public void setFirstLastElementsWidths(int first, int last){
        paddingLeft = RecyclerPickerUtils.getScreenWidth(mContext)/2 - RecyclerPickerUtils.
                dpToPx(mContext, first/2);
        paddingRight = RecyclerPickerUtils.getScreenWidth(mContext)/2 - RecyclerPickerUtils.
                dpToPx(mContext, last/2);
        updateRecyclerViewPadding();
    }

    private void setRecyclerViewPadding() {
        paddingLeft = RecyclerPickerUtils.getScreenWidth(mContext)/2;
        paddingRight = RecyclerPickerUtils.getScreenWidth(mContext)/2;
        updateRecyclerViewPadding();
    }

    private void updateRecyclerViewPadding(){
        mRecyclerView.setPadding(paddingLeft, 0, paddingRight, 0);
    }
}
