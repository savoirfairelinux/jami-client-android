package net.jami.model;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000b\n\u0002\b\b\u0018\u00002\u00020\u0001B5\b\u0016\u0012\b\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\b\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0006\u001a\u0004\u0018\u00010\u0007\u0012\u0006\u0010\b\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\tB;\b\u0016\u0012\b\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\b\u0010\u0006\u001a\u0004\u0018\u00010\u0007\u0012\u0006\u0010\b\u001a\u00020\u0003\u0012\u0006\u0010\f\u001a\u00020\r\u00a2\u0006\u0002\u0010\u000eB\u000f\b\u0016\u0012\u0006\u0010\u000f\u001a\u00020\u0001\u00a2\u0006\u0002\u0010\u0010R\u001a\u0010\u0011\u001a\u00020\rX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0011\u0010\u0012\"\u0004\b\u0013\u0010\u0014\u00a8\u0006\u0015"}, d2 = {"Lnet/jami/model/TextMessage;", "Lnet/jami/model/Interaction;", "author", "", "account", "daemonId", "conversation", "Lnet/jami/model/ConversationHistory;", "message", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lnet/jami/model/ConversationHistory;Ljava/lang/String;)V", "timestamp", "", "isIncoming", "", "(Ljava/lang/String;Ljava/lang/String;JLnet/jami/model/ConversationHistory;Ljava/lang/String;Z)V", "interaction", "(Lnet/jami/model/Interaction;)V", "isNotified", "()Z", "setNotified", "(Z)V", "libringclient"})
public final class TextMessage extends net.jami.model.Interaction {
    private boolean isNotified = false;
    
    public final boolean isNotified() {
        return false;
    }
    
    public final void setNotified(boolean p0) {
    }
    
    public TextMessage(@org.jetbrains.annotations.Nullable()
    java.lang.String author, @org.jetbrains.annotations.NotNull()
    java.lang.String account, @org.jetbrains.annotations.Nullable()
    java.lang.String daemonId, @org.jetbrains.annotations.Nullable()
    net.jami.model.ConversationHistory conversation, @org.jetbrains.annotations.NotNull()
    java.lang.String message) {
        super();
    }
    
    public TextMessage(@org.jetbrains.annotations.Nullable()
    java.lang.String author, @org.jetbrains.annotations.NotNull()
    java.lang.String account, long timestamp, @org.jetbrains.annotations.Nullable()
    net.jami.model.ConversationHistory conversation, @org.jetbrains.annotations.NotNull()
    java.lang.String message, boolean isIncoming) {
        super();
    }
    
    public TextMessage(@org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction interaction) {
        super();
    }
}