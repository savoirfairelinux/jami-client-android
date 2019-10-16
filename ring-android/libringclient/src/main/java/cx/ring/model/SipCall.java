/*
 *  Copyright (C) 2004-2019 Savoir-faire Linux Inc.
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cx.ring.model;


import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import cx.ring.utils.ProfileChunk;
import cx.ring.utils.StringUtils;
import cx.ring.utils.VCardUtils;
import ezvcard.Ezvcard;
import ezvcard.VCard;

public class SipCall extends Interaction {

    public final static String KEY_ACCOUNT_ID = "ACCOUNTID";
    public final static String KEY_AUDIO_ONLY = "AUDIO_ONLY";
    public final static String KEY_CALL_TYPE = "CALL_TYPE";
    public final static String KEY_CALL_STATE = "CALL_STATE";
    public final static String KEY_PEER_NUMBER = "PEER_NUMBER";
    public final static String KEY_PEER_HOLDING = "PEER_HOLDING";
    public final static String KEY_AUDIO_MUTED = "PEER_NUMBER";
    public final static String KEY_VIDEO_MUTED = "VIDEO_MUTED";
    public final static String KEY_AUDIO_CODEC = "AUDIO_CODEC";
    public final static String KEY_VIDEO_CODEC = "VIDEO_CODEC";
    public final static String KEY_DURATION = "duration";
    public final static String KEY_CONF_ID = "CONF_ID";

    private boolean isPeerHolding = false;
    private boolean isAudioMuted = false;
    private boolean isVideoMuted = false;
    private boolean isRecording = false;
    private boolean isAudioOnly = false;

    private CallStatus mCallStatus = CallStatus.NONE;

    private long timestampEnd = 0;
    private boolean missed = true;
    private String mAudioCodec;
    private String mVideoCodec;
    private String mContactNumber;
    private String mConfId;

    private ProfileChunk mProfileChunk = null;

    public SipCall(String daemonId, String author, String account, ConversationHistory conversation, CallContact contact, Direction direction) {
        mDaemonId = daemonId == null ? null : Long.parseLong(daemonId);
        mAuthor = direction == Direction.INCOMING ? author : null;
        mAccount = account;
        mConversation = conversation;
        mIsIncoming = direction == Direction.INCOMING;
        mTimestamp = System.currentTimeMillis();
        mType = InteractionType.CALL.toString();
        mContact = contact;
        mIsRead = 1;
    }

    public SipCall(Interaction interaction) {
        mId = interaction.getId();
        mAuthor = interaction.getAuthor();
        mConversation = interaction.getConversation();
        mIsIncoming = mAuthor != null;
        mTimestamp = interaction.getTimestamp();
        mType = InteractionType.CALL.toString();
        mStatus = interaction.getStatus().toString();
        mDaemonId = interaction.getDaemonId();
        mIsRead = interaction.isRead() ? 1 : 0;
        mAccount = interaction.getAccount();
        mExtraFlag = fromJson(interaction.getExtraFlag());
        missed = getDuration() == 0;
        mIsRead = 1;
        mContact = interaction.getContact();
    }

    public SipCall(String daemonId, String account, String contactNumber, Direction direction) {
        mDaemonId = daemonId == null ? null : Long.parseLong(daemonId);
        mIsIncoming = direction == Direction.INCOMING;
        mAccount = account;
        mAuthor = direction == Direction.INCOMING ? contactNumber : null;
        mContactNumber = contactNumber;
        mTimestamp = System.currentTimeMillis();
        mType = InteractionType.CALL.toString();
        mIsRead = 1;
    }

    public SipCall(String daemonId, Map<String, String> call_details) {
        this(daemonId, call_details.get(KEY_ACCOUNT_ID), call_details.get(KEY_PEER_NUMBER), Direction.fromInt(Integer.parseInt(call_details.get(KEY_CALL_TYPE))));
        setCallState(CallStatus.fromString(call_details.get(KEY_CALL_STATE)));
        setDetails(call_details);
    }

    public void setDetails(Map<String, String> details) {
        isPeerHolding = "true".equals(details.get(KEY_PEER_HOLDING));
        isAudioMuted = "true".equals(details.get(KEY_AUDIO_MUTED));
        isVideoMuted = "true".equals(details.get(KEY_VIDEO_MUTED));
        isAudioOnly = "true".equals(details.get(KEY_AUDIO_ONLY));
        mAudioCodec = details.get(KEY_AUDIO_CODEC);
        mVideoCodec = details.get(KEY_VIDEO_CODEC);
        String confId = details.get(KEY_CONF_ID);
        mConfId = StringUtils.isEmpty(confId) ? null : confId;
    }

    public boolean isConferenceParticipant() {
        return mConfId != null;
    }

    public String getContactNumber() {
        return mContactNumber;
    }

    public Long getDuration() {
        return toJson(mExtraFlag).get(KEY_DURATION) == null ? 0 : toJson(mExtraFlag).get(KEY_DURATION).getAsLong();
    }

    public void setDuration(Long value) {
        JsonObject jsonObject = getExtraFlag();
        jsonObject.addProperty(KEY_DURATION, value);
        mExtraFlag = fromJson(jsonObject);
    }

    public String getDurationString() {
        Long mDuration = getDuration() / 1000;
        if (mDuration < 60) {
            return String.format(Locale.getDefault(), "%02d secs", mDuration);
        }

        if (mDuration < 3600) {
            return String.format(Locale.getDefault(), "%02d mins %02d secs", (mDuration % 3600) / 60, (mDuration % 60));
        }

        return String.format(Locale.getDefault(), "%d h %02d mins %02d secs", mDuration / 3600, (mDuration % 3600) / 60, (mDuration % 60));
    }

    public long getTimestampEnd() {
        return timestampEnd;
    }

    public void setTimestampEnd(long timestampEnd) {
        this.timestampEnd = timestampEnd;
        if (timestampEnd != 0 && !isMissed())
            setDuration(timestampEnd - mTimestamp);
    }

    public boolean isMissed() {
        return missed;
    }

    public boolean isAudioOnly() {
        return isAudioOnly;
    }


    public void muteVideo(boolean mute) {
        isVideoMuted = mute;
    }

    public String getVideoCodec() {
        return mVideoCodec;
    }

    public String getAudioCodec() {
        return mAudioCodec;
    }

    public String getConfId() {
        return mConfId;
    }

    public void setConfId(String confId) {
        mConfId = confId;
    }

    public void setCallState(CallStatus callStatus) {
        mCallStatus = callStatus;
        if (callStatus == CallStatus.CURRENT) {
            missed = false;
            mStatus = InteractionStatus.SUCCESS.toString();
        } else if (isRinging() || isOnGoing()) {
            mStatus = InteractionStatus.SUCCESS.toString();
        } else if (mCallStatus == CallStatus.FAILURE) {
            mStatus = InteractionStatus.FAILURE.toString();
        }
    }

    public CallStatus getCallStatus() {
        return mCallStatus;
    }

    public void setTimestamp(long timestamp) {
        mTimestamp = timestamp;
    }

    public boolean isRinging() {
        return mCallStatus == CallStatus.CONNECTING || mCallStatus == CallStatus.RINGING || mCallStatus == CallStatus.NONE || mCallStatus == CallStatus.SEARCHING;
    }

    public boolean isOnGoing() {
        return mCallStatus == CallStatus.CURRENT || mCallStatus == CallStatus.HOLD || mCallStatus == CallStatus.UNHOLD;
    }

    public void setIsIncoming(Direction direction) {
        mIsIncoming = (direction == Direction.INCOMING);
    }

    public VCard appendToVCard(Map<String, String> messages) {
        for (Map.Entry<String, String> message : messages.entrySet()) {
            HashMap<String, String> messageKeyValue = VCardUtils.parseMimeAttributes(message.getKey());
            String mimeType = messageKeyValue.get(VCardUtils.VCARD_KEY_MIME_TYPE);
            if (!VCardUtils.MIME_RING_PROFILE_VCARD.equals(mimeType)) {
                continue;
            }
            int part = Integer.parseInt(messageKeyValue.get(VCardUtils.VCARD_KEY_PART));
            int nbPart = Integer.parseInt(messageKeyValue.get(VCardUtils.VCARD_KEY_OF));
            if (null == mProfileChunk) {
                mProfileChunk = new ProfileChunk(nbPart);
            }
            mProfileChunk.addPartAtIndex(message.getValue(), part);
            if (mProfileChunk.isProfileComplete()) {
                VCard ret = Ezvcard.parse(mProfileChunk.getCompleteProfile()).first();
                mProfileChunk = null;
                return ret;
            }
        }
        return null;
    }

    public enum CallStatus {
        NONE,
        SEARCHING,
        CONNECTING,
        RINGING,
        CURRENT,
        HUNGUP,
        BUSY,
        FAILURE,
        HOLD,
        UNHOLD,
        INACTIVE,
        OVER;

        public static CallStatus fromString(String state) {
            switch (state) {
                case "SEARCHING":
                    return SEARCHING;
                case "CONNECTING":
                    return CONNECTING;
                case "INCOMING":
                case "RINGING":
                    return RINGING;
                case "CURRENT":
                    return CURRENT;
                case "HUNGUP":
                    return HUNGUP;
                case "BUSY":
                    return BUSY;
                case "FAILURE":
                    return FAILURE;
                case "HOLD":
                    return HOLD;
                case "UNHOLD":
                    return UNHOLD;
                case "INACTIVE":
                    return INACTIVE;
                case "OVER":
                    return OVER;
                case "NONE":
                default:
                    return NONE;
            }
        }
    }

    public enum  Direction {
        INCOMING(0),
        OUTGOING(1);

        private final int value;
        Direction(int v) {
            value = v;
        }
        int getValue() {
            return value;
        }
        static Direction fromInt(int value) {
            return value == INCOMING.value ? INCOMING : OUTGOING;
        }
    }
}
