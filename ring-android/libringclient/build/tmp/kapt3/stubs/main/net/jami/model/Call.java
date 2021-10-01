package net.jami.model;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000^\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0010$\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0013\n\u0002\u0010\u000b\n\u0002\b\u000b\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\t\u0018\u0000 M2\u00020\u0001:\u0003LMNBA\b\u0016\u0012\b\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0004\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0006\u001a\u0004\u0018\u00010\u0007\u0012\b\u0010\b\u001a\u0004\u0018\u00010\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u00a2\u0006\u0002\u0010\fB\u000f\b\u0016\u0012\u0006\u0010\r\u001a\u00020\u0001\u00a2\u0006\u0002\u0010\u000eB5\b\u0016\u0012\b\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u000f\u001a\u0004\u0018\u00010\u0003\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\u0006\u0010\u0010\u001a\u00020\u0011\u00a2\u0006\u0002\u0010\u0012B\'\b\u0016\u0012\b\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u0012\u0014\u0010\u0013\u001a\u0010\u0012\u0006\u0012\u0004\u0018\u00010\u0003\u0012\u0004\u0012\u00020\u00030\u0014\u00a2\u0006\u0002\u0010\u0015J\u001c\u0010B\u001a\u0004\u0018\u00010C2\u0012\u0010D\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00030\u0014J\u000e\u0010E\u001a\u00020F2\u0006\u0010G\u001a\u00020.J\u000e\u0010H\u001a\u00020F2\u0006\u0010G\u001a\u00020.J\u000e\u0010I\u001a\u00020F2\u0006\u0010\u001b\u001a\u00020\u001aJ\u001c\u0010J\u001a\u00020F2\u0014\u0010K\u001a\u0010\u0012\u0006\u0012\u0004\u0018\u00010\u0003\u0012\u0004\u0012\u00020\u00030\u0014R\"\u0010\u0017\u001a\u0004\u0018\u00010\u00032\b\u0010\u0016\u001a\u0004\u0018\u00010\u0003@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019R\u001e\u0010\u001b\u001a\u00020\u001a2\u0006\u0010\u0016\u001a\u00020\u001a@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u001dR\u001c\u0010\u001e\u001a\u0004\u0018\u00010\u0003X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u001f\u0010\u0019\"\u0004\b \u0010!R\"\u0010\u000f\u001a\u0004\u0018\u00010\u00032\b\u0010\u0016\u001a\u0004\u0018\u00010\u0003@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010\u0019R\u0016\u0010#\u001a\u0004\u0018\u00010\u0003X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010\u0019R,\u0010&\u001a\u0004\u0018\u00010\u00112\b\u0010%\u001a\u0004\u0018\u00010\u00118F@FX\u0086\u000e\u00a2\u0006\u0010\n\u0002\u0010+\u001a\u0004\b\'\u0010(\"\u0004\b)\u0010*R\u0011\u0010,\u001a\u00020\u00038F\u00a2\u0006\u0006\u001a\u0004\b-\u0010\u0019R\u001e\u0010/\u001a\u00020.2\u0006\u0010\u0016\u001a\u00020.@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b/\u00100R\u001e\u00101\u001a\u00020.2\u0006\u0010\u0016\u001a\u00020.@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b1\u00100R\u0011\u00102\u001a\u00020.8F\u00a2\u0006\u0006\u001a\u0004\b2\u00100R\u001e\u00103\u001a\u00020.2\u0006\u0010\u0016\u001a\u00020.@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b3\u00100R\u0011\u00104\u001a\u00020.8F\u00a2\u0006\u0006\u001a\u0004\b4\u00100R\u000e\u00105\u001a\u00020.X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u00106\u001a\u00020.X\u0082D\u00a2\u0006\u0002\n\u0000R\u0011\u00107\u001a\u00020.8F\u00a2\u0006\u0006\u001a\u0004\b7\u00100R\u000e\u00108\u001a\u00020.X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u00109\u001a\u0004\u0018\u00010:X\u0082\u000e\u00a2\u0006\u0002\n\u0000R$\u0010;\u001a\u00020\u00112\u0006\u0010;\u001a\u00020\u0011@FX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b<\u0010=\"\u0004\b>\u0010?R\"\u0010@\u001a\u0004\u0018\u00010\u00032\b\u0010\u0016\u001a\u0004\u0018\u00010\u0003@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\bA\u0010\u0019\u00a8\u0006O"}, d2 = {"Lnet/jami/model/Call;", "Lnet/jami/model/Interaction;", "daemonId", "", "author", "account", "conversation", "Lnet/jami/model/ConversationHistory;", "contact", "Lnet/jami/model/Contact;", "direction", "Lnet/jami/model/Call$Direction;", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lnet/jami/model/ConversationHistory;Lnet/jami/model/Contact;Lnet/jami/model/Call$Direction;)V", "interaction", "(Lnet/jami/model/Interaction;)V", "contactNumber", "timestamp", "", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lnet/jami/model/Call$Direction;J)V", "call_details", "", "(Ljava/lang/String;Ljava/util/Map;)V", "<set-?>", "audioCodec", "getAudioCodec", "()Ljava/lang/String;", "Lnet/jami/model/Call$CallStatus;", "callStatus", "getCallStatus", "()Lnet/jami/model/Call$CallStatus;", "confId", "getConfId", "setConfId", "(Ljava/lang/String;)V", "getContactNumber", "daemonIdString", "getDaemonIdString", "value", "duration", "getDuration", "()Ljava/lang/Long;", "setDuration", "(Ljava/lang/Long;)V", "Ljava/lang/Long;", "durationString", "getDurationString", "", "isAudioMuted", "()Z", "isAudioOnly", "isConferenceParticipant", "isMissed", "isOnGoing", "isPeerHolding", "isRecording", "isRinging", "isVideoMuted", "mProfileChunk", "Lnet/jami/utils/ProfileChunk;", "timestampEnd", "getTimestampEnd", "()J", "setTimestampEnd", "(J)V", "videoCodec", "getVideoCodec", "appendToVCard", "Lezvcard/VCard;", "messages", "muteAudio", "", "mute", "muteVideo", "setCallState", "setDetails", "details", "CallStatus", "Companion", "Direction", "libringclient"})
public final class Call extends net.jami.model.Interaction {
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String daemonIdString = null;
    private boolean isPeerHolding = false;
    private boolean isAudioMuted = false;
    private boolean isVideoMuted = false;
    private final boolean isRecording = false;
    private boolean isAudioOnly = false;
    @org.jetbrains.annotations.NotNull()
    private net.jami.model.Call.CallStatus callStatus = net.jami.model.Call.CallStatus.NONE;
    private long timestampEnd = 0L;
    @org.jetbrains.annotations.Nullable()
    private java.lang.Long duration;
    private boolean isMissed = true;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String audioCodec;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String videoCodec;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String contactNumber;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String confId;
    private net.jami.utils.ProfileChunk mProfileChunk;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.model.Call.Companion Companion = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = null;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String KEY_ACCOUNT_ID = "ACCOUNTID";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String KEY_AUDIO_ONLY = "AUDIO_ONLY";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String KEY_CALL_TYPE = "CALL_TYPE";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String KEY_CALL_STATE = "CALL_STATE";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String KEY_PEER_NUMBER = "PEER_NUMBER";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String KEY_PEER_HOLDING = "PEER_HOLDING";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String KEY_AUDIO_MUTED = "PEER_NUMBER";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String KEY_VIDEO_MUTED = "VIDEO_MUTED";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String KEY_AUDIO_CODEC = "AUDIO_CODEC";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String KEY_VIDEO_CODEC = "VIDEO_CODEC";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String KEY_REGISTERED_NAME = "REGISTERED_NAME";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String KEY_DURATION = "duration";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String KEY_CONF_ID = "CONF_ID";
    
