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
import cx.ring.adapters.BaselineLastLineTextView
import cx.ring.adapters.CustomMessageBubble
import cx.ring.adapters.Message
import cx.ring.adapters.MessageType
import cx.ring.views.MessageStatusView
import io.reactivex.rxjava3.disposables.CompositeDisposable

class ConversationViewHolder(v: ViewGroup, val type: MessageType) : RecyclerView.ViewHolder(v) {
    // Layout messages used to add bottom margins
    val mItem: View? = when (type) {
        MessageType.INCOMING_TEXT_MESSAGE -> v.findViewById(R.id.txt_entry)
        MessageType.OUTGOING_TEXT_MESSAGE -> v.findViewById(R.id.msg_layout)
        else -> null
    }


    // Time and date
    var mMsgDetailTxt: TextView? = null
    var mMsgDetailTxtPerm: TextView? = null

    // Avatar on the left of the conversation
    val mAvatar: ImageView? = when (type) {
        MessageType.INCOMING_TEXT_MESSAGE,
        MessageType.INCOMING_FILE,
        MessageType.INCOMING_IMAGE,
        MessageType.INCOMING_AUDIO,
        MessageType.INCOMING_VIDEO -> v.findViewById(R.id.photo)
        else -> null
    }

    // Images
    val mImage: ImageView? = when (type) {
        MessageType.INCOMING_IMAGE,
        MessageType.OUTGOING_IMAGE -> v.findViewById(R.id.image)
        MessageType.LINK_PREVIEW -> v.findViewById(R.id.link_preview_img)
        MessageType.CONTACT_EVENT -> v.findViewById(R.id.photo)
        else -> null
    }

    // Video
    val video: TextureView? = when (type) {
        MessageType.INCOMING_VIDEO,
        MessageType.OUTGOING_VIDEO -> v.findViewById(R.id.video)
        else -> null
    }


    // Reply messages
    val mReplyName: TextView? = v.findViewById(R.id.reply_contact_name)
    // ici mReplyTxt change le background mais aussi la couleur de la police
    // pour le incoming on veut police = mReplyTxt
    // mais pour le background on veut msg_reply_bubble_content
    val mReplyTxt: TextView? = v.findViewById(R.id.reply_message_txt)

    // Reactions and status icon
    val reactionChip: Chip? = v.findViewById(R.id.reaction_chip)
    val mStatusIcon: MessageStatusView? = when (type) {
        MessageType.OUTGOING_TEXT_MESSAGE,
        MessageType.OUTGOING_FILE,
        MessageType.OUTGOING_IMAGE,
        MessageType.OUTGOING_AUDIO,
        MessageType.OUTGOING_VIDEO -> v.findViewById(R.id.status_icon)
        else -> null
    }
    // Icons for call, file, composing
    val mIcon: ImageView? = when (type) {
        MessageType.CALL_INFORMATION -> v.findViewById(R.id.call_icon)
        MessageType.INCOMING_FILE,
        MessageType.OUTGOING_FILE -> v.findViewById(R.id.file_icon)
        MessageType.COMPOSING_INDICATION -> v.findViewById(R.id.status_icon)
        else -> null
    }

    // Link preview
    var mLinkPreviewTitle: TextView? = null
    var mLinkPreviewDomain: TextView? = null
    var mLayout: View? = null
    var mLinkPreviewLayout: ViewGroup? = null
    var mLinkPreviewDescription: TextView? = null

    // Text messages
    // Message text
    var mMsgTxt: BaselineLastLineTextView? = null
    //
    // TODO
    // Only incoming messages
//    val mInReplyTo: TextView? = v.findViewById(R.id.msg_in_reply_to)
//    val mPeerDisplayName: TextView? = v.findViewById(R.id.msg_display_name)

    //

    // TODO
    // only outgoing messages
    // à utiliser pour changer le fond de la bulle de contact reply to si incoming ou outgoing
    val mReplyContactLayout: ViewGroup? = v.findViewById(R.id.reply_contact_layout)
    // pour l'avatar du reply contact
    val mReplyContactAvatar: ImageView? = v.findViewById(R.id.reply_contact_avatar)
    // utiliser pour changer couleurs background aimantation message
    // ne pas utiliser msgTxt du coup
    val mWhiteBorder : ViewGroup? = v.findViewById(R.id.white_border)
    val mBubbleMessageLayout: ViewGroup? = v.findViewById(R.id.bubble_message_layout)
    // pour le message edité
//    var mEditedMessage: TextView? = v.findViewById(R.id.edited_message)
    // pour modifier uniquement le background de la bulle de reply (a la place de mReplyTxt)
    val mMsgReplyBubbleContent : ViewGroup? = v.findViewById(R.id.msg_reply_bubble_content)

    // pour le calcul de la position de l'heure dans la bulle message
    val mMainBubbleContainer: ViewGroup? = v.findViewById(R.id.main_bubble_container)

    val mMsgReplyContent: ViewGroup? = v.findViewById(R.id.msg_reply_content)
    var message: Message = Message("","",false)
    var mCustomBubble: CustomMessageBubble? = v.findViewById(R.id.custom_message_bubble)
    //


    // Call finished
    var mCallLayout: LinearLayout? = null
    var mCallInfoLayout: LinearLayout? = null
    val mCallLastedInfo: TextView? = v.findViewById(R.id.call_details_txt)
    var mTypeCall: TextView? = null

