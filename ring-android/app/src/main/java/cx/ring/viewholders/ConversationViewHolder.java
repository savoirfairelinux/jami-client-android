/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
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
package cx.ring.viewholders;

import androidx.core.widget.ContentLoadingProgressBar;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.ValueAnimator;
import android.media.MediaPlayer;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import cx.ring.R;
import cx.ring.adapters.ConversationAdapter;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class ConversationViewHolder extends RecyclerView.ViewHolder {
    public ConversationAdapter.MessageType type;
    public View mItem;
    public TextView mMsgTxt;
    public TextView mMsgDetailTxt;
    public TextView mMsgDetailTxtPerm;
    public ImageView mAvatar;
    public ImageView mImage;
    public ImageView mStatusIcon;
    public ImageView mIcon;
    public TextView mHistTxt;
    public TextView mHistDetailTxt;
    public TextView mPreviewDomain;
    public View mLayout;
    public ViewGroup mAnswerLayout;
    public View btnAccept;
    public View btnRefuse;
    public ContentLoadingProgressBar progress;
    public MediaPlayer player;
    public TextureView video;
    public Surface surface = null;
    public LinearLayout mCallInfoLayout, mFileInfoLayout, mAudioInfoLayout;
    public ValueAnimator animator;

    public final CompositeDisposable compositeDisposable = new CompositeDisposable();

    public ConversationViewHolder(ViewGroup v, ConversationAdapter.MessageType type) {
        super(v);
        this.type = type;
        if (type == ConversationAdapter.MessageType.CONTACT_EVENT) {
            mMsgTxt = v.findViewById(R.id.contact_event_txt);
            mMsgDetailTxt = v.findViewById(R.id.contact_event_details_txt);
        } else if (type == ConversationAdapter.MessageType.CALL_INFORMATION) {
            mHistTxt = v.findViewById(R.id.call_hist_txt);
            mHistDetailTxt = v.findViewById(R.id.call_details_txt);
            mIcon = v.findViewById(R.id.call_icon);
            mCallInfoLayout = v.findViewById(R.id.callInfoLayout);
        } else if (type != ConversationAdapter.MessageType.INVALID) {
            switch (type) {
                // common layout elements
                case INCOMING_TEXT_MESSAGE:
                case OUTGOING_TEXT_MESSAGE:
                    mItem = v.findViewById(R.id.txt_entry);
                    mMsgTxt = v.findViewById(R.id.msg_txt);
                    mMsgDetailTxt = v.findViewById(R.id.msg_details_txt);
                    mMsgDetailTxtPerm = v.findViewById(R.id.msg_details_txt_perm);
                    mAnswerLayout = v.findViewById(R.id.link_preview);
                    mImage = v.findViewById(R.id.link_preview_img);
                    mHistTxt = v.findViewById(R.id.link_preview_title);
                    mHistDetailTxt = v.findViewById(R.id.link_preview_description);
                    mPreviewDomain = v.findViewById(R.id.link_preview_domain);
                    break;
                case INCOMING_FILE:
                case OUTGOING_FILE:
                    mMsgTxt = v.findViewById(R.id.call_hist_filename);
                    mMsgDetailTxt = v.findViewById(R.id.file_details_txt);
                    mLayout = v.findViewById(R.id.file_layout);
                    mFileInfoLayout = v.findViewById(R.id.fileInfoLayout);
                    mIcon = v.findViewById(R.id.file_icon);
                    progress = v.findViewById(R.id.progress);
                    mAnswerLayout = v.findViewById(R.id.llAnswer);
                    btnAccept = v.findViewById(R.id.btnAccept);
                    btnRefuse = v.findViewById(R.id.btnRefuse);
                    mMsgDetailTxtPerm = v.findViewById(R.id.msg_details_txt_perm);
                    break;
                case INCOMING_IMAGE:
                case OUTGOING_IMAGE:
                    mImage = v.findViewById(R.id.image);
                    mAnswerLayout = v.findViewById(R.id.imageLayout);
                    mMsgDetailTxtPerm = v.findViewById(R.id.msg_details_txt_perm);
                    mMsgDetailTxt = v.findViewById(R.id.msg_details_txt);
                    break;
                case INCOMING_AUDIO:
                case OUTGOING_AUDIO:
                    btnAccept = v.findViewById(R.id.play);
                    btnRefuse = v.findViewById(R.id.replay);
                    mMsgTxt = v.findViewById(R.id.msg_txt);
                    mAudioInfoLayout = v.findViewById(R.id.audioInfoLayout);
                    mMsgDetailTxt = v.findViewById(R.id.file_details_txt);
                    mMsgDetailTxtPerm = v.findViewById(R.id.msg_details_txt_perm);
                    break;
                case INCOMING_VIDEO:
                case OUTGOING_VIDEO:
                    mLayout = v.findViewById(R.id.video_frame);
                    video = v.findViewById(R.id.video);
                    mAnswerLayout = v.findViewById(R.id.imageLayout);
                    mMsgDetailTxt = v.findViewById(R.id.msg_details_txt);
                    mMsgDetailTxtPerm = v.findViewById(R.id.msg_details_txt_perm);
                    break;
                case COMPOSING_INDICATION:
                    mStatusIcon = v.findViewById(R.id.status_icon);
            }
            // msg-direction-specific layout elements
            switch (type) {
                case INCOMING_TEXT_MESSAGE:
                case INCOMING_FILE:
                case INCOMING_IMAGE:
                case INCOMING_AUDIO:
                case INCOMING_VIDEO:
                    mAvatar = v.findViewById(R.id.photo);
                    break;
                case OUTGOING_TEXT_MESSAGE:
                case OUTGOING_FILE:
                case OUTGOING_IMAGE:
                case OUTGOING_AUDIO:
                case OUTGOING_VIDEO:
                    mStatusIcon = v.findViewById(R.id.status_icon);
                    break;
            }
        }
    }
}
