package cx.ring.adapters;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import cx.ring.R;
import cx.ring.model.HistoryCall;
import cx.ring.model.HistoryEntry;
import cx.ring.model.TextMessage;
import cx.ring.smartlist.SmartListViewModel;
import cx.ring.utils.BitmapUtils;
import cx.ring.viewholders.SmartListViewHolder;

/**
 * Created by hdsousa on 17-03-16.
 */

public class SmartListAdapter extends RecyclerView.Adapter<SmartListViewHolder> {

    private ArrayList<SmartListViewModel> mSmartListViewModels;
    private SmartListViewHolder.SmartListListeners listener;

    public SmartListAdapter(ArrayList<SmartListViewModel> smartListViewModels, SmartListViewHolder.SmartListListeners listener) {
        this.mSmartListViewModels = smartListViewModels;
        this.listener = listener;
    }

    @Override
    public SmartListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_smartlist, parent, false);

        return new SmartListViewHolder(v);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onBindViewHolder(SmartListViewHolder holder, int position) {
        final SmartListViewModel smartListViewModel = mSmartListViewModels.get(position);

        holder.convParticipants.setText(smartListViewModel.getContactName());
        long lastInteraction = smartListViewModel.getLastInteractionTime();
        holder.convTime.setText(lastInteraction == 0 ? "" :
                DateUtils.getRelativeTimeSpanString(lastInteraction, System.currentTimeMillis(), 0L, DateUtils.FORMAT_ABBREV_ALL));
        if (smartListViewModel.hasOngoingCall()) {
            holder.convStatus.setText(holder.itemView.getContext().getString(R.string.ongoing_call));
        } else if (smartListViewModel.getLastInteraction() != null) {
            holder.convStatus.setText(getLastInteractionSummary(smartListViewModel.getLastEntryType(),
                    smartListViewModel.getLastInteraction(),
                    holder.itemView.getContext()));
        }
        if (smartListViewModel.hasUnreadTextMessage()) {
            holder.convParticipants.setTypeface(null, Typeface.BOLD);
            holder.convTime.setTypeface(null, Typeface.BOLD);
            holder.convStatus.setTypeface(null, Typeface.BOLD);
        } else {
            holder.convParticipants.setTypeface(null, Typeface.NORMAL);
            holder.convTime.setTypeface(null, Typeface.NORMAL);
            holder.convStatus.setTypeface(null, Typeface.NORMAL);
        }

        if (smartListViewModel.getPhotoUri() != null) {
            String test = smartListViewModel.getPhotoUri();
            String[] byteValues = test.substring(1, test.length() - 1).split(",");
            byte[] bytes = new byte[byteValues.length];

            for (int i = 0, len = bytes.length; i < len; i++) {
                bytes[i] = Byte.parseByte(byteValues[i].trim());
            }
            holder.photo.setImageBitmap(BitmapUtils.cropImageToCircle(bytes));
        } else {
            holder.photo.setImageResource(R.drawable.ic_contact_picture);
        }

        holder.bind(listener, smartListViewModel);
    }

    @Override
    public int getItemCount() {
        return mSmartListViewModels.size();
    }

    private String getLastInteractionSummary(int type, String lastInteraction, Context context) {
        switch (type) {
            case SmartListViewModel.TYPE_INCOMING_CALL:
                return String.format(context.getString(R.string.hist_in_call), lastInteraction);
            case SmartListViewModel.TYPE_OUTGOING_CALL:
                return String.format(context.getString(R.string.hist_out_call), lastInteraction);
            case SmartListViewModel.TYPE_INCOMING_MESSAGE:
                return lastInteraction;
            case SmartListViewModel.TYPE_OUTGOING_MESSAGE:
                return context.getText(R.string.you_txt_prefix) + " " + lastInteraction;
            default:
                return null;
        }
    }
}