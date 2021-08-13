/*
 *  Copyright (C) 2004-2021 Savoir-faire Linux Inc.
 *
 *  Author: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *          Rayan Osseiran <rayan.osseiran@savoirfairelinux.com>
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
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package net.jami.model

import net.jami.utils.HashUtils
import net.jami.utils.StringUtils
import java.io.File
import java.io.IOException
import java.lang.Exception

class DataTransfer : Interaction {
    var totalSize: Long = 0
        private set
    var bytesProgress: Long = 0

    //private final String mPeerId;
    private var mExtension: String? = null
    var fileId: String? = null
        private set
    var destination: File? = null
    var daemonPath: File? = null

    /* Legacy constructor */
    constructor(
        conversation: ConversationHistory?,
        peer: String?,
        account: String?,
        displayName: String,
        isOutgoing: Boolean,
        totalSize: Long,
        bytesProgress: Long,
        fileId: String?
    ) {
        author = if (isOutgoing) null else peer
        this.account = account
        this.conversation = conversation
        this.totalSize = totalSize
        this.bytesProgress = bytesProgress
        body = displayName
        mStatus = InteractionStatus.TRANSFER_CREATED.toString()
        mType = InteractionType.DATA_TRANSFER.toString()
        timestamp = System.currentTimeMillis()
        mIsRead = 1
        isIncoming = !isOutgoing
        if (fileId != null) {
            this.fileId = fileId
            try {
                daemonId = fileId.toULong().toLong()
            } catch (e: Exception) {
            }
        }
    }

    constructor(interaction: Interaction) {
        id = interaction.id
        daemonId = interaction.daemonId
        author = interaction.author
        conversation = interaction.conversation
        // mPeerId = interaction.getConversation().getParticipant();
        body = interaction.body
        mStatus = interaction.status.toString()
        mType = interaction.type.toString()
        timestamp = interaction.timestamp
        account = interaction.account
        contact = interaction.contact
        mIsRead = 1
        isIncoming = interaction.isIncoming //mAuthor != null;
    }

    constructor(
        fileId: String?,
        accountId: String,
        peerUri: String,
        displayName: String,
        isOutgoing: Boolean,
        timestamp: Long,
        totalSize: Long,
        bytesProgress: Long
    ) {
        account = accountId
        this.fileId = fileId
        body = displayName
        author = peerUri
        isIncoming = !isOutgoing
        this.totalSize = totalSize
        this.bytesProgress = bytesProgress
        this.timestamp = timestamp
        mType = InteractionType.DATA_TRANSFER.toString()
    }

    val extension: String?
        get() {
            if (body == null) return null
            if (mExtension == null) mExtension = StringUtils.getFileExtension(body!!).lowercase()
            return mExtension
        }
    val isPicture: Boolean
        get() = IMAGE_EXTENSIONS.contains(extension)
    val isAudio: Boolean
        get() = AUDIO_EXTENSIONS.contains(extension)
    val isVideo: Boolean
        get() = VIDEO_EXTENSIONS.contains(extension)
    val isComplete: Boolean
        get() = conversationId == null && isOutgoing || InteractionStatus.TRANSFER_FINISHED.toString() == mStatus

    fun showPicture(): Boolean {
        return isPicture && isComplete
    }

    val storagePath: String
        get() {
            val b = body
            return if (b == null) {
                if (StringUtils.isEmpty(fileId)) { "Error" } else fileId!!
            } else {
                var ext = StringUtils.getFileExtension(b)
                if (ext.length > 8) ext = ext.substring(0, 8)
                val dId = daemonId
                if (dId == null || dId == 0L) {
                    id.toLong().toString() + '_' + HashUtils.sha1(b) + '.' + ext
                } else {
                    dId.toString() + '_' + HashUtils.sha1(b) + '.' + ext
                }
            }
        }

    fun setSize(size: Long) {
        totalSize = size
    }

    val displayName: String
        get() = body!!
    val isOutgoing: Boolean
        get() = !isIncoming
    val isError: Boolean
        get() = status.isError

    fun canAutoAccept(maxSize: Int): Boolean {
        return maxSize == UNLIMITED_SIZE || totalSize <= maxSize
    }

    val publicPath: File?
        get() = if (daemonPath == null) {
            null
        } else try {
            daemonPath!!.canonicalFile
        } catch (e: IOException) {
            null
        }

    companion object {
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif")
        private val AUDIO_EXTENSIONS = setOf("ogg", "mp3", "aac", "flac", "m4a")
        private val VIDEO_EXTENSIONS = setOf("webm", "mp4", "mkv")
        private const val MAX_SIZE = 32 * 1024 * 1024
        private const val UNLIMITED_SIZE = 256 * 1024 * 1024
    }
}