package cx.ring.client;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import cx.ring.R;

public class ColorChooserBottomSheet extends BottomSheetDialogFragment {

    private static final int[] colors = {
            R.color.pink_500,
            R.color.purple_500, R.color.deep_purple_500,
            R.color.indigo_500, R.color.blue_500,
            R.color.cyan_500, R.color.teal_500,
            R.color.green_500, R.color.light_green_500,
            R.color.grey_500, R.color.lime_500,
            R.color.amber_500, R.color.deep_orange_500,
            R.color.brown_500, R.color.blue_grey_500
    };

    interface IColorSelected {
        void onColorSelected(int color);
    }

    private IColorSelected callback;

    public void setCallback(IColorSelected cb) {
        callback = cb;
    }

    private class ColorView extends RecyclerView.ViewHolder {
        ImageView view;
        int color;
        ColorView(@NonNull View itemView) {
            super(itemView);
            view = (ImageView) itemView;
            itemView.setOnClickListener(v -> {
                if (callback != null)
                    callback.onColorSelected(color);
                dismiss();
            });
        }
    }

    class ColorAdapter extends RecyclerView.Adapter<ColorView>  {
        @NonNull
        @Override
        public ColorView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_color, parent, false);
            return new ColorView(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ColorView holder, int position) {
            int color = colors[position];
            holder.color = getResources().getColor(color);
            ImageViewCompat.setImageTintList(holder.view, ColorStateList.valueOf(holder.color));
        }

        @Override
        public int getItemCount() {
            return colors.length;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        RecyclerView view = (RecyclerView) inflater.inflate(R.layout.frag_color_chooser, container);
        view.setAdapter(new ColorAdapter());
        return view;
    }
}
