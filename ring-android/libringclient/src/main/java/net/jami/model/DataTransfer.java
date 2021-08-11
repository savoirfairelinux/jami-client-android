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
package net.jami.model;

import net.jami.utils.HashUtils;
import net.jami.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class DataTransfer extends Interaction {

    private long mTotalSize;
    private long mBytesProgress;
    //private final String mPeerId;
    private String mExtension;
    private String mFileId;
    public File destination;
    private File mDaemonPath;

    private static final Set<String> IMAGE_EXTENSIONS = HashUtils.asSet("jpg", "jpeg", "png", "gif");
    private static final Set<String> AUDIO_EXTENSIONS = HashUtils.asSet("ogg", "mp3", "aac", "flac", "m4a");
    private static final Set<String> VIDEO_EXTENSIONS = HashUtils.asSet("webm", "mp4", "mkv");
    private static final int MAX_SIZE = 32 * 1024 * 1024;
    private static final int UNLIMITED_SIZE = 256 * 1024 * 1024;

    /* Legacy constructor */
    public DataTransfer(ConversationHistory conversation, String peer, String account, String displayName, boolean isOutgoing, long totalSize, long bytesProgress, String fileId) {
        mAuthor = isOutgoing ? null : peer;
        mAccount = account;
        mConversation = conversation;
        mTotalSize = totalSize;
        mBytesProgress = bytesProgress;
        mBody = displayName;
        mStatus = InteractionStatus.TRANSFER_CREATED.toString();
        mType = InteractionType.DATA_TRANSFER.toString();
        mTimestamp = System.currentTimeMillis();
        mIsRead = 1;
        mIsIncoming = !isOutgoing;
        if (fileId != null) {
            mFileId = fileId;
            try {
                mDaemonId = Long.parseUnsignedLong(fileId);
            } catch (Exception e) {

            }
        }
    }

    public DataTransfer(Interaction interaction) {
        mId = interaction.getId();
        mDaemonId = interaction.getDaemonId();
        mAuthor = interaction.getAuthor();
        mConversation = interaction.getConversation();
        // mPeerId = interaction.getConversation().getParticipant();
        mBody = interaction.getBody();
        mStatus = interaction.getStatus().toString();
        mType = interaction.getType().toString();
        mTimestamp = interaction.getTimestamp();
        mAccount = interaction.getAccount();
        mContact = interaction.getContact();
        mIsRead = 1;
        mIsIncoming = interaction.mIsIncoming;//mAuthor != null;
    }

    public DataTransfer(String fileId, String accountId, String peerUri, String displayName, boolean isOutgoing, long timestamp, long totalSize, long bytesProgress) {
        mAccount = accountId;
        mFileId = fileId;
        mBody = displayName;
        mAuthor = peerUri;
        mIsIncoming = !isOutgoing;
        mTotalSize = totalSize;
        mBytesProgress = bytesProgress;
        mTimestamp = timestamp;
        mType = InteractionType.DATA_TRANSFER.toString();
    }

    public String getExtension() {
        if (mBody == null)
            return null;
        if (mExtension == null)
            mExtension = StringUtils.getFileExtension(mBody).toLowerCase();
        return mExtension;
    }


    public boolean isPicture() {
        return IMAGE_EXTENSIONS.contains(getExtension());
    }
    public boolean isAudio() {
        return AUDIO_EXTENSIONS.contains(getExtension());
    }
    public boolean isVideo() {
        return VIDEO_EXTENSIONS.contains(getExtension());
    }

    public boolean isComplete() {
        return (getConversationId() == null && isOutgoing()) || InteractionStatus.TRANSFER_FINISHED.toString().equals(mStatus);
    }
    public boolean showPicture() {
        return isPicture() && isComplete();
    }

    public String getStoragePath() {
        if (mBody == null) {
            if (StringUtils.isEmpty(mFileId)) {
                return "Error";
            }
            return mFileId;
        } else {
            String ext = StringUtils.getFileExtension(mBody);
            if (ext.length() > 8)
                ext = ext.substring(0, 8);
            if (mDaemonId == null || mDaemonId == 0) {
                return Long.toString(mId) + '_' + HashUtils.sha1(mBody) + '.' + ext;
            } else {
                return Long.toString(mDaemonId) + '_' + HashUtils.sha1(mBody) + '.' + ext;
            }
        }
    }

    public void setSize(long size) {
        mTotalSize = size;
    }

    public String getDisplayName() {
        return mBody;
    }

    public boolean isOutgoing() {
        return !mIsIncoming;
    }

    public long getTotalSize() {
        return mTotalSize;
    }

    public long getBytesProgress() {
        return mBytesProgress;
    }

    public void setBytesProgress(long bytesProgress) {
        mBytesProgress = bytesProgress;
    }

    public boolean isError() {
        return getStatus().isError();
    }

    public boolean canAutoAccept(int maxSize) {
        return maxSize == UNLIMITED_SIZE || getTotalSize() <= maxSize;
    }

    public String getFileId() {
        return mFileId;
    }

    public void setDaemonPath(File file) {
        mDaemonPath = file;
    }

    public File getDaemonPath() {
        return mDaemonPath;
    }

    public File getPublicPath() {
        if (mDaemonPath == null) {
            return  null;
        }
        try {
            return mDaemonPath.getCanonicalFile();
        } catch (IOException e) {
            return null;
        }
    }
}
