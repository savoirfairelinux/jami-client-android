package net.jami.model;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000x\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0010\u000b\n\u0002\b\n\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\"\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010#\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010!\n\u0002\b\u0007\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\f\u0018\u00002\u00020\u0001:\u0001UB\u000f\b\u0016\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004B\u000f\b\u0016\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\u0002\u0010\u0007B\u000f\b\u0016\u0012\u0006\u0010\b\u001a\u00020\u0000\u00a2\u0006\u0002\u0010\tJ\u000e\u0010C\u001a\u00020D2\u0006\u0010E\u001a\u00020\u0003J\u0013\u0010F\u001a\u00020\u00162\b\u0010G\u001a\u0004\u0018\u00010\u0006H\u0086\u0002J\u0010\u0010H\u001a\u0004\u0018\u00010\u00032\u0006\u0010I\u001a\u00020JJ\u0012\u0010K\u001a\u0004\u0018\u00010\u00032\b\u0010G\u001a\u0004\u0018\u00010\u0006J\u0006\u0010L\u001a\u00020\u0016J\u000e\u0010M\u001a\u00020\u00162\u0006\u0010N\u001a\u00020\u0003J\u0006\u0010O\u001a\u00020DJ\u0014\u0010P\u001a\u00020D2\f\u0010Q\u001a\b\u0012\u0004\u0012\u00020#0\"J\u0016\u0010R\u001a\u00020D2\u0006\u0010S\u001a\u00020&2\u0006\u0010=\u001a\u00020\u0016J\u0010\u0010T\u001a\u00020D2\b\u0010=\u001a\u0004\u0018\u00010\u0006R\u0013\u0010\u0002\u001a\u0004\u0018\u00010\u00038F\u00a2\u0006\u0006\u001a\u0004\b\n\u0010\u000bR\u0013\u0010\f\u001a\u0004\u0018\u00010\r8F\u00a2\u0006\u0006\u001a\u0004\b\u000e\u0010\u000fR\u0013\u0010\u0010\u001a\u0004\u0018\u00010\u00038F\u00a2\u0006\u0006\u001a\u0004\b\u0011\u0010\u000bR\u0011\u0010\u0012\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014R\u0011\u0010\u0015\u001a\u00020\u00168F\u00a2\u0006\u0006\u001a\u0004\b\u0015\u0010\u0017R\u0011\u0010\u0018\u001a\u00020\u00168F\u00a2\u0006\u0006\u001a\u0004\b\u0018\u0010\u0017R\u001a\u0010\u0019\u001a\u00020\u0016X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0019\u0010\u0017\"\u0004\b\u001a\u0010\u001bR\u0011\u0010\u001c\u001a\u00020\u00168F\u00a2\u0006\u0006\u001a\u0004\b\u001c\u0010\u0017R\u0011\u0010\u001d\u001a\u00020\u00168F\u00a2\u0006\u0006\u001a\u0004\b\u001d\u0010\u0017R\u0011\u0010\u001e\u001a\u00020\u00168F\u00a2\u0006\u0006\u001a\u0004\b\u001e\u0010\u0017R\u0010\u0010\u001f\u001a\u0004\u0018\u00010\rX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010 \u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020#0\"0!X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010$\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020&0%0!X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\'\u001a\b\u0012\u0004\u0012\u00020&0(X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010)\u001a\b\u0012\u0004\u0012\u00020\u00030*X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010+\u001a\u00020\u0016X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001c\u0010,\u001a\u0004\u0018\u00010&X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b-\u0010.\"\u0004\b/\u00100R\u001d\u00101\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020#0\"028F\u00a2\u0006\u0006\u001a\u0004\b3\u00104R\u001d\u00105\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020&0%028F\u00a2\u0006\u0006\u001a\u0004\b6\u00104R\u0017\u00107\u001a\b\u0012\u0004\u0012\u00020\u0003088F\u00a2\u0006\u0006\u001a\u0004\b9\u0010:R\u0011\u0010;\u001a\u00020\u00068F\u00a2\u0006\u0006\u001a\u0004\b<\u0010\u0014R\u0013\u0010=\u001a\u0004\u0018\u00010\r8F\u00a2\u0006\u0006\u001a\u0004\b>\u0010\u000fR\u0011\u0010?\u001a\u00020@8F\u00a2\u0006\u0006\u001a\u0004\bA\u0010B\u00a8\u0006V"}, d2 = {"Lnet/jami/model/Conference;", "", "call", "Lnet/jami/model/Call;", "(Lnet/jami/model/Call;)V", "cID", "", "(Ljava/lang/String;)V", "c", "(Lnet/jami/model/Conference;)V", "getCall", "()Lnet/jami/model/Call;", "confState", "Lnet/jami/model/Call$CallStatus;", "getConfState", "()Lnet/jami/model/Call$CallStatus;", "firstCall", "getFirstCall", "id", "getId", "()Ljava/lang/String;", "isConference", "", "()Z", "isIncoming", "isModerator", "setModerator", "(Z)V", "isOnGoing", "isRinging", "isSimpleCall", "mConfState", "mParticipantInfo", "Lio/reactivex/rxjava3/subjects/Subject;", "", "Lnet/jami/model/Conference$ParticipantInfo;", "mParticipantRecording", "", "Lnet/jami/model/Contact;", "mParticipantRecordingSet", "", "mParticipants", "Ljava/util/ArrayList;", "mRecording", "maximizedParticipant", "getMaximizedParticipant", "()Lnet/jami/model/Contact;", "setMaximizedParticipant", "(Lnet/jami/model/Contact;)V", "participantInfo", "Lio/reactivex/rxjava3/core/Observable;", "getParticipantInfo", "()Lio/reactivex/rxjava3/core/Observable;", "participantRecording", "getParticipantRecording", "participants", "", "getParticipants", "()Ljava/util/List;", "pluginId", "getPluginId", "state", "getState", "timestampStart", "", "getTimestampStart", "()J", "addParticipant", "", "part", "contains", "callID", "findCallByContact", "uri", "Lnet/jami/model/Uri;", "getCallById", "hasVideo", "removeParticipant", "toRemove", "removeParticipants", "setInfo", "info", "setParticipantRecording", "contact", "setState", "ParticipantInfo", "libringclient"})
public final class Conference {
    private final io.reactivex.rxjava3.subjects.Subject<java.util.List<net.jami.model.Conference.ParticipantInfo>> mParticipantInfo = null;
    private final java.util.Set<net.jami.model.Contact> mParticipantRecordingSet = null;
    private final io.reactivex.rxjava3.subjects.Subject<java.util.Set<net.jami.model.Contact>> mParticipantRecording = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String id = null;
    private net.jami.model.Call.CallStatus mConfState;
    private final java.util.ArrayList<net.jami.model.Call> mParticipants = null;
    private boolean mRecording;
    @org.jetbrains.annotations.Nullable()
    private net.jami.model.Contact maximizedParticipant;
    private boolean isModerator = false;
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getId() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.Contact getMaximizedParticipant() {
        return null;
    }
    
