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

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.LruCache;
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

import com.bumptech.glide.load.resource.bitmap.CenterInside;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.target.DrawableImageViewTarget;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import cx.ring.R;
import cx.ring.client.MediaViewerActivity;
import cx.ring.contacts.AvatarFactory;
import cx.ring.conversation.ConversationPresenter;
import cx.ring.fragments.ConversationFragment;
import cx.ring.model.CallContact;
import cx.ring.model.ContactEvent;
import cx.ring.model.ConversationElement;
import cx.ring.model.DataTransferEventCode;
import cx.ring.model.HistoryCall;
import cx.ring.model.DataTransfer;
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

    private static final long MINUTE = 60L * 1000L;
    private static final long HOUR = 60 * MINUTE;

    private final ArrayList<ConversationElement> mConversationElements = new ArrayList<>();
    private final ConversationPresenter presenter;
    private final ConversationFragment conversationFragment;
    private final int hPadding;
    private final int vPadding;
    private final int vPaddingEmoticon;
    private final int mPictureMaxSize;
    private final GlideOptions PICTURE_OPTIONS;
    private RecyclerViewContextMenuInfo mCurrentLongItem = null;
    private int convColor = 0;

    private final Map<File, MediaPlayer> mediaCache;

    public ConversationAdapter(ConversationFragment conversationFragment, ConversationPresenter presenter, Map<File, MediaPlayer> media) {
        this.conversationFragment = conversationFragment;
        this.presenter = presenter;
        mediaCache = media;
        Context context = conversationFragment.getActivity();
        Resources res = context.getResources();
        hPadding = res.getDimensionPixelSize(R.dimen.padding_medium);
        vPadding = res.getDimensionPixelSize(R.dimen.padding_small);
        vPaddingEmoticon = res.getDimensionPixelSize(R.dimen.padding_xsmall);
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
     * @param list an arraylist of IConversationElement
     */
    public void updateDataset(final List<ConversationElement> list) {
        Log.d(TAG, "updateDataset: list size=" + list.size());
        if (mConversationElements.isEmpty()) {
            mConversationElements.addAll(list);
        } else if (list.size() > mConversationElements.size()) {
            mConversationElements.addAll(list.subList(mConversationElements.size(), list.size()));
        } else {
            mConversationElements.clear();
            mConversationElements.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void add(ConversationElement e) {
        boolean update = !mConversationElements.isEmpty();
        mConversationElements.add(e);
        notifyItemInserted(mConversationElements.size()-1);
        if (update)
            notifyItemChanged(mConversationElements.size()-2);
    }

    public void update(ConversationElement e) {
        for(int i=mConversationElements.size()-1; i >= 0; i--){
            ConversationElement element = mConversationElements.get(i);
            if (e == element) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void remove(ConversationElement e) {
        for(int i=mConversationElements.size()-1; i >= 0; i--){
            ConversationElement element = mConversationElements.get(i);
            if (e == element) {
                mConversationElements.remove(i);
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
        return mConversationElements.size();
    }

    @Override
    public long getItemId(int position) {
        return mConversationElements.get(position).getId();
    }

    @Override
    public int getItemViewType(int position) {
        ConversationElement conversationElement = mConversationElements.get(position);
        if (conversationElement != null) {
            switch (conversationElement.getType()) {
                case TEXT: {
                    TextMessage ht = (TextMessage) conversationElement;
                    if (ht.isIncoming()) {
                        return MessageType.INCOMING_TEXT_MESSAGE.ordinal();
                    } else {
                        return MessageType.OUTGOING_TEXT_MESSAGE.ordinal();
                    }
                }
                case FILE: {
                    DataTransfer file = (DataTransfer) conversationElement;
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
                }
                case CALL:
                    return MessageType.CALL_INFORMATION.ordinal();
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
        ConversationElement conversationElement = mConversationElements.get(position);
        if (conversationElement != null) {
            if (conversationElement.getType() == ConversationElement.CEType.TEXT) {
                configureForTextMessage(conversationViewHolder, conversationElement, position);
            } else if (conversationElement.getType() == ConversationElement.CEType.FILE) {
                configureForFileInfoTextMessage(conversationViewHolder, conversationElement, position);
            } else if (conversationElement.getType() == ConversationElement.CEType.CALL) {
                configureForCallInfoTextMessage(conversationViewHolder, conversationElement);
            } else if (conversationElement.getType() == ConversationElement.CEType.CONTACT) {
                configureForContactEvent(conversationViewHolder, conversationElement);
            }
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
            if(holder.player.isPlaying())
                holder.player.stop();
            holder.player.reset();
            holder.player.release();
            holder.player = null;
            mediaCache.remove(holder.playerFile);
            holder.playerFile = null;
        }
        super.onViewRecycled(holder);
    }

    public void setPrimaryColor(int color) {
        convColor = color;
        notifyDataSetChanged();
    }

    public static class RecyclerViewContextMenuInfo implements ContextMenu.ContextMenuInfo {
        public RecyclerViewContextMenuInfo(int position, long id) {
            this.position = position;
            this.id = id;
        }
        final public int position;
        final public long id;
    }

    public RecyclerViewContextMenuInfo getCurrentLongItem() {
        return mCurrentLongItem;
    }

    public boolean onContextItemSelected(MenuItem item) {
        ConversationAdapter.RecyclerViewContextMenuInfo info = getCurrentLongItem();
        if (info == null) {
            return false;
        }
        ConversationElement conversationElement = mConversationElements.get(info.position);
        if (conversationElement == null)
            return false;
        if (conversationElement.getType() != ConversationElement.CEType.FILE)
            return false;
        DataTransfer file = (DataTransfer) conversationElement;
        switch (item.getItemId()) {
            case R.id.conv_action_download: {
                File downloadDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Ring");
                downloadDir.mkdirs();
                File newFile = new File(downloadDir, file.getDisplayName());
                if (newFile.exists())
                    newFile.delete();
                presenter.downloadFile(file, newFile);
                break;
            }
            case R.id.conv_action_share: {
                presenter.shareFile(file);
                break;
            }
            case R.id.conv_action_open: {
                presenter.openFile(file);
                break;
            }
            case R.id.conv_action_delete:
                presenter.deleteFile(file);
                break;
        }
        return true;
    }

    private void configureForFileInfoTextMessage(@NonNull final ConversationViewHolder viewHolder,
                                                 @NonNull final ConversationElement conversationElement, int position) {
        DataTransfer file = (DataTransfer) conversationElement;

        String timeSeparationString = timestampToDetailString(viewHolder.itemView.getContext(), file.getDate());
        if (file.getEventCode() == DataTransferEventCode.FINISHED) {
            boolean separateByDetails = shouldSeparateByDetails(conversationElement, position);
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
                    ResourceMapper.getReadableFileTransferStatus(conversationFragment.getActivity(), file.getEventCode())));
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
        View longPressView = type == MessageType.IMAGE ? viewHolder.mPhoto : (type == MessageType.VIDEO) ? viewHolder.video : viewHolder.itemView;
        longPressView.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
            menu.setHeaderTitle(file.getDisplayName());
            conversationFragment.onCreateContextMenu(menu, v, menuInfo);
            MenuInflater inflater = conversationFragment.getActivity().getMenuInflater();
            inflater.inflate(R.menu.conversation_item_actions, menu);
            if (!file.isComplete()) {
                menu.removeItem(R.id.conv_action_download);
                menu.removeItem(R.id.conv_action_share);
            }
        });
        longPressView.setOnLongClickListener(v -> {
            mCurrentLongItem = new RecyclerViewContextMenuInfo(viewHolder.getLayoutPosition(), v.getId());
            return false;
        });

        Context context = viewHolder.itemView.getContext();
        if (type == MessageType.AUDIO || type == MessageType.VIDEO) {
            File path = presenter.getDeviceRuntimeService().getConversationPath(file.getPeerId(), file.getStoragePath());
            MediaPlayer player = mediaCache.get(path);
            if (player == null) {
                player = MediaPlayer.create(context, getUriForFile(context, ContentUriHandler.AUTHORITY_FILES, path));
                mediaCache.put(path, player);
            }
            viewHolder.playerFile = path;
            viewHolder.player = player;
        }
        final MediaPlayer player = viewHolder.player;

        if (type == MessageType.IMAGE) {
            File path = presenter.getDeviceRuntimeService().getConversationPath(file.getPeerId(), file.getStoragePath());

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

            ((LinearLayout)viewHolder.mAnswerLayout).setGravity(file.isOutgoing() ? Gravity.END : Gravity.START);
            viewHolder.mPhoto.setOnClickListener(v -> {
                Uri contentUri = getUriForFile(v.getContext(), ContentUriHandler.AUTHORITY_FILES, path);
                Intent i = new Intent(context, MediaViewerActivity.class);
                i.setAction(Intent.ACTION_VIEW).setDataAndType(contentUri, "image/*").setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(conversationFragment.getActivity(), viewHolder.mPhoto, "picture");
                conversationFragment.startActivityForResult(i, 3006, options.toBundle());
            });
            return;
        } else if (type == MessageType.VIDEO) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) viewHolder.mAnswerLayout.getLayoutParams();
            params.gravity = file.isOutgoing() ? Gravity.END : Gravity.START;
            viewHolder.mAnswerLayout.setLayoutParams(params);

            final Drawable playBtn = ContextCompat.getDrawable(context, R.drawable.baseline_play_arrow_24).mutate();
            DrawableCompat.setTint(playBtn, Color.WHITE);
            ((CardView)viewHolder.mLayout).setForeground(playBtn);
            player.setOnCompletionListener(mp -> {
                if (player.isPlaying())
                    player.pause();
                player.seekTo(1);
                ((CardView)viewHolder.mLayout).setForeground(playBtn);
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
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}
                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    player.setSurface(null);
                    viewHolder.surface = null;
                    return true;
                }
                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
            });
            viewHolder.video.setOnClickListener(v -> {
                if (player.isPlaying()) {
                    player.pause();
                    ((CardView)viewHolder.mLayout).setForeground(playBtn);
                } else {
                    player.start();
                    ((CardView)viewHolder.mLayout).setForeground(null);
                }
            });
            player.seekTo(1);
            return;
        } else if (type == MessageType.AUDIO) {
            //File path = presenter.getDeviceRuntimeService().getConversationPath(file.getPeerId(), file.getStoragePath());
            //final MediaPlayer player =  MediaPlayer.create(context, getUriForFile(context, ContentUriHandler.AUTHORITY_FILES, path));
            viewHolder.player = player;
            player.setOnCompletionListener(mp -> {
                player.seekTo(0);
                ((ImageView)viewHolder.btnAccept).setImageResource(R.drawable.baseline_play_arrow_24);
            });
            ((ImageView)viewHolder.btnAccept).setImageResource(R.drawable.baseline_play_arrow_24);
            viewHolder.btnAccept.setOnClickListener((b) -> {
                if (player.isPlaying()) {
                    player.pause();
                    ((ImageView)viewHolder.btnAccept).setImageResource(R.drawable.baseline_play_arrow_24);
                } else {
                    player.start();
                    ((ImageView)viewHolder.btnAccept).setImageResource(R.drawable.baseline_pause_24);
                }
            });
            viewHolder.btnRefuse.setOnClickListener((b) -> {
                if (player.isPlaying())
                    player.pause();
                player.seekTo(0);
                ((ImageView)viewHolder.btnAccept).setImageResource(R.drawable.baseline_play_arrow_24);
            });
            viewHolder.updater = new UiUpdater(() -> {
                int pS = player.getCurrentPosition() / 1000;
                int dS = player.getDuration() / 1000;
                viewHolder.mMsgTxt.setText(String.format("%02d:%02d / %02d:%02d", pS / 60, pS % 60, dS / 60, dS % 60));
            });
            viewHolder.updater.start();
            return;
        }

        if (file.getEventCode().isError()) {
            viewHolder.icon.setImageResource(R.drawable.ic_warning);
        } else {
            viewHolder.icon.setImageResource(R.drawable.ic_clip_black);
        }

        viewHolder.mMsgTxt.setText(file.getDisplayName());

        ((LinearLayout)viewHolder.mLayout).setGravity(file.isOutgoing() ? Gravity.END : Gravity.START);

        if (file.getEventCode() == DataTransferEventCode.WAIT_HOST_ACCEPTANCE) {
            viewHolder.mAnswerLayout.setVisibility(View.VISIBLE);
            viewHolder.btnAccept.setOnClickListener(v -> {
                if (!presenter.getDeviceRuntimeService().hasWriteExternalStoragePermission()) {
                    conversationFragment.askWriteExternalStoragePermission();
                    return;
                }
                Context c = v.getContext();
                File cacheDir = context.getCacheDir();
                long spaceLeft = AndroidFileUtils.getSpaceLeft(cacheDir.toString());
                if (spaceLeft == -1L || file.getTotalSize() > spaceLeft) {
                    presenter.noSpaceLeft();
                    return;
                }
                c.startService(new Intent(DRingService.ACTION_FILE_ACCEPT)
                        .setClass(c.getApplicationContext(), DRingService.class)
                        .putExtra(DRingService.KEY_TRANSFER_ID, file.getDataTransferId()));
            });
            viewHolder.btnRefuse.setOnClickListener(v -> {
                Context c = v.getContext();
                c.startService(new Intent(DRingService.ACTION_FILE_CANCEL)
                        .setClass(c.getApplicationContext(), DRingService.class)
                        .putExtra(DRingService.KEY_TRANSFER_ID, file.getDataTransferId()));
            });
        } else {
            viewHolder.mAnswerLayout.setVisibility(View.GONE);
        }
    }

    /**
     * Configures the viewholder to display a classic text message, ie. not a call info text message
     *
     * @param convViewHolder The conversation viewHolder
     * @param convElement    The conversation element to display
     * @param position       The position of the viewHolder
     */
    private void configureForTextMessage(@NonNull final ConversationViewHolder convViewHolder,
                                         @NonNull final ConversationElement convElement,
                                         int position) {
        TextMessage textMessage = (TextMessage) convElement;
        CallContact contact = textMessage.getContact();
        if (contact == null) {
            Log.e(TAG, "Invalid contact, not able to display message correctly");
            return;
        }

        convViewHolder.mCid = textMessage.getContact().getId();
        String message = textMessage.getMessage().trim();
        if (StringUtils.isOnlyEmoji(message)) {
            convViewHolder.mMsgTxt.getBackground().setAlpha(0);
            convViewHolder.mMsgTxt.setTextSize(24.f);
            convViewHolder.mMsgTxt.setPadding(hPadding, vPaddingEmoticon, hPadding, vPaddingEmoticon);
        } else {
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

        boolean separateByDetails = shouldSeparateByDetails(textMessage, position);
        boolean isLast = position == mConversationElements.size() - 1;
        boolean sameAsPreviousMsg = isMessageConfigSameAsPrevious(textMessage, position);
        final Context context = convViewHolder.itemView.getContext();

        if (textMessage.isIncoming() && !sameAsPreviousMsg) {
            AvatarFactory.loadGlideAvatar(convViewHolder.mPhoto, contact);
        }

        switch (textMessage.getStatus()) {
            case SENDING:
                if (!textMessage.isIncoming()) {
                    convViewHolder.mPhoto.setVisibility(View.VISIBLE);
                    convViewHolder.mPhoto.setImageResource(R.drawable.baseline_circle_24);
                    convViewHolder.mMsgDetailTxt.setVisibility(View.GONE);
                }
                break;
            case FAILURE:
                if (!textMessage.isIncoming()) {
                    convViewHolder.mPhoto.setVisibility(View.VISIBLE);
                    convViewHolder.mPhoto.setImageResource(R.drawable.round_highlight_off_24);
                    convViewHolder.mMsgDetailTxt.setVisibility(View.GONE);
                }
                break;
            default:
                if (separateByDetails) {
                    if (!textMessage.isIncoming()) {
                        convViewHolder.mPhoto.setVisibility(View.VISIBLE);
                        convViewHolder.mPhoto.setImageResource(R.drawable.baseline_check_circle_24);
                    }
                    if (!isLast) {
                        convViewHolder.updater = new UiUpdater(() -> {
                            convViewHolder.mMsgDetailTxt.setText(timestampToDetailString(context, textMessage.getDate()));
                        }, 10000);
                        convViewHolder.updater.start();
                        convViewHolder.mMsgDetailTxt.setVisibility(View.VISIBLE);
                    } else {
                        convViewHolder.mMsgDetailTxt.setVisibility(View.GONE);
                    }
                } else {
                    if (!textMessage.isIncoming()) {
                        convViewHolder.mPhoto.setVisibility(View.GONE);
                    }
                    convViewHolder.mMsgDetailTxt.setVisibility(View.GONE);
                }
        }
    }

    private void configureForContactEvent(@NonNull final ConversationViewHolder viewHolder, @NonNull final ConversationElement conversationElement) {
        ContactEvent event = (ContactEvent) conversationElement;
        if (event.event == ContactEvent.Event.ADDED) {
            viewHolder.mMsgTxt.setText(R.string.hist_contact_added);
        } else if (event.event == ContactEvent.Event.INCOMING_REQUEST) {
            viewHolder.mMsgTxt.setText(R.string.hist_invitation_received);
        }
        viewHolder.updater = new UiUpdater(() -> {
            String timeSeparationString = timestampToDetailString(viewHolder.itemView.getContext(), conversationElement.getDate());
            viewHolder.mMsgDetailTxt.setText(timeSeparationString);
        }, 10000);
        viewHolder.updater.start();
    }

    /**
     * Configures the viewholder to display a call info text message, ie. not a classic text message
     *
     * @param convViewHolder The conversation viewHolder
     * @param convElement    The conversation element to display
     */
    private void configureForCallInfoTextMessage(@NonNull final ConversationViewHolder convViewHolder,
                                                 @NonNull final ConversationElement convElement) {
        int pictureResID;
        String historyTxt;
        convViewHolder.mPhoto.setScaleY(1);
        Context context = convViewHolder.itemView.getContext();

        HistoryCall hc = (HistoryCall) convElement;

        if (hc.isMissed()) {
            if (hc.isIncoming()) {
                pictureResID = R.drawable.ic_call_missed_incoming_black;
            } else {
                pictureResID = R.drawable.ic_call_missed_outgoing_black;
                // Flip the photo upside down to show a "missed outgoing call"
                convViewHolder.mPhoto.setScaleY(-1);
            }
            historyTxt = hc.isIncoming() ?
                    context.getString(R.string.notif_missed_incoming_call) :
                    context.getString(R.string.notif_missed_outgoing_call);
        } else {
            pictureResID = (hc.isIncoming()) ?
                    R.drawable.ic_incoming_black :
                    R.drawable.ic_outgoing_black;
            historyTxt = hc.isIncoming() ?
                    context.getString(R.string.notif_incoming_call) :
                    context.getString(R.string.notif_outgoing_call);
        }

        convViewHolder.mCid = hc.getContactID();
        convViewHolder.mPhoto.setImageResource(pictureResID);
        convViewHolder.mHistTxt.setText(historyTxt);
        convViewHolder.mHistDetailTxt.setText(DateFormat.getDateTimeInstance()
                .format(hc.getStartDate()));
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
        long elapsed = now - timestamp;
        if (elapsed < MINUTE) {
            return context.getString(R.string.time_just_now);
        } else if (elapsed < HOUR) {
            return DateUtils.getRelativeTimeSpanString(timestamp, now, 0, 0).toString();
        } else {
            return DateUtils.formatSameDayTime(timestamp, now, DateFormat.SHORT, DateFormat.SHORT).toString();
        }
    }

    /**
     * Helper method to return the previous TextMessage relative to an initial position.
     *
     * @param position The initial position
     * @return the previous TextMessage if any, null otherwise
     */
    @Nullable
    private ConversationElement getPreviousMessageFromPosition(int position) {
        if (!mConversationElements.isEmpty() && position > 0) {
            return mConversationElements.get(position - 1);
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
    private ConversationElement getNextMessageFromPosition(int position) {
        if (!mConversationElements.isEmpty() && position < mConversationElements.size() - 1) {
            return mConversationElements.get(position + 1);
        }
        return null;
    }

    /**
     * Helper used to determine if a text details string should be displayed under a message at a
     * certain position.
     *
     * @param ht       The conversationElement at the given position
     * @param position The position of the current message
     * @return true if a text details string should be displayed under the message
     */
    private boolean shouldSeparateByDetails(final ConversationElement ht, int position) {
        if (ht == null) {
            return false;
        }
        boolean shouldSeparateMsg = false;
        ConversationElement previousTextMessage = getPreviousMessageFromPosition(position);
        if (previousTextMessage != null) {
            shouldSeparateMsg = true;
            ConversationElement nextTextMessage = getNextMessageFromPosition(position);
            if (nextTextMessage != null) {
                long diff = nextTextMessage.getDate() - ht.getDate();
                if (diff < MINUTE) {
                    shouldSeparateMsg = false;
                }
            }
        }
        return shouldSeparateMsg;
    }

    /**
     * Helper method determining if a given conversationElement should be distinguished from the
     * previous ie. if their configuration is not the same.
     *
     * @param textMessage The conversationElement at the given position
     * @param position    The position of the current message
     * @return true if the configuration is the same as the previous message, false otherwise.
     */
    private boolean isMessageConfigSameAsPrevious(final TextMessage textMessage,
                                                  int position) {
        if (textMessage == null) {
            return false;
        }

        boolean sameConfig = false;
        ConversationElement previousMessage = getPreviousMessageFromPosition(position);
        if (previousMessage != null &&
                textMessage.getType() == previousMessage.getType() &&
                textMessage.isIncoming() &&
                previousMessage.getContactNumber().equals(textMessage.getContactNumber())) {
            sameConfig = true;
        }
        return sameConfig;
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
        MessageType(int l) { layout = l; }
    }
}