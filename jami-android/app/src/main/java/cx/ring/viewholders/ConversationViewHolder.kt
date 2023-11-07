/*
 *  Copyright (C) 2004-2023 Savoir-faire Linux Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cx.ring.viewholders

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import androidx.core.widget.ContentLoadingProgressBar
import android.view.TextureView
import android.widget.LinearLayout
import android.animation.ValueAnimator
import android.media.MediaPlayer
import android.view.Surface
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import com.google.android.material.chip.Chip
import cx.ring.R
import cx.ring.adapters.MessageType
import cx.ring.views.MessageStatusView
import io.reactivex.rxjava3.disposables.CompositeDisposable

class ConversationViewHolder(v: ViewGroup, val type: MessageType) : RecyclerView.ViewHolder(v) {
    val mItem: View? = when (type) {
        MessageType.INCOMING_TEXT_MESSAGE,
        MessageType.OUTGOING_TEXT_MESSAGE -> v.findViewById(R.id.txt_entry)
        else -> null
    }
    var mMsgTxt: TextView? = null
    var mMsgDetailTxt: TextView? = null
    var mMsgDetailTxtPerm: TextView? = null
    val mAvatar: ImageView? = when (type) {
        MessageType.INCOMING_TEXT_MESSAGE,
        MessageType.ONGOING_GROUP_CALL,
        MessageType.INCOMING_FILE,
        MessageType.INCOMING_IMAGE,
        MessageType.INCOMING_AUDIO,
        MessageType.INCOMING_VIDEO -> v.findViewById(R.id.photo)
        else -> null
    }
    val mImage: ImageView? = when (type) {
        MessageType.INCOMING_IMAGE,
        MessageType.OUTGOING_IMAGE -> v.findViewById(R.id.image)
        MessageType.INCOMING_TEXT_MESSAGE,
        MessageType.OUTGOING_TEXT_MESSAGE -> v.findViewById(R.id.link_preview_img)
        MessageType.CONTACT_EVENT -> v.findViewById(R.id.imageView)
        else -> null
    }
    val mStatusIcon: MessageStatusView? = when (type) {
        MessageType.OUTGOING_TEXT_MESSAGE,
        MessageType.OUTGOING_FILE,
        MessageType.OUTGOING_IMAGE,
        MessageType.OUTGOING_AUDIO,
        MessageType.OUTGOING_VIDEO -> v.findViewById(R.id.status_icon)
        else -> null
    }
    val mReplyName: TextView? = v.findViewById(R.id.msg_reply_name)
    val mReplyTxt: TextView? = v.findViewById(R.id.msg_reply_txt)
    val mInReplyTo: TextView? = v.findViewById(R.id.msg_in_reply_to)
    val mPeerDisplayName: TextView? = when (type){
        MessageType.INCOMING_TEXT_MESSAGE,
        MessageType.ONGOING_GROUP_CALL -> v.findViewById(R.id.msg_display_name)
        else -> null
    }
    val reactionChip: Chip? = v.findViewById(R.id.reaction_chip)
    val mIcon: ImageView? = when (type) {
        MessageType.INCOMING_CALL_INFORMATION,
        MessageType.OUTGOING_CALL_INFORMATION -> v.findViewById(R.id.call_icon)
        MessageType.INCOMING_FILE,
        MessageType.OUTGOING_FILE -> v.findViewById(R.id.file_icon)
        MessageType.COMPOSING_INDICATION -> v.findViewById(R.id.status_icon)
        else -> null
    }
    var mHistTxt: TextView? = null
    var mHistDetailTxt: TextView? = null
    var mPreviewDomain: TextView? = null
    var mLayout: View? = null
    var mAnswerLayout: ViewGroup? = null

    // Ongoing call
    var mCallLayout: LinearLayout? = null
    var mGroupCallLayout: LinearLayout? = null
    var mCallInfoLayout: LinearLayout? = null
    var mAcceptCallVideoButton: ImageButton? = null
    var mAcceptCallAudioButton: ImageButton? = null

    var btnAccept: View? = null
    var btnRefuse: View? = null
    var progress: ContentLoadingProgressBar? = null
    val video: TextureView? = when (type) {
        MessageType.INCOMING_VIDEO,
        MessageType.OUTGOING_VIDEO -> v.findViewById(R.id.video)
        else -> null
    }
    var mMsgLayout: ViewGroup? = null
    var mCallInfoText: TextView? = null
    var mCallAcceptLayout: LinearLayout? = null
    var mFileInfoLayout: LinearLayout? = null
    var mAudioInfoLayout: LinearLayout? = null

    var player: MediaPlayer? = null
    var surface: Surface? = null
    var animator: ValueAnimator? = null
    val compositeDisposable = CompositeDisposable()

    var primaryClickableView: View? = null

    init {
        when (type) {
            MessageType.CONTACT_EVENT -> {
                mMsgTxt = v.findViewById(R.id.contact_event_txt)
                mMsgDetailTxt = v.findViewById(R.id.contact_event_details_txt)
                primaryClickableView = v.findViewById(R.id.contactDetailsGroup)
            }
            MessageType.OUTGOING_CALL_INFORMATION,
            MessageType.INCOMING_CALL_INFORMATION -> {
                mHistTxt = v.findViewById(R.id.call_hist_txt)
                mHistDetailTxt = v.findViewById(R.id.call_details_txt)
                mCallInfoLayout = v.findViewById(R.id.callInfoLayout)
                mCallLayout = v.findViewById(R.id.callLayout)
                mMsgDetailTxtPerm = v.findViewById(R.id.msg_details_txt_perm)
                primaryClickableView = mCallInfoLayout
            }
            MessageType.ONGOING_GROUP_CALL -> {
                mMsgLayout = v.findViewById(R.id.msg_layout)
                mGroupCallLayout = v.findViewById(R.id.groupCallLayout)
                mCallInfoText = v.findViewById(R.id.call_info_text)
                mCallAcceptLayout = v.findViewById(R.id.callAcceptLayout)
                mAcceptCallAudioButton = v.findViewById(R.id.acceptCallAudioButton)
                mAcceptCallVideoButton = v.findViewById(R.id.acceptCallVideoButton)
            }
            MessageType.INCOMING_TEXT_MESSAGE,
            MessageType.OUTGOING_TEXT_MESSAGE -> {
                mMsgTxt = v.findViewById(R.id.msg_txt)
                mMsgDetailTxt = v.findViewById(R.id.msg_details_txt)
                mMsgDetailTxtPerm = v.findViewById(R.id.msg_details_txt_perm)
                mAnswerLayout = v.findViewById(R.id.link_preview)
                mHistTxt = v.findViewById(R.id.link_preview_title)
                mHistDetailTxt = v.findViewById(R.id.link_preview_description)
                mPreviewDomain = v.findViewById(R.id.link_preview_domain)
                primaryClickableView = mMsgTxt
            }
            MessageType.INCOMING_FILE,
            MessageType.OUTGOING_FILE -> {
                mMsgTxt = v.findViewById(R.id.call_hist_filename)
                mMsgDetailTxt = v.findViewById(R.id.file_details_txt)
                mLayout = v.findViewById(R.id.file_layout)
                mFileInfoLayout = v.findViewById(R.id.fileInfoLayout)
                progress = v.findViewById(R.id.progress)
                mAnswerLayout = v.findViewById(R.id.llAnswer)
                btnAccept = v.findViewById(R.id.btnAccept)
                btnRefuse = v.findViewById(R.id.btnRefuse)
                mMsgDetailTxtPerm = v.findViewById(R.id.msg_details_txt_perm)
                primaryClickableView = mFileInfoLayout
            }
            MessageType.INCOMING_IMAGE,
            MessageType.OUTGOING_IMAGE -> {
                mAnswerLayout = v.findViewById(R.id.imageLayout)
                mMsgDetailTxtPerm = v.findViewById(R.id.msg_details_txt_perm)
                mMsgDetailTxt = v.findViewById(R.id.msg_details_txt)
                primaryClickableView = mImage
            }
            MessageType.INCOMING_AUDIO,
            MessageType.OUTGOING_AUDIO -> {
                btnAccept = v.findViewById(R.id.play)
                btnRefuse = v.findViewById(R.id.replay)
                mMsgTxt = v.findViewById(R.id.msg_txt)
                mAudioInfoLayout = v.findViewById(R.id.audioInfoLayout)
                mMsgDetailTxt = v.findViewById(R.id.file_details_txt)
                mMsgDetailTxtPerm = v.findViewById(R.id.msg_details_txt_perm)
                primaryClickableView = mAudioInfoLayout
            }
            MessageType.INCOMING_VIDEO,
            MessageType.OUTGOING_VIDEO -> {
                mLayout = v.findViewById(R.id.video_frame)
                mAnswerLayout = v.findViewById(R.id.imageLayout)
                mMsgDetailTxt = v.findViewById(R.id.msg_details_txt)
                mMsgDetailTxtPerm = v.findViewById(R.id.msg_details_txt_perm)
                primaryClickableView = video
            }
            else ->  {}
        }
    }
}