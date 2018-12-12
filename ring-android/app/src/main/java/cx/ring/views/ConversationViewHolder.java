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
package cx.ring.views;

import androidx.recyclerview.widget.RecyclerView;

import android.media.MediaPlayer;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;

import cx.ring.R;
import cx.ring.adapters.ConversationAdapter;
import cx.ring.utils.UiUpdater;

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
    public MediaPlayer player;
    public File playerFile;
    public TextureView video;
    public Surface surface = null;
    public long mCid = -1;
    public UiUpdater updater;

    public ConversationViewHolder(ViewGroup v, ConversationAdapter.MessageType type) {
        super(v);
        if (type == ConversationAdapter.MessageType.INCOMING_TEXT_MESSAGE) {
            mMsgTxt = v.findViewById(R.id.msg_txt);
            mMsgDetailTxt = v.findViewById(R.id.msg_details_txt);
            mPhoto = v.findViewById(R.id.photo);
        } else if (type == ConversationAdapter.MessageType.OUTGOING_TEXT_MESSAGE) {
            mMsgTxt = v.findViewById(R.id.msg_txt);
            mMsgDetailTxt = v.findViewById(R.id.msg_details_txt);
            mPhoto = v.findViewById(R.id.status_icon);
        } else if (type == ConversationAdapter.MessageType.CALL_INFORMATION) {
            mHistTxt = v.findViewById(R.id.call_hist_txt);
            mHistDetailTxt = v.findViewById(R.id.call_details_txt);
            mPhoto = v.findViewById(R.id.call_icon);
        } else if (type == ConversationAdapter.MessageType.FILE_TRANSFER) {
            mMsgTxt = v.findViewById(R.id.call_hist_filename);
            mMsgDetailTxt = v.findViewById(R.id.file_details_txt);
            mLayout = v.findViewById(R.id.file_layout);
            mAnswerLayout = v.findViewById(R.id.llAnswer);
            btnAccept = v.findViewById(R.id.btnAccept);
            btnRefuse = v.findViewById(R.id.btnRefuse);
            progress = v.findViewById(R.id.progress);
            icon = v.findViewById(R.id.file_icon);
        } else if (type == ConversationAdapter.MessageType.IMAGE) {
            mPhoto = v.findViewById(R.id.image);
            mAnswerLayout = v.findViewById(R.id.imageLayout);
            mMsgDetailTxt = v.findViewById(R.id.msg_details_txt);
        } else if (type == ConversationAdapter.MessageType.VIDEO) {
            mLayout = v.findViewById(R.id.video_frame);
            video = v.findViewById(R.id.video);
            mAnswerLayout = v.findViewById(R.id.imageLayout);
            mMsgDetailTxt = v.findViewById(R.id.msg_details_txt);
        } else if (type == ConversationAdapter.MessageType.AUDIO) {
            btnAccept = v.findViewById(R.id.play);
            btnRefuse = v.findViewById(R.id.replay);
            mMsgTxt = v.findViewById(R.id.msg_txt);
            mMsgDetailTxt = v.findViewById(R.id.file_details_txt);
        } else if (type == ConversationAdapter.MessageType.CONTACT_EVENT) {
            mMsgTxt = v.findViewById(R.id.contact_event_txt);
            mMsgDetailTxt = v.findViewById(R.id.contact_event_details_txt);
        }
    }
}
