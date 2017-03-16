package cx.ring.viewholders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;

/**
 * Created by hdsousa on 17-03-16.
 */

public class SmartListViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.conv_participant)
    public TextView convParticipants;
    @BindView(R.id.conv_last_item)
    public TextView convStatus;
    @BindView(R.id.conv_last_time)
    public TextView convTime;
    @BindView(R.id.photo)
    public ImageView photo;

    public SmartListViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public void bind(final SmartListListeners clickListener) {
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListener.onItemClick(getAdapterPosition());

            }
        });
        itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                clickListener.onItemLongClick(getAdapterPosition());
                return true;
            }
        });
        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListener.onPhotoClick(getAdapterPosition());
            }
        });
    }

    public interface SmartListListeners {
        void onItemClick(int position);

        void onItemLongClick(int position);

        void onPhotoClick(int position);
    }

}
