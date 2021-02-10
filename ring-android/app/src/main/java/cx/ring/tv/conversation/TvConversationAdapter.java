/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 *  Authors:    AmirHossein Naghshzan <amirhossein.naghshzan@savoirfairelinux.com>
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
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package cx.ring.tv.conversation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.bitmap.CenterInside;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.target.DrawableImageViewTarget;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import cx.ring.R;
import cx.ring.client.MediaViewerActivity;
import net.jami.conversation.ConversationPresenter;
import net.jami.model.CallContact;
import net.jami.model.ContactEvent;
import net.jami.model.DataTransfer;
import net.jami.model.Interaction;
import net.jami.model.Interaction.InteractionStatus;
import net.jami.model.Interaction.InteractionType;
import net.jami.model.SipCall;
import net.jami.model.TextMessage;
import cx.ring.service.DRingService;
import cx.ring.utils.AndroidFileUtils;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.GlideApp;
import cx.ring.utils.GlideOptions;
import cx.ring.utils.ResourceMapper;
import net.jami.utils.StringUtils;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class TvConversationAdapter extends RecyclerView.Adapter<TvConversationViewHolder> {
    private final static String TAG = TvConversationAdapter.class.getSimpleName();

    private final ArrayList<Interaction> mInteractions = new ArrayList<>();

    private final ConversationPresenter presenter;
    private final TvConversationFragment mTvConversationFragment;
    private final int hPadding;
    private final int vPadding;
    private final int mPictureMaxSize;
    private final GlideOptions PICTURE_OPTIONS;
    private RecyclerViewContextMenuInfo mCurrentLongItem = null;
    private int convColor = 0;

    private int expandedItemPosition = -1;
    private int lastDeliveredPosition = -1;
    private Observable<Long> timestampUpdateTimer;

    private static int[] msgBGLayouts = new int[] {
            R.drawable.textmsg_bg_out_last,
            R.drawable.textmsg_bg_out_middle,
            R.drawable.textmsg_bg_out_first,
            R.drawable.textmsg_bg_out,
            R.drawable.textmsg_bg_in_last,
            R.drawable.textmsg_bg_in_middle,
            R.drawable.textmsg_bg_in_first,
            R.drawable.textmsg_bg_in
    };

    public TvConversationAdapter(TvConversationFragment tvConversationFragment, ConversationPresenter presenter) {
        mTvConversationFragment = tvConversationFragment;
        this.presenter = presenter;
        Context context = tvConversationFragment.getActivity();
        Resources res = context.getResources();
        hPadding = res.getDimensionPixelSize(R.dimen.padding_medium);
        vPadding = res.getDimensionPixelSize(R.dimen.padding_small);
        mPictureMaxSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, res.getDisplayMetrics());
        int corner = (int) res.getDimension(R.dimen.conversation_message_radius);
        PICTURE_OPTIONS = new GlideOptions()
                .transform(new CenterInside())
                .fitCenter()
                .override(mPictureMaxSize)
                .transform(new RoundedCorners(corner));
        timestampUpdateTimer = Observable.interval(10, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .startWith(0L);
    }

    /**
     * Refreshes the data and notifies the changes
     *
     * @param list an arraylist of interactions
     */
    public void updateDataset(final List<Interaction> list) {
        Log.d(TAG, "updateDataset: list size=" + list.size());
        if (mInteractions.isEmpty()) {
            mInteractions.addAll(list);
        } else if (list.size() > mInteractions.size()) {
            mInteractions.addAll(list.subList(mInteractions.size(), list.size()));
        } else {
            mInteractions.clear();
            mInteractions.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void add(Interaction e) {
        boolean update = !mInteractions.isEmpty();
        mInteractions.add(e);
        notifyItemInserted(mInteractions.size() - 1);
        if (update)
            notifyItemChanged(mInteractions.size() - 2);
    }

    public void update(Interaction e) {
        if (!e.isIncoming() && e.getStatus() == InteractionStatus.SUCCESS) {
            notifyItemChanged(lastDeliveredPosition);
        }
        for (int i = mInteractions.size() - 1; i >= 0; i--) {
            Interaction element = mInteractions.get(i);
            if (e == element) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void remove(Interaction e) {
        for (int i = mInteractions.size() - 1; i >= 0; i--) {
            Interaction element = mInteractions.get(i);
            if (e.getId() == element.getId()) {
                mInteractions.remove(i);
                notifyItemRemoved(i);
                if (i > 0) {
                    notifyItemChanged(i - 1);
                }
                if (i != mInteractions.size()) {
                    notifyItemChanged(i);
                }
                break;
            }
        }
    }

    /**
     * Updates the contact photo to use for this conversation
     */
    public void setPhoto() {
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mInteractions.size() + 1;
    }

    @Override
    public long getItemId(int position) {
        return mInteractions.get(position).getId();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == mInteractions.size()) {
            return MessageType.HEADER.ordinal();
        }

        Interaction interaction = mInteractions.get(position);

        if (interaction != null) {
            switch (interaction.getType()) {
                case CONTACT:
                    return MessageType.CONTACT_EVENT.ordinal();
                case CALL:
                    return MessageType.CALL_INFORMATION.ordinal();
                case TEXT:
                    if (interaction.isIncoming()) {
                        return MessageType.INCOMING_TEXT_MESSAGE.ordinal();
                    } else {
                        return MessageType.OUTGOING_TEXT_MESSAGE.ordinal();
                    }
                case DATA_TRANSFER:
                    DataTransfer file = (DataTransfer) interaction;
                    int out = interaction.isIncoming() ? 0 : 4;
                    if (file.isComplete()) {
                        if (file.isPicture()) {
                            return MessageType.INCOMING_IMAGE.ordinal() + out;
                        } else if (file.isAudio()) {
                            return MessageType.INCOMING_AUDIO.ordinal() + out;
                        } else if (file.isVideo()) {
                            return MessageType.INCOMING_VIDEO.ordinal() + out;
                        }
                    }
                    return out;
            }
        }
        return MessageType.CALL_INFORMATION.ordinal();
    }

    @NonNull
    @Override
    public TvConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        MessageType type = MessageType.values()[viewType];
        ViewGroup v = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(type.layout, parent, false);
        return new TvConversationViewHolder(v, type);
    }

    @Override
    public void onBindViewHolder(@NonNull TvConversationViewHolder tvConversationViewHolder, int position) {
        if (position == mInteractions.size()) {
            return;
        }

        Interaction interaction = mInteractions.get(position);
        if (interaction == null)
            return;

        tvConversationViewHolder.compositeDisposable.clear();

        if (interaction.getType() == (InteractionType.TEXT)) {
            configureForTextMessage(tvConversationViewHolder, interaction, position);
        } else if (interaction.getType() == (InteractionType.CALL)) {
            configureForCallInfoTextMessage(tvConversationViewHolder, interaction);
        } else if (interaction.getType() == (InteractionType.CONTACT)) {
            configureForContactEvent(tvConversationViewHolder, interaction);
        } else if (interaction.getType() == (InteractionType.DATA_TRANSFER)) {
            configureForFileInfoTextMessage(tvConversationViewHolder, interaction, position);
        }
    }

    @Override
    public void onViewRecycled(@NonNull TvConversationViewHolder holder) {
        holder.itemView.setOnLongClickListener(null);
        if (holder.mImage != null) {
            holder.mImage.setOnLongClickListener(null);
        }
        if (holder.video != null) {
            holder.video.setOnClickListener(null);
            holder.video.setSurfaceTextureListener(null);
        }
        if (holder.surface != null) {
            holder.surface.release();
            holder.surface = null;
        }
        if (holder.player != null) {
            if (holder.player.isPlaying())
                holder.player.stop();
            holder.player.reset();
            holder.player.release();
            holder.player = null;
        }
        if (holder.mMsgTxt != null) {
            holder.mMsgTxt.setOnClickListener(null);
            holder.mMsgTxt.setOnLongClickListener(null);
        }
        if (expandedItemPosition == holder.getLayoutPosition()) {
            holder.mMsgDetailTxt.setVisibility(View.GONE);
            expandedItemPosition = -1;
        }
        holder.compositeDisposable.clear();
        super.onViewRecycled(holder);
    }

    public void setPrimaryColor(int color) {
        convColor = color;
        notifyDataSetChanged();
    }

    private static class RecyclerViewContextMenuInfo implements ContextMenu.ContextMenuInfo {
        RecyclerViewContextMenuInfo(int position, long id) {
            this.position = position;
            this.id = id;
        }
        final public int position;
        final public long id;
    }

    public boolean onContextItemSelected(MenuItem item) {
        TvConversationAdapter.RecyclerViewContextMenuInfo info = mCurrentLongItem;
        Interaction interaction = null;
        if (info == null) {
            return false;
        }
        try {
            interaction = mInteractions.get(info.position);
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Interaction array may be empty or null", e);
        }
        if (interaction == null)
            return false;
        if (interaction.getType() == (InteractionType.CONTACT))
            return false;

        switch (item.getItemId()) {
            case R.id.conv_action_download: {
                presenter.saveFile(interaction);
                break;
            }
            case R.id.conv_action_open: {
                presenter.openFile(interaction);
                break;
            }
            case R.id.conv_action_delete: {
                presenter.deleteConversationItem(interaction);
                break;
            }
            case R.id.conv_action_cancel_message: {
                presenter.cancelMessage(interaction);
                break;
            }
        }
        return true;
    }

    private void configureForFileInfoTextMessage(@NonNull final TvConversationViewHolder viewHolder,
                                                 @NonNull final Interaction interaction, int position) {
        DataTransfer file = (DataTransfer) interaction;
        File path = presenter.getDeviceRuntimeService().getConversationPath(file.getConversationId(), file.getStoragePath());
        file.setSize(path.length());

        String timeString = timestampToDetailString(viewHolder.itemView.getContext(), file.getTimestamp());
        viewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe(t -> {
            if (file.getStatus() == InteractionStatus.TRANSFER_FINISHED) {
                viewHolder.mMsgDetailTxt.setText(String.format("%s - %s",
                        timeString, Formatter.formatFileSize(viewHolder.itemView.getContext(), file.getTotalSize())));
            } else {
                viewHolder.mMsgDetailTxt.setText(String.format("%s - %s - %s",
                        timeString, Formatter.formatFileSize(viewHolder.itemView.getContext(), file.getTotalSize()),
                        ResourceMapper.getReadableFileTransferStatus(mTvConversationFragment.getActivity(), file.getStatus())));
            }
        }));

        TransferMsgType type;
        if (!file.isComplete()) {
            type = TransferMsgType.FILE;
        } else if (file.isPicture()) {
            type = TransferMsgType.IMAGE;
        } else if (file.isAudio()) {
            type = TransferMsgType.AUDIO;
        } else if (file.isVideo()) {
            type = TransferMsgType.VIDEO;
        } else {
            type = TransferMsgType.FILE;
        }

        viewHolder.compositeDisposable.clear();

        if (hasPermanentTimeString(file, position)) {
            viewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe(t -> {
                String timeSeparationString = timestampToDetailString(viewHolder.itemView.getContext(), file.getTimestamp());
                viewHolder.mMsgDetailTxtPerm.setText(timeSeparationString);
            }));
            viewHolder.mMsgDetailTxtPerm.setVisibility(View.VISIBLE);
        } else {
            viewHolder.mMsgDetailTxtPerm.setVisibility(View.GONE);
        }

        if (interaction.isIncoming()) {
            viewHolder.mAvatar.setImageBitmap(null);
            viewHolder.mAvatar.setVisibility(View.VISIBLE);
            CallContact contact = interaction.getContact();
            if (contact != null) {
                viewHolder.mAvatar.setImageDrawable(
                        mTvConversationFragment.getConversationAvatar(contact.getPrimaryNumber())
                );
            }
        } else {
            switch (interaction.getStatus()) {
                case SENDING:
                    viewHolder.mStatusIcon.setVisibility(View.VISIBLE);
                    viewHolder.mStatusIcon.setImageResource(R.drawable.baseline_circle_24);
                    break;
                case FAILURE:
                    viewHolder.mStatusIcon.setVisibility(View.VISIBLE);
                    viewHolder.mStatusIcon.setImageResource(R.drawable.round_highlight_off_24);
                    break;
                default:
                    viewHolder.mStatusIcon.setVisibility(View.VISIBLE);
                    viewHolder.mStatusIcon.setImageResource(R.drawable.baseline_check_circle_24);
                    lastDeliveredPosition = position;
            }
        }

        View longPressView = viewHolder.itemView;

        longPressView.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
            menu.setHeaderTitle(file.getDisplayName());
            mTvConversationFragment.onCreateContextMenu(menu, v, menuInfo);
            MenuInflater inflater = mTvConversationFragment.getActivity().getMenuInflater();
            inflater.inflate(R.menu.conversation_item_actions_file_tv, menu);
            if (!file.isComplete()) {
                menu.removeItem(R.id.conv_action_download);
                menu.removeItem(R.id.conv_action_share);
            }
        });
        longPressView.setOnLongClickListener(v -> {
            if (type == TransferMsgType.AUDIO || type == TransferMsgType.FILE) {
                mTvConversationFragment.updatePosition(viewHolder.getAdapterPosition());
            }
            mCurrentLongItem = new RecyclerViewContextMenuInfo(viewHolder.getAdapterPosition(), v.getId());
            return false;

        });

        if (type == TransferMsgType.IMAGE) {
            Context context = viewHolder.mImage.getContext();

            viewHolder.itemView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    viewHolder.itemView.setBackgroundResource(hasFocus? R.drawable.tv_item_selected_background : R.drawable.tv_item_unselected_background);
                    viewHolder.mAnswerLayout.animate().scaleY(hasFocus? 1.1f : 1f).scaleX(hasFocus? 1.1f : 1f);
                }
            });

            GlideApp.with(context)
                    .load(path)
                    .apply(PICTURE_OPTIONS)
                    .into(new DrawableImageViewTarget(viewHolder.mImage).waitForLayout());

            viewHolder.itemView.setOnClickListener(v -> {
                Uri contentUri = ContentUriHandler.getUriForFile(v.getContext(), ContentUriHandler.AUTHORITY_FILES, path);
                Intent i = new Intent(context, MediaViewerActivity.class);
                i.setAction(Intent.ACTION_VIEW).setDataAndType(contentUri, "image/*").setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                ActivityOptionsCompat options = ActivityOptionsCompat.
                        makeSceneTransitionAnimation(mTvConversationFragment.getActivity(), viewHolder.mImage, "picture");
                mTvConversationFragment.startActivityForResult(i, 3006, options.toBundle());
            });
            return;
        } else if (type == TransferMsgType.VIDEO) {
            Context context = viewHolder.itemView.getContext();

            viewHolder.itemView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    viewHolder.itemView.setBackgroundResource(hasFocus? R.drawable.tv_item_selected_background : R.drawable.tv_item_unselected_background);
                    viewHolder.mLayout.animate().scaleY(hasFocus? 1.1f : 1f).scaleX(hasFocus? 1.1f : 1f);
                }
            });

            if (viewHolder.player != null) {
                viewHolder.player.release();
            }
            final MediaPlayer player = MediaPlayer.create(context,
                    ContentUriHandler.getUriForFile(context, ContentUriHandler.AUTHORITY_FILES, path));
            if (player == null)
                return;
            viewHolder.player = player;
            final Drawable playBtn = ContextCompat.getDrawable(viewHolder.mLayout.getContext(), R.drawable.baseline_play_arrow_24).mutate();
            DrawableCompat.setTint(playBtn, Color.WHITE);
            ((CardView) viewHolder.mLayout).setForeground(playBtn);
            player.setOnCompletionListener(mp -> {
                if (player.isPlaying())
                    player.pause();
                player.seekTo(1);
                ((CardView) viewHolder.mLayout).setForeground(playBtn);
            });
            player.setOnVideoSizeChangedListener((mp, width, height) -> {
                Log.w(TAG, "OnVideoSizeChanged " + width + "x" + height);
                FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) viewHolder.video.getLayoutParams();
                int maxDim = Math.max(width, height);
                p.width = width * mPictureMaxSize / maxDim;
                p.height = height * mPictureMaxSize / maxDim;
                viewHolder.video.setLayoutParams(p);
            });
            if (viewHolder.video.isAvailable()) {
                if (viewHolder.surface == null) {
                    viewHolder.surface = new Surface(viewHolder.video.getSurfaceTexture());
                }
                player.setSurface(viewHolder.surface);
            }
            viewHolder.video.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    if (viewHolder.surface == null) {
                        viewHolder.surface = new Surface(surface);
                        player.setSurface(viewHolder.surface);
                    }
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    player.setSurface(null);
                    viewHolder.surface = null;
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                }
            });
            viewHolder.itemView.setOnClickListener(v -> {
                if (player.isPlaying()) {
                    player.pause();
                    ((CardView) viewHolder.mLayout).setForeground(playBtn);
                } else {
                    player.start();
                    ((CardView) viewHolder.mLayout).setForeground(null);
                }
            });
            player.seekTo(1);
            return;
        } else if (type == TransferMsgType.AUDIO) {
            Context context = viewHolder.itemView.getContext();

            viewHolder.itemView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    viewHolder.itemView.setBackgroundResource(hasFocus? R.drawable.tv_item_selected_background : R.drawable.tv_item_unselected_background);
                    viewHolder.mAudioInfoLayout.animate().scaleY(hasFocus? 1.1f : 1f).scaleX(hasFocus? 1.1f : 1f);
                }
            });

            try {
                ((ImageView) viewHolder.btnAccept).setImageResource(R.drawable.baseline_play_arrow_24);
                final MediaPlayer player = MediaPlayer.create(context,
                        ContentUriHandler.getUriForFile(context, ContentUriHandler.AUTHORITY_FILES, path));
                viewHolder.player = player;
                if (player != null) {
                    player.setOnCompletionListener(mp -> {
                        player.seekTo(0);
                        ((ImageView) viewHolder.btnAccept).setImageResource(R.drawable.baseline_play_arrow_24);
                    });
                    viewHolder.itemView.setOnClickListener((b) -> {
                        if (player.isPlaying()) {
                            player.pause();
                            ((ImageView) viewHolder.btnAccept).setImageResource(R.drawable.baseline_play_arrow_24);
                        } else {
                            player.start();
                            ((ImageView) viewHolder.btnAccept).setImageResource(R.drawable.baseline_pause_24);
                        }
                    });
                    viewHolder.compositeDisposable.add(Observable.interval(1L, TimeUnit.SECONDS)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .startWith(0L)
                            .subscribe(t -> {
                                int pS = player.getCurrentPosition() / 1000;
                                int dS = player.getDuration() / 1000;
                                viewHolder.mMsgTxt.setText(String.format(Locale.getDefault(),
                                        "%02d:%02d / %02d:%02d", pS / 60, pS % 60, dS / 60, dS % 60));
                            }));
                } else {
                    viewHolder.btnAccept.setOnClickListener(null);
                    viewHolder.updater = null;
                }
            } catch (IllegalStateException | NullPointerException e) {
                Log.e(TAG, "Error initializing player, it may have already been released: " + e.getMessage());
            }
            return;
        } else {
            viewHolder.itemView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    viewHolder.itemView.setBackgroundResource(hasFocus? R.drawable.tv_item_selected_background : R.drawable.tv_item_unselected_background);
                    viewHolder.mFileInfoLayout.animate().scaleY(hasFocus? 1.1f : 1f).scaleX(hasFocus? 1.1f : 1f);
                }
            });
        }

        if (file.getStatus().isError()) {
            viewHolder.mIcon.setImageResource(R.drawable.baseline_warning_24);
        } else {
            viewHolder.mIcon.setImageResource(R.drawable.baseline_attach_file_24);
        }

        viewHolder.mMsgTxt.setText(file.getDisplayName());

        if (file.getStatus() == InteractionStatus.TRANSFER_AWAITING_HOST) {
            viewHolder.mAnswerLayout.setVisibility(View.VISIBLE);
            viewHolder.btnAccept.setOnClickListener(v -> {
                if (!presenter.getDeviceRuntimeService().hasWriteExternalStoragePermission()) {
                    mTvConversationFragment.askWriteExternalStoragePermission();
                    return;
                }
                Context context = v.getContext();
                File cacheDir = context.getCacheDir();
                long spaceLeft = AndroidFileUtils.getSpaceLeft(cacheDir.toString());
                if (spaceLeft == -1L || file.getTotalSize() > spaceLeft) {
                    presenter.noSpaceLeft();
                    return;
                }
                context.startService(new Intent(DRingService.ACTION_FILE_ACCEPT)
                        .setClass(context.getApplicationContext(), DRingService.class)
                        .putExtra(DRingService.KEY_TRANSFER_ID, file.getDaemonId()));
            });
            viewHolder.btnRefuse.setOnClickListener(v -> {
                Context context = v.getContext();
                context.startService(new Intent(DRingService.ACTION_FILE_CANCEL)
                        .setClass(context.getApplicationContext(), DRingService.class)
                        .putExtra(DRingService.KEY_TRANSFER_ID, file.getDaemonId()));
            });
        } else {
            viewHolder.mAnswerLayout.setVisibility(View.GONE);
        }
    }

    /**
     * Configures the viewholder to display a classic text message, ie. not a call info text message
     *
     * @param convViewHolder The conversation viewHolder
     * @param interaction    The conversation element to display
     * @param position       The position of the viewHolder
     */
    private void configureForTextMessage(@NonNull final TvConversationViewHolder convViewHolder,
                                         @NonNull final Interaction interaction,
                                         int position) {
        final Context context = convViewHolder.itemView.getContext();
        TextMessage textMessage = (TextMessage)interaction;
        CallContact contact = textMessage.getContact();
        if (contact == null) {
            Log.e(TAG, "Invalid contact, not able to display message correctly");
            return;
        }

        convViewHolder.itemView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                convViewHolder.itemView.setBackgroundResource(hasFocus? R.drawable.tv_item_selected_background : R.drawable.tv_item_unselected_background);
                convViewHolder.mMsgTxt.animate().scaleY(hasFocus? 1.1f : 1f).scaleX(hasFocus? 1.1f : 1f);
            }
        });

        convViewHolder.mCid = textMessage.getConversation().getParticipant();
        String message = textMessage.getBody().trim();

        View longPressView = convViewHolder.itemView;
        longPressView.getBackground().setTintList(null);

        longPressView.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
            Date date = new Date(interaction.getTimestamp());
            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
            menu.setHeaderTitle(dateFormat.format(date));
            mTvConversationFragment.onCreateContextMenu(menu, v, menuInfo);
            MenuInflater inflater = mTvConversationFragment.getActivity().getMenuInflater();
            inflater.inflate(R.menu.conversation_item_actions_messages_tv, menu);

            if ((interaction).getStatus() == (InteractionStatus.SENDING)) {
                menu.removeItem(R.id.conv_action_delete);
            } else {
                menu.findItem(R.id.conv_action_delete).setTitle(R.string.menu_message_delete);
                menu.removeItem(R.id.conv_action_cancel_message);
            }
        });

        longPressView.setOnLongClickListener((View v) -> {
            if (expandedItemPosition == position) {
                expandedItemPosition = -1;
            }
            mTvConversationFragment.updatePosition(convViewHolder.getAdapterPosition());
            mCurrentLongItem = new RecyclerViewContextMenuInfo(convViewHolder.getAdapterPosition(), v.getId());
            return false;
        });

        final boolean isTimeShown = hasPermanentTimeString(textMessage, position);
        final SequenceType msgSequenceType = getMsgSequencing(position, isTimeShown);
        if (StringUtils.isOnlyEmoji(message)) {
            convViewHolder.mMsgTxt.getBackground().setAlpha(0);
            convViewHolder.mMsgTxt.setTextSize(32.0f);
            convViewHolder.mMsgTxt.setPadding(0, 0, 0, 0);
        } else {
            int resIndex = msgSequenceType.ordinal() + (textMessage.isIncoming() ? 1 : 0) * 4;
            convViewHolder.mMsgTxt.setBackground(ContextCompat.getDrawable(context, msgBGLayouts[resIndex]));
            if (convColor != 0 && !textMessage.isIncoming()) {
                convViewHolder.mMsgTxt.getBackground().setTint(convColor);
            }
            convViewHolder.mMsgTxt.getBackground().setAlpha(255);
            convViewHolder.mMsgTxt.setTextSize(16.f);
            convViewHolder.mMsgTxt.setPadding(hPadding, vPadding, hPadding, vPadding);
        }

        convViewHolder.mMsgTxt.setText(message);

        if (textMessage.isIncoming()) {
            convViewHolder.mAvatar.setImageBitmap(null);
            convViewHolder.mAvatar.setVisibility(View.VISIBLE);
        } else {
            switch (textMessage.getStatus()) {
                case SENDING:
                    convViewHolder.mStatusIcon.setVisibility(View.VISIBLE);
                    convViewHolder.mStatusIcon.setImageResource(R.drawable.baseline_circle_24);
                    break;
                case FAILURE:
                    convViewHolder.mStatusIcon.setVisibility(View.VISIBLE);
                    convViewHolder.mStatusIcon.setImageResource(R.drawable.round_highlight_off_24);
                    break;
                default:
                    if (position == lastOutgoingIndex()) {
                        convViewHolder.mStatusIcon.setVisibility(View.VISIBLE);
                        convViewHolder.mStatusIcon.setImageResource(R.drawable.baseline_check_circle_24);
                        lastDeliveredPosition = position;
                    } else {
                        convViewHolder.mStatusIcon.setVisibility(View.GONE);
                    }
            }
        }

        if (msgSequenceType == SequenceType.LAST || msgSequenceType == SequenceType.SINGLE) {
            setBottomMargin(convViewHolder.mMsgTxt, 8);
            if (textMessage.isIncoming()) {
                convViewHolder.mAvatar.setImageDrawable(
                        mTvConversationFragment.getConversationAvatar(contact.getPrimaryNumber())
                );
            }
        } else {
            setBottomMargin(convViewHolder.mMsgTxt, 0);
        }

        if (isTimeShown) {
            convViewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe(t -> {
                String timeSeparationString = timestampToDetailString(context, textMessage.getTimestamp());
                convViewHolder.mMsgDetailTxtPerm.setText(timeSeparationString);
            }));
            convViewHolder.mMsgDetailTxtPerm.setVisibility(View.VISIBLE);
        } else {
            convViewHolder.mMsgDetailTxtPerm.setVisibility(View.GONE);
            final boolean isExpanded = position == expandedItemPosition;
            if (isExpanded) {
                convViewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe(t -> {
                    String timeSeparationString = timestampToDetailString(context, textMessage.getTimestamp());
                    convViewHolder.mMsgDetailTxt.setText(timeSeparationString);
                }));
            }
            setItemViewExpansionState(convViewHolder, isExpanded);
            convViewHolder.itemView.setOnClickListener((View v) -> {
                if (convViewHolder.animator != null && convViewHolder.animator.isRunning()) {
                    return;
                }
                if (expandedItemPosition >= 0) {
                    int prev = expandedItemPosition;
                    notifyItemChanged(prev);
                }
                expandedItemPosition = isExpanded ? -1 : position;
                notifyItemChanged(expandedItemPosition);
            });
        }
    }

    private void configureForContactEvent(@NonNull final TvConversationViewHolder viewHolder, @NonNull final Interaction interaction) {
        ContactEvent event = (ContactEvent) interaction;
        if (event.event == ContactEvent.Event.ADDED) {
            viewHolder.mMsgTxt.setText(R.string.hist_contact_added);
        } else if (event.event == ContactEvent.Event.INCOMING_REQUEST) {
            viewHolder.mMsgTxt.setText(R.string.hist_invitation_received);
        }
        viewHolder.compositeDisposable.add(timestampUpdateTimer.subscribe(t -> {
            String timeSeparationString = timestampToDetailString(viewHolder.itemView.getContext(), event.getTimestamp());
            viewHolder.mMsgDetailTxt.setText(timeSeparationString);
        }));
    }

    /**
     * Configures the viewholder to display a call info text message, ie. not a classic text message
     *
     * @param convViewHolder The conversation viewHolder
     * @param interaction    The conversation element to display
     */
    private void configureForCallInfoTextMessage(@NonNull final TvConversationViewHolder convViewHolder,
                                                 @NonNull final Interaction interaction) {
        int pictureResID;
        String historyTxt;
        convViewHolder.mIcon.setScaleY(1);
        Context context = convViewHolder.itemView.getContext();

        View longPressView = convViewHolder.mCallInfoLayout;
        longPressView.getBackground().setTintList(null);

        SipCall call = (SipCall) interaction;

        if (call.isMissed()) {
            if (call.isIncoming()) {
                pictureResID = R.drawable.baseline_call_missed_24;
            } else {
                pictureResID = R.drawable.baseline_call_missed_outgoing_24;
                // Flip the photo upside down to show a "missed outgoing call"
                convViewHolder.mIcon.setScaleY(-1);
            }
            historyTxt = call.isIncoming() ?
                    context.getString(R.string.notif_missed_incoming_call) :
                    context.getString(R.string.notif_missed_outgoing_call);
        } else {
            pictureResID = (call.isIncoming()) ?
                    R.drawable.baseline_call_received_24 :
                    R.drawable.baseline_call_made_24;
            historyTxt = call.isIncoming() ?
                    context.getString(R.string.notif_incoming_call) :
                    context.getString(R.string.notif_outgoing_call);
        }

        convViewHolder.mCid = call.getConversation().getParticipant();
        convViewHolder.mIcon.setImageResource(pictureResID);
        convViewHolder.mHistTxt.setText(historyTxt);
        convViewHolder.mHistDetailTxt.setText(DateFormat.getDateTimeInstance()
                .format(call.getTimestamp())); // start date
    }

    /**
     * Computes the string to set in text details between messages, indicating time separation.
     *
     * @param timestamp The timestamp used to launch the computation with Date().getTime().
     *                  Can be the last received message timestamp for example.
     * @return The string to display in the text details between messages.
     */
    private String timestampToDetailString(Context context, long timestamp) {
        long diff = new Date().getTime() - timestamp;
        String timeStr;
        if (diff < DateUtils.WEEK_IN_MILLIS) {
            if (diff < DateUtils.DAY_IN_MILLIS && DateUtils.isToday(timestamp)) { // 11:32 A.M.
                timeStr = DateUtils.formatDateTime(context, timestamp, DateUtils.FORMAT_SHOW_TIME);
            } else {
                timeStr = DateUtils.formatDateTime(context, timestamp,
                        DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_NO_YEAR |
                                DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_TIME);
            }
        } else if (diff < DateUtils.YEAR_IN_MILLIS) { // JAN. 7, 11:02 A.M.
            timeStr = DateUtils.formatDateTime(context, timestamp,
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR |
                            DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_TIME);
        } else {
            timeStr = DateUtils.formatDateTime(context, timestamp,
                    DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE |
                            DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_WEEKDAY |
                            DateUtils.FORMAT_ABBREV_ALL);
        }
        return timeStr.toUpperCase(Locale.getDefault());
    }

    /**
     * Helper method to return the previous TextMessage relative to an initial position.
     *
     * @param position The initial position
     * @return the previous TextMessage if any, null otherwise
     */
    @Nullable
    private Interaction getPreviousMessageFromPosition(int position) {
        if (!mInteractions.isEmpty() && position > 0) {
            return mInteractions.get(position - 1);
        }
        return null;
    }

    /**
     * Helper method to return the next TextMessage relative to an initial position.
     *
     * @param position The initial position
     * @return the next TextMessage if any, null otherwise
     */
    @Nullable
    private Interaction getNextMessageFromPosition(int position) {
        if (!mInteractions.isEmpty() && position < mInteractions.size() - 1) {
            return mInteractions.get(position + 1);
        }
        return null;
    }

    private boolean isSeqBreak(@NonNull Interaction first, @NonNull Interaction second) {
        return StringUtils.isOnlyEmoji(first.getBody()) != StringUtils.isOnlyEmoji(second.getBody())
                || first.isIncoming() != second.isIncoming()
                || first.getType() != InteractionType.TEXT
                || second.getType() != InteractionType.TEXT;
    }

    private boolean isAlwaysSingleMsg(@NonNull Interaction msg) {
        return msg.getType() != InteractionType.TEXT
                || StringUtils.isOnlyEmoji(msg.getBody());
    }

    private SequenceType getMsgSequencing(final int i, final boolean isTimeShown) {
        Interaction msg = mInteractions.get(i);
        if (isAlwaysSingleMsg(msg)) {
            return SequenceType.SINGLE;
        }
        if (mInteractions.size() == 1 || i == 0) {
            if (mInteractions.size() == i + 1) {
                return SequenceType.SINGLE;
            }
            Interaction nextMsg = getNextMessageFromPosition(i);
            if (nextMsg != null) {
                if (isSeqBreak(msg, nextMsg) || hasPermanentTimeString(nextMsg, i + 1)) {
                    return SequenceType.SINGLE;
                } else {
                    return SequenceType.FIRST;
                }
            }
        } else if (mInteractions.size() == i + 1) {
            Interaction prevMsg = getPreviousMessageFromPosition(i);
            if (prevMsg != null) {
                if (isSeqBreak(msg, prevMsg) || isTimeShown) {
                    return SequenceType.SINGLE;
                } else {
                    return SequenceType.LAST;
                }
            }
        }
        Interaction prevMsg = getPreviousMessageFromPosition(i);
        Interaction nextMsg = getNextMessageFromPosition(i);
        if (prevMsg != null && nextMsg != null) {
            boolean nextMsgHasTime = hasPermanentTimeString(nextMsg, i + 1);
            if (((isSeqBreak(msg, prevMsg) || isTimeShown) && !(isSeqBreak(msg, nextMsg) || nextMsgHasTime))) {
                return SequenceType.FIRST;
            } else if (!isSeqBreak(msg, prevMsg) && !isTimeShown && isSeqBreak(msg, nextMsg)) {
                return SequenceType.LAST;
            } else if (!isSeqBreak(msg, prevMsg) && !isTimeShown && !isSeqBreak(msg, nextMsg)) {
                return nextMsgHasTime ? SequenceType.LAST : SequenceType.MIDDLE;
            }
        }
        return SequenceType.SINGLE;
    }

    private void setItemViewExpansionState(TvConversationViewHolder viewHolder, boolean expanded) {
        View view = viewHolder.mMsgDetailTxt;
        if (viewHolder.animator == null) {
            if (view.getHeight() == 0 && !expanded) {
                return;
            }
            viewHolder.animator = new ValueAnimator();
        }
        if (viewHolder.animator.isRunning()) {
            viewHolder.animator.reverse();
            return;
        }
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        viewHolder.animator.setIntValues(0, view.getMeasuredHeight());
        if (expanded) {
            view.setVisibility(View.VISIBLE);
        }
        viewHolder.animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ValueAnimator va = (ValueAnimator) animation;
                if ((Integer) va.getAnimatedValue() == 0) {
                    view.setVisibility(View.GONE);
                }
                viewHolder.animator = null;
            }
        });
        viewHolder.animator.setDuration(200);
        viewHolder.animator.addUpdateListener(animation -> {
            view.getLayoutParams().height = (Integer) animation.getAnimatedValue();
            view.requestLayout();
        });
        if (!expanded) {
            viewHolder.animator.reverse();
        } else {
            viewHolder.animator.start();
        }
    }

    private static void setBottomMargin(View view, int value) {
        int targetSize = (int) (value * view.getContext().getResources().getDisplayMetrics().density);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        params.bottomMargin = targetSize;
    }

    private boolean hasPermanentTimeString(final Interaction msg, int position) {
        if (msg == null) {
            return false;
        }
        Interaction prevMsg = getPreviousMessageFromPosition(position);
        return (prevMsg != null &&
                (msg.getTimestamp() - prevMsg.getTimestamp()) > 10 * DateUtils.MINUTE_IN_MILLIS);
    }

    private int lastOutgoingIndex() {
        int i;
        for (i = mInteractions.size() - 1; i >= 0; i--) {
            if (!mInteractions.get(i).isIncoming()) {
                break;
            }
        }
        return i;
    }

    private enum SequenceType {
        FIRST,
        MIDDLE,
        LAST,
        SINGLE;
    }

    public enum MessageType {
        INCOMING_FILE(R.layout.item_conv_file_peer_tv),
        INCOMING_IMAGE(R.layout.item_conv_image_peer_tv),
        INCOMING_AUDIO(R.layout.item_conv_audio_peer_tv),
        INCOMING_VIDEO(R.layout.item_conv_video_peer_tv),
        OUTGOING_FILE(R.layout.item_conv_file_me_tv),
        OUTGOING_IMAGE(R.layout.item_conv_image_me_tv),
        OUTGOING_AUDIO(R.layout.item_conv_audio_me_tv),
        OUTGOING_VIDEO(R.layout.item_conv_video_me_tv),
        CONTACT_EVENT(R.layout.item_conv_contact_tv),
        CALL_INFORMATION(R.layout.item_conv_call_tv),
        INCOMING_TEXT_MESSAGE(R.layout.item_conv_msg_peer_tv),
        OUTGOING_TEXT_MESSAGE(R.layout.item_conv_msg_me_tv),
        HEADER(R.layout.tv_header_blank);

        private final int layout;

        MessageType(int l) {
            layout = l;
        }
    }

    private enum TransferMsgType {
        FILE,
        IMAGE,
        AUDIO,
        VIDEO;
    }
}
