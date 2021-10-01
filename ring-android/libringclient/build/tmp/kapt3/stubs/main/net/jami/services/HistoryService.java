package net.jami.services;

import java.lang.System;

@kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000|\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0006\b&\u0018\u0000 62\u00020\u0001:\u00016B\u0005\u00a2\u0006\u0002\u0010\u0002J,\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\b2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u000b2\u0006\u0010\r\u001a\u00020\u000b2\u0006\u0010\u000e\u001a\u00020\u000fJ\u000e\u0010\u0010\u001a\u00020\u00112\u0006\u0010\n\u001a\u00020\u000bJ\u001e\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u000b2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\u0013\u001a\u00020\u0014J\u0014\u0010\u0010\u001a\u00020\u00112\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00170\u0016J\u0010\u0010\u0018\u001a\u00020\u00192\u0006\u0010\n\u001a\u00020\u000bH$J\u0016\u0010\u001a\u001a\u00020\u00112\u0006\u0010\u001b\u001a\u00020\u001c2\u0006\u0010\n\u001a\u00020\u000bJ\u0012\u0010\u001d\u001a\u0004\u0018\u00010\u001e2\u0006\u0010\u001f\u001a\u00020\u000bH$J\u001c\u0010 \u001a\u000e\u0012\u0004\u0012\u00020\"\u0012\u0004\u0012\u00020\u001c0!2\u0006\u0010\u001f\u001a\u00020\u000bH$J\"\u0010#\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020$0\u00160\b2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010%\u001a\u00020\u001cJ\u0012\u0010&\u001a\u0004\u0018\u00010\u00012\u0006\u0010\u001f\u001a\u00020\u000bH$J\u001c\u0010\'\u001a\u000e\u0012\u0004\u0012\u00020$\u0012\u0004\u0012\u00020\u001c0!2\u0006\u0010\u001f\u001a\u00020\u000bH$J\u001a\u0010(\u001a\u0004\u0018\u00010\u000b2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010)\u001a\u00020*H&J\u001a\u0010+\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020$0\u00160\b2\u0006\u0010\n\u001a\u00020\u000bJ.\u0010,\u001a\b\u0012\u0004\u0012\u00020\t0\b2\u0006\u0010\n\u001a\u00020\u000b2\b\u0010\f\u001a\u0004\u0018\u00010\u000b2\u0006\u0010-\u001a\u00020\u000b2\u0006\u0010.\u001a\u00020\u000bJ\u001e\u0010/\u001a\u00020\u00112\u0006\u0010\n\u001a\u00020\u000b2\u0006\u00100\u001a\u0002012\u0006\u00102\u001a\u00020$J \u00103\u001a\u00020\u00192\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010)\u001a\u00020*2\u0006\u00104\u001a\u00020\u000bH&J\u0016\u00105\u001a\u00020\u00112\u0006\u00102\u001a\u00020$2\u0006\u0010\n\u001a\u00020\u000bR\u0011\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u00067"}, d2 = {"Lnet/jami/services/HistoryService;", "", "()V", "scheduler", "Lio/reactivex/rxjava3/core/Scheduler;", "getScheduler", "()Lio/reactivex/rxjava3/core/Scheduler;", "accountMessageStatusChanged", "Lio/reactivex/rxjava3/core/Single;", "Lnet/jami/model/TextMessage;", "accountId", "", "daemonId", "peer", "status", "Lnet/jami/model/Interaction$InteractionStatus;", "clearHistory", "Lio/reactivex/rxjava3/core/Completable;", "contactId", "deleteConversation", "", "accounts", "", "Lnet/jami/model/Account;", "deleteAccountHistory", "", "deleteInteraction", "id", "", "getConnectionSource", "Lcom/j256/ormlite/support/ConnectionSource;", "dbName", "getConversationDataDao", "Lcom/j256/ormlite/dao/Dao;", "Lnet/jami/model/ConversationHistory;", "getConversationHistory", "Lnet/jami/model/Interaction;", "conversationId", "getHelper", "getInteractionDataDao", "getLastMessageRead", "conversationUri", "Lnet/jami/model/Uri;", "getSmartlist", "incomingMessage", "from", "message", "insertInteraction", "conversation", "Lnet/jami/model/Conversation;", "interaction", "setMessageRead", "lastId", "updateInteraction", "Companion", "libringclient"})
public abstract class HistoryService {
    @org.jetbrains.annotations.NotNull()
    private final io.reactivex.rxjava3.core.Scheduler scheduler = null;
    @org.jetbrains.annotations.NotNull()
    public static final net.jami.services.HistoryService.Companion Companion = null;
    private static final java.lang.String TAG = null;
    
