package net.jami.model;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000b\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0010\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0010\t\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0010\u000b\n\u0002\b\u0014\n\u0002\u0018\u0002\n\u0002\b\u000f\n\u0002\u0010\u0002\n\u0002\b\b\b\u0017\u0018\u0000 e2\u00020\u0001:\u0003efgB\u0007\b\u0016\u00a2\u0006\u0002\u0010\u0002B\u000f\b\u0016\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\u0002\u0010\u0005B\u0017\b\u0016\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u00a2\u0006\u0002\u0010\nBa\b\u0016\u0012\u0006\u0010\u000b\u001a\u00020\u0004\u0012\b\u0010\f\u001a\u0004\u0018\u00010\u0004\u0012\b\u0010\u0006\u001a\u0004\u0018\u00010\r\u0012\u0006\u0010\u000e\u001a\u00020\u0004\u0012\b\u0010\u000f\u001a\u0004\u0018\u00010\u0004\u0012\b\u0010\b\u001a\u0004\u0018\u00010\u0004\u0012\u0006\u0010\u0010\u001a\u00020\u0004\u0012\b\u0010\u0011\u001a\u0004\u0018\u00010\u0004\u0012\u0006\u0010\u0012\u001a\u00020\u0004\u0012\u0006\u0010\u0013\u001a\u00020\u0004\u00a2\u0006\u0002\u0010\u0014J\u000e\u0010]\u001a\u00020\u00042\u0006\u0010^\u001a\u000202J\u0006\u0010_\u001a\u00020`J\u000e\u0010a\u001a\u00020`2\u0006\u0010(\u001a\u00020\u0004J \u0010a\u001a\u00020`2\u0006\u0010(\u001a\u00020\u00042\u0006\u0010L\u001a\u00020\u00042\b\u0010b\u001a\u0004\u0018\u00010\u0004J\u0010\u0010c\u001a\u0002022\b\u0010d\u001a\u0004\u0018\u00010\u0004R\u001c\u0010\u0015\u001a\u0004\u0018\u00010\u0004X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0016\u0010\u0017\"\u0004\b\u0018\u0010\u0005R \u0010\f\u001a\u0004\u0018\u00010\u00048\u0006@\u0006X\u0087\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0019\u0010\u0017\"\u0004\b\u001a\u0010\u0005R \u0010\u000f\u001a\u0004\u0018\u00010\u00048\u0006@\u0006X\u0087\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u001b\u0010\u0017\"\u0004\b\u001c\u0010\u0005R\u001c\u0010\u001d\u001a\u0004\u0018\u00010\u001eX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u001f\u0010 \"\u0004\b!\u0010\"R \u0010\u0006\u001a\u0004\u0018\u00010\r8\u0006@\u0006X\u0087\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b#\u0010$\"\u0004\b%\u0010&R\"\u0010(\u001a\u0004\u0018\u00010\u00042\b\u0010\'\u001a\u0004\u0018\u00010\u0004@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b)\u0010\u0017R\"\u0010\u0011\u001a\u0004\u0018\u00010*8\u0006@\u0006X\u0087\u000e\u00a2\u0006\u0010\n\u0002\u0010/\u001a\u0004\b+\u0010,\"\u0004\b-\u0010.R\u0016\u00100\u001a\u0004\u0018\u00010\u00048VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b1\u0010\u0017R\u0011\u0010\u0013\u001a\u0002028F\u00a2\u0006\u0006\u001a\u0004\b3\u00104R\u001e\u0010\u000b\u001a\u0002058\u0006@\u0006X\u0087\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b6\u00107\"\u0004\b8\u00109R\u001a\u0010:\u001a\u00020;X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b:\u0010<\"\u0004\b=\u0010>R\u0011\u0010\u0012\u001a\u00020;8F\u00a2\u0006\u0006\u001a\u0004\b\u0012\u0010<R\u0011\u0010?\u001a\u00020;8F\u00a2\u0006\u0006\u001a\u0004\b?\u0010<R\u001e\u0010@\u001a\u00020\u00048\u0006@\u0006X\u0087\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\bA\u0010\u0017\"\u0004\bB\u0010\u0005R\u001e\u0010C\u001a\u0002058\u0006@\u0006X\u0087\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\bD\u00107\"\u0004\bE\u00109R\u001e\u0010F\u001a\u00020\u00048\u0006@\u0006X\u0087\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\bG\u0010\u0017\"\u0004\bH\u0010\u0005R \u0010I\u001a\u0004\u0018\u00010\u00048\u0006@\u0006X\u0087\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\bJ\u0010\u0017\"\u0004\bK\u0010\u0005R\"\u0010L\u001a\u0004\u0018\u00010\u00042\b\u0010\'\u001a\u0004\u0018\u00010\u0004@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\bM\u0010\u0017R\"\u0010N\u001a\u0004\u0018\u00010\u00042\b\u0010\'\u001a\u0004\u0018\u00010\u0004@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\bO\u0010\u0017R$\u0010\u0010\u001a\u00020P2\u0006\u0010\u0010\u001a\u00020P8F@FX\u0086\u000e\u00a2\u0006\f\u001a\u0004\bQ\u0010R\"\u0004\bS\u0010TR\u001e\u0010\u000e\u001a\u00020*8\u0006@\u0006X\u0087\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\bU\u0010V\"\u0004\bW\u0010XR$\u0010\b\u001a\u00020\t2\u0006\u0010\b\u001a\u00020\t8F@FX\u0086\u000e\u00a2\u0006\f\u001a\u0004\bY\u0010Z\"\u0004\b[\u0010\\\u00a8\u0006h"}, d2 = {"Lnet/jami/model/Interaction;", "", "()V", "accountId", "", "(Ljava/lang/String;)V", "conversation", "Lnet/jami/model/Conversation;", "type", "Lnet/jami/model/Interaction$InteractionType;", "(Lnet/jami/model/Conversation;Lnet/jami/model/Interaction$InteractionType;)V", "id", "author", "Lnet/jami/model/ConversationHistory;", "timestamp", "body", "status", "daemonId", "isRead", "extraFlag", "(Ljava/lang/String;Ljava/lang/String;Lnet/jami/model/ConversationHistory;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", "account", "getAccount", "()Ljava/lang/String;", "setAccount", "getAuthor", "setAuthor", "getBody", "setBody", "contact", "Lnet/jami/model/Contact;", "getContact", "()Lnet/jami/model/Contact;", "setContact", "(Lnet/jami/model/Contact;)V", "getConversation", "()Lnet/jami/model/ConversationHistory;", "setConversation", "(Lnet/jami/model/ConversationHistory;)V", "<set-?>", "conversationId", "getConversationId", "", "getDaemonId", "()Ljava/lang/Long;", "setDaemonId", "(Ljava/lang/Long;)V", "Ljava/lang/Long;", "daemonIdString", "getDaemonIdString", "Lcom/google/gson/JsonObject;", "getExtraFlag", "()Lcom/google/gson/JsonObject;", "", "getId", "()I", "setId", "(I)V", "isIncoming", "", "()Z", "setIncoming", "(Z)V", "isSwarm", "mExtraFlag", "getMExtraFlag", "setMExtraFlag", "mIsRead", "getMIsRead", "setMIsRead", "mStatus", "getMStatus", "setMStatus", "mType", "getMType", "setMType", "messageId", "getMessageId", "parentId", "getParentId", "Lnet/jami/model/Interaction$InteractionStatus;", "getStatus", "()Lnet/jami/model/Interaction$InteractionStatus;", "setStatus", "(Lnet/jami/model/Interaction$InteractionStatus;)V", "getTimestamp", "()J", "setTimestamp", "(J)V", "getType", "()Lnet/jami/model/Interaction$InteractionType;", "setType", "(Lnet/jami/model/Interaction$InteractionType;)V", "fromJson", "json", "read", "", "setSwarmInfo", "parent", "toJson", "value", "Companion", "InteractionStatus", "InteractionType", "libringclient"})
@com.j256.ormlite.table.DatabaseTable(tableName = "interactions")
public class Interaction {
    @org.jetbrains.annotations.Nullable()
    private java.lang.String account;
    private boolean isIncoming = false;
    @org.jetbrains.annotations.Nullable()
    private net.jami.model.Contact contact;
    @com.j256.ormlite.field.DatabaseField(generatedId = true, columnName = "id", index = true)
    private int id = 0;
    @org.jetbrains.annotations.Nullable()
    @com.j256.ormlite.field.DatabaseField(columnName = "author", index = true)
    private java.lang.String author;
    @org.jetbrains.annotations.Nullable()
    @com.j256.ormlite.field.DatabaseField(columnName = "conversation", foreignColumnName = "id", foreign = true)
    private net.jami.model.ConversationHistory conversation;
    @com.j256.ormlite.field.DatabaseField(columnName = "timestamp", index = true)
    private long timestamp = 0L;
    @org.jetbrains.annotations.Nullable()
    @com.j256.ormlite.field.DatabaseField(columnName = "body")
    private java.lang.String body;
    @org.jetbrains.annotations.Nullable()
    @com.j256.ormlite.field.DatabaseField(columnName = "type")
    private java.lang.String mType;
    @org.jetbrains.annotations.NotNull()
    @com.j256.ormlite.field.DatabaseField(columnName = "status")
    private java.lang.String mStatus;
    @org.jetbrains.annotations.Nullable()
    @com.j256.ormlite.field.DatabaseField(columnName = "daemon_id")
    private java.lang.Long daemonId;
    @com.j256.ormlite.field.DatabaseField(columnName = "is_read")
    private int mIsRead = 0;
    @org.jetbrains.annotations.NotNull()
    @com.j256.ormlite.field.DatabaseField(columnName = "extra_data")
    private java.lang.String mExtraFlag;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String conversationId;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String messageId;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String parentId;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.model.Interaction.Companion Companion = null;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String TABLE_NAME = "interactions";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String COLUMN_ID = "id";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String COLUMN_AUTHOR = "author";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String COLUMN_CONVERSATION = "conversation";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String COLUMN_TIMESTAMP = "timestamp";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String COLUMN_BODY = "body";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String COLUMN_TYPE = "type";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String COLUMN_STATUS = "status";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String COLUMN_DAEMON_ID = "daemon_id";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String COLUMN_IS_READ = "is_read";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String COLUMN_EXTRA_FLAG = "extra_data";
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getAccount() {
        return null;
    }
    
    public final void setAccount(@org.jetbrains.annotations.Nullable()
    java.lang.String p0) {
    }
    
    public final boolean isIncoming() {
        return false;
    }
    
    public final void setIncoming(boolean p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.Contact getContact() {
        return null;
    }
    
    public final void setContact(@org.jetbrains.annotations.Nullable()
    net.jami.model.Contact p0) {
    }
    
    public final int getId() {
        return 0;
    }
    
    public final void setId(int p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getAuthor() {
        return null;
    }
    
    public final void setAuthor(@org.jetbrains.annotations.Nullable()
    java.lang.String p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final net.jami.model.ConversationHistory getConversation() {
        return null;
    }
    
    public final void setConversation(@org.jetbrains.annotations.Nullable()
    net.jami.model.ConversationHistory p0) {
    }
    
    public final long getTimestamp() {
        return 0L;
    }
    
    public final void setTimestamp(long p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getBody() {
        return null;
    }
    
    public final void setBody(@org.jetbrains.annotations.Nullable()
    java.lang.String p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getMType() {
        return null;
    }
    
    public final void setMType(@org.jetbrains.annotations.Nullable()
    java.lang.String p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getMStatus() {
        return null;
    }
    
    public final void setMStatus(@org.jetbrains.annotations.NotNull()
    java.lang.String p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long getDaemonId() {
        return null;
    }
    
    public final void setDaemonId(@org.jetbrains.annotations.Nullable()
    java.lang.Long p0) {
    }
    
    public final int getMIsRead() {
        return 0;
    }
    
    public final void setMIsRead(int p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getMExtraFlag() {
        return null;
    }
    
    public final void setMExtraFlag(@org.jetbrains.annotations.NotNull()
    java.lang.String p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getConversationId() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getMessageId() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getParentId() {
        return null;
    }
    
    public Interaction() {
        super();
    }
    
    public Interaction(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId) {
        super();
    }
    
    public Interaction(@org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation conversation, @org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction.InteractionType type) {
        super();
    }
    
    public Interaction(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.Nullable()
    java.lang.String author, @org.jetbrains.annotations.Nullable()
    net.jami.model.ConversationHistory conversation, @org.jetbrains.annotations.NotNull()
    java.lang.String timestamp, @org.jetbrains.annotations.Nullable()
    java.lang.String body, @org.jetbrains.annotations.Nullable()
    java.lang.String type, @org.jetbrains.annotations.NotNull()
    java.lang.String status, @org.jetbrains.annotations.Nullable()
    java.lang.String daemonId, @org.jetbrains.annotations.NotNull()
    java.lang.String isRead, @org.jetbrains.annotations.NotNull()
    java.lang.String extraFlag) {
        super();
    }
    
    public final void read() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.model.Interaction.InteractionType getType() {
        return null;
    }
    
    public final void setType(@org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction.InteractionType type) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final net.jami.model.Interaction.InteractionStatus getStatus() {
        return null;
    }
    
    public final void setStatus(@org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction.InteractionStatus status) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.google.gson.JsonObject getExtraFlag() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.google.gson.JsonObject toJson(@org.jetbrains.annotations.Nullable()
    java.lang.String value) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String fromJson(@org.jetbrains.annotations.NotNull()
    com.google.gson.JsonObject json) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public java.lang.String getDaemonIdString() {
        return null;
    }
    
    public final boolean isRead() {
        return false;
    }
    
    public final boolean isSwarm() {
        return false;
    }
    
    public final void setSwarmInfo(@org.jetbrains.annotations.NotNull()
    java.lang.String conversationId) {
    }
    
    public final void setSwarmInfo(@org.jetbrains.annotations.NotNull()
    java.lang.String conversationId, @org.jetbrains.annotations.NotNull()
    java.lang.String messageId, @org.jetbrains.annotations.Nullable()
    java.lang.String parent) {
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0015\b\u0086\u0001\u0018\u0000 \u00182\b\u0012\u0004\u0012\u00020\u00000\u0001:\u0001\u0018B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0011\u0010\u0003\u001a\u00020\u00048F\u00a2\u0006\u0006\u001a\u0004\b\u0003\u0010\u0005R\u0011\u0010\u0006\u001a\u00020\u00048F\u00a2\u0006\u0006\u001a\u0004\b\u0006\u0010\u0005j\u0002\b\u0007j\u0002\b\bj\u0002\b\tj\u0002\b\nj\u0002\b\u000bj\u0002\b\fj\u0002\b\rj\u0002\b\u000ej\u0002\b\u000fj\u0002\b\u0010j\u0002\b\u0011j\u0002\b\u0012j\u0002\b\u0013j\u0002\b\u0014j\u0002\b\u0015j\u0002\b\u0016j\u0002\b\u0017\u00a8\u0006\u0019"}, d2 = {"Lnet/jami/model/Interaction$InteractionStatus;", "", "(Ljava/lang/String;I)V", "isError", "", "()Z", "isOver", "UNKNOWN", "SENDING", "SUCCESS", "DISPLAYED", "INVALID", "FAILURE", "TRANSFER_CREATED", "TRANSFER_ACCEPTED", "TRANSFER_CANCELED", "TRANSFER_ERROR", "TRANSFER_UNJOINABLE_PEER", "TRANSFER_ONGOING", "TRANSFER_AWAITING_PEER", "TRANSFER_AWAITING_HOST", "TRANSFER_TIMEOUT_EXPIRED", "TRANSFER_FINISHED", "FILE_AVAILABLE", "Companion", "libringclient"})
    public static enum InteractionStatus {
        /*public static final*/ UNKNOWN /* = new UNKNOWN() */,
        /*public static final*/ SENDING /* = new SENDING() */,
        /*public static final*/ SUCCESS /* = new SUCCESS() */,
        /*public static final*/ DISPLAYED /* = new DISPLAYED() */,
        /*public static final*/ INVALID /* = new INVALID() */,
        /*public static final*/ FAILURE /* = new FAILURE() */,
        /*public static final*/ TRANSFER_CREATED /* = new TRANSFER_CREATED() */,
        /*public static final*/ TRANSFER_ACCEPTED /* = new TRANSFER_ACCEPTED() */,
        /*public static final*/ TRANSFER_CANCELED /* = new TRANSFER_CANCELED() */,
        /*public static final*/ TRANSFER_ERROR /* = new TRANSFER_ERROR() */,
        /*public static final*/ TRANSFER_UNJOINABLE_PEER /* = new TRANSFER_UNJOINABLE_PEER() */,
        /*public static final*/ TRANSFER_ONGOING /* = new TRANSFER_ONGOING() */,
        /*public static final*/ TRANSFER_AWAITING_PEER /* = new TRANSFER_AWAITING_PEER() */,
        /*public static final*/ TRANSFER_AWAITING_HOST /* = new TRANSFER_AWAITING_HOST() */,
        /*public static final*/ TRANSFER_TIMEOUT_EXPIRED /* = new TRANSFER_TIMEOUT_EXPIRED() */,
        /*public static final*/ TRANSFER_FINISHED /* = new TRANSFER_FINISHED() */,
        /*public static final*/ FILE_AVAILABLE /* = new FILE_AVAILABLE() */;
        @org.jetbrains.annotations.NotNull()
        public static final net.jami.model.Interaction.InteractionStatus.Companion Companion = null;
        
        InteractionStatus() {
        }
        
        public final boolean isError() {
            return false;
        }
        
        public final boolean isOver() {
            return false;
        }
        
        @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006J\u000e\u0010\u0007\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006J\u000e\u0010\b\u001a\u00020\u00042\u0006\u0010\t\u001a\u00020\n\u00a8\u0006\u000b"}, d2 = {"Lnet/jami/model/Interaction$InteractionStatus$Companion;", "", "()V", "fromIntFile", "Lnet/jami/model/Interaction$InteractionStatus;", "n", "", "fromIntTextMessage", "fromString", "str", "", "libringclient"})
        public static final class Companion {
            
            private Companion() {
                super();
            }
            
            @org.jetbrains.annotations.NotNull()
            public final net.jami.model.Interaction.InteractionStatus fromString(@org.jetbrains.annotations.NotNull()
            java.lang.String str) {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final net.jami.model.Interaction.InteractionStatus fromIntTextMessage(int n) {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final net.jami.model.Interaction.InteractionStatus fromIntFile(int n) {
                return null;
            }
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\b\b\u0086\u0001\u0018\u0000 \b2\b\u0012\u0004\u0012\u00020\u00000\u0001:\u0001\bB\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006j\u0002\b\u0007\u00a8\u0006\t"}, d2 = {"Lnet/jami/model/Interaction$InteractionType;", "", "(Ljava/lang/String;I)V", "INVALID", "TEXT", "CALL", "CONTACT", "DATA_TRANSFER", "Companion", "libringclient"})
    public static enum InteractionType {
        /*public static final*/ INVALID /* = new INVALID() */,
        /*public static final*/ TEXT /* = new TEXT() */,
        /*public static final*/ CALL /* = new CALL() */,
        /*public static final*/ CONTACT /* = new CONTACT() */,
        /*public static final*/ DATA_TRANSFER /* = new DATA_TRANSFER() */;
        @org.jetbrains.annotations.NotNull()
        public static final net.jami.model.Interaction.InteractionType.Companion Companion = null;
        
        InteractionType() {
        }
        
        @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\b\u0010\u0005\u001a\u0004\u0018\u00010\u0006\u00a8\u0006\u0007"}, d2 = {"Lnet/jami/model/Interaction$InteractionType$Companion;", "", "()V", "fromString", "Lnet/jami/model/Interaction$InteractionType;", "str", "", "libringclient"})
        public static final class Companion {
            
            private Companion() {
                super();
            }
            
            @org.jetbrains.annotations.NotNull()
            public final net.jami.model.Interaction.InteractionType fromString(@org.jetbrains.annotations.Nullable()
            java.lang.String str) {
                return null;
            }
        }
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u000b\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u001a\u0010\u000f\u001a\u00020\u00102\b\u0010\u0011\u001a\u0004\u0018\u00010\u00122\b\u0010\u0013\u001a\u0004\u0018\u00010\u0012R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0014"}, d2 = {"Lnet/jami/model/Interaction$Companion;", "", "()V", "COLUMN_AUTHOR", "", "COLUMN_BODY", "COLUMN_CONVERSATION", "COLUMN_DAEMON_ID", "COLUMN_EXTRA_FLAG", "COLUMN_ID", "COLUMN_IS_READ", "COLUMN_STATUS", "COLUMN_TIMESTAMP", "COLUMN_TYPE", "TABLE_NAME", "compare", "", "a", "Lnet/jami/model/Interaction;", "b", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        public final int compare(@org.jetbrains.annotations.Nullable()
        net.jami.model.Interaction a, @org.jetbrains.annotations.Nullable()
        net.jami.model.Interaction b) {
            return 0;
        }
    }
}