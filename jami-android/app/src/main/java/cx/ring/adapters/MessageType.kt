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
package cx.ring.adapters

import androidx.annotation.LayoutRes
import cx.ring.R

enum class MessageType(@LayoutRes val layout: Int, @LayoutRes val tvLayout: Int = layout) {
    INCOMING_FILE(R.layout.item_conv_file_peer, R.layout.item_conv_file_peer_tv),
    INCOMING_IMAGE(R.layout.item_conv_image_peer, R.layout.item_conv_image_peer_tv),
    INCOMING_AUDIO(R.layout.item_conv_audio_peer, R.layout.item_conv_audio_peer_tv),
    INCOMING_VIDEO(R.layout.item_conv_video_peer, R.layout.item_conv_video_peer_tv),
    OUTGOING_FILE(R.layout.item_conv_file_me, R.layout.item_conv_file_me_tv),
    OUTGOING_IMAGE(R.layout.item_conv_image_me, R.layout.item_conv_image_me_tv),
    OUTGOING_AUDIO(R.layout.item_conv_audio_me, R.layout.item_conv_audio_me_tv),
    OUTGOING_VIDEO(R.layout.item_conv_video_me, R.layout.item_conv_video_me_tv),
    CONTACT_EVENT(R.layout.item_conv_contact, R.layout.item_conv_contact_tv),
    CALL_INFORMATION(R.layout.item_conv_call, R.layout.item_conv_call_tv),
    ONGOING_GROUP_CALL(R.layout.item_conv_group_call, R.layout.item_conv_group_call_tv),
    INCOMING_TEXT_MESSAGE(R.layout.item_conv_msg_peer, R.layout.item_conv_msg_peer_tv),
    OUTGOING_TEXT_MESSAGE(R.layout.item_conv_msg_me, R.layout.item_conv_msg_me_tv),
    LINK_PREVIEW(R.layout.msg_link_preview),
    COMPOSING_INDICATION(R.layout.item_conv_composing),
    HEADER(R.layout.tv_header_blank),
    INVALID(-1);

    val isFile: Boolean
        get() = this == INCOMING_FILE || this == OUTGOING_FILE
    val isAudio: Boolean
        get() = this == INCOMING_AUDIO || this == OUTGOING_AUDIO
    val isVideo: Boolean
        get() = this == INCOMING_VIDEO || this == OUTGOING_VIDEO
    val isImage: Boolean
        get() = this == INCOMING_IMAGE || this == OUTGOING_IMAGE
    val transferType: TransferType
        get() = when {
            isFile -> TransferType.FILE
            isImage -> TransferType.IMAGE
            isAudio -> TransferType.AUDIO
            isVideo -> TransferType.VIDEO
            else -> TransferType.FILE
        }

    enum class TransferType { FILE, IMAGE, AUDIO, VIDEO }
}