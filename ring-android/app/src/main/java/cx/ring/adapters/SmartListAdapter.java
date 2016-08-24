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
import java.util.HashMap;
import java.util.concurrent.ExecutorService;

import cx.ring.R;
import cx.ring.model.CallContact;
import cx.ring.model.Conversation;

public class SmartListAdapter extends BaseAdapter {

    private static String TAG = SmartListAdapter.class.getSimpleName();

    private final ArrayList<Conversation> mCalls = new ArrayList<>();
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
        }
    };

    private SmartListAdapterCallback mCallbacks = sDummyCallbacks;

    public SmartListAdapter(Context act, LruCache<Long, Bitmap> cache, ExecutorService pool) {
        super();
        mContext = act;
        mMemoryCache = cache;
        mInfosFetcher = pool;
    }

    private String stringFormatting(String query){
        return Normalizer.normalize(query.toLowerCase(), Normalizer.Form.NFD).replaceAll("[\u0300-\u036F]", "");
    }

    public void updateDataset(final Collection<Conversation> list, String query) {
        Log.d(TAG, "updateDataset " + list.size() + " with query: " + query);

        if (list.isEmpty() && mCalls.isEmpty()) {
            return;
        }

        mCalls.clear();
        for (Conversation c : list) {
            if (!c.getContact().isUnknown()
                    || !c.getAccountsUsed().isEmpty()
                    || c.getCurrentCall() != null) {
                if (TextUtils.isEmpty(query) || c.getCurrentCall() != null) {
                    mCalls.add(c);
                } else if (c.getContact() != null) {
                    CallContact contact = c.getContact();
                    if (!TextUtils.isEmpty(contact.getDisplayName()) &&
                            stringFormatting(contact.getDisplayName()).contains(stringFormatting(query))) {
                        mCalls.add(c);
                    } else if (contact.getPhones() != null && !contact.getPhones().isEmpty()) {
                        ArrayList<CallContact.Phone> phones = contact.getPhones();
                        for (CallContact.Phone phone : phones) {
                            if (phone.getNumber() != null) {
                                String rawUriString = phone.getNumber().getRawUriString();
                                if (!TextUtils.isEmpty(rawUriString) &&
                                        stringFormatting(rawUriString.toLowerCase()).contains(stringFormatting(query))) {
                                    mCalls.add(c);
                                }
                            }
                        }
                    }
                }
            }
        }

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mCalls.size();
    }

    @Override
    public Conversation getItem(int position) {
        return mCalls.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public class ViewHolder {
        TextView convParticipants;
        TextView convStatus;
        TextView convTime;
        ImageView photo;
        int position;
        public Conversation conv;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(mContext).inflate(cx.ring.R.layout.item_smartlist, null);

        ViewHolder holder = (ViewHolder) convertView.getTag();
        if (holder == null) {
            holder = new ViewHolder();
            holder.photo = (ImageView) convertView.findViewById(R.id.photo);
            holder.convParticipants = (TextView) convertView.findViewById(R.id.conv_participant);
            holder.convStatus = (TextView) convertView.findViewById(R.id.conv_last_item);
            holder.convTime = (TextView) convertView.findViewById(R.id.conv_last_time);
            holder.position = -1;
            convertView.setTag(holder);
        }
        final ViewHolder h = holder;
        h.conv = mCalls.get(position);
        h.position = position;
        h.convParticipants.setText(h.conv.getContact().getDisplayName());
        long lastInteraction = h.conv.getLastInteraction().getTime();
        h.convTime.setText(lastInteraction == 0 ? "" : DateUtils.getRelativeTimeSpanString(lastInteraction, System.currentTimeMillis(), 0L, DateUtils.FORMAT_ABBREV_ALL));
        h.convStatus.setText(h.conv.getLastInteractionSumary(mContext.getResources()));
        if (h.conv.hasUnreadTextMessages()) {
            h.convParticipants.setTypeface(null, Typeface.BOLD);
            h.convTime.setTypeface(null, Typeface.BOLD);
            h.convStatus.setTypeface(null, Typeface.BOLD);
        } else {
            h.convParticipants.setTypeface(null, Typeface.NORMAL);
            h.convTime.setTypeface(null, Typeface.NORMAL);
            h.convStatus.setTypeface(null, Typeface.NORMAL);
        }

        holder.photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallbacks.pictureTapped(h.conv);
            }
        });

        final Long cid = h.conv.getContact().getId();
        final Bitmap bmp = mMemoryCache.get(cid);
        if (bmp != null && cid != -1L) {
            h.photo.setImageBitmap(bmp);
        } else {
            holder.photo.setImageBitmap(mMemoryCache.get(-1L));
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
                task = new ContactDetailsTask(mContext, h.photo, h.convParticipants, h.conv.getContact(), new ContactDetailsTask.DetailsLoadedCallback() {
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
}
