/*
 *  Copyright (C) 2015-2016 Savoir-faire Linux Inc.
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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import cx.ring.R;
import cx.ring.model.Conversation;
import cx.ring.model.TextMessage;
import cx.ring.views.ConversationViewHolder;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationViewHolder> {
    private final static String TAG = ConversationAdapter.class.getSimpleName();

    private static final double MINUTE = 60L * 1000L;
    private static final double HOUR = 3600L * 1000L;

    private final ArrayList<Conversation.ConversationElement> mTexts = new ArrayList<>();

    public enum ConversationMessageType {
        INCOMING_TEXT_MESSAGE(0),
        OUTGOING_TEXT_MESSAGE(1),
        CALL_INFORMATION_TEXT_MESSAGE(2);

        int type;

        ConversationMessageType(int p) {
            type = p;
        }

        public int getType() {
            return type;
        }
    }

    /**
     * Refreshes the data and notifies the changes
     *
     * @param list an arraylist of ConversationElement
     * @param id   the message id initiating the update, 0 if full refresh
     */
    public void updateDataset(final ArrayList<Conversation.ConversationElement> list, long id) {
        Log.d(TAG, "updateDataset, list size: " + list.size() + " - mId: " + id);
        if (list.size() == mTexts.size()) {
            if (id != 0) {
                notifyDataSetChanged();
            }
            return;
        }
        mTexts.clear();
        mTexts.addAll(list);
        notifyDataSetChanged();
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
                return ConversationMessageType.INCOMING_TEXT_MESSAGE.getType();
            else
                return ConversationMessageType.OUTGOING_TEXT_MESSAGE.getType();
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
        } else {
            res = R.layout.item_conv_call;
        }
        ViewGroup v = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(res, parent, false);
        return new ConversationViewHolder(v, viewType);
    }

    @Override
    public void onBindViewHolder(ConversationViewHolder conversationViewHolder, int position) {
        Conversation.ConversationElement textElement = mTexts.get(position);
        if (textElement.text != null) {
            this.configureForTextMessage(conversationViewHolder, textElement, position);
        } else {
            this.configureForCallInfoTextMessage(conversationViewHolder, textElement);
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
            //this.setImage(convViewHolder, convElement);
            Glide.with(convViewHolder.itemView.getContext())
                    .load(R.drawable.ic_contact_picture)
                    .crossFade()
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
                pictureResID = R.drawable.ic_call_missed_black;
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
                    R.drawable.ic_call_received_black :
                    R.drawable.ic_call_made_black;
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
        if (!mTexts.isEmpty() && position > 0) {
            return mTexts.get(position - 1).text;
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
        if (!mTexts.isEmpty() && position < mTexts.size() - 1) {
            return mTexts.get(position + 1).text;
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
}