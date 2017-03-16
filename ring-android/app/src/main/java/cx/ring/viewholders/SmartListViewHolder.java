package cx.ring.viewholders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.model.Conversation;

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
    ImageView photo;

    public SmartListViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public void bind(final Conversation conversation, final SmartListListeners clickListener) {
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListener.onItemClick(conversation);
            }
        });
        itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                clickListener.onItemLongClick(conversation);
                return true;
            }
        });
        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListener.onPhotoClick(conversation);
            }
        });
    }

    public interface SmartListListeners {
        void onItemClick(Conversation conversation);
        void onItemLongClick(Conversation conversation);
        void onPhotoClick(Conversation conversation);
    }

}