    // Ongoing group call
    var mAcceptCallVideoButton: ImageButton? = null
    var mAcceptCallAudioButton: ImageButton? = null
    var btnAccept: View? = null
    var btnRefuse: View? = null
    var mAcceptCallLayout: LinearLayout? = null

    // File
    var progress: ContentLoadingProgressBar? = null
    var mFileInfoLayout: LinearLayout? = null
    var mDownloadButton : ViewGroup? = null
    var mFileName: TextView? = null

    // Audio
    var mAudioInfoLayout: LinearLayout? = null
    var btnReplay: ImageView? = null
    var mTimeDuration: TextView? = null

    // Contact event
    var mContactEventTxt: TextView? = null

    // Other
    var player: MediaPlayer? = null
    var surface: Surface? = null
    var animator: ValueAnimator? = null
    val compositeDisposable = CompositeDisposable()

    var primaryClickableView: View? = null

    init {
        when (type) {
            MessageType.CONTACT_EVENT -> {
                mContactEventTxt = v.findViewById(R.id.contact_event_txt)
                mMsgDetailTxt = v.findViewById(R.id.contact_event_details_txt)
                primaryClickableView = v.findViewById(R.id.contactDetailsGroup)
            }
            MessageType.CALL_INFORMATION -> {
                mTypeCall = v.findViewById(R.id.type_call)
                mCallInfoLayout = v.findViewById(R.id.callInfoLayout)
                mCallLayout = v.findViewById(R.id.callLayout)
                mMsgDetailTxtPerm = v.findViewById(R.id.msg_details_txt_perm)
                primaryClickableView = mCallInfoLayout
            }
            MessageType.ONGOING_GROUP_CALL -> {
                mAcceptCallLayout = v.findViewById(R.id.callAcceptLayout)
                mAcceptCallAudioButton = v.findViewById(R.id.acceptCallAudioButton)
                mAcceptCallVideoButton = v.findViewById(R.id.acceptCallVideoButton)
            }
            MessageType.INCOMING_TEXT_MESSAGE-> {
//                mMsgTxt = v.findViewById(R.id.msg_txt)
//                mMsgDetailTxt = v.findViewById(R.id.msg_details_txt)
                mCustomBubble = v.findViewById(R.id.custom_message_bubble)
                mMsgDetailTxtPerm = v.findViewById(R.id.msg_details_txt_perm)
                primaryClickableView = mMsgTxt
            }
            MessageType.OUTGOING_TEXT_MESSAGE -> {
                // ajouter tout
//                mMsgTxt = v.findViewById(R.id.message_text)
//                mMsgDetailTxt = v.findViewById(R.id.message_time)
                mCustomBubble = v.findViewById(R.id.custom_message_bubble)
                mMsgDetailTxtPerm = v.findViewById(R.id.msg_time_perm)
                primaryClickableView = mMsgTxt
            }
            MessageType.INCOMING_FILE,
            MessageType.OUTGOING_FILE -> {
                mFileName = v.findViewById(R.id.call_hist_filename)
                mMsgDetailTxt = v.findViewById(R.id.file_details_txt)
                mLayout = v.findViewById(R.id.file_layout)
                mFileInfoLayout = v.findViewById(R.id.fileInfoLayout)
                progress = v.findViewById(R.id.progress)
                mDownloadButton = v.findViewById(R.id.llAnswer)
                btnAccept = v.findViewById(R.id.btnAccept)
                btnRefuse = v.findViewById(R.id.btnRefuse)
                mMsgDetailTxtPerm = v.findViewById(R.id.msg_details_txt_perm)
                primaryClickableView = mFileInfoLayout
            }
            MessageType.INCOMING_IMAGE,
            MessageType.OUTGOING_IMAGE -> {
                // prob ici imageLayout non utilisé
                mMsgDetailTxtPerm = v.findViewById(R.id.msg_details_txt_perm)
                mMsgDetailTxt = v.findViewById(R.id.msg_details_txt)
                primaryClickableView = mImage
            }
            MessageType.INCOMING_AUDIO,
            MessageType.OUTGOING_AUDIO -> {
                btnAccept = v.findViewById(R.id.play)
                btnReplay = v.findViewById(R.id.replay)
                mTimeDuration = v.findViewById(R.id.time_duration)
                mAudioInfoLayout = v.findViewById(R.id.audioInfoLayout)
                mMsgDetailTxt = v.findViewById(R.id.file_details_txt)
                mMsgDetailTxtPerm = v.findViewById(R.id.msg_details_txt_perm)
                primaryClickableView = mAudioInfoLayout
            }
            MessageType.INCOMING_VIDEO,
            MessageType.OUTGOING_VIDEO -> {
                mLayout = v.findViewById(R.id.video_frame)
                mMsgDetailTxt = v.findViewById(R.id.msg_details_txt)
                mMsgDetailTxtPerm = v.findViewById(R.id.msg_details_txt_perm)
                primaryClickableView = video
            }
            MessageType.LINK_PREVIEW -> {
                mLinkPreviewLayout = v.findViewById(R.id.link_preview)
                mLinkPreviewTitle = v.findViewById(R.id.link_preview_title)
                mLinkPreviewDescription = v.findViewById(R.id.link_preview_description)
                mLinkPreviewDomain = v.findViewById(R.id.link_preview_domain)
                primaryClickableView = mLinkPreviewLayout
            }
            else ->  {}
        }
    }
}