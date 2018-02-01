/*
 *  Copyright (C) 2015-2017 Savoir-faire Linux Inc.
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

import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import cx.ring.R;
import cx.ring.conversation.ConversationPresenter;
import cx.ring.model.Conversation;
import cx.ring.model.DataTransferEventCode;
import cx.ring.model.HistoryFileTransfer;
import cx.ring.model.TextMessage;
import cx.ring.utils.CircleTransform;
import cx.ring.utils.FileUtils;
import cx.ring.utils.ResourceMapper;
import cx.ring.views.ConversationViewHolder;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationViewHolder> {
    private final static String TAG = ConversationAdapter.class.getSimpleName();

    private static final double MINUTE = 60L * 1000L;
    private static final double HOUR = 3600L * 1000L;

    private final ArrayList<Conversation.ConversationElement> mConversationElements = new ArrayList<>();
    private final ConversationPresenter presenter;
    private final Activity activity;
    private byte[] mPhoto;

    public ConversationAdapter(Activity activity, ConversationPresenter presenter) {
        this.activity = activity;
        this.presenter = presenter;
    }

    /**
     * Refreshes the data and notifies the changes
     *
     * @param list an arraylist of ConversationElement
     */
    public void updateDataset(final ArrayList<Conversation.ConversationElement> list) {
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
        Conversation.ConversationElement conversationElement = mConversationElements.get(position);
        if (conversationElement.text != null) {
            if (conversationElement.text.isIncoming()) {
                return ConversationMessageType.INCOMING_TEXT_MESSAGE.getType();
            } else {
                return ConversationMessageType.OUTGOING_TEXT_MESSAGE.getType();
            }
        }
        if (conversationElement.file != null) {
            return ConversationMessageType.FILE_TRANSFER_TEXT_MESSAGE.getType();
        }
        return ConversationMessageType.CALL_INFORMATION_TEXT_MESSAGE.getType();
    }

    @Override
    public ConversationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int res;
        if (viewType == ConversationMessageType.INCOMING_TEXT_MESSAGE.getType()) {
            res = R.layout.item_conv_msg_peer;
        } else if (viewType == ConversationMessageType.OUTGOING_TEXT_MESSAGE.getType()) {
            res = R.layout.item_conv_msg_me;
        } else if (viewType == ConversationMessageType.FILE_TRANSFER_TEXT_MESSAGE.getType()) {
            res = R.layout.item_conv_file;
        } else {
            res = R.layout.item_conv_call;
        }
        ViewGroup v = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(res, parent, false);
        return new ConversationViewHolder(v, viewType);
    }

    @Override
    public void onBindViewHolder(ConversationViewHolder conversationViewHolder, int position) {
        Conversation.ConversationElement textElement = mConversationElements.get(position);
        if (textElement.text != null) {
            this.configureForTextMessage(conversationViewHolder, textElement, position);
        } else if (textElement.file != null) {
            this.configureForFileInfoTextMessage(conversationViewHolder, textElement);
        } else if (textElement.call != null) {
            this.configureForCallInfoTextMessage(conversationViewHolder, textElement);
        }
    }

    private void configureForFileInfoTextMessage(final ConversationViewHolder conversationViewHolder,
                                                 final Conversation.ConversationElement conversationElement) {
        if (conversationViewHolder == null || conversationElement == null) {
            return;
        }
        HistoryFileTransfer file = conversationElement.file;
        if (file == null) {
            Log.d(TAG, "configureForFileInfoTextMessage: not able to get file from conversationElement");
            return;
        }

        if (file.getDataTransferEventCode().isError()) {
            conversationViewHolder.icon.setImageResource(R.drawable.ic_warning);
        }

        conversationViewHolder.mMsgTxt.setText(file.getDisplayName());

        String timeSeparationString = computeTimeSeparationStringFromMsgTimeStamp(
                conversationViewHolder.itemView.getContext(),
                file.getTimestamp());
        conversationViewHolder.mMsgDetailTxt.setText(String.format("%s - %s - %s",
                timeSeparationString, FileUtils.readableFileSize(file.getTotalSize()),
                ResourceMapper.getReadableFileTransferStatus(activity, file.getDataTransferEventCode())));
        if (file.isOutgoing()) {
            conversationViewHolder.mPhoto.setImageResource(R.drawable.ic_outgoing_black);
        } else {
            conversationViewHolder.mPhoto.setImageResource(R.drawable.ic_incoming_black);
        }

        if (file.getDataTransferEventCode() == DataTransferEventCode.WAIT_HOST_ACCEPTANCE) {
            conversationViewHolder.mAnswerLayout.setVisibility(View.VISIBLE);
            conversationViewHolder.btnAccept.setOnClickListener(v -> {
                conversationViewHolder.mAnswerLayout.setVisibility(View.GONE);

                if (!FileUtils.isExternalStorageWritable()) {
                    Log.e(TAG, "configureForFileInfoTextMessage: external storage is not writable");
                    return;
                }

                File cacheDir = activity.getCacheDir();
                if (!cacheDir.exists()) {
                    boolean mkdirs = cacheDir.mkdirs();
                    if (!mkdirs) {
                        Log.e(TAG, "configureForFileInfoTextMessage: not able to create directory at " + cacheDir.toString());
                        return;
                    }
                }

                File cacheFile = new File(cacheDir, file.getDisplayName());
                if (cacheFile.exists()) {
                    boolean delete = cacheFile.delete();
                    if (!delete) {
                        Log.e(TAG, "configureForFileInfoTextMessage: not able to delete cache file at " + cacheFile.toString());
                        return;
                    }
                }

                Log.d(TAG, "configureForFileInfoTextMessage: cacheFile=" + cacheFile + ",exists=" + cacheFile.exists());

                presenter.acceptTransfer(file.getDataTransferId(), cacheFile.toString());
            });
            conversationViewHolder.btnRefuse.setOnClickListener(v -> {
                conversationViewHolder.mAnswerLayout.setVisibility(View.GONE);
                presenter.cancelTransfer(file.getDataTransferId());
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
                                         final Conversation.ConversationElement convElement,
                                         int position) {
        if (convViewHolder == null || convElement == null || convElement.text == null) {
            return;
        }

        convViewHolder.mCid = convElement.text.getContact().getId();
        convViewHolder.mMsgTxt.setText(convElement.text.getMessage());
        if (convViewHolder.mPhoto != null) {
            convViewHolder.mPhoto.setImageBitmap(null);
        }

        boolean shouldSeparateByDetails = this.shouldSeparateByDetails(convElement, position);
        boolean isConfigSameAsPreviousMsg = this.isMessageConfigSameAsPrevious(convElement, position);

        if (convElement.text.isIncoming() && !isConfigSameAsPreviousMsg) {
            Glide.with(convViewHolder.itemView.getContext())
                    .fromBytes()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(false)
                    .load(mPhoto)
                    .crossFade()
                    .placeholder(R.drawable.ic_contact_picture)
                    .transform(new CircleTransform(convViewHolder.itemView.getContext()))
                    .error(R.drawable.ic_contact_picture)
                    .into(convViewHolder.mPhoto);
        }

        if (convElement.text.getStatus() == TextMessage.Status.SENDING) {
            convViewHolder.mMsgDetailTxt.setVisibility(View.VISIBLE);
            convViewHolder.mMsgDetailTxt.setText(R.string.message_sending);
        } else if (shouldSeparateByDetails) {
            convViewHolder.mMsgDetailTxt.setVisibility(View.VISIBLE);
            String timeSeparationString = computeTimeSeparationStringFromMsgTimeStamp(
                    convViewHolder.itemView.getContext(),
                    convElement.text.getTimestamp());
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
                                                 final Conversation.ConversationElement convElement) {
        if (convViewHolder == null || convElement == null || convElement.call == null) {
            return;
        }

        int pictureResID;
        String historyTxt;
        convViewHolder.mPhoto.setScaleY(1);
        Context context = convViewHolder.itemView.getContext();

        if (convElement.call.isMissed()) {
            if (convElement.call.isIncoming()) {
                pictureResID = R.drawable.ic_call_missed_incoming_black;
            } else {
                pictureResID = R.drawable.ic_call_missed_outgoing_black;
                // Flip the photo upside down to show a "missed outgoing call"
                convViewHolder.mPhoto.setScaleY(-1);
            }
            historyTxt = convElement.call.isIncoming() ?
                    context.getString(R.string.notif_missed_incoming_call) :
                    context.getString(R.string.notif_missed_outgoing_call);
        } else {
            pictureResID = (convElement.call.isIncoming()) ?
                    R.drawable.ic_incoming_black :
                    R.drawable.ic_outgoing_black;
            historyTxt = convElement.call.isIncoming() ?
                    context.getString(R.string.notif_incoming_call) :
                    context.getString(R.string.notif_outgoing_call);
        }

        convViewHolder.mCid = convElement.call.getContactID();
        convViewHolder.mPhoto.setImageResource(pictureResID);
        convViewHolder.mHistTxt.setText(historyTxt);
        convViewHolder.mHistDetailTxt.setText(DateFormat.getDateTimeInstance()
                .format(convElement.call.getStartDate()));
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
            return mConversationElements.get(position - 1).text;
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
            return mConversationElements.get(position + 1).text;
        }
        return null;
    }

    /**
     * Helper used to determine if a text details string should be displayed under a message at a
     * certain position.
     *
     * @param convElement The conversationElement at the given position
     * @param position    The position of the current message
     * @return true if a text details string should be displayed under the message
     */
    private boolean shouldSeparateByDetails(final Conversation.ConversationElement convElement,
                                            int position) {
        if (convElement == null || convElement.text == null) {
            return false;
        }

        boolean shouldSeparateMsg = false;
        TextMessage previousTextMessage = this.getPreviousMessageFromPosition(position);
        if (previousTextMessage != null) {
            shouldSeparateMsg = true;
            TextMessage nextTextMessage = this.getNextMessageFromPosition(position);
            if (nextTextMessage != null) {
                long diff = nextTextMessage.getTimestamp() - convElement.text.getTimestamp();
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
     * @param convElement The conversationElement at the given position
     * @param position    The position of the current message
     * @return true if the configuration is the same as the previous message, false otherwise.
     */
    private boolean isMessageConfigSameAsPrevious(final Conversation.ConversationElement convElement,
                                                  int position) {
        if (convElement == null || convElement.text == null) {
            return false;
        }

        boolean sameConfig = false;
        TextMessage previousMessage = this.getPreviousMessageFromPosition(position);
        if (previousMessage != null &&
                previousMessage.isIncoming() &&
                convElement.text.isIncoming() &&
                previousMessage.getNumber().equals(convElement.text.getNumber())) {
            sameConfig = true;
        }
        return sameConfig;
    }

    public enum ConversationMessageType {
        INCOMING_TEXT_MESSAGE(0),
        OUTGOING_TEXT_MESSAGE(1),
        CALL_INFORMATION_TEXT_MESSAGE(2),
        FILE_TRANSFER_TEXT_MESSAGE(3);

        int type;

        ConversationMessageType(int p) {
            type = p;
        }

        public int getType() {
            return type;
        }
    }
}