package cx.ring.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;

import cx.ring.R;
import cx.ring.client.ConversationActivity;
import cx.ring.model.Conversation;
import cx.ring.model.TextMessage;
import cx.ring.views.ConversationViewHolder;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationViewHolder> {
    private final static String TAG = ConversationAdapter.class.getSimpleName();

    private final Context mContext;
    private final ArrayList<Conversation.ConversationElement> mTexts = new ArrayList<>();
    private final LruCache<Long, Bitmap> mMemoryCache;
    private final ExecutorService mInfosFetcher;
    private final HashMap<Long, WeakReference<ContactPictureTask>> mRunningTasks = new HashMap<>();

    public ConversationAdapter(Context ctx, LruCache<Long, Bitmap> cache, ExecutorService pool) {
        mContext = ctx;
        mMemoryCache = cache;
        mInfosFetcher = pool;
    }

    public void updateDataset(final ArrayList<Conversation.ConversationElement> list, long rid) {
        Log.i(TAG, "updateDataset " + list.size() + " " + rid);
        if (list.size() == mTexts.size()) {
            if (rid != 0) {
                notifyDataSetChanged();
            }
            return;
        }
        int lastPos = mTexts.size();
        int newItmes = list.size() - lastPos;
        if (lastPos == 0 || newItmes < 0) {
            mTexts.clear();
            mTexts.addAll(list);
            notifyDataSetChanged();
        } else {
            for (int i = lastPos; i < list.size(); i++)
                mTexts.add(list.get(i));
            notifyItemRangeInserted(lastPos, newItmes);
        }
    }

    @Override
    public int getItemCount() {
        return mTexts.size();
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        Conversation.ConversationElement txt = mTexts.get(position);
        if (txt.text != null) {
            if (txt.text.isIncoming())
                return ConversationActivity.ConversationMessageType.INCOMING_TEXT_MESSAGE.getType();
            else
                return ConversationActivity.ConversationMessageType.OUTGOING_TEXT_MESSAGE.getType();
        }
        return ConversationActivity.ConversationMessageType.CALL_INFORMATION_TEXT_MESSAGE.getType();
    }

    @Override
    public ConversationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int res;
        if (viewType == ConversationActivity.ConversationMessageType.INCOMING_TEXT_MESSAGE.getType()) {
            res = R.layout.item_conv_msg_peer;
        } else if (viewType == ConversationActivity.ConversationMessageType.OUTGOING_TEXT_MESSAGE.getType()) {
            res = R.layout.item_conv_msg_me;
        } else {
            res = R.layout.item_conv_call;
        }
        ViewGroup v = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(res, parent, false);
        return new ConversationViewHolder(v, viewType);
    }

    @Override
    public void onBindViewHolder(ConversationViewHolder h, int position) {
        Conversation.ConversationElement txt = mTexts.get(position);

        if (txt.text != null) {
            boolean sep = false;
            boolean sep_same = false;
            if (position > 0 && mTexts.get(position - 1).text != null) {
                TextMessage prev = mTexts.get(position - 1).text;
                if (prev.isIncoming() && txt.text.isIncoming() && prev.getNumber().equals(txt.text.getNumber()))
                    sep_same = true;
                sep = true;
                if (position < mTexts.size() - 1) {
                    TextMessage next = mTexts.get(position + 1).text;
                    if (next != null) {
                        long diff = next.getTimestamp() - txt.text.getTimestamp();
                        if (diff < 60 * 1000)
                            sep = false;
                    }
                }
            }

            h.mCid = txt.text.getContact().getId();
            if (h.mPhoto != null)
                h.mPhoto.setImageBitmap(null);
            if (txt.text.isIncoming() && !sep_same) {
                final Long cid = txt.text.getContact().getId();
                Bitmap bmp = mMemoryCache.get(cid);
                if (bmp != null)
                    h.mPhoto.setImageBitmap(bmp);
                else {
                    h.mPhoto.setImageBitmap(mMemoryCache.get(-1L));
                    final WeakReference<ConversationViewHolder> wh = new WeakReference<>(h);
                    final ContactPictureTask.PictureLoadedCallback cb = new ContactPictureTask.PictureLoadedCallback() {
                        @Override
                        public void onPictureLoaded(final Bitmap bmp) {
                            final ConversationViewHolder fh = wh.get();
                            if (fh == null || fh.mPhoto.getParent() == null)
                                return;
                            if (fh.mCid == cid) {
                                fh.mPhoto.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        fh.mPhoto.setImageBitmap(bmp);
                                        fh.mPhoto.startAnimation(AnimationUtils.loadAnimation(fh.mPhoto.getContext(), R.anim.contact_fadein));
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
                        task = new ContactPictureTask(mContext, h.mPhoto, txt.text.getContact(), new ContactPictureTask.PictureLoadedCallback() {
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
            }
            h.mMsgTxt.setText(txt.text.getMessage());
            if (txt.text.getStatus() == TextMessage.Status.SENDING) {
                h.mMsgDetailTxt.setVisibility(View.VISIBLE);
                h.mMsgDetailTxt.setText(R.string.message_sending);
            } else {
                if (sep) {
                    h.mMsgDetailTxt.setVisibility(View.VISIBLE);
                    long now = new Date().getTime();
                    if (now - txt.text.getTimestamp() < 60L * 1000L)
                        h.mMsgDetailTxt.setText(R.string.time_just_now);
                    else if (now - txt.text.getTimestamp() < 3600L * 1000L)
                        h.mMsgDetailTxt.setText(DateUtils.getRelativeTimeSpanString(txt.text.getTimestamp(), now, 0, 0));
                    else
                        h.mMsgDetailTxt.setText(DateUtils.formatSameDayTime(txt.text.getTimestamp(), now, DateFormat.SHORT, DateFormat.SHORT));
                } else {
                    h.mMsgDetailTxt.setVisibility(View.GONE);
                }
            }
        } else {
            h.mCid = txt.call.getContactID();
            if (txt.call.isMissed()) {
                h.mPhoto.setImageResource(txt.call.isIncoming() ? R.drawable.ic_call_missed_black_24dp : R.drawable.ic_call_missed_outgoing_black_24dp);
                h.mHistTxt.setText(txt.call.isIncoming() ? mContext.getString(R.string.notif_missed_incoming_call, txt.call.getNumber())
                        : mContext.getString(R.string.notif_missed_outgoing_call, txt.call.getNumber()));
            } else {
                h.mPhoto.setImageResource(txt.call.isIncoming() ? R.drawable.ic_call_received_black_24dp : R.drawable.ic_call_made_black_24dp);
                h.mHistTxt.setText(txt.call.isIncoming() ? mContext.getString(R.string.notif_incoming_call_title, txt.call.getNumber())
                        : mContext.getString(R.string.notif_outgoing_call_title, txt.call.getNumber()));
            }
            h.mHistDetailTxt.setText(DateFormat.getDateTimeInstance().format(txt.call.getStartDate()));
        }
    }
}