    public final void setMaximizedParticipant(@org.jetbrains.annotations.Nullable()
    net.jami.model.Contact p0) {
    }
    
    public final boolean isModerator() {
        return false;
    }
    
    public final void setModerator(boolean p0) {
    }
    
    public Conference(@org.jetbrains.annotations.NotNull()
    net.jami.model.Call call) {
        super();
    }
    
    public Conference(@org.jetbrains.annotations.NotNull()
    java.lang.String cID) {
        super();
    }
    
    public Conference(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conference c) {
        super();
    }
    
    public final boolean isRinging() {
        return false;
    }
    
    public final boolean isConference() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.Call getCall() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.Call getFirstCall() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getPluginId() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.Call.CallStatus getState() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.Call.CallStatus getConfState() {
        return null;
    }
    
    public final boolean isSimpleCall() {
        return false;
    }
    
    public final void setState(@org.jetbrains.annotations.Nullable()
    java.lang.String state) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<net.jami.model.Call> getParticipants() {
        return null;
    }
    
    public final void addParticipant(@org.jetbrains.annotations.NotNull()
    net.jami.model.Call part) {
    }
    
    public final boolean removeParticipant(@org.jetbrains.annotations.NotNull()
    net.jami.model.Call toRemove) {
        return false;
    }
    
