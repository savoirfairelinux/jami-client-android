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
import cx.ring.fragments.ConversationFragment;
import cx.ring.model.DataTransferEventCode;
import cx.ring.model.HistoryCall;
import cx.ring.model.HistoryFileTransfer;
import cx.ring.model.IConversationElement;
import cx.ring.model.TextMessage;
import cx.ring.utils.CircleTransform;
import cx.ring.utils.FileUtils;
import cx.ring.utils.ResourceMapper;
import cx.ring.views.ConversationViewHolder;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationViewHolder> {
    private final static String TAG = ConversationAdapter.class.getSimpleName();

    private static final double MINUTE = 60L * 1000L;
    private static final double HOUR = 3600L * 1000L;

    private final ArrayList<IConversationElement> mConversationElements = new ArrayList<>();
    private final ConversationPresenter presenter;
    private final ConversationFragment conversationFragment;
    private byte[] mPhoto;

    public ConversationAdapter(ConversationFragment conversationFragment, ConversationPresenter presenter) {
        this.conversationFragment = conversationFragment;
        this.presenter = presenter;
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
                return ConversationMessageType.FILE_TRANSFER_TEXT_MESSAGE.getType();
            }
            if (conversationElement.getType() == IConversationElement.CEType.CALL) {
                return ConversationMessageType.CALL_INFORMATION_TEXT_MESSAGE.getType();
            }
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

    private void configureForFileInfoTextMessage(final ConversationViewHolder conversationViewHolder,
                                                 final IConversationElement conversationElement) {
        if (conversationViewHolder == null || conversationElement == null) {
            return;
        }
        HistoryFileTransfer file = (HistoryFileTransfer) conversationElement;

        if (file.getDataTransferEventCode().isError()) {
            conversationViewHolder.icon.setImageResource(R.drawable.ic_warning);
        } else {
            conversationViewHolder.icon.setImageResource(R.drawable.ic_clip_black);
        }

        conversationViewHolder.mMsgTxt.setText(file.getDisplayName());

        String timeSeparationString = computeTimeSeparationStringFromMsgTimeStamp(
                conversationViewHolder.itemView.getContext(),
                file.getDate());
        conversationViewHolder.mMsgDetailTxt.setText(String.format("%s - %s - %s",
                timeSeparationString, FileUtils.readableFileSize(file.getTotalSize()),
                ResourceMapper.getReadableFileTransferStatus(conversationFragment.getActivity(), file.getDataTransferEventCode())));
        if (file.isOutgoing()) {
            conversationViewHolder.mPhoto.setImageResource(R.drawable.ic_outgoing_black);
        } else {
            conversationViewHolder.mPhoto.setImageResource(R.drawable.ic_incoming_black);
        }

        if (file.getDataTransferEventCode() == DataTransferEventCode.WAIT_HOST_ACCEPTANCE) {
            conversationViewHolder.mAnswerLayout.setVisibility(View.VISIBLE);
            conversationViewHolder.btnAccept.setOnClickListener(v -> {

                if (!presenter.getDeviceRuntimeService().hasWriteExternalStoragePermission()) {
                    conversationFragment.askWriteExternalStoragePermission();
                    return;
                }

                File cacheDir = conversationFragment.getActivity().getCacheDir();
                if (!cacheDir.exists()) {
                    boolean mkdirs = cacheDir.mkdirs();
                    if (!mkdirs) {
                        Log.e(TAG, "configureForFileInfoTextMessage: not able to create directory at " + cacheDir.toString());
                        return;
                    }
                }

                long spaceLeft = FileUtils.getSpaceLeft(cacheDir.toString());
                if (spaceLeft == -1L || file.getTotalSize() > spaceLeft) {
                    presenter.noSpaceLeft();
                    return;
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

                conversationViewHolder.mAnswerLayout.setVisibility(View.GONE);
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
                                         final IConversationElement convElement,
                                         int position) {
        if (convViewHolder == null || convElement == null) {
            return;
        }

        TextMessage ht = (TextMessage) convElement;

        convViewHolder.mCid = ht.getContact().getId();
        convViewHolder.mMsgTxt.setText(ht.getMessage());
        if (convViewHolder.mPhoto != null) {
            convViewHolder.mPhoto.setImageBitmap(null);
        }

        boolean shouldSeparateByDetails = this.shouldSeparateByDetails(ht, position);
        boolean isConfigSameAsPreviousMsg = this.isMessageConfigSameAsPrevious(ht, position);

        if (ht.isIncoming() && !isConfigSameAsPreviousMsg) {
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

        if (ht.getStatus() == TextMessage.Status.SENDING) {
            convViewHolder.mMsgDetailTxt.setVisibility(View.VISIBLE);
            convViewHolder.mMsgDetailTxt.setText(R.string.message_sending);
        } else if (shouldSeparateByDetails) {
            convViewHolder.mMsgDetailTxt.setVisibility(View.VISIBLE);
            String timeSeparationString = computeTimeSeparationStringFromMsgTimeStamp(
                    convViewHolder.itemView.getContext(),
                    ht.getDate());
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
     * @param textMessage       The conversationElement at the given position
     * @param position The position of the current message
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