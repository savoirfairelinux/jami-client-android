package cx.ring.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Date;

import cx.ring.R;
import cx.ring.model.Conversation;
import cx.ring.model.HistoryCall;
import cx.ring.model.HistoryEntry;
import cx.ring.model.TextMessage;
import cx.ring.utils.Tuple;
import cx.ring.viewholders.SmartListViewHolder;

/**
 * Created by hdsousa on 17-03-16.
 */

public class SmartListAdapter2 extends RecyclerView.Adapter<SmartListViewHolder> {

    private ArrayList<Conversation> mConversations;
    private SmartListViewHolder.SmartListListeners listener;

    public SmartListAdapter2(ArrayList<Conversation> mConversations, SmartListViewHolder.SmartListListeners listener) {
        this.mConversations = mConversations;
        this.listener = listener;
    }

    @Override
    public SmartListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_smartlist, parent, false);

        return new SmartListViewHolder(v);
    }

    @Override
    public void onBindViewHolder(SmartListViewHolder holder, int position) {
        final Conversation conversation = mConversations.get(position);

        holder.convParticipants.setText(conversation.getContact().getDisplayName());
        long lastInteraction = conversation.getLastInteraction().getTime();
        holder.convTime.setText(lastInteraction == 0 ? "" :
                DateUtils.getRelativeTimeSpanString(lastInteraction, System.currentTimeMillis(), 0L, DateUtils.FORMAT_ABBREV_ALL));
        holder.convStatus.setText(getLastInteractionSummary(conversation, holder.itemView.getContext()));
        if (conversation.hasUnreadTextMessages()) {
            holder.convParticipants.setTypeface(null, Typeface.BOLD);
            holder.convTime.setTypeface(null, Typeface.BOLD);
            holder.convStatus.setTypeface(null, Typeface.BOLD);
        } else {
            holder.convParticipants.setTypeface(null, Typeface.NORMAL);
            holder.convTime.setTypeface(null, Typeface.NORMAL);
            holder.convStatus.setTypeface(null, Typeface.NORMAL);
        }

        holder.bind(conversation, listener);
    }

    @Override
    public int getItemCount() {
        return mConversations.size();
    }

    private String getLastInteractionSummary(Conversation conversation, Context context) {
        if (conversation.hasCurrentCall()) {
            return context.getString(R.string.ongoing_call);
        }
        Tuple<Date, String> d = new Tuple<>(new Date(0), null);

        for (HistoryEntry e : conversation.getHistory().values()) {
            Date entryDate = e.getLastInteractionDate();
            String entrySummary = getLastInteractionSummary(e, context);
            if (entryDate == null || entrySummary == null) {
                continue;
            }
            Tuple<Date, String> tmp = new Tuple<>(entryDate, entrySummary);
            if (d.first.compareTo(entryDate) < 0) {
                d = tmp;
            }
        }
        return d.second;
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
