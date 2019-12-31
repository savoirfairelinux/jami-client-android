/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
 *
 *  Authors:    Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *              Romain Bertozzi <romain.bertozzi@savoirfairelinux.com>
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
package cx.ring.adapters;

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
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

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

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import cx.ring.R;
import cx.ring.client.MediaViewerActivity;
import cx.ring.conversation.ConversationPresenter;
import cx.ring.fragments.ConversationFragment;
import cx.ring.model.CallContact;
import cx.ring.model.ContactEvent;
import cx.ring.model.DataTransfer;
import cx.ring.model.Interaction;
import cx.ring.model.Interaction.InteractionStatus;
import cx.ring.model.Interaction.InteractionType;
import cx.ring.model.SipCall;
import cx.ring.model.TextMessage;
import cx.ring.service.DRingService;
import cx.ring.utils.AndroidFileUtils;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.FileUtils;
import cx.ring.utils.GlideApp;
import cx.ring.utils.GlideOptions;
import cx.ring.utils.ResourceMapper;
import cx.ring.utils.StringUtils;
import cx.ring.utils.UiUpdater;
import cx.ring.views.ConversationViewHolder;

import static androidx.core.content.FileProvider.getUriForFile;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationViewHolder> {
    private final static String TAG = ConversationAdapter.class.getSimpleName();

    private final ArrayList<Interaction> mInteractions = new ArrayList<>();

    private final long timestampUpdateInterval = 10 * DateUtils.SECOND_IN_MILLIS;

    private final ConversationPresenter presenter;
    private final ConversationFragment conversationFragment;
    private final int hPadding;
    private final int vPadding;
    private final int mPictureMaxSize;
    private final GlideOptions PICTURE_OPTIONS;
    private RecyclerViewContextMenuInfo mCurrentLongItem = null;
    private int convColor = 0;

    private int expandedItemPosition = -1;
    private HashMap<Integer, ValueAnimator> animators = new HashMap<>();
    private int lastDeliveredPosition = -1;

    private static int[] msgBGLayouts = new int[] {
            R.drawable.textmsg_bg_out_first,
            R.drawable.textmsg_bg_out_middle,
            R.drawable.textmsg_bg_out_last,
            R.drawable.textmsg_bg_out,
            R.drawable.textmsg_bg_in_first,
            R.drawable.textmsg_bg_in_middle,
            R.drawable.textmsg_bg_in_last,
            R.drawable.textmsg_bg_in
    };

    public ConversationAdapter(ConversationFragment conversationFragment, ConversationPresenter presenter) {
        this.conversationFragment = conversationFragment;
        this.presenter = presenter;
        Context context = conversationFragment.getActivity();
        Resources res = context.getResources();
        hPadding = res.getDimensionPixelSize(R.dimen.padding_medium);
        vPadding = res.getDimensionPixelSize(R.dimen.padding_small);
        mPictureMaxSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, res.getDisplayMetrics());
        int corner = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, res.getDisplayMetrics());
        PICTURE_OPTIONS = new GlideOptions()
                .override(mPictureMaxSize)
                .transform(new CenterInside())
                .transform(new RoundedCorners(corner));
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
            if (e.getId().equals(element.getId())) {
                mInteractions.remove(i);
                notifyItemRemoved(i);
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
        return mInteractions.size();
    }

    @Override
    public long getItemId(int position) {
        return mInteractions.get(position).getId();
    }

    @Override
    public int getItemViewType(int position) {
        Interaction interaction = mInteractions.get(position);

        if (interaction != null) {
            switch (interaction.getType()) {
                case TEXT:
                    if (interaction.isIncoming()) {
                        return MessageType.INCOMING_TEXT_MESSAGE.ordinal();
                    } else {
                        return MessageType.OUTGOING_TEXT_MESSAGE.ordinal();
                    }
                case CALL:
                    return MessageType.CALL_INFORMATION.ordinal();
                case DATA_TRANSFER:
                    DataTransfer file = (DataTransfer) interaction;
                    if (file.isComplete()) {
                        if (file.isPicture()) {
                            return MessageType.IMAGE.ordinal();
                        } else if (file.isAudio()) {
                            return MessageType.AUDIO.ordinal();
                        } else if (file.isVideo()) {
                            return MessageType.VIDEO.ordinal();
                        } else {
                            return MessageType.FILE_TRANSFER.ordinal();
                        }
                    } else
                        return MessageType.FILE_TRANSFER.ordinal();
                case CONTACT:
                    return MessageType.CONTACT_EVENT.ordinal();
            }
        }
        return MessageType.CALL_INFORMATION.ordinal();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        MessageType type = MessageType.values()[viewType];
        ViewGroup v = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(type.layout, parent, false);
        return new ConversationViewHolder(v, type);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder conversationViewHolder, int position) {
        Interaction interaction = mInteractions.get(position);
        if (interaction == null)
            return;

        if (interaction.getType() == (InteractionType.TEXT)) {
            configureForTextMessage(conversationViewHolder, interaction, position);
        } else if (interaction.getType() == (InteractionType.CALL)) {
            configureForCallInfoTextMessage(conversationViewHolder, interaction);
        } else if (interaction.getType() == (InteractionType.CONTACT)) {
            configureForContactEvent(conversationViewHolder, interaction);
        } else if (interaction.getType() == (InteractionType.DATA_TRANSFER)) {
            configureForFileInfoTextMessage(conversationViewHolder, interaction, position);
        }
    }

    @Override
    public void onViewRecycled(@NonNull ConversationViewHolder holder) {
        holder.itemView.setOnLongClickListener(null);
        if (holder.mPhoto != null)
            holder.mPhoto.setOnLongClickListener(null);
        if (holder.updater != null) {
            holder.updater.stop();
            holder.updater = null;
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
        ConversationAdapter.RecyclerViewContextMenuInfo info = mCurrentLongItem;
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
            case R.id.conv_action_share: {
                presenter.shareFile(interaction);
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
            case R.id.conv_action_copy_text: {
                addToClipboard((interaction).getBody());
                break;
            }
        }
        return true;
    }

    private void addToClipboard(String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) conversationFragment.requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Message", text);
        clipboard.setPrimaryClip(clip);
    }


    private void configureForFileInfoTextMessage(@NonNull final ConversationViewHolder viewHolder,
                                                 @NonNull final Interaction interaction, int position) {
        DataTransfer file = (DataTransfer) interaction;
        File path = presenter.getDeviceRuntimeService().getConversationPath(file.getPeerId(), file.getStoragePath());
        file.setSize(path.length());

        String timeSeparationString = timestampToDetailString(viewHolder.itemView.getContext(), file.getTimestamp());
        if (file.getStatus() == InteractionStatus.TRANSFER_FINISHED) {
            boolean separateByDetails = shouldSeparateByDetails(interaction, position);
            if (separateByDetails) {
                viewHolder.mMsgDetailTxt.setVisibility(View.VISIBLE);
                viewHolder.mMsgDetailTxt.setText(String.format("%s - %s",
                        timeSeparationString, FileUtils.readableFileSize(file.getTotalSize())));
            } else
                viewHolder.mMsgDetailTxt.setVisibility(View.GONE);
        } else {
            viewHolder.mMsgDetailTxt.setVisibility(View.VISIBLE);
            viewHolder.mMsgDetailTxt.setText(String.format("%s - %s - %s",
                    timeSeparationString, FileUtils.readableFileSize(file.getTotalSize()),
                    ResourceMapper.getReadableFileTransferStatus(conversationFragment.getActivity(), file.getStatus())));
        }

        MessageType type;
        if (!file.isComplete()) {
            type = MessageType.FILE_TRANSFER;
        } else if (file.isPicture()) {
            type = MessageType.IMAGE;
        } else if (file.isAudio()) {
            type = MessageType.AUDIO;
        } else if (file.isVideo()) {
            type = MessageType.VIDEO;
        } else {
            type = MessageType.FILE_TRANSFER;
        }
        View longPressView = type == MessageType.IMAGE ? viewHolder.mPhoto : (type == MessageType.VIDEO) ? viewHolder.video : (type == MessageType.AUDIO) ? viewHolder.mAudioInfoLayout : viewHolder.mFileInfoLayout;
        if (type == MessageType.AUDIO || type == MessageType.FILE_TRANSFER) {
            longPressView.getBackground().setTintList(null);
        }

        longPressView.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
            menu.setHeaderTitle(file.getDisplayName());
            conversationFragment.onCreateContextMenu(menu, v, menuInfo);
            MenuInflater inflater = conversationFragment.getActivity().getMenuInflater();
            inflater.inflate(R.menu.conversation_item_actions_file, menu);
            if (!file.isComplete()) {
                menu.removeItem(R.id.conv_action_download);
                menu.removeItem(R.id.conv_action_share);
            }
        });
        longPressView.setOnLongClickListener(v -> {
            if (type == MessageType.AUDIO || type == MessageType.FILE_TRANSFER) {
                conversationFragment.updatePosition(viewHolder.getAdapterPosition());
                longPressView.getBackground().setTint(conversationFragment.getResources().getColor(R.color.grey_500));
            }
            mCurrentLongItem = new RecyclerViewContextMenuInfo(viewHolder.getAdapterPosition(), v.getId());
            return false;

        });

        if (type == MessageType.IMAGE) {
            Context context = viewHolder.mPhoto.getContext();

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) viewHolder.mAnswerLayout.getLayoutParams();
            params.gravity = (file.isOutgoing() ? Gravity.END : Gravity.START) | Gravity.BOTTOM;
            viewHolder.mAnswerLayout.setLayoutParams(params);

            LinearLayout.LayoutParams imageParams = (LinearLayout.LayoutParams) viewHolder.mPhoto.getLayoutParams();
            imageParams.height = mPictureMaxSize;
            viewHolder.mPhoto.setLayoutParams(imageParams);

            GlideApp.with(context)
                    .load(path)
                    .apply(PICTURE_OPTIONS)
                    .into(new DrawableImageViewTarget(viewHolder.mPhoto).waitForLayout());

            ((LinearLayout) viewHolder.mAnswerLayout).setGravity(file.isOutgoing() ? Gravity.END : Gravity.START);
            viewHolder.mPhoto.setOnClickListener(v -> {
                Uri contentUri = getUriForFile(v.getContext(), ContentUriHandler.AUTHORITY_FILES, path);
                Intent i = new Intent(context, MediaViewerActivity.class);
                i.setAction(Intent.ACTION_VIEW).setDataAndType(contentUri, "image/*").setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                ActivityOptionsCompat options = ActivityOptionsCompat.
                        makeSceneTransitionAnimation(conversationFragment.getActivity(), viewHolder.mPhoto, "picture");
                conversationFragment.startActivityForResult(i, 3006, options.toBundle());
            });
            return;
        } else if (type == MessageType.VIDEO) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) viewHolder.mAnswerLayout.getLayoutParams();
            params.gravity = file.isOutgoing() ? Gravity.END : Gravity.START;
            viewHolder.mAnswerLayout.setLayoutParams(params);

            Context context = viewHolder.itemView.getContext();
            if (viewHolder.player != null) {
                viewHolder.player.release();
            }
            final MediaPlayer player = MediaPlayer.create(context, getUriForFile(context, ContentUriHandler.AUTHORITY_FILES, path));
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
            viewHolder.video.setOnClickListener(v -> {
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
        } else if (type == MessageType.AUDIO) {
            Context context = viewHolder.itemView.getContext();
            try {
                ((ImageView) viewHolder.btnAccept).setImageResource(R.drawable.baseline_play_arrow_24);
                final MediaPlayer player = MediaPlayer.create(context, getUriForFile(context, ContentUriHandler.AUTHORITY_FILES, path));
                viewHolder.player = player;
                if (player != null) {
                    player.setOnCompletionListener(mp -> {
                        player.seekTo(0);
                        ((ImageView) viewHolder.btnAccept).setImageResource(R.drawable.baseline_play_arrow_24);
                    });
                    viewHolder.btnAccept.setOnClickListener((b) -> {
                        if (player.isPlaying()) {
                            player.pause();
                            ((ImageView) viewHolder.btnAccept).setImageResource(R.drawable.baseline_play_arrow_24);
                        } else {
                            player.start();
                            ((ImageView) viewHolder.btnAccept).setImageResource(R.drawable.baseline_pause_24);
                        }
                    });
                    viewHolder.btnRefuse.setOnClickListener((b) -> {
                        if (player.isPlaying())
                            player.pause();
                        player.seekTo(0);
                        ((ImageView) viewHolder.btnAccept).setImageResource(R.drawable.baseline_play_arrow_24);
                    });
                    viewHolder.updater = new UiUpdater(() -> {
                        int pS = player.getCurrentPosition() / 1000;
                        int dS = player.getDuration() / 1000;
                        viewHolder.mMsgTxt.setText(String.format("%02d:%02d / %02d:%02d", pS / 60, pS % 60, dS / 60, dS % 60));
                    });
                    viewHolder.updater.start();
                } else {
                    viewHolder.btnAccept.setOnClickListener(null);
                    viewHolder.btnRefuse.setOnClickListener(null);
                    viewHolder.updater = null;
                }
            } catch (IllegalStateException | NullPointerException e) {
                Log.e(TAG, "Error initializing player, it may have already been released: " + e.getMessage());
            }
            return;
        }

        if (file.getStatus().isError()) {
            viewHolder.icon.setImageResource(R.drawable.baseline_warning_24);
        } else {
            viewHolder.icon.setImageResource(R.drawable.baseline_attach_file_24);
        }

        viewHolder.mMsgTxt.setText(file.getDisplayName());

        ((LinearLayout) viewHolder.mLayout).setGravity(file.isOutgoing() ? Gravity.END : Gravity.START);

        if (file.getStatus() == InteractionStatus.TRANSFER_AWAITING_HOST) {
            viewHolder.mAnswerLayout.setVisibility(View.VISIBLE);
            viewHolder.btnAccept.setOnClickListener(v -> {
                if (!presenter.getDeviceRuntimeService().hasWriteExternalStoragePermission()) {
                    conversationFragment.askWriteExternalStoragePermission();
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
    private void configureForTextMessage(@NonNull final ConversationViewHolder convViewHolder,
                                         @NonNull final Interaction interaction,
                                         int position) {
        final Context context = convViewHolder.itemView.getContext();
        TextMessage textMessage = (TextMessage)interaction;
        CallContact contact = textMessage.getContact();
        if (contact == null) {
            Log.e(TAG, "Invalid contact, not able to display message correctly");
            return;
        }

        convViewHolder.mCid = textMessage.getConversation().getParticipant();
        String message = textMessage.getBody().trim();

        View longPressView = convViewHolder.mMsgTxt;
        longPressView.getBackground().setTintList(null);

        longPressView.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
            Date date = new Date(interaction.getTimestamp());
            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
            menu.setHeaderTitle(dateFormat.format(date));
            conversationFragment.onCreateContextMenu(menu, v, menuInfo);
            MenuInflater inflater = conversationFragment.getActivity().getMenuInflater();
            inflater.inflate(R.menu.conversation_item_actions_messages, menu);

            if ((interaction).getStatus() == (InteractionStatus.SENDING)) {
                menu.removeItem(R.id.conv_action_delete);
            } else {
                menu.findItem(R.id.conv_action_delete).setTitle(R.string.menu_message_delete);
                menu.removeItem(R.id.conv_action_cancel_message);
            }
        });

        longPressView.setOnLongClickListener((View v) -> {
            conversationFragment.updatePosition(convViewHolder.getAdapterPosition());
            if (textMessage.isIncoming()) {
                longPressView.getBackground().setTint(conversationFragment.getResources().getColor(R.color.grey_500));
            } else {
                longPressView.getBackground().setTint(conversationFragment.getResources().getColor(R.color.blue_900));
            }
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
        if (convViewHolder.mPhoto != null) {
            convViewHolder.mPhoto.setImageBitmap(null);
        }

        if (msgSequenceType == SequenceType.LAST || msgSequenceType == SequenceType.SINGLE) {
            setBottomMargin(convViewHolder.mMsgTxt, 8);
            if (textMessage.isIncoming()) {
                convViewHolder.mPhoto.setImageDrawable(
                        conversationFragment.getConversationAvatar(contact.getPrimaryNumber())
                );
            }
        } else {
            setBottomMargin(convViewHolder.mMsgTxt, 0);
        }

        if (!textMessage.isIncoming()) {
            switch (textMessage.getStatus()) {
                case SENDING:
                    convViewHolder.mPhoto.setVisibility(View.VISIBLE);
                    convViewHolder.mPhoto.setImageResource(R.drawable.baseline_circle_24);
                    break;
                case FAILURE:
                    convViewHolder.mPhoto.setVisibility(View.VISIBLE);
                    convViewHolder.mPhoto.setImageResource(R.drawable.round_highlight_off_24);
                    break;
                default:
                    if (shouldSeparateByDetails(textMessage, position) && position == lastOutgoingIndex()) {
                        convViewHolder.mPhoto.setVisibility(View.VISIBLE);
                        convViewHolder.mPhoto.setImageResource(R.drawable.baseline_check_circle_24);
                        lastDeliveredPosition = position;
                    } else {
                        convViewHolder.mPhoto.setVisibility(View.GONE);
                    }
            }
        }

        if (isTimeShown) {
            convViewHolder.updater = new UiUpdater(() -> {
                String timeSeparationString = timestampToDetailString(context, textMessage.getTimestamp());
                convViewHolder.mMsgDetailTxtPerm.setText(timeSeparationString);
            }, timestampUpdateInterval);
            convViewHolder.updater.start();
            convViewHolder.mMsgDetailTxtPerm.setVisibility(View.VISIBLE);
        } else {
            convViewHolder.mMsgDetailTxtPerm.setVisibility(View.GONE);
            final boolean isExpanded = position == expandedItemPosition;
            if (isExpanded) {
                convViewHolder.updater = new UiUpdater(() -> {
                    String timeSeparationString = timestampToDetailString(context, textMessage.getTimestamp());
                    convViewHolder.mMsgDetailTxt.setText(timeSeparationString);
                }, timestampUpdateInterval);
                convViewHolder.updater.start();
            }
            setItemViewExpansionState(convViewHolder.mMsgDetailTxt, isExpanded, position);

            convViewHolder.mMsgTxt.setOnClickListener((View v) -> {
                ValueAnimator va = animators.get(position);
                if (va != null && va.isRunning()) {
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

    private void configureForContactEvent(@NonNull final ConversationViewHolder viewHolder, @NonNull final Interaction interaction) {
        ContactEvent event = (ContactEvent) interaction;
        if (event.event == ContactEvent.Event.ADDED) {
            viewHolder.mMsgTxt.setText(R.string.hist_contact_added);
        } else if (event.event == ContactEvent.Event.INCOMING_REQUEST) {
            viewHolder.mMsgTxt.setText(R.string.hist_invitation_received);
        }
        viewHolder.updater = new UiUpdater(() -> {
            String timeSeparationString = timestampToDetailString(viewHolder.itemView.getContext(), event.getTimestamp());
            viewHolder.mMsgDetailTxt.setText(timeSeparationString);
        }, timestampUpdateInterval);
        viewHolder.updater.start();
    }

    /**
     * Configures the viewholder to display a call info text message, ie. not a classic text message
     *
     * @param convViewHolder The conversation viewHolder
     * @param interaction    The conversation element to display
     */
    private void configureForCallInfoTextMessage(@NonNull final ConversationViewHolder convViewHolder,
                                                 @NonNull final Interaction interaction) {
        int pictureResID;
        String historyTxt;
        convViewHolder.mPhoto.setScaleY(1);
        Context context = convViewHolder.itemView.getContext();


        View longPressView = convViewHolder.mCallInfoLayout;
        longPressView.getBackground().setTintList(null);


        longPressView.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
            conversationFragment.onCreateContextMenu(menu, v, menuInfo);
            MenuInflater inflater = conversationFragment.getActivity().getMenuInflater();
            inflater.inflate(R.menu.conversation_item_actions_messages, menu);

            menu.findItem(R.id.conv_action_delete).setTitle(R.string.menu_delete);
            menu.removeItem(R.id.conv_action_cancel_message);
            menu.removeItem(R.id.conv_action_copy_text);
        });


        longPressView.setOnLongClickListener((View v) -> {
            longPressView.getBackground().setTint(conversationFragment.getResources().getColor(R.color.grey_500));
            conversationFragment.updatePosition(convViewHolder.getAdapterPosition());
            mCurrentLongItem = new RecyclerViewContextMenuInfo(convViewHolder.getAdapterPosition(), v.getId());
            return false;
        });


        SipCall call = (SipCall) interaction;

        if (call.isMissed()) {
            if (call.isIncoming()) {
                pictureResID = R.drawable.baseline_call_missed_24;
            } else {
                pictureResID = R.drawable.baseline_call_missed_outgoing_24;
                // Flip the photo upside down to show a "missed outgoing call"
                convViewHolder.mPhoto.setScaleY(-1);
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
        convViewHolder.mPhoto.setImageResource(pictureResID);
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
        long now = new Date().getTime();
        long diff = now - timestamp;
        if (diff < DateUtils.MINUTE_IN_MILLIS) {
            return context.getString(R.string.time_just_now);
        } else if (diff < DateUtils.HOUR_IN_MILLIS) {  // 35 minutes ago
            return DateUtils.getRelativeTimeSpanString(timestamp, now, 0, 0).toString();
        } else {
            if (diff < DateUtils.WEEK_IN_MILLIS) {
                if (diff < DateUtils.DAY_IN_MILLIS && DateUtils.isToday(timestamp)) { // 11:32 A.M.
                    return DateUtils.formatDateTime(context, timestamp, DateUtils.FORMAT_SHOW_TIME);
                }
                return DateUtils.formatDateTime(context, timestamp,
                        DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_NO_YEAR |
                                DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_TIME)
                        .toUpperCase();
            } else if (diff < DateUtils.YEAR_IN_MILLIS) { // JAN. 7, 11:02 A.M.
                return DateUtils.formatDateTime(context, timestamp,
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR |
                                DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_TIME)
                        .toUpperCase();
            } else {
                return DateUtils.formatDateTime(context, timestamp,
                        DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE |
                                DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_WEEKDAY |
                                DateUtils.FORMAT_ABBREV_ALL).toUpperCase();
            }
        }
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

    /**
     * Helper used to determine if a text details string should be displayed for a message at a
     * certain position.
     *
     * @param msg       The Interaction at the given position
     * @param position The position of the current message
     * @return true if a text details string should be displayed under the message
     */
    private boolean shouldSeparateByDetails(final Interaction msg, int position) {
        if (msg == null) {
            return false;
        }
        boolean shouldSeparateMsg = false;
        Interaction previousTextMessage = getPreviousMessageFromPosition(position);
        if (previousTextMessage != null) {
            shouldSeparateMsg = true;
            Interaction nextTextMessage = getNextMessageFromPosition(position);
            if (nextTextMessage != null) {
                long diff = nextTextMessage.getTimestamp() - msg.getTimestamp();
                if (diff < DateUtils.MINUTE_IN_MILLIS) {
                    shouldSeparateMsg = false;
                }
            }
        }
        return shouldSeparateMsg;
    }

    private boolean isSeqBreak(@NotNull Interaction first, @NotNull Interaction second) {
        return StringUtils.isOnlyEmoji(first.getBody()) != StringUtils.isOnlyEmoji(second.getBody())
                || first.isIncoming() != second.isIncoming()
                || first.getType() == InteractionType.CONTACT
                || first.getType() == InteractionType.CALL
                || second.getType() == InteractionType.CONTACT
                || second.getType() == InteractionType.CALL;
    }

    private boolean isAlwaysSingleMsg(@NotNull Interaction msg) {
        return msg.getType() == InteractionType.CONTACT
                || msg.getType() == InteractionType.CALL
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
                if (isSeqBreak(msg, nextMsg)) {
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
            if (((isSeqBreak(msg, prevMsg) || isTimeShown) && !isSeqBreak(msg, nextMsg))) {
                return SequenceType.FIRST;
            } else if (!isSeqBreak(msg, prevMsg) && !isTimeShown && isSeqBreak(msg, nextMsg)) {
                return SequenceType.LAST;
            } else if (!isSeqBreak(msg, prevMsg) && !isTimeShown && !isSeqBreak(msg, nextMsg)) {
                return SequenceType.MIDDLE;
            }
        }
        return SequenceType.SINGLE;
    }

    private void setItemViewExpansionState(View view, boolean expanded, int position) {
        ValueAnimator anim = animators.get(position);
        if (anim == null) {
            if (view.getHeight() == 0 && !expanded) {
                return;
            }
            anim = new ValueAnimator();
            animators.put(position, anim);
        }
        if (anim.isRunning()) {
            anim.reverse();
            return;
        }
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        anim.setIntValues(0, view.getMeasuredHeight());
        if (expanded) {
            view.setVisibility(View.VISIBLE);
        }
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ValueAnimator va = (ValueAnimator) animation;
                if ((Integer) va.getAnimatedValue() == 0) {
                    view.setVisibility(View.GONE);
                }
                animators.remove(position);
            }
        });
        anim.setDuration(200);
        anim.addUpdateListener(animation -> {
            view.getLayoutParams().height = (Integer) animation.getAnimatedValue();
            view.requestLayout();
        });
        if (!expanded) {
            anim.reverse();
        } else {
            anim.start();
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
        INCOMING_TEXT_MESSAGE(R.layout.item_conv_msg_peer),
        OUTGOING_TEXT_MESSAGE(R.layout.item_conv_msg_me),
        CALL_INFORMATION(R.layout.item_conv_call),
        FILE_TRANSFER(R.layout.item_conv_file),
        IMAGE(R.layout.item_conv_image),
        AUDIO(R.layout.item_conv_audio),
        VIDEO(R.layout.item_conv_video),
        CONTACT_EVENT(R.layout.item_conv_contact);

        private final int layout;

        MessageType(int l) {
            layout = l;
        }
    }
}