/*
 *  Copyright (C) 2004-2016 Savoir-faire Linux Inc.
 *
 *  Authors: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *           Romain Bertozzi <romain.bertozzi@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package cx.ring.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;

import butterknife.BindView;
import butterknife.ButterKnife;
import cx.ring.R;
import cx.ring.model.HistoryCall;
import cx.ring.model.HistoryEntry;
import cx.ring.utils.Tuple;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;
import cx.ring.model.Phone;
import cx.ring.model.TextMessage;

public class SmartListAdapter extends BaseAdapter {

    private static String TAG = SmartListAdapter.class.getSimpleName();

    private final ArrayList<Conversation> mConversations = new ArrayList<>();
    private final ExecutorService mInfosFetcher;
    private final LruCache<Long, Bitmap> mMemoryCache;
    private final HashMap<Long, WeakReference<ContactDetailsTask>> mRunningTasks = new HashMap<>();

    private final Context mContext;

    public interface SmartListAdapterCallback {
        void pictureTapped(Conversation conversation);
    }

    private static SmartListAdapterCallback sDummyCallbacks = new SmartListAdapterCallback() {
        @Override
        public void pictureTapped(Conversation conversation) {
            // Stub
        }
    };

    private SmartListAdapterCallback mCallbacks = sDummyCallbacks;

    public SmartListAdapter(Context act, LruCache<Long, Bitmap> cache, ExecutorService pool) {
        super();
        mContext = act;
        mMemoryCache = cache;
        mInfosFetcher = pool;
    }

    private String stringFormatting(String query) {
        return Normalizer.normalize(query.toLowerCase(), Normalizer.Form.NFD).replaceAll("[\u0300-\u036F]", "");
    }

    public void emptyDataset() {
        mConversations.clear();
        notifyDataSetChanged();
    }

    public void updateDataset(final Collection<Conversation> list, String query) {
        Log.d(TAG, "updateDataset " + list.size() + " with query: " + query);

        if (list.isEmpty() && mConversations.isEmpty()) {
            return;
        }

        List<Conversation> newConversations = new ArrayList<>();

        for (Conversation c : list) {
            if (c.getContact() != null
                    || !c.getAccountsUsed().isEmpty()
                    || c.getCurrentCall() != null) {
                if (TextUtils.isEmpty(query) || c.getCurrentCall() != null) {
                    newConversations.add(c);
                } else if (c.getContact() != null) {
                    CallContact contact = c.getContact();
                    if (!TextUtils.isEmpty(contact.getDisplayName()) &&
                            stringFormatting(contact.getDisplayName()).contains(stringFormatting(query))) {
                        newConversations.add(c);
                    } else if (contact.getPhones() != null && !contact.getPhones().isEmpty()) {
                        ArrayList<Phone> phones = contact.getPhones();
                        for (Phone phone : phones) {
                            if (phone.getNumber() != null) {
                                String rawUriString = phone.getNumber().getRawUriString();
                                if (!TextUtils.isEmpty(rawUriString) &&
                                        stringFormatting(rawUriString.toLowerCase()).contains(stringFormatting(query))) {
                                    newConversations.add(c);
                                }
                            }
                        }
                    }
                }
            }
        }

        // only refresh display if there is new data to display
        if (areConversationListDifferent(mConversations, newConversations)) {
            mConversations.clear();
            mConversations.addAll(newConversations);
            notifyDataSetChanged();
        }

    }

    /**
     * @return true if list are different
     */
    private boolean areConversationListDifferent(List<Conversation> leftList, List<Conversation> rightList) {
        if (leftList == null || rightList == null) {
            return true;
        }

        if (leftList.size() != rightList.size()) {
            return true;
        }

        for (Conversation rightConversation : rightList) {
            if (rightConversation.hasCurrentCall() || rightConversation.hasUnreadTextMessages()) {
                return true;
            }

            int rightId = rightConversation.getUuid();
            CallContact rightContact = rightConversation.getContact();
            boolean found = false;
            for (Conversation leftConversation : leftList) {

                if (leftConversation.hasCurrentCall() || leftConversation.hasUnreadTextMessages()) {
                    return true;
                }

                int leftId = leftConversation.getUuid();
                CallContact leftContact = leftConversation.getContact();
                if (leftId == rightId
                        && leftContact != null
                        && leftContact.equals(rightContact)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int getCount() {
        return mConversations.size();
    }

    @Override
    public Conversation getItem(int position) {
        return mConversations.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public class ViewHolder {
        @BindView(R.id.conv_participant)
        TextView convParticipants;
        @BindView(R.id.conv_last_item)
        TextView convStatus;
        @BindView(R.id.conv_last_time)
        TextView convTime;
        @BindView(R.id.photo)
        ImageView photo;
        int position;
        public Conversation conv;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(cx.ring.R.layout.item_smartlist, null);
        }

        final ViewHolder holder = convertView.getTag() != null
                ? (ViewHolder) convertView.getTag()
                : new ViewHolder(convertView);

        holder.position = -1;
        convertView.setTag(holder);

        holder.conv = mConversations.get(position);
        holder.position = position;
        holder.convParticipants.setText(holder.conv.getContact().getDisplayName());
        long lastInteraction = holder.conv.getLastInteraction().getTime();
        holder.convTime.setText(lastInteraction == 0 ? "" :
                DateUtils.getRelativeTimeSpanString(lastInteraction, System.currentTimeMillis(), 0L, DateUtils.FORMAT_ABBREV_ALL));
        holder.convStatus.setText(getLastInteractionSummary(holder.conv, mContext.getResources()));
        if (holder.conv.hasUnreadTextMessages()) {
            holder.convParticipants.setTypeface(null, Typeface.BOLD);
            holder.convTime.setTypeface(null, Typeface.BOLD);
            holder.convStatus.setTypeface(null, Typeface.BOLD);
        } else {
            holder.convParticipants.setTypeface(null, Typeface.NORMAL);
            holder.convTime.setTypeface(null, Typeface.NORMAL);
            holder.convStatus.setTypeface(null, Typeface.NORMAL);
        }

        holder.photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallbacks.pictureTapped(holder.conv);
            }
        });

        final Long cid = holder.conv.getContact().getId();
        final Bitmap bmp = mMemoryCache.get(cid);
        if (bmp != null && cid != -1L) {
            holder.photo.setImageBitmap(bmp);
        } else {
            final WeakReference<ViewHolder> holderWeakRef = new WeakReference<>(holder);
            final ContactDetailsTask.DetailsLoadedCallback cb = new ContactDetailsTask.DetailsLoadedCallback() {
                @Override
                public void onDetailsLoaded(final Bitmap bmp, final String name) {
                    final ViewHolder holder = holderWeakRef.get();
                    if (holder == null || holder.photo.getParent() == null)
                        return;
                    if (holder.conv.getContact().getId() == cid) {
                        holder.photo.post(new Runnable() {
                            @Override
                            public void run() {
                                holder.photo.setImageBitmap(bmp);
                                holder.photo.startAnimation(AnimationUtils.loadAnimation(holder.photo.getContext(), R.anim.contact_fadein));
                            }
                        });

                        holder.convParticipants.post(new Runnable() {
                            @Override
                            public void run() {
                                holder.convParticipants.setText(name);
                                holder.photo.startAnimation(AnimationUtils.loadAnimation(holder.convParticipants.getContext(), R.anim.contact_fadein));
                            }
                        });
                    }
                }
            };
            WeakReference<ContactDetailsTask> wtask = mRunningTasks.get(cid);
            ContactDetailsTask task = wtask == null ? null : wtask.get();
            if (task != null && cid != -1L) {
                task.addCallback(cb);
            } else {
                task = new ContactDetailsTask(mContext, holder.photo, holder.convParticipants, holder.conv.getContact(), new ContactDetailsTask.DetailsLoadedCallback() {
                    @Override
                    public void onDetailsLoaded(Bitmap bmp, final String name) {
                        mMemoryCache.put(cid, bmp);
                        mRunningTasks.remove(cid);
                    }
                });
                task.addCallback(cb);
                mRunningTasks.put(cid, new WeakReference<>(task));
                mInfosFetcher.execute(task);
            }
        }
        return convertView;
    }

    public void setCallback(SmartListAdapterCallback callback) {
        if (callback == null) {
            this.mCallbacks = sDummyCallbacks;
            return;
        }
        this.mCallbacks = callback;
    }

    private String getLastInteractionSummary(Conversation conversation, Resources resources) {
        if (conversation.hasCurrentCall()) {
            return resources.getString(R.string.ongoing_call);
        }
        Tuple<Date, String> d = new Tuple<>(new Date(0), null);

        for (HistoryEntry e : conversation.getHistory().values()) {
            Date entryDate = e.getLastInteractionDate();
            String entrySummary = getLastInteractionSummary(e, resources);
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

    private String getLastInteractionSummary(HistoryEntry e, Resources resources) {
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
            return (msg.isIncoming() ? "" : resources.getText(R.string.you_txt_prefix) + " ") + msgString;
        }
        if (lastCallTimestamp > 0) {
            HistoryCall lastCall = e.getCalls().lastEntry().getValue();
            return String.format(resources.getString(lastCall.isIncoming()
                    ? R.string.hist_in_call
                    : R.string.hist_out_call), lastCall.getDurationString());
        }
        return null;
    }
}