    public HistoryService() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Scheduler getScheduler() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    protected abstract com.j256.ormlite.support.ConnectionSource getConnectionSource(@org.jetbrains.annotations.NotNull()
    java.lang.String dbName);
    
    @org.jetbrains.annotations.NotNull()
    protected abstract com.j256.ormlite.dao.Dao<net.jami.model.Interaction, java.lang.Integer> getInteractionDataDao(@org.jetbrains.annotations.NotNull()
    java.lang.String dbName);
    
    @org.jetbrains.annotations.NotNull()
    protected abstract com.j256.ormlite.dao.Dao<net.jami.model.ConversationHistory, java.lang.Integer> getConversationDataDao(@org.jetbrains.annotations.NotNull()
    java.lang.String dbName);
    
    @org.jetbrains.annotations.Nullable()
    protected abstract java.lang.Object getHelper(@org.jetbrains.annotations.NotNull()
    java.lang.String dbName);
    
    public abstract void setMessageRead(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri conversationUri, @org.jetbrains.annotations.NotNull()
    java.lang.String lastId);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.String getLastMessageRead(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Uri conversationUri);
    
    protected abstract void deleteAccountHistory(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId);
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Completable clearHistory(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId) {
        return null;
    }
    
    /**
     * Clears a conversation's history
     *
     * @param contactId          the participant's contact ID
     * @param accountId          the user's contact ID
     * @param deleteConversation true to completely delete the conversation including contact events
     * @return
     */
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Completable clearHistory(@org.jetbrains.annotations.NotNull()
    java.lang.String contactId, @org.jetbrains.annotations.NotNull()
    java.lang.String accountId, boolean deleteConversation) {
        return null;
    }
    
    /**
     * Clears all interactions in the app. Maintains contact events and actual conversations.
     *
     * @param accounts the list of accounts in the app
     * @return a completable
     */
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Completable clearHistory(@org.jetbrains.annotations.NotNull()
    java.util.List<net.jami.model.Account> accounts) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Completable updateInteraction(@org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction interaction, @org.jetbrains.annotations.NotNull()
    java.lang.String accountId) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Completable deleteInteraction(int id, @org.jetbrains.annotations.NotNull()
    java.lang.String accountId) {
        return null;
    }
    
    /**
     * Inserts an interaction into the database, and if necessary, a conversation
     *
     * @param accountId    the user's account ID
     * @param conversation the conversation
     * @param interaction  the interaction to insert
     * @return a conversation single
     */
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Completable insertInteraction(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    net.jami.model.Conversation conversation, @org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction interaction) {
        return null;
    }
    
    /**
     * Loads data required to load the smartlist. Only requires the most recent message or contact action.
     *
     * @param accountId required to query the appropriate account database
     * @return a list of the most recent interactions with each contact
     */
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<java.util.List<net.jami.model.Interaction>> getSmartlist(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId) {
        return null;
    }
    
    /**
     * Retrieves an entire conversations history
     *
     * @param accountId      the user's account id
     * @param conversationId the conversation id
     * @return a conversation and all of its interactions
     */
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<java.util.List<net.jami.model.Interaction>> getConversationHistory(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, int conversationId) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<net.jami.model.TextMessage> incomingMessage(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.Nullable()
    java.lang.String daemonId, @org.jetbrains.annotations.NotNull()
    java.lang.String from, @org.jetbrains.annotations.NotNull()
    java.lang.String message) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactivex.rxjava3.core.Single<net.jami.model.TextMessage> accountMessageStatusChanged(@org.jetbrains.annotations.NotNull()
    java.lang.String accountId, @org.jetbrains.annotations.NotNull()
    java.lang.String daemonId, @org.jetbrains.annotations.NotNull()
    java.lang.String peer, @org.jetbrains.annotations.NotNull()
    net.jami.model.Interaction.InteractionStatus status) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 5, 1}, k = 1, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0016\u0010\u0003\u001a\n \u0005*\u0004\u0018\u00010\u00040\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Lnet/jami/services/HistoryService$Companion;", "", "()V", "TAG", "", "kotlin.jvm.PlatformType", "libringclient"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}