    @org.jetbrains.annotations.Nullable()
    @java.lang.Override()
    public java.lang.String getDaemonIdString() {
        return null;
    }
    
    public final boolean isAudioMuted() {
        return false;
    }
    
    public final boolean isAudioOnly() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.model.Call.CallStatus getCallStatus() {
        return null;
    }
    
    public final long getTimestampEnd() {
        return 0L;
    }
    
    public final void setTimestampEnd(long timestampEnd) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long getDuration() {
        return null;
    }
    
    public final void setDuration(@org.jetbrains.annotations.Nullable()
    java.lang.Long value) {
    }
    
    public final boolean isMissed() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getAudioCodec() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getVideoCodec() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getContactNumber() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getConfId() {
        return null;
    }
    
    public final void setConfId(@org.jetbrains.annotations.Nullable()
    java.lang.String p0) {
    }
    
    public Call(@org.jetbrains.annotations.Nullable()
    java.lang.String daemonId, @org.jetbrains.annotations.Nullable()
    java.lang.String author, @org.jetbrains.annotations.Nullable()
    java.lang.String account, @org.jetbrains.annotations.Nullable()
    net.jami.model.ConversationHistory conversation, @org.jetbrains.annotations.Nullable()
    net.jami.model.Contact contact, @org.jetbrains.annotations.NotNull()
    net.jami.model.Call.Direction direction) {
        super();
    }
    
