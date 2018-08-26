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
package cx.ring.views;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import cx.ring.R;
import cx.ring.adapters.ConversationAdapter;

public class ConversationViewHolder extends RecyclerView.ViewHolder {
    public TextView mMsgTxt;
    public TextView mMsgDetailTxt;
    public ImageView mPhoto;
    public TextView mHistTxt;
    public TextView mHistDetailTxt;
    public View mLayout;
    public ViewGroup mAnswerLayout;
    public View btnAccept;
    public View btnRefuse;
    public ImageView icon;
    public ProgressBar progress;
    public long mCid = -1;

    public ConversationViewHolder(ViewGroup v, int type) {
        super(v);
        if (type == ConversationAdapter.ConversationMessageType.CALL_INFORMATION_TEXT_MESSAGE.getType()) {
            mHistTxt = v.findViewById(R.id.call_hist_txt);
            mHistDetailTxt = v.findViewById(R.id.call_details_txt);
            mPhoto = v.findViewById(R.id.call_icon);
        } else if (type == ConversationAdapter.ConversationMessageType.FILE_TRANSFER.getType()) {
            mMsgTxt = v.findViewById(R.id.call_hist_filename);
            mMsgDetailTxt = v.findViewById(R.id.file_details_txt);
            mLayout = v.findViewById(R.id.file_layout);
            mAnswerLayout = v.findViewById(R.id.llAnswer);
            btnAccept = v.findViewById(R.id.btnAccept);
            btnRefuse = v.findViewById(R.id.btnRefuse);
            progress = v.findViewById(R.id.progress);
            icon = v.findViewById(R.id.file_icon);
        } else if (type == ConversationAdapter.ConversationMessageType.IMAGE.getType()) {
            mPhoto = v.findViewById(R.id.image);
            mAnswerLayout = v.findViewById(R.id.imageLayout);
            mMsgDetailTxt = v.findViewById(R.id.msg_details_txt);
        } else if (type == ConversationAdapter.ConversationMessageType.CONTACT_EVENT.getType()) {
            mMsgTxt = v.findViewById(R.id.contact_event_txt);
            mMsgDetailTxt = v.findViewById(R.id.contact_event_details_txt);
        } else {
            mMsgTxt = v.findViewById(R.id.msg_txt);
            mMsgDetailTxt = v.findViewById(R.id.msg_details_txt);
            if (type == ConversationAdapter.ConversationMessageType.INCOMING_TEXT_MESSAGE.getType()) {
                mPhoto = v.findViewById(R.id.photo);
            }
        }
    }
}
