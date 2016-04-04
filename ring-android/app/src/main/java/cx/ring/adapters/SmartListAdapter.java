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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;

import cx.ring.R;
import cx.ring.model.Conversation;

public class SmartListAdapter extends BaseAdapter {
    private static String TAG = SmartListAdapter.class.getSimpleName();

    final private ArrayList<Conversation> mCalls = new ArrayList<>();
    final private ExecutorService mInfosFetcher;
    final private LruCache<Long, Bitmap> mMemoryCache;
    final private HashMap<Long, WeakReference<ContactPictureTask>> mRunningTasks = new HashMap<>();

    final private Context mContext;

    public SmartListAdapter(Context act, LruCache<Long, Bitmap> cache, ExecutorService pool) {
        super();
        mContext = act;
        mMemoryCache = cache;
        mInfosFetcher = pool;
    }

    public void updateDataset(final Collection<Conversation> list) {
        Log.i(TAG, "updateDataset " + list.size());

        if (list.size() == 0 && mCalls.size() == 0) {
            return;
        }

        mCalls.clear();
        for (Conversation c : list) {
            if (!c.getContact().isUnknown() || !c.getAccountsUsed().isEmpty() || c.getCurrentCall() != null)
                mCalls.add(c);
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
        TextView conv_participants;
        TextView conv_status;
        TextView conv_time;
        ImageView photo;
        int position;
        public Conversation conv;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(mContext).inflate(cx.ring.R.layout.item_calllist, null);

        ViewHolder holder = (ViewHolder) convertView.getTag();
        if (holder == null) {
            holder = new ViewHolder();
            holder.photo = (ImageView) convertView.findViewById(R.id.photo);
            holder.conv_participants = (TextView) convertView.findViewById(R.id.conv_participant);
            holder.conv_status = (TextView) convertView.findViewById(R.id.conv_last_item);
            holder.conv_time = (TextView) convertView.findViewById(R.id.conv_last_time);
            holder.position = -1;
            convertView.setTag(holder);
        }
        final ViewHolder h = holder;
        h.conv = mCalls.get(position);
        h.position = position;
        h.conv_participants.setText(h.conv.getContact().getDisplayName());
        long last_interaction = h.conv.getLastInteraction().getTime();
        h.conv_time.setText(last_interaction == 0 ? "" : DateUtils.getRelativeTimeSpanString(last_interaction, System.currentTimeMillis(), 0L, DateUtils.FORMAT_ABBREV_ALL));
        h.conv_status.setText(h.conv.getLastInteractionSumary(mContext.getResources()));
        if (h.conv.hasUnreadTextMessages()) {
            h.conv_participants.setTypeface(null, Typeface.BOLD);
            h.conv_time.setTypeface(null, Typeface.BOLD);
            h.conv_status.setTypeface(null, Typeface.BOLD);
        } else {
            h.conv_participants.setTypeface(null, Typeface.NORMAL);
            h.conv_time.setTypeface(null, Typeface.NORMAL);
            h.conv_status.setTypeface(null, Typeface.NORMAL);
        }

        final Long cid = h.conv.getContact().getId();
        Bitmap bmp = mMemoryCache.get(cid);
        if (bmp != null) {
            h.photo.setImageBitmap(bmp);
        } else {
            holder.photo.setImageBitmap(mMemoryCache.get(-1l));
            final WeakReference<ViewHolder> wh = new WeakReference<>(holder);
            final ContactPictureTask.PictureLoadedCallback cb = new ContactPictureTask.PictureLoadedCallback() {
                @Override
                public void onPictureLoaded(final Bitmap bmp) {
                    final ViewHolder fh = wh.get();
                    if (fh == null || fh.photo.getParent() == null)
                        return;
                    if (fh.conv.getContact().getId() == cid) {
                        fh.photo.post(new Runnable() {
                            @Override
                            public void run() {
                                fh.photo.setImageBitmap(bmp);
                                fh.photo.startAnimation(AnimationUtils.loadAnimation(fh.photo.getContext(), R.anim.contact_fadein));
                            }
                        });
                    }
                }
            };
            WeakReference<ContactPictureTask> wtask = mRunningTasks.get(cid);
            ContactPictureTask task = wtask == null ? null : wtask.get();
            if (task != null) {
                task.addCallback(cb);
            } else {
                task = new ContactPictureTask(mContext, h.photo, h.conv.getContact(), new ContactPictureTask.PictureLoadedCallback() {
                    @Override
                    public void onPictureLoaded(Bitmap bmp) {
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
}