    public Call(@org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction interaction) {
        super();
    }
    
    public Call(@org.jetbrains.annotations.Nullable()
    java.lang.String daemonId, @org.jetbrains.annotations.Nullable()
    java.lang.String account, @org.jetbrains.annotations.Nullable()
    java.lang.String contactNumber, @org.jetbrains.annotations.NotNull()
    net.jami.model.Call.Direction direction, long timestamp) {
        super();
    }
    
    public Call(@org.jetbrains.annotations.Nullable()
    java.lang.String daemonId, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> call_details) {
        super();
    }
    
    public final void setDetails(@org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> details) {
    }
    
    public final boolean isConferenceParticipant() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getDurationString() {
        return null;
    }
    
    public final void muteVideo(boolean mute) {
    }
    
    public final void muteAudio(boolean mute) {
    }
    
    public final void setCallState(@org.jetbrains.annotations.NotNull()
    net.jami.model.Call.CallStatus callStatus) {
    }
    
    public final boolean isRinging() {
        return false;
    }
    
    public final boolean isOnGoing() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final ezvcard.VCard appendToVCard(@org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> messages) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u000f\b\u0086\u0001\u0018\u0000 \u000f2\b\u0012\u0004\u0012\u00020\u00000\u0001:\u0001\u000fB\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006j\u0002\b\u0007j\u0002\b\bj\u0002\b\tj\u0002\b\nj\u0002\b\u000bj\u0002\b\fj\u0002\b\rj\u0002\b\u000e\u00a8\u0006\u0010"}, d2 = {"Lnet/jami/model/Call$CallStatus;", "", "(Ljava/lang/String;I)V", "NONE", "SEARCHING", "CONNECTING", "RINGING", "CURRENT", "HUNGUP", "BUSY", "FAILURE", "HOLD", "UNHOLD", "INACTIVE", "OVER", "Companion", "libringclient"})
    public static enum CallStatus {
        /*public static final*/ NONE /* = new NONE() */,
        /*public static final*/ SEARCHING /* = new SEARCHING() */,
        /*public static final*/ CONNECTING /* = new CONNECTING() */,
        /*public static final*/ RINGING /* = new RINGING() */,
        /*public static final*/ CURRENT /* = new CURRENT() */,
        /*public static final*/ HUNGUP /* = new HUNGUP() */,
        /*public static final*/ BUSY /* = new BUSY() */,
        /*public static final*/ FAILURE /* = new FAILURE() */,
        /*public static final*/ HOLD /* = new HOLD() */,
        /*public static final*/ UNHOLD /* = new UNHOLD() */,
        /*public static final*/ INACTIVE /* = new INACTIVE() */,
        /*public static final*/ OVER /* = new OVER() */;
        @org.jetbrains.annotations.NotNull()
        public static final net.jami.model.Call.CallStatus.Companion Companion = null;
        
