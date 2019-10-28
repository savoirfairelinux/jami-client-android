package cx.ring.Plugins.RecyclerPicker;

import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerPickerLayoutManager extends LinearLayoutManager {
    private RecyclerView recyclerView;
    private LinearSnapHelper snapHelper;
    private ItemSelectedListener listener;

    public RecyclerPickerLayoutManager(Context context, int orientation, boolean reverseLayout, ItemSelectedListener listener) {
        super(context, orientation, reverseLayout);
        this.listener = listener;
    }


    @Override
    public void onAttachedToWindow(RecyclerView view) {
        super.onAttachedToWindow(view);
        recyclerView = view;

        // Smart snapping
        snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onLayoutCompleted(RecyclerView.State state) {
        super.onLayoutCompleted(state);
        scaleDownView();
    }


    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler,
                                    RecyclerView.State state) {
        int scrolled = super.scrollHorizontallyBy(dx, recycler, state);

        if (getOrientation() == VERTICAL) {
            return 0;
        } else {
            scaleDownView();
            return scrolled;
        }
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
        // When scroll stops we notify on the selected item
        if (state == RecyclerView.SCROLL_STATE_IDLE) {

            // Find the closest child to the recyclerView center --> this is the selected item.
            int recyclerViewCenterX = getRecyclerViewCenterX();
            int minDistance = recyclerView.getWidth();
            int position = -1;
            for (int i=0; i< recyclerView.getChildCount(); i++) {
                View child = recyclerView.getChildAt(i);
                int childCenterX = getDecoratedLeft(child) + (getDecoratedRight(child) - getDecoratedLeft(child)) / 2;
                int newDistance = Math.abs(childCenterX - recyclerViewCenterX);
                if (newDistance < minDistance) {
                    minDistance = newDistance;
                    position = recyclerView.getChildLayoutPosition(child);
                }
            }
            listener.onItemSelected(position);
        }
    }

    private int getRecyclerViewCenterX() {
        Log.i("ZZZ", "recyclerView width: " + recyclerView.getWidth() + " Right-Left: " + (recyclerView.getRight()-recyclerView.getLeft()));
        return (recyclerView.getWidth())/2 + recyclerView.getLeft();
    }

    private void scaleDownView() {
        float mid = getWidth() / 2.0f;
        for (int i=0; i<getChildCount(); i++) {

            // Calculating the distance of the child from the center
            View child = getChildAt(i);
            float childMid = (getDecoratedLeft(child) + getDecoratedRight(child)) / 2.0f;
            float distanceFromCenter = Math.abs(mid - childMid);

            // The scaling formula
            float k = (float) Math.sqrt((double)(distanceFromCenter/getWidth()));
            k *= 1.5f;
            float scale = 1-k*0.66f;

            // Set scale to view
            child.setScaleX(scale);
            child.setScaleY(scale);
        }
    }

    public interface ItemSelectedListener {
        void onItemSelected(int position);
    }
}
