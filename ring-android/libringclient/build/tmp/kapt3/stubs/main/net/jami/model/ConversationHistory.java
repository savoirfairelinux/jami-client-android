package net.jami.model;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u000f\b\u0017\u0018\u0000 \u00172\u00020\u0001:\u0001\u0017B\u0007\b\u0016\u00a2\u0006\u0002\u0010\u0002B\u000f\b\u0016\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\u0002\u0010\u0005B\u0017\b\u0016\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u00a2\u0006\u0002\u0010\nB\u000f\b\u0016\u0012\u0006\u0010\b\u001a\u00020\t\u00a2\u0006\u0002\u0010\u000bR\"\u0010\u0006\u001a\u0004\u0018\u00010\u00078\u0006@\u0006X\u0087\u000e\u00a2\u0006\u0010\n\u0002\u0010\u0010\u001a\u0004\b\f\u0010\r\"\u0004\b\u000e\u0010\u000fR \u0010\u0011\u001a\u0004\u0018\u00010\t8\u0006@\u0006X\u0087\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0012\u0010\u0013\"\u0004\b\u0014\u0010\u000bR \u0010\b\u001a\u0004\u0018\u00010\t8\u0006@\u0006X\u0087\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0015\u0010\u0013\"\u0004\b\u0016\u0010\u000b\u00a8\u0006\u0018"}, d2 = {"Lnet/jami/model/ConversationHistory;", "", "()V", "conversation", "Lnet/jami/model/Conversation;", "(Lnet/jami/model/Conversation;)V", "id", "", "participant", "", "(ILjava/lang/String;)V", "(Ljava/lang/String;)V", "getId", "()Ljava/lang/Integer;", "setId", "(Ljava/lang/Integer;)V", "Ljava/lang/Integer;", "mExtraData", "getMExtraData", "()Ljava/lang/String;", "setMExtraData", "getParticipant", "setParticipant", "Companion", "libringclient"})
@com.j256.ormlite.table.DatabaseTable(tableName = "conversations")
public class ConversationHistory {
    @org.jetbrains.annotations.Nullable()
    @com.j256.ormlite.field.DatabaseField(generatedId = true, columnName = "id", canBeNull = false)
    private java.lang.Integer id;
    @org.jetbrains.annotations.Nullable()
    @com.j256.ormlite.field.DatabaseField(columnName = "participant", index = true)
    private java.lang.String participant;
    @org.jetbrains.annotations.Nullable()
    @com.j256.ormlite.field.DatabaseField(columnName = "extra_data")
    private java.lang.String mExtraData;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.model.ConversationHistory.Companion Companion = null;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String TABLE_NAME = "conversations";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String COLUMN_CONVERSATION_ID = "id";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String COLUMN_PARTICIPANT = "participant";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String COLUMN_EXTRA_DATA = "extra_data";
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer getId() {
        return null;
    }
    
    public final void setId(@org.jetbrains.annotations.Nullable()
    java.lang.Integer p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getParticipant() {
        return null;
    }
    
    public final void setParticipant(@org.jetbrains.annotations.Nullable()
    java.lang.String p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getMExtraData() {
        return null;
    }
    
    public final void setMExtraData(@org.jetbrains.annotations.Nullable()
    java.lang.String p0) {
    }
    
    public ConversationHistory() {
        super();
    }
    
    public ConversationHistory(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation conversation) {
        super();
    }
    
    public ConversationHistory(int id, @org.jetbrains.annotations.NotNull()
    java.lang.String participant) {
        super();
    }
    
    public ConversationHistory(@org.jetbrains.annotations.NotNull()
    java.lang.String participant) {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0004\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\b"}, d2 = {"Lnet/jami/model/ConversationHistory$Companion;", "", "()V", "COLUMN_CONVERSATION_ID", "", "COLUMN_EXTRA_DATA", "COLUMN_PARTICIPANT", "TABLE_NAME", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}