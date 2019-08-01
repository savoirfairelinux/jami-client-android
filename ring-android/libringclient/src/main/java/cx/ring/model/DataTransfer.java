package cx.ring.model;

import java.util.Set;

import cx.ring.utils.HashUtils;
import cx.ring.utils.StringUtils;

public class DataTransfer extends Interaction {

    private long mTotalSize;
    private long mBytesProgress;
    private String mPeerId;
    private String mExtension;

    private static final Set<String> IMAGE_EXTENSIONS = HashUtils.asSet("jpg", "jpeg", "png", "gif");
    private static final Set<String> AUDIO_EXTENSIONS = HashUtils.asSet("ogg", "mp3", "aac", "flac");
    private static final Set<String> VIDEO_EXTENSIONS = HashUtils.asSet("webm", "mp4", "mkv");
    private static final int MAX_SIZE = 32 * 1024 * 1024;


    public DataTransfer(ConversationHistory conversation, String displayName, boolean isOutgoing, long totalSize, long bytesProgress) {
        mAuthor = isOutgoing ? null : conversation.getParticipant();
        mConversation = conversation;
        mIsIncoming = !isOutgoing;
        mPeerId = conversation.getParticipant();
        mTotalSize = totalSize;
        mBytesProgress = bytesProgress;
        mBody = displayName;
        mStatus = InteractionStatus.TRANSFER_CREATED.toString();
        mType = InteractionType.DATA_TRANSFER.toString();
        mTimestamp = System.currentTimeMillis();
        isRead = 1;
    }


    public DataTransfer(Interaction interaction) {
        mId = interaction.getId();
        mDaemonId = interaction.getDaemonId();
        mAuthor = interaction.getAuthor();
        mConversation = interaction.getConversation();
        mIsIncoming = interaction.isIncoming();
        mPeerId = interaction.getConversation().getParticipant();
        mBody = interaction.getBody();
        mStatus = interaction.getStatus().toString();
        mType = interaction.getType().toString();
        mTimestamp = interaction.getTimestamp();
        mAccount = interaction.getAccount();
        isRead = 1;
    }

    public String getExtension() {
        if (mExtension == null)
            mExtension = StringUtils.getFileExtension(getDisplayName()).toLowerCase();
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
        return isOutgoing() || InteractionStatus.TRANSFER_FINISHED.toString().equals(mStatus);
    }
    public boolean showPicture() {
        return isPicture() && isComplete();
    }

    public String getStoragePath() {
        String ext = StringUtils.getFileExtension(mBody);
        if (ext.length() > 8)
            ext = ext.substring(0, 8);
        return Long.toString(mId) + '_' + HashUtils.sha1(mBody) + '.' + ext;
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

    public void setBytesProgress(long bytesProgress) { mBytesProgress = bytesProgress;
    }

    public String getPeerId() {
        return mPeerId;
    }


    public boolean isError() {
        return mStatus.equals(InteractionStatus.TRANSFER_ERROR.toString()) || mStatus.equals(InteractionStatus.TRANSFER_ERROR.toString()) || mStatus.equals(InteractionStatus.TRANSFER_UNJOINABLE_PEER.toString()) || mStatus.equals(InteractionStatus.TRANSFER_CANCELED.toString());
    }

    public boolean canAutoAccept() {
        return getTotalSize() <= MAX_SIZE;
    }


}
