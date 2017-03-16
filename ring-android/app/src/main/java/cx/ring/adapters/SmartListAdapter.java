package cx.ring.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import cx.ring.R;
import cx.ring.model.HistoryCall;
import cx.ring.model.HistoryEntry;
import cx.ring.model.TextMessage;
import cx.ring.smartlist.SmartListViewModel;
import cx.ring.utils.CircleTransform;
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
            holder.convStatus.setText(getLastInteractionSummary(smartListViewModel.getLastInteraction(), holder.itemView.getContext()));
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

        Picasso.with(holder.itemView.getContext())
                .load(smartListViewModel.getPhotoUri())
                .error(R.drawable.ic_contact_picture)
                .placeholder(R.drawable.ic_contact_picture)
                .transform(new CircleTransform())
                .into(holder.photo);

        holder.bind(listener, smartListViewModel);
    }

    @Override
    public int getItemCount() {
        return mSmartListViewModels.size();
    }

    private String getLastInteractionSummary(HistoryEntry e, Context context) {
        long lastTextTimestamp = e.getTextMessages().isEmpty() ? 0 : e.getTextMessages().lastEntry().getKey();
        long lastCallTimestamp = e.getCalls().isEmpty() ? 0 : e.getCalls().lastEntry().getKey();
        if (lastTextTimestamp > 0 && lastTextTimestamp > lastCallTimestamp) {
            TextMessage msg = e.getTextMessages().lastEntry().getValue();
            String msgString = msg.getMessage();
            if (msgString != null && !msgString.isEmpty() && msgString.contains("\n")) {
                int lastIndexOfChar = msgString.lastIndexOf("\n");
                if (lastIndexOfChar + 1 < msgString.length()) {
                    msgString = msgString.substring(msgString.lastIndexOf("\n") + 1);
                }
            }
            return (msg.isIncoming() ? "" : context.getText(R.string.you_txt_prefix) + " ") + msgString;
        }
        if (lastCallTimestamp > 0) {
            HistoryCall lastCall = e.getCalls().lastEntry().getValue();
            return String.format(context.getString(lastCall.isIncoming()
                    ? R.string.hist_in_call
                    : R.string.hist_out_call), lastCall.getDurationString());
        }
        return null;
    }
}