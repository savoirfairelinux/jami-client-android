package cx.ring.plugins.RecyclerPicker;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cx.ring.R;

public class RecyclerPickerAdapter extends RecyclerView.Adapter<RecyclerPickerAdapter.ItemViewHolder> {
    private List<Drawable> mList;
    private ItemClickListener mItemClickListener;
    private int mItemLayoutResource;
    private final LayoutInflater mInflater;

    public RecyclerPickerAdapter(Context ctx, @LayoutRes int recyclerItemLayout, ItemClickListener itemClickListener) {
        this.mItemLayoutResource = recyclerItemLayout;
        this.mItemClickListener = itemClickListener;
        mInflater = LayoutInflater.from(ctx);
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(mItemLayoutResource, parent, false);
        view.setOnClickListener(v -> mItemClickListener.onItemClicked(v));
        return new ItemViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.update(mList.get(position));
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if(mList != null) {
            return mList.size();
        } else {
            return 0;
        }
    }

    public void updateData(List<Drawable> newlist) {
        this.mList = newlist;
        notifyDataSetChanged();
    }


    public static class ItemViewHolder extends RecyclerView.ViewHolder{
        private ImageView itemImageView;
        private View view;
        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            this.view = itemView;
            itemImageView = itemView.findViewById(R.id.item_image_view);
        }

        void update(Drawable drawable) {
            itemImageView.setImageDrawable(drawable);
        }

        public View getView() {
            return view;
        }
    }

    public interface ItemClickListener {
        void onItemClicked(View view);
    }

}