    public final boolean contains(@org.jetbrains.annotations.Nullable()
    java.lang.String callID) {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.Call getCallById(@org.jetbrains.annotations.Nullable()
    java.lang.String callID) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.Call findCallByContact(@org.jetbrains.annotations.NotNull()
    net.jami.model.Uri uri) {
        return null;
    }
    
    public final boolean isIncoming() {
        return false;
    }
    
    public final boolean isOnGoing() {
        return false;
    }
    
    public final boolean hasVideo() {
        return false;
    }
    
    public final long getTimestampStart() {
        return 0L;
    }
    
    public final void removeParticipants() {
    }
    
    public final void setInfo(@org.jetbrains.annotations.NotNull()
    java.util.List<net.jami.model.Conference.ParticipantInfo> info) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.util.List<net.jami.model.Conference.ParticipantInfo>> getParticipantInfo() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Observable<java.util.Set<net.jami.model.Contact>> getParticipantRecording() {
        return null;
    }
    
    public final void setParticipantRecording(@org.jetbrains.annotations.NotNull()
    net.jami.model.Contact contact, boolean state) {
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\t\n\u0002\u0010\b\n\u0002\b\u0014\u0018\u00002\u00020\u0001B+\u0012\b\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\b0\u0007\u00a2\u0006\u0002\u0010\tR\u001a\u0010\n\u001a\u00020\u000bX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\f\u0010\r\"\u0004\b\u000e\u0010\u000fR\u0013\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u001a\u0010\u0014\u001a\u00020\u0015X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0016\u0010\u0017\"\u0004\b\u0018\u0010\u0019R\u0011\u0010\u001a\u001a\u00020\u000b8F\u00a2\u0006\u0006\u001a\u0004\b\u001a\u0010\rR\u001a\u0010\u001b\u001a\u00020\u000bX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u001b\u0010\r\"\u0004\b\u001c\u0010\u000fR\u001a\u0010\u001d\u001a\u00020\u000bX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u001e\u0010\r\"\u0004\b\u001f\u0010\u000fR\u001a\u0010 \u001a\u00020\u0015X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b!\u0010\u0017\"\u0004\b\"\u0010\u0019R\u001a\u0010#\u001a\u00020\u0015X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b$\u0010\u0017\"\u0004\b%\u0010\u0019R\u001a\u0010&\u001a\u00020\u0015X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\'\u0010\u0017\"\u0004\b(\u0010\u0019\u00a8\u0006)"}, d2 = {"Lnet/jami/model/Conference$ParticipantInfo;", "", "call", "Lnet/jami/model/Call;", "contact", "Lnet/jami/model/Contact;", "i", "", "", "(Lnet/jami/model/Call;Lnet/jami/model/Contact;Ljava/util/Map;)V", "audioMuted", "", "getAudioMuted", "()Z", "setAudioMuted", "(Z)V", "getCall", "()Lnet/jami/model/Call;", "getContact", "()Lnet/jami/model/Contact;", "h", "", "getH", "()I", "setH", "(I)V", "isEmpty", "isModerator", "setModerator", "videoMuted", "getVideoMuted", "setVideoMuted", "w", "getW", "setW", "x", "getX", "setX", "y", "getY", "setY", "libringclient"})
    public static final class ParticipantInfo {
        @org.jetbrains.annotations.Nullable()
        private final net.jami.model.Call call = null;
        @org.jetbrains.annotations.NotNull()
        private final net.jami.model.Contact contact = null;
        private int x;
        private int y;
        private int w;
        private int h;
        private boolean videoMuted;
        private boolean audioMuted;
        private boolean isModerator;
        
        public ParticipantInfo(@org.jetbrains.annotations.Nullable()
        net.jami.model.Call call, @org.jetbrains.annotations.NotNull()
        net.jami.model.Contact contact, @org.jetbrains.annotations.NotNull()
        java.util.Map<java.lang.String, java.lang.String> i) {
            super();
        }
        
        @org.jetbrains.annotations.Nullable()
        public final net.jami.model.Call getCall() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final net.jami.model.Contact getContact() {
            return null;
        }
        
        public final int getX() {
            return 0;
        }
        
        public final void setX(int p0) {
        }
        
        public final int getY() {
            return 0;
        }
        
        public final void setY(int p0) {
        }
        
        public final int getW() {
            return 0;
        }
        
        public final void setW(int p0) {
        }
        
        public final int getH() {
            return 0;
        }
        
        public final void setH(int p0) {
        }
        
        public final boolean getVideoMuted() {
            return false;
        }
        
        public final void setVideoMuted(boolean p0) {
        }
        
        public final boolean getAudioMuted() {
            return false;
        }
        
        public final void setAudioMuted(boolean p0) {
        }
        
        public final boolean isModerator() {
            return false;
        }
        
        public final void setModerator(boolean p0) {
        }
        
        public final boolean isEmpty() {
            return false;
        }
    }
}