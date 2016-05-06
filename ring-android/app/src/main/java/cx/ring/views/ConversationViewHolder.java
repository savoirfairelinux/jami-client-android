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

package cx.ring.views;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import cx.ring.R;
import cx.ring.adapters.ConversationAdapter;
import cx.ring.model.Conversation;

public class ConversationViewHolder extends RecyclerView.ViewHolder {
    public ViewGroup mTxtEntry;
    public TextView mMsgTxt;
    public TextView mMsgDetailTxt;
    public ImageView mPhoto;
    public ViewGroup mCallEntry;
    public TextView mHistTxt;
    public TextView mHistDetailTxt;
    public long mCid = -1;

    public ConversationViewHolder(ViewGroup v, int type) {
        super(v);
        if (type == ConversationAdapter.ConversationMessageType.CALL_INFORMATION_TEXT_MESSAGE.getType()) {
            mCallEntry = (ViewGroup) v.findViewById(R.id.call_entry);
            mHistTxt = (TextView) v.findViewById(R.id.call_hist_txt);
            mHistDetailTxt = (TextView) v.findViewById(R.id.call_details_txt);
            mPhoto = (ImageView) v.findViewById(R.id.call_icon);
        } else {
            mTxtEntry = (ViewGroup) v.findViewById(R.id.txt_entry);
            mMsgTxt = (TextView) v.findViewById(R.id.msg_txt);
            mMsgDetailTxt = (TextView) v.findViewById(R.id.msg_details_txt);
            if (type == ConversationAdapter.ConversationMessageType.INCOMING_TEXT_MESSAGE.getType()) {
                mPhoto = (ImageView) v.findViewById(R.id.photo);
            }
        }
    }
}