        CallStatus() {
        }
        
        @org.jetbrains.annotations.NotNull()
        @kotlin.jvm.JvmStatic()
        public static final net.jami.model.Call.CallStatus fromString(@org.jetbrains.annotations.Nullable()
        java.lang.String state) {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        @kotlin.jvm.JvmStatic()
        public static final net.jami.model.Call.CallStatus fromConferenceString(@org.jetbrains.annotations.Nullable()
        java.lang.String state) {
            return null;
        }
        
        @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0003\u001a\u00020\u00042\b\u0010\u0005\u001a\u0004\u0018\u00010\u0006H\u0007J\u0012\u0010\u0007\u001a\u00020\u00042\b\u0010\u0005\u001a\u0004\u0018\u00010\u0006H\u0007\u00a8\u0006\b"}, d2 = {"Lnet/jami/model/Call$CallStatus$Companion;", "", "()V", "fromConferenceString", "Lnet/jami/model/Call$CallStatus;", "state", "", "fromString", "libringclient"})
        public static final class Companion {
            
            private Companion() {
                super();
            }
            
            @org.jetbrains.annotations.NotNull()
            @kotlin.jvm.JvmStatic()
            public final net.jami.model.Call.CallStatus fromString(@org.jetbrains.annotations.Nullable()
            java.lang.String state) {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            @kotlin.jvm.JvmStatic()
            public final net.jami.model.Call.CallStatus fromConferenceString(@org.jetbrains.annotations.Nullable()
            java.lang.String state) {
                return null;
            }
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\b\n\u0002\b\u0007\b\u0086\u0001\u0018\u0000 \t2\b\u0012\u0004\u0012\u00020\u00000\u0001:\u0001\tB\u000f\b\u0002\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006j\u0002\b\u0007j\u0002\b\b\u00a8\u0006\n"}, d2 = {"Lnet/jami/model/Call$Direction;", "", "value", "", "(Ljava/lang/String;II)V", "getValue", "()I", "INCOMING", "OUTGOING", "Companion", "libringclient"})
    public static enum Direction {
        /*public static final*/ INCOMING /* = new INCOMING(0) */,
        /*public static final*/ OUTGOING /* = new OUTGOING(0) */;
        private final int value = 0;
        @org.jetbrains.annotations.NotNull()
        public static final net.jami.model.Call.Direction.Companion Companion = null;
        
        Direction(int value) {
        }
        
        public final int getValue() {
            return 0;
        }
        
        @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006\u00a8\u0006\u0007"}, d2 = {"Lnet/jami/model/Call$Direction$Companion;", "", "()V", "fromInt", "Lnet/jami/model/Call$Direction;", "value", "", "libringclient"})
        public static final class Companion {
            
            private Companion() {
                super();
            }
            
            @org.jetbrains.annotations.NotNull()
            public final net.jami.model.Call.Direction fromInt(int value) {
                return null;
            }
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0010\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u0011\u0010\u0011\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013\u00a8\u0006\u0014"}, d2 = {"Lnet/jami/model/Call$Companion;", "", "()V", "KEY_ACCOUNT_ID", "", "KEY_AUDIO_CODEC", "KEY_AUDIO_MUTED", "KEY_AUDIO_ONLY", "KEY_CALL_STATE", "KEY_CALL_TYPE", "KEY_CONF_ID", "KEY_DURATION", "KEY_PEER_HOLDING", "KEY_PEER_NUMBER", "KEY_REGISTERED_NAME", "KEY_VIDEO_CODEC", "KEY_VIDEO_MUTED", "TAG", "getTAG", "()Ljava/lang/String;", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getTAG() {
            return null;
        }
    }
}