/*
 *  Copyright (C) 2004-2018 Savoir-faire Linux Inc.
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

import android.app.DownloadManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterInside;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.target.ImageViewTarget;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import cx.ring.R;
import cx.ring.client.MediaViewerActivity;
import cx.ring.contacts.AvatarFactory;
import cx.ring.conversation.ConversationPresenter;
import cx.ring.fragments.ConversationFragment;
import cx.ring.model.CallContact;
import cx.ring.model.DataTransferEventCode;
import cx.ring.model.HistoryCall;
import cx.ring.model.DataTransfer;
import cx.ring.model.IConversationElement;
import cx.ring.model.TextMessage;
import cx.ring.service.DRingService;
import cx.ring.utils.AndroidFileUtils;
import cx.ring.utils.ContentUriHandler;
import cx.ring.utils.FileUtils;
import cx.ring.utils.GlideApp;
import cx.ring.utils.GlideOptions;
import cx.ring.utils.ResourceMapper;
import cx.ring.utils.StringUtils;
import cx.ring.views.ConversationViewHolder;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationViewHolder> {
    private final static String TAG = ConversationAdapter.class.getSimpleName();

    private static final double MINUTE = 60L * 1000L;
    private static final double HOUR = 3600L * 1000L;

    private final ArrayList<IConversationElement> mConversationElements = new ArrayList<>();
    private final ConversationPresenter presenter;
    private final ConversationFragment conversationFragment;
    private byte[] mPhoto;
    private final int hPadding;
    private final int vPadding;
    private final int vPaddingEmoticon;
    private final int mPictureMaxSize;
    private final GlideOptions PICTURE_OPTIONS;
    private RecyclerViewContextMenuInfo mCurrentLongItem = null;

    public ConversationAdapter(ConversationFragment conversationFragment, ConversationPresenter presenter) {
        this.conversationFragment = conversationFragment;
        this.presenter = presenter;
        Context context = conversationFragment.getActivity();
        Resources res = context.getResources();
        hPadding = res.getDimensionPixelSize(R.dimen.padding_medium);
        vPadding = res.getDimensionPixelSize(R.dimen.padding_small);
        vPaddingEmoticon = res.getDimensionPixelSize(R.dimen.padding_xsmall);
        mPictureMaxSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, res.getDisplayMetrics());
        int corner = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, res.getDisplayMetrics());
        PICTURE_OPTIONS = new GlideOptions().override(mPictureMaxSize).transform(new CenterInside()).transform(new RoundedCorners(corner));
    }

    /**
     * Refreshes the data and notifies the changes
     *
     * @param list an arraylist of IConversationElement
     */
    public void updateDataset(final ArrayList<IConversationElement> list) {
        Log.d(TAG, "updateDataset: list size=" + list.size());

        if (list.size() > mConversationElements.size()) {
            mConversationElements.addAll(list.subList(mConversationElements.size(), list.size()));
        } else {
            mConversationElements.clear();
            mConversationElements.addAll(list);
        }
        notifyDataSetChanged();
    }

    /**
     * Updates the contact photo to use for this conversation
     *
     * @param photo contact photo to display.
     */
    public void setPhoto(byte[] photo) {
        mPhoto = photo;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mConversationElements.size();
    }

    @Override
    public long getItemId(int position) {
        return mConversationElements.get(position).hashCode();
    }

    @Override
    public int getItemViewType(int position) {
        IConversationElement conversationElement = mConversationElements.get(position);
        if (conversationElement != null) {
            if (conversationElement.getType() == IConversationElement.CEType.TEXT) {
                TextMessage ht = (TextMessage) conversationElement;
                if (ht.isIncoming()) {
                    return ConversationMessageType.INCOMING_TEXT_MESSAGE.getType();
                } else {
                    return ConversationMessageType.OUTGOING_TEXT_MESSAGE.getType();
                }
            }
            if (conversationElement.getType() == IConversationElement.CEType.FILE) {
                DataTransfer file = (DataTransfer) conversationElement;
                if (file.showPicture()) {
                    return ConversationMessageType.IMAGE.getType();
                } else
                    return ConversationMessageType.FILE_TRANSFER.getType();
            }
            if (conversationElement.getType() == IConversationElement.CEType.CALL) {
                return ConversationMessageType.CALL_INFORMATION_TEXT_MESSAGE.getType();
            }
        }
        return ConversationMessageType.CALL_INFORMATION_TEXT_MESSAGE.getType();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int res;
        if (viewType == ConversationMessageType.INCOMING_TEXT_MESSAGE.getType()) {
            res = R.layout.item_conv_msg_peer;
        } else if (viewType == ConversationMessageType.OUTGOING_TEXT_MESSAGE.getType()) {
            res = R.layout.item_conv_msg_me;
        } else if (viewType == ConversationMessageType.FILE_TRANSFER.getType()) {
            res = R.layout.item_conv_file;
        } else if (viewType == ConversationMessageType.IMAGE.getType()) {
            res = R.layout.item_conv_image;
        } else {
            res = R.layout.item_conv_call;
        }
        ViewGroup v = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(res, parent, false);
        return new ConversationViewHolder(v, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder conversationViewHolder, int position) {
        IConversationElement conversationElement = mConversationElements.get(position);
        if (conversationElement != null) {
            if (conversationElement.getType() == IConversationElement.CEType.TEXT) {
                this.configureForTextMessage(conversationViewHolder, conversationElement, position);
            } else if (conversationElement.getType() == IConversationElement.CEType.FILE) {
                this.configureForFileInfoTextMessage(conversationViewHolder, conversationElement);
            } else if (conversationElement.getType() == IConversationElement.CEType.CALL) {
                this.configureForCallInfoTextMessage(conversationViewHolder, conversationElement);
            }
        }
    }

    @Override
    public void onViewRecycled(@NonNull ConversationViewHolder holder) {
        if (holder.itemView != null)
            holder.itemView.setOnLongClickListener(null);
        if (holder.mPhoto != null)
            holder.mPhoto.setOnLongClickListener(null);
        super.onViewRecycled(holder);
    }

    private static void runJustBeforeBeingDrawn(final ImageView view, final Runnable runnable) {

        final ViewTreeObserver.OnPreDrawListener preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                view.getViewTreeObserver().removeOnPreDrawListener(this);
                runnable.run();
                return true;
            }
        };
        view.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
    }

    public void updateTransfer(DataTransfer transfer) {
        for(int i=mConversationElements.size()-1; i >= 0; i--){
            IConversationElement e = mConversationElements.get(i);
            if (e == transfer) {
                notifyItemChanged(i);
                return;
            }
        }
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
        IConversationElement conversationElement = mConversationElements.get(info.position);
        if (conversationElement == null)
            return false;
        if (conversationElement.getType() != IConversationElement.CEType.FILE)
            return false;
        DataTransfer file = (DataTransfer) conversationElement;
        Context context = conversationFragment.getActivity();
        switch (item.getItemId()) {
            case R.id.conv_action_download: {
                File downloadDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Ring");
                if (!downloadDir.mkdirs()) {
                    Log.e(TAG, "Directory not created");
                }
                File newFile = new File(downloadDir, file.getDisplayName());
                if (newFile.exists())
                    newFile.delete();
                if (presenter.downloadFile(file, newFile)) {
                    DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                    if (downloadManager != null) {
                        downloadManager.addCompletedDownload(file.getDisplayName(),
                                file.getDisplayName(),
                                true,
                                file.isPicture() ? "image/jpeg" : "text/plain",
                                newFile.getAbsolutePath(),
                                newFile.length(),
                                true);
                    }
                }
                break;
            }
            case R.id.conv_action_share: {
                presenter.shareFile(file);
                break;
            }
            case R.id.conv_action_delete:
                presenter.deleteFile(file);
                break;
        }
        return true;
    }


    private void configureForFileInfoTextMessage(final ConversationViewHolder conversationViewHolder,
                                                 final IConversationElement conversationElement) {
        if (conversationViewHolder == null || conversationElement == null) {
            return;
        }
        DataTransfer file = (DataTransfer) conversationElement;

        String timeSeparationString = computeTimeSeparationStringFromMsgTimeStamp(
                conversationViewHolder.itemView.getContext(),
                file.getDate());
        if (file.getEventCode() == DataTransferEventCode.FINISHED) {
            conversationViewHolder.mMsgDetailTxt.setText(String.format("%s - %s",
                    timeSeparationString, FileUtils.readableFileSize(file.getTotalSize())));
        } else {
            conversationViewHolder.mMsgDetailTxt.setText(String.format("%s - %s - %s",
                    timeSeparationString, FileUtils.readableFileSize(file.getTotalSize()),
                    ResourceMapper.getReadableFileTransferStatus(conversationFragment.getActivity(), file.getEventCode())));
        }

        boolean showPicture = file.showPicture();
        View longPressView = showPicture ? conversationViewHolder.mPhoto : conversationViewHolder.itemView;
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
            mCurrentLongItem = new RecyclerViewContextMenuInfo(conversationViewHolder.getLayoutPosition(), v.getId());
            return false;
        });

        if (showPicture) {
            Context context = conversationViewHolder.mPhoto.getContext();
            File path = presenter.getDeviceRuntimeService().getConversationPath(file.getPeerId(), file.getStoragePath());

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) conversationViewHolder.mAnswerLayout.getLayoutParams();
            params.gravity = (file.isOutgoing() ? Gravity.END : Gravity.START) | Gravity.BOTTOM;
            conversationViewHolder.mAnswerLayout.setLayoutParams(params);

            LinearLayout.LayoutParams imageParams = (LinearLayout.LayoutParams) conversationViewHolder.mPhoto.getLayoutParams();
            imageParams.height = mPictureMaxSize;
            conversationViewHolder.mPhoto.setLayoutParams(imageParams);

            GlideApp.with(context)
                    .load(path)
                    .apply(PICTURE_OPTIONS)
                    .into(new ImageViewTarget<Drawable>(conversationViewHolder.mPhoto) {
                        @Override
                        protected void setResource(@Nullable Drawable resource) {
                            ImageView view = getView();
                            runJustBeforeBeingDrawn(view, () -> {
                                if (view.getDrawable() != null) {
                                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
                                    params.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                                    view.setLayoutParams(params);
                                }
                            });
                            view.setImageDrawable(resource);
                        }
                    });

            ((LinearLayout)conversationViewHolder.mAnswerLayout).setGravity(file.isOutgoing() ? Gravity.END : Gravity.START);
            conversationViewHolder.mPhoto.setOnClickListener(v -> {
                Uri contentUri = FileProvider.getUriForFile(v.getContext(), ContentUriHandler.AUTHORITY_FILES, path);
                Intent i = new Intent(context, MediaViewerActivity.class);
                i.setAction(Intent.ACTION_VIEW).setDataAndType(contentUri, "image/*").setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                ActivityOptionsCompat options = ActivityOptionsCompat.
                        makeSceneTransitionAnimation(conversationFragment.getActivity(), conversationViewHolder.mPhoto, "picture");
                conversationFragment.startActivityForResult(i, 3006, options.toBundle());
            });
            return;
        }

        if (file.getEventCode().isError()) {
            conversationViewHolder.icon.setImageResource(R.drawable.ic_warning);
        } else {
            conversationViewHolder.icon.setImageResource(R.drawable.ic_clip_black);
        }

        conversationViewHolder.mMsgTxt.setText(file.getDisplayName());

        ((LinearLayout)conversationViewHolder.mLayout).setGravity(file.isOutgoing() ? Gravity.END : Gravity.START);

        if (file.getEventCode() == DataTransferEventCode.WAIT_HOST_ACCEPTANCE) {
            conversationViewHolder.mAnswerLayout.setVisibility(View.VISIBLE);
            conversationViewHolder.btnAccept.setOnClickListener(v -> {
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
                        .putExtra(DRingService.KEY_TRANSFER_ID, file.getDataTransferId()));
            });
            conversationViewHolder.btnRefuse.setOnClickListener(v -> {
                Context context = v.getContext();
                context.startService(new Intent(DRingService.ACTION_FILE_CANCEL)
                        .setClass(context.getApplicationContext(), DRingService.class)
                        .putExtra(DRingService.KEY_TRANSFER_ID, file.getDataTransferId()));
            });
        } else {
            conversationViewHolder.mAnswerLayout.setVisibility(View.GONE);
        }
    }

    /**
     * Configures the viewholder to display a classic text message, ie. not a call info text message
     *
     * @param convViewHolder The conversation viewHolder
     * @param convElement    The conversation element to display
     * @param position       The position of the viewHolder
     */
    private void configureForTextMessage(final ConversationViewHolder convViewHolder,
                                         final IConversationElement convElement,
                                         int position) {
        if (convViewHolder == null || convElement == null) {
            return;
        }

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
            convViewHolder.mMsgTxt.getBackground().setAlpha(255);
            convViewHolder.mMsgTxt.setTextSize(16.f);
            convViewHolder.mMsgTxt.setPadding(hPadding, vPadding, hPadding, vPadding);
        }

        convViewHolder.mMsgTxt.setText(message);
        if (convViewHolder.mPhoto != null) {
            convViewHolder.mPhoto.setImageBitmap(null);
        }

        boolean shouldSeparateByDetails = this.shouldSeparateByDetails(textMessage, position);
        boolean isConfigSameAsPreviousMsg = this.isMessageConfigSameAsPrevious(textMessage, position);

        if (textMessage.isIncoming() && !isConfigSameAsPreviousMsg) {
            Drawable contactPicture = AvatarFactory.getAvatar(
                    convViewHolder.itemView.getContext(),
                    mPhoto,
                    contact.getUsername(),
                    textMessage.getNumberUri().getHost());

            Glide.with(convViewHolder.itemView.getContext())
                    .load(contactPicture)
                    .apply(AvatarFactory.getGlideOptions(true, true))
                    .into(convViewHolder.mPhoto);
        }

        if (textMessage.getStatus() == TextMessage.Status.SENDING) {
            convViewHolder.mMsgDetailTxt.setVisibility(View.VISIBLE);
            convViewHolder.mMsgDetailTxt.setText(R.string.message_sending);
        } else if (shouldSeparateByDetails) {
            convViewHolder.mMsgDetailTxt.setVisibility(View.VISIBLE);
            String timeSeparationString = computeTimeSeparationStringFromMsgTimeStamp(
                    convViewHolder.itemView.getContext(),
                    textMessage.getDate());
            convViewHolder.mMsgDetailTxt.setText(timeSeparationString);
        } else {
            convViewHolder.mMsgDetailTxt.setVisibility(View.GONE);
        }
    }

    /**
     * Configures the viewholder to display a call info text message, ie. not a classic text message
     *
     * @param convViewHolder The conversation viewHolder
     * @param convElement    The conversation element to display
     */
    private void configureForCallInfoTextMessage(final ConversationViewHolder convViewHolder,
                                                 final IConversationElement convElement) {
        if (convViewHolder == null || convElement == null) {
            return;
        }

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
    private String computeTimeSeparationStringFromMsgTimeStamp(Context context, long timestamp) {
        long now = new Date().getTime();
        if (now - timestamp < MINUTE) {
            return context.getString(R.string.time_just_now);
        } else if (now - timestamp < HOUR) {
            return DateUtils.getRelativeTimeSpanString(timestamp, now, 0, 0).toString();
        } else {
            return DateUtils.formatSameDayTime(timestamp, now, DateFormat.SHORT, DateFormat.SHORT)
                    .toString();
        }
    }

    /**
     * Helper method to return the previous TextMessage relative to an initial position.
     *
     * @param position The initial position
     * @return the previous TextMessage if any, null otherwise
     */
    @Nullable
    private TextMessage getPreviousMessageFromPosition(int position) {
        if (!mConversationElements.isEmpty() && position > 0) {
            IConversationElement conversationElement = mConversationElements.get(position - 1);
            if (conversationElement.getType() == IConversationElement.CEType.TEXT) {
                return (TextMessage) conversationElement;
            }
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
    private TextMessage getNextMessageFromPosition(int position) {
        if (!mConversationElements.isEmpty() && position < mConversationElements.size() - 1) {
            IConversationElement conversationElement = mConversationElements.get(position + 1);
            if (conversationElement.getType() == IConversationElement.CEType.TEXT) {
                return (TextMessage) conversationElement;
            }
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
    private boolean shouldSeparateByDetails(final TextMessage ht, int position) {
        if (ht == null) {
            return false;
        }

        boolean shouldSeparateMsg = false;
        TextMessage previousTextMessage = getPreviousMessageFromPosition(position);
        if (previousTextMessage != null) {
            shouldSeparateMsg = true;
            TextMessage nextTextMessage = getNextMessageFromPosition(position);
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
        TextMessage previousMessage = getPreviousMessageFromPosition(position);
        if (previousMessage != null &&
                previousMessage.isIncoming() &&
                textMessage.isIncoming() &&
                previousMessage.getNumber().equals(textMessage.getNumber())) {
            sameConfig = true;
        }
        return sameConfig;
    }

    public enum ConversationMessageType {
        INCOMING_TEXT_MESSAGE(0),
        OUTGOING_TEXT_MESSAGE(1),
        CALL_INFORMATION_TEXT_MESSAGE(2),
        FILE_TRANSFER(3),
        IMAGE(4);

        int type;

        ConversationMessageType(int p) {
            type = p;
        }

        public int getType() {
            return type;
        }
    }
}